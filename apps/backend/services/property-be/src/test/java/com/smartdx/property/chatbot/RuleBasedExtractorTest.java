package com.smartdx.property.chatbot;

import com.smartdx.property.chatbot.extractor.RuleBasedExtractor;
import com.smartdx.property.chatbot.model.ExtractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RuleBasedExtractor 単体テスト
 */
@DisplayName("RuleBasedExtractor 単体テスト")
class RuleBasedExtractorTest {

    private RuleBasedExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new RuleBasedExtractor();
    }

    @Test
    @DisplayName("単純なエリア条件を抽出できる")
    void extractArea() {
        ExtractionResult result = extractor.extract("渋谷でマンション探して");

        assertThat(result.getExtractedFields()).contains("area", "propertyType");
        assertThat(result.getCondition().getSearchItems().getArea())
                .containsExactly("tokyo-shibuya");
        assertThat(result.getCondition().getSearchItems().getPropertyType())
                .containsExactly("mansion");
        assertThat(result.getConfidence()).isEqualTo(ExtractionResult.ConfidenceLevel.HIGH);
    }

    @Test
    @DisplayName("複数エリアを抽出できる")
    void extractMultipleAreas() {
        ExtractionResult result = extractor.extract("渋谷か新宿でマンション");

        assertThat(result.getCondition().getSearchItems().getArea())
                .containsExactlyInAnyOrder("tokyo-shibuya", "tokyo-shinjuku");
    }

    @Test
    @DisplayName("価格上限を抽出できる")
    void extractPriceMax() {
        ExtractionResult result = extractor.extract("15万以下で探して");

        assertThat(result.getExtractedFields()).contains("priceJpyMax");
        assertThat(result.getCondition().getSearchItems().getPriceJpyMax())
                .isEqualTo(150000L);
    }

    @Test
    @DisplayName("価格範囲を抽出できる")
    void extractPriceRange() {
        ExtractionResult result = extractor.extract("10万〜20万で");

        assertThat(result.getCondition().getSearchItems().getPriceJpyMin())
                .isEqualTo(100000L);
        assertThat(result.getCondition().getSearchItems().getPriceJpyMax())
                .isEqualTo(200000L);
    }

    @Test
    @DisplayName("駅徒歩を抽出できる")
    void extractStationWalk() {
        ExtractionResult result = extractor.extract("駅徒歩10分以内");

        assertThat(result.getExtractedFields()).contains("stationWalkMax");
        assertThat(result.getCondition().getSearchItems().getStationWalkMax())
                .isEqualTo(10);
    }

    @Test
    @DisplayName("駅近を5分として抽出できる")
    void extractStationNear() {
        ExtractionResult result = extractor.extract("駅近のマンション");

        assertThat(result.getCondition().getSearchItems().getStationWalkMax())
                .isEqualTo(5);
    }

    @Test
    @DisplayName("間取りを抽出できる")
    void extractLayout() {
        ExtractionResult result = extractor.extract("2LDKで探して");

        assertThat(result.getExtractedFields()).contains("keyword");
        assertThat(result.getCondition().getKeyword()).isEqualTo("2LDK");
    }

    @Test
    @DisplayName("複合条件を抽出できる")
    void extractComplexConditions() {
        ExtractionResult result = extractor.extract("渋谷で2LDK、15万以下、駅徒歩10分以内");

        assertThat(result.getExtractedFields())
                .containsExactlyInAnyOrder("area", "keyword", "priceJpyMax", "stationWalkMax");
        assertThat(result.getCondition().getSearchItems().getArea())
                .containsExactly("tokyo-shibuya");
        assertThat(result.getCondition().getKeyword()).isEqualTo("2LDK");
        assertThat(result.getCondition().getSearchItems().getPriceJpyMax())
                .isEqualTo(150000L);
        assertThat(result.getCondition().getSearchItems().getStationWalkMax())
                .isEqualTo(10);
        assertThat(result.getConfidence()).isEqualTo(ExtractionResult.ConfidenceLevel.HIGH);
    }

    @Test
    @DisplayName("曖昧表現を検出できる")
    void detectAmbiguousExpressions() {
        ExtractionResult result = extractor.extract("都心で広めの部屋、なるべく安い");

        assertThat(result.isAmbiguousExpressionDetected()).isTrue();
        assertThat(result.getAmbiguousExpressions())
                .containsExactlyInAnyOrder("都心", "広め", "なるべく", "安い");
        assertThat(result.getConfidence()).isEqualTo(ExtractionResult.ConfidenceLevel.LOW);
    }

    @Test
    @DisplayName("曖昧表現ありで具体条件もある場合はPARTIAL")
    void partialConfidenceWithAmbiguous() {
        ExtractionResult result = extractor.extract("渋谷で安いマンション");

        assertThat(result.isAmbiguousExpressionDetected()).isTrue();
        assertThat(result.getExtractedFields()).contains("area", "propertyType");
        assertThat(result.getConfidence()).isEqualTo(ExtractionResult.ConfidenceLevel.PARTIAL);
    }

    @Test
    @DisplayName("空入力は0項目抽出")
    void emptyInput() {
        ExtractionResult result = extractor.extract("");

        assertThat(result.getExtractedFields()).isEmpty();
        assertThat(result.getConfidence()).isEqualTo(ExtractionResult.ConfidenceLevel.LOW);
    }

    @Test
    @DisplayName("全角数字を正規化して抽出できる")
    void normalizeFullWidthNumbers() {
        ExtractionResult result = extractor.extract("１５万以下");

        assertThat(result.getCondition().getSearchItems().getPriceJpyMax())
                .isEqualTo(150000L);
    }

    @Test
    @DisplayName("矛盾する価格範囲は無視される")
    void invalidPriceRangeIgnored() {
        ExtractionResult result = extractor.extract("20万以上で10万以下");

        // min > max なので両方nullになる
        assertThat(result.getCondition().getSearchItems().getPriceJpyMin()).isNull();
        assertThat(result.getCondition().getSearchItems().getPriceJpyMax()).isNull();
    }

    @Test
    @DisplayName("hasMinimumConditionが正しく判定される")
    void hasMinimumCondition() {
        // エリアあり
        ExtractionResult result1 = extractor.extract("渋谷");
        assertThat(result1.hasMinimumCondition()).isTrue();

        // 価格あり
        ExtractionResult result2 = extractor.extract("15万以下");
        assertThat(result2.hasMinimumCondition()).isTrue();

        // 物件種別あり
        ExtractionResult result3 = extractor.extract("マンション");
        assertThat(result3.hasMinimumCondition()).isTrue();

        // 駅徒歩あり
        ExtractionResult result4 = extractor.extract("駅徒歩10分以内");
        assertThat(result4.hasMinimumCondition()).isTrue();

        // keywordあり
        ExtractionResult result5 = extractor.extract("2LDK");
        assertThat(result5.hasMinimumCondition()).isTrue();

        // 何もなし
        ExtractionResult result6 = extractor.extract("物件探して");
        assertThat(result6.hasMinimumCondition()).isFalse();
    }
}
