package com.smartdx.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdx.system.model.entity.Notice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知 Mapper
 */
@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {
}
