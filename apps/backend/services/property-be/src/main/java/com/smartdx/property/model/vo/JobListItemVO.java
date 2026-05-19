package com.smartdx.property.model.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class JobListItemVO {

    private String jobRef;

    private String targetScope;

    private String status;

    private int succeeded;

    private int failed;

    private int total;

    private OffsetDateTime startedAt;

    private OffsetDateTime endedAt;

    private JobOwnerVO owner;
}
