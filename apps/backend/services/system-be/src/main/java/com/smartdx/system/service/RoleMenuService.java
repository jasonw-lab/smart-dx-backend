package com.smartdx.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartdx.system.model.entity.RoleMenu;

import java.util.List;
import java.util.Set;

/**
 * 角色菜单 Service
 */
public interface RoleMenuService extends IService<RoleMenu> {

    /**
     * 获取角色的菜单ID列表
     */
    List<Long> listMenuIdsByRoleId(Long roleId);

    /**
     * 获取角色编码对应的权限列表
     */
    Set<String> listPermsByRoleCodes(Set<String> roleCodes);

    /**
     * 刷新角色权限缓存
     */
    void refreshRolePermsCache(String... roleCodes);
}
