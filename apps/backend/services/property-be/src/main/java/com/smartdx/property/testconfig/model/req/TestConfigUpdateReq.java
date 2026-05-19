package com.smartdx.property.testconfig.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * テスト設定更新リクエスト
 */
@Data
@Schema(description = "テスト設定更新リクエスト")
public class TestConfigUpdateReq {

    @Schema(description = "ログインスキップフラグ")
    private Boolean skipLogin;

    @Schema(description = "外部APIモックフラグ")
    private Boolean mockExternalApis;
}
