(function ($) {
  'use strict';

  var cfg = window.icopayWcCheckout || {};
  var statusEl = document.getElementById('icopay-wc-status');
  var C3 = window.IcopayCheckout3ds;

  function msg(key, fallback) {
    return (cfg.messages && cfg.messages[key]) ? cfg.messages[key] : fallback;
  }

  function showStatus(text, isError) {
    if (!statusEl) return;
    statusEl.hidden = false;
    statusEl.textContent = text || '';
    statusEl.className = 'icopay-wc-status' + (isError ? ' icopay-wc-status--error' : ' icopay-wc-status--info');
  }

  function pollPaid(callback) {
    var attempts = 0;
    var maxAttempts = 12;

    function tick() {
      attempts += 1;
      $.post(cfg.ajaxUrl, {
        action: 'icopay_confirm_status',
        order_id: cfg.orderId,
        order_key: cfg.orderKey,
        nonce: cfg.nonce
      })
        .done(function (res) {
          if (res && res.success && res.data && res.data.paid && res.data.redirect) {
            window.location.href = res.data.redirect;
            return;
          }
          if (attempts >= maxAttempts) {
            if (callback) callback(false);
            return;
          }
          setTimeout(tick, 1500);
        })
        .fail(function () {
          if (attempts >= maxAttempts) {
            if (callback) callback(false);
            return;
          }
          setTimeout(tick, 1500);
        });
    }

    tick();
  }

  window.addEventListener('message', function (ev) {
    if (!ev || !ev.data || ev.data.type !== 'ICOPAY_INLINE_CHECKOUT') {
      return;
    }
    if (cfg.allowedOrigin && ev.origin !== cfg.allowedOrigin) {
      return;
    }

    var detail = ev.data.detail || {};
    var phase = detail.phase || '';

    if (phase === 'wait_authorize') {
      var payUrl = detail.paymentUrl || detail.redirectUrl;
      showStatus(msg('wait3ds', msg('processing', 'Processing…')), false);
      if (payUrl) {
        if (C3 && C3.navigateToPaymentUrl) {
          C3.navigateToPaymentUrl(payUrl, { embed: true, waitMessage: msg('wait3ds', '') });
        } else {
          try {
            (window.top || window).location.href = payUrl;
          } catch (eTop) {
            window.location.href = payUrl;
          }
        }
      }
      return;
    }

    if (phase === 'finished') {
      if (detail.success) {
        showStatus(msg('processing', 'Confirming…'), false);
        pollPaid(function (ok) {
          if (!ok && cfg.returnUrl) {
            window.location.href = cfg.returnUrl;
          }
        });
      } else {
        showStatus(msg('failed', 'Payment failed.'), true);
      }
    }
  }, false);
})(jQuery);
