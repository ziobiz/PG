/**
 * PG별 카드 BIN·브랜드·블랙리스트 정책 (결제창).
 * checkout-context 의 cardPayPolicy 로 초기화합니다.
 */
(function (global) {
  'use strict';

  function digitsOnly(s) {
    var out = '';
    var x = String(s == null ? '' : s);
    for (var i = 0; i < x.length; i++) {
      var c = x.charAt(i);
      if (c >= '0' && c <= '9') out += c;
    }
    return out;
  }

  function detectBrand(pan) {
    if (!pan || pan.length < 2) return 'UNKNOWN';
    if (pan.indexOf('34') === 0 || pan.indexOf('37') === 0) return 'AMEX';
    if (pan.charAt(0) === '4') return 'VISA';
    if (pan.indexOf('35') === 0) return 'JCB';
    if (pan.indexOf('62') === 0) return 'UNIONPAY';
    if (pan.indexOf('60') === 0 || pan.indexOf('81') === 0) return 'UNIONPAY';
    var d0 = pan.charCodeAt(0) - 48;
    var d1 = pan.charCodeAt(1) - 48;
    if (d0 === 5 && d1 >= 1 && d1 <= 5) return 'MASTERCARD';
    if (d0 === 2 && d1 >= 2 && d1 <= 7) return 'MASTERCARD';
    return 'UNKNOWN';
  }

  function expectedLen(brand) {
    return brand === 'AMEX' ? 15 : 16;
  }

  function langKey(lang) {
    var u = String(lang || 'KO').toUpperCase();
    if (u === 'ENG' || u.indexOf('EN') === 0) return 'EN';
    if (u === 'JPN' || u.indexOf('JA') === 0) return 'JP';
    if (u === 'CHN' || u.indexOf('ZH') === 0) return 'CH';
    if (u === 'THA' || u.indexOf('TH') === 0) return 'TH';
    return 'KO';
  }

  function msg(policy, key, lang, arg) {
    var lk = langKey(lang);
    var bag = policy && policy.messages && policy.messages[key];
    if (bag && bag[lk]) {
      var s = bag[lk];
      if (arg != null) s = s.replace('{0}', String(arg));
      return s;
    }
    return key;
  }

  function validate(policy, panRaw, selectedBrand, lang) {
    if (!policy) return { valid: true };
    var pan = digitsOnly(panRaw);
    if (pan.length < 6) return { valid: true };
    var prefixes = policy.blockedPrefixes || [];
    var i;
    for (i = 0; i < prefixes.length; i++) {
      var p = String(prefixes[i] || '');
      if (p && pan.indexOf(p) === 0) {
        return { valid: false, message: msg(policy, 'BLOCKED_PREFIX', lang, p), errorCode: 'BLOCKED_PREFIX' };
      }
    }
    var pg = String(policy.pgVendor || '').toUpperCase();
    if (pg.indexOf('JPAY') === 0) {
      if (pan.indexOf('60') === 0 || pan.indexOf('81') === 0) {
        return { valid: false, message: msg(policy, 'UNION_60_81', lang), errorCode: 'UNION_60_81' };
      }
    }
    var detected = detectBrand(pan);
    var brand = selectedBrand && selectedBrand !== 'AUTO' ? selectedBrand : detected;
    if (brand === 'UNKNOWN' && selectedBrand && selectedBrand !== 'AUTO') brand = selectedBrand;
    var allowed = policy.allowedBrands || [];
    if (allowed.length && allowed.indexOf(brand) < 0 && brand !== 'UNKNOWN') {
      return { valid: false, message: msg(policy, 'BRAND_NOT_ALLOWED', lang, brand), errorCode: 'BRAND_NOT_ALLOWED' };
    }
    if (pg.indexOf('JPAY') === 0 && detected === 'UNIONPAY' && pan.indexOf('62') !== 0) {
      return { valid: false, message: msg(policy, 'UNION_NOT_62', lang), errorCode: 'UNION_NOT_62' };
    }
    var exp = expectedLen(brand === 'UNKNOWN' ? detected : brand);
    if (pan.length >= exp - 1 && pan.length !== exp && pan.length >= 14) {
      var k = brand === 'AMEX' || detected === 'AMEX' ? 'AMEX_LEN' : 'CARD_LEN';
      return { valid: false, message: msg(policy, k, lang, exp), errorCode: k };
    }
    return { valid: true, brand: brand, expectedLength: exp };
  }

  function formatPanInput(input, brand) {
    if (!input) return;
    var pan = digitsOnly(input.value);
    var max = expectedLen(brand && brand !== 'AUTO' && brand !== 'UNKNOWN' ? brand : detectBrand(pan));
    if (pan.length > max) pan = pan.substring(0, max);
    input.value = pan;
    input.maxLength = max + 4;
  }

  function brandOptionLabel(brand) {
    switch (String(brand || '').toUpperCase()) {
      case 'VISA': return 'Visa';
      case 'MASTERCARD': return 'Mastercard';
      case 'JCB': return 'JCB';
      case 'UNIONPAY': return 'UnionPay (62…)';
      case 'AMEX': return 'American Express';
      default: return brand;
    }
  }

  function syncBrandSelectOptions(brandSelect, policy, autoLabel) {
    if (!brandSelect || !policy) return;
    var cur = brandSelect.value || 'AUTO';
    var allowed = policy.allowedBrands || [];
    brandSelect.innerHTML = '';
    var autoOpt = document.createElement('option');
    autoOpt.value = 'AUTO';
    autoOpt.textContent = autoLabel || 'Auto detect';
    autoOpt.setAttribute('data-i18n', 'cardAutoDetectOption');
    brandSelect.appendChild(autoOpt);
    for (var i = 0; i < allowed.length; i++) {
      var b = String(allowed[i] || '').trim().toUpperCase();
      if (!b || b === 'AUTO') continue;
      var opt = document.createElement('option');
      opt.value = b;
      opt.textContent = brandOptionLabel(b);
      brandSelect.appendChild(opt);
    }
    var hasCur = false;
    for (var j = 0; j < brandSelect.options.length; j++) {
      if (brandSelect.options[j].value === cur) { hasCur = true; break; }
    }
    brandSelect.value = hasCur ? cur : 'AUTO';
  }

  function init(opts) {
    opts = opts || {};
    var policy = opts.policy;
    var panInput = opts.panInput;
    var brandSelect = opts.brandSelect;
    var alertEl = opts.alertEl;
    var cvvInput = opts.cvvInput;
    var lang = opts.lang || 'KO';
    var onLangChange = opts.onLangChange;

    if (!policy || !panInput) return;

    var autoLabel = opts.autoDetectLabel || 'Auto detect';
    if (brandSelect) {
      syncBrandSelectOptions(brandSelect, policy, autoLabel);
    }

    function showAlert(res) {
      if (!alertEl) return;
      if (!res || res.valid) {
        alertEl.classList.add('d-none');
        alertEl.textContent = '';
        return;
      }
      alertEl.textContent = res.message || '';
      alertEl.classList.remove('d-none');
    }

    function currentBrand() {
      if (brandSelect && brandSelect.value) return brandSelect.value;
      return 'AUTO';
    }

    function runValidate() {
      var res = validate(policy, panInput.value, currentBrand(), typeof onLangChange === 'function' ? onLangChange() : lang);
      showAlert(res);
      return res;
    }

    function applyBrandUi() {
      var b = currentBrand();
      if (b === 'AUTO') b = detectBrand(digitsOnly(panInput.value));
      formatPanInput(panInput, b);
      if (cvvInput) cvvInput.maxLength = (b === 'AMEX') ? 4 : 4;
      if (cvvInput) cvvInput.placeholder = (b === 'AMEX') ? '4' : '3';
    }

    function syncDetectedBrandToSelect() {
      if (!brandSelect) return;
      if (brandSelect.value && brandSelect.value !== 'AUTO') return;
      var d = detectBrand(digitsOnly(panInput.value));
      if (d === 'UNKNOWN') return;
      for (var oi = 0; oi < brandSelect.options.length; oi++) {
        if (brandSelect.options[oi].value === d) {
          brandSelect.value = d;
          return;
        }
      }
      brandSelect.value = 'AUTO';
    }

    if (brandSelect && policy.brandSelectEnabled) {
      var row = brandSelect.closest ? brandSelect.closest('#payCardBrandRow') : null;
      if (row && row.style.display !== 'none') row.style.display = '';
      brandSelect.addEventListener('change', function () {
        applyBrandUi();
        runValidate();
      });
    }

    panInput.addEventListener('input', function () {
      syncDetectedBrandToSelect();
      applyBrandUi();
      runValidate();
    });

    applyBrandUi();
    return {
      validate: runValidate,
      refreshAutoDetectLabel: function (label) {
        if (!brandSelect) return;
        syncBrandSelectOptions(brandSelect, policy, label || autoLabel);
        syncDetectedBrandToSelect();
        applyBrandUi();
      },
      validateFinal: function () {
        var pan = digitsOnly(panInput.value);
        var b = currentBrand();
        if (b === 'AUTO') b = detectBrand(pan);
        var exp = expectedLen(b === 'UNKNOWN' ? detectBrand(pan) : b);
        if (pan.length !== exp) {
          var k = (b === 'AMEX' || detectBrand(pan) === 'AMEX') ? 'AMEX_LEN' : 'CARD_LEN';
          return { valid: false, message: msg(policy, k, lang, exp), errorCode: k };
        }
        return validate(policy, pan, b, typeof onLangChange === 'function' ? onLangChange() : lang);
      }
    };
  }

  global.PG_CARD_PAY_POLICY = {
    init: init,
    validate: validate,
    detectBrand: detectBrand,
    digitsOnly: digitsOnly,
    syncBrandSelectOptions: syncBrandSelectOptions,
    brandOptionLabel: brandOptionLabel
  };
})(typeof window !== 'undefined' ? window : this);
