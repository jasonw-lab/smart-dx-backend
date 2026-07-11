package com.smartdx.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartdx.core.model.Option;
import com.smartdx.core.result.PageResult;
import com.smartdx.core.result.Result;
import com.smartdx.system.model.form.DictForm;
import com.smartdx.system.model.form.DictItemForm;
import com.smartdx.system.model.query.DictItemQuery;
import com.smartdx.system.model.query.DictQuery;
import com.smartdx.system.model.vo.DictItemOptionVO;
import com.smartdx.system.model.vo.DictItemPageVO;
import com.smartdx.system.model.vo.DictPageVO;
import com.smartdx.system.service.DictItemService;
import com.smartdx.system.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 辞書コントローラー
 */
@Tag(name = "06.辞書API")
@RestController
@RequestMapping("/api/v1/dicts")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;
    private final DictItemService dictItemService;

    // ---------------------------------------------------
    // 辞書API
    // ---------------------------------------------------

    @Operation(summary = "辞書ページリスト")
    @GetMapping({"", "/page"})
    public PageResult<DictPageVO> getDictPage(DictQuery query) {
        Page<DictPageVO> result = dictService.getDictPage(query);
        return PageResult.success(result);
    }

    @Operation(summary = "辞書オプションリスト")
    @GetMapping("/options")
    public Result<List<Option<String>>> getDictOptions() {
        List<Option<String>> list = dictService.getDictOptions();
        return Result.success(list);
    }

    @Operation(summary = "辞書フォーム取得")
    @GetMapping("/{id}/form")
    public Result<DictForm> getDictForm(
            @Parameter(description = "辞書ID") @PathVariable Long id
    ) {
        DictForm formData = dictService.getDictForm(id);
        return Result.success(formData);
    }

    @Operation(summary = "辞書新規作成")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:dict:create')")
    public Result<?> saveDict(@Valid @RequestBody DictForm form) {
        boolean result = dictService.saveDict(form);
        return Result.judge(result);
    }

    @Operation(summary = "辞書更新")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:dict:update')")
    public Result<?> updateDict(
            @PathVariable Long id,
            @RequestBody DictForm form
    ) {
        boolean status = dictService.updateDict(id, form);
        return Result.judge(status);
    }

    @Operation(summary = "辞書削除")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('sys:dict:delete')")
    public Result<?> deleteDicts(
            @Parameter(description = "辞書ID（複数はカンマ区切り）") @PathVariable String ids
    ) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        dictService.deleteDictByIds(idList);
        return Result.success();
    }

    // ---------------------------------------------------
    // 辞書項目API
    // ---------------------------------------------------

    @Operation(summary = "辞書項目ページリスト")
    @GetMapping({"/{dictCode}/items", "/{dictCode}/items/page"})
    public PageResult<DictItemPageVO> getDictItemPage(
            @Parameter(description = "辞書コード") @PathVariable String dictCode,
            DictItemQuery query
    ) {
        query.setDictCode(dictCode);
        Page<DictItemPageVO> result = dictItemService.getDictItemPage(query);
        return PageResult.success(result);
    }

    @Operation(summary = "辞書項目オプションリスト")
    @GetMapping("/{dictCode}/items/options")
    public Result<List<DictItemOptionVO>> getDictItemOptions(
            @Parameter(description = "辞書コード") @PathVariable String dictCode
    ) {
        List<DictItemOptionVO> list = dictItemService.getDictItemOptions(dictCode);
        return Result.success(list);
    }

    @Operation(summary = "辞書項目フォーム取得")
    @GetMapping("/{dictCode}/items/{itemId}/form")
    public Result<DictItemForm> getDictItemForm(
            @Parameter(description = "辞書コード") @PathVariable String dictCode,
            @Parameter(description = "辞書項目ID") @PathVariable Long itemId
    ) {
        DictItemForm formData = dictItemService.getDictItemForm(itemId);
        return Result.success(formData);
    }

    @Operation(summary = "辞書項目新規作成")
    @PostMapping("/{dictCode}/items")
    @PreAuthorize("@ss.hasPerm('sys:dict-item:create')")
    public Result<Void> saveDictItem(
            @Parameter(description = "辞書コード") @PathVariable String dictCode,
            @Valid @RequestBody DictItemForm form
    ) {
        form.setDictCode(dictCode);
        boolean result = dictItemService.saveDictItem(form);
        return Result.judge(result);
    }

    @Operation(summary = "辞書項目更新")
    @PutMapping("/{dictCode}/items/{itemId}")
    @PreAuthorize("@ss.hasPerm('sys:dict-item:update')")
    public Result<?> updateDictItem(
            @Parameter(description = "辞書コード") @PathVariable String dictCode,
            @PathVariable Long itemId,
            @RequestBody DictItemForm form
    ) {
        form.setId(itemId);
        form.setDictCode(dictCode);
        boolean status = dictItemService.updateDictItem(form);
        return Result.judge(status);
    }

    @Operation(summary = "辞書項目削除")
    @DeleteMapping("/{dictCode}/items/{itemIds}")
    @PreAuthorize("@ss.hasPerm('sys:dict-item:delete')")
    public Result<Void> deleteDictItems(
            @Parameter(description = "辞書コード") @PathVariable String dictCode,
            @Parameter(description = "辞書項目ID（複数はカンマ区切り）") @PathVariable String itemIds
    ) {
        List<Long> idList = Arrays.stream(itemIds.split(","))
                .map(Long::parseLong)
                .toList();
        dictItemService.deleteDictItemByIds(idList);
        return Result.success();
    }
}
