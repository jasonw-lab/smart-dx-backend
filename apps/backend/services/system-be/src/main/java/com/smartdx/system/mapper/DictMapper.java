package com.smartdx.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdx.system.model.entity.Dict;
import org.apache.ibatis.annotations.Mapper;

/**
 * 辞書 Mapper
 */
@Mapper
public interface DictMapper extends BaseMapper<Dict> {
}
