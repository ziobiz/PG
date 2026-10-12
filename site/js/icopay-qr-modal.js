/**
 * Admin QR preview modal — custody / seller wallet addresses.
 * Loads qrcode.js from CDN on first use.
 */
(function (global) {
  'use strict';

  var QR_CDN = 'https://cdn.jsdelivr.net/npm/qrcode@1.5.4/build/qrcode.min.js';
  var loading = null;

  function ensureQrLib() {
    if (global.QRCode && typeof global.QRCode.toCanvas === 'function') {
      return Promise.resolve(global.QRCode);
    }
    if (loading) return loading;
    loading = new Promise(function (resolve, reject) {
      var s = document.createElement('script');
      s.src = QR_CDN;
      s.async = true;
      s.onload = function () {
        if (global.QRCode) resolve(global.QRCode);
        else reject(new Error('QRCode load failed'));
      };
      s.onerror = function () { reject(new Error('QRCode CDN failed')); };
      document.head.appendChild(s);
    });
    return loading;
  }

  function t(key, fallback) {
    try {
      if (typeof global.pgAdminUiT === 'function') {
        var v = global.pgAdminUiT(key);
        if (v && v !== key) return v;
      }
    } catch (e0) { /* ignore */ }
    return fallback != null ? fallback : key;
  }

  function showAddressQrModal(opts) {
    opts = opts || {};
    var address = String(opts.address || '').trim();
    var title = String(opts.title || t('지갑 주소', '지갑 주소'));
    var subtitle = String(opts.subtitle || '').trim();
    if (!address) {
      if (typeof global.alert === 'function') {
        global.alert(t('주소가 없습니다.', '주소가 없습니다.'));
      }
      return;
    }
    var modalId = 'icopayQrAddrModal';
    var existing = document.getElementById(modalId);
    if (existing) existing.remove();

    var wrap = document.createElement('div');
    wrap.id = modalId;
    wrap.className = 'modal fade';
    wrap.tabIndex = -1;
    wrap.innerHTML =
      '<div class="modal-dialog modal-dialog-centered">' +
      '<div class="modal-content">' +
      '<div class="modal-header py-2">' +
      '<h5 class="modal-title">' + escapeHtml(title) + '</h5>' +
      '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
      '</div>' +
      '<div class="modal-body text-center">' +
      (subtitle ? '<p class="small text-muted mb-2">' + escapeHtml(subtitle) + '</p>' : '') +
      '<canvas id="icopayQrCanvas" class="mx-auto d-block mb-3" width="220" height="220"></canvas>' +
      '<div class="font-monospace small text-break user-select-all border rounded p-2 bg-light" id="icopayQrAddrText"></div>' +
      '<button type="button" class="btn btn-sm btn-outline-secondary mt-3" id="icopayQrCopyBtn">' +
      escapeHtml(t('주소 복사', '주소 복사')) + '</button>' +
      '</div>' +
      '<div class="modal-footer py-2">' +
      '<button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">' +
      escapeHtml(t('닫기', '닫기')) + '</button>' +
      '</div></div></div>';
    document.body.appendChild(wrap);
    var addrEl = wrap.querySelector('#icopayQrAddrText');
    if (addrEl) addrEl.textContent = address;
    var copyBtn = wrap.querySelector('#icopayQrCopyBtn');
    if (copyBtn) {
      copyBtn.addEventListener('click', function () {
        try {
          if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(address);
          } else {
            var ta = document.createElement('textarea');
            ta.value = address;
            document.body.appendChild(ta);
            ta.select();
            document.execCommand('copy');
            ta.remove();
          }
          copyBtn.textContent = t('복사됨', '복사됨');
        } catch (e1) { /* ignore */ }
      });
    }

    function openModal() {
      try {
        if (global.bootstrap && global.bootstrap.Modal) {
          global.bootstrap.Modal.getOrCreateInstance(wrap).show();
        } else {
          wrap.style.display = 'block';
          wrap.classList.add('show');
        }
      } catch (e2) {
        wrap.style.display = 'block';
      }
    }

    ensureQrLib().then(function (QR) {
      var canvas = wrap.querySelector('#icopayQrCanvas');
      return QR.toCanvas(canvas, address, { width: 220, margin: 2 });
    }).then(openModal).catch(function () {
      openModal();
    });
  }

  function escapeHtml(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  /** Parse sellerWalletJson → [{key, address}] */
  function parseSellerWallets(jsonStr) {
    var out = [];
    if (!jsonStr || !String(jsonStr).trim()) return out;
    try {
      var o = typeof jsonStr === 'string' ? JSON.parse(jsonStr) : jsonStr;
      if (!o || typeof o !== 'object') return out;
      Object.keys(o).forEach(function (k) {
        var v = o[k];
        if (v != null && String(v).trim()) {
          out.push({ key: k, address: String(v).trim() });
        }
      });
    } catch (e3) { /* ignore */ }
    return out;
  }

  function buildSellerJsonFromFields(usdtTrc20, erc20) {
    var o = {};
    var t1 = String(usdtTrc20 || '').trim();
    var t2 = String(erc20 || '').trim();
    if (t1) o.USDT_TRC20 = t1;
    if (t2) o.ERC20 = t2;
    return Object.keys(o).length ? JSON.stringify(o) : '';
  }

  /** Collect addresses from dynamic .crypto-seller-addr[data-wallet-key] inputs. */
  function buildSellerJsonFromContainer(root) {
    var o = {};
    if (!root || !root.querySelectorAll) return '';
    root.querySelectorAll('.crypto-seller-addr[data-wallet-key]').forEach(function (inp) {
      var k = String(inp.getAttribute('data-wallet-key') || '').trim().toUpperCase();
      var v = String(inp.value || '').trim();
      if (k && v) o[k] = v;
    });
    return Object.keys(o).length ? JSON.stringify(o) : '';
  }

  function fillWalletFieldsFromJson(root, jsonStr, selUsdt, selErc) {
    var list = parseSellerWallets(jsonStr);
    var map = {};
    list.forEach(function (w) {
      map[String(w.key || '').toUpperCase()] = w.address;
    });
    if (root && root.querySelectorAll) {
      root.querySelectorAll('.crypto-seller-addr[data-wallet-key]').forEach(function (inp) {
        var k = String(inp.getAttribute('data-wallet-key') || '').toUpperCase();
        if (map[k]) inp.value = map[k];
        else if (k === 'USDT_TRC20' && map.TRC20) inp.value = map.TRC20;
        else if (k.indexOf('_ERC20') > 0 && map.ERC20) inp.value = map.ERC20;
      });
    }
    if (selUsdt || selErc) {
      var usdt = map.USDT_TRC20 || map.TRC20 || '';
      var erc = map.ERC20 || map.USDT_ERC20 || map.USDC_ERC20 || map.EURC_ERC20 || '';
      var a = root && selUsdt ? root.querySelector(selUsdt) : null;
      var b = root && selErc ? root.querySelector(selErc) : null;
      if (a) a.value = usdt;
      if (b) b.value = erc;
    }
  }

  global.ICOPAY_QR_MODAL = {
    showAddressQrModal: showAddressQrModal,
    parseSellerWallets: parseSellerWallets,
    buildSellerJsonFromFields: buildSellerJsonFromFields,
    buildSellerJsonFromContainer: buildSellerJsonFromContainer,
    fillWalletFieldsFromJson: fillWalletFieldsFromJson
  };
})(typeof window !== 'undefined' ? window : this);
