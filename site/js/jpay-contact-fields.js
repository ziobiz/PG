/**
 * JPAY URL 결제창 — 국가코드(ISO2) + 전화번호 분리 (JPAY 필수).
 * window.PG_JPAY_CONTACT
 */
(function (global) {
  'use strict';

  var FALLBACK_ISO = 'JP';
  var PRIORITY_ISO = ['JP', 'KR', 'TH', 'US', 'CN', 'SG', 'HK'];
  var ISO2_LIST = [
    'AF', 'AL', 'DZ', 'AR', 'AU', 'AT', 'BD', 'BE', 'BR', 'BN', 'BG', 'KH', 'CA', 'CL', 'CN', 'CO', 'HR', 'CY', 'CZ', 'DK',
    'EG', 'FI', 'FR', 'DE', 'GR', 'HK', 'HU', 'IN', 'ID', 'IE', 'IL', 'IT', 'JP', 'JO', 'KZ', 'KE', 'KR', 'KW', 'LA', 'LU',
    'MO', 'MY', 'MX', 'MM', 'NL', 'NZ', 'NG', 'NO', 'PK', 'PH', 'PL', 'PT', 'QA', 'RO', 'RU', 'SA', 'RS', 'SG', 'SK', 'SI',
    'ZA', 'ES', 'LK', 'SE', 'CH', 'TW', 'TH', 'TR', 'AE', 'GB', 'US', 'VN'
  ];
  var PAY_LANG_TO_ISO = { KOR: 'KR', JPN: 'JP', ENG: 'US', CHN: 'CN', THA: 'TH' };
  var BROWSER_LANG_TO_ISO = { ko: 'KR', ja: 'JP', th: 'TH', zh: 'CN' };

  var userPickedCountry = false;

  function langTagFromPayLang(lang) {
    var m = { KOR: 'ko', ENG: 'en', JPN: 'ja', CHN: 'zh', THA: 'th' };
    return m[String(lang || '').toUpperCase()] || 'en';
  }

  function canonicalIso2(raw) {
    if (!raw) return '';
    var u = String(raw).trim().toUpperCase();
    return u.length === 2 ? u : '';
  }

  function browserLanguageTags() {
    var list = [];
    try {
      if (navigator.languages && navigator.languages.length) {
        for (var i = 0; i < navigator.languages.length; i++) list.push(navigator.languages[i]);
      }
    } catch (e) { /* ignore */ }
    try { if (navigator.language) list.push(navigator.language); } catch (e2) { /* ignore */ }
    return list;
  }

  function detectBrowserCountryIso2() {
    var list = browserLanguageTags();
    for (var j = 0; j < list.length; j++) {
      var tag = String(list[j] || '');
      var m = tag.match(/^[a-zA-Z]{2,3}[-_]([a-zA-Z]{2})$/);
      if (m) return m[1].toUpperCase();
    }
    for (var k = 0; k < list.length; k++) {
      var primary = String(list[k] || '').split(/[-_]/)[0].toLowerCase();
      if (BROWSER_LANG_TO_ISO[primary]) return BROWSER_LANG_TO_ISO[primary];
    }
    return '';
  }

  function countryIsoFromPayLang(lang) {
    return PAY_LANG_TO_ISO[String(lang || '').toUpperCase()] || '';
  }

  function pickDefaultCountryIso2(ctx, lang) {
    if (ctx && ctx.visitorCountryIso2) {
      var v = canonicalIso2(ctx.visitorCountryIso2);
      if (v) return v;
    }
    if (ctx && ctx.defaultCountryIso2) {
      var d = canonicalIso2(ctx.defaultCountryIso2);
      if (d) return d;
    }
    var browser = detectBrowserCountryIso2();
    if (browser) return browser;
    var fromLang = countryIsoFromPayLang(lang);
    if (fromLang) return fromLang;
    return FALLBACK_ISO;
  }

  function stripDialPrefix(raw) {
    var s = (raw == null) ? '' : String(raw).trim();
    if (!s) return '';
    var m = s.match(/^(\+\d{1,4})[\s\-]*(.*)$/);
    if (m) return (m[2] || '').trim();
    return s;
  }

  function buildCountryOptionsHtml(lang, selectedIso, selectLabel) {
    var tag = langTagFromPayLang(lang);
    var regionName = null;
    try { regionName = new Intl.DisplayNames([tag], { type: 'region' }); } catch (e) { /* ignore */ }
    var sel = canonicalIso2(selectedIso);
    var ph = (selectLabel != null && String(selectLabel).trim()) ? String(selectLabel).trim() : '—';
    var parts = ['<option value="" disabled' + (sel ? '' : ' selected') + '>' + ph + '</option>'];
    function pushOption(iso) {
      if (ISO2_LIST.indexOf(iso) < 0) return;
      var label = (regionName && regionName.of(iso)) || iso;
      parts.push('<option value="' + iso + '"' + (sel === iso ? ' selected' : '') + '>' + iso + ' — ' + label + '</option>');
    }
    PRIORITY_ISO.forEach(pushOption);
    parts.push('<option value="" disabled>---------------</option>');
    ISO2_LIST.filter(function (iso) { return PRIORITY_ISO.indexOf(iso) < 0; })
      .sort(function (a, b) {
        var la = (regionName && regionName.of(a)) || a;
        var lb = (regionName && regionName.of(b)) || b;
        return String(la).localeCompare(String(lb), tag);
      })
      .forEach(pushOption);
    return parts.join('');
  }

  function syncBillingCountryHidden(form) {
    if (!form) return;
    var cc = form.querySelector('#payContactCountryCode');
    var hidden = form.querySelector('#payCountryIsoCode2');
    if (cc && hidden) hidden.value = canonicalIso2(cc.value);
  }

  function syncPhoneLocalOnly(form) {
    if (!form) return;
    var tel = form.querySelector('#payTelephone');
    if (!tel) return;
    tel.value = stripDialPrefix(tel.value);
  }

  function syncBeforeSubmit(form) {
    syncBillingCountryHidden(form);
    syncPhoneLocalOnly(form);
  }

  function isCountryFieldLocked(form) {
    var ccSel = form && form.querySelector('#payContactCountryCode');
    return !!(ccSel && (ccSel.disabled || ccSel.readOnly));
  }

  function setCountrySelect(form, iso2, lang, selectLabel) {
    var sel = form.querySelector('#payContactCountryCode');
    if (!sel) return;
    var iso = canonicalIso2(iso2);
    var prev = sel.value;
    sel.innerHTML = buildCountryOptionsHtml(lang, iso || prev, selectLabel);
    if (iso && sel.querySelector('option[value="' + iso + '"]')) sel.value = iso;
    else if (prev && sel.querySelector('option[value="' + prev + '"]')) sel.value = prev;
    syncBillingCountryHidden(form);
  }

  function syncCountryToPayLang(form, lang, selectLabel, opts) {
    if (!form || isCountryFieldLocked(form)) return;
    opts = opts || {};
    if (opts.onlyIfNotUserPicked && userPickedCountry) return;
    var iso = countryIsoFromPayLang(lang);
    if (!iso) return;
    setCountrySelect(form, iso, lang, selectLabel);
  }

  function init(form, ctx, lang, selectLabel) {
    if (!form) return;
    var ccSel = form.querySelector('#payContactCountryCode');
    if (!ccSel) return;
    userPickedCountry = false;
    var defaultIso = pickDefaultCountryIso2(ctx || {}, lang);
    var hiddenEl = form.querySelector('#payCountryIsoCode2');
    var existing = canonicalIso2(ccSel.value)
      || canonicalIso2(hiddenEl && hiddenEl.value);
    setCountrySelect(form, existing || defaultIso, lang, selectLabel);
    if (!ccSel._jpayContactBound) {
      ccSel._jpayContactBound = true;
      ccSel.addEventListener('change', function () {
        userPickedCountry = true;
        syncBillingCountryHidden(form);
      });
      var tel = form.querySelector('#payTelephone');
      if (tel) {
        tel.addEventListener('blur', function () { syncPhoneLocalOnly(form); });
      }
    }
  }

  function applyPrefill(form, ctx, prefill, lang, selectLabel) {
    if (!form || !prefill) return;
    var p = typeof prefill === 'object' ? prefill : {};
    var iso = canonicalIso2(p.countryIso2 || p.payCountryIsoCode2 || p.country);
    if (!iso) iso = pickDefaultCountryIso2(ctx, lang);
    userPickedCountry = !!iso;
    setCountrySelect(form, iso, lang, selectLabel);
    var tel = form.querySelector('#payTelephone');
    if (tel) {
      var raw = p.phone || p.payTelephone || p.telephone || '';
      if (raw) tel.value = stripDialPrefix(raw);
    }
    var em = form.querySelector('#payEmailAddress');
    if (em && (p.email || p.payEmailAddress)) em.value = String(p.email || p.payEmailAddress).trim();
    syncBeforeSubmit(form);
  }

  function refreshCountryLabels(form, lang, selectLabel) {
    if (!form) return;
    var ccSel = form.querySelector('#payContactCountryCode');
    if (!ccSel) return;
    setCountrySelect(form, ccSel.value, lang, selectLabel);
  }

  function readCountryIso2(form) {
    if (!form) return '';
    var ccSel = form.querySelector('#payContactCountryCode');
    if (ccSel && ccSel.value) return canonicalIso2(ccSel.value);
    var hidden = form.querySelector('#payCountryIsoCode2');
    return hidden ? canonicalIso2(hidden.value) : '';
  }

  global.PG_JPAY_CONTACT = {
    init: init,
    applyPrefill: applyPrefill,
    syncBeforeSubmit: syncBeforeSubmit,
    refreshCountryLabels: refreshCountryLabels,
    syncCountryToPayLang: syncCountryToPayLang,
    stripDialPrefix: stripDialPrefix,
    pickDefaultCountryIso2: pickDefaultCountryIso2,
    countryIsoFromPayLang: countryIsoFromPayLang,
    readCountryIso2: readCountryIso2,
    buildCountryOptionsHtml: buildCountryOptionsHtml
  };
})(typeof window !== 'undefined' ? window : global);
