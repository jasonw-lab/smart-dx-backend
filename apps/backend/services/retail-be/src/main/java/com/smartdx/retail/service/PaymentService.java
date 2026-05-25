package com.smartdx.retail.service;

import com.smartdx.retail.model.form.PaymentPayload;

/**
 * 決済サービスインターフェース
 */
public interface PaymentService {

    /**
     * 決済結果を受信・処理する
     *
     * @param payload 決済ペイロード
     * @return 処理結果（生成された売上ID）
     */
    Long receivePayment(PaymentPayload payload);
}
