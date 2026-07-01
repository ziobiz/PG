package com.pg.merchantdeploy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.pg.merchantdeploy.MerchantDeployL10n.Bundle;
import static com.pg.merchantdeploy.MerchantDeployL10n.putDescription;
import static com.pg.merchantdeploy.MerchantDeployL10n.putMeaning;
import static com.pg.merchantdeploy.MerchantDeployL10n.putRemark;
import static com.pg.merchantdeploy.MerchantDeployL10n.putTextFields;
import static com.pg.merchantdeploy.MerchantDeployL10n.textMap;

/**
 * 가맹점 통합 checkout API — prepare·status 요청 파라미터 규격(배포 키트·문서용, 5개 언어).
 */
public final class MerchantCheckoutApiParameterSpec {

    private MerchantCheckoutApiParameterSpec() {
    }

    public static Map<String, Object> build(String publicApiBase, String compId) {
        String base = publicApiBase != null ? publicApiBase.trim() : "";
        String cid = compId != null ? compId.trim() : "";
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("specVersion", "1.1");
        spec.put("specId", "ICOPAY-CHECKOUT-PREPARE-001");
        putTextFields(spec, "title", new Bundle(
                "표 1.1: ICOPAY 통합 Checkout Prepare API 요청 파라미터",
                "Table 1.1: ICOPAY Unified Checkout Prepare API Request Parameters",
                "表 1.1: ICOPAY 統合 Checkout Prepare API リクエストパラメータ",
                "表 1.1：ICOPAY 统一 Checkout Prepare API 请求参数",
                "ตาราง 1.1: พารามิเตอร์คำขอ ICOPAY Unified Checkout Prepare API"
        ));
        putTextFields(spec, "scope", new Bundle(
                "PG 무관 통합 인라인 checkout — 운영 WEB PG(ChillPay/JPAY) 자동 분기",
                "PG-agnostic unified inline checkout — routes to operational WEB PG (ChillPay/JPAY)",
                "PG 非依存の統合インライン checkout — 運用 WEB PG（ChillPay/JPAY）へ自動分岐",
                "与 PG 无关的统一内联 checkout — 自动路由至运营 WEB PG（ChillPay/JPAY）",
                "checkout อินไลน์รวมไม่ผูก PG — แยกไป WEB PG ที่ใช้งาน (ChillPay/JPAY) อัตโนมัติ"
        ));
        spec.put("endpointMethod", "POST");
        spec.put("endpointPath", "/api/middleware/v1/merchant/checkout/prepare");
        spec.put("endpointUrl", base + "/api/middleware/v1/merchant/checkout/prepare");
        spec.put("contentType", "application/json");
        spec.put("acceptHeader", "application/json");
        spec.put("authHeader", MerchantBrokerAccessVerifier.HEADER_MERCHANT_BROKER_SECRET);
        putTextFields(spec, "authHeaderRequired", new Bundle(
                "브로커 시크릿 「강제」 시 필수. 값은 본사 키트 credentialScopes 참고.",
                "Required when broker secret enforce is on. See kit credentialScopes for the value.",
                "ブローカーシークレット「強制」時は必須。値は本社キット credentialScopes を参照。",
                "broker 密钥「强制」时必填。取值见总部套件 credentialScopes。",
                "จำเป็นเมื่อบังคับ broker secret ดูค่าใน credentialScopes ของชุด HQ"
        ));
        spec.put("documentHtmlUrl", base + "/merchant-api-samples/docs/unified-checkout-api-parameters.html");
        spec.put("documentHtmlUrlKo", base + "/merchant-api-samples/docs/unified-checkout-api-parameters.ko.html");
        spec.put("documentHtmlUrlJa", base + "/merchant-api-samples/docs/unified-checkout-api-parameters.ja.html");
        spec.put("documentHtmlUrlCh", base + "/merchant-api-samples/docs/unified-checkout-api-parameters.ch.html");
        spec.put("documentHtmlUrlTh", base + "/merchant-api-samples/docs/unified-checkout-api-parameters.th.html");
        spec.put("documentTextUrl", base + "/merchant-api-samples/docs/unified-checkout-api-parameters.txt");
        spec.put("documentTextUrlKo", base + "/merchant-api-samples/docs/unified-checkout-api-parameters.ko.txt");
        spec.put("documentTextUrlJa", base + "/merchant-api-samples/docs/unified-checkout-api-parameters.ja.txt");
        spec.put("documentTextUrlCh", base + "/merchant-api-samples/docs/unified-checkout-api-parameters.ch.txt");
        spec.put("documentTextUrlTh", base + "/merchant-api-samples/docs/unified-checkout-api-parameters.th.txt");
        spec.put("documentMarkdown", "docs/가맹점_통합Checkout_API_연동파라미터_규격.md");
        spec.put("sampleRequestJsonUrl", base + "/merchant-api-samples/json/unified-prepare-request.json");
        spec.put("sampleResponseJsonUrl", base + "/merchant-api-samples/json/unified-prepare-response.example.json");
        spec.put("compIdExample", cid);
        spec.put("httpHeaders", httpHeaders());
        spec.put("prepareBodyParameters", prepareBodyParameters(cid));
        spec.put("buyerObjectParameters", buyerObjectParameters());
        spec.put("statusQueryParameters", statusQueryParameters(cid));
        spec.put("prepareResponseFields", prepareResponseFields());
        spec.put("errorCodes", commonErrorCodes());
        spec.put("integrationModes", List.of("JSON", "PHP"));
        spec.put("notes", notes());
        return spec;
    }

    private static List<Map<String, Object>> notes() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(textMap(new Bundle(
                "가맹 서버에서 prepare 호출 — 브라우저·앱에 브로커 시크릿 노출 금지.",
                "Call prepare from the merchant server — never expose broker secret in browsers or apps.",
                "prepare は加盟店サーバーから呼び出す — ブラウザ・アプリにブローカーシークレットを露出しない。",
                "在商户服务器调用 prepare — 切勿在浏览器或应用中暴露 broker 密钥。",
                "เรียก prepare จากเซิร์ฟเวอร์ร้าน — ห้ามเปิด broker secret ในเบราว์เซอร์หรือแอป"
        )));
        rows.add(textMap(new Bundle(
                "표 1.1의 buyer 객체 하위 필드 email·phone·countryIso2 는 JPAY·ICOPAY prepare 공통 필수(M). "
                        + "JPAY 서버 직접 sale 시 payEmailAddress·payTelephone·payCountryIsoCode2 로 동일 요건.",
                "Under buyer (Table 1.2), email, phone, and countryIso2 are required (M) for JPAY and ICOPAY prepare. "
                        + "For JPAY direct sale, use payEmailAddress, payTelephone, payCountryIsoCode2 with the same rules.",
                "表 1.1 の buyer 配下の email・phone・countryIso2 は JPAY・ICOPAY prepare 共通必須(M)。"
                        + "JPAY 直接 sale では payEmailAddress・payTelephone・payCountryIsoCode2 が同要件。",
                "表 1.1 的 buyer 子字段 email、phone、countryIso2 为 JPAY·ICOPAY prepare 共同必填(M)。"
                        + "JPAY 直接 sale 对应 payEmailAddress、payTelephone、payCountryIsoCode2。",
                "ภายใต้ buyer (ตาราง 1.2) email phone countryIso2 จำเป็น(M) สำหรับ JPAY·ICOPAY prepare "
                        + "sale ตรง JPAY ใช้ payEmailAddress payTelephone payCountryIsoCode2"
        )));
        rows.add(textMap(new Bundle(
                "orderNo 는 ChillPay 운영 시 최대 20자(영숫자·하이픈·언더스코어만). JPAY 운영 시 최대 64자.",
                "orderNo: max 20 chars for ChillPay ops (alphanumeric, hyphen, underscore). Max 64 for JPAY ops.",
                "orderNo: ChillPay 運用時は最大20文字（英数字・ハイフン・アンダースコアのみ）。JPAY 運用時は最大64文字。",
                "orderNo：ChillPay 运营最长 20 字符（字母数字、连字符、下划线）。JPAY 运营最长 64 字符。",
                "orderNo: ChillPay สูงสุด 20 ตัว (ตัวอักษร ตัวเลข - _) JPAY สูงสุด 64 ตัว"
        )));
        rows.add(textMap(new Bundle(
                "결제창 언어 lang: KOR|ENG|JPN|CHN|THA. 생략 시 embed·페이지 언어 자동 감지.",
                "Checkout UI lang: KOR|ENG|JPN|CHN|THA. Omit to auto-detect from embed/page language.",
                "決済画面 lang: KOR|ENG|JPN|CHN|THA。省略時は embed・ページ言語を自動検出。",
                "支付页 lang：KOR|ENG|JPN|CHN|THA。省略则按 embed/页面语言自动检测。",
                "ภาษา lang: KOR|ENG|JPN|CHN|THA ไม่ระบุจะตรวจจาก embed/หน้าเว็บ"
        )));
        rows.add(textMap(new Bundle(
                "레거시 PG별 경로(.../chillpay/..., .../jpay/...)는 이행용. 신규는 통합 checkout 경로 권장.",
                "Legacy per-PG paths (.../chillpay/..., .../jpay/...) are for migration. Prefer unified checkout for new integrations.",
                "レガシー PG 別パスは移行用。新規は統合 checkout パスを推奨。",
                "遗留分 PG 路径用于过渡。新对接推荐统一 checkout 路径。",
                "เส้นทาง PG แยกแบบเดิมใช้ช่วงย้าย แนะนำ unified checkout สำหรับงานใหม่"
        )));
        return rows;
    }

    private static List<Map<String, Object>> httpHeaders() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(headerRow(1, "Content-Type", "M", "application/json", new Bundle(
                "POST 본문 형식", "POST body format", "POST 本文形式", "POST 正文格式", "รูปแบบ body POST"
        )));
        rows.add(headerRow(2, "Accept", "O", "application/json", new Bundle(
                "응답 JSON 권장", "JSON response recommended", "JSON 応答を推奨", "建议 JSON 响应", "แนะนำตอบ JSON"
        )));
        rows.add(headerRow(3, MerchantBrokerAccessVerifier.HEADER_MERCHANT_BROKER_SECRET, "C",
                "브로커 시크릿 문자열", new Bundle(
                        "강제(enforceYn=Y) 시 M. 미설정·비강제 시 생략 가능",
                        "Required (M) when enforceYn=Y. Optional if not enforced.",
                        "enforceYn=Y のとき M。未設定・非強制時は省略可",
                        "enforceYn=Y 时必填(M)。未强制时可省略",
                        "M เมื่อ enforceYn=Y ไม่บังคับจึงข้ามได้"
                )));
        return rows;
    }

    private static Map<String, Object> headerRow(int no, String name, String required, String valueExample, Bundle remark) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("no", no);
        m.put("name", name);
        m.put("required", required);
        m.put("valueExample", valueExample);
        putRemark(m, remark);
        return m;
    }

    private static List<Map<String, Object>> prepareBodyParameters(String compId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(param(1, "compId", "compId", "String", 64, "M",
                new Bundle("가맹점 업체코드(플랫폼 부여)", "Merchant company code assigned by ICOPAY",
                        "加盟店コード（プラットフォーム付与）", "商户代码（平台分配）", "รหัสร้าน (จากแพลตฟอร์ม)"),
                new Bundle("예: " + compId + ". merchantId(숫자) 대체 가능",
                        "e.g. " + compId + ". merchantId (numeric) alternative",
                        "例: " + compId + "。merchantId（数値）で代替可",
                        "例：" + compId + "。可用 merchantId（数字）代替",
                        "เช่น " + compId + " ใช้ merchantId แทนได้")));
        rows.add(param(2, "merchantId", "merchantId", "Number", null, "O",
                new Bundle("가맹 조직 ID(숫자). compId 와 택1", "Internal org unit id. Use compId or merchantId",
                        "加盟店組織 ID（数値）。compId とどちらか", "商户组织 ID（数字）。与 compId 二选一", "org unit id ใช้ compId หรือ merchantId"),
                new Bundle("키트 merchantOrgUnitId 와 동일할 수 있음", "May match kit merchantOrgUnitId",
                        "キット merchantOrgUnitId と同じ場合あり", "可与套件 merchantOrgUnitId 相同", "อาจตรง merchantOrgUnitId ในชุด")));
        rows.add(param(3, "orderNo", "orderNo", "String", 64, "M",
                new Bundle("가맹 주문번호·거래 참조코드", "Unique order / transaction reference",
                        "加盟店注文番号・取引参照", "商户订单号/交易参考号", "เลขอ้างอิงคำสั่งซื้อ"),
                new Bundle("ChillPay 운영: 영숫자·-·_ 만, 최대 20자. JPAY 운영: 최대 64자.",
                        "ChillPay ops: alphanumeric, -, _ only, max 20. JPAY ops: max 64.",
                        "ChillPay 運用: 英数字・-・_ のみ最大20。JPAY 運用: 最大64。",
                        "ChillPay 运营：字母数字、-、_，最长 20。JPAY 运营：最长 64。",
                        "ChillPay สูงสุด 20 ตัว JPAY สูงสุด 64")));
        rows.add(param(4, "amount", "amount", "Number", 12, "M",
                new Bundle("결제 금액(0 초과)", "Payment amount (> 0)",
                        "決済金額（0超）", "支付金额（>0）", "จำนวนเงิน (> 0)"),
                new Bundle("JPY·KRW 는 정수 권장. JSON number 또는 문자열(쉼표 제거)",
                        "Integer amounts recommended for JPY/KRW. JSON number or string without commas",
                        "JPY・KRW は整数推奨。JSON number またはカンマなし文字列",
                        "JPY/KRW 建议整数。JSON 数字或无逗号字符串",
                        "JPY/KRW แนะนำจำนวนเต็ม")));
        rows.add(param(5, "currency", "currency", "String", 3, "O",
                new Bundle("ISO 4217 통화코드", "ISO 4217 currency code",
                        "ISO 4217 通貨コード", "ISO 4217 货币代码", "รหัสสกุล ISO 4217"),
                new Bundle("예: USD, JPY, KRW, THB. 생략 시 가맹 기준통화·운영 PG 정책",
                        "e.g. USD, JPY, KRW, THB. Omit to use merchant base currency / PG policy",
                        "例: USD, JPY, KRW, THB。省略時は基準通貨・PG 方針",
                        "例：USD、JPY、KRW、THB。省略则按商户基准货币/PG 策略",
                        "เช่น USD JPY KRW THB ไม่ระบุใช้สกุลฐานร้าน")));
        rows.add(param(6, "productName", "productName", "String", 500, "O",
                new Bundle("상품명·주문 설명", "Product name or order description",
                        "商品名・注文説明", "商品名/订单说明", "ชื่อสินค้าหรือคำอธิบาย"),
                new Bundle("productName·item 중 하나 권장", "Provide productName or item",
                        "productName または item のいずれか", "建议提供 productName 或 item", "แนะนำ productName หรือ item")));
        rows.add(param(7, "item", "item", "String", 500, "O",
                new Bundle("상품명 별칭(productName 대체)", "Alias for productName",
                        "productName の別名", "productName 别名", "ชื่อทดแทน productName"),
                new Bundle("productName 이 없을 때 사용", "Use when productName is absent",
                        "productName がない場合", "无 productName 时使用", "ใช้เมื่อไม่มี productName")));
        rows.add(param(8, "lang", "lang", "String", 5, "O",
                new Bundle("결제창 UI 언어", "Checkout UI language",
                        "決済画面 UI 言語", "支付页 UI 语言", "ภาษา UI หน้าชำระ"),
                new Bundle("KOR|ENG|JPN|CHN|THA 또는 ko/en/ja/zh/th",
                        "KOR|ENG|JPN|CHN|THA or ko/en/ja/zh/th",
                        "KOR|ENG|JPN|CHN|THA または ko/en/ja/zh/th",
                        "KOR|ENG|JPN|CHN|THA 或 ko/en/ja/zh/th",
                        "KOR|ENG|JPN|CHN|THA หรือ ko/en/ja/zh/th")));
        rows.add(param(9, "buyer", "buyer", "Object", null, "M",
                new Bundle("구매자 연락처·배송 prefill", "Buyer contact & optional shipping prefill",
                        "購入者連絡先・配送 prefill", "买家联系信息与配送预填", "ข้อมูลผู้ซื้อและ prefill จัดส่ง"),
                new Bundle("표 1.2 필수 하위: email·phone·countryIso2 (JPAY·ICOPAY). buyerPrefill 동일 구조",
                        "Table 1.2 required: email, phone, countryIso2 (JPAY & ICOPAY). buyerPrefill same shape",
                        "表 1.2 必須: email・phone・countryIso2 (JPAY・ICOPAY)。buyerPrefill も同構造",
                        "表 1.2 必填子字段：email、phone、countryIso2（JPAY·ICOPAY）。buyerPrefill 结构相同",
                        "ตาราง 1.2 บังคับ: email phone countryIso2 (JPAY·ICOPAY) buyerPrefill เหมือนกัน")));
        return rows;
    }

    private static List<Map<String, Object>> buyerObjectParameters() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(param(1, "email", "buyer.email", "String", 254, "M",
                new Bundle("구매자 이메일", "Buyer email", "購入者メール", "买家邮箱", "อีเมลผู้ซื้อ"),
                new Bundle("결제·영수증·3DS 연락용. JPAY·ICOPAY 필수. sale: payEmailAddress",
                        "Payment, receipt, 3DS contact. Required for JPAY & ICOPAY. sale: payEmailAddress",
                        "決済・領収・3DS 連絡用。JPAY・ICOPAY 必須。sale: payEmailAddress",
                        "支付、收据、3DS 联系。JPAY·ICOPAY 必填。sale: payEmailAddress",
                        "ติดต่อชำระ/ใบเสร็จ/3DS จำเป็น JPAY·ICOPAY sale: payEmailAddress")));
        rows.add(param(2, "phone", "buyer.phone", "String", 32, "M",
                new Bundle("구매자 전화(로컬 번호)", "Buyer local phone",
                        "購入者電話（国内番号）", "买家电话（本地号）", "โทรศัพท์ผู้ซื้อ (เลขในประเทศ)"),
                new Bundle("국가번호 + 제거·로컬만. JPAY·ICOPAY 필수. sale: payTelephone",
                        "Strip country code +; local digits only. Required JPAY & ICOPAY. sale: payTelephone",
                        "国番号 + 除去・国内番号のみ。JPAY・ICOPAY 必須。sale: payTelephone",
                        "去掉国家码 +，仅本地号码。JPAY·ICOPAY 必填。sale: payTelephone",
                        "ตัดรหัสประเทศ + เลขในประเทศ จำเป็น JPAY·ICOPAY sale: payTelephone")));
        rows.add(param(3, "countryIso2", "buyer.countryIso2", "String", 2, "M",
                new Bundle("구매자 국가 ISO2", "Buyer country ISO 3166-1 alpha-2",
                        "購入者国 ISO2", "买家国家 ISO2", "ประเทศผู้ซื้อ ISO2"),
                new Bundle("예: KR, US, TH. 대문자 2자. JPAY·ICOPAY 필수. sale: payCountryIsoCode2",
                        "e.g. KR, US, TH. Uppercase ISO2. Required JPAY & ICOPAY. sale: payCountryIsoCode2",
                        "例: KR, US, TH。大文字2文字。JPAY・ICOPAY 必須。sale: payCountryIsoCode2",
                        "例：KR、US、TH。两位大写。JPAY·ICOPAY 必填。sale: payCountryIsoCode2",
                        "เช่น KR US TH ตัวพิมพ์ใหญ่ จำเป็น JPAY·ICOPAY sale: payCountryIsoCode2")));
        rows.add(param(4, "address", "buyer.address", "String", 200, "O",
                new Bundle("배송 주소 1행(선택 prefill)", "Shipping address line 1 (optional)",
                        "配送住所1行（任意 prefill）", "配送地址第 1 行（可选）", "ที่อยู่จัดส่งบรรทัด 1 (ไม่บังคับ)"),
                emptyRemark()));
        rows.add(param(5, "address2", "buyer.address2", "String", 200, "O",
                new Bundle("배송 주소 2행", "Shipping address line 2",
                        "配送住所2行目", "配送地址第 2 行", "ที่อยู่บรรทัด 2"),
                emptyRemark()));
        rows.add(param(6, "city", "buyer.city", "String", 100, "O",
                new Bundle("도시", "City", "市区町村", "城市", "เมือง"),
                emptyRemark()));
        rows.add(param(7, "state", "buyer.state", "String", 100, "O",
                new Bundle("주·도", "State / province", "都道府県", "省/州", "จังหวัด"),
                emptyRemark()));
        rows.add(param(8, "postcode", "buyer.postcode", "String", 20, "O",
                new Bundle("우편번호", "Postal / ZIP code", "郵便番号", "邮编", "รหัสไปรษณีย์"),
                new Bundle("zip 별칭", "zip alias", "zip 別名", "zip 别名", "ชื่อ zip")));
        rows.add(param(9, "shippingAddress", "buyer.shippingAddress", "String", 200, "O",
                new Bundle("별도 배송지 주소(선택)", "Separate shipping street (optional)",
                        "別配送先住所（任意）", "单独配送地址（可选）", "ที่อยู่จัดส่งแยก (ไม่บังคับ)"),
                new Bundle("address 와 다를 때", "When different from address",
                        "address と異なる場合", "与 address 不同时", "เมื่อต่างจาก address")));
        rows.add(param(10, "shippingPhone", "buyer.shippingPhone", "String", 32, "O",
                new Bundle("별도 배송지 전화(선택)", "Separate shipping phone (optional)",
                        "別配送先電話（任意）", "单独配送电话（可选）", "โทรจัดส่งแยก (ไม่บังคับ)"),
                emptyRemark()));
        return rows;
    }

    private static Bundle emptyRemark() {
        return new Bundle("", "", "", "", "");
    }

    private static List<Map<String, Object>> statusQueryParameters(String compId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(param(1, "compId", "compId", "String", 64, "M",
                new Bundle("가맹 업체코드", "Merchant company code",
                        "加盟店コード", "商户代码", "รหัสร้าน"),
                new Bundle("예: " + compId, "e.g. " + compId, "例: " + compId, "例：" + compId, "เช่น " + compId)));
        rows.add(param(2, "orderNo", "orderNo", "String", 64, "M",
                new Bundle("조회할 주문번호", "Order number to query",
                        "照会する注文番号", "要查询的订单号", "เลขคำสั่งซื้อที่สอบถาม"),
                new Bundle("prepare 시 전달한 orderNo", "orderNo sent in prepare",
                        "prepare で渡した orderNo", "prepare 时传入的 orderNo", "orderNo จาก prepare")));
        rows.add(param(3, "merchantId", "merchantId", "Number", null, "O",
                new Bundle("가맹 조직 ID", "Org unit id", "加盟店組織 ID", "商户组织 ID", "org unit id"),
                new Bundle("compId 대체", "Alternative to compId", "compId の代替", "替代 compId", "แทน compId")));
        return rows;
    }

    private static List<Map<String, Object>> prepareResponseFields() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(responseField(1, "sessionToken", "String", "M", new Bundle(
                "embed 스크립트 data-session-token 값", "Value for embed script data-session-token",
                "embed の data-session-token", "embed 脚本 data-session-token", "ค่า data-session-token ของ embed")));
        rows.add(responseField(2, "pgVendor", "String", "M", new Bundle(
                "운영 PG: CHILLPAY 또는 JPAY", "Operational PG: CHILLPAY or JPAY",
                "運用 PG: CHILLPAY または JPAY", "运营 PG：CHILLPAY 或 JPAY", "PG ที่ใช้: CHILLPAY หรือ JPAY")));
        rows.add(responseField(3, "operationalPgCd", "String", "M", new Bundle(
                "운영 WEB 바인딩 pg_cd", "Operational WEB binding pg_cd",
                "運用 WEB バインディング pg_cd", "运营 WEB 绑定 pg_cd", "pg_cd binding WEB")));
        rows.add(responseField(4, "embedScriptUrl", "String", "M", new Bundle(
                "/v1/embed-checkout/{compId}", "/v1/embed-checkout/{compId}",
                "/v1/embed-checkout/{compId}", "/v1/embed-checkout/{compId}", "/v1/embed-checkout/{compId}")));
        rows.add(responseField(5, "payUrl", "String", "O", new Bundle(
                "iframe 직접 src 대안 URL", "Alternative iframe src URL",
                "iframe 直接 src の代替 URL", "iframe 直接 src 备选 URL", "URL ทางเลือกสำหรับ iframe src")));
        rows.add(responseField(6, "buyerPrefill", "Object", "O", new Bundle(
                "세션에 저장된 buyer 정규화 결과", "Normalized buyer stored in session",
                "セッションに保存された buyer", "会话中规范化的 buyer", "buyer ที่ normalize ใน session")));
        rows.add(responseField(7, "expiresAt", "String", "M", new Bundle(
                "세션 만료 시각(ISO-8601)", "Session expiry (ISO-8601)",
                "セッション有効期限(ISO-8601)", "会话过期时间（ISO-8601）", "หมดอายุ session (ISO-8601)")));
        return rows;
    }

    private static List<Map<String, Object>> commonErrorCodes() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(errorCode("BUYER_REQUIRED", new Bundle(
                "buyer.email·phone·countryIso2 누락",
                "Missing buyer.email, phone, or countryIso2",
                "buyer.email・phone・countryIso2 不足",
                "缺少 buyer.email、phone 或 countryIso2",
                "ขาด buyer.email phone หรือ countryIso2")));
        rows.add(errorCode("BROKER_AUTH", new Bundle(
                "브로커 시크릿 누락·불일치(403)",
                "Broker secret missing or mismatch (403)",
                "ブローカーシークレット不足・不一致(403)",
                "broker 密钥缺失或不匹配(403)",
                "broker secret ขาดหรือไม่ตรง (403)")));
        rows.add(errorCode("INVALID_ORDER_NO", new Bundle(
                "orderNo 형식·길이 오류", "Invalid orderNo format or length",
                "orderNo 形式・長さエラー", "orderNo 格式或长度错误", "รูปแบบ/ความยาว orderNo ไม่ถูกต้อง")));
        rows.add(errorCode("INVALID_AMOUNT", new Bundle(
                "amount 누락 또는 0 이하", "amount missing or not positive",
                "amount 未指定または0以下", "amount 缺失或 ≤0", "amount ขาดหรือ ≤ 0")));
        rows.add(errorCode("NOT_FOUND", new Bundle(
                "compId·merchantId 미등록", "compId or merchantId not registered",
                "compId・merchantId 未登録", "compId/merchantId 未注册", "ไม่พบ compId/merchantId")));
        rows.add(errorCode("URL_PAYMENT_PG_MISSING", new Bundle(
                "운영 WEB PG 바인딩 없음", "No operational WEB PG binding",
                "運用 WEB PG バインディングなし", "无运营 WEB PG 绑定", "ไม่มี binding WEB PG")));
        return rows;
    }

    private static Map<String, Object> param(int no, String name, String jsonPath, String dataType,
                                             Integer maxLength, String required,
                                             Bundle description, Bundle remark) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("no", no);
        m.put("name", name);
        m.put("jsonPath", jsonPath);
        m.put("dataType", dataType);
        if (maxLength != null) {
            m.put("maxLength", maxLength);
        }
        m.put("required", required);
        putDescription(m, description);
        putRemark(m, remark);
        return m;
    }

    private static Map<String, Object> responseField(int no, String name, String dataType, String required, Bundle remark) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("no", no);
        m.put("name", name);
        m.put("dataType", dataType);
        m.put("required", required);
        putRemark(m, remark);
        return m;
    }

    private static Map<String, Object> errorCode(String code, Bundle meaning) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("errorCode", code);
        putMeaning(m, meaning);
        return m;
    }
}
