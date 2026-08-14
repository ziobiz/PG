/**
 * ICOPAY 결제창 결제수단 로고(인라인 SVG) — 신용카드 브랜드·PayPay·편의점/Pay-easy·UnionPay.
 * 구매자 UI에 PG사명은 넣지 않는다.
 */
(function (global) {
  'use strict';

  function svg(inner, vb, cls) {
    return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="' + vb + '" class="' + (cls || 'pm-logo') + '" aria-hidden="true">' + inner + '</svg>';
  }

  var MARKS = {
    visa: svg(
      '<text x="2" y="13" font-family="Arial,Helvetica,sans-serif" font-size="12" font-weight="800" font-style="italic" fill="#1A1F71">VISA</text>',
      '0 0 42 16', 'pm-logo pm-logo-visa'),
    mastercard: svg(
      '<circle cx="12" cy="10" r="8" fill="#EB001B"/><circle cx="20" cy="10" r="8" fill="#F79E1B"/><path d="M16 4.2a8 8 0 000 11.6 8 8 0 000-11.6z" fill="#FF5F00"/>',
      '0 0 32 20', 'pm-logo pm-logo-mc'),
    amex: svg(
      '<rect width="44" height="16" rx="2" fill="#2E77BB"/><text x="22" y="12" text-anchor="middle" font-family="Arial,Helvetica,sans-serif" font-size="7.5" font-weight="700" fill="#fff">AMERICAN EXPRESS</text>',
      '0 0 44 16', 'pm-logo pm-logo-amex'),
    jcb: svg(
      '<rect x="0" y="0" width="12" height="16" rx="1" fill="#0E4C96"/><text x="6" y="11" text-anchor="middle" font-size="8" font-weight="700" fill="#fff" font-family="Arial,Helvetica,sans-serif">J</text>' +
      '<rect x="13" y="0" width="12" height="16" rx="1" fill="#E00000"/><text x="19" y="11" text-anchor="middle" font-size="8" font-weight="700" fill="#fff" font-family="Arial,Helvetica,sans-serif">C</text>' +
      '<rect x="26" y="0" width="12" height="16" rx="1" fill="#00A650"/><text x="32" y="11" text-anchor="middle" font-size="8" font-weight="700" fill="#fff" font-family="Arial,Helvetica,sans-serif">B</text>',
      '0 0 38 16', 'pm-logo pm-logo-jcb'),
    diners: svg(
      '<circle cx="10" cy="10" r="9" fill="#0079BE"/><circle cx="10" cy="10" r="6.5" fill="#fff"/><rect x="6.2" y="3.8" width="7.6" height="12.4" fill="#0079BE"/>',
      '0 0 20 20', 'pm-logo pm-logo-diners'),
    discover: svg(
      '<text x="0" y="13" font-family="Arial,Helvetica,sans-serif" font-size="11" font-weight="700" fill="#F76F00">DISCOVER</text>',
      '0 0 72 16', 'pm-logo pm-logo-discover'),
    paypay: svg(
      '<rect width="56" height="22" rx="4" fill="#FF0033"/><text x="28" y="15.5" text-anchor="middle" font-family="Arial,Helvetica,sans-serif" font-size="11" font-weight="800" fill="#fff">PayPay</text>',
      '0 0 56 22', 'pm-logo pm-logo-paypay'),
    konbini: svg(
      '<rect width="72" height="22" rx="3" fill="#1B6BCC"/><text x="36" y="15.5" text-anchor="middle" font-family="Arial,Helvetica,sans-serif" font-size="9" font-weight="700" fill="#fff">コンビニ決済</text>',
      '0 0 72 22', 'pm-logo pm-logo-konbini'),
    payeasy: svg(
      '<rect width="70" height="22" rx="3" fill="#00A0A0"/><text x="35" y="15.5" text-anchor="middle" font-family="Arial,Helvetica,sans-serif" font-size="10" font-weight="800" fill="#fff">Pay-easy</text>',
      '0 0 70 22', 'pm-logo pm-logo-payeasy'),
    unionpay: svg(
      '<rect x="0" y="1" width="18" height="18" rx="2" fill="#E21836"/>' +
      '<rect x="14" y="1" width="18" height="18" rx="2" fill="#00447C"/>' +
      '<rect x="28" y="1" width="18" height="18" rx="2" fill="#00A651"/>' +
      '<text x="23" y="14" text-anchor="middle" font-size="6.5" font-weight="700" fill="#fff" font-family="Arial,Helvetica,sans-serif">UnionPay</text>',
      '0 0 46 20', 'pm-logo pm-logo-unionpay')
  };

  var GROUPS = {
    CARD: ['visa', 'mastercard', 'amex', 'jcb', 'diners', 'discover'],
    PAYPAY: ['paypay'],
    JPCONVBANK: ['konbini', 'payeasy'],
    UNIONPAY: ['unionpay']
  };

  function logosFor(key) {
    var k = String(key || '').toUpperCase();
    var ids = GROUPS[k];
    if (!ids) return '';
    return ids.map(function (id) { return MARKS[id] || ''; }).join('');
  }

  function fillButton(btn, key, label) {
    if (!btn) return;
    var k = String(key || '').toUpperCase();
    var cap = String(label || k);
    btn.setAttribute('data-method', k);
    btn.innerHTML =
      '<span class="eximbay-method-logos">' + logosFor(k) + '</span>' +
      '<span class="eximbay-method-caption"></span>';
    var capEl = btn.querySelector('.eximbay-method-caption');
    if (capEl) capEl.textContent = cap;
  }

  global.ICOPAY_PAY_METHOD_UI = {
    logosFor: logosFor,
    fillButton: fillButton
  };
})(typeof window !== 'undefined' ? window : this);
