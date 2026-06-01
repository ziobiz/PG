/**
 * ICOPAY 통합 인라인 결제 iframe 위젯 — 운영 PG(ChillPay/JPAY) 자동 분기.
 * 부트스트랩: /v1/embed-checkout/{compId}
 *
 * 1) POST .../merchant/checkout/prepare → sessionToken
 * 2) <script src="https://{BASE}/v1/embed-checkout/{compId}" data-session-token="{token}"></script>
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
  var compEnc = encodeURIComponent(String(cfg.compId).trim());
  var langCode = '';
  try {
    if (window.IcopayCheckoutLang && typeof window.IcopayCheckoutLang.resolveFromScript === 'function') {
      langCode = window.IcopayCheckoutLang.resolveFromScript(scriptEl);
    }
  } catch (eLang) { /* ignore */ }

  function mountIframe(pgVendor) {
    var vendor = String(pgVendor || 'CHILLPAY').toUpperCase();
    var isJpay = vendor.indexOf('JPAY') === 0;
    var pagePath = isJpay ? '/jpay-pay/' : '/pay/';
    var frameSrc = origin + pagePath + compEnc
        + '?entry=merchant_api&embed=1'
        + '&session=' + encodeURIComponent(sessionToken);
    if (langCode) {
      frameSrc += '&lang=' + encodeURIComponent(langCode);
    }
    var iframe = document.createElement('iframe');
    iframe.title = 'ICOPAY secure checkout';
    iframe.setAttribute('referrerpolicy', 'strict-origin-when-cross-origin');
    iframe.setAttribute('allow', 'payment *');
    iframe.style.cssText = 'display:block;width:100%;min-height:640px;height:100%;border:0;background:#fff;border-radius:12px;';
    iframe.src = frameSrc;
    mount.appendChild(iframe);
  }

  fetch(origin + '/api/middleware/v1/merchant/checkout/session?token=' + encodeURIComponent(sessionToken), {
    credentials: 'omit',
    headers: { Accept: 'application/json' }
  })
    .then(function (r) { return r.json(); })
    .then(function (res) {
      if (!res || !res.success || !res.data) {
        throw new Error((res && res.message) || 'invalid session');
      }
      mountIframe(res.data.pgVendor);
    })
    .catch(function (err) {
      console.error('[ICOPAY] embed-checkout session failed:', err);
      mount.innerHTML = '<p style="color:#c00;font:14px sans-serif;padding:12px;">ICOPAY checkout session invalid.</p>';
    });

  function onPayMessage(ev) {
    if (!ev || !ev.data || typeof ev.data !== 'object') {
      return;
    }
    if (ev.data.type !== 'ICOPAY_INLINE_CHECKOUT') {
      return;
    }
    try {
      if (ev.origin !== origin) {
        return;
      }
    } catch (eO) {
      return;
    }
    var detail = ev.data.detail || {};
    try {
      mount.dispatchEvent(new CustomEvent('icopay-checkout', { detail: detail, bubbles: true }));
    } catch (eEv) { /* ignore */ }
    if (typeof window.onIcopayCheckout === 'function') {
      window.onIcopayCheckout(detail);
    }
  }
  window.addEventListener('message', onPayMessage, false);
})();
