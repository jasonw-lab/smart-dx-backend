package com.smartdx.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.system.converter.ConfigConverter;
import com.smartdx.system.mapper.ConfigMapper;
import com.smartdx.system.model.entity.Config;
import com.smartdx.system.model.form.ConfigForm;
import com.smartdx.system.model.query.ConfigQuery;
import com.smartdx.system.model.vo.ConfigPageVO;
import com.smartdx.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * システム設定サービス実装
 */
@Service
@RequiredArgsConstructor
public class ConfigServiceImpl extends ServiceImpl<ConfigMapper, Config> implements ConfigService {

    private final ConfigConverter configConverter;

    @Override
    public Page<ConfigPageVO> getConfigPage(ConfigQuery query) {
        Page<Config> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<Config> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getKeywords())) {
            wrapper.and(w -> w
                    .like(Config::getConfigName, query.getKeywords())
                    .or()
                    .like(Config::getConfigKey, query.getKeywords())
            );
        }
        wrapper.orderByDesc(Config::getCreateTime);

        Page<Config> result = this.page(page, wrapper);
        return configConverter.toPageVo(result);
    }

    @Override
    public ConfigForm getConfigForm(Long id) {
        Config entity = this.getById(id);
        return configConverter.toForm(entity);
    }

    @Override
    public boolean saveConfig(ConfigForm form) {
        Config entity = configConverter.toEntity(form);
        return this.save(entity);
    }

    @Override
    public boolean updateConfig(Long id, ConfigForm form) {
        Config entity = configConverter.toEntity(form);
        entity.setId(id);
        return this.updateById(entity);
    }

    @Override
    public boolean deleteConfigs(String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }
}
