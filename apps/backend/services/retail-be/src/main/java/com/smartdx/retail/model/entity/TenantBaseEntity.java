package com.smartdx.retail.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

/**
 * テナント分離対象エンティティの基底クラス
 * <p>
 * マルチテナント対応のエンティティはこのクラスを継承するか、
 * tenantIdフィールドを直接定義する。
 * </p>
 *
 * @author jason.w
 */
@Data
public abstract class TenantBaseEntity implements Serializable {

    /**
     * テナントID
     * <p>
     * MyBatis Plus TenantLineInnerInterceptor により自動設定される
     * </p>
     */
    @TableField("tenant_id")
    private Long tenantId;
}
