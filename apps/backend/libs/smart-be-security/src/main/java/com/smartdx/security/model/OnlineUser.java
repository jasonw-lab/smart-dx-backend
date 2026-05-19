package com.smartdx.security.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * オンラインユーザー情報
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private Long deptId;
    private List<RoleDataScope> dataScopes;
    private Long tenantId;
    private Boolean canSwitchTenant;
    private Set<String> roles;
}
