/**
 * ChillPay·JPAY 공통 URL 결제 프레젠테이션(언어·결제문구·표시통화 FX).
 * pay.html / jpay-pay.html 에서 PG_URL_PAY_SHELL 로 사용합니다.
 */
(function (g) {
  'use strict';

  function pickLangMap(m, lang) {
    if (!m || typeof m !== 'object') return '';
    var L = lang || 'ENG';
    if (m[L]) return String(m[L]);
    if (m.ENG) return String(m.ENG);
    if (m.KOR) return String(m.KOR);
    var ks = Object.keys(m);
    return ks.length ? String(m[ks[0]]) : '';
  }

  function absPayAssetUrl(path) {
    if (!path) return '';
    var p = String(path).trim();
    if (!p) return '';
    if (/^https?:\/\//i.test(p)) return p;
    try {
      return new URL(p, g.location.origin).href;
    } catch (e) {
      return p.charAt(0) === '/' ? p : '/' + p;
    }
  }

  function fetchJsonWithRetry(url, attempts, delayMs) {
    var left = attempts != null ? attempts : 3;
    var delay = delayMs != null ? delayMs : 500;
    function run() {
      return fetch(url, { credentials: 'omit', cache: 'no-store' }).then(function (r) {
        return r.text().then(function (text) {
          if (!r.ok) throw new Error(text || ('HTTP ' + r.status));
          return text ? JSON.parse(text) : {};
        });
      }).catch(function (err) {
        left -= 1;
        if (left <= 0) throw err;
        return new Promise(function (resolve) {
          setTimeout(resolve, delay);
        }).then(run);
      });
    }
    return run();
  }

  /**
   * @param {object} opts
   * @param {string} opts.lang
   * @param {object} [opts.cardCopy] urlPayCardCopy from checkout-context
   * @param {function} opts.t i18n lookup
   * @param {function} [opts.onApply] called after DOM text updates
   */
  function applyCardCopyPresentation(opts) {
    opts = opts || {};
    var lang = opts.lang || 'ENG';
    var c = opts.cardCopy;
    g.__payUrlCardCopy = c || null;
    var sub = document.querySelector('#jpayBrandBlock .pay-brand-sub, #payBrandTextWrap .pay-brand-sub, .pay-brand-sub');
    if (sub && c && c.brandSub) {
      var bs = pickLangMap(c.brandSub, lang);
      if (bs) sub.textContent = bs;
    } else if (sub && opts.t) {
      var fb = opts.t('brandSub');
      if (fb) sub.textContent = fb;
    }
    var title = '';
    if (c && c.browserTabTitle && typeof c.browserTabTitle === 'object') {
      title = String(pickLangMap(c.browserTabTitle, lang) || '').trim();
    }
    if (title) g.document.title = title;
    var favPath = (c && c.faviconUrl) ? String(c.faviconUrl).trim() : '';
    var head = g.document.head;
    if (head) {
      var link = g.document.querySelector('link[data-pay-favicon="1"]');
      if (!favPath) {
        if (link) link.remove();
      } else {
        var href = absPayAssetUrl(favPath);
        if (!href) return;
        if (!link) {
          link = g.document.createElement('link');
          link.setAttribute('data-pay-favicon', '1');
          link.setAttribute('rel', 'icon');
          link.setAttribute('type', 'image/png');
          head.appendChild(link);
        }
        link.setAttribute('href', href + (href.indexOf('?') >= 0 ? '&' : '?') + 'v=' + Date.now());
      }
    }
    var sec = g.document.getElementById('payCardSectionLabel');
    var note = g.document.getElementById('payCardNoteText');
    if (sec) {
      var t1 = c && c.cardSection ? pickLangMap(c.cardSection, lang) : '';
      sec.textContent = t1 || (opts.t ? opts.t('card') : 'Card');
    }
    if (note) {
      var tNote = c && c.cardNote ? pickLangMap(c.cardNote, lang) : '';
      if (tNote) {
        note.textContent = tNote;
        note.classList.remove('d-none');
      } else {
        note.textContent = '';
        note.classList.add('d-none');
      }
    }
    if (typeof opts.onApply === 'function') opts.onApply();
  }

  function applyAmountScaleNotice(opts) {
    opts = opts || {};
    var el = g.document.getElementById('payAmountScaleNotice');
    if (!el) return;
    function hide() {
      el.textContent = '';
      el.classList.add('d-none');
    }
    var c = opts.cardCopy;
    var mode = String(opts.scaleMode || 'SAME').toUpperCase();
    if (mode !== 'MULTIPLY_100' && mode !== 'DIVIDE_100') {
      hide();
      return;
    }
    if (!c || typeof c !== 'object') {
      hide();
      return;
    }
    var showFlag = c.amountScaleNoticeShow;
    if (showFlag === false || showFlag === 'N' || showFlag === 'n' || showFlag === 0) {
      hide();
      return;
    }
    var map = c.amountScaleNotice;
    var custom = map && typeof map === 'object' ? pickLangMap(map, opts.lang) : '';
    if (!custom) {
      hide();
      return;
    }
    el.textContent = custom;
    el.classList.remove('d-none');
  }

  /**
   * 표시통화(FX) UI — ChillPay display-fx-quote API 재사용.
   * @param {object} st mutable state: urlPayFxQuote, urlPayDisplayFxActive, ...
   */
  function initDisplayFx(st) {
    st = st || {};
    var ctx = st.checkoutCtx;
    var compId = st.compId;
    var apiUrl = st.apiUrl;
    var t = st.t || function (k) { return k; };
    if (!ctx || !compId || !apiUrl) return Promise.resolve();

    st.urlPayDisplayFxActive = ctx.urlPayDisplayFxActive === true;
    st.urlPayPricingMode = String(ctx.urlPayPricingMode || 'CHECKOUT_CURRENCY');
    st.urlPayFxUiBlind = ctx.urlPayFxUiBlind === true;
    st.urlPayDisplayFxRefreshSeconds = parseInt(ctx.urlPayDisplayFxRefreshSeconds, 10) || 600;

    function payFxResolvedDisplayCurrencyLocal() {
      return payFxResolvedDisplayCurrency(ctx);
    }

    function updatePayFxCurrencyHint() {
      var curHint = g.document.getElementById('payAmountCurrencyHint');
      if (!curHint) return;
      if (st.urlPayDisplayFxActive) {
        curHint.textContent = '(' + payFxResolvedDisplayCurrencyLocal() + ')';
      }
    }

    function payFxFormatSettleAmount(n, intSet) {
      if (!isFinite(n)) return String(n);
      if (intSet) return String(Math.round(n));
      var r = Math.round(n * 100) / 100;
      return (Math.abs(r - Math.round(r)) < 1e-6) ? String(Math.round(r)) : r.toFixed(2);
    }

    function updateFxSettlementEstimateText() {
      var el = g.document.getElementById('payFxThbEstimateText');
      if (!el) return;
      var q = st.urlPayFxQuote;
      var spuRaw = q && (q.settlementPerUnit != null ? q.settlementPerUnit : q.thbPerUnit);
      if (!q || spuRaw == null) {
        el.textContent = '—';
        return;
      }
      var amtEl = g.document.getElementById('amount');
      var amt = amtEl ? parseFloat(String(amtEl.value).replace(/,/g, '')) : NaN;
      if (!(amt > 0)) {
        el.textContent = '—';
        return;
      }
      var tpu = parseFloat(String(spuRaw));
      var mgn = parseFloat(String(q.marginRate != null ? q.marginRate : '0'));
      if (isNaN(tpu) || isNaN(mgn)) {
        el.textContent = '—';
        return;
      }
      var setCur = q.settlementCurrency != null && String(q.settlementCurrency).trim() !== ''
        ? String(q.settlementCurrency).trim().toUpperCase() : 'THB';
      var gross = amt * tpu * (1 + mgn);
      var intSet = (setCur === 'JPY' || setCur === 'KRW');
      var shown = intSet ? Math.round(gross) : Math.round(gross * 100) / 100;
      el.textContent = payFxFormatSettleAmount(shown, intSet) + ' ' + setCur;
    }

    function fetchFxQuote() {
      var cur = payFxResolvedDisplayCurrencyLocal();
      var q = 'compId=' + encodeURIComponent(compId) + '&displayCurrency=' + encodeURIComponent(cur);
      return fetchJsonWithRetry(apiUrl('/api/pay/url/display-fx-quote', q), 4, 700).then(function (res) {
        if (res.success !== true || !res.data) throw new Error(res.message || t('configErr'));
        st.urlPayFxQuote = res.data;
        updateFxSettlementEstimateText();
        if (st.urlPayFxReloadTimer) {
          try { g.clearTimeout(st.urlPayFxReloadTimer); } catch (eT) { /* ignore */ }
        }
        var sec = parseInt(st.urlPayDisplayFxRefreshSeconds, 10);
        if (isNaN(sec) || sec < 1) sec = 600;
        st.urlPayFxReloadTimer = g.setTimeout(function () {
          try { g.location.reload(); } catch (eR) { /* ignore */ }
        }, sec * 1000);
      }).catch(function (err) {
        st.urlPayFxQuote = null;
        var fel = g.document.getElementById('payFxThbEstimateText');
        if (fel) fel.textContent = (err && err.message) ? String(err.message) : t('configErr');
      });
    }

    if (!st.urlPayDisplayFxActive) {
      var rowC0 = g.document.getElementById('payFxDisplayCurrencyRow');
      var rowQ0 = g.document.getElementById('payFxQuoteRow');
      if (rowC0) rowC0.classList.add('d-none');
      if (rowQ0) rowQ0.classList.add('d-none');
      return Promise.resolve();
    }

    var blind = st.urlPayFxUiBlind === true;
    var multi = ctx.urlPayDisplayFxDisplayCurrencyMulti === true;
    var rowC = g.document.getElementById('payFxDisplayCurrencyRow');
    var rowQ = g.document.getElementById('payFxQuoteRow');
    if (rowC) {
      if (blind && !multi) rowC.classList.add('d-none');
      else rowC.classList.remove('d-none');
    }
    if (rowQ) {
      if (blind) rowQ.classList.add('d-none');
      else rowQ.classList.remove('d-none');
    }
    var sc = g.document.getElementById('payDisplayCurrencySelect');
    var stFx = g.document.getElementById('payFxDisplayCurrencyStatic');
    if (sc) {
      var allowed = ctx.urlPayDisplayFxDisplayCurrencies;
      var codes = Array.isArray(allowed) && allowed.length
        ? allowed.map(function (c) { return String(c || '').trim().toUpperCase(); }).filter(Boolean)
        : ['THB', 'JPY', 'USD', 'KRW', 'SGD', 'HKD', 'CNY'];
      if (codes.length) {
        sc.innerHTML = codes.map(function (u) {
          return '<option value="' + u + '">' + u + '</option>';
        }).join('');
      }
      var defFx = ctx.urlPayDisplayFxDefaultDisplayCurrency != null
        ? String(ctx.urlPayDisplayFxDefaultDisplayCurrency).trim().toUpperCase() : '';
      var pick = codes.indexOf(defFx) >= 0 ? defFx : '';
      if (!pick) pick = codes.indexOf('JPY') >= 0 ? 'JPY' : (codes[0] || 'JPY');
      sc.value = pick;
      if (multi) {
        sc.classList.remove('d-none');
        if (stFx) stFx.classList.add('d-none');
      } else {
        sc.classList.add('d-none');
        if (stFx) {
          stFx.textContent = pick;
          stFx.classList.remove('d-none');
        }
      }
    }
    g.__pgPayFxDisplaySelectSynced = true;
    updatePayFxCurrencyHint();
    if (sc && !sc._payFxBound) {
      sc._payFxBound = true;
      sc.addEventListener('change', function () {
        updatePayFxCurrencyHint();
        fetchFxQuote();
      });
    }
    var amtEl = g.document.getElementById('amount');
    if (amtEl && !amtEl._payFxAmtBound) {
      amtEl._payFxAmtBound = true;
      amtEl.addEventListener('input', function () { updateFxSettlementEstimateText(); });
    }
    return fetchFxQuote();
  }

  function wireLanguageButtons(getLang, setLang) {
    g.document.querySelectorAll('.pay-lang button[data-lang]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var code = btn.getAttribute('data-lang');
        if (!code) return;
        setLang(code);
        g.document.querySelectorAll('.pay-lang button[data-lang]').forEach(function (b) {
          b.classList.toggle('active', b.getAttribute('data-lang') === getLang());
        });
      });
    });
    g.document.querySelectorAll('.pay-lang button[data-lang]').forEach(function (b) {
      b.classList.toggle('active', b.getAttribute('data-lang') === getLang());
    });
  }

  /** DISPLAY_FX: 멀티 표시통화 셀렉트 또는 본사 고정 표시통화. */
  function payFxResolvedDisplayCurrency(checkoutCtx) {
    if (!checkoutCtx) return 'JPY';
    if (checkoutCtx.urlPayDisplayFxDisplayCurrencyMulti === true) {
      if (!g.__pgPayFxDisplaySelectSynced) {
        return checkoutCtx.urlPayDisplayFxDefaultDisplayCurrency != null
          ? String(checkoutCtx.urlPayDisplayFxDefaultDisplayCurrency).trim().toUpperCase() : 'JPY';
      }
      var sc = g.document.getElementById('payDisplayCurrencySelect');
      if (sc && !sc.classList.contains('d-none')) {
        return String(sc.value || checkoutCtx.urlPayDisplayFxDefaultDisplayCurrency || 'JPY').trim().toUpperCase();
      }
    }
    return checkoutCtx.urlPayDisplayFxDefaultDisplayCurrency != null
      ? String(checkoutCtx.urlPayDisplayFxDefaultDisplayCurrency).trim().toUpperCase() : 'JPY';
  }

  g.PG_URL_PAY_SHELL = {
    pickLangMap: pickLangMap,
    absPayAssetUrl: absPayAssetUrl,
    fetchJsonWithRetry: fetchJsonWithRetry,
    applyCardCopyPresentation: applyCardCopyPresentation,
    applyAmountScaleNotice: applyAmountScaleNotice,
    initDisplayFx: initDisplayFx,
    wireLanguageButtons: wireLanguageButtons,
    payFxResolvedDisplayCurrency: payFxResolvedDisplayCurrency
  };
})(window);
