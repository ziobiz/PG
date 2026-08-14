/**
 * 결제 결과(resmsg)·오류 문구 — 결제창 언어(KOR/ENG/JPN/CHN/THA)로 표시.
 * 원문(영문)은 키로 보관하고, 미등록 문구는 원문 그대로 반환.
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
      KOR: '이 가맹점은 결제가 설정되어 있지 않습니다.',
      ENG: 'Payment is not configured for this merchant.',
      JPN: 'この加盟店は決済が設定されていません。',
      CHN: '该商户未配置支付。',
      THA: 'ร้านค้านี้ยังไม่ได้ตั้งค่าการชำระเงิน'
    },
    'EXIMBAY_AGENCY_MISSING': {
      KOR: '결제대행사 설정을 찾을 수 없습니다.',
      ENG: 'Payment provider settings were not found.',
      JPN: '決済代行の設定が見つかりません。',
      CHN: '未找到支付机构设置。',
      THA: 'ไม่พบการตั้งค่าผู้ให้บริการชำระเงิน'
    },
    'EXIMBAY_CREDENTIALS_MISSING': {
      KOR: '결제 MID/Secret Key 가 설정되지 않았습니다.',
      ENG: 'Payment MID/Secret Key is not configured.',
      JPN: '決済 MID/Secret Key が設定されていません。',
      CHN: '未配置支付 MID/Secret Key。',
      THA: 'ยังไม่ได้ตั้งค่า MID/Secret Key'
    },
    'EXIMBAY_PAYMENTS_FAILED': {
      KOR: '결제창을 열지 못했습니다. 잠시 후 다시 시도하세요.',
      ENG: 'Could not open the payment window. Please try again.',
      JPN: '決済画面を開けませんでした。しばらくして再度お試しください。',
      CHN: '无法打开支付窗口，请稍后重试。',
      THA: 'เปิดหน้าต่างชำระเงินไม่สำเร็จ โปรดลองอีกครั้ง'
    },
    'EXIMBAY_HOSTED_URL_INVALID': {
      KOR: '결제창 주소를 받지 못했습니다. 잠시 후 다시 시도하세요.',
      ENG: 'Could not open the payment window. Please try again.',
      JPN: '決済画面のURLを取得できませんでした。しばらくして再度お試しください。',
      CHN: '未能取得支付窗口地址，请稍后重试。',
      THA: 'ไม่ได้รับที่อยู่หน้าต่างชำระเงิน โปรดลองอีกครั้ง'
    },
      KOR: '결제준비 호출에 실패했습니다. MID·Secret Key·연동 설정을 확인하세요.',
      ENG: 'Failed to prepare the payment. Check MID, Secret Key, and integration settings.',
      JPN: '決済準備の呼び出しに失敗しました。MID・Secret Key・連携設定を確認してください。',
      CHN: '支付准备调用失败。请检查MID、Secret Key与对接设置。',
      THA: 'เตรียมการชำระเงินไม่สำเร็จ ตรวจสอบ MID, Secret Key และการตั้งค่า'
    },
    'EXIMBAY_EMAIL_REQUIRED': {
      KOR: '이메일은 필수입니다.',
      ENG: 'Email is required.',
      JPN: 'メールアドレスは必須です。',
      CHN: '邮箱为必填。',
      THA: 'ต้องระบุอีเมล'
    },
    'EXIMBAY_PAYPAY_JPY_REQUIRED': {
      KOR: 'PayPay·일본 편의점·은행 결제는 JPY 금액으로만 진행됩니다.',
      ENG: 'PayPay and Japan convenience-store/bank payments require JPY.',
      JPN: 'PayPay・コンビニ・銀行決済はJPYのみです。',
      CHN: 'PayPay/日本便利店/银行支付仅支持日元(JPY)。',
      THA: 'PayPay และการชำระร้านสะดวกซื้อ/ธนาคารญี่ปุ่นใช้ได้เฉพาะ JPY'
    },
    'EXIMBAY_JPY_WHOLE_YEN': {
      KOR: 'JPY 결제는 소수점 없이 엔 단위로 입력하세요.',
      ENG: 'JPY amounts must be whole yen (no decimals).',
      JPN: 'JPYは小数なしの円単位で入力してください。',
      CHN: '日元金额须为整数。',
      THA: 'ยอด JPY ต้องเป็นจำนวนเต็มเยน'
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
