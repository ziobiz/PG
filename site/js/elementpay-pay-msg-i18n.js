/**
 * ElementPay 결제창 오류·결과 — KOR/ENG/JPN/CHN/THA
 */
(function (global) {
  'use strict';
  var TABLE = {
    'ELEMENTPAY_THB_ONLY': {
      KOR: 'ElementPay는 THB(태국 바트)만 지원합니다.',
      ENG: 'ElementPay supports THB (Thai Baht) only.',
      JPN: 'ElementPayはTHB（タイバーツ）のみ対応です。',
      CHN: 'ElementPay仅支持泰铢(THB)。',
      THA: 'ElementPay รองรับเฉพาะ THB (บาทไทย) เท่านั้น'
    },
    'ELEMENTPAY_INIT_FAILED': {
      KOR: '결제 초기화에 실패했습니다. Merchant Key·Secret Key·서비스 alias를 확인하세요.',
      ENG: 'Payment initialization failed. Check Merchant Key, Secret Key, and service aliases.',
      JPN: '決済の初期化に失敗しました。Merchant Key・Secret Key・サービスaliasを確認してください。',
      CHN: '支付初始化失败。请检查Merchant Key、Secret Key与服务alias。',
      THA: 'เริ่มต้นการชำระเงินไม่สำเร็จ ตรวจสอบ Merchant Key, Secret Key และ service alias'
    },
    'ELEMENTPAY_METHOD_DISABLED': {
      KOR: 'ElementPay 결제수단이 맞지 않습니다. 본사 PG에서 [결제수단 조회] 후 cardServiceAlias를 맞추세요(예: kCards).',
      ENG: 'ElementPay payment method mismatch. In HQ PG settings, run method lookup and set cardServiceAlias (e.g. kCards).',
      JPN: 'ElementPay決済手段が一致しません。本社PGで［決済手段照会］後、cardServiceAliasを合わせてください（例: kCards）。',
      CHN: 'ElementPay 支付方式不匹配。请在总部 PG 中「查询支付方式」后设置 cardServiceAlias（例如 kCards）。',
      THA: 'วิธีชำระ ElementPay ไม่ตรง ตรวจที่ HQ PG แล้วตั้ง cardServiceAlias (เช่น kCards)'
    },
    'Payment method is disabled. Please contact with support': {
      KOR: 'ElementPay 결제수단이 맞지 않습니다. 본사 PG에서 [결제수단 조회] 후 cardServiceAlias를 맞추세요(예: kCards).',
      ENG: 'ElementPay payment method mismatch. In HQ PG settings, run method lookup and set cardServiceAlias (e.g. kCards).',
      JPN: 'ElementPay決済手段が一致しません。本社PGで［決済手段照会］後、cardServiceAliasを合わせてください（例: kCards）。',
      CHN: 'ElementPay 支付方式不匹配。请在总部 PG 中「查询支付方式」后设置 cardServiceAlias（例如 kCards）。',
      THA: 'วิธีชำระ ElementPay ไม่ตรง ตรวจที่ HQ PG แล้วตั้ง cardServiceAlias (เช่น kCards)'
    },
    'ELEMENTPAY_WRONG_SIGNATURE': {
      KOR: '결제 서명 검증에 실패했습니다. 본사 PG의 Secret Key·Merchant Key·Sandbox 설정을 확인하세요.',
      ENG: 'Payment signature verification failed. Check Secret Key, Merchant Key, and Sandbox settings in HQ PG config.',
      JPN: '決済署名の検証に失敗しました。本社PGのSecret Key・Merchant Key・Sandbox設定を確認してください。',
      CHN: '支付签名校验失败。请检查总部 PG 的 Secret Key、Merchant Key 与 Sandbox 设置。',
      THA: 'ตรวจสอบลายเซ็นการชำระเงินไม่ผ่าน ตรวจ Secret Key, Merchant Key และ Sandbox ในการตั้งค่า PG'
    },
    'ELEMENTPAY_CARD_ONLY': {
      KOR: '이 결제창은 신용카드만 지원합니다.',
      ENG: 'This checkout supports credit card only.',
      JPN: 'この決済画面はクレジットカードのみ対応です。',
      CHN: '此结账页仅支持信用卡。',
      THA: 'หน้าชำระนี้รองรับเฉพาะบัตรเครดิต'
    },
    'Wrong signature': {
      KOR: '결제 서명 검증에 실패했습니다. 본사 PG의 Secret Key·Merchant Key·Sandbox 설정을 확인하세요.',
      ENG: 'Payment signature verification failed. Check Secret Key, Merchant Key, and Sandbox settings in HQ PG config.',
      JPN: '決済署名の検証に失敗しました。本社PGのSecret Key・Merchant Key・Sandbox設定を確認してください。',
      CHN: '支付签名校验失败。请检查总部 PG 的 Secret Key、Merchant Key 与 Sandbox 设置。',
      THA: 'ตรวจสอบลายเซ็นการชำระเงินไม่ผ่าน ตรวจ Secret Key, Merchant Key และ Sandbox ในการตั้งค่า PG'
    },
    'ELEMENTPAY_PG_MISSING': {
      KOR: 'ElementPay 운영 바인딩이 없습니다.',
      ENG: 'ElementPay operational binding is missing.',
      JPN: 'ElementPay運用バインディングがありません。',
      CHN: '缺少ElementPay运营绑定。',
      THA: 'ไม่พบการผูก ElementPay สำหรับการใช้งาน'
    },
    'ELEMENTPAY_PAYMENT_REJECTED': {
      KOR: '결제가 거절되었습니다.',
      ENG: 'Payment was rejected.',
      JPN: '決済が拒否されました。',
      CHN: '支付已被拒绝。',
      THA: 'การชำระถูกปฏิเสธ'
    },
    'ELEMENTPAY_PAYMENT_REVERSED': {
      KOR: '승인된 결제가 은행에 의해 취소되었습니다.',
      ENG: 'The approved payment was reversed by the bank.',
      JPN: '承認済み決済が銀行により取消されました。',
      CHN: '已核准的支付被银行撤销。',
      THA: 'ธนาคารยกเลิกการชำระที่อนุมัติแล้ว'
    },
    'ELEMENTPAY_PAYMENT_REFUNDED': {
      KOR: '결제가 환불되었습니다.',
      ENG: 'The payment was refunded.',
      JPN: '決済が返金されました。',
      CHN: '该支付已退款。',
      THA: 'การชำระถูกคืนเงินแล้ว'
    },
    'ELEMENTPAY_REFUND_PAID': {
      KOR: '결제가 환불되었습니다.',
      ENG: 'The payment was refunded.',
      JPN: '決済が返金されました。',
      CHN: '该支付已退款。',
      THA: 'การชำระถูกคืนเงินแล้ว'
    },
    'ELEMENTPAY_REFUND_CREATED': {
      KOR: '환불이 접수되었습니다.',
      ENG: 'A refund request was created.',
      JPN: '返金依頼を受け付けました。',
      CHN: '已受理退款申请。',
      THA: 'รับคำขอคืนเงินแล้ว'
    },
    'ELEMENTPAY_REFUND_CANCELED': {
      KOR: '환불 요청이 취소되었습니다.',
      ENG: 'The refund request was canceled.',
      JPN: '返金依頼が取消されました。',
      CHN: '退款申请已取消。',
      THA: 'คำขอคืนเงินถูกยกเลิก'
    },
    'ELEMENTPAY_WRONG_PAYER': {
      KOR: '결제자 정보가 일치하지 않아 진행할 수 없습니다.',
      ENG: 'Payer details do not match. Payment cannot continue.',
      JPN: '支払者情報が一致しないため続行できません。',
      CHN: '付款人信息不匹配，无法继续。',
      THA: 'ข้อมูลผู้ชำระไม่ตรง ไม่สามารถดำเนินการต่อได้'
    },
    'ELEMENTPAY_DISPUTED': {
      KOR: '결제 상태가 분쟁입니다. 가맹점에 문의해 주세요.',
      ENG: 'This payment is in dispute. Please contact the merchant.',
      JPN: '決済が紛争状態です。加盟店にお問い合わせください。',
      CHN: '该支付处于争议状态。请联系商户。',
      THA: 'รายการนี้อยู่ในข้อพิพาท กรุณาติดต่อร้านค้า'
    },
    'ELEMENTPAY_REJECTED_BY_BANK': {
      KOR: '은행에서 거절됨',
      ENG: 'Rejected by bank',
      JPN: '銀行により拒否',
      CHN: '银行拒绝',
      THA: 'ธนาคารปฏิเสธ'
    },
    'ELEMENTPAY_WAITING_BANK_CONFIRM': {
      KOR: '은행 응답을 확인하는 중입니다. 잠시만 기다려 주세요…',
      ENG: 'Confirming the bank response. Please wait…',
      JPN: '銀行の応答を確認しています。しばらくお待ちください…',
      CHN: '正在确认银行响应，请稍候…',
      THA: 'กำลังยืนยันผลจากธนาคาร กรุณารอสักครู่…'
    }
  };
  function translate(msg, lang) {
    if (!msg) return '';
    var L = String(lang || 'ENG').toUpperCase();
    if (L === 'KO' || L === 'KR') L = 'KOR';
    if (L === 'EN') L = 'ENG';
    if (L === 'JA' || L === 'JP') L = 'JPN';
    if (L === 'ZH' || L === 'CN') L = 'CHN';
    if (L === 'TH') L = 'THA';
    var row = TABLE[msg] || TABLE[String(msg).trim()];
    if (row && row[L]) return row[L];
    return msg;
  }
  global.PG_ELEMENTPAY_PAY_MSG = { translate: translate, TABLE: TABLE };
})(typeof window !== 'undefined' ? window : globalThis);
