package com.smartdx.security.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * ロールのデータスコープ情報
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleDataScope implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ロールコード
     */
    private String roleCode;

    /**
     * データスコープ種別
     */
    private Integer dataScope;

    /**
     * カスタム部署IDリスト（CUSTOM スコープ時に使用）
     */
    private List<Long> customDeptIds;

    public RoleDataScope(Integer dataScope) {
        this.dataScope = dataScope;
    }

    public RoleDataScope(Integer dataScope, List<Long> customDeptIds) {
        this.dataScope = dataScope;
        this.customDeptIds = customDeptIds;
    }
}
