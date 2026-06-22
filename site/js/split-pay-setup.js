(function () {
  'use strict';
  var LANG = 'ENG';
  var I18N = {
    KOR: { brand: 'ICOPAY', subtitle: '분할결제 신청', email: '이메일', name: '이름', totalAmount: '총금액', count: '회차 수', interval: '나누기 방식', month: '월 단위', day: 'N일 단위', intervalDays: '일 간격', preview: '일정 미리보기', create: '계약 생성', created: '계약이 생성되었습니다.', firstPay: '1회차 결제', err: '처리할 수 없습니다.', disabled: '이 가맹점은 분할결제를 사용하지 않습니다.' },
    ENG: { brand: 'ICOPAY', subtitle: 'Split payment setup', email: 'Email', name: 'Name', totalAmount: 'Total amount', count: 'Installments', interval: 'Interval', month: 'Monthly', day: 'Every N days', intervalDays: 'Day interval', preview: 'Preview schedule', create: 'Create contract', created: 'Contract created.', firstPay: 'Pay first installment', err: 'Request failed.', disabled: 'Split pay is not enabled for this merchant.' },
    JPN: { brand: 'ICOPAY', subtitle: '分割払い申込', email: 'メール', name: '氏名', totalAmount: '総額', count: '回数', interval: '分割方式', month: '月単位', day: 'N日単位', intervalDays: '日間隔', preview: 'スケジュール確認', create: '契約作成', created: '契約を作成しました。', firstPay: '初回支払い', err: '処理できません。', disabled: '分割払いは利用できません。' },
    CHN: { brand: 'ICOPAY', subtitle: '分期付款申请', email: '邮箱', name: '姓名', totalAmount: '总金额', count: '期数', interval: '分期方式', month: '按月', day: '按N天', intervalDays: '天数间隔', preview: '预览计划', create: '创建合同', created: '合同已创建。', firstPay: '支付首期', err: '无法处理。', disabled: '该商户未启用分期付款。' },
    THA: { brand: 'ICOPAY', subtitle: 'ตั้งค่าแบ่งงวด', email: 'อีเมล', name: 'ชื่อ', totalAmount: 'ยอดรวม', count: 'จำนวนงวด', interval: 'รอบ', month: 'รายเดือน', day: 'ทุก N วัน', intervalDays: 'ช่วงวัน', preview: 'ดูตาราง', create: 'สร้างสัญญา', created: 'สร้างสัญญาแล้ว', firstPay: 'ชำระงวดแรก', err: 'ดำเนินการไม่ได้', disabled: 'ร้านนี้ไม่เปิดแบ่งงวด' }
  };
  function t(k) { return (I18N[LANG] && I18N[LANG][k]) || I18N.ENG[k] || k; }
  function apiBase() {
    var h = location.hostname;
    if (h.indexOf('api.') === 0) return location.protocol + '//' + h;
    return location.protocol + '//api.' + h.replace(/^www\./, '');
  }
  function qs() { return new URLSearchParams(location.search || ''); }
  function compId() { return (qs().get('m') || qs().get('compId') || '').trim(); }
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
  }
  document.querySelectorAll('.pay-lang button').forEach(function (b) {
    b.addEventListener('click', function () {
      LANG = b.getAttribute('data-lang') || 'ENG';
      applyLang();
    });
  });
  function bodyPayload() {
    return {
      compId: compId(),
      customerEmail: document.getElementById('email').value.trim(),
      customerName: document.getElementById('customerName').value.trim(),
      totalAmount: document.getElementById('totalAmount').value,
      installmentCount: parseInt(document.getElementById('installmentCount').value, 10),
      intervalType: document.getElementById('intervalType').value,
      intervalValue: parseInt(document.getElementById('intervalValue').value, 10) || 1,
      currencyCode: 'JPY'
    };
  }
  function toggleIntervalRow() {
    var day = document.getElementById('intervalType').value === 'DAY';
    document.getElementById('intervalValueRow').classList.toggle('d-none', !day);
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
        if (j.data.splitPayEnabledYn !== 'Y') showMsg(t('disabled'), true);
        if (j.data.splitPayIntervalDayYn !== 'Y') {
          var sel = document.getElementById('intervalType');
          var opt = sel.querySelector('option[value="DAY"]');
          if (opt) opt.remove();
        }
        if (j.data.splitPayIntervalMonthYn !== 'Y') {
          var sel2 = document.getElementById('intervalType');
          var opt2 = sel2.querySelector('option[value="MONTH"]');
          if (opt2) opt2.remove();
        }
        if (j.data.splitPayDayIntervalDays) {
          document.getElementById('intervalValue').value = j.data.splitPayDayIntervalDays;
        }
      }).catch(function () { /* ignore */ });
    toggleIntervalRow();
  }
  applyLang();
  boot();
})();
