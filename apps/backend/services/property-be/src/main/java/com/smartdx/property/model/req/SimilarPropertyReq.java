package com.smartdx.property.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * LST-SIM-01: 類似物件検索リクエスト（v2.0.0）
 * 複数検索起点対応: propertyId, imageId, demoRef
 */
@Data
@Schema(description = "類似物件検索リクエスト")
public class SimilarPropertyReq {

    // 検索起点パラメータ（いずれか1つ必須）
    @Schema(description = "基準物件キー（UUID v4）")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "物件キーの形式が不正です")
    private String propertyId;

    @Schema(description = "アップロード画像ID（UUID v4）※Phase 2")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "画像IDの形式が不正です")
    private String imageId;

    @Schema(description = "DEMO用固定参照キー（demo-* prefix）")
    @Pattern(regexp = "^demo-[a-zA-Z0-9-]+$",
            message = "demoRefの形式が不正です（demo-で始まる英数字ハイフンのみ）")
    private String demoRef;

    // 共通パラメータ
    @Schema(description = "取得件数（1〜50）", defaultValue = "20")
    @Min(value = 1, message = "limit は 1 以上である必要があります")
    @Max(value = 50, message = "limit は 50 以下である必要があります")
    private Integer limit = 20;

    @Schema(description = "最低類似度スコア（0.0〜1.0）", defaultValue = "0.0")
    @Min(value = 0, message = "minScore は 0 以上である必要があります")
    @Max(value = 1, message = "minScore は 1 以下である必要があります")
    private Double minScore = 0.0;

    @Schema(description = "検索起点と同一物件を除外", defaultValue = "true")
    private Boolean excludeSameProperty = true;

    /**
     * 検索起点が指定されているか確認
     */
    public boolean hasSearchOrigin() {
        return propertyId != null || imageId != null || demoRef != null;
    }
}
