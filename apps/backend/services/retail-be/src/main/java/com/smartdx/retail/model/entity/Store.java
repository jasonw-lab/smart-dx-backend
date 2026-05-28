package com.smartdx.retail.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Store Entity
 */
@TableName("retail_store")
@Data
public class Store implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * テナントID (リクエストボディからは設定不可、interceptor で自動付与)
     */
    @JsonIgnore
    private Long tenantId;

    private String storeCode;

    private String storeName;

    private String address;

    private String phone;

    private String manager;

    /**
     * Status: ONLINE, MAINTENANCE, OFFLINE
     */
    private String status;

    private String openingHours;

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
