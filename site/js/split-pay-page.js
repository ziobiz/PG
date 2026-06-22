(function () {
  'use strict';
  var LANG = 'ENG';
  var I18N = {
    KOR: { brand: 'ICOPAY', subtitle: '분할결제', contractNo: '계약번호', orderNo: '주문번호', totalAmount: '총금액', installments: '총 회차', current: '이번 회차·금액', remaining: '남은 회차', next: '다음 금액·일자', goPay: '결제 진행', loadErr: '결제 정보를 불러올 수 없습니다.', paid: '이미 결제된 회차입니다.' },
    ENG: { brand: 'ICOPAY', subtitle: 'Split payment', contractNo: 'Contract', orderNo: 'Order No', totalAmount: 'Total', installments: 'Installments', current: 'This installment', remaining: 'Remaining', next: 'Next amount · date', goPay: 'Proceed to payment', loadErr: 'Cannot load payment info.', paid: 'Already paid.' },
    JPN: { brand: 'ICOPAY', subtitle: '分割払い', contractNo: '契約番号', orderNo: '注文番号', totalAmount: '総額', installments: '総回数', current: '今回の支払い', remaining: '残り回数', next: '次回金額・日付', goPay: '支払いへ', loadErr: '情報を読み込めません。', paid: '支払い済みです。' },
    CHN: { brand: 'ICOPAY', subtitle: '分期付款', contractNo: '合同号', orderNo: '订单号', totalAmount: '总金额', installments: '总期数', current: '本期付款', remaining: '剩余期数', next: '下期金额·日期', goPay: '前往付款', loadErr: '无法加载付款信息。', paid: '已付款。' },
    THA: { brand: 'ICOPAY', subtitle: 'ชำระแบบแบ่งงวด', contractNo: 'เลขสัญญา', orderNo: 'เลขคำสั่ง', totalAmount: 'ยอดรวม', installments: 'จำนวนงวด', current: 'งวดนี้', remaining: 'งวดคงเหลือ', next: 'งวดถัดไป', goPay: 'ดำเนินการชำระ', loadErr: 'โหลดข้อมูลไม่ได้', paid: 'ชำระแล้ว' }
  };
  function t(k) { return (I18N[LANG] && I18N[LANG][k]) || I18N.ENG[k] || k; }
  function apiBase() {
    var h = location.hostname;
    if (h.indexOf('api.') === 0) return location.protocol + '//' + h;
    return location.protocol + '//api.' + h.replace(/^www\./, '');
  }
  function qs() { return new URLSearchParams(location.search || ''); }
  function isJpayPg(pgCd) {
    if (!pgCd) return false;
    var u = String(pgCd).trim().toUpperCase();
    return u === 'JPAY' || u.indexOf('JPAY') === 0 || u.indexOf('JPAY_') === 0;
  }
  function resolvePayPage(d) {
    if (d.checkoutPage) return String(d.checkoutPage);
    if (isJpayPg(d.operationalPgCd)) return 'jpay-pay.html';
    if (d.operationalPgCd) return 'pay.html';
    return 'pay.html';
  }
  function showMsg(text, bad) {
    var el = document.getElementById('msg');
    el.textContent = text;
    el.className = 'alert ' + (bad ? 'alert-warning' : 'alert-info');
    el.classList.remove('d-none');
  }
  function applyLang() {
    document.querySelectorAll('[data-i18n]').forEach(function (n) {
      var k = n.getAttribute('data-i18n');
      if (k) n.textContent = t(k);
    });
  }
  document.querySelectorAll('.pay-lang button').forEach(function (b) {
    b.addEventListener('click', function () {
      LANG = b.getAttribute('data-lang') || 'ENG';
      applyLang();
    });
  });
  function fmtAmt(v, cur) { return (v != null ? v : '') + ' ' + (cur || ''); }
  function boot() {
    var token = (qs().get('token') || '').trim();
    if (!token) { showMsg(t('loadErr'), true); return; }
    fetch(apiBase() + '/api/pay/split/installment?token=' + encodeURIComponent(token), { credentials: 'omit' })
      .then(function (r) { return r.json(); })
      .then(function (j) {
        if (!j || !j.success || !j.data) { showMsg((j && j.message) || t('loadErr'), true); return; }
        var d = j.data;
        document.getElementById('summary').classList.remove('d-none');
        document.getElementById('vContractNo').textContent = d.contractNo || '';
        document.getElementById('vOrderNo').textContent = d.orderNo || '';
        document.getElementById('vTotal').textContent = fmtAmt(d.totalAmount, d.currencyCode);
        document.getElementById('vInstallments').textContent = (d.currentInstallmentNo || '') + ' / ' + (d.installmentCount || '');
        document.getElementById('vCurrent').textContent = fmtAmt(d.currentAmount, d.currencyCode);
        document.getElementById('vRemaining').textContent = String(d.remainingInstallments != null ? d.remainingInstallments : '');
        var next = d.nextAmount ? fmtAmt(d.nextAmount, d.currencyCode) : '—';
        if (d.nextDueDate) next += ' · ' + d.nextDueDate;
        document.getElementById('vNext').textContent = next;
        var btn = document.getElementById('goPayBtn');
        var payPage = resolvePayPage(d);
        var payUrl = '/' + payPage + '?m=' + encodeURIComponent(d.compId || '') +
          '&orderNo=' + encodeURIComponent(d.orderNo || '') +
          '&amount=' + encodeURIComponent(d.amount || d.currentAmount || '') +
          '&item=' + encodeURIComponent('SplitPay ' + (d.contractNo || '')) +
          '&splitPay=1';
        if (d.currencyCode) payUrl += '&currency=' + encodeURIComponent(d.currencyCode);
        if (d.customerEmail) payUrl += '&email=' + encodeURIComponent(d.customerEmail);
        if (d.customerName) payUrl += '&buyerName=' + encodeURIComponent(d.customerName);
        btn.href = payUrl;
        btn.classList.remove('d-none');
      })
      .catch(function () { showMsg(t('loadErr'), true); });
  }
  applyLang();
  boot();
})();
