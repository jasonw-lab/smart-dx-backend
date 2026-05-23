package com.smartdx.retail.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Device Entity
 */
@TableName("retail_device")
@Data
public class Device implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long storeId;

    private String deviceCode;

    /**
     * Device type: PAYMENT_TERMINAL, CAMERA, GATE, REFRIGERATOR_SENSOR, PRINTER, NETWORK_ROUTER
     */
    private String deviceType;

    private String deviceName;

    /**
     * Status: ONLINE, OFFLINE, ERROR, MAINTENANCE
     */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastHeartbeat;

    private String errorCode;

    /**
     * Device metadata (JSON)
     */
    private String metadata;

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
