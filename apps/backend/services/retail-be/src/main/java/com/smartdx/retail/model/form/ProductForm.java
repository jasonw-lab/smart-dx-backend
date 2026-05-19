package com.smartdx.retail.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Product form
 */
@Schema(description = "Product form")
@Data
public class ProductForm {

    @Schema(description = "Product name")
    @NotBlank(message = "Product name is required")
    @Size(max = 100, message = "Product name must be 100 characters or less")
    private String name;

    @Schema(description = "Product code")
    @NotBlank(message = "Product code is required")
    @Size(max = 30, message = "Product code must be 30 characters or less")
    private String code;

    @Schema(description = "Barcode")
    @Size(max = 50, message = "Barcode must be 50 characters or less")
    private String barcode;

    @Schema(description = "Category ID")
    private Long categoryId;

    @Schema(description = "Category name")
    private String categoryName;

    @Schema(description = "Unit price")
    @NotNull(message = "Unit price is required")
    private BigDecimal price;

    @Schema(description = "Cost price")
    private BigDecimal costPrice;

    @Schema(description = "Unit (pcs, kg, etc.)")
    private String unit;

    @Schema(description = "Shelf life in days")
    private Integer shelfLifeDays;

    @Schema(description = "Supplier ID")
    private Long supplierId;

    @Schema(description = "Supplier name")
    private String supplierName;

    @Schema(description = "Product description")
    @Size(max = 500, message = "Description must be 500 characters or less")
    private String description;

    @Schema(description = "Product image URL")
    private String imageUrl;

    @Schema(description = "Status (active, inactive)")
    private String status;

    @Schema(description = "Reorder point")
    private Integer reorderPoint;

    @Schema(description = "Max stock threshold")
    private Integer maxStock;
}
