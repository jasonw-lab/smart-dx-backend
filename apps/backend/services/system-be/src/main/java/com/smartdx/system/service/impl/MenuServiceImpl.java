package com.smartdx.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.core.model.Option;
import com.smartdx.security.util.SecurityUtils;
import com.smartdx.system.converter.MenuConverter;
import com.smartdx.system.mapper.MenuMapper;
import com.smartdx.system.model.entity.Menu;
import com.smartdx.system.model.form.MenuForm;
import com.smartdx.system.model.query.MenuQuery;
import com.smartdx.system.model.vo.MenuVO;
import com.smartdx.system.model.vo.RouteVO;
import com.smartdx.system.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * メニュー Service 実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    private final MenuConverter menuConverter;

    private static final String MENU_TYPE_CATALOG = "C";
    private static final String MENU_TYPE_MENU = "M";
    private static final String MENU_TYPE_BUTTON = "B";

    @Override
    public List<MenuVO> listMenus(MenuQuery queryParams) {
        List<Menu> menuList = this.list(new LambdaQueryWrapper<Menu>()
                .like(StrUtil.isNotBlank(queryParams.getKeywords()), Menu::getName, queryParams.getKeywords())
                .eq(queryParams.getScope() != null, Menu::getScope, queryParams.getScope())
                .orderByAsc(Menu::getSort)
        );

        List<MenuVO> voList = menuConverter.toVoList(menuList);
        return buildMenuTree(voList);
    }

    @Override
    public List<Option<Long>> listMenuOptions(boolean onlyParent, Integer scope) {
        LambdaQueryWrapper<Menu> query = new LambdaQueryWrapper<Menu>()
                .eq(scope != null, Menu::getScope, scope)
                .select(Menu::getId, Menu::getParentId, Menu::getName);

        if (onlyParent) {
            query.in(Menu::getType, MENU_TYPE_CATALOG, MENU_TYPE_MENU);
        }

        query.orderByAsc(Menu::getSort);
        List<Menu> menuList = this.list(query);

        return buildOptionTree(menuList, 0L);
    }

    @Override
    @Cacheable(cacheNames = "menu", key = "'routes:' + T(com.smartdx.security.util.SecurityUtils).getCurrentUserId()")
    public List<RouteVO> listCurrentUserRoutes() {
        Set<String> roleCodes = SecurityUtils.getRoles();
        if (CollectionUtil.isEmpty(roleCodes)) {
            return Collections.emptyList();
        }

        // ROOT の場合は全メニューを取得
        List<Menu> menuList;
        if (SecurityUtils.isRoot()) {
            menuList = this.list(new LambdaQueryWrapper<Menu>()
                    .in(Menu::getType, MENU_TYPE_CATALOG, MENU_TYPE_MENU)
                    .eq(Menu::getVisible, 1)
                    .orderByAsc(Menu::getSort));
        } else {
            menuList = baseMapper.selectMenusByRoleCodes(roleCodes);
            menuList = menuList.stream()
                    .filter(m -> !MENU_TYPE_BUTTON.equals(m.getType()) && m.getVisible() == 1)
                    .toList();
        }

        return buildRouteTree(menuList, 0L);
    }

    @Override
    public MenuForm getMenuForm(Long id) {
        Menu menu = this.getById(id);
        return menuConverter.toForm(menu);
    }

    @Override
    @CacheEvict(cacheNames = "menu", allEntries = true)
    public boolean saveMenu(MenuForm menuForm) {
        Menu menu = menuConverter.toEntity(menuForm);

        // ツリーパスの生成
        if (menu.getParentId() != null && menu.getParentId() > 0) {
            Menu parent = this.getById(menu.getParentId());
            if (parent != null) {
                menu.setTreePath(parent.getTreePath() + "," + parent.getId());
            }
        } else {
            menu.setTreePath("0");
            menu.setParentId(0L);
        }

        return this.saveOrUpdate(menu);
    }

    @Override
    @CacheEvict(cacheNames = "menu", allEntries = true)
    public boolean deleteMenu(Long id) {
        // 子メニューの確認
        long childCount = this.count(new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, id));
        if (childCount > 0) {
            throw new RuntimeException("存在子菜单，无法删除");
        }
        return this.removeById(id);
    }

    @Override
    @CacheEvict(cacheNames = "menu", allEntries = true)
    public boolean updateMenuVisible(Long menuId, Integer visible) {
        return this.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Menu>()
                .eq(Menu::getId, menuId)
                .set(Menu::getVisible, visible));
    }

    @Override
    public Set<String> listPermsByRoleCodes(Set<String> roleCodes) {
        if (CollectionUtil.isEmpty(roleCodes)) {
            return Collections.emptySet();
        }
        return baseMapper.selectPermsByRoleCodes(roleCodes);
    }

    /**
     * メニューツリー構築
     */
    private List<MenuVO> buildMenuTree(List<MenuVO> menus) {
        Map<Long, MenuVO> menuMap = menus.stream()
                .collect(Collectors.toMap(MenuVO::getId, m -> m));

        List<MenuVO> result = new ArrayList<>();
        for (MenuVO menu : menus) {
            if (menu.getParentId() == null || menu.getParentId() == 0) {
                result.add(menu);
            } else {
                MenuVO parent = menuMap.get(menu.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(menu);
                }
            }
        }
        return result;
    }

    /**
     * オプションツリー構築
     */
    private List<Option<Long>> buildOptionTree(List<Menu> menus, Long parentId) {
        List<Option<Long>> options = new ArrayList<>();
        for (Menu menu : menus) {
            if (Objects.equals(menu.getParentId(), parentId)) {
                List<Option<Long>> children = buildOptionTree(menus, menu.getId());
                Option<Long> option = new Option<>(menu.getId(), menu.getName(),
                        children.isEmpty() ? null : children);
                options.add(option);
            }
        }
        return options;
    }

    /**
     * ルートツリー構築
     */
    private List<RouteVO> buildRouteTree(List<Menu> menus, Long parentId) {
        List<RouteVO> routes = new ArrayList<>();
        for (Menu menu : menus) {
            if (Objects.equals(menu.getParentId(), parentId)) {
                RouteVO route = new RouteVO();
                route.setPath(menu.getRoutePath());
                route.setComponent(menu.getComponent());
                route.setRedirect(menu.getRedirect());
                route.setName(menu.getRouteName());

                RouteVO.Meta meta = new RouteVO.Meta();
                meta.setTitle(menu.getName());
                meta.setIcon(menu.getIcon());
                meta.setHidden(menu.getVisible() == 0);
                meta.setKeepAlive(menu.getKeepAlive() != null && menu.getKeepAlive() == 1);
                meta.setAlwaysShow(menu.getAlwaysShow() != null && menu.getAlwaysShow() == 1);
                if (menu.getParams() != null) {
                    meta.setParams(menu.getParams().entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue()))));
                }
                route.setMeta(meta);

                List<RouteVO> children = buildRouteTree(menus, menu.getId());
                if (!children.isEmpty()) {
                    route.setChildren(children);
                }

                routes.add(route);
            }
        }
        return routes;
    }
}
