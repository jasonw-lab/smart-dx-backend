package com.smartdx.property.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 登録者情報
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登録者情報")
public class PropertyRegistrantVO {

    @Schema(description = "ユーザー ID")
    private String userId;

    @Schema(description = "表示名")
    private String displayName;
}
