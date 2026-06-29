/**
 * ICOPAY JPAY 인라인 결제 iframe 위젯.
 */
(function () {
  'use strict';
  var cfg = window.__ICOPAY_EMBED_JPAY_PAY__;
  if (!cfg || !cfg.compId || !cfg.origin) {
    return;
  }
  var scriptEl = cfg.script || document.currentScript;
  if (!scriptEl) {
    return;
  }
  var sessionToken = String(scriptEl.getAttribute('data-session-token') || '').trim();
  if (!sessionToken) {
    console.error('[ICOPAY] embed-jpay-pay: data-session-token is required');
    return;
  }
  var targetId = String(scriptEl.getAttribute('data-target') || 'icopay-jpay-checkout').trim();
  var mount = document.getElementById(targetId);
  if (!mount) {
    mount = document.createElement('div');
    mount.id = targetId;
    scriptEl.parentNode.insertBefore(mount, scriptEl.nextSibling);
  }
  if (mount.getAttribute('data-icopay-jpay-mounted') === '1') {
    return;
  }
  mount.setAttribute('data-icopay-jpay-mounted', '1');
  mount.style.cssText = 'width:100%;max-width:560px;min-height:720px;margin:0 auto;';

  var origin = String(cfg.origin).replace(/\/$/, '');
  var langCode = '';
  try {
    if (window.IcopayCheckoutLang && typeof window.IcopayCheckoutLang.resolveFromScript === 'function') {
      langCode = window.IcopayCheckoutLang.resolveFromScript(scriptEl);
    }
  } catch (eLang) { /* ignore */ }

  if (!window.IcopayEmbedWidget) {
    console.error('[ICOPAY] embed-jpay-pay: IcopayEmbedWidget not loaded');
    return;
  }

  window.IcopayEmbedWidget.fetchSessionAndMount({
    cfg: cfg,
    sessionToken: sessionToken,
    mount: mount,
    sessionUrl: origin + '/api/middleware/v1/merchant/checkout/session?token=' + encodeURIComponent(sessionToken),
    pagePathForVendor: function () { return '/jpay-pay/'; },
    langCode: langCode,
    iframeTitle: 'ICOPAY JPAY checkout',
    eventName: 'icopay-jpay-checkout',
    globalCallbackName: 'onIcopayJpayCheckout',
    detailPatcher: function (d) { d.pgVendor = d.pgVendor || 'JPAY'; }
  });
})();
