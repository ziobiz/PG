package com.pg.service;

import com.pg.entity.HqLedgerSysSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 챗봇 주문 intent 직후 — 고객이 요청한 경우 결제 URL·금액을 이메일(SMTP) 또는 LINE Notify 로 전달합니다.
 */
@Service
public class ChatbotPayLinkDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotPayLinkDeliveryService.class);

    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final LedgerSmtpMailService ledgerSmtpMailService;
    private final LineNotifyClient lineNotifyClient;

    public ChatbotPayLinkDeliveryService(HqLedgerSysSettingsService hqLedgerSysSettingsService,
                                        LedgerSmtpMailService ledgerSmtpMailService,
                                        LineNotifyClient lineNotifyClient) {
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.ledgerSmtpMailService = ledgerSmtpMailService;
        this.lineNotifyClient = lineNotifyClient;
    }

    /**
     * {@code data} 에 발송 결과 플래그를 넣습니다. 발송 실패 시에도 주문·{@code payUrl} 은 유지됩니다.
     */
    public void deliverIfRequested(Map<String, Object> data,
                                   boolean sendPayLinkEmail,
                                   String ordererEmail,
                                   String lineNotifyToken,
                                   String compCode,
                                   String checkoutOrderNo,
                                   String itemTitle,
                                   BigDecimal amount,
                                   String currency,
                                   String payUrl) {
        if (sendPayLinkEmail) {
            try {
                HqLedgerSysSettings smtp = hqLedgerSysSettingsService.getOrCreate();
                String to = ordererEmail != null ? ordererEmail.trim() : "";
                if (to.isEmpty()) {
                    data.put("payLinkEmailSent", false);
                    data.put("payLinkEmailError", "이메일 주소가 비어 있습니다.");
                } else {
                    String subject = "[ICOPAY] 결제 안내 · " + compCode + " · #" + checkoutOrderNo;
                    String body = buildEmailBody(compCode, checkoutOrderNo, itemTitle, amount, currency, payUrl);
                    ledgerSmtpMailService.sendPlainText(smtp, to, subject, body);
                    data.put("payLinkEmailSent", true);
                }
            } catch (Exception e) {
                log.warn("챗봇 결제 링크 이메일 실패 comp={} orderNo={}: {}", compCode, checkoutOrderNo, e.getMessage());
                data.put("payLinkEmailSent", false);
                data.put("payLinkEmailError", shortErr(e));
            }
        }
        if (lineNotifyToken != null && !lineNotifyToken.isBlank()) {
            try {
                String msg = buildLineMessage(compCode, checkoutOrderNo, itemTitle, amount, currency, payUrl);
                lineNotifyClient.postNotify(lineNotifyToken, msg);
                data.put("payLinkLineSent", true);
            } catch (Exception e) {
                log.warn("챗봇 결제 링크 LINE Notify 실패 comp={} orderNo={}: {}", compCode, checkoutOrderNo, e.getMessage());
                data.put("payLinkLineSent", false);
                data.put("payLinkLineError", shortErr(e));
            }
        }
    }

    private static String buildEmailBody(String compCode, String orderNo, String title,
                                         BigDecimal amount, String currency, String payUrl) {
        String amt = amount != null ? amount.stripTrailingZeros().toPlainString() : "";
        String cur = currency != null ? currency : "";
        String t = title != null ? title : "";
        return """
                ICOPAY 챗봇 결제 안내

                가맹점 코드: %s
                주문번호: %s
                상품: %s
                금액: %s %s

                아래 링크에서 결제를 진행해 주세요.
                %s

                ※ 본 메일은 결제 페이지 안내를 위해 요청 시 자동 발송되었습니다.
                """.formatted(compCode, orderNo, t, amt, cur, payUrl);
    }

    private static String buildLineMessage(String compCode, String orderNo, String title,
                                           BigDecimal amount, String currency, String payUrl) {
        String amt = amount != null ? amount.stripTrailingZeros().toPlainString() : "";
        String cur = currency != null ? currency : "";
        String t = title != null ? title : "";
        StringBuilder sb = new StringBuilder();
        sb.append("[ICOPAY] 챗봇 결제 안내\n");
        sb.append("가맹: ").append(compCode).append('\n');
        sb.append("주문: ").append(orderNo).append('\n');
        sb.append("상품: ").append(t).append('\n');
        sb.append("금액: ").append(amt).append(' ').append(cur).append("\n\n");
        sb.append("결제 링크\n").append(payUrl);
        return sb.toString();
    }

    private static String shortErr(Exception e) {
        String m = e.getMessage();
        if (m == null || m.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return m.length() > 200 ? m.substring(0, 197) + "..." : m;
    }
}
