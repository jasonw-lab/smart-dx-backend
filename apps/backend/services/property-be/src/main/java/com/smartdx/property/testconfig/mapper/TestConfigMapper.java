package com.smartdx.property.testconfig.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdx.property.testconfig.model.entity.TestConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * テスト設定Mapper
 */
@Mapper
public interface TestConfigMapper extends BaseMapper<TestConfig> {
}
