package com.smartdx.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdx.system.model.entity.Dept;
import org.apache.ibatis.annotations.Mapper;

/**
 * 部門 Mapper
 */
@Mapper
public interface DeptMapper extends BaseMapper<Dept> {
}
