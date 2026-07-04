/**
 * Eximbay 결과(resmsg)·오류 문구 — 결제창 언어(KOR/ENG/JPN/CHN/THA)로 표시.
 * Eximbay 원문(영문)은 키로 보관하고, 미등록 문구는 원문 그대로 반환.
 */
(function (global) {
  'use strict';

  var TABLE = {
    'Success': {
      KOR: '결제가 완료되었습니다.', ENG: 'Payment successful.', JPN: '決済が完了しました。',
      CHN: '支付成功。', THA: 'ชำระเงินสำเร็จ'
    },
    'Success.': {
      KOR: '결제가 완료되었습니다.', ENG: 'Payment successful.', JPN: '決済が完了しました。',
      CHN: '支付成功。', THA: 'ชำระเงินสำเร็จ'
    },
    'User cancelled': {
      KOR: '결제가 취소되었습니다.', ENG: 'Payment was cancelled.', JPN: '決済がキャンセルされました。',
      CHN: '支付已取消。', THA: 'การชำระเงินถูกยกเลิก'
    },
    'Cancel': {
      KOR: '결제가 취소되었습니다.', ENG: 'Payment was cancelled.', JPN: '決済がキャンセルされました。',
      CHN: '支付已取消。', THA: 'การชำระเงินถูกยกเลิก'
    },
    'EXIMBAY_PG_MISSING': {
      KOR: '이 가맹점은 Eximbay 결제가 설정되어 있지 않습니다.',
      ENG: 'Eximbay payment is not configured for this merchant.',
      JPN: 'この加盟店はEximbay決済が設定されていません。',
      CHN: '该商户未配置Eximbay支付。',
      THA: 'ร้านค้านี้ยังไม่ได้ตั้งค่าการชำระเงิน Eximbay'
    },
    'EXIMBAY_READY_FAILED': {
      KOR: 'Eximbay 결제준비 호출에 실패했습니다. MID·Secret Key·연동 설정을 확인하세요.',
      ENG: 'Failed to prepare the Eximbay payment. Check MID, Secret Key, and integration settings.',
      JPN: 'Eximbay決済準備の呼び出しに失敗しました。MID・Secret Key・連携設定を確認してください。',
      CHN: 'Eximbay支付准备调用失败。请检查MID、Secret Key与对接设置。',
      THA: 'เตรียมการชำระเงิน Eximbay ไม่สำเร็จ ตรวจสอบ MID, Secret Key และการตั้งค่า'
    },
    'EXIMBAY_CREDENTIALS_MISSING': {
      KOR: 'Eximbay MID/Secret Key 가 설정되지 않았습니다.',
      ENG: 'Eximbay MID/Secret Key is not configured.',
      JPN: 'Eximbay MID/Secret Key が設定されていません。',
      CHN: '未配置Eximbay MID/Secret Key。',
      THA: 'ยังไม่ได้ตั้งค่า Eximbay MID/Secret Key'
    }
  };

  var PARTIAL = [
    { re: /cancel/i, key: 'Cancel' },
    { re: /success/i, key: 'Success' }
  ];

  function normalizeLang(lang) {
    var c = String(lang || 'ENG').toUpperCase();
    if (c === 'KO' || c === 'KR') return 'KOR';
    if (c === 'JA' || c === 'JP') return 'JPN';
    if (c === 'ZH' || c === 'CN') return 'CHN';
    if (c === 'TH') return 'THA';
    if (c === 'EN') return 'ENG';
    if (c === 'KOR' || c === 'ENG' || c === 'JPN' || c === 'CHN' || c === 'THA') return c;
    return 'ENG';
  }

  function lookup(key, lang) {
    var pack = TABLE[key];
    if (!pack) return null;
    return pack[lang] || pack.ENG || key;
  }

  function translate(msg, lang) {
    var raw = String(msg == null ? '' : msg).trim();
    if (!raw) return raw;
    var L = normalizeLang(lang);
    var hit = lookup(raw, L);
    if (hit) return hit;
    for (var i = 0; i < PARTIAL.length; i++) {
      if (PARTIAL[i].re.test(raw)) {
        var p = lookup(PARTIAL[i].key, L);
        if (p) return p;
      }
    }
    return raw;
  }

  global.PG_EXIMBAY_PAY_MSG = { translate: translate };
})(typeof window !== 'undefined' ? window : this);
