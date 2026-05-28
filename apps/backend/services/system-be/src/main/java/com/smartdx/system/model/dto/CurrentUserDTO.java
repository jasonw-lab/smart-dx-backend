package com.smartdx.system.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

/**
 * Current logged-in user DTO
 */
@Schema(description = "Current logged-in user")
@Data
public class CurrentUserDTO {

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "Tenant ID")
    private Long tenantId;

    @Schema(description = "Username")
    private String username;

    @Schema(description = "Nickname")
    private String nickname;

    @Schema(description = "Avatar URL")
    private String avatar;

    @Schema(description = "Can switch tenant")
    private Boolean canSwitchTenant;

    @Schema(description = "User role codes")
    private Set<String> roles;

    @Schema(description = "User permissions")
    private Set<String> perms;
}
