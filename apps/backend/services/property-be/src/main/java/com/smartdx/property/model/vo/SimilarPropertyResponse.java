package com.smartdx.property.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * LST-SIM-01: 類似物件検索レスポンス
 */
@Data
@Schema(description = "類似物件検索レスポンス")
public class SimilarPropertyResponse {

    @Schema(description = "基準物件情報")
    private SimilarPropertyReferenceVO reference;

    @Schema(description = "類似物件件数（minScore適用後）")
    private Integer total;

    @Schema(description = "類似物件リスト（similarity降順）")
    private List<PropertySummaryVO> list;
}
