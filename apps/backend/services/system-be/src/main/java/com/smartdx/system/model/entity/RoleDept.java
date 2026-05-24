package com.smartdx.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色部门关联实体（自定义数据权限）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_role_dept")
public class RoleDept {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roleId;

    private Long deptId;

    private Long tenantId;

    public RoleDept(Long roleId, Long deptId, Long tenantId) {
        this.roleId = roleId;
        this.deptId = deptId;
        this.tenantId = tenantId;
    }
}
