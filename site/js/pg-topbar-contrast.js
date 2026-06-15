/**
 * 관리자 상단 헤더(.header-top) — 배경 밝기에 따라 글자·아이콘 색 자동 보정.
 * 총판 배경테마(BROWN 등)와 태블릿 모드(밝은 헤더)가 겹칠 때 사용자명이 안 보이는 문제 방지.
 */
(function (w) {
  'use strict';

  function parseRgb(cssColor) {
    if (!cssColor) return null;
    var m = String(cssColor).trim().match(/rgba?\(\s*([\d.]+)\s*,\s*([\d.]+)\s*,\s*([\d.]+)(?:\s*,\s*([\d.]+))?\s*\)/i);
    if (!m) return null;
    return {
      r: Math.min(255, Math.max(0, parseFloat(m[1]))),
      g: Math.min(255, Math.max(0, parseFloat(m[2]))),
      b: Math.min(255, Math.max(0, parseFloat(m[3]))),
      a: m[4] != null ? parseFloat(m[4]) : 1
    };
  }

  function relativeLuminance(r, g, b) {
    function lin(c) {
      c /= 255;
      return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
    return 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b);
  }

  function effectiveLuminance(rgb) {
    if (!rgb) return null;
    var r = rgb.r;
    var g = rgb.g;
    var b = rgb.b;
    if (rgb.a < 1) {
      var a = rgb.a;
      r = r * a + 255 * (1 - a);
      g = g * a + 255 * (1 - a);
      b = b * a + 255 * (1 - a);
    }
    return relativeLuminance(r, g, b);
  }

  function syncTopbarContrast() {
    var header = w.document && w.document.querySelector('.header-top');
    if (!header) return;
    var rgb = parseRgb(w.getComputedStyle(header).backgroundColor);
    var lum = effectiveLuminance(rgb);
    if (lum == null) return;
    var light = lum > 0.52;
    header.style.setProperty('--theme-topbar-fg', light ? '#1e293b' : '#f8fafc');
    header.style.setProperty('--theme-topbar-icon', light ? '#475569' : '#e2e8f0');
    try {
      w.document.body.setAttribute('data-topbar-tone', light ? 'light' : 'dark');
    } catch (eAttr) { /* ignore */ }
  }

  function scheduleSync() {
    w.requestAnimationFrame(function () {
      syncTopbarContrast();
      w.requestAnimationFrame(syncTopbarContrast);
    });
  }

  w.PG_TOPBAR_CONTRAST = {
    sync: syncTopbarContrast,
    schedule: scheduleSync
  };

  if (w.document) {
    if (w.document.readyState === 'loading') {
      w.document.addEventListener('DOMContentLoaded', scheduleSync);
    } else {
      scheduleSync();
    }
  }
  w.addEventListener('resize', scheduleSync);
})(window);
