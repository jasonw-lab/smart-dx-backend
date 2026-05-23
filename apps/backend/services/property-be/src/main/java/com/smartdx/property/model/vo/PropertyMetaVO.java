package com.smartdx.property.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 物件メタデータ
 */
@Getter
@Setter
@Schema(description = "物件メタデータ")
public class PropertyMetaVO {

    @Schema(description = "エリアコード", example = "tokyo-shibuya")
    private String area;

    @Schema(description = "所在地（建物名まで、号室除く）")
    private String address;

    @Schema(description = "物件種別コード", example = "mansion")
    private String propertyType;

    @Schema(description = "価格（円、整数）")
    private Long priceJpy;

    @Schema(description = "間取り", example = "3LDK")
    private String layout;

    @Schema(description = "専有面積（㎡）")
    private BigDecimal areaSqm;

    @Schema(description = "駅徒歩（分）")
    private Integer stationWalkMin;

    @Schema(description = "築年月（YYYY-MM）", example = "2018-04")
    private String builtYearMonth;

    @Schema(description = "掲載日（YYYY-MM-DD）", example = "2026-03-12")
    private String listedDate;

    @Schema(description = "推奨ランク", example = "A")
    private String priorityRank;
}
