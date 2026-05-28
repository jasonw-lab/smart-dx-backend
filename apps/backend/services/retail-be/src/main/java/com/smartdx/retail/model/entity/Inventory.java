package com.smartdx.retail.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Inventory Entity (lot-based)
 */
@TableName("retail_inventory")
@Data
public class Inventory implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * テナントID (リクエストボディからは設定不可、interceptor で自動付与)
     */
    @JsonIgnore
    private Long tenantId;

    private Long storeId;

    private Long productId;

    private String lotNumber;

    private Integer quantity;

    private LocalDate expiryDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receivedAt;

    private String location;

    /**
     * Status: normal, low, high, expired, out_of_stock
     */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastCountDate;

    private String remarks;

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
