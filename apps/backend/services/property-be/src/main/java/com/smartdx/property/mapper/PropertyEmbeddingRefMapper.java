package com.smartdx.property.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdx.property.model.entity.PropertyEmbeddingRef;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * LST-EMB-01: 短命embeddingRef マッパー
 */
@Mapper
public interface PropertyEmbeddingRefMapper extends BaseMapper<PropertyEmbeddingRef> {

    /**
     * embeddingRefで検索（有効期限チェック付き）
     */
    @Select("SELECT * FROM property_embedding_ref " +
            "WHERE embedding_ref = #{embeddingRef} " +
            "AND expires_at > #{now}")
    PropertyEmbeddingRef selectByRefAndNotExpired(
            @Param("embeddingRef") String embeddingRef,
            @Param("now") LocalDateTime now
    );

    /**
     * ユーザーのembeddingRef数をカウント
     */
    @Select("SELECT COUNT(*) FROM property_embedding_ref " +
            "WHERE user_id = #{userId} AND tenant_id = #{tenantId}")
    int countByUserAndTenant(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    /**
     * 期限切れレコードを削除
     */
    @Delete("DELETE FROM property_embedding_ref WHERE expires_at <= #{now}")
    int deleteExpired(@Param("now") LocalDateTime now);
}
