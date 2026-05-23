package com.smartdx.property.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartdx.core.exception.BusinessException;
import com.smartdx.security.util.SecurityUtils;
import com.smartdx.property.exception.PropertyErrorCode;
import com.smartdx.property.mapper.PropertyListingMapper;
import com.smartdx.property.model.entity.PropertyListing;
import com.smartdx.property.model.req.PropertyLookupReq;
import com.smartdx.property.model.req.PropertySearchItemsReq;
import com.smartdx.property.model.vo.*;
import com.smartdx.property.service.PropertySearchService;
import com.smartdx.property.support.PropertySearchSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PropertySearchServiceImpl implements PropertySearchService {

    private final PropertyListingMapper listingMapper;

    private static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public IPage<PropertySummaryVO> lookup(PropertyLookupReq req) {
        PropertySearchSupport.normalize(req);

        Page<PropertyListing> page = new Page<>(req.getPage() + 1L, req.getSize());
        IPage<PropertyListing> listings = listingMapper.selectPage(page, buildQuery(req));
        return listings.convert(this::toSummary);
    }

    @Override
    public PropertyDetailVO getDetail(String propertyKey, String scope) {
        // scope 権限チェック
        if ("draft".equals(scope) && !canAccessDraftScope()) {
            throw new BusinessException(PropertyErrorCode.SCOPE_NOT_ALLOWED);
        }

        PropertyListing listing = listingMapper.selectOne(
                new LambdaQueryWrapper<PropertyListing>()
                        .eq(PropertyListing::getPropertyKey, propertyKey)
                        .eq(PropertyListing::getScope, scope)
                        .eq(PropertyListing::getIsDeleted, 0)
                        .last("LIMIT 1")
        );
        if (listing == null) {
            throw new BusinessException(PropertyErrorCode.LISTING_NOT_FOUND);
        }

        return toDetail(listing, scope);
    }

    private boolean canAccessDraftScope() {
        Set<String> roles = SecurityUtils.getRoles();
        return roles.contains("ADMIN") || roles.contains("REVIEWER") || roles.contains("FIELD_AGENT");
    }

    private PropertyDetailVO toDetail(PropertyListing listing, String scope) {
        PropertyDetailVO vo = new PropertyDetailVO();

        // 基本情報
        vo.setPropertyKey(listing.getPropertyKey());
        vo.setScope(scope);
        vo.setVersion(listing.getVersion());
        vo.setRegisteredAt(listing.getRegisteredAt() != null ? listing.getRegisteredAt().format(ISO_DATETIME) : null);
        vo.setUpdatedAt(listing.getUpdateTime() != null ? listing.getUpdateTime().format(ISO_DATETIME) : null);

        // 登録者情報
        if (listing.getRegistrantUserId() != null) {
            vo.setRegistrant(new PropertyRegistrantVO(
                    String.valueOf(listing.getRegistrantUserId()),
                    listing.getRegistrantDisplayName()
            ));
        }

        // 物件メタデータ
        PropertyMetaVO meta = new PropertyMetaVO();
        meta.setArea(listing.getArea());
        meta.setAddress(listing.getAddress());
        meta.setPropertyType(listing.getPropertyType());
        meta.setPriceJpy(listing.getPriceJpy());
        meta.setLayout(listing.getLayout());
        meta.setAreaSqm(listing.getAreaSqm());
        meta.setStationWalkMin(listing.getStationWalkMin());
        meta.setBuiltYearMonth(listing.getBuiltYearMonth());
        meta.setListedDate(listing.getListedDate() != null ? listing.getListedDate().toString() : null);
        meta.setPriorityRank(listing.getPriorityRank());
        vo.setMeta(meta);

        // 画像一覧（DB fallback では空リスト）
        vo.setAssets(Collections.emptyList());

        // 文書一覧（DB fallback では空リスト）
        vo.setDocuments(Collections.emptyList());

        // 審査情報
        PropertyReviewVO review = new PropertyReviewVO();
        review.setStatus(listing.getReviewStatus());
        review.setPriorityRank(listing.getPriorityRank());
        vo.setReview(review);

        // 監査サマリー（DB fallback では 0）
        vo.setAuditSummary(new PropertyAuditSummaryVO(0));

        // 権限情報
        vo.setPermissions(calculatePermissions(listing, scope));

        // お気に入り状態（DB fallback では常に false）
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId != null) {
            vo.setIsFavorite(false);
        }

        return vo;
    }

    private PropertyPermissionsVO calculatePermissions(PropertyListing listing, String scope) {
        PropertyPermissionsVO permissions = new PropertyPermissionsVO();
        Set<String> roles = SecurityUtils.getRoles();
        Long currentUserId = SecurityUtils.getUserId();

        boolean isAdmin = roles.contains("ADMIN");
        boolean isReviewer = roles.contains("REVIEWER");
        boolean isFieldAgent = roles.contains("FIELD_AGENT");

        boolean isRegistrant = currentUserId != null &&
                listing.getRegistrantUserId() != null &&
                currentUserId.equals(listing.getRegistrantUserId());

        String reviewStatus = listing.getReviewStatus();

        permissions.setCanViewReviewMemo(isReviewer || isFieldAgent || isAdmin);
        permissions.setCanEditReviewMemo((isReviewer || isAdmin) && "draft".equals(scope));
        permissions.setCanViewSensitiveDocs(isReviewer || isAdmin);
        permissions.setCanDownloadRaw(currentUserId != null);
        permissions.setCanDeleteDraft(
                "draft".equals(scope) && ((isFieldAgent && isRegistrant) || isReviewer || isAdmin)
        );

        boolean canResubmitStatus = "REJECTED".equals(reviewStatus) || "PENDING".equals(reviewStatus);
        permissions.setCanResubmit(
                isFieldAgent && isRegistrant && "draft".equals(scope) && canResubmitStatus
        );

        return permissions;
    }

    private LambdaQueryWrapper<PropertyListing> buildQuery(PropertyLookupReq req) {
        LambdaQueryWrapper<PropertyListing> query = new LambdaQueryWrapper<PropertyListing>()
                .eq(PropertyListing::getIsDeleted, 0);

        if (!"all".equals(req.getScope())) {
            query.eq(PropertyListing::getScope, req.getScope());
        }

        PropertySearchItemsReq items = req.getSearchItems();
        if (items != null) {
            applySearchItems(query, items);
        }

        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            String keyword = req.getKeyword().trim();
            query.and(wrapper -> wrapper
                    .like(PropertyListing::getPropertyKey, keyword)
                    .or().like(PropertyListing::getAddress, keyword)
                    .or().like(PropertyListing::getArea, keyword)
                    .or().like(PropertyListing::getPropertyType, keyword)
                    .or().like(PropertyListing::getPriorityRank, keyword)
            );
        }

        applySort(query, req.getSortBy(), req.getOrderBy());
        return query;
    }

    private void applySearchItems(LambdaQueryWrapper<PropertyListing> query, PropertySearchItemsReq items) {
        if (items.getArea() != null && !items.getArea().isEmpty()) {
            query.in(PropertyListing::getArea, items.getArea());
        }
        if (items.getPriceJpyMin() != null) {
            query.ge(PropertyListing::getPriceJpy, items.getPriceJpyMin());
        }
        if (items.getPriceJpyMax() != null) {
            query.le(PropertyListing::getPriceJpy, items.getPriceJpyMax());
        }
        if (items.getPropertyType() != null && !items.getPropertyType().isEmpty()) {
            query.in(PropertyListing::getPropertyType, items.getPropertyType());
        }
        if (items.getStationWalkMax() != null) {
            query.le(PropertyListing::getStationWalkMin, items.getStationWalkMax());
        }
        if (items.getPriorityRank() != null && !items.getPriorityRank().isEmpty()) {
            query.in(PropertyListing::getPriorityRank, items.getPriorityRank());
        }
        if (items.getReviewStatus() != null && !items.getReviewStatus().isBlank()) {
            query.eq(PropertyListing::getReviewStatus, items.getReviewStatus());
        }
        if (items.getRegisteredAfter() != null) {
            query.ge(PropertyListing::getRegisteredAt, items.getRegisteredAfter().atStartOfDay());
        }
        if (items.getRegisteredBefore() != null) {
            query.le(PropertyListing::getRegisteredAt, items.getRegisteredBefore().atTime(LocalTime.MAX));
        }
        if (items.getRegistrantId() != null && !items.getRegistrantId().isBlank()) {
            query.eq(PropertyListing::getRegistrantUserId, PropertySearchSupport.resolveUserId(items.getRegistrantId()));
        }
    }

    private void applySort(LambdaQueryWrapper<PropertyListing> query, String sortBy, String orderBy) {
        boolean asc = "asc".equals(orderBy);
        switch (sortBy) {
            case "listedDate" -> query.orderBy(true, asc, PropertyListing::getListedDate);
            case "registeredAt" -> query.orderBy(true, asc, PropertyListing::getRegisteredAt);
            case "priorityRank" -> query.orderBy(true, asc, PropertyListing::getPriorityRank);
            case "priceJpy" -> query.orderBy(true, asc, PropertyListing::getPriceJpy);
            case "area" -> query.orderBy(true, asc, PropertyListing::getArea);
            case "stationWalkMin" -> query.orderBy(true, asc, PropertyListing::getStationWalkMin);
            default -> throw new IllegalArgumentException("Unsupported sortBy: " + sortBy);
        }
        query.orderByDesc(PropertyListing::getId);
    }

    private PropertySummaryVO toSummary(PropertyListing listing) {
        PropertySummaryVO vo = new PropertySummaryVO();
        vo.setPropertyKey(listing.getPropertyKey());
        vo.setScope(listing.getScope());
        vo.setVersion(listing.getVersion());
        vo.setTitle(listing.getAddress());
        vo.setArea(listing.getArea());
        vo.setAddress(listing.getAddress());
        vo.setPropertyType(listing.getPropertyType());
        vo.setPriceJpy(listing.getPriceJpy());
        vo.setLayout(listing.getLayout());
        vo.setStationWalkMin(listing.getStationWalkMin());
        vo.setListedDate(listing.getListedDate());
        vo.setRegisteredAt(listing.getRegisteredAt());
        vo.setPriorityRank(listing.getPriorityRank());
        vo.setReviewStatus(listing.getReviewStatus());
        vo.setRegistrantUserId(listing.getRegistrantUserId());
        vo.setRegistrantDisplayName(listing.getRegistrantDisplayName());
        vo.setDraftSuggested(false);
        return vo;
    }
}
