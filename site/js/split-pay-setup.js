(function () {

  'use strict';

  var LANG = 'ENG';

  var merchantMultiMax = 0;

  var checkoutCtx = {};

  var I18N = {

    KOR: { brand: 'ICOPAY', subtitle: '분할결제 신청', email: '이메일', name: '이름', totalAmount: '총금액', count: '회차 수', payTerm: '결제 기간', interval: '나누기 방식', month: '월 단위', day: 'N일 단위', multi: '멀티(고객 선택)', intervalDays: '일 간격', intervalMonths: '월 간격', preview: '일정 미리보기', create: '계약 생성', created: '계약이 생성되었습니다.', firstPay: '1회차 결제', err: '처리할 수 없습니다.', disabled: '이 가맹점은 분할결제를 사용하지 않습니다.' },

    ENG: { brand: 'ICOPAY', subtitle: 'Split payment setup', email: 'Email', name: 'Name', totalAmount: 'Total amount', count: 'Installments', payTerm: 'Pay term', interval: 'Interval', month: 'Monthly', day: 'Every N days', multi: 'Multi (customer choice)', intervalDays: 'Day interval', intervalMonths: 'Month interval', preview: 'Preview schedule', create: 'Create contract', created: 'Contract created.', firstPay: 'Pay first installment', err: 'Request failed.', disabled: 'Split pay is not enabled for this merchant.' },

    JPN: { brand: 'ICOPAY', subtitle: '分割払い申込', email: 'メール', name: '氏名', totalAmount: '総額', count: '回数', payTerm: '支払期間', interval: '分割方式', month: '月単位', day: 'N日単位', multi: 'マルチ(お客様選択)', intervalDays: '日間隔', intervalMonths: '月間隔', preview: 'スケジュール確認', create: '契約作成', created: '契約を作成しました。', firstPay: '初回支払い', err: '処理できません。', disabled: '分割払いは利用できません。' },

    CHN: { brand: 'ICOPAY', subtitle: '分期付款申请', email: '邮箱', name: '姓名', totalAmount: '总金额', count: '期数', payTerm: '付款期限', interval: '分期方式', month: '按月', day: '按N天', multi: '多选(客户自选)', intervalDays: '天数间隔', intervalMonths: '月数间隔', preview: '预览计划', create: '创建合同', created: '合同已创建。', firstPay: '支付首期', err: '无法处理。', disabled: '该商户未启用分期付款。' },

    THA: { brand: 'ICOPAY', subtitle: 'ตั้งค่าแบ่งงวด', email: 'อีเมล', name: 'ชื่อ', totalAmount: 'ยอดรวม', count: 'จำนวนงวด', payTerm: 'ระยะชำระ', interval: 'รอบ', month: 'รายเดือน', day: 'ทุก N วัน', multi: 'มัลติ(ลูกค้าเลือก)', intervalDays: 'ช่วงวัน', intervalMonths: 'ช่วงเดือน', preview: 'ดูตาราง', create: 'สร้างสัญญา', created: 'สร้างสัญญาแล้ว', firstPay: 'ชำระงวดแรก', err: 'ดำเนินการไม่ได้', disabled: 'ร้านนี้ไม่เปิดแบ่งงวด' }

  };

  var PG_SPLIT_PAY_DAY_INTERVALS = [5, 7, 10, 15, 20, 40, 50];

  function t(k) { return (I18N[LANG] && I18N[LANG][k]) || I18N.ENG[k] || k; }

  function splitPayShell() { return window.PG_URL_PAY_SHELL || null; }

  function detectBrowserSplitPayLang() {
    var nav = (navigator.language || navigator.userLanguage || 'en').toLowerCase();
    if (nav.indexOf('ko') === 0) return 'KOR';
    if (nav.indexOf('ja') === 0) return 'JPN';
    if (nav.indexOf('zh') === 0) return 'CHN';
    if (nav.indexOf('th') === 0) return 'THA';
    return 'ENG';
  }

  function applySplitPayHeader() {
    var shell = splitPayShell();
    if (!shell || !shell.applyCheckoutHeaderLogo) return;
    var opts = {
      brandBlockId: 'splitPayBrandBlock',
      imgId: 'splitPayBrandLogoImg',
      textWrapId: 'splitPayBrandTextWrap',
      t: function (k) {
        if (k === 'brandTitle') return t('brand');
        if (k === 'brandSub3ds' || k === 'brandSub') return t('subtitle');
        return t(k);
      }
    };
    shell.applyCheckoutHeaderLogo(checkoutCtx, opts);
    if (shell.applyCheckoutHeaderSubtitle) shell.applyCheckoutHeaderSubtitle(checkoutCtx, opts);
  }

  function applySplitPayLangMenu() {
    var show = String(checkoutCtx.splitPayLangMenuUseYn || 'Y').trim().toUpperCase() === 'Y';
    document.querySelectorAll('.pay-lang').forEach(function (el) {
      el.style.display = show ? '' : 'none';
    });
    if (!show) {
      var bl = detectBrowserSplitPayLang();
      if (bl && bl !== LANG) {
        LANG = bl;
        applyLang();
        if (!isMultiMode()) toggleIntervalRow();
      }
    }
  }

  function monthSuffix() {

    if (LANG === 'KOR') return '개월';

    if (LANG === 'JPN') return 'ヶ月';

    if (LANG === 'CHN') return '个月';

    if (LANG === 'THA') return ' เดือน';

    return LANG === 'ENG' ? ' mo' : ' months';

  }

  function apiBase() {

    var h = location.hostname;

    if (h.indexOf('api.') === 0) return location.protocol + '//' + h;

    return location.protocol + '//api.' + h.replace(/^www\./, '');

  }

  function qs() { return new URLSearchParams(location.search || ''); }

  function compId() { return (qs().get('m') || qs().get('compId') || '').trim(); }

  function isMultiMode() { return merchantMultiMax > 0; }

  function showMsg(text, bad) {

    var el = document.getElementById('msg');

    el.textContent = text;

    el.className = 'alert ' + (bad ? 'alert-warning' : 'alert-success');

    el.classList.remove('d-none');

  }

  function applyLang() {

    document.querySelectorAll('[data-i18n]').forEach(function (n) {

      var k = n.getAttribute('data-i18n');

      if (k) n.textContent = t(k);

    });

    document.querySelectorAll('[data-i18n-opt]').forEach(function (n) {

      var k = n.getAttribute('data-i18n-opt');

      if (k) n.textContent = t(k);

    });

    if (isMultiMode()) setupMultiPayTermControl(merchantMultiMax);

    applySplitPayHeader();

  }

  document.querySelectorAll('.pay-lang button').forEach(function (b) {

    b.addEventListener('click', function () {

      LANG = b.getAttribute('data-lang') || 'ENG';

      applyLang();

      if (!isMultiMode()) toggleIntervalRow();

    });

  });

  function bodyPayload() {

    var payload = {

      compId: compId(),

      customerEmail: document.getElementById('email').value.trim(),

      customerName: document.getElementById('customerName').value.trim(),

      totalAmount: document.getElementById('totalAmount').value,

      currencyCode: 'JPY',

      locale: LANG

    };

    if (isMultiMode()) {

      var term = parseInt(document.getElementById('installmentCount').value, 10);

      payload.installmentCount = term;

      payload.intervalType = 'MULTI';

      payload.intervalValue = 1;

    } else {

      payload.installmentCount = parseInt(document.getElementById('installmentCount').value, 10);

      payload.intervalType = document.getElementById('intervalType').value;

      payload.intervalValue = parseInt(document.getElementById('intervalValue').value, 10) || 1;

    }

    return payload;

  }

  function rebuildIntervalValueControl(isDay, value) {

    var row = document.getElementById('intervalValueRow');

    var label = row ? row.querySelector('.pay-row-label') : null;

    var valueWrap = row ? row.querySelector('.pay-row-value') : null;

    if (!valueWrap) return;

    var sel = document.createElement('select');

    sel.className = 'form-select form-select-sm';

    sel.id = 'intervalValue';

    if (isDay) {

      if (label) label.textContent = t('intervalDays');

      PG_SPLIT_PAY_DAY_INTERVALS.forEach(function (d) {

        var opt = document.createElement('option');

        opt.value = String(d);

        opt.textContent = d + (LANG === 'KOR' ? '일' : (LANG === 'JPN' ? '日' : (LANG === 'CHN' ? '天' : (LANG === 'THA' ? ' วัน' : ' days'))));

        sel.appendChild(opt);

      });

      var dv = parseInt(value, 10);

      if (PG_SPLIT_PAY_DAY_INTERVALS.indexOf(dv) < 0) dv = 10;

      sel.value = String(dv);

    } else {

      if (label) label.textContent = t('intervalMonths');

      for (var m = 1; m <= 24; m++) {

        var optM = document.createElement('option');

        optM.value = String(m);

        optM.textContent = m + monthSuffix();

        sel.appendChild(optM);

      }

      var mv = parseInt(value, 10);

      if (!mv || mv < 1 || mv > 24) mv = 1;

      sel.value = String(mv);

    }

    valueWrap.innerHTML = '';

    valueWrap.appendChild(sel);

  }

  function setupMultiPayTermControl(maxMonths) {

    var countRow = document.getElementById('installmentCountRow');

    if (!countRow) return;

    var label = countRow.querySelector('.pay-row-label');

    var valueWrap = countRow.querySelector('.pay-row-value');

    if (label) label.textContent = t('payTerm');

    if (!valueWrap) return;

    var sel = document.createElement('select');

    sel.className = 'form-select form-select-sm';

    sel.id = 'installmentCount';

    sel.required = true;

    for (var i = 1; i <= maxMonths; i++) {

      var opt = document.createElement('option');

      opt.value = String(i);

      opt.textContent = i + monthSuffix();

      sel.appendChild(opt);

    }

    sel.value = String(Math.min(3, maxMonths));

    valueWrap.innerHTML = '';

    valueWrap.appendChild(sel);

    var intervalRow = document.getElementById('intervalType');

    if (intervalRow) {

      var ir = intervalRow.closest('.pay-row');

      if (ir) ir.classList.add('d-none');

    }

    var ivRow = document.getElementById('intervalValueRow');

    if (ivRow) ivRow.classList.add('d-none');

  }

  function toggleIntervalRow() {

    if (isMultiMode()) return;

    var isDay = document.getElementById('intervalType').value === 'DAY';

    document.getElementById('intervalValueRow').classList.remove('d-none');

    var existing = document.getElementById('intervalValue');

    var prev = existing ? existing.value : '';

    rebuildIntervalValueControl(isDay, prev);

  }

  document.getElementById('intervalType').addEventListener('change', toggleIntervalRow);

  document.getElementById('previewBtn').addEventListener('click', function () {

    fetch(apiBase() + '/api/pay/split/preview', {

      method: 'POST', headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },

      body: JSON.stringify(bodyPayload())

    }).then(function (r) { return r.json(); }).then(function (j) {

      var out = document.getElementById('previewOut');

      if (!j || !j.success) { showMsg((j && j.message) || t('err'), true); return; }

      out.textContent = JSON.stringify(j.data.installments, null, 2);

      out.classList.remove('d-none');

    }).catch(function () { showMsg(t('err'), true); });

  });

  document.getElementById('setupForm').addEventListener('submit', function (e) {

    e.preventDefault();

    fetch(apiBase() + '/api/pay/split/contracts', {

      method: 'POST', headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },

      body: JSON.stringify(bodyPayload())

    }).then(function (r) { return r.json(); }).then(function (j) {

      if (!j || !j.success) { showMsg((j && j.message) || t('err'), true); return; }

      showMsg(t('created'), false);

      document.getElementById('setupForm').classList.add('d-none');

      var box = document.getElementById('resultBox');

      box.classList.remove('d-none');

      var link = document.getElementById('firstPayLink');

      link.href = j.data.firstPayUrl || ('/split-pay.html?token=' + encodeURIComponent(j.data.firstPayToken || ''));

    }).catch(function () { showMsg(t('err'), true); });

  });

  function boot() {

    var cid = compId();

    if (!cid) { showMsg(t('err'), true); return; }

    fetch(apiBase() + '/api/pay/split/merchant-config?compId=' + encodeURIComponent(cid))

      .then(function (r) { return r.json(); })

      .then(function (j) {

        if (!j || !j.success || !j.data) return;

        checkoutCtx = j.data;

        applySplitPayLangMenu();

        applySplitPayHeader();

        var enabled = String(j.data.splitPayEnabledYn || 'N').trim().toUpperCase() === 'Y';

        if (!enabled) showMsg(t('disabled'), true);

        var isMulti = String(j.data.splitPayIntervalMultiYn || 'N').trim().toUpperCase() === 'Y';

        if (isMulti) {

          merchantMultiMax = parseInt(j.data.splitPayMultiMaxMonths, 10) || 6;

          if (merchantMultiMax < 1) merchantMultiMax = 6;

          setupMultiPayTermControl(merchantMultiMax);

          return;

        }

        var isDay = String(j.data.splitPayIntervalDayYn || 'N').trim().toUpperCase() === 'Y';

        var sel = document.getElementById('intervalType');

        if (sel) {

          sel.value = isDay ? 'DAY' : 'MONTH';

          var opt = sel.querySelector('option[value="' + (isDay ? 'MONTH' : 'DAY') + '"]');

          if (opt) opt.remove();

          var multiOpt = sel.querySelector('option[value="MULTI"]');

          if (multiOpt) multiOpt.remove();

          if (sel.options.length <= 1) {

            var intervalRow = sel.closest('.pay-row');

            if (intervalRow) intervalRow.classList.add('d-none');

          }

        }

        var periodVal = isDay

          ? (j.data.splitPayDayIntervalDays != null ? j.data.splitPayDayIntervalDays : 10)

          : (j.data.splitPayMonthIntervalMonths != null ? j.data.splitPayMonthIntervalMonths : 1);

        rebuildIntervalValueControl(isDay, periodVal);

      }).catch(function () { /* ignore */ });

    if (!isMultiMode()) toggleIntervalRow();

  }

  applyLang();

  boot();

})();

