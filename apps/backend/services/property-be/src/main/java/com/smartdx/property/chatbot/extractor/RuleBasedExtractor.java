package com.smartdx.property.chatbot.extractor;

import com.smartdx.property.chatbot.model.ExtractionResult;
import com.smartdx.property.model.req.PropertyLookupReq;
import com.smartdx.property.model.req.PropertySearchItemsReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule-Based条件抽出器
 *
 * 正規表現・キーワードマッチングで検索条件を抽出する
 */
@Component
@Slf4j
public class RuleBasedExtractor {

    // ===== エリアコードマッピング =====
    private static final Map<String, String> AREA_MAP = new LinkedHashMap<>();

    static {
        // 東京都
        AREA_MAP.put("渋谷", "tokyo-shibuya");
        AREA_MAP.put("しぶや", "tokyo-shibuya");
        AREA_MAP.put("シブヤ", "tokyo-shibuya");
        AREA_MAP.put("新宿", "tokyo-shinjuku");
        AREA_MAP.put("しんじゅく", "tokyo-shinjuku");
        AREA_MAP.put("シンジュク", "tokyo-shinjuku");
        AREA_MAP.put("港区", "tokyo-minato");
        AREA_MAP.put("みなと", "tokyo-minato");
        AREA_MAP.put("千代田", "tokyo-chiyoda");
        AREA_MAP.put("中央区", "tokyo-chuo");
        AREA_MAP.put("目黒", "tokyo-meguro");
        AREA_MAP.put("世田谷", "tokyo-setagaya");
        AREA_MAP.put("大田区", "tokyo-ota");
        AREA_MAP.put("品川", "tokyo-shinagawa");
        // 大阪
        AREA_MAP.put("大阪", "osaka-kita");
        AREA_MAP.put("梅田", "osaka-kita");
        AREA_MAP.put("北区", "osaka-kita");
        // 神奈川
        AREA_MAP.put("横浜", "kanagawa-yokohama");
        AREA_MAP.put("よこはま", "kanagawa-yokohama");
        AREA_MAP.put("ヨコハマ", "kanagawa-yokohama");
    }

    // ===== 物件種別マッピング =====
    private static final Map<String, String> PROPERTY_TYPE_MAP = new LinkedHashMap<>();

    static {
        PROPERTY_TYPE_MAP.put("マンション", "mansion");
        PROPERTY_TYPE_MAP.put("戸建て", "house");
        PROPERTY_TYPE_MAP.put("戸建", "house");
        PROPERTY_TYPE_MAP.put("一戸建て", "house");
        PROPERTY_TYPE_MAP.put("一軒家", "house");
        PROPERTY_TYPE_MAP.put("オフィス", "office");
        PROPERTY_TYPE_MAP.put("事務所", "office");
        PROPERTY_TYPE_MAP.put("店舗", "retail");
        PROPERTY_TYPE_MAP.put("土地", "land");
        // 英語表記
        PROPERTY_TYPE_MAP.put("office", "office");
        PROPERTY_TYPE_MAP.put("mansion", "mansion");
        PROPERTY_TYPE_MAP.put("house", "house");
        PROPERTY_TYPE_MAP.put("retail", "retail");
        PROPERTY_TYPE_MAP.put("land", "land");
    }

    // ===== 正規表現パターン =====

    // 価格パターン: XX万円以下、XX万以下、XX万円、XX万
    private static final Pattern PRICE_MAX_PATTERN = Pattern.compile(
            "([0-9０-９一二三四五六七八九十百千万億]+)\\s*万\\s*円?\\s*(以下|まで)",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    // 価格パターン（億単位）: X億以下、X億円以下
    private static final Pattern PRICE_MAX_OKU_PATTERN = Pattern.compile(
            "([0-9０-９一二三四五六七八九十]+)\\s*億\\s*円?\\s*(以下|まで)",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    // 価格範囲パターン: XX万〜YY万、XX万円〜YY万円
    private static final Pattern PRICE_RANGE_PATTERN = Pattern.compile(
            "([0-9０-９一二三四五六七八九十百千万億]+)\\s*万\\s*円?\\s*[〜~～からー]\\s*([0-9０-９一二三四五六七八九十百千万億]+)\\s*万",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    // 価格以上パターン: XX万以上、XX万円以上
    private static final Pattern PRICE_MIN_PATTERN = Pattern.compile(
            "([0-9０-９一二三四五六七八九十百千万億]+)\\s*万\\s*円?\\s*以上",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    // 価格以上パターン（億単位）: X億以上、X億円以上
    private static final Pattern PRICE_MIN_OKU_PATTERN = Pattern.compile(
            "([0-9０-９一二三四五六七八九十]+)\\s*億\\s*円?\\s*以上",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    // 駅徒歩パターン: 駅徒歩XX分以内、駅からXX分、駅近
    private static final Pattern STATION_WALK_PATTERN = Pattern.compile(
            "駅\\s*(徒歩|から)?\\s*([0-9０-９]+)\\s*分\\s*(以内)?",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final Pattern STATION_NEAR_PATTERN = Pattern.compile("駅近|駅チカ");

    // 間取りパターン: 1LDK, 2LDK, 3LDK, 1K, 2K, etc.
    private static final Pattern LAYOUT_PATTERN = Pattern.compile(
            "([1-5１-５][LDKSLDKS]+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // ===== 曖昧表現パターン =====
    private static final List<Pattern> AMBIGUOUS_PATTERNS = List.of(
            // 相対的エリア表現
            Pattern.compile("都心|郊外|山手線沿線|城南|城西|城北|城東|環状線内"),
            // 相対的価格表現
            Pattern.compile("安い|安め|高い|高め|手頃|リーズナブル|お手頃|格安|激安"),
            // 相対的広さ表現
            Pattern.compile("広い|広め|狭い|狭め|コンパクト|ゆったり|ファミリー向け"),
            // 優先度表現
            Pattern.compile("なるべく|できれば|できるだけ|優先|重視"),
            // 否定価格表現
            Pattern.compile("高すぎない|高くない|安すぎない"),
            // 否定広さ表現
            Pattern.compile("狭すぎない|広すぎない")
    );

    // ===== 不明瞭入力検出パターン =====
    // 「物件」の誤入力・変換ミスパターン
    private static final Pattern GARBLED_BUKKEN_PATTERN = Pattern.compile(
            "部権|部県|武権|ぶっけん|ブッケン|bukken",
            Pattern.CASE_INSENSITIVE
    );

    // 物件種別を示唆するが認識されなかったパターン
    private static final Pattern UNRECOGNIZED_TYPE_HINT_PATTERN = Pattern.compile(
            "物件|ぶっけん|プロパティ|property|不動産",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 自然文から検索条件を抽出
     *
     * @param message ユーザー入力
     * @return 抽出結果
     */
    public ExtractionResult extract(String message) {
        if (message == null || message.isBlank()) {
            return ExtractionResult.builder()
                    .condition(new PropertyLookupReq())
                    .extractedFields(Collections.emptyList())
                    .ambiguousExpressionDetected(false)
                    .ambiguousExpressions(Collections.emptyList())
                    .confidence(ExtractionResult.ConfidenceLevel.LOW)
                    .build();
        }

        // 正規化: 全角数字→半角
        String normalized = normalizeInput(message);

        PropertyLookupReq condition = new PropertyLookupReq();
        PropertySearchItemsReq searchItems = new PropertySearchItemsReq();
        List<String> extractedFields = new ArrayList<>();

        // エリア抽出
        List<String> areas = extractAreas(normalized);
        if (!areas.isEmpty()) {
            searchItems.setArea(areas);
            extractedFields.add("area");
        }

        // 物件種別抽出
        List<String> propertyTypes = extractPropertyTypes(normalized);
        if (!propertyTypes.isEmpty()) {
            searchItems.setPropertyType(propertyTypes);
            extractedFields.add("propertyType");
        }

        // 価格抽出
        Long[] priceRange = extractPriceRange(normalized);
        if (priceRange[0] != null) {
            searchItems.setPriceJpyMin(priceRange[0]);
            extractedFields.add("priceJpyMin");
        }
        if (priceRange[1] != null) {
            searchItems.setPriceJpyMax(priceRange[1]);
            extractedFields.add("priceJpyMax");
        }

        // 駅徒歩抽出
        Integer stationWalk = extractStationWalk(normalized);
        if (stationWalk != null) {
            searchItems.setStationWalkMax(stationWalk);
            extractedFields.add("stationWalkMax");
        }

        // 間取り抽出 (keyword として設定)
        String layout = extractLayout(normalized);
        if (layout != null) {
            condition.setKeyword(layout);
            extractedFields.add("keyword");
        }

        condition.setSearchItems(searchItems);

        // 曖昧表現検出
        List<String> ambiguousExpressions = detectAmbiguousExpressions(message);
        boolean ambiguousDetected = !ambiguousExpressions.isEmpty();

        // 信頼度判定
        ExtractionResult.ConfidenceLevel confidence;
        if (extractedFields.isEmpty()) {
            confidence = ExtractionResult.ConfidenceLevel.LOW;
        } else if (ambiguousDetected) {
            confidence = ExtractionResult.ConfidenceLevel.PARTIAL;
        } else {
            confidence = ExtractionResult.ConfidenceLevel.HIGH;
        }

        log.debug("Rule-based extraction completed: fields={}, ambiguous={}, confidence={}",
                extractedFields, ambiguousExpressions, confidence);

        return ExtractionResult.builder()
                .condition(condition)
                .extractedFields(extractedFields)
                .ambiguousExpressionDetected(ambiguousDetected)
                .ambiguousExpressions(ambiguousExpressions)
                .confidence(confidence)
                .build();
    }

    /**
     * 入力を正規化（全角数字→半角など）
     */
    private String normalizeInput(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            // 全角数字→半角
            if (c >= '０' && c <= '９') {
                sb.append((char) (c - '０' + '0'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * エリア抽出
     */
    private List<String> extractAreas(String message) {
        Set<String> areas = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : AREA_MAP.entrySet()) {
            if (message.contains(entry.getKey())) {
                areas.add(entry.getValue());
            }
        }
        return new ArrayList<>(areas);
    }

    /**
     * 物件種別抽出
     */
    private List<String> extractPropertyTypes(String message) {
        Set<String> types = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : PROPERTY_TYPE_MAP.entrySet()) {
            if (message.contains(entry.getKey())) {
                types.add(entry.getValue());
            }
        }
        return new ArrayList<>(types);
    }

    /**
     * 価格抽出（min, max）
     */
    private Long[] extractPriceRange(String message) {
        Long min = null;
        Long max = null;

        // 価格範囲パターン: XX万〜YY万
        Matcher rangeMatcher = PRICE_RANGE_PATTERN.matcher(message);
        if (rangeMatcher.find()) {
            min = parseJapaneseNumber(rangeMatcher.group(1)) * 10000;
            max = parseJapaneseNumber(rangeMatcher.group(2)) * 10000;
            return new Long[]{min, max};
        }

        // 価格上限パターン（億単位）: X億以下
        Matcher maxOkuMatcher = PRICE_MAX_OKU_PATTERN.matcher(message);
        if (maxOkuMatcher.find()) {
            max = parseJapaneseNumber(maxOkuMatcher.group(1)) * 100_000_000;
        }

        // 価格上限パターン: XX万以下 (億単位が未マッチの場合のみ)
        if (max == null) {
            Matcher maxMatcher = PRICE_MAX_PATTERN.matcher(message);
            if (maxMatcher.find()) {
                max = parseJapaneseNumber(maxMatcher.group(1)) * 10000;
            }
        }

        // 価格下限パターン（億単位）: X億以上
        Matcher minOkuMatcher = PRICE_MIN_OKU_PATTERN.matcher(message);
        if (minOkuMatcher.find()) {
            min = parseJapaneseNumber(minOkuMatcher.group(1)) * 100_000_000;
        }

        // 価格下限パターン: XX万以上 (億単位が未マッチの場合のみ)
        if (min == null) {
            Matcher minMatcher = PRICE_MIN_PATTERN.matcher(message);
            if (minMatcher.find()) {
                min = parseJapaneseNumber(minMatcher.group(1)) * 10000;
            }
        }

        // 矛盾検証: min > max の場合は両方無視
        if (min != null && max != null && min > max) {
            log.warn("Price range invalid: min={} > max={}", min, max);
            return new Long[]{null, null};
        }

        return new Long[]{min, max};
    }

    /**
     * 日本語数字を数値に変換
     */
    private long parseJapaneseNumber(String str) {
        if (str == null || str.isBlank()) {
            return 0;
        }
        // まず漢数字変換を試行
        String converted = convertKanjiToNumber(str);
        try {
            return Long.parseLong(converted);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 漢数字→アラビア数字変換（簡易版）
     */
    private String convertKanjiToNumber(String str) {
        String result = str;
        // 単純な漢数字置換
        result = result.replace("一", "1").replace("二", "2").replace("三", "3")
                .replace("四", "4").replace("五", "5").replace("六", "6")
                .replace("七", "7").replace("八", "8").replace("九", "9")
                .replace("十", "10").replace("百", "00").replace("千", "000");
        // より複雑な変換は必要に応じて実装
        return result;
    }

    /**
     * 駅徒歩抽出
     */
    private Integer extractStationWalk(String message) {
        // 駅近パターン
        if (STATION_NEAR_PATTERN.matcher(message).find()) {
            return 5;
        }

        // 駅徒歩XX分パターン
        Matcher matcher = STATION_WALK_PATTERN.matcher(message);
        if (matcher.find()) {
            try {
                int minutes = Integer.parseInt(matcher.group(2));
                // 範囲制限: 1-60
                if (minutes >= 1 && minutes <= 60) {
                    return minutes;
                }
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return null;
    }

    /**
     * 間取り抽出
     */
    private String extractLayout(String message) {
        Matcher matcher = LAYOUT_PATTERN.matcher(message);
        if (matcher.find()) {
            // 全角→半角変換して大文字化
            return matcher.group(1).toUpperCase()
                    .replace('Ｌ', 'L').replace('Ｄ', 'D')
                    .replace('Ｋ', 'K').replace('Ｓ', 'S');
        }
        return null;
    }

    /**
     * 曖昧表現検出
     */
    private List<String> detectAmbiguousExpressions(String message) {
        List<String> detected = new ArrayList<>();
        for (Pattern pattern : AMBIGUOUS_PATTERNS) {
            Matcher matcher = pattern.matcher(message);
            while (matcher.find()) {
                detected.add(matcher.group());
            }
        }
        return detected;
    }
}
