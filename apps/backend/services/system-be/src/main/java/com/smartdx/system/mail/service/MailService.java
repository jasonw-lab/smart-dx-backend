package com.smartdx.system.mail.service;

import com.smartdx.system.mail.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * メールサービス
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(JavaMailSender.class)
public class MailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    /**
     * シンプルなテキストメールを送信
     *
     * @param to      宛先アドレス
     * @param subject メール件名
     * @param text    メール内容
     */
    public void sendMail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProperties.getFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("メール送信失敗: {}", e.getMessage());
        }
    }

    /**
     * 添付ファイル付きメールを送信
     *
     * @param to       宛先アドレス
     * @param subject  メール件名
     * @param text     メール内容
     * @param filePath 添付ファイルパス
     */
    public void sendMailWithAttachment(String to, String subject, String text, String filePath) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true);

            FileSystemResource file = new FileSystemResource(new File(filePath));
            helper.addAttachment(file.getFilename(), file);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("添付ファイル付きメール送信失敗: {}", e.getMessage());
        }
    }
}
