package com.smartdx.property.chatbot.model.req;

import com.smartdx.property.model.req.PropertyLookupReq;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Chatbot検索リクエストDTO
 *
 * POST /api/v1/properties/chatbot/search
 */
@Getter
@Setter
public class ChatbotSearchReq {

    /**
     * ユーザーの自然言語入力
     */
    @NotBlank(message = "message は必須です")
    @Size(min = 1, max = 2000, message = "message は1文字以上2000文字以内で入力してください")
    private String message;

    /**
     * 直前の会話履歴（フロントエンド保持、最大30件）
     */
    @Valid
    @Size(max = 30, message = "conversationHistory は最大30件です")
    private List<ConversationHistoryItem> conversationHistory;

    /**
     * 直前の検索条件（マージ用）
     */
    private PropertyLookupReq previousCondition;

    /**
     * 会話履歴アイテム
     */
    @Getter
    @Setter
    public static class ConversationHistoryItem {

        /**
         * 発言者: "user" または "assistant"
         */
        private String role;

        /**
         * 発言内容（最大500文字）
         */
        @Size(max = 500, message = "content は最大500文字です")
        private String content;

        /**
         * アシスタント応答時の抽出条件
         */
        private PropertyLookupReq extractedCondition;
    }
}
