package com.pg.service;

import com.pg.entity.HqLedgerSysSettings;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 전산설정(tb_hq_ledger_sys_settings) SMTP 값으로 단발 메일 발송.
 */
@Service
public class LedgerSmtpMailService {

    public void sendPlainText(HqLedgerSysSettings s, String to, String subject, String text) {
        sendPlainText(s, to, subject, text, null, null);
    }

    public void sendPlainText(HqLedgerSysSettings s, String to, String subject, String text,
                              String fromAddressOverride, String fromNameOverride) {
        if (to == null || to.isBlank()) {
            throw new IllegalStateException("수신 이메일이 비어 있습니다.");
        }
        String fromAddr = fromAddressOverride != null && !fromAddressOverride.isBlank()
                ? fromAddressOverride.trim()
                : (s.getMailFromAddress() != null ? s.getMailFromAddress().trim() : "");
        if (fromAddr.isBlank()) {
            throw new IllegalStateException("발신 메일 주소가 비어 있습니다.");
        }
        if (s.getSmtpHost() == null || s.getSmtpHost().isBlank()) {
            throw new IllegalStateException("SMTP 호스트가 설정되지 않았습니다. 전산설정관리에서 SMTP를 입력하세요.");
        }
        int port = s.getSmtpPort() != null && s.getSmtpPort() > 0 ? s.getSmtpPort() : 587;
        boolean auth = "Y".equalsIgnoreCase(trimYn(s.getSmtpAuthYn()));
        if (auth && (s.getSmtpUsername() == null || s.getSmtpUsername().isBlank()
                || s.getSmtpPassword() == null || s.getSmtpPassword().isBlank())) {
            throw new IllegalStateException("SMTP 인증이 켜져 있으나 사용자·비밀번호가 없습니다.");
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(s.getSmtpHost().trim());
        sender.setPort(port);
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        if (auth) {
            sender.setUsername(s.getSmtpUsername().trim());
            sender.setPassword(s.getSmtpPassword());
        }

        Properties p = new Properties();
        p.put("mail.transport.protocol", "smtp");
        p.put("mail.smtp.auth", auth ? "true" : "false");
        if ("Y".equalsIgnoreCase(trimYn(s.getSmtpTlsYn()))) {
            p.put("mail.smtp.starttls.enable", "true");
        }
        sender.setJavaMailProperties(p);

        SimpleMailMessage msg = new SimpleMailMessage();
        String fromName = fromNameOverride != null && !fromNameOverride.isBlank()
                ? fromNameOverride.trim()
                : (s.getMailFromName() != null ? s.getMailFromName().trim() : "");
        if (!fromName.isEmpty()) {
            msg.setFrom(fromName + " <" + fromAddr + ">");
        } else {
            msg.setFrom(fromAddr);
        }
        msg.setTo(to.trim().split("\\s*,\\s*"));
        msg.setSubject(subject != null ? subject : "");
        msg.setText(text != null ? text : "");
        try {
            sender.send(msg);
        } catch (Exception e) {
            String m = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new IllegalStateException("메일 발송 실패: " + m, e);
        }
    }

    private static String trimYn(String v) {
        return v != null ? v.trim() : "N";
    }
}
