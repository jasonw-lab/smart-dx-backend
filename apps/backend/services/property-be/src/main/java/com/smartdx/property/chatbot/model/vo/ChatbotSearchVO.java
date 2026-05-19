package com.smartdx.property.chatbot.model.vo;

import com.smartdx.property.model.req.PropertyLookupReq;
import com.smartdx.property.model.vo.PropertySummaryVO;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Chatbot検索レスポンスVO
 *
 * API仕様: code/msg/data 構造でラップされる
 */
@Getter
@Setter
@Builder
public class ChatbotSearchVO {

    /**
     * アシスタントの応答メッセージ
     */
    private String reply;

    /**
     * 抽出された検索条件（PropertyLookupReq形式）
     */
    private PropertyLookupReq extractedCondition;

    /**
     * 抽出方式: "rule_based" | "llm_supplemented" | "llm_only" | "none"
     */
    private String extractionMethod;

    /**
     * LLMを使用したかどうか
     */
    private boolean llmUsed;

    /**
     * 使用したLLMモデル名（LLM使用時のみ）
     */
    private String llmModel;

    /**
     * 検索結果
     */
    private SearchResultPage searchResults;

    /**
     * 確認が必要か（Phase 5用）
     */
    private boolean clarificationNeeded;

    /**
     * 確認質問（Phase 5用）
     */
    private String clarificationQuestion;

    /**
     * 検索結果ページ（PageResult.PageData と同等構造）
     */
    @Getter
    @Setter
    @Builder
    public static class SearchResultPage {

        /**
         * 物件サマリー配列
         */
        private List<PropertySummaryVO> list;

        /**
         * 総件数
         */
        private long total;
    }
}
