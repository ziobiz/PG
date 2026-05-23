/**
 * ICOPAY ChillPay 인라인 결제 iframe 위젯 (가맹점 외부 사이트 삽입용).
 * 부트스트랩: /v1/embed-pay/{compId} 가 window.__ICOPAY_EMBED_PAY__ 를 설정한 뒤 이 파일을 로드합니다.
 *
 * 사용:
 * 1) 가맹점 서버에서 POST .../inline-checkout/prepare → sessionToken
 * 2) <div id="icopay-pay-checkout"></div>
 *    <script src="https://{BASE}/v1/embed-pay/{compId}"
 *            data-session-token="{sessionToken}"
 *            data-target="icopay-pay-checkout"></script>
 */
(function () {
  'use strict';
  var cfg = window.__ICOPAY_EMBED_PAY__;
  if (!cfg || !cfg.compId || !cfg.origin) {
    return;
  }
  var scriptEl = cfg.script || document.currentScript;
  if (!scriptEl) {
    return;
  }
  var sessionToken = String(scriptEl.getAttribute('data-session-token') || '').trim();
  if (!sessionToken) {
    console.error('[ICOPAY] embed-pay: data-session-token is required (call inline-checkout/prepare first)');
    return;
  }
  var targetId = String(scriptEl.getAttribute('data-target') || 'icopay-pay-checkout').trim();
  var mount = document.getElementById(targetId);
  if (!mount) {
    mount = document.createElement('div');
    mount.id = targetId;
    scriptEl.parentNode.insertBefore(mount, scriptEl.nextSibling);
  }
  if (mount.getAttribute('data-icopay-pay-mounted') === '1') {
    return;
  }
  mount.setAttribute('data-icopay-pay-mounted', '1');
  mount.style.cssText = 'width:100%;max-width:560px;min-height:640px;margin:0 auto;';

  var compEnc = encodeURIComponent(String(cfg.compId).trim());
  var langCode = '';
  try {
    if (window.IcopayCheckoutLang && typeof window.IcopayCheckoutLang.resolveFromScript === 'function') {
      langCode = window.IcopayCheckoutLang.resolveFromScript(scriptEl);
    }
  } catch (eLang) { /* ignore */ }
  var frameSrc = String(cfg.origin).replace(/\/$/, '')
      + '/pay/' + compEnc
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

  function onPayMessage(ev) {
    if (!ev || !ev.data || typeof ev.data !== 'object') {
      return;
    }
    if (ev.data.type !== 'ICOPAY_INLINE_CHECKOUT') {
      return;
    }
    try {
      var allowed = String(cfg.origin).replace(/\/$/, '');
      if (ev.origin !== allowed) {
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
      try {
        window.onIcopayCheckout(detail);
      } catch (eCb) { /* ignore */ }
    }
  }
  window.addEventListener('message', onPayMessage, false);
})();
