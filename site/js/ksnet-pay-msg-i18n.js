/**
 * 해외 USD 결제창 오류·결과 문구 — 결제창 언어(KOR/ENG/JPN/CHN/THA)로 표시.
 * 서버 오류코드(KSNET_*)·원문 메시지를 키로 보관하고, 미등록 문구는 원문 그대로 반환.
 * (가맹·구매자 노출 문구에는 결제대행사명을 넣지 않는다 — ICOPAY 중립)
 */
(function (global) {
  'use strict';

  var TABLE = {
    'Success': {
      KOR: '결제가 완료되었습니다.', ENG: 'Payment successful.', JPN: '決済が完了しました。',
      CHN: '支付成功。', THA: 'ชำระเงินสำเร็จ'
    },
    'Cancel': {
      KOR: '결제가 취소되었습니다.', ENG: 'Payment was cancelled.', JPN: '決済がキャンセルされました。',
      CHN: '支付已取消。', THA: 'การชำระเงินถูกยกเลิก'
    },
    'KSNET_USD_ONLY': {
      KOR: '해외 결제창은 USD 결제만 지원합니다.',
      ENG: 'The overseas payment window supports USD payments only.',
      JPN: '海外決済画面はUSD決済のみ対応しています。',
      CHN: '海外支付窗口仅支持美元(USD)支付。',
      THA: 'หน้าต่างชำระเงินต่างประเทศรองรับเฉพาะ USD'
    },
    'KSNET_URL_NOT_CONFIGURED': {
      KOR: '이 가맹점은 해외 USD 결제가 설정되어 있지 않습니다.',
      ENG: 'Overseas USD payment is not configured for this merchant.',
      JPN: 'この加盟店は海外USD決済が設定されていません。',
      CHN: '该商户未配置海外美元支付。',
      THA: 'ร้านค้านี้ยังไม่ได้ตั้งค่าการชำระเงิน USD ต่างประเทศ'
    },
    'KSNET_CREDENTIALS_MISSING': {
      KOR: '결제 설정이 완료되지 않았습니다. 관리자에게 문의하세요.',
      ENG: 'Payment settings are incomplete. Please contact the administrator.',
      JPN: '決済設定が完了していません。管理者にお問い合わせください。',
      CHN: '支付设置未完成，请联系管理员。',
      THA: 'การตั้งค่าการชำระเงินไม่สมบูรณ์ โปรดติดต่อผู้ดูแล'
    },
    'KSNET_APPROVE_FAILED': {
      KOR: '결제 승인에 실패했습니다. 다시 시도해 주세요.',
      ENG: 'Payment approval failed. Please try again.',
      JPN: '決済承認に失敗しました。もう一度お試しください。',
      CHN: '支付授权失败，请重试。',
      THA: 'การอนุมัติการชำระเงินล้มเหลว โปรดลองอีกครั้ง'
    },
    'KSNET_AMOUNT_MISMATCH': {
      KOR: '결제 금액이 일치하지 않아 승인을 취소했습니다.',
      ENG: 'The payment amount did not match, so the approval was cancelled.',
      JPN: '決済金額が一致しないため、承認を取り消しました。',
      CHN: '支付金额不一致，已取消授权。',
      THA: 'ยอดชำระไม่ตรงกัน จึงยกเลิกการอนุมัติ'
    },
    'KSNET_ERROR': {
      KOR: '결제 처리 중 오류가 발생했습니다.',
      ENG: 'An error occurred while processing the payment.',
      JPN: '決済処理中にエラーが発生しました。',
      CHN: '支付处理时发生错误。',
      THA: 'เกิดข้อผิดพลาดระหว่างประมวลผลการชำระเงิน'
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

  global.PG_KSNET_PAY_MSG = { translate: translate };
})(typeof window !== 'undefined' ? window : this);
