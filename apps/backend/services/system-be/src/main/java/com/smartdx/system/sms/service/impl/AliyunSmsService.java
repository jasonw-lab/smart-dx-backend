package com.smartdx.system.sms.service.impl;

import cn.hutool.json.JSONUtil;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.smartdx.system.sms.config.AliyunSmsProperties;
import com.smartdx.system.sms.enums.SmsTypeEnum;
import com.smartdx.system.sms.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Aliyun SMS サービス
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "sms.aliyun", name = "access-key-id")
public class AliyunSmsService implements SmsService {

    private final AliyunSmsProperties aliyunSmsProperties;

    @Override
    public boolean sendSms(String mobile, SmsTypeEnum smsType, Map<String, String> templateParams) {
        String templateCode = aliyunSmsProperties.getTemplates().get(smsType.getValue());

        DefaultProfile profile = DefaultProfile.getProfile(
                aliyunSmsProperties.getRegionId(),
                aliyunSmsProperties.getAccessKeyId(),
                aliyunSmsProperties.getAccessKeySecret()
        );
        IAcsClient client = new DefaultAcsClient(profile);

        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain(aliyunSmsProperties.getDomain());
        request.setSysVersion("2017-05-25");
        request.setSysAction("SendSms");
        request.putQueryParameter("RegionId", aliyunSmsProperties.getRegionId());
        request.putQueryParameter("PhoneNumbers", mobile);
        request.putQueryParameter("SignName", aliyunSmsProperties.getSignName());
        request.putQueryParameter("TemplateCode", templateCode);
        request.putQueryParameter("TemplateParam", JSONUtil.toJsonStr(templateParams));

        try {
            CommonResponse response = client.getCommonResponse(request);
            return response.getHttpResponse().isSuccess();
        } catch (ClientException e) {
            log.error("SMS 送信失敗: {}", e.getMessage(), e);
        }
        return false;
    }
}
