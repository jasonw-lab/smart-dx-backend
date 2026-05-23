package com.smartdx.property.chatbot.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartdx.property.chatbot.extractor.RuleBasedExtractor;
import com.smartdx.property.chatbot.llm.*;
import com.smartdx.property.chatbot.model.ExtractionResult;
import com.smartdx.property.chatbot.model.req.ChatbotSearchReq;
import com.smartdx.property.chatbot.model.vo.ChatbotSearchVO;
import com.smartdx.property.chatbot.service.ChatbotSearchService;
import com.smartdx.property.chatbot.validator.ChatbotSearchCondition;
import com.smartdx.property.chatbot.validator.LlmOutputValidationException;
import com.smartdx.property.chatbot.validator.LlmOutputValidator;
import com.smartdx.property.model.req.PropertyLookupReq;
import com.smartdx.property.model.req.PropertySearchItemsReq;
import com.smartdx.property.model.vo.PropertySummaryVO;
import com.smartdx.property.service.PropertyEsSearchService;
import com.smartdx.property.service.PropertySearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Chatbot検索サービス実装
 *
 * LLM優先方式: LLMで入力分析 → 検索API呼び出し
 * LLM利用不可時は Rule-Based にフォールバック
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotSearchServiceImpl implements ChatbotSearchService {

    private final RuleBasedExtractor ruleBasedExtractor;
    private final LlmClient llmClient;
    private final LlmOutputValidator llmOutputValidator;
    private final PropertyEsSearchService propertyEsSearchService;
    private final PropertySearchService propertySearchService;

    @Value("${chatbot.llm.enabled:true}")
    private boolean llmEnabled;

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

    // Chatbot用有効なソート項目
    private static final Set<String> VALID_CHATBOT_SORT_BY = Set.of(
            "listedDate", "priceJpy", "stationWalkMin"
    );

    // エリアコードの日本語名マッピング
    private static final Map<String, String> AREA_DISPLAY_NAMES = Map.ofEntries(
            Map.entry("tokyo-shibuya", "渋谷区"),
            Map.entry("tokyo-shinjuku", "新宿区"),
            Map.entry("tokyo-minato", "港区"),
            Map.entry("tokyo-chiyoda", "千代田区"),
            Map.entry("tokyo-chuo", "中央区"),
            Map.entry("tokyo-meguro", "目黒区"),
            Map.entry("tokyo-setagaya", "世田谷区"),
            Map.entry("tokyo-ota", "大田区"),
            Map.entry("tokyo-shinagawa", "品川区"),
            Map.entry("osaka-kita", "大阪北区"),
            Map.entry("kanagawa-yokohama", "横浜市")
    );

    @Override
    public ChatbotSearchVO search(ChatbotSearchReq req) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.info("[{}] Chatbot search started: messageLength={}", traceId, req.getMessage().length());

        // 1. 入力サニタイズ
        String sanitizedMessage = sanitizeInput(req.getMessage());

        // 2. LLM優先で条件抽出
        PropertyLookupReq searchCondition;
        String extractionMethod;
        boolean llmUsed = false;
        String llmModel = null;

        if (llmEnabled && llmClient.isAvailable()) {
            // LLM で入力分析
            log.info("[{}] LLM extraction started", traceId);
            try {
                LlmExtractionResult llmResult = extractWithLlm(sanitizedMessage, req, traceId);
                searchCondition = llmResult.condition();
                extractionMethod = "llm";
                llmUsed = true;
                llmModel = llmResult.modelName();
                log.info("[{}] LLM extraction succeeded: model={}", traceId, llmModel);
            } catch (LlmException | LlmOutputValidationException e) {
                // LLM失敗 → Rule-Based にフォールバック
                log.warn("[{}] LLM extraction failed, falling back to rule-based: {}", traceId, e.getMessage());
                searchCondition = extractWithRuleBased(sanitizedMessage, traceId);
                extractionMethod = "rule_based_fallback";
            }
        } else {
            // LLM利用不可 → Rule-Based
            log.info("[{}] LLM not available, using rule-based extraction", traceId);
            searchCondition = extractWithRuleBased(sanitizedMessage, traceId);
            extractionMethod = "rule_based";
        }

        // 3. 抽出結果が空の場合は確認質問を返却
        if (isEmptyCondition(searchCondition)) {
            log.info("[{}] No conditions extracted, returning clarification request", traceId);
            return buildClarificationResponse();
        }

        // 4. previousCondition とマージ（LLM抽出で新しい条件が取れた場合はマージしない）
        // LLMで物件種別が抽出された場合、それは新しい検索意図なので前回条件は使わない
        if (req.getPreviousCondition() != null && !llmUsed) {
            searchCondition = mergeWithPrevious(searchCondition, req.getPreviousCondition());
        }

        // 5. 強制設定
        searchCondition.setScope("published");
        searchCondition.setPage(0);
        searchCondition.setSize(20);

        // 6. 検索実行
        IPage<PropertySummaryVO> results = executeSearch(searchCondition, traceId);

        // 7. 応答メッセージ生成
        String reply = generateReplyMessage(searchCondition, results.getTotal(), llmModel);

        log.info("[{}] Chatbot search completed: method={}, llmUsed={}, llmModel={}, resultCount={}",
                traceId, extractionMethod, llmUsed, llmModel, results.getTotal());

        // 8. レスポンス構築
        return ChatbotSearchVO.builder()
                .reply(reply)
                .extractedCondition(searchCondition)
                .extractionMethod(extractionMethod)
                .llmUsed(llmUsed)
                .llmModel(llmModel)
                .searchResults(ChatbotSearchVO.SearchResultPage.builder()
                        .list(results.getRecords())
                        .total(results.getTotal())
                        .build())
                .clarificationNeeded(false)
                .clarificationQuestion(null)
                .build();
    }

    /**
     * Rule-Based で条件抽出
     */
    private PropertyLookupReq extractWithRuleBased(String message, String traceId) {
        long extractionStart = System.currentTimeMillis();
        ExtractionResult result = ruleBasedExtractor.extract(message);
        long extractionTime = System.currentTimeMillis() - extractionStart;
        log.debug("[{}] Rule-based extraction completed: fields={}, time={}ms",
                traceId, result.getExtractedFields(), extractionTime);
        return result.getCondition();
    }

    /**
     * 抽出条件が空かどうか判定
     */
    private boolean isEmptyCondition(PropertyLookupReq condition) {
        if (condition == null) return true;
        if (condition.getKeyword() != null && !condition.getKeyword().isBlank()) return false;
        PropertySearchItemsReq items = condition.getSearchItems();
        if (items == null) return true;
        return (items.getArea() == null || items.getArea().isEmpty())
                && items.getPriceJpyMin() == null
                && items.getPriceJpyMax() == null
                && (items.getPropertyType() == null || items.getPropertyType().isEmpty())
                && items.getStationWalkMax() == null;
    }

    /**
     * LLM抽出結果（条件とモデル名を保持）
     */
    private record LlmExtractionResult(PropertyLookupReq condition, String modelName) {}

    /**
     * 入力サニタイズ
     */
    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        // 長さ制限（2000文字）
        if (input.length() > 2000) {
            input = input.substring(0, 2000);
        }
        return input.trim();
    }

    /**
     * LLMで条件抽出
     */
    private LlmExtractionResult extractWithLlm(String message, ChatbotSearchReq req, String traceId) {
        // システムプロンプト
        String systemPrompt = """
                あなたは不動産検索システムのアシスタントです。
                ユーザーの自然言語入力から検索条件を抽出し、JSONで出力してください。

                【重要】入力に誤字・タイプミス・変換ミスがある場合は、意図を推測して正しく解釈してください。
                例:
                - 「office部権」→「オフィス物件」の意図 → propertyType: ["office"]
                - 「まんしょん」「マンソン」→「マンション」→ propertyType: ["mansion"]
                - 「よこはま」「横濱」→「横浜」→ area: ["kanagawa-yokohama"]

                出力形式:
                {
                  "keyword": "間取り等のフリーワード（2LDK等）",
                  "searchItems": {
                    "area": ["tokyo-shibuya", "tokyo-shinjuku"],
                    "priceJpyMin": 50000000,
                    "priceJpyMax": 100000000,
                    "propertyType": ["mansion"],
                    "stationWalkMax": 10
                  },
                  "sortBy": "listedDate",
                  "orderBy": "desc"
                }

                有効なエリアコード:
                - tokyo-chiyoda (千代田区), tokyo-chuo (中央区), tokyo-minato (港区)
                - tokyo-shinjuku (新宿区), tokyo-shibuya (渋谷区), tokyo-meguro (目黒区)
                - tokyo-setagaya (世田谷区), tokyo-ota (大田区), tokyo-shinagawa (品川区)
                - osaka-kita (大阪北区/梅田)
                - kanagawa-yokohama (横浜市)

                有効な物件種別:
                - mansion (マンション), house (戸建て/一軒家)
                - office (オフィス/事務所), retail (店舗), land (土地)

                有効なsortBy: listedDate, priceJpy, stationWalkMin
                有効なorderBy: asc, desc

                曖昧な表現の解釈:
                - 「都心」→ tokyo-shibuya, tokyo-shinjuku, tokyo-minato, tokyo-chiyoda
                - 「駅近」「駅チカ」→ stationWalkMax: 5
                - 「安い」「安め」→ sortBy: priceJpy, orderBy: asc
                - 「広め」「広い」→ keyword に "広め" を設定

                価格の解釈（円単位で出力）:
                - 「1億」→ 100000000
                - 「5000万」→ 50000000
                - 「3000万以下」→ priceJpyMax: 30000000

                抽出できた条件のみ出力してください。不明な項目は省略してください。
                JSONのみ出力してください。説明文は不要です。
                """;

        // ユーザープロンプト
        String userPrompt = "検索条件を抽出してください: " + message;

        // 会話履歴の構築
        List<ConversationMessage> conversationHistory = new ArrayList<>();
        if (req.getConversationHistory() != null) {
            for (var item : req.getConversationHistory()) {
                conversationHistory.add(new ConversationMessage(item.getRole(), item.getContent()));
            }
        }

        LlmRequest llmRequest = LlmRequest.builder()
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .conversationHistory(conversationHistory)
                .maxOutputTokens(500)
                .temperature(0.1)
                .timeoutMs(3000)
                .build();

        log.info("[{}] LLM request started: provider={}", traceId, llmClient.getProviderName());
        long llmStart = System.currentTimeMillis();

        LlmResponse llmResponse = llmClient.complete(llmRequest);

        long llmTime = System.currentTimeMillis() - llmStart;
        log.info("[{}] LLM response received: time={}ms, model={}, inputTokens={}, outputTokens={}",
                traceId, llmTime, llmResponse.getModel(), llmResponse.getInputTokens(), llmResponse.getOutputTokens());

        // バリデーション & 変換
        ChatbotSearchCondition condition = llmOutputValidator.validateAndParse(llmResponse.getContent());
        PropertyLookupReq lookupReq = llmOutputValidator.toPropertyLookupReq(condition);
        return new LlmExtractionResult(lookupReq, llmResponse.getModel());
    }

    /**
     * previousCondition とマージ（セキュリティ検証付き）
     * 新しく抽出された条件を優先
     */
    private PropertyLookupReq mergeWithPrevious(PropertyLookupReq current, PropertyLookupReq previous) {
        PropertyLookupReq merged = new PropertyLookupReq();

        // scope, page, size はサーバー側で強制設定（権限昇格防止）
        merged.setScope("published");
        merged.setPage(0);
        merged.setSize(20);

        // 許可フィールドのみマージ（現在の条件を優先）
        merged.setKeyword(current.getKeyword() != null ? current.getKeyword() :
                sanitizeKeyword(previous.getKeyword()));
        merged.setSortBy(current.getSortBy() != null ? current.getSortBy() :
                validateSortBy(previous.getSortBy()));
        merged.setOrderBy(current.getOrderBy() != null ? current.getOrderBy() :
                validateOrderBy(previous.getOrderBy()));

        PropertySearchItemsReq mergedItems = new PropertySearchItemsReq();
        PropertySearchItemsReq currentItems = current.getSearchItems();
        PropertySearchItemsReq prevItems = sanitizePreviousSearchItems(previous.getSearchItems());

        if (currentItems != null && prevItems != null) {
            mergedItems.setArea(
                    currentItems.getArea() != null ? currentItems.getArea() : prevItems.getArea());
            mergedItems.setPriceJpyMin(
                    currentItems.getPriceJpyMin() != null ? currentItems.getPriceJpyMin() : prevItems.getPriceJpyMin());
            mergedItems.setPriceJpyMax(
                    currentItems.getPriceJpyMax() != null ? currentItems.getPriceJpyMax() : prevItems.getPriceJpyMax());
            mergedItems.setPropertyType(
                    currentItems.getPropertyType() != null ? currentItems.getPropertyType() : prevItems.getPropertyType());
            mergedItems.setStationWalkMax(
                    currentItems.getStationWalkMax() != null ? currentItems.getStationWalkMax() : prevItems.getStationWalkMax());
        } else if (currentItems != null) {
            mergedItems = currentItems;
        } else if (prevItems != null) {
            mergedItems = prevItems;
        }

        // priceJpyMin ≤ priceJpyMax の検証
        if (mergedItems.getPriceJpyMin() != null && mergedItems.getPriceJpyMax() != null) {
            if (mergedItems.getPriceJpyMin() > mergedItems.getPriceJpyMax()) {
                // 不正な範囲は無視
                mergedItems.setPriceJpyMin(null);
                mergedItems.setPriceJpyMax(null);
            }
        }

        merged.setSearchItems(mergedItems);
        return merged;
    }

    /**
     * previousCondition.searchItems から許可フィールドのみ抽出
     */
    private PropertySearchItemsReq sanitizePreviousSearchItems(PropertySearchItemsReq prev) {
        if (prev == null) {
            return null;
        }
        PropertySearchItemsReq sanitized = new PropertySearchItemsReq();
        sanitized.setArea(validateAreas(prev.getArea()));
        sanitized.setPriceJpyMin(validatePrice(prev.getPriceJpyMin()));
        sanitized.setPriceJpyMax(validatePrice(prev.getPriceJpyMax()));
        sanitized.setPropertyType(validatePropertyTypes(prev.getPropertyType()));
        sanitized.setStationWalkMax(validateStationWalk(prev.getStationWalkMax()));
        // priorityRank, reviewStatus, registered*, *Id は意図的に無視
        return sanitized;
    }

    private List<String> validateAreas(List<String> areas) {
        if (areas == null) return null;
        List<String> valid = areas.stream()
                .filter(VALID_AREAS::contains)
                .collect(Collectors.toList());
        return valid.isEmpty() ? null : valid;
    }

    private List<String> validatePropertyTypes(List<String> types) {
        if (types == null) return null;
        List<String> valid = types.stream()
                .filter(VALID_PROPERTY_TYPES::contains)
                .collect(Collectors.toList());
        return valid.isEmpty() ? null : valid;
    }

    private Long validatePrice(Long price) {
        if (price == null) return null;
        if (price < 0 || price > 10_000_000_000L) return null;
        return price;
    }

    private Integer validateStationWalk(Integer walk) {
        if (walk == null) return null;
        if (walk < 1 || walk > 60) return null;
        return walk;
    }

    private String validateSortBy(String sortBy) {
        if (sortBy == null) return "listedDate";
        return VALID_CHATBOT_SORT_BY.contains(sortBy) ? sortBy : "listedDate";
    }

    private String validateOrderBy(String orderBy) {
        if (orderBy == null) return "desc";
        return "asc".equals(orderBy) || "desc".equals(orderBy) ? orderBy : "desc";
    }

    private String sanitizeKeyword(String keyword) {
        if (keyword == null) return null;
        if (keyword.length() > 200) return keyword.substring(0, 200);
        return keyword;
    }

    /**
     * 検索実行（OpenSearch → DB fallback）
     */
    private IPage<PropertySummaryVO> executeSearch(PropertyLookupReq condition, String traceId) {
        try {
            log.debug("[{}] Executing OpenSearch query", traceId);
            return propertyEsSearchService.lookup(condition);
        } catch (RuntimeException e) {
            log.warn("[{}] OpenSearch search failed, falling back to DB: {}", traceId, e.getMessage());
            return propertySearchService.lookup(condition);
        }
    }

    /**
     * 応答メッセージ生成
     *
     * @param condition 検索条件
     * @param total 検索結果件数
     * @param llmModel LLMモデル名（LLM使用時のみ、未使用時はnull）
     */
    private String generateReplyMessage(PropertyLookupReq condition, long total, String llmModel) {
        StringBuilder sb = new StringBuilder();

        // 検索条件の日本語化
        List<String> conditionParts = new ArrayList<>();

        var items = condition.getSearchItems();
        if (items != null) {
            // エリア
            if (items.getArea() != null && !items.getArea().isEmpty()) {
                String areaNames = items.getArea().stream()
                        .map(code -> AREA_DISPLAY_NAMES.getOrDefault(code, code))
                        .collect(Collectors.joining("、"));
                conditionParts.add(areaNames + "エリア");
            }

            // 物件種別
            if (items.getPropertyType() != null && !items.getPropertyType().isEmpty()) {
                String types = items.getPropertyType().stream()
                        .map(this::getPropertyTypeDisplayName)
                        .collect(Collectors.joining("・"));
                conditionParts.add(types);
            }

            // 価格
            if (items.getPriceJpyMax() != null) {
                conditionParts.add(formatPrice(items.getPriceJpyMax()) + "以下");
            } else if (items.getPriceJpyMin() != null) {
                conditionParts.add(formatPrice(items.getPriceJpyMin()) + "以上");
            }

            // 駅徒歩
            if (items.getStationWalkMax() != null) {
                conditionParts.add("駅徒歩" + items.getStationWalkMax() + "分以内");
            }
        }

        // キーワード（間取り等）
        if (condition.getKeyword() != null && !condition.getKeyword().isBlank()) {
            conditionParts.add(condition.getKeyword());
        }

        if (conditionParts.isEmpty()) {
            sb.append("検索しました。");
        } else {
            sb.append(String.join("、", conditionParts));
            sb.append("で検索しました。");
        }

        sb.append(total).append("件見つかりました。");

        // LLMモデル名を追記
        if (llmModel != null && !llmModel.isBlank()) {
            sb.append("(").append(llmModel).append(")");
        }

        return sb.toString();
    }

    /**
     * 価格フォーマット
     */
    private String formatPrice(Long price) {
        if (price == null) return "";
        if (price >= 100_000_000) {
            return (price / 100_000_000) + "億円";
        } else if (price >= 10000) {
            return (price / 10000) + "万円";
        } else {
            return price + "円";
        }
    }

    /**
     * 物件種別の日本語名取得
     */
    private String getPropertyTypeDisplayName(String code) {
        return switch (code) {
            case "mansion" -> "マンション";
            case "house" -> "戸建て";
            case "office" -> "オフィス";
            case "retail" -> "店舗";
            case "land" -> "土地";
            default -> code;
        };
    }

    /**
     * 確認質問レスポンス生成
     */
    private ChatbotSearchVO buildClarificationResponse() {
        return ChatbotSearchVO.builder()
                .reply("検索条件を教えてください。例えば「渋谷で2LDK、15万以下」のように、エリア・間取り・価格などをお伝えください。")
                .extractedCondition(null)
                .extractionMethod("none")
                .llmUsed(false)
                .searchResults(null)
                .clarificationNeeded(true)
                .clarificationQuestion("どのエリア、価格帯、物件種別をお探しですか？")
                .build();
    }
}
