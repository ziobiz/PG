/**
 * ICOPAY 크립토 지갑·네트워크 아이콘 (첨부 UI 색톤·로고 스타일).
 * 구매자 UI에 결제대행사명 미노출.
 */
(function (global) {
  'use strict';

  function svg(inner, vb) {
    return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="' + (vb || '0 0 32 32') + '" class="ic-crypto-icon" aria-hidden="true">' + inner + '</svg>';
  }

  var ICONS = {
    usdt: svg('<circle cx="16" cy="16" r="14" fill="#26A17B"/><text x="16" y="21" text-anchor="middle" font-size="12" font-weight="700" fill="#fff" font-family="Arial,sans-serif">₮</text>'),
    usdc: svg('<circle cx="16" cy="16" r="14" fill="#2775CA"/><text x="16" y="21" text-anchor="middle" font-size="10" font-weight="700" fill="#fff" font-family="Arial,sans-serif">$</text>'),
    eurc: svg('<circle cx="16" cy="16" r="14" fill="#7C3AED"/><text x="16" y="21" text-anchor="middle" font-size="11" font-weight="700" fill="#fff" font-family="Arial,sans-serif">€</text>'),
    metamask: svg('<path d="M8 6l8 5 8-5-2 10H10L8 6z" fill="#E2761B"/><path d="M10 18l2 8 4-3 4 3 2-8H10z" fill="#E4761B"/>'),
    trust: svg('<path d="M16 4l10 4v8c0 7-4.5 11-10 13C10.5 27 6 23 6 16V8l10-4z" fill="#3375BB"/><path d="M16 8l6 2.4v5.2c0 4.2-2.7 6.8-6 8.1-3.3-1.3-6-3.9-6-8.1V10.4L16 8z" fill="#fff"/>'),
    coinbase: svg('<circle cx="16" cy="16" r="14" fill="#0052FF"/><rect x="10" y="14" width="12" height="4" rx="2" fill="#fff"/>'),
    binance: svg('<path d="M16 4l3 3-3 3-3-3 3-3zm0 8l6 6-6 6-6-6 6-6zm-8 0l3 3-3 3-3-3 3-3zm16 0l3 3-3 3-3-3 3-3z" fill="#F3BA2F"/>'),
    okx: svg('<rect x="4" y="4" width="10" height="10" rx="1" fill="#111"/><rect x="18" y="4" width="10" height="10" rx="1" fill="#111"/><rect x="4" y="18" width="10" height="10" rx="1" fill="#111"/><rect x="18" y="18" width="10" height="10" rx="1" fill="#111"/>'),
    tronlink: svg('<path d="M6 8l10-4 10 4-10 20L6 8z" fill="#1677FF"/><path d="M16 6v16l8-14-8-2z" fill="#69B1FF"/>'),
    tokenpocket: svg('<circle cx="16" cy="16" r="14" fill="#2980FE"/><text x="16" y="21" text-anchor="middle" font-size="11" font-weight="800" fill="#fff" font-family="Arial,sans-serif">TP</text>'),
    phantom: svg('<circle cx="16" cy="16" r="14" fill="#AB9FF2"/><ellipse cx="16" cy="15" rx="8" ry="7" fill="#4B49C6"/><circle cx="13" cy="14" r="1.5" fill="#fff"/><circle cx="19" cy="14" r="1.5" fill="#fff"/>'),
    bitget: svg('<circle cx="16" cy="16" r="14" fill="#00F0FF"/><path d="M10 18l6-8 6 8H10z" fill="#0B3B3E"/>'),
    imtoken: svg('<circle cx="16" cy="16" r="14" fill="#11C4A1"/><path d="M10 12h12v2H10v-2zm0 4h12v2H10v-2zm2 4h8v2h-8v-2z" fill="#fff"/>'),
    card: svg('<rect x="4" y="8" width="24" height="16" rx="2" fill="#64748B"/><rect x="4" y="12" width="24" height="4" fill="#94A3B8"/><rect x="8" y="18" width="8" height="2" rx="1" fill="#E2E8F0"/>'),
    crypto: svg('<circle cx="16" cy="16" r="14" fill="#0F766E"/><path d="M16 8v16M11 12h8a3 3 0 010 6h-6a3 3 0 000 6h9" stroke="#fff" stroke-width="2" fill="none"/>'),
    /* 네트워크 — 첨부 목록 색톤 */
    base: svg('<circle cx="16" cy="16" r="14" fill="#0052FF"/><rect x="8" y="14.5" width="16" height="3" rx="1.5" fill="#fff"/>'),
    arbitrum: svg('<polygon points="16,3 28,10 28,22 16,29 4,22 4,10" fill="#213147"/><path d="M12 20l4-10 4 10h-2.2l-.6-1.6h-2.4L14.2 20H12zm3.1-3.4h1.8L16 12.8l-.9 3.8z" fill="#28A0F0"/><path d="M18.5 20.5l1.2-3.2 1.3 3.2H18.5z" fill="#fff"/>'),
    bep20: svg('<circle cx="16" cy="16" r="14" fill="#F0B90B"/><path d="M16 7l2.2 2.2L16 11.4l-2.2-2.2L16 7zm0 5.2l4.4 4.4L16 21l-4.4-4.4L16 12.2zm-5.2 0L13 14.4l-2.2 2.2L8.6 14.4l2.2-2.2zm10.4 0l2.2 2.2-2.2 2.2-2.2-2.2 2.2-2.2z" fill="#fff"/>'),
    bnb: svg('<circle cx="16" cy="16" r="14" fill="#F0B90B"/><path d="M16 7l2.2 2.2L16 11.4l-2.2-2.2L16 7zm0 5.2l4.4 4.4L16 21l-4.4-4.4L16 12.2zm-5.2 0L13 14.4l-2.2 2.2L8.6 14.4l2.2-2.2zm10.4 0l2.2 2.2-2.2 2.2-2.2-2.2 2.2-2.2z" fill="#fff"/>'),
    ethereum: svg('<path d="M16 3l-.4.9v16.4l.4.4 8.5-5.1L16 3z" fill="#8C8C8C"/><path d="M16 3L7.5 15.6 16 20.7V3z" fill="#343434"/><path d="M16 22.1l-.2.3v6.2l.2.6 8.5-12L16 22.1z" fill="#8C8C8C"/><path d="M16 29.2V22.1L7.5 17.2 16 29.2z" fill="#3C3C3B"/><path d="M16 20.7l8.5-5.1L16 12.4v8.3z" fill="#848484"/><path d="M7.5 15.6L16 20.7V12.4L7.5 15.6z" fill="#141414"/>'),
    erc20: null,
    optimism: svg('<circle cx="16" cy="16" r="14" fill="#FF0420"/><text x="16" y="20.5" text-anchor="middle" font-size="11" font-weight="800" fill="#fff" font-family="Arial,sans-serif">OP</text>'),
    polygon: svg('<circle cx="16" cy="16" r="14" fill="#8247E5"/><path d="M11.2 13.2l2.4-1.4 2.4 1.4v2.8l-2.4 1.4-2.4-1.4v-2.8zm5.2-3l2.4-1.4 2.4 1.4v2.8l-2.4 1.4-2.4-1.4V10.2zm0 6l2.4-1.4 2.4 1.4v2.8l-2.4 1.4-2.4-1.4v-2.8z" fill="#fff"/>'),
    kaia: svg('<circle cx="16" cy="16" r="14" fill="#111"/><path d="M10 22c2.5-6 5-9.5 9.5-12.5 1.8 2.2 3.2 5 3.8 8.2-3.2 1.2-6.5 2.2-9.8 2.8-.9-0.5-2.2-0.8-3.5 1.5z" fill="#B8FF3C"/><circle cx="20.5" cy="11.5" r="1.4" fill="#B8FF3C"/>'),
    tron: svg('<path d="M7 9.5L16 5l9 4.5-9 17.5L7 9.5z" fill="none" stroke="#FF0013" stroke-width="2.2"/><path d="M16 7.2v14.2M9.5 10.2L16 21.4M22.5 10.2L16 21.4" stroke="#FF0013" stroke-width="1.6" fill="none"/>'),
    trc20: null,
    solana: svg('<defs><linearGradient id="icSol" x1="4" y1="24" x2="28" y2="8" gradientUnits="userSpaceOnUse"><stop stop-color="#00FFA3"/><stop offset="1" stop-color="#DC1FFF"/></linearGradient></defs><path d="M8 10.5h14.5c.6 0 1.2.2 1.6.7l1.4 1.5c.4.4.1 1.1-.5 1.1H10.5c-.6 0-1.2-.2-1.6-.7L7.5 11.6c-.4-.4-.1-1.1.5-1.1z" fill="url(#icSol)"/><path d="M8 18.2h14.5c.6 0 1.2.2 1.6.7l1.4 1.5c.4.4.1 1.1-.5 1.1H10.5c-.6 0-1.2-.2-1.6-.7l-1.4-1.5c-.4-.4-.1-1.1.5-1.1z" fill="url(#icSol)"/><path d="M24 14.35H9.5c-.6 0-1.2-.2-1.6-.7L6.5 12.15c-.4-.4-.1-1.1.5-1.1H21.5c.6 0 1.2.2 1.6.7l1.4 1.5c.4.4.1 1.1-.5 1.1z" fill="url(#icSol)"/>'),
    sol: null,
    stellar: svg('<circle cx="16" cy="16" r="14" fill="#000"/><path d="M7 18.5l18-7M7 13.5l18 7" stroke="#fff" stroke-width="2.4" stroke-linecap="round"/>'),
    avaxc: svg('<circle cx="16" cy="16" r="14" fill="#E84142"/><path d="M16 8l7 12H9L16 8zm0 4.2L12.8 17h6.4L16 12.2z" fill="#fff"/>'),
    ton: svg('<circle cx="16" cy="16" r="14" fill="#0098EA"/><path d="M8 10h16l-8 14L8 10z" fill="#fff"/><path d="M16 12v8" stroke="#0098EA" stroke-width="2"/>'),
    opbnb: null,
    near: svg('<circle cx="16" cy="16" r="14" fill="#000"/><path d="M11 9h3.2l6.6 10.2V9H24v14h-3.2L14.2 12.8V23H11V9z" fill="#fff"/>')
  };
  ICONS.erc20 = ICONS.ethereum;
  ICONS.trc20 = ICONS.tron;
  ICONS.bep20 = ICONS.bnb;
  ICONS.opbnb = ICONS.bnb;
  ICONS.sol = ICONS.solana;
  ICONS.optimism = ICONS.optimism;

  var NET_ALIAS = {
    ERC20: 'ethereum', ETHEREUM: 'ethereum', ETH: 'ethereum',
    TRC20: 'tron', TRON: 'tron',
    BEP20: 'bnb', BNB: 'bnb', BSC: 'bnb', OPBNB: 'opbnb',
    ARBITRUM: 'arbitrum', ARB: 'arbitrum',
    OPTIMISM: 'optimism', OP: 'optimism',
    POLYGON: 'polygon', MATIC: 'polygon',
    BASE: 'base',
    KAIA: 'kaia',
    SOL: 'solana', SOLANA: 'solana',
    STELLAR: 'stellar', XLM: 'stellar',
    AVAXC: 'avaxc', AVAX: 'avaxc',
    TON: 'ton',
    NEAR: 'near'
  };

  /** 로고 브랜드색 기반 파스텔 (행·주소 칸 배경) */
  var NET_PASTEL = {
    base: { bg: '#E8F0FF', border: '#7AA2FF', fg: '#0039B3', accent: '#0052FF' },
    arbitrum: { bg: '#E8F4FC', border: '#7EC4F5', fg: '#0B4F7A', accent: '#28A0F0' },
    bnb: { bg: '#FFF8E1', border: '#F0C94A', fg: '#7A5C00', accent: '#F0B90B' },
    bep20: { bg: '#FFF8E1', border: '#F0C94A', fg: '#7A5C00', accent: '#F0B90B' },
    opbnb: { bg: '#FFF8E1', border: '#F0C94A', fg: '#7A5C00', accent: '#F0B90B' },
    ethereum: { bg: '#EEF0F5', border: '#A8AEB8', fg: '#2A2A2A', accent: '#627EEA' },
    erc20: { bg: '#EEF0F5', border: '#A8AEB8', fg: '#2A2A2A', accent: '#627EEA' },
    optimism: { bg: '#FFE8EB', border: '#FF8A96', fg: '#9B0012', accent: '#FF0420' },
    polygon: { bg: '#F3EBFF', border: '#B794F0', fg: '#4C1D95', accent: '#8247E5' },
    kaia: { bg: '#F4FFE8', border: '#C8F07A', fg: '#3F5A00', accent: '#B8FF3C' },
    tron: { bg: '#FFE8EA', border: '#FF808A', fg: '#9B0010', accent: '#FF0013' },
    trc20: { bg: '#FFE8EA', border: '#FF808A', fg: '#9B0010', accent: '#FF0013' },
    solana: { bg: '#F0FDF8', border: '#7EE0C0', fg: '#065F46', accent: '#14F195' },
    sol: { bg: '#F0FDF8', border: '#7EE0C0', fg: '#065F46', accent: '#14F195' },
    stellar: { bg: '#F1F5F9', border: '#94A3B8', fg: '#0F172A', accent: '#334155' },
    avaxc: { bg: '#FEECEC', border: '#F08080', fg: '#8B1A1A', accent: '#E84142' },
    ton: { bg: '#E6F6FD', border: '#6EC8F0', fg: '#005A8C', accent: '#0098EA' },
    near: { bg: '#F1F5F9', border: '#94A3B8', fg: '#0F172A', accent: '#000000' },
    crypto: { bg: '#F1F5F9', border: '#94A3B8', fg: '#0F172A', accent: '#0F766E' }
  };

  function resolveNetKey(key) {
    var raw = String(key || '').toLowerCase();
    var mapped = NET_ALIAS[String(key || '').toUpperCase()];
    return (mapped || raw).toLowerCase();
  }

  function icon(key) {
    var k = resolveNetKey(key);
    return ICONS[k] || ICONS.crypto;
  }

  function networkPastel(key) {
    var k = resolveNetKey(key);
    return NET_PASTEL[k] || NET_PASTEL.crypto;
  }

  /** 요소에 네트워크 파스텔 CSS 변수·클래스 적용 */
  function applyNetworkPastel(el, networkCode) {
    if (!el) return;
    var p = networkPastel(networkCode);
    el.classList.add('crypto-net-pastel');
    el.style.setProperty('--crypto-net-bg', p.bg);
    el.style.setProperty('--crypto-net-border', p.border);
    el.style.setProperty('--crypto-net-fg', p.fg);
    el.style.setProperty('--crypto-net-accent', p.accent);
    el.setAttribute('data-crypto-net', String(networkCode || '').toUpperCase());
  }

  function pastelInlineStyle(networkCode) {
    var p = networkPastel(networkCode);
    return '--crypto-net-bg:' + p.bg + ';--crypto-net-border:' + p.border +
      ';--crypto-net-fg:' + p.fg + ';--crypto-net-accent:' + p.accent +
      ';background-color:' + p.bg;
  }

  function esc(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
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

  /** 첨부 UI형 네트워크 리스트 (로고 + 이름 + ›) */
  function networkListHtml(items, selectedCode) {
    var list = Array.isArray(items) ? items : [];
    var sel = String(selectedCode || '').toUpperCase();
    return '<div class="ic-crypto-net-list" role="listbox">' + list.map(function (it) {
      var code = String((it && (it.code || it.networkCode)) || it || '').toUpperCase();
      if (!code) return '';
      var name = (it && (it.name || it.displayName)) ? String(it.name || it.displayName) : code;
      var on = sel && code === sel ? ' is-selected' : '';
      return '<button type="button" class="ic-crypto-net-row' + on + '" role="option" data-code="' + esc(code) + '" aria-selected="' + (on ? 'true' : 'false') + '">' +
        '<span class="ic-crypto-net-logo">' + icon(code) + '</span>' +
        '<span class="ic-crypto-net-name">' + esc(name) + '</span>' +
        '<span class="ic-crypto-net-chev" aria-hidden="true">›</span>' +
        '</button>';
    }).join('') + '</div>';
  }

  function renderNetworkList(el, items, selectedCode, onPick) {
    if (!el) return;
    el.innerHTML = networkListHtml(items, selectedCode);
    el.querySelectorAll('.ic-crypto-net-row').forEach(function (btn) {
      btn.addEventListener('click', function () {
        el.querySelectorAll('.ic-crypto-net-row').forEach(function (b) {
          b.classList.remove('is-selected');
          b.setAttribute('aria-selected', 'false');
        });
        btn.classList.add('is-selected');
        btn.setAttribute('aria-selected', 'true');
        if (typeof onPick === 'function') onPick(btn.getAttribute('data-code'), btn);
      });
    });
  }

  function netLabel(it, code) {
    if (it && (it.name || it.displayName)) return String(it.name || it.displayName);
    return code;
  }

  /** 텍스트 전용 표준 드롭다운(아이콘 없음 · 높이·너비 통일). */
  function networkSelectHtml(items, selectedCode) {
    var list = Array.isArray(items) ? items : [];
    var sel = String(selectedCode || '').toUpperCase();
    var hasSel = false;
    list.forEach(function (it) {
      var c = String((it && (it.code || it.networkCode)) || it || '').toUpperCase();
      if (c && c === sel) hasSel = true;
    });
    if (!hasSel && list[0]) {
      sel = String((list[0] && (list[0].code || list[0].networkCode)) || list[0] || '').toUpperCase();
    }
    var opts = list.map(function (it) {
      var code = String((it && (it.code || it.networkCode)) || it || '').toUpperCase();
      if (!code) return '';
      var name = netLabel(it, code);
      var on = code === sel ? ' selected' : '';
      return '<option value="' + esc(code) + '"' + on + '>' + esc(name) + '</option>';
    }).join('');
    return '<select class="form-select form-select-sm ic-crypto-net-select-native" data-code="' + esc(sel) + '" aria-label="network">' +
      opts + '</select>';
  }

  function renderNetworkSelect(el, items, selectedCode, onPick) {
    if (!el) return;
    el.innerHTML = networkSelectHtml(items, selectedCode);
    var selEl = el.querySelector('select.ic-crypto-net-select-native');
    if (!selEl) return;
    function tint(code) {
      applyNetworkPastel(selEl, code);
    }
    tint(selEl.value || selectedCode);
    selEl.addEventListener('change', function () {
      var code = String(selEl.value || '').toUpperCase();
      selEl.setAttribute('data-code', code);
      tint(code);
      if (typeof onPick === 'function') onPick(code);
    });
  }

  global.ICOPAY_CRYPTO_UI = {
    icon: icon,
    tile: tile,
    renderGrid: renderGrid,
    networkListHtml: networkListHtml,
    renderNetworkList: renderNetworkList,
    networkSelectHtml: networkSelectHtml,
    renderNetworkSelect: renderNetworkSelect,
    networkPastel: networkPastel,
    applyNetworkPastel: applyNetworkPastel,
    pastelInlineStyle: pastelInlineStyle
  };
})(typeof window !== 'undefined' ? window : this);
