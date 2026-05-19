package com.smartdx.security.util;

import com.smartdx.security.model.RoleDataScope;
import com.smartdx.security.model.UserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * セキュリティユーティリティ
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 現在のユーザー情報を取得
     */
    public static UserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return (UserDetails) principal;
        }
        return null;
    }

    /**
     * 現在のユーザー情報を取得 (Optional)
     */
    public static Optional<UserDetails> getUser() {
        return Optional.ofNullable(getCurrentUser());
    }

    /**
     * 現在のユーザーIDを取得
     */
    public static Long getCurrentUserId() {
        UserDetails user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 現在のユーザーIDを取得 (alias)
     */
    public static Long getUserId() {
        return getCurrentUserId();
    }

    /**
     * 現在のテナントIDを取得
     */
    public static Long getCurrentTenantId() {
        UserDetails user = getCurrentUser();
        return user != null ? user.getTenantId() : null;
    }

    /**
     * 現在のユーザー名を取得
     */
    public static String getCurrentUsername() {
        UserDetails user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    /**
     * 現在のユーザーのロールを取得
     */
    public static Set<String> getRoles() {
        UserDetails user = getCurrentUser();
        return user != null && user.getRoleCodes() != null ? user.getRoleCodes() : Collections.emptySet();
    }

    /**
     * 現在のユーザーのDept IDを取得
     */
    public static Long getDeptId() {
        UserDetails user = getCurrentUser();
        return user != null ? user.getDeptId() : null;
    }

    /**
     * 指定されたロールを持っているか確認
     */
    public static boolean hasRole(String role) {
        return getRoles().contains(role);
    }

    /**
     * 指定されたロールのいずれかを持っているか確認
     */
    public static boolean hasAnyRole(String... roles) {
        Set<String> userRoles = getRoles();
        for (String role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ROOTユーザーかどうかを確認
     */
    public static boolean isRoot() {
        UserDetails user = getCurrentUser();
        if (user == null) {
            return false;
        }
        return hasRole("ROOT") || hasRole("ADMIN");
    }

    /**
     * 現在のユーザーのデータスコープリストを取得
     */
    public static List<RoleDataScope> getDataScopes() {
        UserDetails user = getCurrentUser();
        if (user == null) {
            return Collections.emptyList();
        }
        List<RoleDataScope> dataScopes = user.getDataScopes();
        return dataScopes != null ? dataScopes : Collections.emptyList();
    }
}
