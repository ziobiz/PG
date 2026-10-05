/**
 * ICOPAY 크립토 지갑·자산 파스텔 타일 (인라인 SVG).
 * 구매자 UI에 결제대행사명 미노출.
 */
(function (global) {
  'use strict';

  function svg(inner, vb) {
    return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="' + vb + '" class="ic-crypto-icon" aria-hidden="true">' + inner + '</svg>';
  }

  var ICONS = {
    usdt: svg('<circle cx="16" cy="16" r="14" fill="#26A17B"/><text x="16" y="21" text-anchor="middle" font-size="12" font-weight="700" fill="#fff" font-family="Arial,sans-serif">₮</text>', '0 0 32 32'),
    usdc: svg('<circle cx="16" cy="16" r="14" fill="#2775CA"/><text x="16" y="21" text-anchor="middle" font-size="10" font-weight="700" fill="#fff" font-family="Arial,sans-serif">$</text>', '0 0 32 32'),
    eurc: svg('<circle cx="16" cy="16" r="14" fill="#7C3AED"/><text x="16" y="21" text-anchor="middle" font-size="11" font-weight="700" fill="#fff" font-family="Arial,sans-serif">€</text>', '0 0 32 32'),
    metamask: svg('<path d="M8 6l8 5 8-5-2 10H10L8 6z" fill="#E2761B"/><path d="M10 18l2 8 4-3 4 3 2-8H10z" fill="#E4761B"/>', '0 0 32 32'),
    trust: svg('<path d="M16 4l10 4v8c0 7-4.5 11-10 13C10.5 27 6 23 6 16V8l10-4z" fill="#3375BB"/><path d="M16 8l6 2.4v5.2c0 4.2-2.7 6.8-6 8.1-3.3-1.3-6-3.9-6-8.1V10.4L16 8z" fill="#fff"/>', '0 0 32 32'),
    coinbase: svg('<circle cx="16" cy="16" r="14" fill="#0052FF"/><rect x="10" y="14" width="12" height="4" rx="2" fill="#fff"/>', '0 0 32 32'),
    binance: svg('<path d="M16 4l3 3-3 3-3-3 3-3zm0 8l6 6-6 6-6-6 6-6zm-8 0l3 3-3 3-3-3 3-3zm16 0l3 3-3 3-3-3 3-3z" fill="#F3BA2F"/>', '0 0 32 32'),
    okx: svg('<rect x="4" y="4" width="10" height="10" rx="1" fill="#111"/><rect x="18" y="4" width="10" height="10" rx="1" fill="#111"/><rect x="4" y="18" width="10" height="10" rx="1" fill="#111"/><rect x="18" y="18" width="10" height="10" rx="1" fill="#111"/>', '0 0 32 32'),
    tronlink: svg('<path d="M6 8l10-4 10 4-10 20L6 8z" fill="#1677FF"/><path d="M16 6v16l8-14-8-2z" fill="#69B1FF"/>', '0 0 32 32'),
    tokenpocket: svg('<circle cx="16" cy="16" r="14" fill="#2980FE"/><text x="16" y="21" text-anchor="middle" font-size="11" font-weight="800" fill="#fff" font-family="Arial,sans-serif">TP</text>', '0 0 32 32'),
    phantom: svg('<circle cx="16" cy="16" r="14" fill="#AB9FF2"/><ellipse cx="16" cy="15" rx="8" ry="7" fill="#4B49C6"/><circle cx="13" cy="14" r="1.5" fill="#fff"/><circle cx="19" cy="14" r="1.5" fill="#fff"/>', '0 0 32 32'),
    bitget: svg('<circle cx="16" cy="16" r="14" fill="#00F0FF"/><path d="M10 18l6-8 6 8H10z" fill="#0B3B3E"/>', '0 0 32 32'),
    imtoken: svg('<circle cx="16" cy="16" r="14" fill="#11C4A1"/><path d="M10 12h12v2H10v-2zm0 4h12v2H10v-2zm2 4h8v2h-8v-2z" fill="#fff"/>', '0 0 32 32'),
    card: svg('<rect x="4" y="8" width="24" height="16" rx="2" fill="#64748B"/><rect x="4" y="12" width="24" height="4" fill="#94A3B8"/><rect x="8" y="18" width="8" height="2" rx="1" fill="#E2E8F0"/>', '0 0 32 32'),
    crypto: svg('<circle cx="16" cy="16" r="14" fill="#0F766E"/><path d="M16 8v16M11 12h8a3 3 0 010 6h-6a3 3 0 000 6h9" stroke="#fff" stroke-width="2" fill="none"/>', '0 0 32 32')
  };

  function icon(key) {
    var k = String(key || '').toLowerCase();
    return ICONS[k] || ICONS.crypto;
  }

  function tile(opts) {
    opts = opts || {};
    var bg = opts.pastelBg || '#F1F5F9';
    var fg = opts.pastelFg || '#0F172A';
    var accent = opts.accent || fg;
    var name = opts.name || opts.code || '';
    var code = opts.code || '';
    var ik = opts.iconKey || code;
    return '<button type="button" class="ic-crypto-tile" data-code="' + esc(code) + '"' +
      ' style="--ic-bg:' + esc(bg) + ';--ic-fg:' + esc(fg) + ';--ic-accent:' + esc(accent) + '">' +
      '<span class="ic-crypto-tile-icon">' + icon(ik) + '</span>' +
      '<span class="ic-crypto-tile-name">' + esc(name) + '</span>' +
      '</button>';
  }

  function renderGrid(el, items, onPick) {
    if (!el) return;
    var list = Array.isArray(items) ? items : [];
    el.innerHTML = list.map(function (it) { return tile(it); }).join('');
    el.querySelectorAll('.ic-crypto-tile').forEach(function (btn) {
      btn.addEventListener('click', function () {
        el.querySelectorAll('.ic-crypto-tile').forEach(function (b) { b.classList.remove('is-selected'); });
        btn.classList.add('is-selected');
        if (typeof onPick === 'function') onPick(btn.getAttribute('data-code'), btn);
      });
    });
  }

  function esc(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
  }

  global.ICOPAY_CRYPTO_UI = {
    icon: icon,
    tile: tile,
    renderGrid: renderGrid
  };
})(typeof window !== 'undefined' ? window : this);
