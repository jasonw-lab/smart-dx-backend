package com.smartdx.property.chatbot.model;

import com.smartdx.property.model.req.PropertyLookupReq;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Rule-Based抽出結果を表す内部モデル
 */
@Getter
@Setter
@Builder
public class ExtractionResult {

    /**
     * 抽出された検索条件
     */
    private PropertyLookupReq condition;

    /**
     * 抽出されたフィールド名リスト
     */
    private List<String> extractedFields;

    /**
     * 曖昧表現が検出されたかどうか
     */
    private boolean ambiguousExpressionDetected;

    /**
     * 検出された曖昧表現リスト
     */
    private List<String> ambiguousExpressions;

    /**
     * 抽出の信頼度
     */
    private ConfidenceLevel confidence;

    /**
     * 信頼度レベル
     */
    public enum ConfidenceLevel {
        /**
         * 1項目以上抽出、曖昧表現なし
         */
        HIGH,
        /**
         * 1項目以上抽出、曖昧表現あり
         */
        PARTIAL,
        /**
         * 0項目抽出
         */
        LOW
    }

    /**
     * 最低条件を満たしているか
     */
    public boolean hasMinimumCondition() {
        if (condition == null) {
            return false;
        }
        if (condition.getKeyword() != null && !condition.getKeyword().isBlank()) {
            return true;
        }
        var items = condition.getSearchItems();
        if (items == null) {
            return false;
        }
        return (items.getArea() != null && !items.getArea().isEmpty())
                || items.getPriceJpyMin() != null
                || items.getPriceJpyMax() != null
                || (items.getPropertyType() != null && !items.getPropertyType().isEmpty())
                || items.getStationWalkMax() != null;
    }
}
