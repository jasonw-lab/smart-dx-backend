package com.smartdx.property.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdx.core.exception.BusinessException;
import com.smartdx.property.file.MinioFileService;
import com.smartdx.property.config.PropertyOpenSearchProperties;
import com.smartdx.property.exception.PropertyErrorCode;
import com.smartdx.property.mapper.PropertyDemoEmbeddingMapper;
import com.smartdx.property.model.entity.PropertyDemoEmbedding;
import com.smartdx.property.model.req.SimilarPropertyReq;
import com.smartdx.property.model.vo.PropertySummaryVO;
import com.smartdx.property.model.vo.SimilarPropertyReferenceVO;
import com.smartdx.property.model.vo.SimilarPropertyResponse;
import com.smartdx.property.service.SimilarPropertyService;
import com.smartdx.property.support.PropertySearchSupport;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.KnnQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * LST-SIM-01: 類似物件検索サービス実装（v2.0.0）
 * 複数検索起点対応: propertyId, imageId, demoRef
 */
@Service
@Slf4j
public class SimilarPropertyServiceImpl implements SimilarPropertyService {

    private static final String FEATURE_VECTOR_FIELD = "feature_vector";

    private final PropertyOpenSearchProperties properties;
    private final OpenSearchClient client;
    private final ObjectProvider<MinioFileService> minioFileServiceProvider;
    private final PropertyDemoEmbeddingMapper demoEmbeddingMapper;
    private final ObjectMapper objectMapper;

    public SimilarPropertyServiceImpl(
            PropertyOpenSearchProperties properties,
            @Autowired(required = false) OpenSearchClient client,
            ObjectProvider<MinioFileService> minioFileServiceProvider,
            @Autowired(required = false) PropertyDemoEmbeddingMapper demoEmbeddingMapper,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.client = client;
        this.minioFileServiceProvider = minioFileServiceProvider;
        this.demoEmbeddingMapper = demoEmbeddingMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public SimilarPropertyResponse findSimilar(SimilarPropertyReq req) {
        // ADR-011: OpenSearch 専用戦略 - OpenSearch が利用不可の場合はエラー
        if (!properties.isEnabled()) {
            log.warn("OpenSearch property search is disabled. Similar search is unavailable.");
            throw new BusinessException(PropertyErrorCode.OPENSEARCH_UNAVAILABLE,
                    "類似検索サービスは現在利用できません（OpenSearch無効）");
        }
        if (client == null) {
            log.warn("OpenSearch client is not configured. Similar search is unavailable.");
            throw new BusinessException(PropertyErrorCode.OPENSEARCH_UNAVAILABLE,
                    "類似検索サービスは現在利用できません（クライアント未設定）");
        }
        if (!req.hasSearchOrigin()) {
            throw new BusinessException(PropertyErrorCode.NO_SEARCH_ORIGIN, "検索条件を指定してください");
        }

        try {
            return doFindSimilar(req);
        } catch (IOException e) {
            log.error("OpenSearch similar property search failed. req={}", req, e);
            throw new BusinessException(PropertyErrorCode.OPENSEARCH_UNAVAILABLE,
                    "類似検索サービスで通信エラーが発生しました");
        }
    }

    private SimilarPropertyResponse doFindSimilar(SimilarPropertyReq req) throws IOException {
        // 1. 検索起点からvectorとreference情報を取得
        VectorSource source = resolveVectorSource(req);

        // 2. k-NN 検索を実行
        int k = req.getExcludeSameProperty() && req.getPropertyId() != null
                ? req.getLimit() + 1  // propertyId起点で自己除外の場合は+1
                : req.getLimit();
        List<PropertySummaryVO> similarProperties = executeKnnSearch(source.vector(), k, null);

        // 3. minScore 適用 & 自己除外
        List<PropertySummaryVO> filteredResults = similarProperties.stream()
                .filter(p -> {
                    // propertyId起点の場合、excludeSamePropertyがtrueなら自己除外
                    if (req.getExcludeSameProperty() && req.getPropertyId() != null) {
                        return !req.getPropertyId().equals(p.getPropertyKey());
                    }
                    return true;
                })
                .filter(p -> p.getSimilarity() != null && p.getSimilarity() >= req.getMinScore())
                .limit(req.getLimit())
                .toList();

        // 4. レスポンス構築
        SimilarPropertyResponse response = new SimilarPropertyResponse();
        response.setReference(source.reference());
        response.setTotal(filteredResults.size());
        response.setList(filteredResults);

        return response;
    }

    /**
     * 検索起点からvectorとreference情報を解決
     * 優先順位: propertyId > imageId > demoRef
     */
    private VectorSource resolveVectorSource(SimilarPropertyReq req) throws IOException {
        if (req.getPropertyId() != null) {
            return resolveFromProperty(req.getPropertyId());
        } else if (req.getImageId() != null) {
            // Phase 2: 画像IDからの検索は未実装
            throw new BusinessException(PropertyErrorCode.IMAGE_NOT_FOUND, "画像検索は Phase 2 で実装予定です");
        } else if (req.getDemoRef() != null) {
            return resolveFromDemoRef(req.getDemoRef());
        }
        throw new BusinessException(PropertyErrorCode.NO_SEARCH_ORIGIN, "検索条件を指定してください");
    }

    /**
     * propertyIdから検索起点を解決
     */
    private VectorSource resolveFromProperty(String propertyId) throws IOException {
        ReferencePropertyData data = fetchReferenceProperty(propertyId);
        SimilarPropertyReferenceVO reference = data.reference();
        reference.setType("property");
        return new VectorSource(reference, data.vector());
    }

    /**
     * demoRefから検索起点を解決（Phase 1）
     */
    private VectorSource resolveFromDemoRef(String demoRef) {
        if (demoEmbeddingMapper == null) {
            throw new IllegalStateException("PropertyDemoEmbeddingMapper is not configured.");
        }

        // テナントを無視してDEMO用データを取得（DEMO用途のため）
        PropertyDemoEmbedding demoEmbedding = demoEmbeddingMapper.findByDemoRef(demoRef);
        if (demoEmbedding == null) {
            throw new BusinessException(PropertyErrorCode.DEMO_REF_NOT_FOUND, "指定されたDEMO画像が見つかりません");
        }

        // List<Double>からfloat[]に変換
        float[] vector = convertToFloatArray(demoEmbedding.getEmbeddingVector());
        if (vector == null || vector.length == 0) {
            throw new BusinessException(PropertyErrorCode.VECTOR_NOT_READY, "DEMO画像のベクトルが不正です");
        }

        log.debug("DEMO embedding resolved: demoRef={}, vectorLength={}", demoRef, vector.length);

        // reference情報を構築
        SimilarPropertyReferenceVO reference = new SimilarPropertyReferenceVO();
        reference.setType("demo");
        reference.setDemoRef(demoRef);
        reference.setThumbnailUrl(demoEmbedding.getThumbnailUrl());
        reference.setTitle(demoEmbedding.getTitle());
        reference.setArea(null);  // DEMO画像にはエリアなし

        return new VectorSource(reference, vector);
    }

    /**
     * List<Double>をfloat[]に変換
     */
    private float[] convertToFloatArray(List<Double> vectorList) {
        if (vectorList == null || vectorList.isEmpty()) {
            return null;
        }
        float[] result = new float[vectorList.size()];
        for (int i = 0; i < vectorList.size(); i++) {
            Double val = vectorList.get(i);
            result[i] = val != null ? val.floatValue() : 0.0f;
        }
        return result;
    }

    /**
     * VectorSource: reference情報とvectorのペア
     */
    private record VectorSource(SimilarPropertyReferenceVO reference, float[] vector) {
    }

    /**
     * 基準物件のデータ（reference 情報 + feature_vector）を取得
     */
    private ReferencePropertyData fetchReferenceProperty(String propertyId) throws IOException {
        String publishedIndex = properties.getPublishedIndex();
        Long tenantId = PropertySearchSupport.currentTenantId();

        // propertyId で物件を検索
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        boolBuilder.filter(Query.of(q -> q.term(t -> t.field("propertyKey").value(FieldValue.of(propertyId)))));
        if (tenantId != null) {
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("tenantId").value(FieldValue.of(tenantId)))));
        }
        // scope=published 固定
        boolBuilder.filter(Query.of(q -> q.term(t -> t.field("scope").value(FieldValue.of("published")))));

        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(publishedIndex)
                .size(1)
                .query(Query.of(q -> q.bool(boolBuilder.build())))
                .source(src -> src.filter(f -> f.includes(
                        "propertyKey", "title", "area", "mainImage.thumbnailSmKey",
                        "feature_vector"
                )))
        );

        SearchResponse<Map> response = client.search(searchRequest, Map.class);

        if (response.hits().hits().isEmpty()) {
            throw new BusinessException(PropertyErrorCode.LISTING_NOT_FOUND, "指定された物件が見つかりません");
        }

        Map<String, Object> source = response.hits().hits().get(0).source();
        if (source == null) {
            throw new BusinessException(PropertyErrorCode.LISTING_NOT_FOUND, "指定された物件が見つかりません");
        }

        // feature_vector を取得
        float[] vector = extractFeatureVector(source);
        if (vector == null) {
            throw new BusinessException(PropertyErrorCode.VECTOR_NOT_READY, "物件画像の特徴量が生成されていません");
        }

        // reference 情報を構築
        SimilarPropertyReferenceVO reference = new SimilarPropertyReferenceVO();
        reference.setPropertyKey(getString(source, "propertyKey"));
        reference.setTitle(getString(source, "title"));
        reference.setArea(getString(source, "area"));
        reference.setThumbnailUrl(extractThumbnailUrl(source));

        return new ReferencePropertyData(reference, vector);
    }

    /**
     * k-NN 検索を実行
     */
    private List<PropertySummaryVO> executeKnnSearch(float[] queryVector, int k, String excludePropertyKey) throws IOException {
        String publishedIndex = properties.getPublishedIndex();
        Long tenantId = PropertySearchSupport.currentTenantId();

        // フィルタクエリを構築
        // DEMO 用途: tenantId が null または 0 の場合、tenantId=1 (デフォルトテナント) を使用
        Long effectiveTenantId = (tenantId == null || tenantId == 0L) ? 1L : tenantId;

        BoolQuery.Builder filterBuilder = new BoolQuery.Builder();
        filterBuilder.filter(Query.of(q -> q.term(t -> t.field("tenantId").value(FieldValue.of(effectiveTenantId)))));
        // scope=published 固定
        filterBuilder.filter(Query.of(q -> q.term(t -> t.field("scope").value(FieldValue.of("published")))));

        Query filterQuery = Query.of(q -> q.bool(filterBuilder.build()));

        // KNN検索リクエストを構築
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(publishedIndex)
                .size(k)
                .query(Query.of(q -> q
                        .knn(KnnQuery.of(knn -> knn
                                .field(FEATURE_VECTOR_FIELD)
                                .vector(queryVector)
                                .k(k)
                                .filter(filterQuery)
                        ))
                ))
        );

        log.debug("KNN similar search request: field={}, k={}, index={}, vectorLength={}, effectiveTenantId={}",
                FEATURE_VECTOR_FIELD, k, publishedIndex, queryVector.length, effectiveTenantId);

        SearchResponse<Map> response = client.search(searchRequest, Map.class);

        List<PropertySummaryVO> results = new ArrayList<>();
        for (Hit<Map> hit : response.hits().hits()) {
            Map<String, Object> source = hit.source();
            if (source != null) {
                PropertySummaryVO vo = toSummary(source);
                // OpenSearch k-NN スコアをそのまま similarity として設定
                // cosine similarity の場合、(1 + cosine_similarity) / 2 形式で 0.0〜1.0
                if (hit.score() != null) {
                    vo.setSimilarity(hit.score());
                }
                results.add(vo);
            }
        }

        return results;
    }

    private float[] extractFeatureVector(Map<String, Object> source) {
        // feature_vector から取得（ルートレベル）
        Object featureVector = source.get("feature_vector");
        if (featureVector == null) {
            return null;
        }

        // List<Number> を float[] に変換
        if (featureVector instanceof List<?> vectorList) {
            float[] result = new float[vectorList.size()];
            for (int i = 0; i < vectorList.size(); i++) {
                Object elem = vectorList.get(i);
                if (elem instanceof Number num) {
                    result[i] = num.floatValue();
                } else {
                    return null;
                }
            }
            return result;
        }

        return null;
    }

    private String extractThumbnailUrl(Map<String, Object> source) {
        Map<String, Object> mainImage = asMap(source.get("mainImage"));
        if (mainImage != null) {
            String thumbnailKey = getString(mainImage, "thumbnailSmKey");
            return resolveAssetUrl(thumbnailKey);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private PropertySummaryVO toSummary(Map<String, Object> source) {
        PropertySummaryVO vo = new PropertySummaryVO();
        vo.setPropertyKey(getString(source, "propertyKey"));
        vo.setScope(getString(source, "scope"));
        vo.setVersion(getInteger(source, "version"));
        vo.setTitle(getString(source, "title"));
        vo.setArea(getString(source, "area"));
        vo.setAddress(getString(source, "address"));
        vo.setPropertyType(getString(source, "propertyType"));
        vo.setPriceJpy(getLong(source, "priceJpy"));
        vo.setLayout(getString(source, "layout"));
        vo.setStationWalkMin(getInteger(source, "stationWalkMin"));
        vo.setListedDate(getLocalDate(source, "listedDate"));
        vo.setRegisteredAt(getLocalDateTime(source, "registeredAt"));
        vo.setPriorityRank(getString(source, "priorityRank"));
        vo.setReviewStatus(getString(source, "reviewStatus"));

        Map<String, Object> registrant = asMap(source.get("registrant"));
        if (registrant != null) {
            vo.setRegistrantUserId(getLongFromString(registrant, "userId"));
            vo.setRegistrantDisplayName(getString(registrant, "displayName"));
        }

        Map<String, Object> mainImage = asMap(source.get("mainImage"));
        if (mainImage != null) {
            String thumbnailKey = getString(mainImage, "thumbnailSmKey");
            vo.setThumbnailUrl(resolveAssetUrl(thumbnailKey));
        }

        vo.setDraftSuggested(false);
        return vo;
    }

    private String resolveAssetUrl(String objectKey) {
        if (StrUtil.isBlank(objectKey)) {
            return null;
        }
        MinioFileService minio = minioFileServiceProvider.getIfAvailable();
        if (minio == null) {
            return null;
        }
        return minio.buildObjectUrl(objectKey);
    }

    private String getString(Map<String, Object> source, String field) {
        Object value = source.get(field);
        return value != null ? String.valueOf(value) : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        try {
            return objectMapper.convertValue(value, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (IllegalArgumentException e) {
            log.warn("Failed to coerce value to Map: type={}", value.getClass().getName());
            return null;
        }
    }

    private Integer getInteger(Map<String, Object> source, String field) {
        Object value = source.get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long getLong(Map<String, Object> source, String field) {
        Object value = source.get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long getLongFromString(Map<String, Object> source, String field) {
        String value = getString(source, field);
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate getLocalDate(Map<String, Object> source, String field) {
        String value = getString(source, field);
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private LocalDateTime getLocalDateTime(Map<String, Object> source, String field) {
        String value = getString(source, field);
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (RuntimeException e) {
            try {
                return LocalDateTime.parse(value);
            } catch (RuntimeException ex) {
                return null;
            }
        }
    }

    /**
     * 基準物件データ（reference + vector）
     */
    private record ReferencePropertyData(SimilarPropertyReferenceVO reference, float[] vector) {
    }
}
