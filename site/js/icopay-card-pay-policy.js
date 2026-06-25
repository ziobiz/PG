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

  function applyMsgArgs(text, data) {
    if (!text) return text;
    var s = String(text);
    var d = data || {};
    if (d.remainingMinutes != null && s.indexOf('{0}') >= 0) {
      s = s.replace('{0}', String(d.remainingMinutes));
    }
    if (d.blockedPrefix != null && s.indexOf('{0}') >= 0) {
      s = s.replace('{0}', String(d.blockedPrefix));
    }
    if (d.arg0 != null && s.indexOf('{0}') >= 0) {
      s = s.replace('{0}', String(d.arg0));
    }
    return s;
  }

  /** 서버 검증 응답(flat messages) · checkout policy(nested messages) 공통 */
  function resolveMessage(policy, data, messageKey, lang, extras) {
    var lk = langKey(lang);
    var key = messageKey || (data && (data.messageKey || data.errorCode));
    var bag = data && data.messages;
    var text = null;
    if (bag) {
      if (bag[lk] && typeof bag[lk] === 'string') {
        text = bag[lk];
      } else if (key && bag[key] && typeof bag[key] === 'object') {
        text = bag[key][lk] || bag[key].KO;
      }
    }
    if (!text && policy && key) {
      text = msg(policy, key, lang, extras && extras.arg0 != null ? extras.arg0 : null);
      if (text === key) text = null;
    }
    if (!text && data && data.message) {
      text = data.message;
    }
    return applyMsgArgs(text || key || '', data || extras || {});
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
        return { valid: false, message: msg(policy, 'BLOCKED_PREFIX', lang, p), errorCode: 'BLOCKED_PREFIX', messageKey: 'BLOCKED_PREFIX', blockedPrefix: p };
      }
    }
    var pg = String(policy.pgVendor || '').toUpperCase();
    if (pg.indexOf('JPAY') === 0) {
      if (pan.indexOf('60') === 0 || pan.indexOf('81') === 0) {
        return { valid: false, message: msg(policy, 'UNION_60_81', lang), errorCode: 'UNION_60_81', messageKey: 'UNION_60_81' };
      }
    }
    var detected = detectBrand(pan);
    var brand = selectedBrand && selectedBrand !== 'AUTO' ? selectedBrand : detected;
    if (brand === 'UNKNOWN' && selectedBrand && selectedBrand !== 'AUTO') brand = selectedBrand;
    var allowed = policy.allowedBrands || [];
    if (allowed.length && allowed.indexOf(brand) < 0 && brand !== 'UNKNOWN') {
      return { valid: false, message: msg(policy, 'BRAND_NOT_ALLOWED', lang, brand), errorCode: 'BRAND_NOT_ALLOWED', messageKey: 'BRAND_NOT_ALLOWED', arg0: brand };
    }
    if (pg.indexOf('JPAY') === 0 && detected === 'UNIONPAY' && pan.indexOf('62') !== 0) {
      return { valid: false, message: msg(policy, 'UNION_NOT_62', lang), errorCode: 'UNION_NOT_62', messageKey: 'UNION_NOT_62' };
    }
    var exp = expectedLen(brand === 'UNKNOWN' ? detected : brand);
    if (pan.length >= exp - 1 && pan.length !== exp && pan.length >= 14) {
      var k = brand === 'AMEX' || detected === 'AMEX' ? 'AMEX_LEN' : 'CARD_LEN';
      return { valid: false, message: msg(policy, k, lang, exp), errorCode: k, messageKey: k, arg0: exp };
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

  function resolveLang(opts) {
    if (opts && typeof opts.onLangChange === 'function') return opts.onLangChange();
    return opts && opts.lang ? opts.lang : 'KO';
  }

  function postCardPolicyCheck(opts, pan, brand, lang) {
    var compId = typeof opts.getCompId === 'function' ? opts.getCompId() : opts.compId;
    if (!compId || pan.length < 10) return Promise.resolve(null);
    var url = opts.checkUrl || '/api/pay/url/card-policy-check';
    var base = opts.apiBase != null ? String(opts.apiBase) : '';
    if (base && url.indexOf('http') !== 0 && url.charAt(0) === '/') {
      url = base.replace(/\/$/, '') + url;
    }
    var body = JSON.stringify({
      compId: compId,
      pan: pan,
      cardBrand: brand && brand !== 'AUTO' ? brand : '',
      lang: lang
    });
    if (typeof opts.postJson === 'function') {
      return opts.postJson(url, body).then(function (r) { return r && r.data ? r.data : r; });
    }
    return fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      body: body
    }).then(function (res) { return res.json(); }).then(function (r) { return r && r.data ? r.data : r; });
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
    var serverTimer = null;
    var serverSeq = 0;
    var lastServerBlock = null;

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
      var curLang = resolveLang({ lang: lang, onLangChange: onLangChange });
      var text = res.message;
      if (res.messageKey || res.errorCode) {
        text = resolveMessage(policy, res, res.messageKey || res.errorCode, curLang, res);
      }
      alertEl.textContent = text || '';
      alertEl.classList.remove('d-none');
    }

    function currentBrand() {
      if (brandSelect && brandSelect.value) return brandSelect.value;
      return 'AUTO';
    }

    function runValidate() {
      var curLang = resolveLang({ lang: lang, onLangChange: onLangChange });
      var clientRes = validate(policy, panInput.value, currentBrand(), curLang);
      if (!clientRes.valid) {
        lastServerBlock = null;
        showAlert(clientRes);
        return clientRes;
      }
      if (lastServerBlock && lastServerBlock.valid === false) {
        showAlert(lastServerBlock);
        return lastServerBlock;
      }
      showAlert(clientRes);
      return clientRes;
    }

    function scheduleServerCheck() {
      var pan = digitsOnly(panInput.value);
      var curLang = resolveLang({ lang: lang, onLangChange: onLangChange });
      if (pan.length < 10 || !(typeof opts.getCompId === 'function' ? opts.getCompId() : opts.compId)) {
        lastServerBlock = null;
        return;
      }
      clearTimeout(serverTimer);
      serverTimer = setTimeout(function () {
        var seq = ++serverSeq;
        var brand = currentBrand();
        postCardPolicyCheck(opts, pan, brand, curLang).then(function (data) {
          if (seq !== serverSeq) return;
          if (!data) return;
          if (data.valid === false) {
            var mk = data.messageKey || data.errorCode;
            lastServerBlock = {
              valid: false,
              errorCode: data.errorCode || mk,
              messageKey: mk,
              messages: data.messages || null,
              remainingMinutes: data.remainingMinutes
            };
            showAlert(lastServerBlock);
          } else {
            lastServerBlock = null;
            runValidate();
          }
        }).catch(function () {});
      }, 400);
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
        scheduleServerCheck();
      });
    }

    panInput.addEventListener('input', function () {
      syncDetectedBrandToSelect();
      applyBrandUi();
      runValidate();
      scheduleServerCheck();
    });

    panInput.addEventListener('blur', function () {
      scheduleServerCheck();
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
      refreshLang: function () {
        runValidate();
      },
      validateFinal: function () {
        var pan = digitsOnly(panInput.value);
        var b = currentBrand();
        if (b === 'AUTO') b = detectBrand(pan);
        var exp = expectedLen(b === 'UNKNOWN' ? detectBrand(pan) : b);
        var curLang = resolveLang({ lang: lang, onLangChange: onLangChange });
        if (pan.length !== exp) {
          var k = (b === 'AMEX' || detectBrand(pan) === 'AMEX') ? 'AMEX_LEN' : 'CARD_LEN';
          return { valid: false, message: msg(policy, k, curLang, exp), errorCode: k, messageKey: k, arg0: exp };
        }
        if (lastServerBlock && lastServerBlock.valid === false) {
          return {
            valid: false,
            message: resolveMessage(policy, lastServerBlock, lastServerBlock.messageKey || lastServerBlock.errorCode, curLang, lastServerBlock),
            errorCode: lastServerBlock.errorCode,
            messageKey: lastServerBlock.messageKey,
            messages: lastServerBlock.messages,
            remainingMinutes: lastServerBlock.remainingMinutes
          };
        }
        return validate(policy, pan, b, curLang);
      },
      checkServerNow: function () {
        scheduleServerCheck();
        return postCardPolicyCheck(opts, digitsOnly(panInput.value), currentBrand(), resolveLang({ lang: lang, onLangChange: onLangChange }));
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
