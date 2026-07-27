package com.smartdx.property.testconfig.controller;

import com.smartdx.core.result.Result;
import com.smartdx.property.testconfig.model.req.TestConfigUpdateReq;
import com.smartdx.property.testconfig.model.vo.TestConfigVO;
import com.smartdx.property.testconfig.service.TestConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * E2Eテスト設定コントローラー
 *
 * テスト系プロファイル (dev / e2e / test) でのみ有効。
 * ブラックリスト方式 (!prod) だと本番が docker 等の別プロファイルで起動した際に
 * 有効化されてしまうため、必ずホワイトリスト方式で指定する。
 */
@Tag(name = "システムテスト設定")
@RestController
@RequestMapping("/api/v1/system/test-config")
@Profile({"dev", "e2e", "test"})
@RequiredArgsConstructor
@Slf4j
public class TestConfigController {

    private final TestConfigService testConfigService;

    @Operation(summary = "テスト設定取得", description = "SYS-TEST-01: 現在のテスト設定を取得")
    @GetMapping
    public Result<TestConfigVO> getConfig() {
        log.info("API call: GET /api/v1/system/test-config");
        return Result.success(testConfigService.getConfig());
    }

    @Operation(summary = "テスト設定更新", description = "SYS-TEST-02: テスト設定を更新")
    @PutMapping
    public Result<TestConfigVO> updateConfig(@RequestBody TestConfigUpdateReq req) {
        log.info("API call: PUT /api/v1/system/test-config. req={}", req);
        return Result.success(testConfigService.updateConfig(req));
    }
}
