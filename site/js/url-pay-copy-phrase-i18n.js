/**
 * URL 결제 폼 카피·탭 제목 — 한국어 원문을 5개국어로 보강.
 * 관리자 목록 미리보기·공개 결제창 pickLangMap 폴백에서 사용.
 */
(function (g) {
  'use strict';

  var PHRASES = {
    '온더라인 간편결제 시스템': {
      EN: 'OnTheLine Easy Payment System',
      JP: 'オンザラインかんたん決済システム',
      CH: 'OnTheLine 便捷支付系统',
      TH: 'ระบบชำระเงินง่าย OnTheLine'
    },
    '결제안내': {
      EN: 'Payment information',
      JP: '決済案内',
      CH: '支付说明',
      TH: 'ข้อมูลการชำระเงิน'
    },
    '감사합니다': {
      EN: 'Thank you',
      JP: 'ありがとうございます',
      CH: '谢谢',
      TH: 'ขอบคุณ'
    },
    '주의 - 최종 결제 통화는 태국 바트입니다.': {
      EN: 'Note — the final payment currency is Thai Baht.',
      JP: '注意 — 最終決済通貨はタイバーツです。',
      CH: '注意 — 最终支付货币为泰铢。',
      TH: 'หมายเหตุ — สกุลเงินที่ชำระจริงคือบาทไทย'
    },
    '해외결제 가능 카드만 허용': {
      EN: 'Only cards eligible for overseas payment are accepted',
      JP: '海外決済可能なカードのみご利用いただけます',
      CH: '仅限可用于境外支付的卡',
      TH: 'รับเฉพาะบัตรที่ใช้ชำระต่างประเทศได้'
    },
    '(국내카드 사용불가)': {
      EN: '(Domestic cards cannot be used)',
      JP: '(国内カードはご利用いただけません)',
      CH: '(不可使用国内卡)',
      TH: '(ใช้บัตรในประเทศไม่ได้)'
    },
    '(국내카드 사용불가': {
      EN: '(Domestic cards cannot be used)',
      JP: '(国内カードはご利用いただけません)',
      CH: '(不可使用国内卡)',
      TH: '(ใช้บัตรในประเทศไม่ได้)'
    },
    '해외결제 가능 카드만 허용 (국내카드 사용불가)': {
      EN: 'Only cards eligible for overseas payment are accepted (domestic cards cannot be used)',
      JP: '海外決済可能なカードのみご利用いただけます（国内カードはご利用いただけません）',
      CH: '仅限可用于境外支付的卡（不可使用国内卡）',
      TH: 'รับเฉพาะบัตรที่ใช้ชำระต่างประเทศได้ (ใช้บัตรในประเทศไม่ได้)'
    },
    '해외결제 가능 카드만 허용 (국내카드 사용불가': {
      EN: 'Only cards eligible for overseas payment are accepted (domestic cards cannot be used)',
      JP: '海外決済可能な카드のみご利用いただけます（国内カードはご利用いただけません）',
      CH: '仅限可用于境外支付的卡（不可使用国内卡）',
      TH: 'รับเฉพาะบัตรที่ใช้ชำระต่างประเทศได้ (ใช้บัตรในประเทศไม่ได้)'
    },
    '이름 입력은 카드에 표시된 이름형식과 동일하게 입력해야 합니다.': {
      EN: 'Enter the name in the same format as printed on the card.',
      JP: 'カードに記載の氏名と同じ形式で入力してください。',
      CH: '请按卡面记载的姓名格式输入。',
      TH: 'กรอกชื่อให้ตรงกับรูปแบบที่พิมพ์บนบัตร'
    },
    '사용카드: VISA, MASTER, JCB, UNIONPAY': {
      EN: 'Cards accepted: VISA, MASTER, JCB, UNIONPAY',
      JP: 'ご利用カード: VISA, MASTER, JCB, UNIONPAY',
      CH: '可用卡: VISA, MASTER, JCB, UNIONPAY',
      TH: 'บัตรที่ใช้ได้: VISA, MASTER, JCB, UNIONPAY'
    }
  };

  function locOf(lang) {
    var L = String(lang || '').trim().toUpperCase();
    if (L === 'KO' || L === 'KOR' || L === 'KR') return 'KO';
    if (L === 'JP' || L === 'JPN' || L === 'JA') return 'JP';
    if (L === 'CH' || L === 'CHN' || L === 'ZH' || L === 'CN') return 'CH';
    if (L === 'TH' || L === 'THA') return 'TH';
    if (L === 'EN' || L === 'ENG') return 'EN';
    return L;
  }

  function normKey(s) {
    return String(s == null ? '' : s).replace(/\r\n/g, '\n').replace(/\u00a0/g, ' ').trim();
  }

  function compactKey(s) {
    return normKey(s).replace(/\s*\n\s*/g, ' ').replace(/[ \t]+/g, ' ');
  }

  function pick(row, loc) {
    if (!row) return '';
    if (loc === 'KO') return '';
    return row[loc] || row.EN || '';
  }

  function lookupExact(ko, loc) {
    var k1 = normKey(ko);
    if (!k1) return '';
    var row = PHRASES[k1] || PHRASES[compactKey(k1)];
    if (!row && k1.indexOf('\n') >= 0) {
      row = PHRASES[k1.replace(/\n/g, ' ')];
    }
    return pick(row, loc);
  }

  function lookup(ko, lang) {
    var loc = locOf(lang);
    if (loc === 'KO') return normKey(ko);
    var exact = lookupExact(ko, loc);
    if (exact) return exact;
    var raw = normKey(ko);
    if (!raw) return '';
    var lines = raw.split('\n');
    if (lines.length < 2) return '';
    var out = [];
    var any = false;
    for (var i = 0; i < lines.length; i++) {
      var line = lines[i];
      var tr = lookupExact(line, loc);
      if (tr) {
        out.push(tr);
        any = true;
      } else {
        out.push(line);
      }
    }
    return any ? out.join('\n') : '';
  }

  function fillLangMap(ko) {
    var k = normKey(ko);
    var o = {};
    if (!k) return o;
    o.KOR = k;
    var en = lookup(k, 'EN');
    var jp = lookup(k, 'JP');
    var ch = lookup(k, 'CH');
    var th = lookup(k, 'TH');
    if (en) o.ENG = en;
    if (jp) o.JPN = jp;
    if (ch) o.CHN = ch;
    if (th) o.THA = th;
    return o;
  }

  function mergeFill(existing, ko) {
    var base = existing && typeof existing === 'object' ? existing : {};
    var filled = fillLangMap(ko != null && String(ko).trim() ? ko : (base.KOR || base.KO || ''));
    var out = {};
    Object.keys(base).forEach(function (k) { out[k] = base[k]; });
    if (filled.KOR && !out.KOR) out.KOR = filled.KOR;
    function put(code, val) {
      if (!val) return;
      var cur = out[code] != null ? String(out[code]).trim() : '';
      var kor = out.KOR != null ? String(out.KOR).trim() : '';
      if (!cur || cur === kor) out[code] = val;
    }
    put('ENG', filled.ENG);
    put('JPN', filled.JPN);
    put('CHN', filled.CHN);
    put('THA', filled.THA);
    return out;
  }

  g.PG_URL_PAY_COPY_I18N = {
    lookup: lookup,
    fillLangMap: fillLangMap,
    mergeFill: mergeFill
  };
})(typeof window !== 'undefined' ? window : globalThis);
