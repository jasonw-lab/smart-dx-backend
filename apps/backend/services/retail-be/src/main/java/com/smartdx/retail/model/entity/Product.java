package com.smartdx.retail.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product Entity
 */
@TableName("retail_product")
@Data
public class Product implements Serializable {

    /**
     * Product ID (PK)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * テナントID (リクエストボディからは設定不可、interceptor で自動付与)
     */
    @JsonIgnore
    private Long tenantId;

    /**
     * Product code
     */
    private String productCode;

    /**
     * Product name
     */
    private String productName;

    /**
     * Barcode (JAN code)
     */
    private String barcode;

    /**
     * Category ID
     */
    private Long categoryId;

    /**
     * Category name (denormalized)
     */
    private String categoryName;

    /**
     * Unit price (incl. tax)
     */
    private BigDecimal unitPrice;

    /**
     * Cost price
     */
    private BigDecimal costPrice;

    /**
     * Unit (pcs, kg, etc.)
     */
    private String unit;

    /**
     * Shelf life in days
     */
    private Integer shelfLifeDays;

    /**
     * Supplier ID
     */
    private Long supplierId;

    /**
     * Supplier name (denormalized)
     */
    private String supplierName;

    /**
     * Product description
     */
    private String description;

    /**
     * Product image URL
     */
    private String imageUrl;

    /**
     * Status (active/inactive)
     */
    private String status;

    /**
     * Reorder point
     */
    private Integer reorderPoint;

    /**
     * Max stock threshold
     */
    private Integer maxStock;

    /**
     * Created at
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * Updated at
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * Created by
     */
    private Long createBy;

    /**
     * Updated by
     */
    private Long updateBy;

    /**
     * Deleted flag
     */
    @TableLogic
    private Integer isDeleted;
}
