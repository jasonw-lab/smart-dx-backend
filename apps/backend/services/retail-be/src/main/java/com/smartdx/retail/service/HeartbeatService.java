package com.smartdx.retail.service;

import com.smartdx.retail.model.form.HeartbeatPayload;

/**
 * Heartbeatサービスインターフェース
 */
public interface HeartbeatService {

    /**
     * Heartbeatを受信・処理する
     *
     * @param payload Heartbeatペイロード
     * @return 処理結果
     */
    boolean receiveHeartbeat(HeartbeatPayload payload);
}
