package com.smartdx.security.service;

import com.smartdx.security.model.UserDetails;
import com.smartdx.security.util.SecurityUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

/**
 * セキュリティサービス
 * Spring Security の @PreAuthorize アノテーションで使用
 * 例: @PreAuthorize("@ss.hasPerm('sys:notice:list')")
 *
 * @author jason.w
 */
@Component("ss")
public class SecurityService {

    /**
     * 指定されたパーミッションを持っているか確認
     *
     * @param permission パーミッション文字列 (例: "sys:notice:list")
     * @return true: 権限あり, false: 権限なし
     */
    public boolean hasPerm(String permission) {
        if (permission == null || permission.isEmpty()) {
            return false;
        }

        UserDetails user = SecurityUtils.getCurrentUser();
        if (user == null) {
            return false;
        }

        // ROOT または ADMIN ロールを持つユーザーは全権限を持つ
        Set<String> roleCodes = user.getRoleCodes();
        if (roleCodes != null && (roleCodes.contains("ROOT") || roleCodes.contains("ADMIN"))) {
            return true;
        }

        // ワイルドカード権限チェック (*:*:* = 全権限)
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        if (authorities == null || authorities.isEmpty()) {
            return false;
        }

        for (GrantedAuthority authority : authorities) {
            String auth = authority.getAuthority();
            // ROLE_プレフィックスを除去して比較
            if (auth.startsWith("ROLE_")) {
                auth = auth.substring(5);
            }
            if (auth.equals(permission) || auth.equals("*:*:*")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 指定されたロールを持っているか確認
     *
     * @param role ロール名 (例: "ADMIN")
     * @return true: 権限あり, false: 権限なし
     */
    public boolean hasRole(String role) {
        return SecurityUtils.hasRole(role);
    }

    /**
     * 指定されたロールのいずれかを持っているか確認
     *
     * @param roles ロール名の配列
     * @return true: いずれかの権限あり, false: 権限なし
     */
    public boolean hasAnyRole(String... roles) {
        return SecurityUtils.hasAnyRole(roles);
    }
}
