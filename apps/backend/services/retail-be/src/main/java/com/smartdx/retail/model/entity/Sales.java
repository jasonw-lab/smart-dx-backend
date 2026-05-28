package com.smartdx.retail.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Sales Header Entity
 */
@TableName("retail_sales")
@Data
public class Sales implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * テナントID (リクエストボディからは設定不可、interceptor で自動付与)
     */
    @JsonIgnore
    private Long tenantId;

    private Long storeId;

    private String orderNumber;

    private BigDecimal totalAmount;

    /**
     * Payment method: CASH, CARD, QR, OTHER
     */
    private String paymentMethod;

    private String paymentProvider;

    private String paymentReferenceId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime saleTimestamp;

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
