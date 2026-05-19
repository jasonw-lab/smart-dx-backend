package com.smartdx.property.chatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdx.property.chatbot.validator.ChatbotSearchCondition;
import com.smartdx.property.chatbot.validator.LlmOutputValidationException;
import com.smartdx.property.chatbot.validator.LlmOutputValidator;
import com.smartdx.property.model.req.PropertyLookupReq;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LlmOutputValidator 単体テスト
 */
@DisplayName("LlmOutputValidator 単体テスト")
class LlmOutputValidatorTest {

    private LlmOutputValidator validator;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        Validator jakartaValidator = Validation.buildDefaultValidatorFactory().getValidator();
        validator = new LlmOutputValidator(objectMapper, jakartaValidator);
    }

    @Test
    @DisplayName("有効なJSONをパースできる")
    void parseValidJson() {
        String json = """
                {
                  "keyword": "2LDK",
                  "searchItems": {
                    "area": ["tokyo-shibuya"],
                    "priceJpyMax": 150000,
                    "propertyType": ["mansion"]
                  },
                  "sortBy": "priceJpy",
                  "orderBy": "asc"
                }
                """;

        ChatbotSearchCondition condition = validator.validateAndParse(json);

        assertThat(condition.getKeyword()).isEqualTo("2LDK");
        assertThat(condition.getSearchItems().getArea()).containsExactly("tokyo-shibuya");
        assertThat(condition.getSearchItems().getPriceJpyMax()).isEqualTo(150000L);
        assertThat(condition.getSearchItems().getPropertyType()).containsExactly("mansion");
        assertThat(condition.getSortBy()).isEqualTo("priceJpy");
        assertThat(condition.getOrderBy()).isEqualTo("asc");
    }

    @Test
    @DisplayName("前後にテキストがあるJSONを抽出できる")
    void extractJsonFromText() {
        String output = """
                検索条件を抽出しました:
                {
                  "searchItems": {
                    "area": ["tokyo-shinjuku"]
                  }
                }
                以上です。
                """;

        ChatbotSearchCondition condition = validator.validateAndParse(output);

        assertThat(condition.getSearchItems().getArea()).containsExactly("tokyo-shinjuku");
    }

    @Test
    @DisplayName("未知のフィールドは無視される")
    void ignoreUnknownFields() {
        String json = """
                {
                  "keyword": "2LDK",
                  "unknownField": "ignored",
                  "searchItems": {
                    "area": ["tokyo-shibuya"],
                    "anotherUnknown": 12345
                  }
                }
                """;

        ChatbotSearchCondition condition = validator.validateAndParse(json);

        assertThat(condition.getKeyword()).isEqualTo("2LDK");
        assertThat(condition.getSearchItems().getArea()).containsExactly("tokyo-shibuya");
    }

    @Test
    @DisplayName("無効なエリアコードは除去される")
    void filterInvalidAreaCodes() {
        String json = """
                {
                  "searchItems": {
                    "area": ["tokyo-shibuya", "invalid-area", "tokyo-shinjuku"]
                  }
                }
                """;

        ChatbotSearchCondition condition = validator.validateAndParse(json);

        assertThat(condition.getSearchItems().getArea())
                .containsExactlyInAnyOrder("tokyo-shibuya", "tokyo-shinjuku");
    }

    @Test
    @DisplayName("無効な物件種別は除去される")
    void filterInvalidPropertyTypes() {
        String json = """
                {
                  "searchItems": {
                    "propertyType": ["mansion", "invalid-type", "house"]
                  }
                }
                """;

        ChatbotSearchCondition condition = validator.validateAndParse(json);

        assertThat(condition.getSearchItems().getPropertyType())
                .containsExactlyInAnyOrder("mansion", "house");
    }

    @Test
    @DisplayName("矛盾する価格範囲はクリアされる")
    void clearInvalidPriceRange() {
        String json = """
                {
                  "searchItems": {
                    "priceJpyMin": 200000,
                    "priceJpyMax": 100000
                  }
                }
                """;

        ChatbotSearchCondition condition = validator.validateAndParse(json);

        assertThat(condition.getSearchItems().getPriceJpyMin()).isNull();
        assertThat(condition.getSearchItems().getPriceJpyMax()).isNull();
    }

    @Test
    @DisplayName("JSONがない場合は例外")
    void throwExceptionWhenNoJson() {
        String output = "検索条件が見つかりませんでした";

        assertThatThrownBy(() -> validator.validateAndParse(output))
                .isInstanceOf(LlmOutputValidationException.class)
                .hasMessageContaining("No JSON object found");
    }

    @Test
    @DisplayName("空入力は例外")
    void throwExceptionWhenEmpty() {
        assertThatThrownBy(() -> validator.validateAndParse(""))
                .isInstanceOf(LlmOutputValidationException.class);
    }

    @Test
    @DisplayName("PropertyLookupReqに変換できる")
    void toPropertyLookupReq() {
        String json = """
                {
                  "keyword": "2LDK",
                  "searchItems": {
                    "area": ["tokyo-shibuya"],
                    "priceJpyMax": 150000
                  },
                  "sortBy": "priceJpy",
                  "orderBy": "asc"
                }
                """;

        ChatbotSearchCondition condition = validator.validateAndParse(json);
        PropertyLookupReq req = validator.toPropertyLookupReq(condition);

        assertThat(req.getScope()).isEqualTo("published"); // 強制
        assertThat(req.getPage()).isEqualTo(0); // 強制
        assertThat(req.getSize()).isEqualTo(20); // 強制
        assertThat(req.getKeyword()).isEqualTo("2LDK");
        assertThat(req.getSearchItems().getArea()).containsExactly("tokyo-shibuya");
        assertThat(req.getSearchItems().getPriceJpyMax()).isEqualTo(150000L);
        assertThat(req.getSortBy()).isEqualTo("priceJpy");
        assertThat(req.getOrderBy()).isEqualTo("asc");
    }

    @Test
    @DisplayName("空のJSONでも変換できる")
    void toPropertyLookupReqWithEmptyJson() {
        String json = "{}";

        ChatbotSearchCondition condition = validator.validateAndParse(json);
        PropertyLookupReq req = validator.toPropertyLookupReq(condition);

        assertThat(req.getScope()).isEqualTo("published");
        assertThat(req.getSortBy()).isEqualTo("listedDate"); // デフォルト
        assertThat(req.getOrderBy()).isEqualTo("desc"); // デフォルト
    }
}
