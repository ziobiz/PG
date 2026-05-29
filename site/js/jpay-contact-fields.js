/**
 * JPAY URL 결제창 — 국가코드(ISO2) + 전화번호 분리 (JPAY 필수).
 * window.PG_JPAY_CONTACT
 */
(function (global) {
  'use strict';

  var PRIORITY_ISO = ['JP', 'KR', 'TH', 'US', 'CN', 'SG', 'HK'];
  var ISO2_LIST = [
    'AF', 'AL', 'DZ', 'AR', 'AU', 'AT', 'BD', 'BE', 'BR', 'BN', 'BG', 'KH', 'CA', 'CL', 'CN', 'CO', 'HR', 'CY', 'CZ', 'DK',
    'EG', 'FI', 'FR', 'DE', 'GR', 'HK', 'HU', 'IN', 'ID', 'IE', 'IL', 'IT', 'JP', 'JO', 'KZ', 'KE', 'KR', 'KW', 'LA', 'LU',
    'MO', 'MY', 'MX', 'MM', 'NL', 'NZ', 'NG', 'NO', 'PK', 'PH', 'PL', 'PT', 'QA', 'RO', 'RU', 'SA', 'RS', 'SG', 'SK', 'SI',
    'ZA', 'ES', 'LK', 'SE', 'CH', 'TW', 'TH', 'TR', 'AE', 'GB', 'US', 'VN'
  ];

  function langTagFromPayLang(lang) {
    var m = { KOR: 'ko', ENG: 'en', JPN: 'ja', CHN: 'zh', THA: 'th' };
    return m[String(lang || '').toUpperCase()] || 'en';
  }

  function canonicalIso2(raw) {
    if (!raw) return '';
    var u = String(raw).trim().toUpperCase();
    return u.length === 2 ? u : '';
  }

  function detectBrowserCountryIso2() {
    var list = [];
    try {
      if (navigator.languages && navigator.languages.length) {
        for (var i = 0; i < navigator.languages.length; i++) list.push(navigator.languages[i]);
      }
    } catch (e) { /* ignore */ }
    try { if (navigator.language) list.push(navigator.language); } catch (e2) { /* ignore */ }
    for (var j = 0; j < list.length; j++) {
      var tag = String(list[j] || '');
      var m = tag.match(/^[a-zA-Z]{2,3}[-_]([a-zA-Z]{2})$/);
      if (m) return m[1].toUpperCase();
    }
    return '';
  }

  function pickDefaultCountryIso2(ctx) {
    if (!ctx) return detectBrowserCountryIso2() || 'KR';
    if (ctx.visitorCountryIso2) {
      var v = canonicalIso2(ctx.visitorCountryIso2);
      if (v) return v;
    }
    if (ctx.defaultCountryIso2) {
      var d = canonicalIso2(ctx.defaultCountryIso2);
      if (d) return d;
    }
    return detectBrowserCountryIso2() || 'KR';
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

  function init(form, ctx, lang, selectLabel) {
    if (!form) return;
    var ccSel = form.querySelector('#payContactCountryCode');
    if (!ccSel) return;
    var defaultIso = pickDefaultCountryIso2(ctx || {});
    var hiddenEl = form.querySelector('#payCountryIsoCode2');
    var existing = canonicalIso2(ccSel.value)
      || canonicalIso2(hiddenEl && hiddenEl.value);
    setCountrySelect(form, existing || defaultIso, lang, selectLabel);
    if (!ccSel._jpayContactBound) {
      ccSel._jpayContactBound = true;
      ccSel.addEventListener('change', function () { syncBillingCountryHidden(form); });
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
    if (!iso) iso = pickDefaultCountryIso2(ctx);
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
    stripDialPrefix: stripDialPrefix,
    pickDefaultCountryIso2: pickDefaultCountryIso2,
    readCountryIso2: readCountryIso2,
    buildCountryOptionsHtml: buildCountryOptionsHtml
  };
})(typeof window !== 'undefined' ? window : global);
