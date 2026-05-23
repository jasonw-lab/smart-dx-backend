package com.smartdx.property.chatbot.llm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 会話メッセージ
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConversationMessage {

    /**
     * 発言者: "user" または "assistant"
     */
    private String role;

    /**
     * メッセージ内容
     */
    private String content;
}
