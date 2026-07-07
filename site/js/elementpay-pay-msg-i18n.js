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
    'ELEMENTPAY_PG_MISSING': {
      KOR: 'ElementPay 운영 바인딩이 없습니다.',
      ENG: 'ElementPay operational binding is missing.',
      JPN: 'ElementPay運用バインディングがありません。',
      CHN: '缺少ElementPay运营绑定。',
      THA: 'ไม่พบการผูก ElementPay สำหรับการใช้งาน'
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
