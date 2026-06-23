package com.smartdx.retail.ai.llm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Conversation message.
 *
 * @author jason.w
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConversationMessage {

    /**
     * Role: "user" or "assistant".
     */
    private String role;

    /**
     * Message content.
     */
    private String content;
}
