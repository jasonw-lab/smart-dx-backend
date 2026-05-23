package com.smartdx.system.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * User VO
 */
@Data
@Schema(description = "User information")
public class UserVO {

    @Schema(description = "User ID")
    private Long id;

    @Schema(description = "Tenant ID")
    private Long tenantId;

    @Schema(description = "Username")
    private String username;

    @Schema(description = "Nickname")
    private String nickname;

    @Schema(description = "Mobile")
    private String mobile;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Gender: 0=unknown, 1=male, 2=female")
    private Integer gender;

    @Schema(description = "Avatar URL")
    private String avatar;

    @Schema(description = "Status: 1=enabled, 0=disabled")
    private Integer status;

    @Schema(description = "Department ID")
    private Long deptId;

    @Schema(description = "Can switch tenant")
    private Boolean canSwitchTenant;

    @Schema(description = "Created at")
    private LocalDateTime createTime;
}
