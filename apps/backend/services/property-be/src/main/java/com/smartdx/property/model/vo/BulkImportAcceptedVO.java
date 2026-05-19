package com.smartdx.property.model.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class BulkImportAcceptedVO {

    private String jobRef;

    private String scope;

    private OffsetDateTime acceptedAt;

    private ReceivedCountVO receivedCount;
}
