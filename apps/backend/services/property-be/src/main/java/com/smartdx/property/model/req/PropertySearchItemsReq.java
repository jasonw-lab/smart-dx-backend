package com.smartdx.property.model.req;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class PropertySearchItemsReq {

    private List<String> area;

    private Long priceJpyMin;

    private Long priceJpyMax;

    private List<String> propertyType;

    private Integer stationWalkMax;

    private List<String> priorityRank;

    private String reviewStatus;

    private LocalDate registeredAfter;

    private LocalDate registeredBefore;

    private String registrantId;

    private String reviewerId;
}
