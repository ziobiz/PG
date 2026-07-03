/**

 * 결제 입력창 — 필드별 입력 제한 + 잘못된 입력 시 빨간 인라인 안내.

 * window.PG_PAY_FORM_INPUT_GUARD

 */

(function (g) {

  'use strict';



  var EN_NAME_RE = /^[A-Za-z]+(?:[ '\-][A-Za-z]+)*$/;

  var EMAIL_RE = /^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$/;



  var _guardT = function (k) { return k; };



  function bindOnce(el, flag, fn) {

    if (!el || el[flag]) return;

    el[flag] = true;

    fn(el);

  }



  function ensureHintEl(input) {

    if (!input) return null;

    if (input._payGuardHintEl && input._payGuardHintEl.parentNode) return input._payGuardHintEl;

    var hint = g.document.createElement('div');

    hint.className = 'pay-field-guard-hint';

    hint.setAttribute('role', 'alert');

    hint.setAttribute('aria-live', 'polite');

    input._payGuardHintEl = hint;

    var parent = input.parentNode;

    if (parent) parent.appendChild(hint);

    return hint;

  }



  function showFieldHint(input, hintKey) {

    if (!input || !hintKey) return;

    var hint = ensureHintEl(input);

    if (!hint) return;

    hint.setAttribute('data-i18n-key', hintKey);

    hint.textContent = _guardT(hintKey);

    hint.classList.add('is-visible');

    input.classList.add('pay-field-guard-invalid');

    if (input._payGuardHintTimer) g.clearTimeout(input._payGuardHintTimer);

    input._payGuardHintTimer = g.setTimeout(function () {

      if (!input.classList.contains('pay-field-guard-invalid')) return;

      var key = hint.getAttribute('data-i18n-key');

      if (key) hint.textContent = _guardT(key);

    }, 0);

  }



  function hideFieldHint(input) {

    if (!input) return;

    if (input._payGuardHintTimer) {

      g.clearTimeout(input._payGuardHintTimer);

      input._payGuardHintTimer = null;

    }

    input.classList.remove('pay-field-guard-invalid');

    var hint = input._payGuardHintEl;

    if (hint) hint.classList.remove('is-visible');

  }



  function refreshHints(root) {

    root = root || g.document;

    var nodes = root.querySelectorAll ? root.querySelectorAll('.pay-field-guard-hint.is-visible') : [];

    nodes.forEach(function (hint) {

      var key = hint.getAttribute('data-i18n-key');

      if (key) hint.textContent = _guardT(key);

    });

  }



  function applySanitize(input, raw, sanitized, hintKey) {

    if (raw !== sanitized) {

      input.value = sanitized;

      showFieldHint(input, hintKey);

      return true;

    }

    hideFieldHint(input);

    return false;

  }



  function digitsFilter(el, maxLen, hintKey) {

    hintKey = hintKey || 'warnDigitsOnly';

    bindOnce(el, '_payGuardDigits', function (input) {

      function sanitize(v) {

        var s = String(v || '').replace(/\D/g, '');

        if (maxLen != null && maxLen > 0) s = s.substring(0, maxLen);

        return s;

      }

      input.addEventListener('input', function () {

        applySanitize(input, String(input.value || ''), sanitize(input.value), hintKey);

      });

      input.addEventListener('paste', function (e) {

        e.preventDefault();

        var clip = '';

        try { clip = (e.clipboardData || g.clipboardData).getData('text'); } catch (err) { /* ignore */ }

        var v = sanitize(clip);

        if (String(clip || '') !== v) showFieldHint(input, hintKey);

        else hideFieldHint(input);

        input.value = v;

      });

      input.addEventListener('blur', function () {

        if (sanitize(input.value) === String(input.value || '')) hideFieldHint(input);

      });

    });

  }



  function englishNameFilter(el) {

    bindOnce(el, '_payGuardEnName', function (input) {

      function sanitize(v) {

        return String(v || '').replace(/[^A-Za-z '\-]/g, '');

      }

      input.setAttribute('autocomplete', input.getAttribute('autocomplete') || 'given-name');

      input.addEventListener('input', function () {

        applySanitize(input, String(input.value || ''), sanitize(input.value), 'warnEnglishOnly');

      });

      input.addEventListener('paste', function (e) {

        e.preventDefault();

        var clip = '';

        try { clip = (e.clipboardData || g.clipboardData).getData('text'); } catch (err) { /* ignore */ }

        var v = sanitize(clip);

        if (String(clip || '') !== v) showFieldHint(input, 'warnEnglishOnly');

        else hideFieldHint(input);

        input.value = v;

      });

      input.addEventListener('blur', function () {

        if (sanitize(input.value) === String(input.value || '')) hideFieldHint(input);

      });

    });

  }



  function amountFilter(el) {

    bindOnce(el, '_payGuardAmount', function (input) {

      function sanitize(raw) {

        var v = String(raw || '').replace(/[^\d.]/g, '');

        var dot = v.indexOf('.');

        if (dot >= 0) {

          v = v.substring(0, dot + 1) + v.substring(dot + 1).replace(/\./g, '');

        }

        return v;

      }

      input.addEventListener('keydown', function (e) {

        if (e.key === 'e' || e.key === 'E' || e.key === '+' || e.key === '-') {

          e.preventDefault();

          showFieldHint(input, 'warnAmountOnly');

        }

      });

      input.addEventListener('input', function () {

        applySanitize(input, String(input.value || ''), sanitize(input.value), 'warnAmountOnly');

      });

      input.addEventListener('paste', function (e) {

        e.preventDefault();

        var clip = '';

        try { clip = (e.clipboardData || g.clipboardData).getData('text'); } catch (err) { /* ignore */ }

        var v = sanitize(clip);

        if (String(clip || '') !== v) showFieldHint(input, 'warnAmountOnly');

        else hideFieldHint(input);

        input.value = v;

      });

      input.addEventListener('blur', function () {

        if (sanitize(input.value) === String(input.value || '')) hideFieldHint(input);

      });

    });

  }



  function emailSanitizeFilter(el) {

    bindOnce(el, '_payGuardEmail', function (input) {

      function sanitize(v) {

        return String(v || '').replace(/[^a-zA-Z0-9._%+\-@]/g, '');

      }

      input.addEventListener('input', function () {

        applySanitize(input, String(input.value || ''), sanitize(input.value), 'warnEmailChars');

      });

      input.addEventListener('paste', function (e) {

        e.preventDefault();

        var clip = '';

        try { clip = (e.clipboardData || g.clipboardData).getData('text'); } catch (err) { /* ignore */ }

        var v = sanitize(clip);

        if (String(clip || '') !== v) showFieldHint(input, 'warnEmailChars');

        else hideFieldHint(input);

        input.value = v;

      });

      input.addEventListener('blur', function () {

        if (sanitize(input.value) === String(input.value || '')) hideFieldHint(input);

      });

    });

  }



  function validateEnglishName(val) {

    var s = val != null ? String(val).trim() : '';

    if (!s) return false;

    return EN_NAME_RE.test(s);

  }



  function validateEmail(val) {

    var s = val != null ? String(val).trim() : '';

    if (!s) return false;

    return EMAIL_RE.test(s);

  }



  function validatePhoneDigits(val) {

    var s = val != null ? String(val).trim() : '';

    if (!s) return false;

    return /^\d+$/.test(s);

  }



  function validateAmount(val) {

    var s = val != null ? String(val).replace(/,/g, '').trim() : '';

    if (!s || !/^\d+(\.\d+)?$/.test(s)) return false;

    return parseFloat(s) > 0;

  }



  function query(form, id) {

    if (!form) return g.document.getElementById(id);

    return form.querySelector('#' + id) || g.document.getElementById(id);

  }



  function configureGuard(opts) {

    opts = opts || {};

    if (typeof opts.t === 'function') _guardT = opts.t;

  }



  /** JPAY URL 결제창 (jpay-pay.html) */

  function initJpayForm(form, opts) {

    configureGuard(opts);

    form = form || g.document.getElementById('jpayForm');

    if (!form) return;

    var amt = query(form, 'amount');

    var fn = query(form, 'payFirstname');

    var ln = query(form, 'payLastname');

    var em = query(form, 'payEmailAddress');

    var tel = query(form, 'payTelephone');

    var cvv = query(form, 'payCardcvv');

    var mm = query(form, 'payCardmonth');

    var yy = query(form, 'payCardyear');

    if (amt) amountFilter(amt);

    if (fn) englishNameFilter(fn);

    if (ln) englishNameFilter(ln);

    if (em) emailSanitizeFilter(em);

    if (tel) digitsFilter(tel, 20, 'warnDigitsOnly');

    if (cvv) digitsFilter(cvv, 4, 'warnDigitsOnly');

    if (mm && mm.tagName === 'INPUT') digitsFilter(mm, 2, 'warnDigitsOnly');

    if (yy && yy.tagName === 'INPUT') digitsFilter(yy, 4, 'warnDigitsOnly');

  }



  /** URL 결제 (pay.html) — 카드는 iframe, 연락처·성명·금액만 */

  function initUrlPayForm(form, opts) {

    configureGuard(opts);

    form = form || g.document.getElementById('payForm');

    if (!form) return;

    var amt = query(form, 'amount');

    var fn = query(form, 'firstName');

    var ln = query(form, 'lastName');

    var em = query(form, 'custEmail');

    var tel = query(form, 'phoneNumber');

    if (amt) amountFilter(amt);

    if (fn) englishNameFilter(fn);

    if (ln) englishNameFilter(ln);

    if (em) emailSanitizeFilter(em);

    if (tel) digitsFilter(tel, 20, 'warnDigitsOnly');

  }



  /** jpay-subscribe 등 레거시 JPAY 폼 */

  function initLegacyJpayForm(form, opts) {

    initJpayForm(form, opts);

  }



  g.PG_PAY_FORM_INPUT_GUARD = {

    initJpayForm: initJpayForm,

    initUrlPayForm: initUrlPayForm,

    initLegacyJpayForm: initLegacyJpayForm,

    refreshHints: refreshHints,

    validateEnglishName: validateEnglishName,

    validateEmail: validateEmail,

    validatePhoneDigits: validatePhoneDigits,

    validateAmount: validateAmount,

    digitsFilter: digitsFilter,

    englishNameFilter: englishNameFilter,

    amountFilter: amountFilter

  };

})(typeof window !== 'undefined' ? window : this);


