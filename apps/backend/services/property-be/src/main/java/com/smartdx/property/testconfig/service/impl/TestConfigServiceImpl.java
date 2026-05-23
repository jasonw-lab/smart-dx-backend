package com.smartdx.property.testconfig.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartdx.property.testconfig.mapper.TestConfigMapper;
import com.smartdx.property.testconfig.model.entity.TestConfig;
import com.smartdx.property.testconfig.model.req.TestConfigUpdateReq;
import com.smartdx.property.testconfig.model.vo.TestConfigVO;
import com.smartdx.property.testconfig.service.TestConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * テスト設定サービス実装
 */
@Service
@Profile("!prod")
@RequiredArgsConstructor
@Slf4j
public class TestConfigServiceImpl implements TestConfigService {

    private static final String KEY_SKIP_LOGIN = "skipLogin";
    private static final String KEY_MOCK_EXTERNAL_APIS = "mockExternalApis";

    private final TestConfigMapper testConfigMapper;

    @Override
    public TestConfigVO getConfig() {
        TestConfigVO vo = new TestConfigVO();
        vo.setSkipLogin(getBooleanValue(KEY_SKIP_LOGIN));
        vo.setMockExternalApis(getBooleanValue(KEY_MOCK_EXTERNAL_APIS));
        return vo;
    }

    @Override
    @Transactional
    public TestConfigVO updateConfig(TestConfigUpdateReq req) {
        if (req.getSkipLogin() != null) {
            updateValue(KEY_SKIP_LOGIN, req.getSkipLogin().toString());
            log.info("Test config updated: skipLogin={}", req.getSkipLogin());
        }
        if (req.getMockExternalApis() != null) {
            updateValue(KEY_MOCK_EXTERNAL_APIS, req.getMockExternalApis().toString());
            log.info("Test config updated: mockExternalApis={}", req.getMockExternalApis());
        }
        return getConfig();
    }

    @Override
    public boolean isSkipLogin() {
        return getBooleanValue(KEY_SKIP_LOGIN);
    }

    private boolean getBooleanValue(String key) {
        TestConfig config = testConfigMapper.selectOne(
                new LambdaQueryWrapper<TestConfig>().eq(TestConfig::getConfigKey, key)
        );
        if (config == null) {
            return false;
        }
        return "true".equalsIgnoreCase(config.getConfigValue());
    }

    private void updateValue(String key, String value) {
        testConfigMapper.update(null,
                new LambdaUpdateWrapper<TestConfig>()
                        .eq(TestConfig::getConfigKey, key)
                        .set(TestConfig::getConfigValue, value)
        );
    }
}
