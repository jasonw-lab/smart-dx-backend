package com.smartdx.retail.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Alert Entity
 */
@TableName("retail_alert")
@Data
public class Alert implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long storeId;

    private Long productId;

    private Long deviceId;

    private String lotNumber;

    /**
     * Alert type: LOW_STOCK, EXPIRY_SOON, HIGH_STOCK, COMMUNICATION_DOWN, PAYMENT_TERMINAL_DOWN, CARD_READER_ERROR, PRINTER_PAPER_EMPTY
     */
    private String alertType;

    /**
     * Priority: P1, P2, P3, P4
     */
    private String priority;

    /**
     * Status: NEW, ACK, IN_PROGRESS, RESOLVED, CLOSED
     */
    private String status;

    private String message;

    private String thresholdValue;

    private String currentValue;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime detectedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime acknowledgedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closedAt;

    private String resolutionNote;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    @TableLogic
    private Integer isDeleted;
}
