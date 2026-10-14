/**
 * 통합·크립토 결제창 — 카드(pay.html)와 동일 구조의 로고·안내문구·다국어 메뉴.
 */
(function (global) {
  'use strict';

  var LANG_BTNS = [
    { code: 'JPN', ui: 'JP' },
    { code: 'KOR', ui: 'KO' },
    { code: 'ENG', ui: 'EN' },
    { code: 'CHN', ui: 'CH' },
    { code: 'THA', ui: 'TH' }
  ];

  var CRYPTO_DEFAULT = {
    KOR: '암호화폐로 안전하게 결제합니다. 자산·네트워크·지갑을 선택한 뒤 안내 주소로 입금해 주세요.',
    ENG: 'Pay securely with crypto. Choose asset, network and wallet, then send to the address shown.',
    JPN: '暗号資産で安全にお支払いください。資産・ネットワーク・ウォレットを選び、表示の住所へ送金します。',
    CHN: '使用加密货币安全付款。选择资产、网络与钱包后，转账至所示地址。',
    THA: 'ชำระด้วยคริปโตอย่างปลอดภัย เลือกสินทรัพย์ เครือข่าย และกระเป๋า แล้วโอนไปยังที่อยู่ที่แสดง'
  };

  var UNIFIED_DEFAULT = {
    KOR: '카드 또는 암호화폐로 결제할 수 있습니다. 원하시는 결제 수단을 선택해 주세요.',
    ENG: 'You can pay by card or crypto. Please choose a payment method.',
    JPN: 'カードまたは暗号資産でお支払いできます。お支払い方法を選択してください。',
    CHN: '可使用银行卡或加密货币支付。请选择支付方式。',
    THA: 'ชำระด้วยบัตรหรือคริปโตได้ กรุณาเลือกวิธีชำระเงิน'
  };

  function toPayLang(ui) {
    var u = String(ui || 'KO').toUpperCase();
    if (u === 'KO' || u === 'KR' || u === 'KOR') return 'KOR';
    if (u === 'EN' || u === 'ENG') return 'ENG';
    if (u === 'JP' || u === 'JA' || u === 'JPN') return 'JPN';
    if (u === 'CH' || u === 'ZH' || u === 'CHN') return 'CHN';
    if (u === 'TH' || u === 'THA') return 'THA';
    return 'KOR';
  }

  function toUiLang(pay) {
    var u = String(pay || 'KOR').toUpperCase();
    if (u.indexOf('KO') === 0) return 'KO';
    if (u.indexOf('EN') === 0) return 'EN';
    if (u.indexOf('JP') === 0 || u.indexOf('JA') === 0) return 'JP';
    if (u.indexOf('CH') === 0 || u.indexOf('ZH') === 0) return 'CH';
    if (u.indexOf('TH') === 0) return 'TH';
    return 'KO';
  }

  function resolveSubtitle(ctx, channel, payLang) {
    ctx = ctx || {};
    var mode = String(ctx.checkoutHeaderSubtitleMode || 'DEFAULT').trim().toUpperCase();
    if (mode === 'DISABLED') return { show: false, text: '' };
    var lk = toPayLang(payLang);
    if (mode === 'ACTIVE') {
      var i18n = ctx.checkoutHeaderSubtitleTextI18n;
      var custom = '';
      if (i18n && typeof i18n === 'object') {
        custom = String(i18n[lk] || i18n.KOR || i18n.ENG || '').trim();
      }
      if (!custom) custom = ctx.checkoutHeaderSubtitleText ? String(ctx.checkoutHeaderSubtitleText).trim() : '';
      return custom ? { show: true, text: custom } : { show: false, text: '' };
    }
    var pack = channel === 'unified' ? UNIFIED_DEFAULT : CRYPTO_DEFAULT;
    return { show: true, text: pack[lk] || pack.KOR };
  }

  function applyLogo(ctx, els) {
    els = els || {};
    var brand = els.brand || document.getElementById('cryptoBrand');
    var img = els.img || document.getElementById('cryptoBrandLogoImg');
    var text = els.text || document.getElementById('cryptoBrandText');
    if (!brand || !img || !text) return;
    ctx = ctx || {};
    var mode = String(ctx.checkoutHeaderLogoMode || 'DISABLED').trim().toUpperCase();
    brand.hidden = false;
    if (mode === 'DISABLED') {
      img.hidden = true;
      img.removeAttribute('src');
      text.hidden = true;
      text.textContent = '';
      return;
    }
    var logoUrl = ctx.checkoutHeaderLogoUrl ? String(ctx.checkoutHeaderLogoUrl).trim() : '';
    var title = (ctx.checkoutHeaderHtmlTitle && String(ctx.checkoutHeaderHtmlTitle).trim()) || 'ICOPAY';
    if (mode === 'HTML' || !logoUrl) {
      /* ACTIVE/DEFAULT 인데 URL 없으면 카드와 같이 HTML 타이틀로 폴백(로고 영역 공백 방지) */
      img.hidden = true;
      img.removeAttribute('src');
      text.hidden = false;
      text.textContent = title;
      return;
    }
    text.hidden = true;
    img.hidden = false;
    img.src = logoUrl;
    img.alt = title;
  }

  function applySubtitle(ctx, channel, uiLang, subEl) {
    var el = subEl || document.getElementById('headerSubtitle');
    var brand = document.getElementById('cryptoBrand');
    if (!el) return;
    var r = resolveSubtitle(ctx, channel, uiLang);
    if (!r.show) {
      el.hidden = true;
      el.textContent = '';
      return;
    }
    if (brand) brand.hidden = false;
    el.hidden = false;
    el.textContent = r.text;
  }

  function paintLangBar(barEl, uiLang, onChange) {
    var bar = barEl || document.getElementById('langBar');
    if (!bar) return;
    var cur = toUiLang(uiLang);
    bar.innerHTML = LANG_BTNS.map(function (L) {
      var on = L.ui === cur ? ' active' : '';
      return '<button type="button" class="' + on.trim() + '" data-lang="' + L.code + '">' + L.code + '</button>';
    }).join('');
    bar.querySelectorAll('button').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var next = toUiLang(btn.getAttribute('data-lang'));
        if (typeof onChange === 'function') onChange(next, btn.getAttribute('data-lang'));
      });
    });
  }

  /** 카드(pay.html / url-pay-public-shell)와 동일 — 총본사 브랜드 파비콘 */
  function absAssetUrl(path) {
    if (!path) return '';
    var p = String(path).trim();
    if (!p) return '';
    if (/^https?:\/\//i.test(p)) return p;
    try {
      return new URL(p, global.location.origin).href;
    } catch (e) {
      return p.charAt(0) === '/' ? p : '/' + p;
    }
  }

  function applyFavicon(ctx) {
    ctx = ctx || {};
    var path = ctx.faviconUrl ? String(ctx.faviconUrl).trim() : '';
    var head = global.document && global.document.head;
    if (!head) return;
    var link = global.document.querySelector('link[data-pay-favicon="1"]');
    if (!path) {
      if (link) link.remove();
      return;
    }
    var href = absAssetUrl(path);
    if (!href) return;
    if (!link) {
      link = global.document.createElement('link');
      link.setAttribute('data-pay-favicon', '1');
      link.setAttribute('rel', 'icon');
      link.setAttribute('type', 'image/png');
      head.appendChild(link);
    }
    link.setAttribute('href', href + (href.indexOf('?') >= 0 ? '&' : '?') + 'v=' + Date.now());
  }

  global.ICOPAY_CHECKOUT_HEADER = {
    toPayLang: toPayLang,
    toUiLang: toUiLang,
    applyLogo: applyLogo,
    applySubtitle: applySubtitle,
    applyFavicon: applyFavicon,
    paintLangBar: paintLangBar,
    resolveSubtitle: resolveSubtitle,
    CRYPTO_DEFAULT: CRYPTO_DEFAULT,
    UNIFIED_DEFAULT: UNIFIED_DEFAULT
  };
})(typeof window !== 'undefined' ? window : this);
