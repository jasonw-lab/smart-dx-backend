package com.smartdx.property.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 物件詳細レスポンス（LST-LST-01）
 */
@Getter
@Setter
@Schema(description = "物件詳細レスポンス")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PropertyDetailVO {

    // === 基本情報 ===
    @Schema(description = "物件キー（UUID v4）")
    private String propertyKey;

    @Schema(description = "スコープ: published または draft")
    private String scope;

    @Schema(description = "楽観ロック用バージョン")
    private Integer version;

    @Schema(description = "登録日時（ISO 8601）")
    private String registeredAt;

    @Schema(description = "更新日時（ISO 8601）")
    private String updatedAt;

    // === 登録者情報 ===
    @Schema(description = "登録者情報")
    private PropertyRegistrantVO registrant;

    // === 物件メタデータ ===
    @Schema(description = "物件メタデータ")
    private PropertyMetaVO meta;

    // === 画像一覧 ===
    @Schema(description = "画像一覧")
    private List<PropertyAssetVO> assets;

    // === 文書一覧 ===
    @Schema(description = "文書一覧")
    private List<PropertyDocumentVO> documents;

    // === 審査情報 ===
    @Schema(description = "審査情報")
    private PropertyReviewVO review;

    // === 監査サマリー ===
    @Schema(description = "監査サマリー")
    private PropertyAuditSummaryVO auditSummary;

    // === 権限情報 ===
    @Schema(description = "権限情報（UI 表示制御用）")
    private PropertyPermissionsVO permissions;

    // === お気に入り状態 ===
    @Schema(description = "お気に入り状態（ログイン済み＋登録済み: true、未登録: false、未ログイン: null）")
    private Boolean isFavorite;
}
