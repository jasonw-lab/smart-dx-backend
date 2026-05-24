package com.smartdx.system.model.query;

import com.smartdx.core.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * ログクエリ
 */
@Schema(description = "ログクエリ")
@Getter
@Setter
public class LogQuery extends BaseQuery {

    @Schema(description = "キーワード（タイトル/内容/IP/操作者）")
    private String keywords;

    @Schema(description = "操作時間範囲")
    private List<String> createTime;
}
