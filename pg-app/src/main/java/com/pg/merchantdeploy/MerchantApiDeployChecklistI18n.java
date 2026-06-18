package com.pg.merchantdeploy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.pg.merchantdeploy.MerchantDeployL10n.Bundle;
import static com.pg.merchantdeploy.MerchantDeployL10n.textMap;

/**
 * 배포 연동 체크리스트 — 5개 언어.
 */
public final class MerchantApiDeployChecklistI18n {

    private MerchantApiDeployChecklistI18n() {
    }

    public static List<Map<String, Object>> build(String publicApiBase, String compId) {
        String base = publicApiBase != null ? publicApiBase.trim() : "";
        String cid = compId != null ? compId.trim() : "{compId}";
        String hdr = MerchantBrokerAccessVerifier.HEADER_MERCHANT_BROKER_SECRET;
        List<Map<String, Object>> list = new ArrayList<>();

        list.add(textMap(new Bundle(
                "API배포설정의 publicApiBaseUrl(또는 노티 publicBaseUrl)이 가맹점·PG사에 알려준 도메인과 일치하는지 확인",
                "Confirm API deployment publicApiBaseUrl (or notify publicBaseUrl) matches the domain shared with merchants and PGs.",
                "API配信設定の publicApiBaseUrl（またはノティ publicBaseUrl）が、加盟店・PGに案内したドメインと一致するか確認",
                "确认 API 部署 publicApiBaseUrl（或通知 publicBaseUrl）与告知商户、PG 的域名一致。",
                "ตรวจว่า publicApiBaseUrl การตั้งค่า API (หรือ notify publicBaseUrl) ตรงโดเมนที่แจ้งร้านและ PG"
        )));

        list.add(textMap(new Bundle(
                "PHP/JSP 샘플: " + base + "/merchant-api-samples/ (README.txt · icopay_config · IcopayMerchantApi)",
                "PHP/JSP samples: " + base + "/merchant-api-samples/ (README.txt · icopay_config · IcopayMerchantApi)",
                "PHP/JSP サンプル: " + base + "/merchant-api-samples/ (README.txt · icopay_config · IcopayMerchantApi)",
                "PHP/JSP 示例: " + base + "/merchant-api-samples/ (README.txt · icopay_config · IcopayMerchantApi)",
                "ตัวอย่าง PHP/JSP: " + base + "/merchant-api-samples/ (README.txt · icopay_config · IcopayMerchantApi)"
        )));

        list.add(textMap(new Bundle(
                "통합 인라인(권장): POST " + base + "/api/middleware/v1/merchant/checkout/prepare "
                        + "(buyer.email·phone·countryIso2 필수) → sessionToken → /v1/embed-checkout/" + cid + " (운영 PG 자동 분기)",
                "Unified inline (recommended): POST " + base + "/api/middleware/v1/merchant/checkout/prepare "
                        + "(buyer.email, phone, countryIso2 required) → sessionToken → /v1/embed-checkout/" + cid,
                "統合インライン（推奨）: POST " + base + "/api/middleware/v1/merchant/checkout/prepare "
                        + "（buyer.email・phone・countryIso2 必須）→ sessionToken → /v1/embed-checkout/" + cid,
                "统一内联（推荐）: POST " + base + "/api/middleware/v1/merchant/checkout/prepare "
                        + "（必填 buyer.email·phone·countryIso2）→ sessionToken → /v1/embed-checkout/" + cid,
                "อินไลน์รวม (แนะนำ): POST " + base + "/api/middleware/v1/merchant/checkout/prepare "
                        + "(ต้องมี buyer.email·phone·countryIso2) → sessionToken → /v1/embed-checkout/" + cid
        )));

        list.add(textMap(new Bundle(
                "연동 파라미터 규격(표): 키트 merchantCheckoutApiParameterSpec · "
                        + base + "/merchant-api-samples/docs/unified-checkout-api-parameters.html",
                "Parameter spec (table): kit merchantCheckoutApiParameterSpec · "
                        + base + "/merchant-api-samples/docs/unified-checkout-api-parameters.html",
                "連携パラメータ仕様（表）: キット merchantCheckoutApiParameterSpec · "
                        + base + "/merchant-api-samples/docs/unified-checkout-api-parameters.html",
                "对接参数规范（表）: 套件 merchantCheckoutApiParameterSpec · "
                        + base + "/merchant-api-samples/docs/unified-checkout-api-parameters.html",
                "สเปคพารามิเตอร์ (ตาราง): ชุด merchantCheckoutApiParameterSpec · "
                        + base + "/merchant-api-samples/docs/unified-checkout-api-parameters.html"
        )));

        list.add(textMap(new Bundle(
                "연동 흐름 문서(Prepare·Session·Status·Embed): KO "
                        + base + "/merchant-api-samples/docs/unified-checkout-api-flow.ko.html · EN/JA 동일 경로",
                "Integration flow doc (Prepare·Session·Status·Embed): "
                        + base + "/merchant-api-samples/docs/unified-checkout-api-flow.html (KO/JA variants)",
                "連携フロー文書: JA " + base + "/merchant-api-samples/docs/unified-checkout-api-flow.ja.html · EN/KO 同パス",
                "对接流程文档: " + base + "/merchant-api-samples/docs/unified-checkout-api-flow.html（KO/JA 版あり）",
                "เอกสาร flow การเชื่อมต่อ: " + base + "/merchant-api-samples/docs/unified-checkout-api-flow.html (มี KO/JA)"
        )));

        list.add(textMap(new Bundle(
                "ChillPay 인라인(레거시): POST " + base + "/api/middleware/v1/merchant/chillpay/inline-checkout/prepare "
                        + "→ sessionToken → /v1/embed-pay/" + cid,
                "ChillPay inline (legacy): POST " + base + "/api/middleware/v1/merchant/chillpay/inline-checkout/prepare "
                        + "→ sessionToken → /v1/embed-pay/" + cid,
                "ChillPay インライン（レガシー）: POST " + base + "/api/middleware/v1/merchant/chillpay/inline-checkout/prepare "
                        + "→ sessionToken → /v1/embed-pay/" + cid,
                "ChillPay 内联（遗留）: POST " + base + "/api/middleware/v1/merchant/chillpay/inline-checkout/prepare "
                        + "→ sessionToken → /v1/embed-pay/" + cid,
                "ChillPay อินไลน์ (เดิม): POST " + base + "/api/middleware/v1/merchant/chillpay/inline-checkout/prepare "
                        + "→ sessionToken → /v1/embed-pay/" + cid
        )));

        list.add(textMap(new Bundle(
                "JPAY 인라인(레거시): POST " + base + "/api/middleware/v1/merchant/jpay/inline-checkout/prepare "
                        + "→ sessionToken → /v1/embed-jpay-pay/" + cid,
                "JPAY inline (legacy): POST " + base + "/api/middleware/v1/merchant/jpay/inline-checkout/prepare "
                        + "→ sessionToken → /v1/embed-jpay-pay/" + cid,
                "JPAY インライン（レガシー）: POST " + base + "/api/middleware/v1/merchant/jpay/inline-checkout/prepare "
                        + "→ sessionToken → /v1/embed-jpay-pay/" + cid,
                "JPAY 内联（遗留）: POST " + base + "/api/middleware/v1/merchant/jpay/inline-checkout/prepare "
                        + "→ sessionToken → /v1/embed-jpay-pay/" + cid,
                "JPAY อินไลน์ (เดิม): POST " + base + "/api/middleware/v1/merchant/jpay/inline-checkout/prepare "
                        + "→ sessionToken → /v1/embed-jpay-pay/" + cid
        )));

        list.add(textMap(new Bundle(
                "JPAY 구독: POST " + base + "/api/middleware/v1/merchant/jpay/subscription/prepare "
                        + "→ sessionToken → /v1/embed-jpay-subscribe/" + cid,
                "JPAY subscription: POST " + base + "/api/middleware/v1/merchant/jpay/subscription/prepare "
                        + "→ sessionToken → /v1/embed-jpay-subscribe/" + cid,
                "JPAY サブスク: POST " + base + "/api/middleware/v1/merchant/jpay/subscription/prepare "
                        + "→ sessionToken → /v1/embed-jpay-subscribe/" + cid,
                "JPAY 订阅: POST " + base + "/api/middleware/v1/merchant/jpay/subscription/prepare "
                        + "→ sessionToken → /v1/embed-jpay-subscribe/" + cid,
                "JPAY สมัครสมาชิก: POST " + base + "/api/middleware/v1/merchant/jpay/subscription/prepare "
                        + "→ sessionToken → /v1/embed-jpay-subscribe/" + cid
        )));

        list.add(textMap(new Bundle(
                "가맹 PHP/JSP: prepare는 가맹 서버에서만 호출(브로커 시크릿 노출 금지). 브라우저에는 sessionToken·embed만 전달",
                "Merchant PHP/JSP: call prepare only on the merchant server (never expose broker secret). Pass only sessionToken and embed to the browser.",
                "加盟店 PHP/JSP: prepare は加盟店サーバーでのみ呼び出し（ブローカーシークレットを露出しない）。ブラウザには sessionToken と embed のみ",
                "商户 PHP/JSP：prepare 仅在商户服务器调用（勿暴露 broker secret）。浏览器仅传 sessionToken 与 embed",
                "PHP/JSP ร้าน: เรียก prepare เฉพาะบนเซิร์ฟเวอร์ร้าน (ห้ามเปิด broker secret) ส่งแค่ sessionToken และ embed ไปเบราว์เซอร์"
        )));

        list.add(textMap(new Bundle(
                "ChillPay 콜백·리다이렉트 URL은 본사 API연동설정·노티구성과 동일하게 유지",
                "Keep ChillPay callback and redirect URLs aligned with HQ API integration and notify settings.",
                "ChillPay のコールバック・リダイレクト URL は本社 API 連携・ノティ設定と一致させる",
                "ChillPay 回调与重定向 URL 须与总部 API 联动、通知配置一致",
                "คง URL callback/redirect ของ ChillPay ให้ตรงการตั้งค่า API และแจ้งเตือนของ HQ"
        )));

        list.add(textMap(new Bundle(
                "JPAY pay_notifyurl·콜백은 기본 notifyIngressUrlMiddleware 경로 사용. 레거시만 tb_pg_agency credentials_extra_json jpayNotifyIngressStyle=OPEN",
                "JPAY pay_notifyurl/callbacks default to notifyIngressUrlMiddleware. Legacy only: jpayNotifyIngressStyle=OPEN in credentials_extra_json.",
                "JPAY pay_notifyurl・コールバックは既定で notifyIngressUrlMiddleware。レガシーのみ credentials_extra_json で jpayNotifyIngressStyle=OPEN",
                "JPAY pay_notifyurl/回调默认走 notifyIngressUrlMiddleware。仅遗留时在 credentials_extra_json 设 jpayNotifyIngressStyle=OPEN",
                "JPAY pay_notifyurl/ callback ใช้ notifyIngressUrlMiddleware เป็นค่าเริ่มต้น แบบเดิมตั้ง jpayNotifyIngressStyle=OPEN ใน credentials_extra_json"
        )));

        list.add(textMap(new Bundle(
                "브로커 시크릿 「강제」 시 /api/middleware/v1/pg/... 호출에 " + hdr + " 헤더 필수",
                "When broker secret enforce is on, " + hdr + " header is required for /api/middleware/v1/pg/... calls.",
                "ブローカーシークレット「強制」時、/api/middleware/v1/pg/... 呼び出しに " + hdr + " ヘッダ必須",
                "broker 密钥「强制」时，调用 /api/middleware/v1/pg/... 必须带 " + hdr + " 头",
                "เมื่อบังคับ broker secret ต้องมี header " + hdr + " สำหรับ /api/middleware/v1/pg/..."
        )));

        list.add(textMap(new Bundle(
                "레거시 /api/pay/... 경로는 시크릿 검증 없음(이행 기간용)",
                "Legacy /api/pay/... paths skip secret verification (migration period).",
                "レガシー /api/pay/... はシークレット検証なし（移行期間）",
                "遗留 /api/pay/... 路径不校验密钥（过渡期）",
                "เส้นทางเดิม /api/pay/... ไม่ตรวจ secret (ช่วงย้าย)"
        )));

        list.add(textMap(new Bundle(
                "통합 리다이렉트: POST " + base + "/api/middleware/v1/merchant/checkout/redirect/prepare "
                        + "(buyer·returnUrl HTTPS·cancelUrl) → data.payUrl 브라우저 이동 → status/웹훅",
                "Unified redirect: POST " + base + "/api/middleware/v1/merchant/checkout/redirect/prepare "
                        + "(buyer, HTTPS returnUrl/cancelUrl) → redirect to data.payUrl → status/webhook",
                "統合リダイレクト: POST " + base + "/api/middleware/v1/merchant/checkout/redirect/prepare "
                        + "（buyer・returnUrl HTTPS）→ data.payUrl へ → status/Webhook",
                "统一重定向: POST " + base + "/api/middleware/v1/merchant/checkout/redirect/prepare "
                        + "（buyer·HTTPS returnUrl）→ 跳转 data.payUrl → status/webhook",
                "Unified redirect: POST " + base + "/api/middleware/v1/merchant/checkout/redirect/prepare "
                        + "→ redirect data.payUrl → status/webhook"
        )));

        list.add(textMap(new Bundle(
                "JPAY 리다이렉트: POST " + base + "/api/middleware/v1/merchant/jpay/redirect-checkout/prepare "
                        + "→ payUrl (returnUrl/cancelUrl HTTPS)",
                "JPAY redirect: POST " + base + "/api/middleware/v1/merchant/jpay/redirect-checkout/prepare "
                        + "→ payUrl (HTTPS returnUrl/cancelUrl)",
                "JPAY リダイレクト: POST " + base + "/api/middleware/v1/merchant/jpay/redirect-checkout/prepare "
                        + "→ payUrl（returnUrl/cancelUrl HTTPS）",
                "JPAY 重定向: POST " + base + "/api/middleware/v1/merchant/jpay/redirect-checkout/prepare "
                        + "→ payUrl（HTTPS returnUrl/cancelUrl）",
                "JPAY redirect: POST " + base + "/api/middleware/v1/merchant/jpay/redirect-checkout/prepare → payUrl"
        )));

        list.add(textMap(new Bundle(
                "ChillPay 리다이렉트: POST " + base + "/api/middleware/v1/merchant/chillpay/redirect-checkout/prepare "
                        + "→ payUrl (returnUrl/cancelUrl HTTPS)",
                "ChillPay redirect: POST " + base + "/api/middleware/v1/merchant/chillpay/redirect-checkout/prepare "
                        + "→ payUrl (HTTPS returnUrl/cancelUrl)",
                "ChillPay リダイレクト: POST " + base + "/api/middleware/v1/merchant/chillpay/redirect-checkout/prepare "
                        + "→ payUrl",
                "ChillPay 重定向: POST " + base + "/api/middleware/v1/merchant/chillpay/redirect-checkout/prepare "
                        + "→ payUrl",
                "ChillPay redirect: POST " + base + "/api/middleware/v1/merchant/chillpay/redirect-checkout/prepare → payUrl"
        )));

        list.add(textMap(new Bundle(
                "WordPress: woocommerce/icopay-woocommerce-1.1.0.zip · wordpress/icopay-jpay-1.0.0.zip — 기본 inline, redirect는 HQ REDIRECT Y + flow_mode=redirect",
                "WordPress: icopay-woocommerce-1.1.0.zip · icopay-jpay-1.0.0.zip — default inline; redirect needs HQ REDIRECT Y + flow_mode=redirect",
                "WordPress: ZIP プラグイン — 既定 inline、redirect は HQ REDIRECT Y + flow_mode=redirect",
                "WordPress：插件 ZIP — 默认 inline；redirect 需 HQ REDIRECT Y + flow_mode=redirect",
                "WordPress: ZIP plugin — inline ค่าเริ่มต้น redirect ต้อง HQ REDIRECT Y + flow_mode=redirect"
        )));

        list.add(textMap(new Bundle(
                "가맹 결제 통보(Webhook): 본사 merchantNotifyUrls에 가맹 HTTPS URL 등록 — "
                        + "WooCommerce: https://{도메인}/wp-json/icopay/v1/webhook · "
                        + "일반 WP: https://{도메인}/wp-json/icopay-jpay/v1/webhook",
                "Merchant payment webhook: register HTTPS URL in HQ merchantNotifyUrls — "
                        + "WooCommerce: https://{domain}/wp-json/icopay/v1/webhook · "
                        + "general WP: https://{domain}/wp-json/icopay-jpay/v1/webhook",
                "加盟店 Webhook: 本社 merchantNotifyUrls に HTTPS 登録 — "
                        + "WooCommerce: https://{ドメイン}/wp-json/icopay/v1/webhook · "
                        + "一般 WP: https://{ドメイン}/wp-json/icopay-jpay/v1/webhook",
                "商户 Webhook: 在总部 merchantNotifyUrls 登记 HTTPS — "
                        + "WooCommerce: https://{域名}/wp-json/icopay/v1/webhook · "
                        + "一般 WP: https://{域名}/wp-json/icopay-jpay/v1/webhook",
                "Webhook ร้าน: ลงทะเบียน HTTPS ที่ HQ merchantNotifyUrls — WooCommerce / icopay-jpay REST"
        )));

        list.add(textMap(new Bundle(
                "notifyIngressUrlMiddleware(PG→ICOPAY)는 본사·PG pay_notifyurl 설정 — 가맹 WordPress Webhook과 다름",
                "notifyIngressUrlMiddleware (PG→ICOPAY) is HQ/PG pay_notifyurl — not the merchant WordPress webhook",
                "notifyIngressUrlMiddleware(PG→ICOPAY)は本社・PG 設定 — 加盟店 WordPress Webhook とは別",
                "notifyIngressUrlMiddleware(PG→ICOPAY) 为总部/PG 配置 — 与商户 WordPress Webhook 不同",
                "notifyIngressUrlMiddleware (PG→ICOPAY) ตั้ง HQ/PG — ไม่ใช่ Webhook WordPress ร้าน"
        )));

        list.add(textMap(new Bundle(
                "returnUrl은 브라우저 복귀용 — 결제 확정은 Webhook 또는 Status API로 서버에서 확인",
                "returnUrl is browser return only — confirm payment on the server via webhook or Status API",
                "returnUrl はブラウザ復帰用 — 確定は Webhook または Status API でサーバー確認",
                "returnUrl 仅浏览器返回 — 请在服务器通过 Webhook 或 Status API 确认",
                "returnUrl สำหรับเบราว์เซอร์ — ยืนยันที่เซิร์ฟเวอร์ด้วย webhook หรือ Status API"
        )));

        return List.copyOf(list);
    }
}
