/**
 * 관리자 메뉴 레이아웃 — B안(허브) / LEGACY(기존 21+9) 즉시 전환.
 *
 * 복원(기존 메뉴): 브라우저 콘솔 또는 URL
 *   localStorage.setItem('pg_menu_layout', 'LEGACY'); location.reload();
 *   https://icopay.co.kr/?menu=legacy
 *
 * B안 적용:
 *   localStorage.setItem('pg_menu_layout', 'B'); location.reload();
 *   https://icopay.co.kr/?menu=b
 */
(function (global) {
  'use strict';

  var STORAGE_KEY = 'pg_menu_layout';
  var DEFAULT_LAYOUT = 'B';

  function readQueryOverride() {
    try {
      var params = new URLSearchParams(global.location.search || '');
      var q = (params.get('menu') || params.get('pgMenuLayout') || '').trim().toLowerCase();
      if (q === 'legacy' || q === 'old' || q === 'l') {
        try { global.localStorage.setItem(STORAGE_KEY, 'LEGACY'); } catch (e1) { /* ignore */ }
        return 'LEGACY';
      }
      if (q === 'b' || q === 'hub' || q === 'new') {
        try { global.localStorage.setItem(STORAGE_KEY, 'B'); } catch (e2) { /* ignore */ }
        return 'B';
      }
    } catch (eQ) { /* ignore */ }
    return null;
  }

  function normalizeLayout(v) {
    var t = String(v || '').trim().toUpperCase();
    return t === 'LEGACY' ? 'LEGACY' : 'B';
  }

  function currentLayout() {
    var q = readQueryOverride();
    if (q) return q;
    try {
      var saved = global.localStorage.getItem(STORAGE_KEY);
      if (saved) return normalizeLayout(saved);
    } catch (eLs) { /* ignore */ }
    return DEFAULT_LAYOUT;
  }

  function isLayoutB() {
    return currentLayout() === 'B';
  }

  function isLayoutLegacy() {
    return !isLayoutB();
  }

  function setLayout(mode) {
    var m = normalizeLayout(mode);
    try { global.localStorage.setItem(STORAGE_KEY, m); } catch (eSet) { /* ignore */ }
    return m;
  }

  global.PG_MENU_LAYOUT = currentLayout();
  global.PG_MENU_LAYOUT_CONFIG = {
    STORAGE_KEY: STORAGE_KEY,
    DEFAULT_LAYOUT: DEFAULT_LAYOUT,
    currentLayout: currentLayout,
    isLayoutB: isLayoutB,
    isLayoutLegacy: isLayoutLegacy,
    setLayout: setLayout
  };
})(typeof window !== 'undefined' ? window : this);
