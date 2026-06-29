/**
 * ICOPAY embed iframe widgets — session fetch, mobile payUrl redirect, postMessage parent handler.
 */
(function (global) {
  'use strict';

  var C3 = global.IcopayCheckout3ds;

  function trimOrigin(o) {
    return String(o || '').replace(/\/$/, '');
  }

  function buildFrameSrc(origin, pagePath, compId, sessionToken, langCode) {
    var src = trimOrigin(origin) + pagePath + encodeURIComponent(String(compId).trim())
        + '?entry=merchant_api&embed=1'
        + '&session=' + encodeURIComponent(String(sessionToken).trim());
    if (langCode) {
      src += '&lang=' + encodeURIComponent(langCode);
    }
    return src;
  }

  function mountIframe(mount, frameSrc, title) {
    var iframe = document.createElement('iframe');
    iframe.title = title || 'ICOPAY secure checkout';
    iframe.setAttribute('referrerpolicy', 'strict-origin-when-cross-origin');
    iframe.setAttribute('allow', 'payment *');
    iframe.style.cssText = 'display:block;width:100%;min-height:640px;height:100%;border:0;background:#fff;border-radius:12px;';
    iframe.src = frameSrc;
    mount.appendChild(iframe);
    return iframe;
  }

  function maybeRedirectToPayUrl(sessionData, origin) {
    if (!C3 || !sessionData) return false;
    var mode = sessionData.mobileCheckoutModeEffective || sessionData.mobileCheckoutMode || C3.MODES.EMBED;
    if (!C3.shouldFullPagePayUrl(mode)) return false;
    var payUrl = sessionData.payUrl;
    if (!payUrl) return false;
    try {
      var target = global.top || global;
      target.location.href = payUrl;
    } catch (eR) {
      global.location.href = payUrl;
    }
    return true;
  }

  function dispatchCheckoutEvent(mount, eventName, detail) {
    try {
      mount.dispatchEvent(new CustomEvent(eventName, { detail: detail, bubbles: true }));
    } catch (eEv) { /* ignore */ }
  }

  function bindStandardMessageHandler(cfg, mount, eventName, globalCallbackName, detailPatcher) {
    var origin = trimOrigin(cfg.origin);
    global.addEventListener('message', function (ev) {
      if (!ev || !ev.data || ev.data.type !== 'ICOPAY_INLINE_CHECKOUT') return;
      try {
        if (ev.origin !== origin) return;
      } catch (eO) { return; }
      var detail = ev.data.detail || {};
      if (typeof detailPatcher === 'function') detailPatcher(detail);
      if (C3 && C3.handleInlineCheckoutMessage(detail, {
        embed: true,
        statusUrl: cfg.statusUrl || null,
        onFinished: cfg.onFinished
      })) {
        /* handled wait_authorize navigation */
      }
      dispatchCheckoutEvent(mount, eventName, detail);
      if (globalCallbackName && typeof global[globalCallbackName] === 'function') {
        try { global[globalCallbackName](detail); } catch (eCb) { /* ignore */ }
      }
    }, false);
  }

  function fetchSessionAndMount(opts) {
    opts = opts || {};
    var cfg = opts.cfg;
    var sessionToken = opts.sessionToken;
    var mount = opts.mount;
    var sessionUrl = opts.sessionUrl;
    var pagePathForVendor = opts.pagePathForVendor;
    var langCode = opts.langCode || '';
    var eventName = opts.eventName || 'icopay-checkout';
    var globalCallbackName = opts.globalCallbackName || 'onIcopayCheckout';
    var iframeTitle = opts.iframeTitle || 'ICOPAY secure checkout';
    var detailPatcher = opts.detailPatcher;

    if (!sessionUrl) {
      var vendor = 'CHILLPAY';
      mountIframe(mount, buildFrameSrc(cfg.origin, pagePathForVendor(vendor), cfg.compId, sessionToken, langCode), iframeTitle);
      bindStandardMessageHandler(cfg, mount, eventName, globalCallbackName, detailPatcher);
      return;
    }

    fetch(sessionUrl, { credentials: 'omit', headers: { Accept: 'application/json' } })
      .then(function (r) { return r.json(); })
      .then(function (res) {
        if (!res || !res.success || !res.data) {
          throw new Error((res && res.message) || (C3 ? C3.t('sessionInvalid') : 'invalid session'));
        }
        if (maybeRedirectToPayUrl(res.data, cfg.origin)) {
          return;
        }
        var vendor = String(res.data.pgVendor || 'CHILLPAY').toUpperCase();
        var frameSrc = buildFrameSrc(cfg.origin, pagePathForVendor(vendor), cfg.compId, sessionToken, langCode);
        mountIframe(mount, frameSrc, iframeTitle);
        bindStandardMessageHandler(cfg, mount, eventName, globalCallbackName, detailPatcher);
      })
      .catch(function (err) {
        console.error('[ICOPAY] embed session failed:', err);
        var msg = C3 ? C3.t('sessionInvalid') : 'ICOPAY checkout session invalid.';
        mount.innerHTML = '<p style="color:#c00;font:14px sans-serif;padding:12px;">' + msg + '</p>';
      });
  }

  global.IcopayEmbedWidget = {
    fetchSessionAndMount: fetchSessionAndMount,
    bindStandardMessageHandler: bindStandardMessageHandler,
    buildFrameSrc: buildFrameSrc,
    mountIframe: mountIframe
  };
})(typeof window !== 'undefined' ? window : this);
