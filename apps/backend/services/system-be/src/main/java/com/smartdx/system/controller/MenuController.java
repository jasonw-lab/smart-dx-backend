package com.smartdx.system.controller;

import com.smartdx.core.model.Option;
import com.smartdx.core.result.Result;
import com.smartdx.system.model.form.MenuForm;
import com.smartdx.system.model.query.MenuQuery;
import com.smartdx.system.model.vo.MenuVO;
import com.smartdx.system.model.vo.RouteVO;
import com.smartdx.system.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * メニューコントローラー
 */
@Tag(name = "メニューAPI")
@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "メニュー一覧")
    @GetMapping
    public Result<List<MenuVO>> getMenus(MenuQuery queryParams) {
        List<MenuVO> menuList = menuService.listMenus(queryParams);
        return Result.success(menuList);
    }

    @Operation(summary = "メニュードロップダウン")
    @GetMapping("/options")
    public Result<List<Option<Long>>> getMenuOptions(
            @Parameter(description = "親メニューのみ") @RequestParam(required = false, defaultValue = "false") boolean onlyParent,
            @Parameter(description = "メニュー範囲") @RequestParam(required = false) Integer scope
    ) {
        List<Option<Long>> menus = menuService.listMenuOptions(onlyParent, scope);
        return Result.success(menus);
    }

    @Operation(summary = "現在のユーザーのルート一覧")
    @GetMapping("/routes")
    public Result<List<RouteVO>> getCurrentUserRoutes() {
        List<RouteVO> routeList = menuService.listCurrentUserRoutes();
        return Result.success(routeList);
    }

    @Operation(summary = "メニューフォームデータ")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('sys:menu:update')")
    public Result<MenuForm> getMenuForm(
            @Parameter(description = "メニューID") @PathVariable Long id
    ) {
        MenuForm menu = menuService.getMenuForm(id);
        return Result.success(menu);
    }

    @Operation(summary = "メニュー追加")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:menu:create')")
    public Result<?> addMenu(@RequestBody MenuForm menuForm) {
        boolean result = menuService.saveMenu(menuForm);
        return Result.judge(result);
    }

    @Operation(summary = "メニュー更新")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:menu:update')")
    public Result<?> updateMenu(@RequestBody MenuForm menuForm) {
        boolean result = menuService.saveMenu(menuForm);
        return Result.judge(result);
    }

    @Operation(summary = "メニュー削除")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:menu:delete')")
    public Result<?> deleteMenu(
            @Parameter(description = "メニューID") @PathVariable Long id
    ) {
        boolean result = menuService.deleteMenu(id);
        return Result.judge(result);
    }

    @Operation(summary = "メニュー表示状態更新")
    @PatchMapping("/{menuId}")
    @PreAuthorize("@ss.hasPerm('sys:menu:update')")
    public Result<?> updateMenuVisible(
            @Parameter(description = "メニューID") @PathVariable Long menuId,
            @Parameter(description = "表示状態") Integer visible
    ) {
        boolean result = menuService.updateMenuVisible(menuId, visible);
        return Result.judge(result);
    }
}
