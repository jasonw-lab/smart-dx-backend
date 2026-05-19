package com.smartdx.system.model.query;

import com.smartdx.core.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Tenant query parameters
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Tenant query")
public class TenantQuery extends BaseQuery {

    @Schema(description = "Tenant name")
    private String name;

    @Schema(description = "Tenant code")
    private String code;

    @Schema(description = "Status")
    private Integer status;
}
