package com.smartdx.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartdx.system.model.entity.RoleDept;

import java.util.List;

/**
 * 角色部门 Service
 */
public interface RoleDeptService extends IService<RoleDept> {

    /**
     * 获取角色的部门ID列表
     */
    List<Long> getDeptIdsByRoleId(Long roleId);

    /**
     * 保存角色部门关联
     */
    void saveRoleDepts(Long roleId, List<Long> deptIds);

    /**
     * 删除角色的部门关联
     */
    void deleteByRoleId(Long roleId);
}
