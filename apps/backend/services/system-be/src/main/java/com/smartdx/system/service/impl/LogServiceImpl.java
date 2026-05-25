package com.smartdx.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.system.converter.LogConverter;
import com.smartdx.system.mapper.LogMapper;
import com.smartdx.system.model.entity.Log;
import com.smartdx.system.model.query.LogQuery;
import com.smartdx.system.model.vo.LogPageVO;
import com.smartdx.system.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ログサービス実装
 */
@Service
@RequiredArgsConstructor
public class LogServiceImpl extends ServiceImpl<LogMapper, Log> implements LogService {

    private final LogConverter logConverter;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Page<LogPageVO> getLogPage(LogQuery query) {
        Page<Log> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<Log> wrapper = new LambdaQueryWrapper<>();

        // キーワード検索
        if (StrUtil.isNotBlank(query.getKeywords())) {
            wrapper.and(w -> w
                    .like(Log::getTitle, query.getKeywords())
                    .or()
                    .like(Log::getContent, query.getKeywords())
                    .or()
                    .like(Log::getIp, query.getKeywords())
                    .or()
                    .like(Log::getOperatorName, query.getKeywords())
            );
        }

        // 時間範囲検索
        if (CollUtil.isNotEmpty(query.getCreateTime()) && query.getCreateTime().size() == 2) {
            LocalDateTime startTime = LocalDateTime.parse(query.getCreateTime().get(0), FORMATTER);
            LocalDateTime endTime = LocalDateTime.parse(query.getCreateTime().get(1), FORMATTER);
            wrapper.between(Log::getCreateTime, startTime, endTime);
        }

        wrapper.orderByDesc(Log::getCreateTime);

        Page<Log> result = this.page(page, wrapper);
        return logConverter.toPageVo(result);
    }
}
