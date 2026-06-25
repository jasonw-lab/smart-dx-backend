package com.smartdx.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartdx.core.result.PageResult;
import com.smartdx.system.model.query.LogQuery;
import com.smartdx.system.model.vo.LogPageVO;
import com.smartdx.system.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ログコントローラー
 */
@Tag(name = "09.ログAPI")
@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @Operation(summary = "ログページリスト")
    @GetMapping("/page")
    public PageResult<LogPageVO> getLogPage(LogQuery query) {
        Page<LogPageVO> result = logService.getLogPage(query);
        return PageResult.success(result);
    }
}
