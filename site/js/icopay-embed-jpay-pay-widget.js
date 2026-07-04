/**
 * @deprecated 레거시 URL(/v1/embed-jpay-pay/) 호환 — 통합 위젯과 동일 동작.
 */
(function () {
  'use strict';
  var cfg = window.__ICOPAY_EMBED_CHECKOUT__ || window.__ICOPAY_EMBED_JPAY_PAY__;
  if (!cfg || !cfg.compId || !cfg.origin) {
    return;
  }
  var scriptEl = cfg.script || document.currentScript;
  if (!scriptEl) {
    return;
  }
  var sessionToken = String(scriptEl.getAttribute('data-session-token') || '').trim();
  if (!sessionToken) {
    console.error('[ICOPAY] embed-checkout: data-session-token is required');
    return;
  }
  var targetId = String(scriptEl.getAttribute('data-target') || 'icopay-checkout').trim();
  var mount = document.getElementById(targetId);
  if (!mount) {
    mount = document.createElement('div');
    mount.id = targetId;
    scriptEl.parentNode.insertBefore(mount, scriptEl.nextSibling);
  }
  if (mount.getAttribute('data-icopay-checkout-mounted') === '1') {
    return;
  }
  mount.setAttribute('data-icopay-checkout-mounted', '1');
  mount.style.cssText = 'width:100%;max-width:560px;min-height:640px;margin:0 auto;';

  var origin = String(cfg.origin).replace(/\/$/, '');
  var langCode = '';
  try {
    if (window.IcopayCheckoutLang && typeof window.IcopayCheckoutLang.resolveFromScript === 'function') {
      langCode = window.IcopayCheckoutLang.resolveFromScript(scriptEl);
    }
  } catch (eLang) { /* ignore */ }

  if (!window.IcopayEmbedWidget) {
    console.error('[ICOPAY] embed-checkout: IcopayEmbedWidget not loaded');
    return;
  }

  window.IcopayEmbedWidget.fetchSessionAndMount({
    cfg: cfg,
    sessionToken: sessionToken,
    mount: mount,
    sessionUrl: origin + '/api/middleware/v1/merchant/checkout/session?token=' + encodeURIComponent(sessionToken),
    pagePathForVendor: function () {
      return '/checkout/';
    },
    langCode: langCode,
    iframeTitle: 'ICOPAY secure checkout',
    eventName: 'icopay-checkout',
    globalCallbackName: 'onIcopayCheckout'
  });
})();
