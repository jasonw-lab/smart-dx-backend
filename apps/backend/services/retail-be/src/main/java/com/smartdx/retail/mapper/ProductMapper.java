package com.smartdx.retail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdx.retail.model.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * Product Mapper
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
