package com.smartdx.retail.model.query;

import com.smartdx.core.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Product page query
 */
@Schema(description = "Product page query")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductPageQuery extends BaseQuery {

    @Schema(description = "Product name")
    private String productName;

    @Schema(description = "Product code")
    private String productCode;

    @Schema(description = "Category ID")
    private Long categoryId;

    @Schema(description = "Status (active, inactive)")
    private String status;
}
