package com.smartdx.system.sms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Aliyun SMS 設定プロパティ
 */
@Configuration
@ConfigurationProperties(prefix = "sms.aliyun")
@Data
public class AliyunSmsProperties {

    /**
     * Aliyun アカウントの Access Key ID
     */
    private String accessKeyId;

    /**
     * Aliyun アカウントの Access Key Secret
     */
    private String accessKeySecret;

    /**
     * Aliyun SMS サービス API ドメイン (例: dysmsapi.aliyuncs.com)
     */
    private String domain;

    /**
     * Aliyun サービスのリージョン ID (例: cn-shanghai)
     */
    private String regionId;

    /**
     * SMS 署名（Aliyun SMS サービスで登録・審査済みのもの）
     */
    private String signName;

    /**
     * SMS テンプレートコレクション
     */
    private Map<String, String> templates;
}
