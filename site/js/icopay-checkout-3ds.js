/**
 * ICOPAY checkout 3DS / embed navigation — shared by pay pages, embed widgets, WooCommerce.
 * Default behavior preserves production: iframe embed + top-window 3DS breakout.
 */
(function (global) {
  'use strict';

  var MODES = { EMBED: 'EMBED', MOBILE_REDIRECT: 'MOBILE_REDIRECT', ALWAYS_REDIRECT: 'ALWAYS_REDIRECT' };

  var I18N = {
    KOR: {
      wait3ds: '카드 인증 페이지로 이동 중입니다…',
      wait3dsHint: '새 창이 열리지 않으면 잠시 후 다시 시도해 주세요.',
      confirming: '결제 결과를 확인하는 중입니다…',
      sessionInvalid: '결제 세션이 유효하지 않습니다.'
    },
    ENG: {
      wait3ds: 'Redirecting to card authentication…',
      wait3dsHint: 'If nothing opens, please wait and try again.',
      confirming: 'Confirming payment result…',
      sessionInvalid: 'Checkout session is invalid.'
    },
    JPN: {
      wait3ds: 'カード認証ページへ移動しています…',
      wait3dsHint: '画面が切り替わらない場合はしばらくお待ちください。',
      confirming: '決済結果を確認しています…',
      sessionInvalid: '決済セッションが無効です。'
    },
    CHN: {
      wait3ds: '正在跳转到银行卡认证页面…',
      wait3dsHint: '若未跳转，请稍候后重试。',
      confirming: '正在确认支付结果…',
      sessionInvalid: '支付会话无效。'
    },
    THA: {
      wait3ds: 'กำลังไปหน้ายืนยันบัตร…',
      wait3dsHint: 'หากไม่เปลี่ยนหน้า กรุณารอสักครู่แล้วลองอีกครั้ง',
      confirming: 'กำลังยืนยันผลการชำระ…',
      sessionInvalid: 'เซสชันชำระเงินไม่ถูกต้อง'
    }
  };

  function resolveLang() {
    try {
      if (global.IcopayCheckoutLang && typeof global.IcopayCheckoutLang.resolveFromDocument === 'function') {
        var c = global.IcopayCheckoutLang.resolveFromDocument();
        if (c) return c;
      }
    } catch (e0) { /* ignore */ }
    try {
      var h = (document.documentElement.getAttribute('lang') || '').toLowerCase();
      if (h.indexOf('ko') === 0) return 'KOR';
      if (h.indexOf('ja') === 0) return 'JPN';
      if (h.indexOf('zh') === 0) return 'CHN';
      if (h.indexOf('th') === 0) return 'THA';
    } catch (e1) { /* ignore */ }
    return 'ENG';
  }

  function t(key) {
    var lang = resolveLang();
    var pack = I18N[lang] || I18N.ENG;
    return pack[key] != null ? pack[key] : (I18N.ENG[key] || key);
  }

  function isMobileUa() {
    try {
      if (global.matchMedia && global.matchMedia('(max-width: 767px)').matches) return true;
    } catch (eM) { /* ignore */ }
    try {
      var ua = navigator.userAgent || '';
      return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Mobile/i.test(ua);
    } catch (eU) {
      return false;
    }
  }

  function normalizeMode(mode) {
    var m = String(mode || MODES.EMBED).trim().toUpperCase();
    if (m === MODES.MOBILE_REDIRECT || m === 'MOBILE') return MODES.MOBILE_REDIRECT;
    if (m === MODES.ALWAYS_REDIRECT || m === 'REDIRECT' || m === 'ALWAYS') return MODES.ALWAYS_REDIRECT;
    return MODES.EMBED;
  }

  function shouldFullPagePayUrl(mode) {
    var m = normalizeMode(mode);
    if (m === MODES.ALWAYS_REDIRECT) return true;
    return m === MODES.MOBILE_REDIRECT && isMobileUa();
  }

  function isEmbedContext(opts) {
    if (opts && opts.embed === true) return true;
    if (opts && opts.embed === false) return false;
    try {
      if (global.self !== global.top) return true;
    } catch (eTop) {
      return true;
    }
    try {
      var q = new URLSearchParams(global.location.search);
      return q.get('embed') === '1' || q.get('embed') === 'true';
    } catch (eQ) {
      return false;
    }
  }

  function showWaitOverlay(message, hint) {
    var id = 'icopay-3ds-wait-overlay';
    var el = document.getElementById(id);
    if (!el) {
      el = document.createElement('div');
      el.id = id;
      el.setAttribute('role', 'status');
      el.style.cssText = 'position:fixed;inset:0;z-index:2147483001;background:rgba(255,255,255,.94);'
          + 'display:flex;align-items:center;justify-content:center;padding:24px;font:15px/1.5 system-ui,sans-serif;';
      el.innerHTML = '<div style="max-width:360px;text-align:center">'
          + '<div class="spinner-border text-primary mb-3" role="presentation"></div>'
          + '<div id="icopay-3ds-wait-msg" style="font-weight:600;color:#111"></div>'
          + '<div id="icopay-3ds-wait-hint" style="margin-top:8px;color:#666;font-size:13px"></div>'
          + '</div>';
      document.body.appendChild(el);
    }
    var msgEl = document.getElementById('icopay-3ds-wait-msg');
    var hintEl = document.getElementById('icopay-3ds-wait-hint');
    if (msgEl) msgEl.textContent = message || t('wait3ds');
    if (hintEl) hintEl.textContent = hint || t('wait3dsHint');
    el.style.display = 'flex';
  }

  function hideWaitOverlay() {
    var el = document.getElementById('icopay-3ds-wait-overlay');
    if (el) el.style.display = 'none';
  }

  /**
   * 3DS·OTP — embed iframe 이면 top(또는 parent) 전체 이동, 단독 탭이면 same-tab.
   */
  function navigateToPaymentUrl(url, opts) {
    if (!url) return;
    var u = String(url).trim();
    if (!u) return;
    opts = opts || {};
    if (opts.showWait !== false) {
      showWaitOverlay(opts.waitMessage, opts.waitHint);
    }
    var embed = isEmbedContext(opts);
    try {
      if (embed) {
        var target = global.top || global.parent || global;
        target.location.href = u;
        return;
      }
    } catch (eNav) { /* fallback */ }
    global.location.href = u;
  }

  function pollStatus(statusUrl, opts, done) {
    if (!statusUrl || typeof fetch !== 'function') {
      if (done) done(false);
      return;
    }
    opts = opts || {};
    var attempts = 0;
    var max = opts.maxAttempts != null ? opts.maxAttempts : 12;
    var interval = opts.intervalMs != null ? opts.intervalMs : 1500;

    function tick() {
      attempts += 1;
      fetch(statusUrl, { credentials: 'omit', headers: { Accept: 'application/json' } })
        .then(function (r) { return r.json(); })
        .then(function (res) {
          var ps = '';
          if (res && res.data && res.data.paymentStatus) ps = String(res.data.paymentStatus).toUpperCase();
          if (ps === 'PAID' || ps === 'APPROVED' || ps === 'SUCCESS') {
            if (done) done(true, res);
            return;
          }
          if (attempts >= max) {
            if (done) done(false, res);
            return;
          }
          setTimeout(tick, interval);
        })
        .catch(function () {
          if (attempts >= max) {
            if (done) done(false);
            return;
          }
          setTimeout(tick, interval);
        });
    }
    tick();
  }

  /**
   * embed 부모 — wait_authorize 시 paymentUrl 로 top 이동, finished 시 status 폴링(선택).
   */
  function handleInlineCheckoutMessage(detail, ctx) {
    detail = detail || {};
    ctx = ctx || {};
    var phase = String(detail.phase || '');
    if (phase === 'wait_authorize') {
      var payUrl = detail.paymentUrl || detail.redirectUrl;
      if (payUrl) {
        navigateToPaymentUrl(payUrl, {
          embed: true,
          waitMessage: ctx.waitMessage,
          waitHint: ctx.waitHint
        });
      } else if (ctx.onWaitWithoutUrl) {
        ctx.onWaitWithoutUrl(detail);
      }
      return true;
    }
    if (phase === 'finished' && detail.success && ctx.statusUrl) {
      showWaitOverlay(ctx.confirmMessage || t('confirming'), '');
      pollStatus(ctx.statusUrl, ctx.pollOpts, function (ok) {
        hideWaitOverlay();
        if (ctx.onFinished) ctx.onFinished(ok, detail);
      });
      return true;
    }
    return false;
  }

  function bindEmbedMessageListener(origin, ctx) {
    if (!origin) return;
    var allowed = String(origin).replace(/\/$/, '');
    global.addEventListener('message', function (ev) {
      if (!ev || !ev.data || ev.data.type !== 'ICOPAY_INLINE_CHECKOUT') return;
      try {
        if (ev.origin !== allowed) return;
      } catch (eO) { return; }
      handleInlineCheckoutMessage(ev.data.detail || {}, ctx);
    }, false);
  }

  global.IcopayCheckout3ds = {
    MODES: MODES,
    t: t,
    isMobileUa: isMobileUa,
    normalizeMode: normalizeMode,
    shouldFullPagePayUrl: shouldFullPagePayUrl,
    isEmbedContext: isEmbedContext,
    showWaitOverlay: showWaitOverlay,
    hideWaitOverlay: hideWaitOverlay,
    navigateToPaymentUrl: navigateToPaymentUrl,
    pollStatus: pollStatus,
    handleInlineCheckoutMessage: handleInlineCheckoutMessage,
    bindEmbedMessageListener: bindEmbedMessageListener
  };
})(typeof window !== 'undefined' ? window : this);
