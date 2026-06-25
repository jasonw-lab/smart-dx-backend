package com.smartdx.retail.ai.llm;

import java.util.concurrent.CompletableFuture;

/**
 * LLM client interface.
 *
 * @author jason.w
 */
public interface LlmClient {

    /**
     * Send a prompt and receive a response.
     *
     * @param request LLM request
     * @return LLM response
     * @throws LlmException when the LLM call fails
     */
    LlmResponse complete(LlmRequest request) throws LlmException;

    /**
     * Send a prompt asynchronously.
     *
     * @param request LLM request
     * @return future LLM response
     */
    CompletableFuture<LlmResponse> completeAsync(LlmRequest request);

    /**
     * Get the provider name.
     *
     * @return provider name, e.g. gemini
     */
    String getProviderName();

    /**
     * Check whether the client is available.
     *
     * @return true when available
     */
    boolean isAvailable();
}
