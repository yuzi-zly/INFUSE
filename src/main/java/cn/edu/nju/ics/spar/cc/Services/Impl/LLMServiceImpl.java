package cn.edu.nju.ics.spar.cc.Services.Impl;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

import cn.edu.nju.ics.spar.cc.Constraints.Formulas.FBfunc;
import cn.edu.nju.ics.spar.cc.Services.LLMService;
import cn.edu.nju.ics.spar.cc.Util.InfuseException;
import cn.edu.nju.ics.spar.cc.Util.Loggable;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public class LLMServiceImpl implements LLMService, Loggable {

    // ==================== Inner Classes ====================

    /**
     * AI Service Interface with @MemoryId support for LangChain4j.
     * Returns raw String response from LLM.
     */
    interface ServiceAgent {
        @SystemMessage("You are a professional programmer and programming language expert.")
        String analyze(@MemoryId Object memoryId, @UserMessage String prompt);
    }

    // ==================== Fields ====================

    // LangChain4j related
    private ServiceAgent serviceAgent;
    private boolean isMock;

    // Async LLM call management
    private final ConcurrentHashMap<String, String> asyncAskQueue;
    private final ConcurrentHashMap<String, String> asyncResults;

    // Thread pool for concurrent async execution
    private final ExecutorService asyncExecutor;
    
    // Lock and Condition for thread synchronization (worker threads wait, main thread signals)
    private final Lock lock;
    private final Condition resultsReady;

    // ==================== Constructor ====================

    public LLMServiceImpl() {
        String apiKey = System.getenv("OPENROUTER_API_KEY");
        String modelName = System.getenv("OPENROUTER_MODEL"); 
        if (modelName == null || modelName.isEmpty()) {
            modelName = "deepseek/deepseek-v3.2-exp"; 
        }
        
        if (apiKey != null && !apiKey.isEmpty()) {
            logger.info("Initializing LLM Service with OpenRouter API...");
            try {
                var model = OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl("https://openrouter.ai/api/v1")
                        .modelName(modelName)
                        .timeout(Duration.ofSeconds(120))
                        // .logRequests(true)
                        // .logResponses(true)
                        .build();

                // Configure with ChatMemoryProvider as per documentation
                this.serviceAgent = AiServices.builder(ServiceAgent.class)
                        .chatModel(model)
                        .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                        .build();
                        
                this.isMock = false;
                logger.info("Connected to OpenRouter using model: " + modelName);

            } catch (Exception e) {
                logger.error("Failed to initialize OpenRouter client: " + e.getMessage());
                this.isMock = true;
            }
        } else {
            logger.warn("OPENROUTER_API_KEY not found. Running in MOCK mode.");
            this.isMock = true;
        }

        this.asyncAskQueue = new ConcurrentHashMap<>();
        this.asyncResults = new ConcurrentHashMap<>();
        this.asyncExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.lock = new ReentrantLock();
        this.resultsReady = lock.newCondition();

        // Add shutdown hook for proper resource cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    // ==================== Synchronous LLM Call Methods ====================

    @Override
    public String ask(String prompt) {
        if (isMock) {
            logger.debug("[MOCK LLM] Ask: " + prompt);
            return "true"; // Mock returns string "true"
        }

        // Generate a unique memoryId for this interaction
        Object memoryId = UUID.randomUUID();
        
        logger.debug("[OpenRouter LLM] Ask: " + prompt);
        String result = serviceAgent.analyze(memoryId, prompt);
        logger.debug("[OpenRouter LLM] Result: " + result + " (Type: " + result.getClass().getName() + ")");
        return result;
    }

    // ==================== Asynchronous LLM Methods ====================

    @Override
    public String askAsync(String prompt) {
        // Get requestId from FBfunc's ThreadLocal (must be called from bfunc's virtual thread)
        String requestId = FBfunc.getCurrentRequestId();
        if (requestId == null) {
            throw new IllegalStateException(
                "askAsync() must be called from a bfunc's virtual thread context. No requestId found."
            );
        }
        
        return _askAsync(requestId, prompt);
    }

    private String _askAsync(String requestId, String prompt) {
        asyncAskQueue.put(requestId, prompt);
        
        logger.debug("[Async LLM] Worker thread registered request: " + requestId);
        
        // Block current thread and wait for main thread to execute executeAllAsync()
        lock.lock();
        try {
            // Loop to prevent spurious wakeups
            while (!asyncResults.containsKey(requestId)) {
                logger.debug("[Async LLM] Worker thread waiting for result: " + requestId);
                resultsReady.await(); // Block current virtual thread, release lock
            }
            
            // Get and return the actual result
            String result = asyncResults.remove(requestId);
            logger.debug("[Async LLM] Worker thread got result: " + requestId + " -> " + result);
            return result;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("LLM request interrupted", e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void executeAllAsync() {
        if (asyncAskQueue.isEmpty()) {
            logger.info("[Async LLM] No async requests to execute.");
            return;
        }

        logger.info("[Async LLM] Executing " + asyncAskQueue.size() + " async requests concurrently...");

        // Create a concurrent map to store results
        Map<String, String> results = new ConcurrentHashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Submit all requests to the thread pool concurrently
        Future<?>[] futures = new Future<?>[asyncAskQueue.size()];
        AtomicInteger index = new AtomicInteger(0);

        for (Map.Entry<String, String> entry : asyncAskQueue.entrySet()) {
            String requestId = entry.getKey();
            String prompt = entry.getValue();

            Future<?> future = asyncExecutor.submit(() -> {
                try {
                    String result = ask(prompt); // Call LLM and get String result
                    results.put(requestId, result);
                    successCount.incrementAndGet();
                    logger.debug("[Async LLM] Completed askAsync requestId: " + requestId + ", result: " + result);
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    logger.error("[Async LLM] Failed askAsync requestId: " + requestId + ", error: " + e.getMessage());
                    // For concurrent execution, we store failure results as error message
                    results.put(requestId, "ERROR: " + e.getMessage());
                }
            });

            futures[index.getAndIncrement()] = future;
        }

        // Wait for all tasks to complete
        try {
            for (Future<?> future : futures) {
                future.get(180, TimeUnit.SECONDS); // 3 minutes timeout per request
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("[Async LLM] Async execution interrupted: " + e.getMessage());
            throw new InfuseException("Async execution interrupted", e);
        } catch (ExecutionException e) {
            logger.error("[Async LLM] Async execution failed: " + e.getMessage());
            throw new InfuseException("Async execution failed", e);
        } catch (java.util.concurrent.TimeoutException e) {
            logger.error("[Async LLM] Async execution timed out");
            throw new InfuseException("Async execution timed out", e);
        }

        logger.info("[Async LLM] All async requests completed. Success: " + successCount.get() +
                   ", Failures: " + failureCount.get() + ", Total results: " + results.size());

        // Write results and wake up all blocked worker threads
        lock.lock();
        try {
            asyncResults.putAll(results);
            resultsReady.signalAll(); // Wake up all threads waiting in askAsync()
            logger.debug("[Async LLM] Signaled all waiting worker threads");
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clearAsync() {
        asyncAskQueue.clear();
        asyncResults.clear();
        logger.debug("[Async LLM] Cleared all async queues and results.");
    }

    @Override
    public void retainAsyncRequests(Set<String> validRequestIds) {
        if (validRequestIds == null) {
            // If null, clear everything
            clearAsync();
            return;
        }

        int removedAskCount = asyncAskQueue.size();
        // Retain only valid requests
        asyncAskQueue.keySet().retainAll(validRequestIds);
        asyncResults.keySet().retainAll(validRequestIds);
        removedAskCount -= asyncAskQueue.size();

        logger.debug("[Async LLM] Retained " + asyncAskQueue.size() + " ask requests. Removed " + removedAskCount +
                    " ask requests.");
    }

    /**
     * Gracefully shutdown the async executor. This should be called when the service
     * is no longer needed to clean up resources properly.
     */
    public void shutdown() {
        try {
            logger.info("[Async LLM] Shutting down async executor...");

            // Stop accepting new tasks
            asyncExecutor.shutdown();

            // Wait for existing tasks to complete
            if (!asyncExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("[Async LLM] Async executor did not terminate gracefully, forcing shutdown...");

                // Cancel currently executing tasks
                asyncExecutor.shutdownNow();

                // Wait a bit longer for tasks to respond to being cancelled
                if (!asyncExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.error("[Async LLM] Async executor failed to terminate completely");
                }
            }

            logger.info("[Async LLM] Async executor shutdown completed.");
        } catch (InterruptedException e) {
            logger.error("[Async LLM] Interrupted while waiting for async executor shutdown", e);
            // Preserve interrupt status
            Thread.currentThread().interrupt();
            // Force shutdown
            asyncExecutor.shutdownNow();
        }
    } 
}
