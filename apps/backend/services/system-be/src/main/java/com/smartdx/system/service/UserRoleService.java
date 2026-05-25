package com.smartdx.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartdx.system.model.entity.UserRole;

import java.util.List;

/**
 * 用户角色 Service
 */
public interface UserRoleService extends IService<UserRole> {

    /**
     * 获取角色关联的用户ID列表
     */
    List<Long> listUserIdsByRoleId(Long roleId);

    /**
     * 判断角色是否已分配用户
     */
    boolean hasAssignedUsers(Long roleId);

    /**
     * 获取用户的角色ID列表
     */
    List<Long> listRoleIdsByUserId(Long userId);

    /**
     * 保存用户角色关联
     */
    void saveUserRoles(Long userId, List<Long> roleIds);
}
