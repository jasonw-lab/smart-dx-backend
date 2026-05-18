package com.smartdx.core.result;

import java.io.Serializable;

/**
 * レスポンスコード定義
 * <p>
 * 阿里巴巴エラーコード規約に準拠:
 * - 00000: 成功
 * - A****: ユーザー側エラー (パラメータ、認証、権限等)
 * - B****: システム側エラー (内部エラー、タイムアウト等)
 * - C****: 外部サービスエラー (DB、外部API等)
 * </p>
 */
public enum ResultCode implements IResultCode, Serializable {

    SUCCESS("00000", "成功"),

    // A01xx: ユーザー登録エラー
    USER_ERROR("A0001", "ユーザー側エラー"),
    USER_REGISTRATION_ERROR("A0100", "ユーザー登録エラー"),

    // A02xx: ログインエラー
    USER_LOGIN_EXCEPTION("A0200", "ログイン異常"),
    ACCOUNT_NOT_FOUND("A0201", "アカウントが存在しません"),
    ACCOUNT_FROZEN("A0202", "アカウントが凍結されています"),
    USER_PASSWORD_ERROR("A0210", "ユーザー名またはパスワードが間違っています"),
    ACCESS_TOKEN_INVALID("A0230", "アクセストークンが無効または期限切れです"),
    REFRESH_TOKEN_INVALID("A0231", "リフレッシュトークンが無効または期限切れです"),
    USER_VERIFICATION_CODE_ERROR("A0240", "認証コードが間違っています"),

    // A03xx: 権限エラー
    ACCESS_PERMISSION_EXCEPTION("A0300", "アクセス権限エラー"),
    ACCESS_UNAUTHORIZED("A0301", "アクセスが許可されていません"),

    // A04xx: パラメータエラー
    USER_REQUEST_PARAMETER_ERROR("A0400", "リクエストパラメータエラー"),
    INVALID_USER_INPUT("A0402", "無効な入力"),
    REQUEST_REQUIRED_PARAMETER_IS_EMPTY("A0410", "必須パラメータが空です"),
    PARAMETER_FORMAT_MISMATCH("A0421", "パラメータ形式が不正です"),

    // A05xx: リクエストエラー
    USER_REQUEST_SERVICE_EXCEPTION("A0500", "リクエストサービスエラー"),
    REQUEST_CONCURRENCY_LIMIT_EXCEEDED("A0502", "リクエスト同時実行数が上限を超えました"),
    DUPLICATE_SUBMISSION("A0506", "重複送信しないでください"),

    // A07xx: ファイルエラー
    UPLOAD_FILE_EXCEPTION("A0700", "ファイルアップロードエラー"),
    DELETE_FILE_EXCEPTION("A0710", "ファイル削除エラー"),

    // B0001: システムエラー
    SYSTEM_ERROR("B0001", "システム実行エラー"),
    SYSTEM_EXECUTION_TIMEOUT("B0100", "システムタイムアウト"),

    // C0001: 外部サービスエラー
    THIRD_PARTY_SERVICE_ERROR("C0001", "外部サービス呼び出しエラー"),
    INTERFACE_NOT_EXIST("C0113", "インターフェースが存在しません"),
    DATABASE_SERVICE_ERROR("C0300", "データベースサービスエラー");

    private final String code;
    private final String msg;

    ResultCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    public static ResultCode getValue(String code) {
        for (ResultCode value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return SYSTEM_ERROR;
    }
}
