package com.smartdx.property.testconfig.service;

import com.smartdx.property.testconfig.model.req.TestConfigUpdateReq;
import com.smartdx.property.testconfig.model.vo.TestConfigVO;

/**
 * テスト設定サービス
 */
public interface TestConfigService {

    /**
     * テスト設定を取得
     */
    TestConfigVO getConfig();

    /**
     * テスト設定を更新
     */
    TestConfigVO updateConfig(TestConfigUpdateReq req);

    /**
     * ログインスキップが有効かどうか
     */
    boolean isSkipLogin();
}
