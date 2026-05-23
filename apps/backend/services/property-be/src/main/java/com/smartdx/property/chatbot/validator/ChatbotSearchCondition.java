package com.smartdx.property.chatbot.validator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * LLM出力専用のDTO（Chatbotで許可されたフィールドのみ）
 *
 * PropertyLookupReq とは異なり、draft専用/類似画像検索フィールドを含まない
 */
@Getter
@Setter
public class ChatbotSearchCondition {

    @Size(max = 200, message = "keyword は200文字以内")
    private String keyword;

    @Valid
    private ChatbotSearchItems searchItems;

    @Pattern(regexp = "^(listedDate|priceJpy|stationWalkMin)$",
            message = "sortBy は listedDate, priceJpy, stationWalkMin のいずれか")
    private String sortBy;

    @Pattern(regexp = "^(asc|desc)$", message = "orderBy は asc または desc")
    private String orderBy;

    // scope, page, size はサーバー側で強制設定するため受け付けない

    /**
     * Chatbot用検索条件
     */
    @Getter
    @Setter
    public static class ChatbotSearchItems {

        private List<String> area;

        @Min(value = 0, message = "priceJpyMin は0以上")
        @Max(value = 10_000_000_000L, message = "priceJpyMin は10,000,000,000以下")
        private Long priceJpyMin;

        @Min(value = 0, message = "priceJpyMax は0以上")
        @Max(value = 10_000_000_000L, message = "priceJpyMax は10,000,000,000以下")
        private Long priceJpyMax;

        private List<String> propertyType;

        @Min(value = 1, message = "stationWalkMax は1以上")
        @Max(value = 60, message = "stationWalkMax は60以下")
        private Integer stationWalkMax;

        // priorityRank, reviewStatus, registered*, *Id はChatbot非対応のため含まない
    }
}
