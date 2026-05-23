package com.smartdx.property.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartdx.core.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("property_intake_job")
public class PropertyIntakeJob extends BaseEntity {

    private String jobRef;

    private String jobType;

    private String targetScope;

    private String status;

    private Integer totalCount;

    private Integer succeededCount;

    private Integer failedCount;

    private Integer recordsCount;

    private Integer photosCount;

    private Integer docsCount;

    private LocalDateTime acceptedAt;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private Long ownerUserId;

    private String ownerDisplayName;

    private Integer listedYear;

    private String area;

    private String propertyKey;

    private Integer processedCount;

    private String originalJobRef;

    private String resubmitOfPropertyKey;

    private String resubmitStateFrom;

    private String resubmitStateTo;

    private Integer errorsTruncated;

    private String paramsJson;

    private String lastErrorMessage;

    private Integer isDeleted;
}
