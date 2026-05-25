package com.smartdx.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartdx.system.model.entity.Log;
import com.smartdx.system.model.vo.LogPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * ログコンバーター
 */
@Mapper(componentModel = "spring")
public interface LogConverter {

    @Mapping(target = "region", expression = "java(buildRegion(log))")
    LogPageVO toPageVo(Log log);

    default Page<LogPageVO> toPageVo(Page<Log> page) {
        Page<LogPageVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toPageVo).toList());
        return result;
    }

    default String buildRegion(Log log) {
        if (log.getProvince() == null && log.getCity() == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (log.getProvince() != null) {
            sb.append(log.getProvince());
        }
        if (log.getCity() != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(log.getCity());
        }
        return sb.toString();
    }
}
