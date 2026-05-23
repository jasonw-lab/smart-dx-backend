package com.smartdx.property.model.vo;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class JobDetailVO {

    private String jobRef;

    private String targetScope;

    private String status;

    private int succeeded;

    private int failed;

    private int total;

    private OffsetDateTime startedAt;

    private OffsetDateTime endedAt;

    private JobOwnerVO owner;

    private List<JobErrorVO> errors = new ArrayList<>();

    private boolean errorsTruncated;
}
