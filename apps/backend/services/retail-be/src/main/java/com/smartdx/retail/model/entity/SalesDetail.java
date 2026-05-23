package com.smartdx.retail.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Sales Detail Entity
 */
@TableName("retail_sales_detail")
@Data
public class SalesDetail implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long salesId;

    private Long productId;

    private String lotNumber;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal subtotal;

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
