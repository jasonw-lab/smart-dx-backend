package com.smartdx.system.mail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * メール設定プロパティ
 */
@ConfigurationProperties(prefix = "spring.mail")
@Data
public class MailProperties {

    /**
     * メールサーバーホスト名またはIPアドレス
     */
    private String host;

    /**
     * メールサーバーポート番号
     */
    private int port;

    /**
     * メールサーバー接続用ユーザー名
     */
    private String username;

    /**
     * メールサーバー接続用パスワード
     */
    private String password;

    /**
     * メール送信者アドレス
     */
    private String from;

    /**
     * メールサーバーの追加プロパティ設定
     */
    private Properties properties = new Properties();

    @Data
    public static class Properties {

        private Smtp smtp = new Smtp();

        @Data
        public static class Smtp {

            /**
             * SMTP認証を有効にするかどうか
             */
            private boolean auth;

            /**
             * STARTTLS暗号化設定
             */
            private StartTls starttls = new StartTls();

            @Data
            public static class StartTls {

                /**
                 * STARTTLS暗号化を有効にするかどうか
                 */
                private boolean enable;
            }
        }
    }
}
