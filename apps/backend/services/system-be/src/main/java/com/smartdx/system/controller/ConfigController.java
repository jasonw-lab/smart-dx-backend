package com.smartdx.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartdx.core.result.PageResult;
import com.smartdx.core.result.Result;
import com.smartdx.system.model.form.ConfigForm;
import com.smartdx.system.model.query.ConfigQuery;
import com.smartdx.system.model.vo.ConfigPageVO;
import com.smartdx.system.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * システム設定コントローラー
 */
@Tag(name = "10.システム設定API")
@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @Operation(summary = "システム設定ページリスト")
    @GetMapping({"", "/page"})
    public PageResult<ConfigPageVO> getConfigPage(ConfigQuery query) {
        Page<ConfigPageVO> result = configService.getConfigPage(query);
        return PageResult.success(result);
    }

    @Operation(summary = "システム設定フォーム取得")
    @GetMapping("/{id}/form")
    public Result<ConfigForm> getConfigForm(
            @Parameter(description = "設定ID") @PathVariable Long id
    ) {
        ConfigForm form = configService.getConfigForm(id);
        return Result.success(form);
    }

    @Operation(summary = "システム設定新規作成")
    @PostMapping
    public Result<?> saveConfig(@RequestBody @Valid ConfigForm form) {
        boolean result = configService.saveConfig(form);
        return Result.judge(result);
    }

    @Operation(summary = "システム設定更新")
    @PutMapping("/{id}")
    public Result<Void> updateConfig(
            @Parameter(description = "設定ID") @PathVariable Long id,
            @RequestBody @Valid ConfigForm form
    ) {
        boolean result = configService.updateConfig(id, form);
        return Result.judge(result);
    }

    @Operation(summary = "システム設定削除")
    @DeleteMapping("/{ids}")
    public Result<Void> deleteConfigs(
            @Parameter(description = "設定ID（複数はカンマ区切り）") @PathVariable String ids
    ) {
        boolean result = configService.deleteConfigs(ids);
        return Result.judge(result);
    }

    @Operation(summary = "システム設定キャッシュ更新")
    @PutMapping("/refresh")
    public Result<Void> refreshCache() {
        return Result.success();
    }
}
