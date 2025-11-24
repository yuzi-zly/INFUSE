package cn.edu.nju.ics.spar.cc.Services;

import java.util.Map;
import java.util.Set;

/**
 * Interface for Large Language Model services.
 * User's Bfunction can inject this service to make LLM calls.
 */
public interface LLMService {

    /**
     * Predefined task types with optimized prompts.
     */
        enum TaskType {
            // More task types can be added
        }
    
    // ========== Synchronous Methods ==========
    
    /**
     * General-purpose LLM call with custom prompt (synchronous).
     * 
     * @param prompt The prompt to send
     * @return The boolean result (True/False)
     * @throws Exception if the call fails
     */
    boolean ask(String prompt) throws Exception;

    /**
     * Execute a predefined task with parameters (synchronous).
     * This provides a high-level abstraction for common LLM tasks.
     * 
     * @param taskType The type of task to execute
     * @param params Task-specific parameters
     * @return The boolean result
     * @throws Exception if the call fails
     */
    boolean executeTask(TaskType taskType, Map<String, Object> params) throws Exception;

    // ========== Asynchronous Methods ==========
    
    /**
     * Register an async LLM call with custom prompt.
     * This method does NOT immediately execute the call.
     * The returned boolean is a placeholder value that will be replaced by the actual result later.
     * 
     * @param prompt The prompt to send
     * @return A placeholder boolean value (always true)
     */
    boolean askAsync(String prompt);

    /**
     * Register an async LLM task with parameters.
     * This method does NOT immediately execute the call.
     * The returned boolean is a placeholder value that will be replaced by the actual result later.
     * 
     * @param taskType The type of task to execute
     * @param params Task-specific parameters
     * @return A placeholder boolean value (always true)
     */
    boolean executeTaskAsync(TaskType taskType, Map<String, Object> params);

    // ========== INTERNAL API - LLM Async Resolution (Engine Use Only) ==========
    // WARNING: The following methods are used internally by the INFUSE engine.
    // Bfunction developers should NOT call these methods directly.

    /**
     * [INTERNAL] Execute all registered async LLM calls in batch.
     * This method synchronously processes all queued async requests and returns the results.
     * 
     * Usage by engine:
     * 1. Checker calls rule.llmResolve_ECC() to collect async LLM requests
     * 2. Checker calls this method to execute all requests in batch
     * 3. Checker uses the returned map to update RuntimeNode.llmResolveStatus
     * 
     * @return A map from requestId to boolean result (true/false)
     * @throws Exception if any LLM call fails
     */
    Map<String, Boolean> executeAllAsync() throws Exception;

    /**
     * [INTERNAL] Clear all async queues and results.
     * This should be called by the engine after processing all async requests
     * to prepare for the next batch.
     */
    void clearAsync();

    /**
     * [INTERNAL] Retain only the specified async requests and remove all others.
     * This is used to clean up redundant async requests after short-circuit optimization.
     *
     * @param validRequestIds The set of request IDs that should be retained
     */
    void retainAsyncRequests(Set<String> validRequestIds);

    /**
     * [INTERNAL] Poll and clear the current async requestId from ThreadLocal.
     * 
     * This method is used by the engine to transparently detect if an async LLM call
     * was made within a bfunc execution. The flow is:
     * 1. Bfunc calls llmService.askAsync(prompt) or executeTaskAsync(...)
     * 2. The service stores the requestId in ThreadLocal
     * 3. After bfunc returns, engine calls this method to check if async call was made
     * 4. If requestId exists, engine marks the node as PENDING_LLM
     * 
     * @return The requestId if an async call was made, or null otherwise
     */
    String pollAsyncRequestId();


}
