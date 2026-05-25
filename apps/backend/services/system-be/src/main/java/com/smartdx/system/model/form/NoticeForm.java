package com.smartdx.system.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.util.List;

/**
 * 通知フォーム
 */
@Schema(description = "通知フォーム")
@Data
public class NoticeForm {

    @Schema(description = "通知ID")
    private Long id;

    @Schema(description = "タイトル")
    @NotBlank(message = "タイトルは必須です")
    @Size(max = 50, message = "タイトルは50文字以内で入力してください")
    private String title;

    @Schema(description = "内容")
    @NotBlank(message = "内容は必須です")
    private String content;

    @Schema(description = "タイプ")
    private Integer type;

    @Schema(description = "優先度 (L: 低, M: 中, H: 高)")
    private String level;

    @Schema(description = "対象タイプ (1: 全員, 2: 指定)")
    @Range(min = 1, max = 2, message = "対象タイプは1または2を指定してください")
    private Integer targetType;

    @Schema(description = "対象ユーザーIDリスト")
    private List<String> targetUserIds;
}
