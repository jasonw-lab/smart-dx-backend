package com.smartdx.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdx.system.model.entity.Log;
import org.apache.ibatis.annotations.Mapper;

/**
 * ログ Mapper
 */
@Mapper
public interface LogMapper extends BaseMapper<Log> {
}
