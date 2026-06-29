/**
 * ICOPAY inline checkout iframe — postMessage handler (shared by PHP/JSP samples).
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
      var detail = ev.data.detail || {};
      if (global.IcopayCheckout3ds && global.IcopayCheckout3ds.handleInlineCheckoutMessage) {
        global.IcopayCheckout3ds.handleInlineCheckoutMessage(detail, { embed: true });
      }
      callback(detail, ev);
    }, false);
  }

  global.IcopayCheckout = {
    onMessage: onCheckoutMessage,
    lang: global.IcopayCheckoutLang || null,
    navigate3ds: function (url) {
      if (global.IcopayCheckout3ds && global.IcopayCheckout3ds.navigateToPaymentUrl) {
        global.IcopayCheckout3ds.navigateToPaymentUrl(url, { embed: true });
      } else {
        try { (global.top || global).location.href = url; } catch (e) { global.location.href = url; }
      }
    }
  };
})(typeof window !== 'undefined' ? window : this);
