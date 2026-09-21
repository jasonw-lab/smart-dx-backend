package com.smartdx.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdx.system.model.entity.Config;
import org.apache.ibatis.annotations.Mapper;

/**
 * システム設定Mapper
 */
@Mapper
public interface ConfigMapper extends BaseMapper<Config> {
}
