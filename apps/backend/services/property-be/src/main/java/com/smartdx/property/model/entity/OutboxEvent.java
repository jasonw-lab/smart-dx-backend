package com.smartdx.property.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Outbox Event Entity
 * Transactional Outbox パターン用
 */
@Getter
@Setter
@TableName("outbox_event")
public class OutboxEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    private String eventType;

    private String aggregateType;

    private String aggregateId;

    private String payload;

    private String status;

    private Integer retryCount;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;
}
