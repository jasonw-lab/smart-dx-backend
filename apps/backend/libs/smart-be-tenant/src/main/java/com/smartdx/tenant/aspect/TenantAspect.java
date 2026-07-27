package com.smartdx.tenant.aspect;

import com.smartdx.tenant.TenantContextHolder;
import com.smartdx.tenant.annotation.IgnoreTenant;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * マルチテナントアスペクト
 * <p>
 * @IgnoreTenant アノテーションが付与されたメソッド/クラスで
 * 一時的にテナントフィルタをスキップする
 * </p>
 */
@Aspect
@Component
@Order(1)
public class TenantAspect {

    private static final Logger log = LoggerFactory.getLogger(TenantAspect.class);

    /**
     * @IgnoreTenant アノテーション処理
     */
    @Around("@annotation(ignoreTenant) || @within(ignoreTenant)")
    public Object around(ProceedingJoinPoint joinPoint, IgnoreTenant ignoreTenant) throws Throwable {
        // ネスト呼び出しで外側スコープのフラグを壊さないよう前値を退避して復元する
        boolean previous = TenantContextHolder.isIgnoreTenant();
        try {
            TenantContextHolder.setIgnoreTenant(true);
            log.debug("Method {} ignoring tenant filter", joinPoint.getSignature().getName());
            return joinPoint.proceed();
        } finally {
            TenantContextHolder.setIgnoreTenant(previous);
        }
    }
}
