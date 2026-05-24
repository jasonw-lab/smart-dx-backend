package com.smartdx.system.model.query;

import com.smartdx.core.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 通知クエリ
 */
@Schema(description = "通知クエリ")
@Getter
@Setter
public class NoticeQuery extends BaseQuery {

    @Schema(description = "タイトル")
    private String title;

    @Schema(description = "発行状態 (0: 未発行, 1: 発行済み, -1: 取り消し済み)")
    private Integer publishStatus;

    @Schema(description = "発行日時範囲")
    private List<String> publishTime;

    @Schema(description = "ユーザーID")
    private Long userId;

    @Schema(description = "既読状態 (0: 未読, 1: 既読)")
    private Integer isRead;
}
