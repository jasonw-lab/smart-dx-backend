package com.smartdx.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartdx.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * User entity
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {

    private String username;

    private String password;

    private String nickname;

    private String mobile;

    private String email;

    private Integer gender;

    private String avatar;

    /**
     * Status: 1=enabled, 0=disabled
     */
    private Integer status;

    private Long deptId;

    /**
     * Whether the user can switch tenants
     */
    private Boolean canSwitchTenant;
}
