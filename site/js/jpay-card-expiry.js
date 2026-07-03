/**
 * JPAY URL 결제창 — 카드 유효기간(MM/YY) 입력 UI (Java UrlPayCardExpiryModeUtil 과 동일).
 */
(function (g) {
  'use strict';

  var DROPDOWN = 'DROPDOWN';
  var TEXT = 'TEXT';
  var HYBRID = 'HYBRID';
  var AI_B = 'AI_B';
  var AI_A = 'AI_A';

  function isMobileOrTablet() {
    try {
      if (g.matchMedia && g.matchMedia('(max-width: 991.98px)').matches) return true;
    } catch (e0) { /* ignore */ }
    var ua = '';
    try { ua = String(g.navigator.userAgent || '').toLowerCase(); } catch (e1) { /* ignore */ }
    return /android|iphone|ipad|ipod|mobile|tablet/.test(ua);
  }

  function normalizeMode(raw) {
    var u = String(raw || DROPDOWN).trim().toUpperCase();
    if (u === 'DROP_DOWN' || u === 'SELECT') return DROPDOWN;
    if (u === 'INPUT' || u === 'MANUAL') return TEXT;
    if (u === 'HYBRID_YY') return HYBRID;
    if (u === 'AIB' || u === 'AI-B') return AI_B;
    if (u === 'AIA' || u === 'AI-A') return AI_A;
    if (u === TEXT || u === HYBRID || u === AI_B || u === AI_A) return u;
    return DROPDOWN;
  }

  function resolveUiMode(ctx) {
    var stored = ctx && (ctx.urlPayCardExpiryModeEffective || ctx.urlPayCardExpiryMode);
    var m = normalizeMode(stored);
    if (m === AI_B) return DROPDOWN;
    if (m === AI_A) return isMobileOrTablet() ? DROPDOWN : HYBRID;
    if (m === TEXT || m === HYBRID) return m;
    return DROPDOWN;
  }

  function pad2(n) {
    n = parseInt(n, 10);
    if (isNaN(n) || n < 1) return '';
    if (n > 12) return '12';
    return n < 10 ? '0' + n : String(n);
  }

  function expandYear(y) {
    y = String(y || '').replace(/\D/g, '');
    if (!y) return '';
    if (y.length === 2) {
      var n = parseInt(y, 10);
      if (isNaN(n)) return '';
      return String(2000 + n);
    }
    if (y.length === 4) return y;
    return y.length > 4 ? y.substring(0, 4) : y;
  }

  function inputCls() {
    return 'form-control form-control-sm pay-input-round jpay-field-input pay-expiry-input';
  }

  function monthOptionsHtml(mmLabel) {
    var h = '<option value="">' + mmLabel + '</option>';
    for (var m = 1; m <= 12; m++) {
      var v = m < 10 ? '0' + m : String(m);
      h += '<option value="' + v + '">' + v + '</option>';
    }
    return h;
  }

  function yearOptionsHtml(yyyyLabel) {
    var now = new Date().getFullYear();
    var h = '<option value="">' + yyyyLabel + '</option>';
    for (var y = now; y <= now + 15; y++) {
      h += '<option value="' + y + '">' + y + '</option>';
    }
    return h;
  }

  function buildDropdownRow(t) {
    var mm = t('mm') || 'MM';
    var yyyy = t('yyyy') || 'YYYY';
    var cvv = t('cvv') || 'CVV';
    return '' +
      '<div class="col-4 pay-expiry-cell pay-expiry-mm">' +
      '<label class="pay-expiry-lbl visually-hidden" for="payCardmonth">' + mm + '</label>' +
      '<select class="form-select form-select-sm pay-input-round jpay-field-input pay-expiry-input" id="payCardmonth" required aria-label="' + mm + '">' +
      monthOptionsHtml(mm) + '</select></div>' +
      '<div class="col-4 pay-expiry-cell pay-expiry-yyyy">' +
      '<label class="pay-expiry-lbl visually-hidden" for="payCardyear">' + yyyy + '</label>' +
      '<select class="form-select form-select-sm pay-input-round jpay-field-input pay-expiry-input" id="payCardyear" required aria-label="' + yyyy + '">' +
      yearOptionsHtml(yyyy) + '</select></div>' +
      '<div class="col-4 pay-expiry-cell pay-expiry-cvv">' +
      '<label class="pay-expiry-lbl visually-hidden" for="payCardcvv">' + cvv + '</label>' +
      '<input type="text" class="' + inputCls() + '" id="payCardcvv" maxlength="4" placeholder="' + cvv + '" required autocomplete="cc-csc"></div>';
  }

  function buildTextRow(t, yearLen) {
    var mm = t('mm') || 'MM';
    var yyyy = t('yyyy') || (yearLen === 2 ? 'YY' : 'YYYY');
    var cvv = t('cvv') || 'CVV';
    return '' +
      '<div class="col-4 pay-expiry-cell pay-expiry-mm">' +
      '<input type="text" class="' + inputCls() + '" id="payCardmonth" maxlength="2" placeholder="' + mm + '" inputmode="numeric" required autocomplete="cc-exp-month"></div>' +
      '<div class="col-4 pay-expiry-cell pay-expiry-yyyy">' +
      '<input type="text" class="' + inputCls() + '" id="payCardyear" maxlength="' + yearLen + '" placeholder="' + yyyy + '" inputmode="numeric" required autocomplete="cc-exp-year"></div>' +
      '<div class="col-4 pay-expiry-cell pay-expiry-cvv">' +
      '<input type="text" class="' + inputCls() + '" id="payCardcvv" maxlength="4" placeholder="' + cvv + '" required autocomplete="cc-csc"></div>';
  }

  function updateSelectionState(row) {
    if (!row) return;
    var mmEl = row.querySelector('#payCardmonth');
    var yyEl = row.querySelector('#payCardyear');
    var cvvEl = row.querySelector('#payCardcvv');
    var mm = mmEl ? String(mmEl.value || '').trim() : '';
    var yy = yyEl ? String(yyEl.value || '').trim() : '';
    var cvv = cvvEl ? String(cvvEl.value || '').trim() : '';
    var has = !!(mm && yy);
    row.classList.toggle('has-selection', has);
    row.classList.toggle('has-cvv', !!cvv);
  }

  function attachNumericFilter(el, maxLen) {
    if (!el) return;
    el.addEventListener('input', function () {
      el.value = String(el.value || '').replace(/\D/g, '').substring(0, maxLen);
    });
  }

  function wireRow(row, uiMode) {
    if (!row) return;
    var mmEl = row.querySelector('#payCardmonth');
    var yyEl = row.querySelector('#payCardyear');
    var cvvEl = row.querySelector('#payCardcvv');
    if (uiMode === TEXT || uiMode === HYBRID) {
      attachNumericFilter(mmEl, 2);
      attachNumericFilter(yyEl, uiMode === HYBRID ? 2 : 4);
      attachNumericFilter(cvvEl, 4);
    } else {
      attachNumericFilter(cvvEl, 4);
    }
    [mmEl, yyEl, cvvEl].forEach(function (el) {
      if (!el) return;
      el.addEventListener('change', function () { updateSelectionState(row); });
      el.addEventListener('input', function () { updateSelectionState(row); });
    });
    updateSelectionState(row);
  }

  /**
   * @param {HTMLElement} mountRow #payCardExpiryRow
   * @param {object} ctx checkout-context
   * @param {function} t i18n
   * @returns {{ uiMode: string, getMonth: Function, getYear4: Function }}
   */
  function init(mountRow, ctx, t) {
    t = typeof t === 'function' ? t : function (k) { return k; };
    if (!mountRow) {
      return { uiMode: TEXT, getMonth: function () { return ''; }, getYear4: function () { return ''; } };
    }
    var uiMode = resolveUiMode(ctx || {});
    var html = uiMode === DROPDOWN ? buildDropdownRow(t) : buildTextRow(t, uiMode === HYBRID ? 2 : 4);
    mountRow.className = 'row g-2 mb-2 pay-expiry-row pay-expiry-mode-' + uiMode.toLowerCase();
    mountRow.innerHTML = html;
    wireRow(mountRow, uiMode);
    return {
      uiMode: uiMode,
      getMonth: function () {
        var el = mountRow.querySelector('#payCardmonth');
        return el ? pad2(el.value) : '';
      },
      getYear4: function () {
        var el = mountRow.querySelector('#payCardyear');
        return el ? expandYear(el.value) : '';
      },
      refreshI18n: function (t2) {
        t2 = typeof t2 === 'function' ? t2 : t;
        var mm = t2('mm') || 'MM';
        var yyyy = t2('yyyy') || 'YYYY';
        var cvv = t2('cvv') || 'CVV';
        var mmEl = mountRow.querySelector('#payCardmonth');
        var yyEl = mountRow.querySelector('#payCardyear');
        var cvvEl = mountRow.querySelector('#payCardcvv');
        if (uiMode === DROPDOWN) {
          if (mmEl && mmEl.tagName === 'SELECT') {
            var mv = mmEl.value;
            mmEl.innerHTML = monthOptionsHtml(mm);
            if (mv) mmEl.value = mv;
          }
          if (yyEl && yyEl.tagName === 'SELECT') {
            var yv = yyEl.value;
            yyEl.innerHTML = yearOptionsHtml(yyyy);
            if (yv) yyEl.value = yv;
          }
        } else {
          if (mmEl) mmEl.placeholder = mm;
          if (yyEl) yyEl.placeholder = uiMode === HYBRID ? (t2('yy') || 'YY') : yyyy;
        }
        if (cvvEl) cvvEl.placeholder = cvv;
      }
    };
  }

  g.PG_JPAY_CARD_EXPIRY = {
    init: init,
    resolveUiMode: resolveUiMode,
    normalizeMode: normalizeMode,
    expandYear: expandYear,
    pad2: pad2
  };
})(typeof window !== 'undefined' ? window : this);
