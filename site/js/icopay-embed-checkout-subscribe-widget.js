/**
 * ICOPAY 통합 구독(정기결제) iframe 위젯 — PG 무관 중립 경로(/checkout-subscribe/).
 */
(function () {
  'use strict';
  var cfg = window.__ICOPAY_EMBED_CHECKOUT_SUB__;
  if (!cfg || !cfg.compId || !cfg.origin) {
    return;
  }
  var scriptEl = cfg.script || document.currentScript;
  if (!scriptEl) {
    return;
  }
  var sessionToken = String(scriptEl.getAttribute('data-session-token') || '').trim();
  if (!sessionToken) {
    console.error('[ICOPAY] embed-checkout-subscribe: data-session-token is required');
    return;
  }
  var targetId = String(scriptEl.getAttribute('data-target') || 'icopay-checkout-subscribe').trim();
  var mount = document.getElementById(targetId);
  if (!mount) {
    mount = document.createElement('div');
    mount.id = targetId;
    scriptEl.parentNode.insertBefore(mount, scriptEl.nextSibling);
  }
  if (mount.getAttribute('data-icopay-checkout-sub-mounted') === '1') {
    return;
  }
  mount.setAttribute('data-icopay-checkout-sub-mounted', '1');
  mount.style.cssText = 'width:100%;max-width:560px;min-height:720px;margin:0 auto;';

  var origin = String(cfg.origin).replace(/\/$/, '');
  var langCode = '';
  try {
    if (window.IcopayCheckoutLang && typeof window.IcopayCheckoutLang.resolveFromScript === 'function') {
      langCode = window.IcopayCheckoutLang.resolveFromScript(scriptEl);
    }
  } catch (eLang) { /* ignore */ }

  if (!window.IcopayEmbedWidget) {
    console.error('[ICOPAY] embed-checkout-subscribe: IcopayEmbedWidget not loaded');
    return;
  }

  window.IcopayEmbedWidget.fetchSessionAndMount({
    cfg: cfg,
    sessionToken: sessionToken,
    mount: mount,
    sessionUrl: origin + '/api/middleware/v1/merchant/checkout/subscription/session?token=' + encodeURIComponent(sessionToken),
    pagePathForVendor: function () {
      return '/checkout-subscribe/';
    },
    langCode: langCode,
    iframeTitle: 'ICOPAY subscription checkout',
    eventName: 'icopay-checkout-subscribe',
    globalCallbackName: 'onIcopayCheckoutSubscribe',
    detailPatcher: function (d) {
      d.pgVendor = 'ICOPAY';
      d.checkoutKind = d.checkoutKind || 'SUBSCRIPTION';
    }
  });
})();
