package com.smartdx.property.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.smartdx.property.model.entity.PropertyDemoEmbedding;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * DEMO用固定embedding参照 Mapper
 */
@Mapper
public interface PropertyDemoEmbeddingMapper extends BaseMapper<PropertyDemoEmbedding> {

    /**
     * demoRef と tenantId で有効なレコードを取得
     */
    @Results(id = "demoEmbeddingResultMap", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "tenant_id", property = "tenantId"),
            @Result(column = "demo_ref", property = "demoRef"),
            @Result(column = "embedding_vector", property = "embeddingVector", typeHandler = JacksonTypeHandler.class),
            @Result(column = "thumbnail_url", property = "thumbnailUrl"),
            @Result(column = "title", property = "title"),
            @Result(column = "description", property = "description"),
            @Result(column = "is_active", property = "isActive"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    @Select("SELECT * FROM property_demo_embedding WHERE demo_ref = #{demoRef} AND tenant_id = #{tenantId} AND is_active = 1")
    PropertyDemoEmbedding findByDemoRefAndTenantId(@Param("demoRef") String demoRef, @Param("tenantId") Long tenantId);

    /**
     * demoRef で有効なレコードを取得（テナント無視、DEMO用）
     */
    @InterceptorIgnore(tenantLine = "true")
    @ResultMap("demoEmbeddingResultMap")
    @Select("SELECT * FROM property_demo_embedding WHERE demo_ref = #{demoRef} AND is_active = 1")
    PropertyDemoEmbedding findByDemoRef(@Param("demoRef") String demoRef);
}
