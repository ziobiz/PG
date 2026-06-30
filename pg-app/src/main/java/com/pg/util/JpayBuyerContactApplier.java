package com.pg.util;

import com.pg.entity.PgTrnsctn;

import java.util.Locale;
import java.util.Map;

import static com.pg.util.PayerContactDisplayUtil.isGuestMarker;

/**
 * JPAY 결제·노티에서 구매자 연락처·마스킹 카드번호를 {@link PgTrnsctn}에 반영.
 * <ul>
 *   <li>{@code customer_id} — Customer Email</li>
 *   <li>{@code customer_nm} — 고객 성명</li>
 *   <li>{@code customer_tel} — 전화(로컬 번호, 국가코드 제외)</li>
 *   <li>{@code card_pan_display} — 414520***8306 형식</li>
 * </ul>
 */
public final class JpayBuyerContactApplier {

    private JpayBuyerContactApplier() {
    }

    public static void applyFromSaleBody(PgTrnsctn t, Map<String, Object> body) {
        if (t == null || body == null || body.isEmpty()) {
            return;
        }
        String email = first(body, "payEmailAddress", "pay_email_address", "email");
        String first = first(body, "payFirstname", "pay_firstname", "firstname");
        String last = first(body, "payLastname", "pay_lastname", "lastname");
        String tel = first(body, "payTelephone", "pay_telephone", "telephone", "phone");
        String card = first(body, "payCardno", "pay_cardno", "cardno", "card_no");
        applyResolved(t, email, JpayCardPanMaskUtil.formatBuyerName(first, last), tel, card, false);
    }

    /** JPAY 포털 Export 행 — Customer Email·Name 보강(guest·빈 값만). */
    public static void mergeFromPortalExportRow(PgTrnsctn t, String customerEmail, String customerName) {
        if (t == null) {
            return;
        }
        applyResolved(t, customerEmail, customerName, null, null, true);
    }

    /** 노티·동기 복귀 — 값이 있을 때만 보강(기존 checkout 데이터 유지). */
    public static void mergeFromNotifyForm(PgTrnsctn t, Map<String, String> form) {
        if (t == null || form == null || form.isEmpty()) {
            return;
        }
        String email = mapFirst(form,
                "email", "email_address", "pay_email_address", "customer_email", "payemailaddress");
        String first = mapFirst(form, "firstname", "pay_firstname", "payfirstname", "first_name");
        String last = mapFirst(form, "lastname", "pay_lastname", "paylastname", "last_name");
        String name = mapFirst(form, "customername", "customer_name", "payername", "username");
        if (name == null || name.isBlank()) {
            name = JpayCardPanMaskUtil.formatBuyerName(first, last);
        }
        String tel = mapFirst(form, "telephone", "phone", "pay_telephone", "paytelephone", "mobile");
        String card = mapFirst(form, "cardno", "card_no", "pay_cardno", "paycardno", "cardnumber", "card_number");
        applyResolved(t, email, name, tel, card, true);
    }

    private static void applyResolved(PgTrnsctn t,
                                      String email,
                                      String name,
                                      String tel,
                                      String cardRaw,
                                      boolean mergeOnly) {
        if (!mergeOnly || isBlank(t.getCustomerId()) || isGuestMarker(String.valueOf(t.getCustomerId()))) {
            String em = JpayCardPanMaskUtil.truncate(email, 100);
            if (em != null && !em.isBlank()) {
                t.setCustomerId(em);
            } else if (!mergeOnly && (t.getCustomerId() == null || t.getCustomerId().isBlank())) {
                t.setCustomerId("guest");
            }
        }
        if (!mergeOnly || isBlank(t.getCustomerNm())) {
            String nm = JpayCardPanMaskUtil.truncate(name, 200);
            if (nm != null && !nm.isBlank()) {
                t.setCustomerNm(nm);
            }
        }
        if (!mergeOnly || isBlank(t.getCustomerTel())) {
            String ph = JpayCardPanMaskUtil.truncate(tel, 50);
            if (ph != null && !ph.isBlank()) {
                t.setCustomerTel(ph);
            }
        }
        if (!mergeOnly || isBlank(t.getCardPanDisplay())) {
            String pan = JpayCardPanMaskUtil.normalizeDisplay(cardRaw);
            if (pan != null && !pan.isBlank()) {
                t.setCardPanDisplay(pan.length() > 32 ? pan.substring(0, 32) : pan);
            }
        }
    }

    private static String first(Map<String, Object> body, String... keys) {
        for (String k : keys) {
            if (k == null) {
                continue;
            }
            Object v = body.get(k);
            if (v == null) {
                continue;
            }
            String s = String.valueOf(v).trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        return null;
    }

    private static String mapFirst(Map<String, String> form, String... keys) {
        for (String k : keys) {
            if (k == null) {
                continue;
            }
            String v = form.get(k.toLowerCase(Locale.ROOT));
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
