package com.smartdx.system.model.query;

import com.smartdx.core.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 辞書クエリ
 */
@Schema(description = "辞書クエリ")
@Getter
@Setter
public class DictQuery extends BaseQuery {

    @Schema(description = "キーワード")
    private String keywords;
}
