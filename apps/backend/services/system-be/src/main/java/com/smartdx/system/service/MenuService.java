package com.smartdx.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartdx.core.model.Option;
import com.smartdx.system.model.entity.Menu;
import com.smartdx.system.model.form.MenuForm;
import com.smartdx.system.model.query.MenuQuery;
import com.smartdx.system.model.vo.MenuVO;
import com.smartdx.system.model.vo.RouteVO;

import java.util.List;
import java.util.Set;

/**
 * メニュー Service
 */
public interface MenuService extends IService<Menu> {

    /**
     * メニュー一覧取得
     */
    List<MenuVO> listMenus(MenuQuery queryParams);

    /**
     * メニュードロップダウンリスト
     */
    List<Option<Long>> listMenuOptions(boolean onlyParent, Integer scope);

    /**
     * 現在のユーザーのルート一覧
     */
    List<RouteVO> listCurrentUserRoutes();

    /**
     * メニューフォームデータ取得
     */
    MenuForm getMenuForm(Long id);

    /**
     * メニュー保存
     */
    boolean saveMenu(MenuForm menuForm);

    /**
     * メニュー削除
     */
    boolean deleteMenu(Long id);

    /**
     * メニュー表示状態更新
     */
    boolean updateMenuVisible(Long menuId, Integer visible);

    /**
     * ロールに基づく権限一覧取得
     */
    Set<String> listPermsByRoleCodes(Set<String> roleCodes);
}
