package com.smartdx.security.constant;

/**
 * セキュリティ定数
 */
public final class SecurityConstants {

    private SecurityConstants() {
    }

    /**
     * Bearer トークンプレフィックス
     */
    public static final String BEARER_TOKEN_PREFIX = "Bearer ";

    /**
     * ロールプレフィックス
     */
    public static final String ROLE_PREFIX = "ROLE_";

    /**
     * JWT Claim: ユーザーID
     */
    public static final String JWT_CLAIM_USER_ID = "userId";

    /**
     * JWT Claim: テナントID
     */
    public static final String JWT_CLAIM_TENANT_ID = "tenantId";

    /**
     * JWT Claim: 部門ID
     */
    public static final String JWT_CLAIM_DEPT_ID = "deptId";

    /**
     * JWT Claim: ロール
     */
    public static final String JWT_CLAIM_ROLES = "roles";

    /**
     * Authorization ヘッダー名
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * テナントID ヘッダー名
     */
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
}
