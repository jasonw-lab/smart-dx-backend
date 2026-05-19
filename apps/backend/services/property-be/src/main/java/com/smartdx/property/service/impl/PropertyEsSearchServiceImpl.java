package com.smartdx.property.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartdx.core.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdx.property.file.MinioFileService;
import com.smartdx.security.util.SecurityUtils;
import com.smartdx.property.config.PropertyOpenSearchProperties;
import com.smartdx.property.exception.PropertyErrorCode;
import com.smartdx.property.model.entity.PropertyEmbeddingRef;
import com.smartdx.property.model.req.PropertyLookupReq;
import com.smartdx.property.model.req.PropertySearchItemsReq;
import com.smartdx.property.model.vo.*;
import com.smartdx.property.service.PropertyEmbeddingService;
import com.smartdx.property.service.PropertyEsSearchService;
import com.smartdx.property.support.PropertySearchSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.KnnQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class PropertyEsSearchServiceImpl implements PropertyEsSearchService {

    private static final int DEFAULT_KNN_TOP_K = 50;
    private static final String FEATURE_VECTOR_FIELD_PREFIX = "assets.featureVector.";

    private final PropertyOpenSearchProperties properties;
    private final OpenSearchClient client;
    private final ObjectProvider<MinioFileService> minioFileServiceProvider;
    private final ObjectProvider<PropertyEmbeddingService> embeddingServiceProvider;
    private final ObjectMapper objectMapper;

    public PropertyEsSearchServiceImpl(
            PropertyOpenSearchProperties properties,
            @Autowired(required = false) OpenSearchClient client,
            ObjectProvider<MinioFileService> minioFileServiceProvider,
            ObjectProvider<PropertyEmbeddingService> embeddingServiceProvider,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.client = client;
        this.minioFileServiceProvider = minioFileServiceProvider;
        this.embeddingServiceProvider = embeddingServiceProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public IPage<PropertySummaryVO> lookup(PropertyLookupReq req) {
        PropertySearchSupport.normalize(req);
        if (!properties.isEnabled()) {
            throw new IllegalStateException("OpenSearch property search is disabled.");
        }
        if (client == null) {
            throw new IllegalStateException("OpenSearch client is not configured.");
        }

        try {
            return doLookup(req);
        } catch (IOException e) {
            throw new IllegalStateException("OpenSearch property search failed.", e);
        }
    }

    private IPage<PropertySummaryVO> doLookup(PropertyLookupReq req) throws IOException {
        // KNN類似検索が有効な場合
        if (req.isSimilaritySearchEnabled()) {
            return doKnnLookup(req);
        }

        // 通常の検索
        SearchRequest searchRequest = buildSearchRequest(req);
        log.info("OpenSearch query: indices={}, from={}, size={}",
                searchRequest.index(), searchRequest.from(), searchRequest.size());
        SearchResponse<Map> response = client.search(searchRequest, Map.class);
        log.info("OpenSearch response: total={}, hits={}",
                response.hits().total().value(), response.hits().hits().size());
        return toPage(req, response, false);
    }

    /**
     * KNN類似検索を実行
     */
    private IPage<PropertySummaryVO> doKnnLookup(PropertyLookupReq req) throws IOException {
        // 特徴量ベクトルを取得
        double[] queryVector = resolveQueryVector(req);
        String flag = resolveFlag(req);

        // KNNフィールド名を決定
        String knnField = FEATURE_VECTOR_FIELD_PREFIX + flag;

        // topK を決定
        int topK = req.getTopK() != null ? req.getTopK() : DEFAULT_KNN_TOP_K;

        List<String> indices = resolveIndices(req.getScope());

        // フィルタクエリを構築
        Query filterQuery = buildFilterQuery(req);

        // KNN検索リクエストを構築
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(indices)
                .size(topK)
                .query(Query.of(q -> q
                        .knn(KnnQuery.of(knn -> knn
                                .field(knnField)
                                .vector(toFloatArray(queryVector))
                                .k(topK)
                                .filter(filterQuery)
                        ))
                ))
        );

        log.debug("KNN search request: field={}, topK={}, indices={}", knnField, topK, indices);

        SearchResponse<Map> response = client.search(searchRequest, Map.class);
        return toPage(req, response, true);
    }

    /**
     * リクエストから特徴量ベクトルを解決
     */
    private double[] resolveQueryVector(PropertyLookupReq req) {
        if (StrUtil.isNotBlank(req.getEmbeddingRef())) {
            // embeddingRef から取得
            PropertyEmbeddingService embeddingService = embeddingServiceProvider.getIfAvailable();
            if (embeddingService == null) {
                throw new BusinessException(PropertyErrorCode.INTERNAL_ERROR, "EmbeddingService is not available");
            }

            PropertyEmbeddingRef embeddingRef = embeddingService.findByRef(req.getEmbeddingRef());
            if (embeddingRef == null) {
                throw new BusinessException(PropertyErrorCode.EMBEDDING_NOT_FOUND);
            }

            return parseVectorJson(embeddingRef.getVectorJson());
        } else if (StrUtil.isNotBlank(req.getReferenceAsset())) {
            // referenceAsset から取得（OpenSearchからアセットのベクトルを取得）
            return fetchAssetVector(req.getReferenceAsset(), req.getReferenceFlag());
        }

        throw new BusinessException(PropertyErrorCode.VALIDATION_ERROR, "embeddingRef または referenceAsset が必要です");
    }

    /**
     * flagを解決
     */
    private String resolveFlag(PropertyLookupReq req) {
        if (StrUtil.isNotBlank(req.getEmbeddingRef())) {
            // embeddingRef から flag を取得
            PropertyEmbeddingService embeddingService = embeddingServiceProvider.getIfAvailable();
            if (embeddingService != null) {
                PropertyEmbeddingRef embeddingRef = embeddingService.findByRef(req.getEmbeddingRef());
                if (embeddingRef != null) {
                    return embeddingRef.getFlag();
                }
            }
        }
        return req.getReferenceFlag();
    }

    /**
     * アセットの特徴量ベクトルをOpenSearchから取得
     */
    private double[] fetchAssetVector(String assetKey, String flag) {
        if (StrUtil.isBlank(flag)) {
            throw new BusinessException(PropertyErrorCode.INVALID_FLAG, "referenceAsset 指定時は referenceFlag が必須です");
        }

        // TODO: OpenSearchからアセットのベクトルを取得する実装
        // 現在はVECTOR_NOT_READYエラーを返す
        throw new BusinessException(PropertyErrorCode.VECTOR_NOT_READY,
                "指定されたアセットの特徴量ベクトルがまだ準備されていません");
    }

    /**
     * JSON文字列からベクトルをパース
     */
    private double[] parseVectorJson(String vectorJson) {
        try {
            return objectMapper.readValue(vectorJson, double[].class);
        } catch (Exception e) {
            log.error("Failed to parse vector JSON", e);
            throw new BusinessException(PropertyErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * double配列をfloat配列に変換（OpenSearchクライアント用）
     */
    private float[] toFloatArray(double[] doubles) {
        float[] floats = new float[doubles.length];
        for (int i = 0; i < doubles.length; i++) {
            floats[i] = (float) doubles[i];
        }
        return floats;
    }

    /**
     * フィルタクエリを構築（KNN検索用）
     */
    private Query buildFilterQuery(PropertyLookupReq req) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        Long tenantId = PropertySearchSupport.currentTenantId();
        if (tenantId != null) {
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("tenantId").value(FieldValue.of(tenantId)))));
        }
        if (!"all".equals(req.getScope())) {
            String scope = req.getScope();
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("scope").value(FieldValue.of(scope)))));
        }

        PropertySearchItemsReq items = req.getSearchItems();
        if (items != null) {
            applySearchItems(boolBuilder, items);
        }

        return Query.of(q -> q.bool(boolBuilder.build()));
    }

    @Override
    public PropertyDetailVO getDetail(String propertyKey, String scope) {
        // バリデーション
        if (StrUtil.isBlank(propertyKey)) {
            throw new BusinessException(PropertyErrorCode.VALIDATION_ERROR);
        }
        if (!isValidScope(scope)) {
            throw new BusinessException(PropertyErrorCode.INVALID_SCOPE);
        }

        // scope 権限チェック
        if ("draft".equals(scope) && !canAccessDraftScope()) {
            throw new BusinessException(PropertyErrorCode.SCOPE_NOT_ALLOWED);
        }

        if (!properties.isEnabled()) {
            throw new IllegalStateException("OpenSearch property search is disabled.");
        }
        if (client == null) {
            throw new IllegalStateException("OpenSearch client is not configured.");
        }

        try {
            return doGetDetail(propertyKey, scope);
        } catch (IOException e) {
            throw new IllegalStateException("OpenSearch property detail search failed.", e);
        }
    }

    private PropertyDetailVO doGetDetail(String propertyKey, String scope) throws IOException {
        String index = "published".equals(scope) ? properties.getPublishedIndex() : properties.getDraftIndex();

        // propertyKey で物件を検索
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        boolBuilder.filter(Query.of(q -> q.term(t -> t.field("propertyKey").value(FieldValue.of(propertyKey)))));

        Long tenantId = PropertySearchSupport.currentTenantId();
        if (tenantId != null) {
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("tenantId").value(FieldValue.of(tenantId)))));
        }

        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(index)
                .size(1)
                .query(Query.of(q -> q.bool(boolBuilder.build())))
        );

        SearchResponse<Map> response = client.search(searchRequest, Map.class);

        if (response.hits().hits().isEmpty()) {
            throw new BusinessException(PropertyErrorCode.LISTING_NOT_FOUND, "指定された物件が見つかりません");
        }

        Map<String, Object> source = response.hits().hits().get(0).source();
        if (source == null) {
            throw new BusinessException(PropertyErrorCode.LISTING_NOT_FOUND, "指定された物件が見つかりません");
        }

        return toDetail(source, scope);
    }

    private boolean isValidScope(String scope) {
        return "published".equals(scope) || "draft".equals(scope);
    }

    private boolean canAccessDraftScope() {
        Set<String> roles = SecurityUtils.getRoles();
        // reviewer, field_agent, admin は draft にアクセス可能
        return roles.contains("ADMIN") || roles.contains("REVIEWER") || roles.contains("FIELD_AGENT");
    }

    @SuppressWarnings("unchecked")
    private PropertyDetailVO toDetail(Map<String, Object> source, String scope) {
        PropertyDetailVO vo = new PropertyDetailVO();

        // 基本情報
        vo.setPropertyKey(getString(source, "propertyKey"));
        vo.setScope(scope);
        vo.setVersion(getInteger(source, "version"));
        vo.setRegisteredAt(getString(source, "registeredAt"));
        vo.setUpdatedAt(getString(source, "updatedAt"));

        // 登録者情報
        Object registrant = source.get("registrant");
        if (registrant instanceof Map<?, ?> regMap) {
            PropertyRegistrantVO registrantVO = new PropertyRegistrantVO();
            registrantVO.setUserId(getString((Map<String, Object>) regMap, "userId"));
            registrantVO.setDisplayName(getString((Map<String, Object>) regMap, "displayName"));
            vo.setRegistrant(registrantVO);
        }

        // 物件メタデータ
        PropertyMetaVO meta = new PropertyMetaVO();
        meta.setArea(getString(source, "area"));
        meta.setAddress(getString(source, "address"));
        meta.setPropertyType(getString(source, "propertyType"));
        meta.setPriceJpy(getLong(source, "priceJpy"));
        meta.setLayout(getString(source, "layout"));
        meta.setAreaSqm(getBigDecimal(source, "areaSqm"));
        meta.setStationWalkMin(getInteger(source, "stationWalkMin"));
        meta.setBuiltYearMonth(getString(source, "builtYearMonth"));
        meta.setListedDate(getString(source, "listedDate"));
        meta.setPriorityRank(getString(source, "priorityRank"));
        vo.setMeta(meta);

        // 画像一覧
        vo.setAssets(extractAssets(source));

        // 文書一覧
        vo.setDocuments(extractDocuments(source));

        // 審査情報
        Object review = source.get("review");
        if (review instanceof Map<?, ?> reviewMap) {
            PropertyReviewVO reviewVO = new PropertyReviewVO();
            reviewVO.setStatus(getString((Map<String, Object>) reviewMap, "status"));
            reviewVO.setPriorityRank(getString((Map<String, Object>) reviewMap, "priorityRank"));
            reviewVO.setComment(getString((Map<String, Object>) reviewMap, "comment"));
            reviewVO.setReviewerId(getString((Map<String, Object>) reviewMap, "reviewerId"));
            reviewVO.setReviewerName(getString((Map<String, Object>) reviewMap, "reviewerName"));
            reviewVO.setReviewedAt(getString((Map<String, Object>) reviewMap, "reviewedAt"));
            vo.setReview(reviewVO);
        }

        // 監査サマリー
        Integer auditCount = getInteger(source, "auditLogCount");
        vo.setAuditSummary(new PropertyAuditSummaryVO(auditCount != null ? auditCount : 0));

        // 権限情報
        vo.setPermissions(calculatePermissions(source, scope));

        // お気に入り状態（ログイン時のみ）
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId != null) {
            vo.setIsFavorite(checkIsFavorite(source, currentUserId));
        }

        return vo;
    }

    @SuppressWarnings("unchecked")
    private List<PropertyAssetVO> extractAssets(Map<String, Object> source) {
        Object assets = source.get("assets");
        if (!(assets instanceof List<?> assetList)) {
            return Collections.emptyList();
        }

        List<PropertyAssetVO> result = new ArrayList<>();
        for (Object asset : assetList) {
            if (asset instanceof Map<?, ?> assetMap) {
                Map<String, Object> assetData = (Map<String, Object>) assetMap;
                PropertyAssetVO assetVO = new PropertyAssetVO();
                assetVO.setAssetKey(getString(assetData, "assetKey"));
                assetVO.setFlag(getString(assetData, "flag"));
                assetVO.setMimeType(getString(assetData, "mimeType"));
                assetVO.setSizeBytes(getLong(assetData, "sizeBytes"));
                result.add(assetVO);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<PropertyDocumentVO> extractDocuments(Map<String, Object> source) {
        Object documents = source.get("documents");
        if (!(documents instanceof List<?> docList)) {
            return Collections.emptyList();
        }

        List<PropertyDocumentVO> result = new ArrayList<>();
        for (Object doc : docList) {
            if (doc instanceof Map<?, ?> docMap) {
                Map<String, Object> docData = (Map<String, Object>) docMap;
                PropertyDocumentVO docVO = new PropertyDocumentVO();
                docVO.setDocKey(getString(docData, "docKey"));
                docVO.setDocType(getString(docData, "docType"));
                docVO.setFileName(getString(docData, "fileName"));
                docVO.setSizeBytes(getLong(docData, "sizeBytes"));
                result.add(docVO);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private PropertyPermissionsVO calculatePermissions(Map<String, Object> source, String scope) {
        PropertyPermissionsVO permissions = new PropertyPermissionsVO();
        Set<String> roles = SecurityUtils.getRoles();
        Long currentUserId = SecurityUtils.getUserId();

        boolean isAdmin = roles.contains("ADMIN");
        boolean isReviewer = roles.contains("REVIEWER");
        boolean isFieldAgent = roles.contains("FIELD_AGENT");

        // 登録者かどうか判定
        boolean isRegistrant = false;
        Object registrant = source.get("registrant");
        if (registrant instanceof Map<?, ?> regMap && currentUserId != null) {
            String registrantUserId = getString((Map<String, Object>) regMap, "userId");
            isRegistrant = String.valueOf(currentUserId).equals(registrantUserId);
        }

        // 審査ステータス取得
        String reviewStatus = null;
        Object review = source.get("review");
        if (review instanceof Map<?, ?> reviewMap) {
            reviewStatus = getString((Map<String, Object>) reviewMap, "status");
        }

        // canViewReviewMemo: reviewer, field_agent, admin
        permissions.setCanViewReviewMemo(isReviewer || isFieldAgent || isAdmin);

        // canEditReviewMemo: reviewer, admin かつ scope=draft
        permissions.setCanEditReviewMemo((isReviewer || isAdmin) && "draft".equals(scope));

        // canViewSensitiveDocs: reviewer, admin
        permissions.setCanViewSensitiveDocs(isReviewer || isAdmin);

        // canDownloadRaw: 認証済み全員
        permissions.setCanDownloadRaw(currentUserId != null);

        // canDeleteDraft: (field_agent かつ自分が登録者) または reviewer, admin かつ scope=draft
        permissions.setCanDeleteDraft(
                "draft".equals(scope) && ((isFieldAgent && isRegistrant) || isReviewer || isAdmin)
        );

        // canResubmit: field_agent かつ自分が登録者 かつ scope=draft かつ status=REJECTED または PENDING
        boolean canResubmitStatus = "REJECTED".equals(reviewStatus) || "PENDING".equals(reviewStatus);
        permissions.setCanResubmit(
                isFieldAgent && isRegistrant && "draft".equals(scope) && canResubmitStatus
        );

        return permissions;
    }

    @SuppressWarnings("unchecked")
    private Boolean checkIsFavorite(Map<String, Object> source, Long currentUserId) {
        Object favorites = source.get("favoriteUserIds");
        if (favorites instanceof List<?> favList) {
            for (Object fav : favList) {
                if (String.valueOf(currentUserId).equals(String.valueOf(fav))) {
                    return true;
                }
            }
        }
        return false;
    }

    private BigDecimal getBigDecimal(Map<String, Object> source, String field) {
        Object value = source.get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private SearchRequest buildSearchRequest(PropertyLookupReq req) {
        List<String> indices = resolveIndices(req.getScope());

        return SearchRequest.of(s -> s
                .index(indices)
                .from(req.getPage() * req.getSize())
                .size(req.getSize())
                .query(buildQuery(req))
                .sort(buildSort(req))
        );
    }

    private List<String> resolveIndices(String scope) {
        return switch (scope) {
            case "published" -> List.of(properties.getPublishedIndex());
            case "draft" -> List.of(properties.getDraftIndex());
            case "all" -> List.of(properties.getPublishedIndex(), properties.getDraftIndex());
            default -> throw new IllegalArgumentException("Unsupported scope: " + scope);
        };
    }

    private Query buildQuery(PropertyLookupReq req) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        Long tenantId = PropertySearchSupport.currentTenantId();
        log.info("buildQuery: tenantId={}, scope={}", tenantId, req.getScope());
        if (tenantId != null) {
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("tenantId").value(FieldValue.of(tenantId)))));
        }
        if (!"all".equals(req.getScope())) {
            String scope = req.getScope();
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("scope").value(FieldValue.of(scope)))));
        }

        PropertySearchItemsReq items = req.getSearchItems();
        if (items != null) {
            applySearchItems(boolBuilder, items);
            log.info("buildQuery: searchItems applied - area={}, priceMax={}, propertyType={}",
                    items.getArea(), items.getPriceJpyMax(), items.getPropertyType());
        }

        if (StrUtil.isNotBlank(req.getKeyword())) {
            String keyword = req.getKeyword().trim();
            boolBuilder.must(Query.of(q -> q.multiMatch(m -> m
                    .query(keyword)
                    .fields("title^3", "description^2", "address^2", "attachment.content", "fileName.text")
            )));
        } else {
            boolBuilder.must(Query.of(q -> q.matchAll(ma -> ma)));
        }

        Query query = Query.of(q -> q.bool(boolBuilder.build()));
        log.info("buildQuery: final query type={}", query._kind());
        return query;
    }

    private void applySearchItems(BoolQuery.Builder boolBuilder, PropertySearchItemsReq items) {
        if (items.getArea() != null && !items.getArea().isEmpty()) {
            List<FieldValue> values = items.getArea().stream().map(FieldValue::of).toList();
            boolBuilder.filter(Query.of(q -> q.terms(t -> t.field("area").terms(tv -> tv.value(values)))));
        }
        if (items.getPriceJpyMin() != null || items.getPriceJpyMax() != null) {
            boolBuilder.filter(rangeQuery("priceJpy", items.getPriceJpyMin(), items.getPriceJpyMax()));
        }
        if (items.getPropertyType() != null && !items.getPropertyType().isEmpty()) {
            List<FieldValue> values = items.getPropertyType().stream().map(FieldValue::of).toList();
            boolBuilder.filter(Query.of(q -> q.terms(t -> t.field("propertyType").terms(tv -> tv.value(values)))));
        }
        if (items.getStationWalkMax() != null) {
            boolBuilder.filter(rangeQuery("stationWalkMin", null, items.getStationWalkMax().longValue()));
        }
        if (items.getPriorityRank() != null && !items.getPriorityRank().isEmpty()) {
            List<FieldValue> values = items.getPriorityRank().stream().map(FieldValue::of).toList();
            boolBuilder.filter(Query.of(q -> q.terms(t -> t.field("priorityRank").terms(tv -> tv.value(values)))));
        }
        if (StrUtil.isNotBlank(items.getReviewStatus())) {
            String status = items.getReviewStatus();
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("reviewStatus").value(FieldValue.of(status)))));
        }
        if (items.getRegisteredAfter() != null || items.getRegisteredBefore() != null) {
            boolBuilder.filter(dateRangeQuery("registeredAt", items.getRegisteredAfter(), items.getRegisteredBefore()));
        }
        if (StrUtil.isNotBlank(items.getRegistrantId())) {
            Long userId = PropertySearchSupport.resolveUserId(items.getRegistrantId());
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("registrant.userId").value(FieldValue.of(String.valueOf(userId))))));
        }
        if (StrUtil.isNotBlank(items.getReviewerId())) {
            Long reviewerId = PropertySearchSupport.resolveUserId(items.getReviewerId());
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("review.reviewerId").value(FieldValue.of(String.valueOf(reviewerId))))));
        }
    }

    private Query rangeQuery(String field, Long min, Long max) {
        return Query.of(q -> q.range(r -> {
            r.field(field);
            if (min != null) {
                r.gte(JsonData.of(min));
            }
            if (max != null) {
                r.lte(JsonData.of(max));
            }
            return r;
        }));
    }

    private Query dateRangeQuery(String field, LocalDate from, LocalDate to) {
        return Query.of(q -> q.range(r -> {
            r.field(field);
            if (from != null) {
                r.gte(JsonData.of(from.toString()));
            }
            if (to != null) {
                r.lte(JsonData.of(to.toString()));
            }
            return r;
        }));
    }

    private List<SortOptions> buildSort(PropertyLookupReq req) {
        String field = switch (req.getSortBy()) {
            case "listedDate" -> "listedDate";
            case "registeredAt" -> "registeredAt";
            case "priorityRank" -> "priorityRank";
            case "priceJpy" -> "priceJpy";
            case "area" -> "area";
            case "stationWalkMin" -> "stationWalkMin";
            default -> throw new IllegalArgumentException("Unsupported sortBy: " + req.getSortBy());
        };
        SortOrder order = "asc".equalsIgnoreCase(req.getOrderBy()) ? SortOrder.Asc : SortOrder.Desc;

        List<SortOptions> sortOptions = new ArrayList<>();
        sortOptions.add(SortOptions.of(s -> s.field(f -> f.field(field).order(order))));
        sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("_id").order(SortOrder.Desc))));
        return sortOptions;
    }

    @SuppressWarnings("unchecked")
    private IPage<PropertySummaryVO> toPage(PropertyLookupReq req, SearchResponse<Map> response, boolean isKnnSearch) {
        Page<PropertySummaryVO> page = new Page<>(req.getPage() + 1L, req.getSize());
        page.setTotal(response.hits().total() != null ? response.hits().total().value() : 0);

        List<PropertySummaryVO> records = new ArrayList<>();
        for (Hit<Map> hit : response.hits().hits()) {
            Map<String, Object> source = hit.source();
            if (source != null) {
                PropertySummaryVO vo = toSummary(source);

                // KNN検索の場合、類似度スコアを設定
                if (isKnnSearch && hit.score() != null) {
                    // cosine類似度の場合、スコアは (1 + cosine_similarity) / 2 の形式
                    // 0.0〜1.0 に正規化
                    vo.setSimilarity(hit.score());
                }

                records.add(vo);
            }
        }
        page.setRecords(records);
        return page;
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

        Object registrant = source.get("registrant");
        if (registrant instanceof Map<?, ?> regMap) {
            vo.setRegistrantUserId(getLongFromString((Map<String, Object>) regMap, "userId"));
            vo.setRegistrantDisplayName(getString((Map<String, Object>) regMap, "displayName"));
        }

        Object mainImage = source.get("mainImage");
        if (mainImage instanceof Map<?, ?> imgMap) {
            // Use original image (s3Path) instead of small thumbnail for better resolution
            String imageKey = getString((Map<String, Object>) imgMap, "s3Path");
            vo.setThumbnailUrl(resolveAssetUrl(imageKey));
        }

        vo.setDraftSuggested(false);
        return vo;
    }

    private String getString(Map<String, Object> source, String field) {
        Object value = source.get(field);
        return value != null ? String.valueOf(value) : null;
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
}
