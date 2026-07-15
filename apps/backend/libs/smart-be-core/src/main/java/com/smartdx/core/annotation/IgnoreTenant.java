package com.smartdx.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 忽略多租户注解
 * <p>
 * 标注在方法或类上，表示该方法或类下的所有方法忽略多租户过滤
 * 适用于系统管理、租户管理等不需要租户隔离的场景
 * </p>
 *
 * @author Ray.Hao
 * @since 3.0.0
 * @deprecated {@code com.smartdx.tenant.annotation.IgnoreTenant} を使用すること。
 *             本アノテーションは TenantAspect の対象外であり、付与しても何も起こらない。
 *             次リリースで削除予定 (libs 破壊的変更ルールに基づく deprecate 期間)。
 */
@Deprecated(forRemoval = true)
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreTenant {
}

