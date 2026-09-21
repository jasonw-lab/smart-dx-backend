package com.smartdx.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartdx.system.model.entity.Config;
import com.smartdx.system.model.form.ConfigForm;
import com.smartdx.system.model.vo.ConfigPageVO;
import org.mapstruct.Mapper;

/**
 * システム設定コンバーター
 */
@Mapper(componentModel = "spring")
public interface ConfigConverter {

    ConfigPageVO toPageVo(Config config);

    default Page<ConfigPageVO> toPageVo(Page<Config> page) {
        Page<ConfigPageVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toPageVo).toList());
        return result;
    }

    Config toEntity(ConfigForm form);

    ConfigForm toForm(Config entity);
}
