package com.smartdx.retail.ai.llm;

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
