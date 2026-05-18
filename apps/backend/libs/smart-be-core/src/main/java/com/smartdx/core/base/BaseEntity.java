package com.smartdx.core.base;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * エンティティ基底クラス
 */
public abstract class BaseEntity implements Serializable {

    /**
     * 作成者ID
     */
    private Long createBy;

    /**
     * 作成日時
     */
    private LocalDateTime createTime;

    /**
     * 更新者ID
     */
    private Long updateBy;

    /**
     * 更新日時
     */
    private LocalDateTime updateTime;

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
