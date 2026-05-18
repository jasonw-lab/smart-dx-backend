package com.smartdx.tenant;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 租户上下文工具类
 * <p>
 * 使用 TransmittableThreadLocal 存储当前线程的租户ID，确保线程安全
 * 支持异步任务、线程池、消息队列等场景的上下文传递
 * </p>
 */
public class TenantContextHolder {

    private static final Logger log = LoggerFactory.getLogger(TenantContextHolder.class);

    /**
     * 租户ID线程本地变量
     * 使用 TransmittableThreadLocal 支持父子线程和线程池场景的值传递
     */
    private static final TransmittableThreadLocal<Long> TENANT_ID_HOLDER = new TransmittableThreadLocal<>();

    /**
     * 忽略租户标志（用于某些场景下临时跳过租户过滤）
     */
    private static final TransmittableThreadLocal<Boolean> IGNORE_TENANT_HOLDER = new TransmittableThreadLocal<>();

    private TenantContextHolder() {
    }

    /**
     * 设置当前租户 ID
     *
     * @param tenantId 租户 ID
     */
    public static void setTenantId(Long tenantId) {
        if (tenantId != null) {
            TENANT_ID_HOLDER.set(tenantId);
            log.debug("Set tenant ID: {}", tenantId);
        }
    }

    /**
     * 获取当前租户 ID
     *
     * @return 租户ID，如果未设置则返回 null
     */
    public static Long getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    /**
     * 设置忽略租户标志
     *
     * @param ignore 是否忽略
     */
    public static void setIgnoreTenant(boolean ignore) {
        IGNORE_TENANT_HOLDER.set(ignore);
        log.debug("Set ignore tenant flag: {}", ignore);
    }

    /**
     * 是否忽略租户
     *
     * @return true-忽略，false-不忽略
     */
    public static boolean isIgnoreTenant() {
        Boolean ignore = IGNORE_TENANT_HOLDER.get();
        return ignore != null && ignore;
    }

    /**
     * 清除当前线程的租户上下文
     * <p>
     * 必须在请求结束时调用，避免线程池复用导致的数据泄露
     * </p>
     */
    public static void clear() {
        TENANT_ID_HOLDER.remove();
        IGNORE_TENANT_HOLDER.remove();
        log.debug("Cleared tenant context");
    }
}
