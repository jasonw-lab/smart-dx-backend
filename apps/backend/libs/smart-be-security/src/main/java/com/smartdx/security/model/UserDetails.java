package com.smartdx.security.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ユーザー詳細情報 (Spring Security UserDetails 実装)
 */
public class UserDetails implements org.springframework.security.core.userdetails.UserDetails, Serializable {

    private static final long serialVersionUID = 1L;
    private static final String ROLE_PREFIX = "ROLE_";

    private Long userId;
    private Long tenantId;
    private Long deptId;
    private String username;
    private String password;
    private String nickname;
    private Integer status;
    private Set<String> roleCodes;
    private Collection<SimpleGrantedAuthority> authorities;

    public UserDetails() {
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (authorities != null) {
            return authorities;
        }
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Collections.emptySet();
        }
        return roleCodes.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.status != null && this.status == 1;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Set<String> getRoleCodes() {
        return roleCodes;
    }

    public void setRoleCodes(Set<String> roleCodes) {
        this.roleCodes = roleCodes;
    }

    public void setAuthorities(Collection<SimpleGrantedAuthority> authorities) {
        this.authorities = authorities;
    }
}
