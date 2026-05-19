package com.smartdx.property.chatbot.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdx.property.model.req.PropertyLookupReq;
import com.smartdx.property.model.req.PropertySearchItemsReq;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * LLM出力バリデーター
 *
 * JSON形式検証 → DTOマッピング → Bean Validation → ビジネスルール検証
 */
@Component
@Slf4j
public class LlmOutputValidator {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    // 有効なエリアコード
    private static final Set<String> VALID_AREAS = Set.of(
            "tokyo-chiyoda", "tokyo-chuo", "tokyo-minato", "tokyo-shinjuku",
            "tokyo-shibuya", "tokyo-meguro", "tokyo-setagaya", "tokyo-ota",
            "tokyo-shinagawa", "osaka-kita", "kanagawa-yokohama"
    );

    // 有効な物件種別
    private static final Set<String> VALID_PROPERTY_TYPES = Set.of(
            "mansion", "house", "office", "retail", "land"
    );

    public LlmOutputValidator(ObjectMapper objectMapper, Validator validator) {
        // 未知のプロパティを無視する設定
        this.objectMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.validator = validator;
    }

    /**
     * LLM出力を検証し、ChatbotSearchConditionに変換
     *
     * @param llmOutput LLM出力文字列
     * @return 検証済みChatbotSearchCondition
     * @throws LlmOutputValidationException 検証失敗時
     */
    public ChatbotSearchCondition validateAndParse(String llmOutput) {
        // 1. JSON部分を抽出
        String jsonPart = extractJsonPart(llmOutput);

        // 2. JSONパース + DTOマッピング（未知フィールドは無視）
        ChatbotSearchCondition condition;
        try {
            condition = objectMapper.readValue(jsonPart, ChatbotSearchCondition.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse LLM output as JSON: {}", truncate(llmOutput, 200));
            throw new LlmOutputValidationException("Invalid JSON format", e);
        }

        // 3. Bean Validation
        Set<ConstraintViolation<ChatbotSearchCondition>> violations = validator.validate(condition);
        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            log.warn("LLM output validation failed: {}", errors);
            throw new LlmOutputValidationException("Validation failed: " + errors);
        }

        // 4. 許可リスト検証（不正なエリアコード・物件種別を除去）
        sanitizeCondition(condition);

        // 5. ビジネスルール検証
        validateBusinessRules(condition);

        return condition;
    }

    /**
     * JSON部分を抽出
     */
    private String extractJsonPart(String output) {
        if (output == null || output.isBlank()) {
            throw new LlmOutputValidationException("LLM output is empty");
        }
        int start = output.indexOf('{');
        int end = output.lastIndexOf('}');
        if (start == -1 || end == -1 || start >= end) {
            throw new LlmOutputValidationException("No JSON object found in LLM output");
        }
        return output.substring(start, end + 1);
    }

    /**
     * 許可リストに基づいて不正な値を除去
     */
    private void sanitizeCondition(ChatbotSearchCondition condition) {
        if (condition.getSearchItems() == null) {
            return;
        }

        var items = condition.getSearchItems();

        // エリアコード検証
        if (items.getArea() != null) {
            List<String> validAreas = items.getArea().stream()
                    .filter(VALID_AREAS::contains)
                    .collect(Collectors.toList());
            if (validAreas.size() != items.getArea().size()) {
                log.warn("Invalid area codes filtered: original={}, valid={}",
                        items.getArea(), validAreas);
            }
            items.setArea(validAreas.isEmpty() ? null : validAreas);
        }

        // 物件種別検証
        if (items.getPropertyType() != null) {
            List<String> validTypes = items.getPropertyType().stream()
                    .filter(VALID_PROPERTY_TYPES::contains)
                    .collect(Collectors.toList());
            if (validTypes.size() != items.getPropertyType().size()) {
                log.warn("Invalid property types filtered: original={}, valid={}",
                        items.getPropertyType(), validTypes);
            }
            items.setPropertyType(validTypes.isEmpty() ? null : validTypes);
        }
    }

    /**
     * ビジネスルール検証
     */
    private void validateBusinessRules(ChatbotSearchCondition condition) {
        var items = condition.getSearchItems();
        if (items == null) {
            return;
        }

        // priceJpyMin ≤ priceJpyMax の検証
        if (items.getPriceJpyMin() != null && items.getPriceJpyMax() != null) {
            if (items.getPriceJpyMin() > items.getPriceJpyMax()) {
                log.warn("Price range invalid: min={} > max={}, clearing both",
                        items.getPriceJpyMin(), items.getPriceJpyMax());
                items.setPriceJpyMin(null);
                items.setPriceJpyMax(null);
            }
        }
    }

    /**
     * ChatbotSearchCondition を PropertyLookupReq に変換
     * scope, page, size はサーバー側で強制設定
     */
    public PropertyLookupReq toPropertyLookupReq(ChatbotSearchCondition condition) {
        PropertyLookupReq req = new PropertyLookupReq();
        req.setScope("published"); // 強制
        req.setPage(0);            // 強制
        req.setSize(20);           // 強制
        req.setKeyword(condition.getKeyword());
        req.setSortBy(condition.getSortBy() != null ? condition.getSortBy() : "listedDate");
        req.setOrderBy(condition.getOrderBy() != null ? condition.getOrderBy() : "desc");

        if (condition.getSearchItems() != null) {
            PropertySearchItemsReq items = new PropertySearchItemsReq();
            items.setArea(condition.getSearchItems().getArea());
            items.setPriceJpyMin(condition.getSearchItems().getPriceJpyMin());
            items.setPriceJpyMax(condition.getSearchItems().getPriceJpyMax());
            items.setPropertyType(condition.getSearchItems().getPropertyType());
            items.setStationWalkMax(condition.getSearchItems().getStationWalkMax());
            req.setSearchItems(items);
        }

        return req;
    }

    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "null";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}
