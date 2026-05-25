package com.smartdx.system.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * ルートVO
 */
@Schema(description = "ルートVO")
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RouteVO {

    @Schema(description = "ルートパス")
    private String path;

    @Schema(description = "コンポーネントパス")
    private String component;

    @Schema(description = "リダイレクト")
    private String redirect;

    @Schema(description = "ルート名")
    private String name;

    @Schema(description = "ルートメタ情報")
    private Meta meta;

    @Schema(description = "子ルート")
    private List<RouteVO> children;

    @Data
    public static class Meta {

        @Schema(description = "タイトル")
        private String title;

        @Schema(description = "アイコン")
        private String icon;

        @Schema(description = "非表示フラグ")
        private Boolean hidden;

        @Schema(description = "ページキャッシュ")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Boolean keepAlive;

        @Schema(description = "常に表示")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Boolean alwaysShow;

        @Schema(description = "ルートパラメータ")
        private Map<String, String> params;
    }
}
