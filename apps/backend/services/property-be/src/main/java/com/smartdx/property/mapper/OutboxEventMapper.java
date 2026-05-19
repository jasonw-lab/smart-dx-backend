package com.smartdx.property.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdx.property.model.entity.OutboxEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * Outbox Event Mapper
 */
@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEvent> {
}
