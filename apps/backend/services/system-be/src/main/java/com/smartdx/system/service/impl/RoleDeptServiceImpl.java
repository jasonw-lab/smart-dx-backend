package com.smartdx.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.security.util.SecurityUtils;
import com.smartdx.system.mapper.RoleDeptMapper;
import com.smartdx.system.model.entity.RoleDept;
import com.smartdx.system.service.RoleDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色部門 Service 実装
 */
@Service
@RequiredArgsConstructor
public class RoleDeptServiceImpl extends ServiceImpl<RoleDeptMapper, RoleDept> implements RoleDeptService {

    @Override
    public List<Long> getDeptIdsByRoleId(Long roleId) {
        return baseMapper.selectDeptIdsByRoleId(roleId);
    }

    @Override
    @Transactional
    public void saveRoleDepts(Long roleId, List<Long> deptIds) {
        // 既存の関連を削除
        deleteByRoleId(roleId);

        // 新しい関連を追加
        if (deptIds != null && !deptIds.isEmpty()) {
            Long tenantId = SecurityUtils.getTenantId();
            List<RoleDept> roleDepts = deptIds.stream()
                    .map(deptId -> new RoleDept(roleId, deptId, tenantId))
                    .toList();
            this.saveBatch(roleDepts);
        }
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        this.remove(new LambdaQueryWrapper<RoleDept>().eq(RoleDept::getRoleId, roleId));
    }
}
