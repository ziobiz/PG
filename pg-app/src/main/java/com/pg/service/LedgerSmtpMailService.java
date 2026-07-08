package com.pg.service;

import com.pg.entity.HqLedgerSysSettings;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * 전산설정(tb_hq_ledger_sys_settings) SMTP 값으로 단발 메일 발송.
 * <p>발신 표시명·일본어/한글 등 비ASCII 는 {@link InternetAddress} UTF-8 로 설정합니다.
 * {@code mail_from_address} 에 {@code Name &lt;email&gt;} 형태가 저장돼 있어도 bare 주소만 추출합니다.
 */
@Service
public class LedgerSmtpMailService {

    private static final Pattern BARE_EMAIL = Pattern.compile("^[^@\\s<>]+@[^@\\s<>]+\\.[^@\\s<>]+$");

    public void sendPlainText(HqLedgerSysSettings s, String to, String subject, String text) {
        sendPlainText(s, to, subject, text, null, null);
    }

    public void sendPlainText(HqLedgerSysSettings s, String to, String subject, String text,
                              String fromAddressOverride, String fromNameOverride) {
        sendMultipart(s, to, subject, text, null, fromAddressOverride, fromNameOverride);
    }

    /** HTML 본문 + plain text 대체 본문 */
    public void sendHtml(HqLedgerSysSettings s, String to, String subject, String htmlBody, String plainTextFallback) {
        sendMultipart(s, to, subject, plainTextFallback, htmlBody, null, null);
    }

    private void sendMultipart(HqLedgerSysSettings s, String to, String subject, String plainText,
                               String htmlBody, String fromAddressOverride, String fromNameOverride) {
        if (to == null || to.isBlank()) {
            throw new IllegalStateException("수신 이메일이 비어 있습니다.");
        }
        String fromAddrRaw = fromAddressOverride != null && !fromAddressOverride.isBlank()
                ? fromAddressOverride.trim()
                : (s.getMailFromAddress() != null ? s.getMailFromAddress().trim() : "");
        if (fromAddrRaw.isBlank()) {
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

        String fromNameRaw = fromNameOverride != null && !fromNameOverride.isBlank()
                ? fromNameOverride.trim()
                : (s.getMailFromName() != null ? s.getMailFromName().trim() : "");

        try {
            InternetAddress from = buildFromAddress(fromAddrRaw, fromNameRaw);
            InternetAddress[] recipients = parseRecipients(to);
            MimeMessage message = sender.createMimeMessage();
            boolean multipart = htmlBody != null && !htmlBody.isBlank();
            MimeMessageHelper helper = new MimeMessageHelper(message, multipart, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(recipients);
            helper.setSubject(subject != null ? subject : "");
            if (multipart) {
                helper.setText(plainText != null ? plainText : "", htmlBody);
            } else {
                helper.setText(plainText != null ? plainText : "", false);
            }
            sender.send(message);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            String m = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new IllegalStateException("메일 발송 실패: " + m, e);
        }
    }

    static InternetAddress buildFromAddress(String fromAddrRaw, String fromNameRaw) throws AddressException {
        String email = normalizeBareEmail(fromAddrRaw);
        if (email.isBlank()) {
            throw new IllegalStateException("발신 메일 주소가 비어 있습니다.");
        }
        if (!looksLikeBareEmail(email)) {
            throw new IllegalStateException(
                    "발신 메일 주소 형식이 올바르지 않습니다. 전산설정관리의 「발신 메일 주소」에 example@domain.com 형태만 입력하세요.");
        }
        String displayName = sanitizeDisplayName(fromNameRaw);
        if (displayName.isEmpty()) {
            return new InternetAddress(email);
        }
        try {
            return new InternetAddress(email, displayName, StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException e) {
            throw new AddressException("발신 표시명 인코딩 실패");
        }
    }

    static InternetAddress[] parseRecipients(String to) throws AddressException {
        String[] parts = to.trim().split("\\s*,\\s*");
        List<InternetAddress> list = new ArrayList<>();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            String email = normalizeBareEmail(part);
            if (!looksLikeBareEmail(email)) {
                throw new IllegalStateException("수신 이메일 형식이 올바르지 않습니다: " + email);
            }
            list.add(new InternetAddress(email));
        }
        if (list.isEmpty()) {
            throw new IllegalStateException("수신 이메일이 비어 있습니다.");
        }
        return list.toArray(new InternetAddress[0]);
    }

    /**
     * {@code Name <user@host>} 또는 RFC822 한 덩어리에서 bare email 만 추출.
     */
    static String normalizeBareEmail(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return "";
        }
        try {
            InternetAddress[] parsed = InternetAddress.parse(t, false);
            if (parsed.length > 0) {
                String addr = parsed[0].getAddress();
                if (addr != null && !addr.isBlank()) {
                    return addr.trim();
                }
            }
        } catch (AddressException ignored) {
            /* 수동 분리 시도 */
        }
        int lt = t.lastIndexOf('<');
        int gt = t.lastIndexOf('>');
        if (lt >= 0 && gt > lt) {
            return t.substring(lt + 1, gt).trim();
        }
        return t;
    }

    static String sanitizeDisplayName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim()
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace("<", "(")
                .replace(">", ")");
    }

    static boolean looksLikeBareEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return BARE_EMAIL.matcher(email.trim().toLowerCase(Locale.ROOT)).matches()
                || BARE_EMAIL.matcher(email.trim()).matches();
    }

    private static String trimYn(String v) {
        return v != null ? v.trim() : "N";
    }
}
