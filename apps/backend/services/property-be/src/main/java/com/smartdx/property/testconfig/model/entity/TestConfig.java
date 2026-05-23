package com.smartdx.property.testconfig.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * E2Eテスト設定エンティティ
 */
@Getter
@Setter
@TableName("sys_test_config")
public class TestConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String configKey;

    private String configValue;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
