package com.smartdx.system.model.query;

import com.smartdx.core.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 辞書項目クエリ
 */
@Schema(description = "辞書項目クエリ")
@Getter
@Setter
public class DictItemQuery extends BaseQuery {

    @Schema(description = "辞書コード")
    private String dictCode;

    @Schema(description = "キーワード")
    private String keywords;
}
