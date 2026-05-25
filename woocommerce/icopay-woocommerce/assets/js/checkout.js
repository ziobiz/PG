(function ($) {
  'use strict';

  var cfg = window.icopayWcCheckout || {};
  var statusEl = document.getElementById('icopay-wc-status');

  function showStatus(msg, isError) {
    if (!statusEl) return;
    statusEl.hidden = false;
    statusEl.textContent = msg || '';
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
      showStatus(cfg.messages && cfg.messages.processing ? cfg.messages.processing : 'Processing…', false);
      return;
    }

    if (phase === 'finished') {
      if (detail.success) {
        showStatus(cfg.messages && cfg.messages.processing ? cfg.messages.processing : 'Confirming…', false);
        pollPaid(function (ok) {
          if (!ok && cfg.returnUrl) {
            window.location.href = cfg.returnUrl;
          }
        });
      } else {
        showStatus(cfg.messages && cfg.messages.failed ? cfg.messages.failed : 'Payment failed.', true);
      }
    }
  }, false);
})(jQuery);
