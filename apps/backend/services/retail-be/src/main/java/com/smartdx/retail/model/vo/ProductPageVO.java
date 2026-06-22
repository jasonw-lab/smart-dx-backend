package com.smartdx.retail.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product page VO
 */
@Schema(description = "Product page VO")
@Data
public class ProductPageVO {

    @Schema(description = "Product ID")
    private Long id;

    @Schema(description = "Product code")
    private String productCode;

    @Schema(description = "Product name")
    private String productName;

    @Schema(description = "Barcode")
    private String barcode;

    @Schema(description = "Category ID")
    private Long categoryId;

    @Schema(description = "Category name")
    private String categoryName;

    @Schema(description = "Unit price")
    private BigDecimal unitPrice;

    @Schema(description = "Cost price")
    private BigDecimal costPrice;

    @Schema(description = "Unit")
    private String unit;

    @Schema(description = "Shelf life in days")
    private Integer shelfLifeDays;

    @Schema(description = "Supplier ID")
    private Long supplierId;

    @Schema(description = "Supplier name")
    private String supplierName;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Image URL")
    private String imageUrl;

    @Schema(description = "Status")
    private String status;

    @Schema(description = "Created at")
    private LocalDateTime createTime;

    @Schema(description = "Updated at")
    private LocalDateTime updateTime;

    @Schema(description = "Reorder point")
    private Integer reorderPoint;

    @Schema(description = "Max stock")
    private Integer maxStock;

    @Schema(description = "Total stock quantity across all stores/lots")
    private Integer stock;

    @Schema(description = "Total sales quantity")
    private Integer sales;
}
