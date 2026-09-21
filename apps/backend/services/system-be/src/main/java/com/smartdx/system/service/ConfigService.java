package com.smartdx.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartdx.system.model.entity.Config;
import com.smartdx.system.model.form.ConfigForm;
import com.smartdx.system.model.query.ConfigQuery;
import com.smartdx.system.model.vo.ConfigPageVO;

/**
 * システム設定サービス
 */
public interface ConfigService extends IService<Config> {

    Page<ConfigPageVO> getConfigPage(ConfigQuery query);

    ConfigForm getConfigForm(Long id);

    boolean saveConfig(ConfigForm form);

    boolean updateConfig(Long id, ConfigForm form);

    boolean deleteConfigs(String ids);
}
