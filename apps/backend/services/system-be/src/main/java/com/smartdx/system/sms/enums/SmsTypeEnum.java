package com.smartdx.system.sms.enums;

import com.smartdx.core.base.IBaseEnum;
import lombok.Getter;

/**
 * SMS タイプ列挙
 * <p>
 * value 値は application-*.yml の sms.templates.* 設定に対応
 */
@Getter
public enum SmsTypeEnum implements IBaseEnum<String> {

    /**
     * 登録 SMS 検証コード
     */
    REGISTER("register", "登録 SMS 検証コード"),

    /**
     * ログイン SMS 検証コード
     */
    LOGIN("login", "ログイン SMS 検証コード"),

    /**
     * 電話番号変更 SMS 検証コード
     */
    CHANGE_MOBILE("change-mobile", "電話番号変更 SMS 検証コード");

    private final String value;
    private final String label;

    SmsTypeEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }
}
