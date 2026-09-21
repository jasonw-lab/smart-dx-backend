package com.smartdx.system.model.query;

import com.smartdx.core.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * システム設定クエリ
 */
@Schema(description = "システム設定クエリ")
@Getter
@Setter
public class ConfigQuery extends BaseQuery {

    @Schema(description = "キーワード")
    private String keywords;
}
