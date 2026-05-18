package com.smartdx.security.util;

import com.smartdx.security.model.UserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
     * 現在のユーザーIDを取得
     */
    public static Long getCurrentUserId() {
        UserDetails user = getCurrentUser();
        return user != null ? user.getUserId() : null;
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
}
