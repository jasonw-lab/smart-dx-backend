package com.smartdx.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.security.util.SecurityUtils;
import com.smartdx.system.mapper.UserRoleMapper;
import com.smartdx.system.model.entity.UserRole;
import com.smartdx.system.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户角色 Service 実装
 */
@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {

    @Override
    public List<Long> listUserIdsByRoleId(Long roleId) {
        return baseMapper.selectUserIdsByRoleId(roleId);
    }

    @Override
    public boolean hasAssignedUsers(Long roleId) {
        return baseMapper.existsByRoleId(roleId);
    }

    @Override
    public List<Long> listRoleIdsByUserId(Long userId) {
        return baseMapper.selectRoleIdsByUserId(userId);
    }

    @Override
    @Transactional
    public void saveUserRoles(Long userId, List<Long> roleIds) {
        // 既存の関連を削除
        this.remove(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));

        // 新しい関連を追加
        if (roleIds != null && !roleIds.isEmpty()) {
            Long tenantId = SecurityUtils.getTenantId();
            List<UserRole> userRoles = roleIds.stream()
                    .map(roleId -> new UserRole(userId, roleId, tenantId))
                    .toList();
            this.saveBatch(userRoles);
        }
    }
}
