package com.smartdx.property.testconfig.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * テスト設定レスポンスVO
 */
@Data
@Schema(description = "テスト設定")
public class TestConfigVO {

    @Schema(description = "ログインスキップフラグ")
    private Boolean skipLogin;

    @Schema(description = "外部APIモックフラグ")
    private Boolean mockExternalApis;
}
