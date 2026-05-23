package com.smartdx.system.model.query;

import com.smartdx.core.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * User query parameters
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "User query")
public class UserQuery extends BaseQuery {

    @Schema(description = "Username")
    private String username;

    @Schema(description = "Nickname")
    private String nickname;

    @Schema(description = "Mobile")
    private String mobile;

    @Schema(description = "Status")
    private Integer status;

    @Schema(description = "Department ID")
    private Long deptId;
}
