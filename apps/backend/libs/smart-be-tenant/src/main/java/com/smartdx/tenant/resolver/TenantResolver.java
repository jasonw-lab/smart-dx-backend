package com.smartdx.tenant.resolver;

import jakarta.servlet.http.HttpServletRequest;

/**
 * テナントID解決インターフェース
 * <p>
 * リクエストからテナントIDを解決する。複数の解決方法をチェーン可能。
 * </p>
 */
public interface TenantResolver {

    /**
     * リクエストからテナントIDを解決
     *
     * @param request HTTPリクエスト
     * @return テナントID、解決できない場合はnull
     */
    Long resolve(HttpServletRequest request);

    /**
     * 解決優先順位 (小さいほど優先)
     */
    default int getOrder() {
        return 100;
    }
}
