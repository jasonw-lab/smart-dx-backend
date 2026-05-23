package com.smartdx.property.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("property_intake_job_seq")
public class PropertyIntakeJobSeq {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String jobType;

    private String datePart;

    private Integer seq;

    private LocalDateTime updateTime;
}
