package com.smartdx.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartdx.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Tenant entity
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
public class Tenant extends BaseEntity {

    private String name;

    private String code;

    private String domain;

    /**
     * Status: 1=enabled, 0=disabled
     */
    private Integer status;

    /**
     * sys_tenant 自身がテナントマスタのため tenant_id カラムを持たない。
     * BaseEntity から継承した tenantId を DB マッピング対象外にする。
     */
    @TableField(exist = false)
    private Long tenantId;
}
