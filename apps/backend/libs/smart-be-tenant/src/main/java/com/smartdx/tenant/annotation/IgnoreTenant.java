package com.smartdx.tenant.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * テナントフィルタ無視アノテーション
 * <p>
 * メソッドまたはクラスに付与すると、そのスコープ内のDBアクセスで
 * テナントフィルタ (WHERE tenant_id = ?) が適用されなくなる。
 * </p>
 * <p>
 * 用途: テナント管理、システム設定など、テナント分離が不要な処理
 * </p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreTenant {
}
