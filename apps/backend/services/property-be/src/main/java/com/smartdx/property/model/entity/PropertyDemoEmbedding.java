package com.smartdx.property.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DEMO用固定embedding参照テーブル（Phase 1）
 */
@Data
@TableName(value = "property_demo_embedding", autoResultMap = true)
public class PropertyDemoEmbedding {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * テナントID
     */
    private Long tenantId;

    /**
     * DEMO参照キー（demo-* prefix）
     */
    private String demoRef;

    /**
     * embedding vector（512次元、JSON配列）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Double> embeddingVector;

    /**
     * サムネイルURL
     */
    private String thumbnailUrl;

    /**
     * 表示タイトル
     */
    private String title;

    /**
     * 説明
     */
    private String description;

    /**
     * 有効フラグ
     */
    private Boolean isActive;

    /**
     * 作成日時
     */
    private LocalDateTime createdAt;

    /**
     * 更新日時
     */
    private LocalDateTime updatedAt;
}
