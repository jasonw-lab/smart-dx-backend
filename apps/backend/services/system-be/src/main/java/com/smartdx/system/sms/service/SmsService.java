package com.smartdx.system.sms.service;

import com.smartdx.system.sms.enums.SmsTypeEnum;

import java.util.Map;

/**
 * SMS サービスインターフェース
 */
public interface SmsService {

    /**
     * SMS を送信
     *
     * @param mobile         電話番号 (例: 09012345678)
     * @param smsType        SMS テンプレートタイプ
     * @param templateParams テンプレートパラメータ (例: {"code":"123456"})
     * @return 送信成功かどうか
     */
    boolean sendSms(String mobile, SmsTypeEnum smsType, Map<String, String> templateParams);
}
