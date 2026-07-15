package com.pg.merchantdeploy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.pg.merchantdeploy.MerchantDeployL10n.Bundle;
import static com.pg.merchantdeploy.MerchantDeployL10n.putDescription;
import static com.pg.merchantdeploy.MerchantDeployL10n.textMap;

/**
 * 배포 연동 체크리스트·빠른 시작 — 5개 언어. 가맹점 노출용(ICOPAY만).
 */
public final class MerchantApiDeployChecklistI18n {

    private MerchantApiDeployChecklistI18n() {
    }

    /** 가맹점API / API배포문서 상단 빠른 시작 (3단계). */
    public static Map<String, Object> buildQuickStart(String publicApiBase, String compId) {
        String base = publicApiBase != null ? publicApiBase.trim() : "";
        String cid = compId != null ? compId.trim() : "{compId}";
        Map<String, Object> qs = new LinkedHashMap<>();
        putDescription(qs, new Bundle(
                "본사 API 배포 후 이 화면(업체관리 → 가맹점API)이 공식 연동 자료입니다. 아래 3단계만 구현하세요.",
                "After HQ API deploy, this screen (Company → Merchant API) is the official kit. Implement these 3 steps only.",
                "本社 API 配備後、この画面（加盟店管理→加盟店API）が公式連携資料です。次の3ステップのみ実装してください。",
                "总部完成 API 部署后，本画面（商户管理→商户API）为官方对接资料。仅实现以下 3 步。",
                "หลัง HQ ปล่อย API หน้าจอนี้ (จัดการร้าน→Merchant API) คือเอกสารทางการ ทำแค่ 3 ขั้น"
        ));
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(textMap(new Bundle(
                "Prepare: 가맹 서버에서 POST " + base + "/api/middleware/v1/merchant/checkout/prepare "
                        + "(헤더 X-Icopay-Merchant-Broker-Secret, buyer.email·phone·countryIso2 필수) → sessionToken",
                "Prepare: on merchant server POST " + base + "/api/middleware/v1/merchant/checkout/prepare "
                        + "(header X-Icopay-Merchant-Broker-Secret; buyer.email, phone, countryIso2 required) → sessionToken",
                "Prepare: 加盟店サーバーで POST " + base + "/api/middleware/v1/merchant/checkout/prepare "
                        + "（ヘッダ X-Icopay-Merchant-Broker-Secret、buyer 必須）→ sessionToken",
                "Prepare：在商户服务器 POST " + base + "/api/middleware/v1/merchant/checkout/prepare "
                        + "（头 X-Icopay-Merchant-Broker-Secret，buyer 必填）→ sessionToken",
                "Prepare: บนเซิร์ฟเวอร์ร้าน POST " + base + "/api/middleware/v1/merchant/checkout/prepare → sessionToken"
        )));
        steps.add(textMap(new Bundle(
                "결제창: 브라우저에 /v1/embed-checkout/" + cid + " 또는 응답 payUrl(/checkout/" + cid + ")만 전달",
                "Checkout UI: pass only /v1/embed-checkout/" + cid + " or response payUrl (/checkout/" + cid + ") to the browser",
                "決済画面: ブラウザには /v1/embed-checkout/" + cid + " または payUrl(/checkout/" + cid + ") のみ",
                "支付窗：浏览器仅传 /v1/embed-checkout/" + cid + " 或 payUrl(/checkout/" + cid + ")",
                "หน้าชำระ: ส่งแค่ /v1/embed-checkout/" + cid + " หรือ payUrl (/checkout/" + cid + ") ไปเบราว์เซอร์"
        )));
        steps.add(textMap(new Bundle(
                "확정: 서버에서 Status API 또는 등록 Webhook으로 PAID 확인 (브라우저 결과만으로 확정 금지)",
                "Confirm: on the server use Status API or registered Webhook for PAID (do not rely on browser alone)",
                "確定: サーバーで Status API または Webhook で PAID 確認（ブラウザのみで確定しない）",
                "确认：在服务器用 Status API 或已登记 Webhook 确认 PAID（勿仅凭浏览器）",
                "ยืนยัน: ที่เซิร์ฟเวอร์ใช้ Status API หรือ Webhook สำหรับ PAID"
        )));
        qs.put("steps", List.copyOf(steps));
        return qs;
    }

    public static List<Map<String, Object>> build(String publicApiBase, String compId) {
        String base = publicApiBase != null ? publicApiBase.trim() : "";
        String cid = compId != null ? compId.trim() : "{compId}";
        String hdr = MerchantBrokerAccessVerifier.HEADER_MERCHANT_BROKER_SECRET;
        List<Map<String, Object>> list = new ArrayList<>();

        list.add(textMap(new Bundle(
                "가맹점 로그인 → 업체관리 → 가맹점API에서 키·엔드포인트·파라미터 표·샘플이 보이는지 확인 (공식 전달 경로)",
                "Confirm keys, endpoints, parameter tables, and samples appear under Company → Merchant API after login (official delivery)",
                "加盟店ログイン→加盟店管理→加盟店APIでキー・エンドポイント・パラメータ表・サンプルが表示されるか確認（公式経路）",
                "商户登录→商户管理→商户API 可见密钥、端点、参数表、样例（官方交付）",
                "เข้าสู่ระบบร้าน→จัดการร้าน→Merchant API มีคีย์/endpoint/ตาราง/ตัวอย่าง (ช่องทางทางการ)"
        )));

        list.add(textMap(new Bundle(
                "공개 API 베이스 URL이 가맹 서버 설정과 일치하는지 확인: " + base,
                "Confirm public API base URL matches merchant server config: " + base,
                "公開 API ベース URL が加盟店サーバー設定と一致するか: " + base,
                "确认公开 API 基址与商户服务器配置一致: " + base,
                "ตรวจว่า public API base URL ตรงการตั้งค่าเซิร์ฟเวอร์ร้าน: " + base
        )));

        list.add(textMap(new Bundle(
                "PHP/JSP 샘플: " + base + "/merchant-api-samples/ (README.txt · icopay_config · IcopayMerchantApi · checkout_unified)",
                "PHP/JSP samples: " + base + "/merchant-api-samples/ (README.txt · icopay_config · IcopayMerchantApi · checkout_unified)",
                "PHP/JSP サンプル: " + base + "/merchant-api-samples/",
                "PHP/JSP 示例: " + base + "/merchant-api-samples/",
                "ตัวอย่าง PHP/JSP: " + base + "/merchant-api-samples/"
        )));

        list.add(textMap(new Bundle(
                "ICOPAY 통합 인라인(권장): POST " + base + "/api/middleware/v1/merchant/checkout/prepare "
                        + "(buyer.email·phone·countryIso2 필수) → sessionToken → /v1/embed-checkout/" + cid,
                "ICOPAY unified inline (recommended): POST " + base + "/api/middleware/v1/merchant/checkout/prepare "
                        + "(buyer.email, phone, countryIso2 required) → sessionToken → /v1/embed-checkout/" + cid,
                "ICOPAY 統合インライン（推奨）: POST " + base + "/api/middleware/v1/merchant/checkout/prepare "
                        + "→ sessionToken → /v1/embed-checkout/" + cid,
                "ICOPAY 统一内联（推荐）: POST " + base + "/api/middleware/v1/merchant/checkout/prepare "
                        + "→ sessionToken → /v1/embed-checkout/" + cid,
                "ICOPAY อินไลน์รวม (แนะนำ): POST " + base + "/api/middleware/v1/merchant/checkout/prepare "
                        + "→ sessionToken → /v1/embed-checkout/" + cid
        )));

        list.add(textMap(new Bundle(
                "연동 파라미터 규격(표): 가맹점API 화면 또는 "
                        + base + "/merchant-api-samples/docs/unified-checkout-api-parameters.html",
                "Parameter spec (table): Merchant API screen or "
                        + base + "/merchant-api-samples/docs/unified-checkout-api-parameters.html",
                "連携パラメータ仕様（表）: 加盟店API画面または "
                        + base + "/merchant-api-samples/docs/unified-checkout-api-parameters.html",
                "对接参数规范（表）: 商户API 画面或 "
                        + base + "/merchant-api-samples/docs/unified-checkout-api-parameters.html",
                "สเปคพารามิเตอร์: หน้า Merchant API หรือ "
                        + base + "/merchant-api-samples/docs/unified-checkout-api-parameters.html"
        )));

        list.add(textMap(new Bundle(
                "연동 흐름 문서: " + base + "/merchant-api-samples/docs/unified-checkout-api-flow.ko.html (EN/JA 동일 경로)",
                "Integration flow doc: " + base + "/merchant-api-samples/docs/unified-checkout-api-flow.html",
                "連携フロー文書: " + base + "/merchant-api-samples/docs/unified-checkout-api-flow.ja.html",
                "对接流程文档: " + base + "/merchant-api-samples/docs/unified-checkout-api-flow.html",
                "เอกสาร flow: " + base + "/merchant-api-samples/docs/unified-checkout-api-flow.html"
        )));

        list.add(textMap(new Bundle(
                "ICOPAY 통합 구독(정기결제): POST " + base + "/api/middleware/v1/merchant/checkout/subscription/prepare "
                        + "→ sessionToken → /v1/embed-checkout-subscribe/" + cid,
                "ICOPAY unified subscription: POST " + base + "/api/middleware/v1/merchant/checkout/subscription/prepare "
                        + "→ sessionToken → /v1/embed-checkout-subscribe/" + cid,
                "ICOPAY 統合サブスク: POST " + base + "/api/middleware/v1/merchant/checkout/subscription/prepare "
                        + "→ sessionToken → /v1/embed-checkout-subscribe/" + cid,
                "ICOPAY 统一订阅: POST " + base + "/api/middleware/v1/merchant/checkout/subscription/prepare "
                        + "→ sessionToken → /v1/embed-checkout-subscribe/" + cid,
                "ICOPAY subscription: POST " + base + "/api/middleware/v1/merchant/checkout/subscription/prepare "
                        + "→ /v1/embed-checkout-subscribe/" + cid
        )));

        list.add(textMap(new Bundle(
                "가맹 PHP/JSP: prepare는 가맹 서버에서만 호출(브로커 시크릿 노출 금지). 브라우저에는 sessionToken·embed만 전달",
                "Merchant PHP/JSP: call prepare only on the merchant server (never expose broker secret). Pass only sessionToken and embed to the browser.",
                "加盟店 PHP/JSP: prepare は加盟店サーバーでのみ呼び出し（ブローカーシークレットを露出しない）",
                "商户 PHP/JSP：prepare 仅在商户服务器调用（勿暴露 broker secret）",
                "PHP/JSP ร้าน: เรียก prepare เฉพาะบนเซิร์ฟเวอร์ร้าน"
        )));

        list.add(textMap(new Bundle(
                "결제망 콜백·리다이렉트·노티 URL은 ICOPAY(본사)에서 구성·관리 — 가맹점은 별도 설정 불필요",
                "Payment-network callback/redirect/notify URLs are configured by ICOPAY (HQ) — no separate merchant setup.",
                "決済網のコールバック・リダイレクト・ノティ URL は ICOPAY（本社）が構成・管理",
                "支付通道回调/重定向/通知 URL 由 ICOPAY（总部）配置管理",
                "URL callback/redirect/notify ของเครือข่ายชำระ ICOPAY (HQ) จัดการให้"
        )));

        list.add(textMap(new Bundle(
                "브로커 시크릿 「강제」 시 Checkout prepare/status 등 호출에 " + hdr + " 헤더 필수",
                "When broker secret enforce is on, " + hdr + " header is required for Checkout prepare/status calls.",
                "ブローカーシークレット「強制」時、Checkout prepare/status に " + hdr + " ヘッダ必須",
                "broker 密钥「强制」时，Checkout prepare/status 必须带 " + hdr + " 头",
                "เมื่อบังคับ broker secret ต้องมี header " + hdr + " สำหรับ Checkout prepare/status"
        )));

        list.add(textMap(new Bundle(
                "통합 리다이렉트: POST " + base + "/api/middleware/v1/merchant/checkout/redirect/prepare "
                        + "(buyer 필수, returnUrl/cancelUrl body 금지) → payUrl → Status·웹훅",
                "Unified redirect: POST " + base + "/api/middleware/v1/merchant/checkout/redirect/prepare "
                        + "(buyer required; no returnUrl/cancelUrl in body) → payUrl → status/webhook",
                "統合リダイレクト: POST " + base + "/api/middleware/v1/merchant/checkout/redirect/prepare "
                        + "→ payUrl → status/Webhook",
                "统一重定向: POST " + base + "/api/middleware/v1/merchant/checkout/redirect/prepare "
                        + "→ payUrl → status/webhook",
                "Unified redirect: POST " + base + "/api/middleware/v1/merchant/checkout/redirect/prepare → payUrl"
        )));

        list.add(textMap(new Bundle(
                "WordPress: " + base + " 측 제공 ZIP(woocommerce/icopay-woocommerce-*.zip) — 기본 inline, redirect는 HQ 리다이렉트 채널 ON + flow_mode=redirect",
                "WordPress: use provided ZIP (icopay-woocommerce-*.zip) — default inline; redirect needs HQ redirect channel ON + flow_mode=redirect",
                "WordPress: 提供 ZIP（icopay-woocommerce）— 既定 inline、redirect は HQ ON + flow_mode=redirect",
                "WordPress：使用提供的 ZIP（icopay-woocommerce）— 默认 inline；redirect 需 HQ 开启 + flow_mode=redirect",
                "WordPress: ใช้ ZIP ที่ให้ (icopay-woocommerce) — inline ค่าเริ่มต้น"
        )));

        list.add(textMap(new Bundle(
                "가맹 결제 통보(Webhook): 본사 merchantNotifyUrls에 가맹 HTTPS URL 등록 — "
                        + "예: https://{도메인}/wp-json/icopay/v1/webhook",
                "Merchant payment webhook: register HTTPS URL in HQ merchantNotifyUrls — "
                        + "e.g. https://{domain}/wp-json/icopay/v1/webhook",
                "加盟店 Webhook: 本社 merchantNotifyUrls に HTTPS 登録 — "
                        + "例 https://{ドメイン}/wp-json/icopay/v1/webhook",
                "商户 Webhook: 在总部 merchantNotifyUrls 登记 HTTPS — "
                        + "例 https://{域名}/wp-json/icopay/v1/webhook",
                "Webhook ร้าน: ลงทะเบียน HTTPS ที่ HQ merchantNotifyUrls — /wp-json/icopay/v1/webhook"
        )));

        list.add(textMap(new Bundle(
                "브라우저 복귀 URL은 prepare body에 넣지 않음 — ICOPAY NOTI Result → 가맹, 확정은 Webhook·Status API",
                "Do not put browser return URL in prepare body — ICOPAY NOTI Result → merchant; confirm via webhook/Status API",
                "ブラウザ復帰 URL は prepare に入れない — NOTI Result → 加盟店、確定は Webhook/Status API",
                "浏览器返回 URL 勿放入 prepare — NOTI Result → 商户，用 Webhook/Status API 确认",
                "อย่าใส่ URL กลับเบราว์เซอร์ใน prepare — NOTI Result → ร้าน ยืนยันด้วย webhook/Status"
        )));

        return List.copyOf(list);
    }
}
