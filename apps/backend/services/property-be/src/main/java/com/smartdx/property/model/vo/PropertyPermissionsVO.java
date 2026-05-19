package com.smartdx.property.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 権限情報（UI 表示制御用）
 */
@Getter
@Setter
@Schema(description = "権限情報（UI 表示制御用）")
public class PropertyPermissionsVO {

    @Schema(description = "審査メモタブ表示可否")
    private Boolean canViewReviewMemo;

    @Schema(description = "審査メモ編集可否")
    private Boolean canEditReviewMemo;

    @Schema(description = "機微文書（重説/契約書/査定書）閲覧可否")
    private Boolean canViewSensitiveDocs;

    @Schema(description = "原本ダウンロード可否")
    private Boolean canDownloadRaw;

    @Schema(description = "下書き破棄可否")
    private Boolean canDeleteDraft;

    @Schema(description = "修正再提出可否")
    private Boolean canResubmit;

    /**
     * デフォルト権限を生成
     */
    public static PropertyPermissionsVO defaults() {
        PropertyPermissionsVO vo = new PropertyPermissionsVO();
        vo.setCanViewReviewMemo(false);
        vo.setCanEditReviewMemo(false);
        vo.setCanViewSensitiveDocs(false);
        vo.setCanDownloadRaw(true);
        vo.setCanDeleteDraft(false);
        vo.setCanResubmit(false);
        return vo;
    }
}
