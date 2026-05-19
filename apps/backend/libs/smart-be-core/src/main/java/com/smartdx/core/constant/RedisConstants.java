package com.smartdx.core.constant;

/**
 * Redis 定数
 */
public interface RedisConstants {

    interface RateLimiter {
        String IP = "rate_limiter:ip:{}";
    }

    interface Lock {
        String RESUBMIT = "lock:resubmit:{}:{}";
    }

    interface Auth {
        String ACCESS_TOKEN_USER = "auth:token:access:{}";
        String REFRESH_TOKEN_USER = "auth:token:refresh:{}";
        String USER_ACCESS_TOKEN = "auth:user:access:{}";
        String USER_REFRESH_TOKEN = "auth:user:refresh:{}";
        String BLACKLIST_TOKEN = "auth:token:blacklist:{}";
        String REVOKED_JTI = BLACKLIST_TOKEN;
        String USER_TOKEN_VERSION = "auth:user:token_version:{}";
    }

    interface Captcha {
        String IMAGE_CODE = "captcha:image:{}";
        String SMS_LOGIN_CODE = "captcha:sms_login:{}";
        String SMS_REGISTER_CODE = "captcha:sms_register:{}";
        String MOBILE_CODE = "captcha:mobile:{}";
        String EMAIL_CODE = "captcha:email:{}";
    }

    interface System {
        String CONFIG = "system:config";
        String ROLE_PERMS = "system:role:perms";
    }
}
