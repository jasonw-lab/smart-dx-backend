package com.smartdx.property.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 監査サマリー
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "監査サマリー")
public class PropertyAuditSummaryVO {

    @Schema(description = "監査ログ件数")
    private Integer totalEntries;
}
