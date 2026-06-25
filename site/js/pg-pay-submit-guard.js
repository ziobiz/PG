/**
 * Public checkout double-submit guard — only the first PG request is sent.
 * Subsequent clicks show a localized "payment in progress" warning.
 */
(function (global) {
  'use strict';

  var FALLBACK = {
    KOR: '이미 결제 중입니다. 잠시만 기다려 주세요.',
    ENG: 'Payment is already in progress. Please wait.',
    JPN: '決済処理中です。しばらくお待ちください。',
    CHN: '支付正在进行中，请稍候。',
    THA: 'กำลังชำระเงินอยู่แล้ว กรุณารอสักครู่'
  };

  var locked = false;

  function normalizeLang(lang) {
    var L = String(lang || 'ENG').trim().toUpperCase();
    if (L === 'KO' || L === 'KR') return 'KOR';
    if (L === 'EN') return 'ENG';
    if (L === 'JA' || L === 'JP') return 'JPN';
    if (L === 'ZH' || L === 'CN') return 'CHN';
    if (L === 'TH') return 'THA';
    if (FALLBACK[L]) return L;
    return 'ENG';
  }

  function message(lang, tFn) {
    if (typeof tFn === 'function') {
      var fromT = tFn('payInProgress');
      if (fromT && fromT !== 'payInProgress') return fromT;
    }
    var L = normalizeLang(lang);
    return FALLBACK[L] || FALLBACK.ENG;
  }

  function setButtonBusy(btn, busy) {
    if (!btn) return;
    if (busy) {
      btn.disabled = true;
      btn.setAttribute('aria-busy', 'true');
    } else {
      btn.disabled = false;
      btn.removeAttribute('aria-busy');
    }
  }

  function warnDuplicate(opts) {
    opts = opts || {};
    var msg = message(opts.lang, opts.t);
    if (typeof opts.onWarn === 'function') opts.onWarn(msg);
    return msg;
  }

  function isLocked() {
    return locked;
  }

  /** @returns {boolean} true when this click may proceed */
  function tryLock(opts) {
    opts = opts || {};
    if (locked) {
      warnDuplicate(opts);
      return false;
    }
    locked = true;
    setButtonBusy(opts.button, true);
    return true;
  }

  function unlockForRetry(opts) {
    locked = false;
    setButtonBusy(opts && opts.button, false);
  }

  function unlockAfterFailure(opts) {
    unlockForRetry(opts);
  }

  global.PG_PAY_SUBMIT_GUARD = {
    isLocked: isLocked,
    tryLock: tryLock,
    unlockForRetry: unlockForRetry,
    unlockAfterFailure: unlockAfterFailure,
    message: message,
    warnDuplicate: warnDuplicate
  };
})(typeof window !== 'undefined' ? window : this);
