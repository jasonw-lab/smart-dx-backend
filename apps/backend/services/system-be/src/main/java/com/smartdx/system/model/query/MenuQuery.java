package com.smartdx.system.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * メニュークエリ
 */
@Schema(description = "メニュークエリ")
@Data
public class MenuQuery {

    @Schema(description = "キーワード")
    private String keywords;

    @Schema(description = "メニュー範囲(1=プラットフォーム 2=テナント)")
    private Integer scope;
}
