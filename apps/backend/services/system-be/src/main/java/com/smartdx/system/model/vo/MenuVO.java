package com.smartdx.system.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * メニューVO
 */
@Schema(description = "メニューVO")
@Data
public class MenuVO {

    @Schema(description = "メニューID")
    private Long id;

    @Schema(description = "親メニューID")
    private Long parentId;

    @Schema(description = "メニュー名")
    private String name;

    @Schema(description = "メニュータイプ")
    private String type;

    @Schema(description = "ルート名")
    private String routeName;

    @Schema(description = "ルートパス")
    private String routePath;

    @Schema(description = "コンポーネントパス")
    private String component;

    @Schema(description = "ソート順")
    private Integer sort;

    @Schema(description = "表示状態")
    private Integer visible;

    @Schema(description = "メニュー範囲")
    private Integer scope;

    @Schema(description = "アイコン")
    private String icon;

    @Schema(description = "リダイレクト")
    private String redirect;

    @Schema(description = "権限識別子")
    private String perm;

    @Schema(description = "子メニュー")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<MenuVO> children;
}
