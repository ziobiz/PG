/**
 * ICOPAY JPAY 구독 인라인 iframe 위젯.
 */
(function () {
  'use strict';
  var cfg = window.__ICOPAY_EMBED_JPAY_SUB__;
  if (!cfg || !cfg.compId || !cfg.origin) {
    return;
  }
  var scriptEl = cfg.script || document.currentScript;
  if (!scriptEl) {
    return;
  }
  var sessionToken = String(scriptEl.getAttribute('data-session-token') || '').trim();
  if (!sessionToken) {
    console.error('[ICOPAY] embed-jpay-subscribe: data-session-token is required');
    return;
  }
  var targetId = String(scriptEl.getAttribute('data-target') || 'icopay-jpay-subscribe').trim();
  var mount = document.getElementById(targetId);
  if (!mount) {
    mount = document.createElement('div');
    mount.id = targetId;
    scriptEl.parentNode.insertBefore(mount, scriptEl.nextSibling);
  }
  if (mount.getAttribute('data-icopay-jpay-sub-mounted') === '1') {
    return;
  }
  mount.setAttribute('data-icopay-jpay-sub-mounted', '1');
  mount.style.cssText = 'width:100%;max-width:560px;min-height:720px;margin:0 auto;';

  var compEnc = encodeURIComponent(String(cfg.compId).trim());
  var langCode = '';
  try {
    if (window.IcopayCheckoutLang && typeof window.IcopayCheckoutLang.resolveFromScript === 'function') {
      langCode = window.IcopayCheckoutLang.resolveFromScript(scriptEl);
    }
  } catch (eLang) { /* ignore */ }
  var frameSrc = String(cfg.origin).replace(/\/$/, '')
      + '/jpay-subscribe/' + compEnc
      + '?entry=merchant_api&embed=1'
      + '&session=' + encodeURIComponent(sessionToken);
  if (langCode) {
    frameSrc += '&lang=' + encodeURIComponent(langCode);
  }

  var iframe = document.createElement('iframe');
  iframe.title = 'ICOPAY JPAY subscription';
  iframe.setAttribute('referrerpolicy', 'strict-origin-when-cross-origin');
  iframe.setAttribute('allow', 'payment *');
  iframe.style.cssText = 'display:block;width:100%;min-height:720px;height:100%;border:0;background:#fff;border-radius:12px;';
  iframe.src = frameSrc;
  mount.appendChild(iframe);

  window.addEventListener('message', function (ev) {
    if (!ev || !ev.data || ev.data.type !== 'ICOPAY_INLINE_CHECKOUT') return;
    try {
      if (ev.origin !== String(cfg.origin).replace(/\/$/, '')) return;
    } catch (eO) { return; }
    var detail = ev.data.detail || {};
    detail.pgVendor = detail.pgVendor || 'JPAY';
    detail.checkoutKind = detail.checkoutKind || 'SUBSCRIPTION';
    try {
      mount.dispatchEvent(new CustomEvent('icopay-jpay-subscribe', { detail: detail, bubbles: true }));
    } catch (eEv) { /* ignore */ }
    if (typeof window.onIcopayJpaySubscribe === 'function') {
      try { window.onIcopayJpaySubscribe(detail); } catch (eCb) { /* ignore */ }
    }
  }, false);
})();
