/**
 * JPAY pay_index 동기 응답 msg — 결제창 언어(KOR/ENG/JPN/CHN/THA)로 표시.
 * J-Pay 원문(중국어·영어)은 키로 보관하고, 미등록 문구는 원문 그대로 반환.
 */
(function (global) {
  'use strict';

  var TABLE = {
    '用户未分配通道,暂时无法发起支付请求!': {
      KOR: '결제 채널이 할당되지 않아 결제를 진행할 수 없습니다. J-Pay 가맹 포털에서 채널·웹사이트 승인 상태를 확인하세요.',
      ENG: 'No payment channel is assigned to this merchant. Check channel and website approval in the J-Pay merchant portal.',
      JPN: '決済チャネルが割り当てられていないため、決済を開始できません。J-Pay加盟店ポータルでチャネル・サイト承認を確認してください。',
      CHN: '用户未分配通道,暂时无法发起支付请求!',
      THA: 'ร้านค้ายังไม่ได้รับช่องทางชำระเงิน ตรวจสอบการอนุมัติช่องทางและเว็บไซต์ใน J-Pay'
    },
    '不存在的商户编号': {
      KOR: '등록되지 않은 가맹점 번호(MID)입니다. API연동설정의 MID·ApiKey를 확인하세요.',
      ENG: 'Merchant ID does not exist. Verify MID and ApiKey in API integration settings.',
      JPN: '存在しない加盟店番号(MID)です。API連携設定のMID・ApiKeyを確認してください。',
      CHN: '不存在的商户编号',
      THA: 'ไม่มีหมายเลขร้านค้า (MID) นี้ ตรวจสอบ MID และ ApiKey'
    },
    'transaction success': {
      KOR: '결제가 완료되었습니다.',
      ENG: 'Transaction successful.',
      JPN: '決済が完了しました。',
      CHN: '交易成功',
      THA: 'ทำรายการสำเร็จ'
    },
    'transaction failed': {
      KOR: '결제에 실패했습니다.',
      ENG: 'Transaction failed.',
      JPN: '決済に失敗しました。',
      CHN: '交易失败',
      THA: 'ทำรายการล้มเหลว'
    },
    '3DS redirect URL missing': {
      KOR: '3DS 인증 URL을 받지 못했습니다. J-Pay 가맹 포털·채널 승인 상태를 확인하세요.',
      ENG: '3DS redirect URL was not returned. Check J-Pay merchant portal and channel approval.',
      JPN: '3DS認証URLを受信できませんでした。J-Pay加盟店ポータルとチャネル承認を確認してください。',
      CHN: '未收到3DS认证跳转地址。请检查J-Pay商户后台与通道审批状态。',
      THA: 'ไม่ได้รับ URL ยืนยัน 3DS ตรวจสอบพอร์ทัลร้านค้า J-Pay และการอนุมัติช่องทาง'
    },
    'JPAY response status unknown': {
      KOR: 'J-Pay 응답을 해석하지 못했습니다. MID·ApiKey·pay_index URL을 확인하세요.',
      ENG: 'Could not interpret J-Pay response. Verify MID, ApiKey, and pay_index URL.',
      JPN: 'J-Pay応答を解釈できませんでした。MID・ApiKey・pay_index URLを確認してください。',
      CHN: '无法解析J-Pay响应。请核对MID、ApiKey与pay_index地址。',
      THA: 'ไม่สามารถอ่านการตอบกลับจาก J-Pay ได้ ตรวจสอบ MID ApiKey และ pay_index URL'
    },
    'Signature verification failed': {
      KOR: 'J-Pay 서명 검증에 실패했습니다. MID·ApiKey·pay_md5sign 규칙을 확인하세요.',
      ENG: 'J-Pay signature verification failed. Check MID, ApiKey, and MD5 sign fields.',
      JPN: 'J-Pay署名検証に失敗しました。MID・ApiKey・署名ルールを確認してください。',
      CHN: 'J-Pay签名校验失败。请核对MID、ApiKey与签名规则。',
      THA: 'การตรวจสอบลายเซ็น J-Pay ล้มเหลว ตรวจสอบ MID ApiKey และกฎ MD5'
    },
    'JPAY_AMOUNT_RANGE': {
      KOR: 'J-Pay에 전달한 결제 금액이 허용 범위를 벗어났습니다. 금액·통화·결제통화 스케일(×100 등) 설정을 확인하세요.',
      ENG: 'Payment amount sent to J-Pay is out of allowed range. Check amount, currency, and pay currency scale (×100 etc.).',
      JPN: 'J-Payへ送信した決済金額が許容範囲外です。金額・通貨・スケール設定を確認してください。',
      CHN: '发送给J-Pay的支付金额超出允许范围。请检查金额、币种及货币换算规则。',
      THA: 'จำนวนเงินที่ส่งไป J-Pay เกินช่วงที่อนุญาต ตรวจสอบจำนวน สกุลเงิน และกฎสเกล'
    },
    'JPAY pay_index returned empty response (verify pay_index URL)': {
      KOR: 'J-Pay pay_index 가 빈 응답을 반환했습니다. API연동설정의 pay_index URL(운영: https://api.j-pay.net/pay_index)을 확인하세요.',
      ENG: 'J-Pay pay_index returned an empty response. Verify pay_index URL (live: https://api.j-pay.net/pay_index).',
      JPN: 'J-Pay pay_index が空の応答を返しました。pay_index URL（本番: https://api.j-pay.net/pay_index）を確認してください。',
      CHN: 'J-Pay pay_index 返回空响应。请核对 pay_index URL（生产: https://api.j-pay.net/pay_index）。',
      THA: 'J-Pay pay_index ตอบกลับว่าง ตรวจสอบ pay_index URL (live: https://api.j-pay.net/pay_index)'
    },
    'ICOPAY_ORDER_DUP': {
      KOR: '이 주문번호는 이미 결제가 시도되었습니다. 쇼핑몰에서 새 orderNo로 prepare를 다시 호출해 주세요.',
      ENG: 'This order number was already used for a payment attempt. Call prepare again with a new orderNo.',
      JPN: 'この注文番号はすでに決済が試行されています。新しい orderNo で prepare を再度呼び出してください。',
      CHN: '该订单号已用于支付尝试。请使用新的 orderNo 重新调用 prepare。',
      THA: 'หมายเลขคำสั่งซื้อนี้ถูกใช้แล้ว โปรดเรียก prepare ใหม่ด้วย orderNo ใหม่'
    },
    'ICOPAY_ORDER_PENDING': {
      KOR: '이 주문번호로 결제가 진행 중입니다. 3DS 인증을 완료하거나 status API로 결과를 확인해 주세요.',
      ENG: 'A payment is already in progress for this orderNo. Complete 3DS or check the status API.',
      JPN: 'この orderNo で決済が進行中です。3DSを完了するか status API で結果を確認してください。',
      CHN: '该 orderNo 支付进行中。请完成 3DS 或通过 status API 查询。',
      THA: 'คำสั่งซื้อนี้กำลังชำระอยู่ ให้ทำ 3DS ให้เสร็จหรือตรวจ status API'
    },
    '중복 주문! 주문을 다시 제출해 주세요.': {
      KOR: '중복 주문입니다. 쇼핑몰에서 새 orderNo로 prepare를 다시 호출해 주세요.',
      ENG: 'Duplicate order. Call prepare again from your store with a new orderNo.',
      JPN: '重複注文です。ストアで新しい orderNo で prepare を再度呼び出してください。',
      CHN: '重复订单。请从商城使用新的 orderNo 重新调用 prepare。',
      THA: 'คำสั่งซื้อซ้ำ โปรดเรียก prepare ใหม่ด้วย orderNo ใหม่จากร้านค้า'
    }
  };

  var PARTIAL = [
    { re: /未分配通道/, key: '用户未分配通道,暂时无法发起支付请求!' },
    { re: /不存在的商户编号/, key: '不存在的商户编号' },
    { re: /Signature verification failed/i, key: 'Signature verification failed' },
    { re: /pay_actualamount|Out of range value/i, key: 'JPAY_AMOUNT_RANGE' },
    { re: /empty response/i, key: 'JPAY pay_index returned empty response (verify pay_index URL)' },
    { re: /duplicate.*order|order.*duplicate/i, key: 'ICOPAY_ORDER_DUP' },
    { re: /重复.*订单|订单.*重复/, key: 'ICOPAY_ORDER_DUP' },
    { re: /중복.*주문|주문.*중복/, key: '중복 주문! 주문을 다시 제출해 주세요.' }
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

  function isDuplicateOrder(msg) {
    var raw = String(msg == null ? '' : msg).trim();
    if (!raw) return false;
    var lower = raw.toLowerCase();
    if (lower.indexOf('duplicate') >= 0 && lower.indexOf('order') >= 0) return true;
    if (raw.indexOf('重复') >= 0 && raw.indexOf('订单') >= 0) return true;
    if (raw.indexOf('중복') >= 0 && raw.indexOf('주문') >= 0) return true;
    if (raw.indexOf('重複') >= 0 && (raw.indexOf('注文') >= 0 || raw.indexOf('オーダー') >= 0)) return true;
    return false;
  }

  global.PG_JPAY_PAY_MSG = { translate: translate, isDuplicateOrder: isDuplicateOrder };
})(typeof window !== 'undefined' ? window : this);
