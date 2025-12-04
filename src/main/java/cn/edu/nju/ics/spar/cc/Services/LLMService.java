package cn.edu.nju.ics.spar.cc.Services;

import java.util.Set;

/**
 * Interface for Large Language Model services.
 * User's Bfunction can inject this service to make LLM calls.
 */
public interface LLMService {
    
    /**
     * General-purpose LLM call with custom prompt (synchronous).
     * 
     * @param prompt The prompt to send
     * @return The raw String response from LLM
     */
    String ask(String prompt);
    
    /**
     * Register an async LLM call with custom prompt and wait for the result.
     * This method will block the current thread until the main thread executes all async requests.
     * Should be called from virtual threads to avoid blocking OS threads.
     * 
     * @param prompt The prompt to send
     * @return The actual String result from LLM
     */
    String askAsync(String prompt);

    // ========== INTERNAL API - LLM Async Resolution (Engine Use Only) ==========
    // WARNING: The following methods are used internally by the INFUSE engine.
    // Bfunction developers should NOT call these methods directly.

    /**
     * [INTERNAL] Execute all registered async LLM calls in batch.
     * This method synchronously processes all queued async requests and returns the results.
     * After execution, it will wake up all blocked worker threads waiting in askAsync().
     * 
     * Usage by engine:
     * 1. Checker calls rule.llmResolve_ECC() to collect async LLM requests
     * 2. Checker calls this method to execute all requests in batch
     * 3. Results are stored in asyncResults and signaled to waiting threads
     */
    void executeAllAsync();

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

}
