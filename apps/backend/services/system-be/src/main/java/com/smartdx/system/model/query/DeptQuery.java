package com.smartdx.system.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 部門クエリ
 */
@Schema(description = "部門クエリ")
@Data
public class DeptQuery {

    @Schema(description = "キーワード")
    private String keywords;

    @Schema(description = "状態")
    private Integer status;
}
