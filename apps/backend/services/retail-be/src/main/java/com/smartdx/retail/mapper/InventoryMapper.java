package com.smartdx.retail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdx.retail.model.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;

/**
 * Inventory Mapper
 */
@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {
}
