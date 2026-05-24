package com.smartdx.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartdx.core.model.Option;
import com.smartdx.system.model.entity.Role;
import com.smartdx.system.model.form.RoleForm;
import com.smartdx.system.model.query.RoleQuery;
import com.smartdx.system.model.vo.RolePageVO;

import java.util.List;
import java.util.Set;

/**
 * 角色 Service
 */
public interface RoleService extends IService<Role> {

    /**
     * 角色分页列表
     */
    Page<RolePageVO> getRolePage(RoleQuery queryParams);

    /**
     * 角色下拉列表
     */
    List<Option<Long>> listRoleOptions();

    /**
     * 保存角色
     */
    boolean saveRole(RoleForm roleForm);

    /**
     * 获取角色表单数据
     */
    RoleForm getRoleForm(Long roleId);

    /**
     * 修改角色状态
     */
    boolean updateRoleStatus(Long roleId, Integer status);

    /**
     * 批量删除角色
     */
    void deleteRoles(String ids);

    /**
     * 获取角色的菜单ID集合
     */
    List<Long> getRoleMenuIds(Long roleId);

    /**
     * 角色分配菜单权限
     */
    void assignMenusToRole(Long roleId, List<Long> menuIds);

    /**
     * 获取最大范围的数据权限
     */
    Integer getMaximumDataScope(Set<String> roles);

    /**
     * 获取角色的部门ID列表（自定义数据权限）
     */
    List<Long> getRoleDeptIds(Long roleId);
}
