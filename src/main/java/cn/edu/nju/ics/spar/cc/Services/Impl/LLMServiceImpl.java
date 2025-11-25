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
     */
    interface ServiceAgent {
        @SystemMessage("You are a professional programmer and programming language expert.")
        boolean analyze(@MemoryId Object memoryId, @UserMessage String prompt);
    }

    // ==================== Fields ====================

    // LangChain4j related
    private ServiceAgent serviceAgent;
    private boolean isMock;

    // Async LLM call management
    private final ConcurrentHashMap<String, String> asyncAskQueue = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> asyncResults = new ConcurrentHashMap<>();

    // Thread pool for concurrent async execution
    private final ExecutorService asyncExecutor;

    // ThreadLocal to pass requestId transparently for async detection
    private final ThreadLocal<String> currentAsyncRequestId = new ThreadLocal<>();

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

        // Initialize async executor with a reasonable thread pool size
        // Use min(10, available processors) to balance concurrency and resource usage
        int poolSize = Math.min(10, Runtime.getRuntime().availableProcessors());
        this.asyncExecutor = Executors.newFixedThreadPool(poolSize);
        logger.info("Async executor initialized with pool size: " + poolSize);

        // Add shutdown hook for proper resource cleanup
        addShutdownHook();
    }

    // ==================== Synchronous LLM Call Methods ====================

    @Override
    public boolean ask(String prompt) throws Exception {
        if (isMock) {
            logger.debug("[MOCK LLM] Ask: " + prompt);
            return true;
        }

        // Generate a unique memoryId for this interaction
        Object memoryId = UUID.randomUUID();
        int maxRetries = 10;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.debug("[OpenRouter LLM] Ask (Attempt " + attempt + "): " + prompt);
                boolean result = serviceAgent.analyze(memoryId, prompt);
                logger.debug("[OpenRouter LLM] Result: " + result);
                return result;
            } catch (Exception e) {
                // Check if it's a parsing error
                if (e.getClass().getName().contains("OutputParsingException")) {
                    logger.warn("LLM output parsing failed (Attempt " + attempt + "/" + maxRetries + ")");
                    
                    if (attempt == maxRetries) {
                        logger.error("Max retries reached. Giving up.");
                        throw new InfuseException("LLM Call Failed after " + maxRetries + " retries", e);
                    }
                    
                    // Continue to next iteration with corrective prompt
                    // The next call will use the same memoryId, so chat history is preserved
                    prompt = "Your previous answer was not in valid format.";
                } else {
                    // Non-parsing error (e.g., network), throw immediately
                    logger.error("OpenRouter Call Failed: " + e.getMessage());
                    throw new InfuseException("LLM Call Failed", e);
                }
            }
        }
        
        // Should never reach here
        throw new InfuseException("Unexpected error in LLM retry loop");
    }

    // ==================== Asynchronous LLM Methods ====================

    @Override
    public boolean askAsync(String prompt) {
        String requestId = UUID.randomUUID().toString();
        asyncAskQueue.put(requestId, prompt);
        currentAsyncRequestId.set(requestId); // Store in ThreadLocal for transparent detection
        logger.debug("[Async LLM] Registered askAsync with requestId: " + requestId);
        return true; // Return placeholder value (actual result determined later)
    }

    @Override
    public Map<String, Boolean> executeAllAsync() throws Exception {
        if (asyncAskQueue.isEmpty()) {
            logger.info("[Async LLM] No async requests to execute.");
            return new ConcurrentHashMap<>();
        }

        logger.info("[Async LLM] Executing " + asyncAskQueue.size() + " async requests concurrently...");

        // Create a concurrent map to store results
        Map<String, Boolean> results = new ConcurrentHashMap<>();
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
                    boolean result = ask(prompt); // Reuse synchronous ask with retry logic
                    results.put(requestId, result);
                    successCount.incrementAndGet();
                    logger.debug("[Async LLM] Completed askAsync requestId: " + requestId + ", result: " + result);
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    logger.error("[Async LLM] Failed askAsync requestId: " + requestId + ", error: " + e.getMessage());
                    // For concurrent execution, we store failure results as false rather than throwing
                    results.put(requestId, false);
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

        return results;
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

    @Override
    public String pollAsyncRequestId() {
        String requestId = currentAsyncRequestId.get();
        currentAsyncRequestId.remove(); // Clear ThreadLocal after reading
        return requestId;
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

    /**
     * Add a shutdown hook to ensure proper resource cleanup
     */
    public void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }
}
