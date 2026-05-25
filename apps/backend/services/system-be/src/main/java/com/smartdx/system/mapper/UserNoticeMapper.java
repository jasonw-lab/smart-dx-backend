package com.smartdx.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdx.system.model.entity.UserNotice;
import org.apache.ibatis.annotations.Mapper;

/**
 * ユーザー通知 Mapper
 */
@Mapper
public interface UserNoticeMapper extends BaseMapper<UserNotice> {
}
