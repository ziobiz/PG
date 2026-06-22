/**
 * ChillPay·JPAY 공통 URL 결제 프레젠테이션(언어·결제문구·표시통화 FX).
 * pay.html / jpay-pay.html 에서 PG_URL_PAY_SHELL 로 사용합니다.
 */
(function (g) {
  'use strict';

  var URL_PAY_DEFAULT_TAB_TITLE = {
    KOR: 'ICOPAY 간편결제',
    ENG: 'ICOPAY Easy Payment',
    JPN: 'ICOPAY かんたん決済',
    CHN: 'ICOPAY 便捷支付',
    THA: 'ICOPAY ชำระเงินง่าย'
  };

  function resolveUrlPayBrowserTabTitle(lang, cardCopy, tFn) {
    if (cardCopy && cardCopy.browserTabTitle && typeof cardCopy.browserTabTitle === 'object') {
      var custom = String(pickLangMap(cardCopy.browserTabTitle, lang) || '').trim();
      if (custom) return custom;
    }
    if (typeof tFn === 'function') {
      var fromT = String(tFn('title') || '').trim();
      if (fromT && fromT !== 'title') return fromT;
    }
    return URL_PAY_DEFAULT_TAB_TITLE[lang] || URL_PAY_DEFAULT_TAB_TITLE.ENG;
  }

  function applyUrlPayBrowserTabTitle(lang, cardCopy, tFn) {
    g.document.title = resolveUrlPayBrowserTabTitle(lang, cardCopy, tFn);
  }

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
    if (sub && c && c.brandSub && !opts.skipBrandSub) {
      var bs = pickLangMap(c.brandSub, lang);
      if (bs) sub.textContent = bs;
    } else if (sub && opts.t && !opts.skipBrandSub) {
      var fb = opts.t('brandSub3ds') || opts.t('brandSub');
      if (fb) sub.textContent = fb;
    }
    applyUrlPayBrowserTabTitle(lang, c, opts.t);
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
        if (g.__urlPayLangMenuDisabled) return;
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

  function urlPayYnIsYes(v, defaultYes) {
    if (v == null || String(v).trim() === '') return defaultYes !== false;
    return String(v).trim().toUpperCase() !== 'N';
  }

  /** jpay-pay.html·pay.html 등 — 가맹점명(Merchant) 행 */
  function resolveMerchantNameDisplayRow() {
    var disp = g.document.getElementById('merchantNameDisplay');
    if (disp) {
      var byId = disp.closest('.pay-row-static');
      if (byId) return byId;
    }
    return g.document.querySelector('#payOrderSummaryTop .pay-row-static');
  }

    /** checkout-context — 입력방식(GENERAL|TYPE_AA|…|TYPE_CN) 및 표시 옵션 */
  function normalizeUrlPayInputMode(raw) {
    if (g.PG_URL_PAY_INPUT_MODE && g.PG_URL_PAY_INPUT_MODE.normalize) {
      return g.PG_URL_PAY_INPUT_MODE.normalize(raw);
    }
    return String(raw || 'GENERAL').trim().toUpperCase();
  }

  function getUrlPayInputModePreset(raw) {
    if (g.PG_URL_PAY_INPUT_MODE && g.PG_URL_PAY_INPUT_MODE.getPreset) {
      return g.PG_URL_PAY_INPUT_MODE.getPreset(raw);
    }
    return null;
  }

  function isUrlPayInputModeControlled(raw) {
    return getUrlPayInputModePreset(raw) != null;
  }

  function resolveUrlPayEffectiveCheckoutCtx(ctx) {
    return ctx || {};
  }

  function applyUrlPayInputMode(ctx, opts) {
    opts = opts || {};
    if (!ctx) return;
    var mode = normalizeUrlPayInputMode(ctx.urlPayInputMode);
    /* 가맹 저장값(로고·경고·상품명·가맹점명·다국어) — 입력방식 프리set으로 덮지 않음 */
    applyUrlPayPresentationOptions(ctx, opts);

    var brandRow = g.document.getElementById('payCardBrandRow');
    var brandSelect = g.document.getElementById('payCardBrandSelect');
    var hideBrand = (g.PG_URL_PAY_INPUT_MODE && g.PG_URL_PAY_INPUT_MODE.hidesCardBrandSelect)
      ? g.PG_URL_PAY_INPUT_MODE.hidesCardBrandSelect(mode)
      : (mode === 'TYPE_AN' || mode === 'TYPE_AG' || mode === 'TYPE_AF' || mode === 'TYPE_AE'
        || mode === 'TYPE_AA');
    if (brandSelect && (hideBrand || mode === 'GENERAL')) {
      brandSelect.value = 'AUTO';
    }
    /* TYPE_A/AG: 드롭다운만 숨김 — PAN 입력 시 PG_CARD_PAY_POLICY 자동 인식·포맷은 유지 */
    if (brandRow) {
      brandRow.style.display = hideBrand ? 'none' : '';
    }

    /* A/AG/B/BG/C·일반 — JPAY 필수: 카드 + 이름·성 */
    var nameRow = g.document.getElementById('nameRow');
    if (nameRow) nameRow.style.display = '';
    ['payFirstname', 'payLastname'].forEach(function (id) {
      var el = g.document.getElementById(id);
      if (el) el.setAttribute('required', 'required');
    });
  }

  /** checkout-context — 상품명·가맹점명·다국어 메뉴 표시 옵션(결제 전문과 별개, 화면 노출만) */
  function applyUrlPayPresentationOptions(ctx, opts) {
    opts = opts || {};
    if (!ctx) return;
    var showCo = urlPayYnIsYes(ctx.urlPayCompanyNameShowYn, true);
    var showItem = urlPayYnIsYes(ctx.urlPayProductNameUseYn, true);
    var showLang = urlPayYnIsYes(ctx.urlPayLangMenuUseYn, true);
    var merchRow = resolveMerchantNameDisplayRow();
    if (merchRow) merchRow.style.display = showCo ? '' : 'none';
    var itemEl = g.document.getElementById('item');
    if (itemEl) {
      var itemWrap = itemEl.closest('.pay-row');
      if (itemWrap) {
        itemWrap.style.display = showItem ? '' : 'none';
        if (showItem) itemEl.setAttribute('required', 'required');
        else {
          itemEl.removeAttribute('required');
          if (!itemEl.readOnly) itemEl.value = '';
        }
      }
    }
    g.document.querySelectorAll('.pay-lang').forEach(function (el) {
      el.style.display = showLang ? '' : 'none';
    });
    g.__urlPayLangMenuDisabled = !showLang;
    if (!showLang && typeof opts.detectBrowserLang === 'function' && typeof opts.setLang === 'function') {
      var bl = opts.detectBrowserLang();
      var cur = typeof opts.getLang === 'function' ? opts.getLang() : '';
      if (bl && bl !== cur) opts.setLang(bl);
    }
  }

  function resolveUrlPayItemValue(ctx) {
    var itemEl = g.document.getElementById('item');
    var v = itemEl ? String(itemEl.value || '').trim() : '';
    if (v) return v;
    if (ctx && !urlPayYnIsYes(ctx.urlPayProductNameUseYn, true)) return 'Online Payment';
    return v;
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

  /**
   * 결제창 상단 로고 — DEFAULT(총판 이미지·없으면 숨김), HTML(ICOPAY 문구), ACTIVE(가맹 이미지), DISABLED(전체 숨김).
   * @param {object} ctx checkout-context
   * @param {{ brandEl?: Element, brandBlockId?: string, logoWrapSelector?: string, imgId?: string, textWrapId?: string, t?: function }} [opts]
   */
  function applyCheckoutHeaderLogo(ctx, opts) {
    opts = opts || {};
    ctx = ctx || {};
    var mode = String(ctx.checkoutHeaderLogoMode || 'DEFAULT').trim().toUpperCase();
    var brand = opts.brandEl
      || (opts.brandBlockId ? g.document.getElementById(opts.brandBlockId) : null)
      || g.document.querySelector('.pay-brand');
    if (!brand) return;

    if (mode === 'DISABLED') {
      brand.style.display = 'none';
      return;
    }

    var logoUrl = ctx.checkoutHeaderLogoUrl ? String(ctx.checkoutHeaderLogoUrl).trim() : '';
    var showImage = false;
    var showText = false;

    if (mode === 'HTML') {
      showText = true;
    } else if (mode === 'ACTIVE') {
      if (!logoUrl) {
        brand.style.display = 'none';
        return;
      }
      showImage = true;
    } else if (mode === 'DEFAULT') {
      if (!logoUrl) {
        brand.style.display = 'none';
        return;
      }
      showImage = true;
    } else {
      if (!logoUrl) {
        brand.style.display = 'none';
        return;
      }
      showImage = true;
    }

    brand.style.display = '';

    var img = opts.imgId ? g.document.getElementById(opts.imgId) : null;
    var textWrap = opts.textWrapId ? g.document.getElementById(opts.textWrapId) : null;
    var logoWrap = opts.logoWrapSelector ? brand.querySelector(opts.logoWrapSelector) : null;
    var brandTitle = opts.t ? (opts.t('brandTitle') || 'ICOPAY') : 'ICOPAY';

    if (img && textWrap) {
      if (showImage) {
        img.src = absPayAssetUrl(logoUrl);
        img.alt = ctx.merchantName || 'ICOPAY';
        img.classList.remove('d-none');
        textWrap.classList.add('d-none');
      } else if (showText) {
        img.removeAttribute('src');
        img.classList.add('d-none');
        textWrap.classList.remove('d-none');
        var textEl = textWrap.querySelector('.pay-brand-logo');
        if (textEl) textEl.textContent = brandTitle;
      }
      return;
    }

    if (logoWrap) {
      if (showImage) {
        logoWrap.innerHTML = '';
        var imgEl = g.document.createElement('img');
        imgEl.src = absPayAssetUrl(logoUrl);
        imgEl.alt = ctx.merchantName || 'ICOPAY';
        imgEl.className = 'pay-brand-logo-img';
        logoWrap.appendChild(imgEl);
      } else if (showText) {
        logoWrap.innerHTML = '';
        logoWrap.className = 'pay-brand-logo';
        logoWrap.textContent = brandTitle;
      }
    }
  }

  /**
   * 결제창 로고 아래 경고/안내 문구 — 가맹 webPaymentHeaderSubtitle* + 로고 미활성 시 숨김.
   * @param {object} ctx checkout-context
   * @param {{ t?: function }} [opts]
   */
  function applyCheckoutHeaderSubtitle(ctx, opts) {
    opts = opts || {};
    var sub = g.document.querySelector('#jpayBrandBlock .pay-brand-sub, #payBrandTextWrap .pay-brand-sub, .pay-brand-sub');
    if (!sub) return;
    ctx = ctx || {};
    var logoMode = String(ctx.checkoutHeaderLogoMode || 'DEFAULT').trim().toUpperCase();
    if (logoMode === 'DISABLED') {
      sub.style.display = 'none';
      sub.textContent = '';
      return;
    }
    if (logoMode === 'ACTIVE') {
      var logoUrl = ctx.checkoutHeaderLogoUrl ? String(ctx.checkoutHeaderLogoUrl).trim() : '';
      if (!logoUrl) {
        sub.style.display = 'none';
        sub.textContent = '';
        return;
      }
    }
    var brand = g.document.getElementById('jpayBrandBlock') || g.document.querySelector('.pay-brand');
    if (brand && brand.style.display === 'none') {
      sub.style.display = 'none';
      return;
    }
    var mode = String(ctx.checkoutHeaderSubtitleMode || 'DEFAULT').trim().toUpperCase();
    if (mode === 'DISABLED') {
      sub.style.display = 'none';
      return;
    }
    if (mode === 'ACTIVE') {
      var custom = ctx.checkoutHeaderSubtitleText ? String(ctx.checkoutHeaderSubtitleText).trim() : '';
      if (!custom) {
        sub.style.display = 'none';
        return;
      }
      sub.style.display = '';
      sub.textContent = custom;
      return;
    }
    sub.style.display = '';
    if (opts.t) {
      sub.textContent = opts.t('brandSub3ds') || opts.t('brandSub') || '3DS Secure Payment';
    } else {
      sub.textContent = '3DS Secure Payment';
    }
  }

  g.PG_URL_PAY_SHELL = {
    applyUrlPayBrowserTabTitle: applyUrlPayBrowserTabTitle,
    resolveUrlPayBrowserTabTitle: resolveUrlPayBrowserTabTitle,
    URL_PAY_DEFAULT_TAB_TITLE: URL_PAY_DEFAULT_TAB_TITLE,
    absPayAssetUrl: absPayAssetUrl,
    fetchJsonWithRetry: fetchJsonWithRetry,
    applyCardCopyPresentation: applyCardCopyPresentation,
    applyCheckoutHeaderLogo: applyCheckoutHeaderLogo,
    applyCheckoutHeaderSubtitle: applyCheckoutHeaderSubtitle,
    applyAmountScaleNotice: applyAmountScaleNotice,
    initDisplayFx: initDisplayFx,
    wireLanguageButtons: wireLanguageButtons,
    applyUrlPayPresentationOptions: applyUrlPayPresentationOptions,
    applyUrlPayInputMode: applyUrlPayInputMode,
    normalizeUrlPayInputMode: normalizeUrlPayInputMode,
    getUrlPayInputModePreset: getUrlPayInputModePreset,
    isUrlPayInputModeControlled: isUrlPayInputModeControlled,
    resolveUrlPayEffectiveCheckoutCtx: resolveUrlPayEffectiveCheckoutCtx,
    resolveUrlPayItemValue: resolveUrlPayItemValue,
    payFxResolvedDisplayCurrency: payFxResolvedDisplayCurrency
  };
})(window);
