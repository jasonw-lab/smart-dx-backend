package com.smartdx.property.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartdx.core.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("property_listing")
public class PropertyListing extends BaseEntity {

    private String propertyKey;

    private String scope;

    private Integer version;

    private String area;

    private String address;

    private String propertyType;

    private Long priceJpy;

    private String layout;

    private BigDecimal areaSqm;

    private Integer stationWalkMin;

    private String builtYearMonth;

    private LocalDate listedDate;

    private Integer listedYear;

    private String priorityRank;

    private String reviewStatus;

    private Long registrantUserId;

    private String registrantDisplayName;

    private LocalDateTime registeredAt;

    private LocalDateTime publishedAt;

    private Integer isDeleted;
}
