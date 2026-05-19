package com.smartdx.property.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * LST-SIM-01: 類似物件検索 - 基準物件情報（v2.0.0）
 * 複数検索起点対応: property, image, demo
 */
@Data
@Schema(description = "基準物件情報")
public class SimilarPropertyReferenceVO {

    @Schema(description = "検索起点種別: property / image / demo")
    private String type;

    @Schema(description = "物件キー（UUID v4）- property起点時のみ")
    private String propertyKey;

    @Schema(description = "アップロード画像ID - image起点時のみ（Phase 2）")
    private String imageId;

    @Schema(description = "DEMO参照キー - demo起点時のみ")
    private String demoRef;

    @Schema(description = "サムネイルURL")
    private String thumbnailUrl;

    @Schema(description = "表示タイトル")
    private String title;

    @Schema(description = "エリアコード - property起点時のみ")
    private String area;
}
