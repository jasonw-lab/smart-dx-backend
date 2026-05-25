package com.smartdx.system.model.form;

import com.smartdx.core.model.KeyValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.util.List;

/**
 * メニュー表単
 */
@Schema(description = "メニュー表単")
@Data
public class MenuForm {

    @Schema(description = "メニューID")
    private Long id;

    @Schema(description = "親メニューID")
    private Long parentId;

    @Schema(description = "メニュー名")
    private String name;

    @Schema(description = "メニュータイプ（C-ディレクトリ M-メニュー B-ボタン）")
    private String type;

    @Schema(description = "ルート名")
    private String routeName;

    @Schema(description = "ルートパス")
    private String routePath;

    @Schema(description = "コンポーネントパス")
    private String component;

    @Schema(description = "権限識別子")
    private String perm;

    @Schema(description = "表示状態(1:表示;0:非表示)")
    @Range(max = 1, min = 0, message = "表示状態が不正です")
    private Integer visible;

    @Schema(description = "メニュー範囲(1=プラットフォーム 2=テナント)")
    @Range(max = 2, min = 1, message = "メニュー範囲が不正です")
    private Integer scope;

    @Schema(description = "ソート順")
    private Integer sort;

    @Schema(description = "アイコン")
    private String icon;

    @Schema(description = "リダイレクトパス")
    private String redirect;

    @Schema(description = "ページキャッシュ有効")
    private Integer keepAlive;

    @Schema(description = "常に表示")
    private Integer alwaysShow;

    @Schema(description = "ルートパラメータ")
    private List<KeyValue> params;
}
