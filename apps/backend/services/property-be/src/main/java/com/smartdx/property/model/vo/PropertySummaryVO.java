package com.smartdx.property.model.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class PropertySummaryVO {

    private String propertyKey;

    private String scope;

    private Integer version;

    private String title;

    private String area;

    private String address;

    private String propertyType;

    private Long priceJpy;

    private String layout;

    private Integer stationWalkMin;

    private LocalDate listedDate;

    private LocalDateTime registeredAt;

    private String priorityRank;

    private String reviewStatus;

    private Long registrantUserId;

    private String registrantDisplayName;

    private String thumbnailUrl;

    private String previewSm;

    private String previewLg;

    private Boolean draftSuggested;

    /**
     * 類似度スコア（KNN検索時のみ設定）
     * 0.0〜1.0 の範囲で高いほど類似
     */
    private Double similarity;
}
