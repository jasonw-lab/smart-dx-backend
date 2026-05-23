package com.smartdx.system.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Tenant VO
 */
@Data
@Schema(description = "Tenant information")
public class TenantVO {

    @Schema(description = "Tenant ID")
    private Long id;

    @Schema(description = "Tenant name")
    private String name;

    @Schema(description = "Tenant code")
    private String code;

    @Schema(description = "Tenant domain")
    private String domain;

    @Schema(description = "Status: 1=enabled, 0=disabled")
    private Integer status;

    @Schema(description = "Created at")
    private LocalDateTime createTime;
}
