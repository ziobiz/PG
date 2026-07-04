/**
 * ICOPAY 통합 인라인 결제 iframe 위젯.
 * 실제 결제 대행사는 노출하지 않는다 — 항상 중립 결제창(/checkout/{compId})으로 iframe 을 띄우고,
 * 서버가 운영 PG를 판별해 실제 결제 페이지로 내부 forward 한다.
 */
(function () {
  'use strict';
  var cfg = window.__ICOPAY_EMBED_CHECKOUT__;
  if (!cfg || !cfg.compId || !cfg.origin) {
    return;
  }
  var scriptEl = cfg.script || document.currentScript;
  if (!scriptEl) {
    return;
  }
  var sessionToken = String(scriptEl.getAttribute('data-session-token') || '').trim();
  if (!sessionToken) {
    console.error('[ICOPAY] embed-checkout: data-session-token is required (call checkout/prepare first)');
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
      // PG 무관 중립 경로 — 실제 결제 대행사는 서버 forward 로 숨긴다.
      return '/checkout/';
    },
    langCode: langCode,
    eventName: 'icopay-checkout',
    globalCallbackName: 'onIcopayCheckout'
  });
})();
