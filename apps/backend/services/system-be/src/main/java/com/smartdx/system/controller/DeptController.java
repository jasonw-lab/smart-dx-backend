package com.smartdx.system.controller;

import com.smartdx.core.model.Option;
import com.smartdx.core.result.Result;
import com.smartdx.system.model.form.DeptForm;
import com.smartdx.system.model.query.DeptQuery;
import com.smartdx.system.model.vo.DeptVO;
import com.smartdx.system.service.DeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部門コントローラー
 */
@Tag(name = "部門API")
@RestController
@RequestMapping("/api/v1/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @Operation(summary = "部門一覧")
    @GetMapping
    public Result<List<DeptVO>> getDepts(DeptQuery queryParams) {
        List<DeptVO> deptList = deptService.listDepts(queryParams);
        return Result.success(deptList);
    }

    @Operation(summary = "部門ドロップダウン")
    @GetMapping("/options")
    public Result<List<Option<Long>>> getDeptOptions() {
        List<Option<Long>> options = deptService.listDeptOptions();
        return Result.success(options);
    }

    @Operation(summary = "部門フォームデータ")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('sys:dept:update')")
    public Result<DeptForm> getDeptForm(
            @Parameter(description = "部門ID") @PathVariable Long id
    ) {
        DeptForm deptForm = deptService.getDeptForm(id);
        return Result.success(deptForm);
    }

    @Operation(summary = "部門追加")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:dept:create')")
    public Result<?> addDept(@Valid @RequestBody DeptForm deptForm) {
        boolean result = deptService.saveDept(deptForm);
        return Result.judge(result);
    }

    @Operation(summary = "部門更新")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:dept:update')")
    public Result<?> updateDept(@Valid @RequestBody DeptForm deptForm) {
        boolean result = deptService.saveDept(deptForm);
        return Result.judge(result);
    }

    @Operation(summary = "部門削除")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('sys:dept:delete')")
    public Result<?> deleteDepts(
            @Parameter(description = "部門ID、カンマ区切り") @PathVariable String ids
    ) {
        boolean result = deptService.deleteDepts(ids);
        return Result.judge(result);
    }
}
