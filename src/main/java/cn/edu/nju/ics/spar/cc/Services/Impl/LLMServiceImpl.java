package cn.edu.nju.ics.spar.cc.Services.Impl;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    interface BooleanAgent {
        @SystemMessage("Analyze the user's statement and answer strictly with 'true' or 'false'.")
        boolean analyze(@MemoryId Object memoryId, @UserMessage String prompt);
    }

    /**
     * Internal class to hold task request details for async execution.
     */
    private static class TaskRequest {
        LLMService.TaskType taskType;
        Map<String, Object> params;

        TaskRequest(LLMService.TaskType taskType, Map<String, Object> params) {
            this.taskType = taskType;
            this.params = params;
        }
    }

    // ==================== Fields ====================

    // LangChain4j related
    private BooleanAgent booleanAgent;
    private boolean isMock;

    // Async LLM call management
    private final ConcurrentHashMap<String, String> asyncAskQueue = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TaskRequest> asyncTaskQueue = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> asyncResults = new ConcurrentHashMap<>();
    
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
                        .timeout(Duration.ofSeconds(60))
                        .logRequests(true)
                        .logResponses(true)
                        .build();

                // Configure with ChatMemoryProvider as per documentation
                this.booleanAgent = AiServices.builder(BooleanAgent.class)
                        .chatModel(model)
                        .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
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
        int maxRetries = 3;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.debug("[OpenRouter LLM] Ask (Attempt " + attempt + "): " + prompt);
                boolean result = booleanAgent.analyze(memoryId, prompt);
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

    // ==================== Task-based LLM Methods ====================

    @Override
    public boolean executeTask(LLMService.TaskType taskType, Map<String, Object> params) throws Exception {
        String prompt = buildPromptFromTask(taskType, params);
        logger.debug("[LLM Task] Type: " + taskType + ", Prompt: " + prompt);
        return ask(prompt); // Reuse the ask method with retry logic
    }

    /**
     * Build prompt from task type and parameters.
     */
    private String buildPromptFromTask(LLMService.TaskType taskType, Map<String, Object> params) {
        switch (taskType) {
            default:
                throw new UnsupportedOperationException("Task type not implemented: " + taskType);
        }
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
    public boolean executeTaskAsync(LLMService.TaskType taskType, Map<String, Object> params) {
        String requestId = UUID.randomUUID().toString();
        asyncTaskQueue.put(requestId, new TaskRequest(taskType, params));
        currentAsyncRequestId.set(requestId); // Store in ThreadLocal
        logger.debug("[Async LLM] Registered executeTaskAsync with requestId: " + requestId);
        return true; // Return placeholder value (actual result determined later)
    }

    @Override
    public Map<String, Boolean> executeAllAsync() throws Exception {
        logger.info("[Async LLM] Executing " + (asyncAskQueue.size() + asyncTaskQueue.size()) + " async requests...");

        Map<String, Boolean> results = new ConcurrentHashMap<>();

        // Execute all async ask requests
        for (Map.Entry<String, String> entry : asyncAskQueue.entrySet()) {
            String requestId = entry.getKey();
            String prompt = entry.getValue();
            try {
                boolean result = ask(prompt); // Reuse synchronous ask with retry logic
                results.put(requestId, result);
                logger.debug("[Async LLM] Completed askAsync requestId: " + requestId + ", result: " + result);
            } catch (Exception e) {
                logger.error("[Async LLM] Failed askAsync requestId: " + requestId + ", error: " + e.getMessage());
                throw e; // Re-throw to signal failure
            }
        }

        // Execute all async task requests
        for (Map.Entry<String, TaskRequest> entry : asyncTaskQueue.entrySet()) {
            String requestId = entry.getKey();
            TaskRequest taskRequest = entry.getValue();
            try {
                boolean result = executeTask(taskRequest.taskType, taskRequest.params); // Reuse synchronous executeTask
                results.put(requestId, result);
                logger.debug("[Async LLM] Completed executeTaskAsync requestId: " + requestId + ", result: " + result);
            } catch (Exception e) {
                logger.error("[Async LLM] Failed executeTaskAsync requestId: " + requestId + ", error: " + e.getMessage());
                throw e;
            }
        }

        logger.info("[Async LLM] All async requests completed. Total results: " + results.size());
        return results;
    }

    @Override
    public void clearAsync() {
        asyncAskQueue.clear();
        asyncTaskQueue.clear();
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
        int removedTaskCount = asyncTaskQueue.size();

        // Retain only valid requests
        asyncAskQueue.keySet().retainAll(validRequestIds);
        asyncTaskQueue.keySet().retainAll(validRequestIds);
        asyncResults.keySet().retainAll(validRequestIds);

        removedAskCount -= asyncAskQueue.size();
        removedTaskCount -= asyncTaskQueue.size();

        logger.debug("[Async LLM] Retained " + asyncAskQueue.size() + " ask requests and " +
                    asyncTaskQueue.size() + " task requests. Removed " + removedAskCount +
                    " ask requests and " + removedTaskCount + " task requests.");
    }

    @Override
    public String pollAsyncRequestId() {
        String requestId = currentAsyncRequestId.get();
        currentAsyncRequestId.remove(); // Clear ThreadLocal after reading
        return requestId;
    }
}
