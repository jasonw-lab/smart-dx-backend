package com.smartdx.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartdx.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Role entity
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class Role extends BaseEntity {

    private String name;

    private String code;

    private Integer sort;

    /**
     * Status: 1=enabled, 0=disabled
     */
    private Integer status;

    /**
     * Data scope: 0=all, 1=dept, 2=dept_and_sub, 3=self
     */
    private Integer dataScope;
}
