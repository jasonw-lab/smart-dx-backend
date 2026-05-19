package com.smartdx.property.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("property_intake_job_error")
public class PropertyIntakeJobError {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String jobRef;

    private String targetType;

    private String propertyKey;

    @TableField("target_key")
    private String assetKey;

    @TableField(exist = false)
    private String docKey;

    @TableField(exist = false)
    private Integer recordNo;

    private Integer rowNo;

    @TableField(exist = false)
    private String fileName;

    private String errorCode;

    @TableField("error_message")
    private String message;

    private String errorFingerprint;

    @TableField(exist = false)
    private Integer retried;

    @TableField(exist = false)
    private String retryJobRef;

    private LocalDateTime createdAt;
}
