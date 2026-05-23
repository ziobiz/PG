/**
 * ICOPAY 인라인 결제 iframe — postMessage 수신 (PHP/JSP 공통).
 * 사용: <script src="{publicApiBaseUrl}/merchant-api-samples/common/icopay-checkout.js"></script>
 */
(function (global) {
  'use strict';

  function onCheckoutMessage(callback, allowedOrigin) {
    if (typeof callback !== 'function') return;
    global.addEventListener('message', function (ev) {
      if (!ev || !ev.data || ev.data.type !== 'ICOPAY_INLINE_CHECKOUT') return;
      if (allowedOrigin) {
        try {
          var ok = String(allowedOrigin).replace(/\/$/, '');
          if (ev.origin !== ok) return;
        } catch (eO) { return; }
      }
      callback(ev.data.detail || {}, ev);
    }, false);
  }

  global.IcopayCheckout = {
    onMessage: onCheckoutMessage,
    lang: global.IcopayCheckoutLang || null
  };
})(typeof window !== 'undefined' ? window : this);
