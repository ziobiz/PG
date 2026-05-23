/**
 * ICOPAY 결제창 UI 언어 — pay.html / embed 위젯 공통 (KOR|ENG|JPN|CHN|THA).
 */
(function (global) {
  'use strict';

  var SUPPORTED = { KOR: 1, ENG: 1, JPN: 1, CHN: 1, THA: 1 };

  function mapBrowserLocale(tag) {
    if (!tag) return null;
    var s = String(tag).trim().toLowerCase();
    if (!s) return null;
    if (s.indexOf('ko') === 0) return 'KOR';
    if (s.indexOf('ja') === 0) return 'JPN';
    if (s.indexOf('zh') === 0) return 'CHN';
    if (s.indexOf('th') === 0) return 'THA';
    if (s.indexOf('en') === 0) return 'ENG';
    return null;
  }

  function normalize(raw) {
    if (raw == null) return '';
    var u = String(raw).trim().toUpperCase();
    if (SUPPORTED[u]) return u;
    if (u === 'KO' || u === 'KR' || u === 'KOREAN') return 'KOR';
    if (u === 'EN' || u === 'ENGLISH') return 'ENG';
    if (u === 'JA' || u === 'JP' || u === 'JPY' || u === 'JAPANESE') return 'JPN';
    if (u === 'ZH' || u === 'CN' || u === 'CH' || u === 'CHINESE') return 'CHN';
    if (u === 'TH' || u === 'THAI') return 'THA';
    return mapBrowserLocale(String(raw).trim()) || '';
  }

  function detectBrowserLang() {
    var list = [];
    try {
      if (global.navigator && global.navigator.languages && global.navigator.languages.length) {
        for (var i = 0; i < global.navigator.languages.length; i++) {
          list.push(global.navigator.languages[i]);
        }
      }
    } catch (e1) { /* ignore */ }
    try {
      if (global.navigator && global.navigator.language) list.push(global.navigator.language);
    } catch (e2) { /* ignore */ }
    for (var j = 0; j < list.length; j++) {
      var hit = mapBrowserLocale(list[j]);
      if (hit) return hit;
    }
    return 'ENG';
  }

  function detectPageLang() {
    try {
      var htmlLang = global.document && global.document.documentElement
          ? String(global.document.documentElement.getAttribute('lang') || '').trim()
          : '';
      var hit = normalize(htmlLang);
      if (hit) return hit;
    } catch (eH) { /* ignore */ }
    return detectBrowserLang();
  }

  function resolveFromScript(scriptEl) {
    if (scriptEl) {
      var attr = normalize(scriptEl.getAttribute('data-lang') || scriptEl.getAttribute('data-locale'));
      if (attr) return attr;
    }
    return detectPageLang();
  }

  global.IcopayCheckoutLang = {
    normalize: normalize,
    detectBrowserLang: detectBrowserLang,
    detectPageLang: detectPageLang,
    resolveFromScript: resolveFromScript
  };
})(typeof window !== 'undefined' ? window : this);
