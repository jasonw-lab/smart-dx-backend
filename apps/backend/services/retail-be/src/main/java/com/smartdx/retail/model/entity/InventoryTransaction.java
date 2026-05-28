package com.smartdx.retail.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Inventory Transaction Entity
 */
@TableName("retail_inventory_transaction")
@Data
public class InventoryTransaction implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * テナントID (リクエストボディからは設定不可、interceptor で自動付与)
     */
    @JsonIgnore
    private Long tenantId;

    private Long inventoryId;

    private Long storeId;

    private Long productId;

    private String lotNumber;

    /**
     * Transaction type: INBOUND, SALE, ADJUSTMENT, DISPOSAL, TRANSFER_IN, TRANSFER_OUT
     */
    private String txnType;

    private Integer quantityDelta;

    /**
     * Source type: MANUAL, POS, BATCH
     */
    private String sourceType;

    private String referenceNo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime occurredAt;

    private String note;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    @TableLogic
    private Integer isDeleted;
}
