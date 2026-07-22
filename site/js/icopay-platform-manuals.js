/**
 * 운영관리 > 운영 메뉴얼
 * 목록 클릭 → 새 창에서 정식 PDF + PDF 표지형 브랜드 로고(PNG) 헤더
 * 버전 = ICOPAY_PLATFORM_RELEASE.currentLiveVersion
 * 브랜드 로고 = /api/hq/platformManuals/coverLogo (매뉴얼 PDF와 동일 PNG)
 * 본문 = /api/hq/platformManuals/pdf
 * 노출 = 로그인 조직 단계 이하 audience 만
 */
(function (global) {
  'use strict';

  var FALLBACK_VERSION = '2.54';

  var AUDIENCE_ORDER = ['super', 'hqdist', 'merchant'];
  var AUDIENCE_LABEL = {
    super: '총본사용',
    hqdist: '본사·총판용',
    merchant: '가맹점용'
  };

  var AUDIENCE_MIN_ORG_CODE = {
    super: 1,
    hqdist: 2,
    merchant: 7
  };

  var ORG_LEVEL_CODE = {
    HEADQUARTERS: 1,
    REGIONAL: 2,
    MASTER_DIST: 3,
    BRANCH: 4,
    AGENCY: 5,
    SALES_OFFICE: 6,
    MERCHANT: 7
  };

  /** 전체 PDF 통일 */
  var ITEMS = [
    { id: 'super-ops', audience: 'super', title: '총본사 운영 메뉴얼' },
    { id: 'hq-ops', audience: 'hqdist', title: '본사 운영 메뉴얼' },
    { id: 'dist-ops', audience: 'hqdist', title: '총판 운영 메뉴얼' },
    { id: 'hqdist-risk-intro', audience: 'hqdist', title: '리스크 트리거 발동 소개 안내' },
    { id: 'merchant-ops', audience: 'merchant', title: '가맹점 운영 메뉴얼' },
    { id: 'merchant-chatbot', audience: 'merchant', title: '챗봇결제 가맹점 사용 메뉴얼' }
  ];

  function uiT(s) {
    if (global.PG_UI_I18N && typeof global.PG_UI_I18N.t === 'function') return global.PG_UI_I18N.t(String(s));
    return String(s);
  }

  function esc(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/"/g, '&quot;');
  }

  function liveVersion() {
    var r = global.ICOPAY_PLATFORM_RELEASE;
    if (r && r.currentLiveVersion) return String(r.currentLiveVersion);
    return FALLBACK_VERSION;
  }

  function adminLang() {
    try {
      var L = (global.PG_UI_I18N && global.PG_UI_I18N.getLang && global.PG_UI_I18N.getLang()) || 'KO';
      L = String(L).toUpperCase();
      if (L === 'EN') return 'en';
      if (L === 'JP' || L === 'JA') return 'ja';
      if (L === 'CH' || L === 'ZH') return 'zh';
      if (L === 'TH') return 'th';
      return 'ko';
    } catch (e) {
      return 'ko';
    }
  }

  function sessionOrgLevel() {
    try {
      var u = JSON.parse(global.sessionStorage.getItem('pg_admin_user') || '{}') || {};
      if (String(u.role || '').toUpperCase() === 'ADMIN') return 'HEADQUARTERS';
      return String(u.orgLevel || '').toUpperCase();
    } catch (e) {
      return '';
    }
  }

  function orgLevelCode(ol) {
    var k = String(ol || '').toUpperCase();
    if (ORG_LEVEL_CODE[k] != null) return ORG_LEVEL_CODE[k];
    return 1;
  }

  function allowedAudiencesForOrgLevel(ol) {
    var code = orgLevelCode(ol);
    if (code <= 1) return AUDIENCE_ORDER.slice();
    if (code <= 3) return ['hqdist', 'merchant'];
    return ['merchant'];
  }

  function normalizeAllowedAudiences(raw, fallbackOl) {
    if (Array.isArray(raw) && raw.length) {
      var set = {};
      raw.forEach(function (a) { set[String(a)] = true; });
      return AUDIENCE_ORDER.filter(function (a) { return set[a]; });
    }
    return allowedAudiencesForOrgLevel(fallbackOl || sessionOrgLevel());
  }

  /** blob 창에서는 상대 로고 URL이 깨지므로 절대 URL로 변환 */
  function resolveBrandLogoUrl(logo) {
    var u = String(logo || '').trim();
    if (!u) return '';
    if (/^(https?:|data:|blob:)/i.test(u)) return u;
    if (u.indexOf('//') === 0) {
      try {
        return (global.location.protocol || 'https:') + u;
      } catch (e0) {
        return 'https:' + u;
      }
    }
    var pageOrigin = '';
    var apiOrigin = '';
    try { pageOrigin = (global.location && global.location.origin) || ''; } catch (e1) { /* ignore */ }
    try {
      if (global.PG_API && typeof global.PG_API.getBaseUrl === 'function') {
        apiOrigin = String(global.PG_API.getBaseUrl() || '');
      }
    } catch (e2) { /* ignore */ }
    if (u.charAt(0) === '/') {
      if (u.indexOf('/api/') === 0 && apiOrigin) return apiOrigin.replace(/\/$/, '') + u;
      if (pageOrigin) return pageOrigin.replace(/\/$/, '') + u;
      if (apiOrigin) return apiOrigin.replace(/\/$/, '') + u;
      return u;
    }
    var base = pageOrigin || apiOrigin;
    if (base) return base.replace(/\/$/, '') + '/' + u.replace(/^\.\//, '');
    return u;
  }

  /** PDF 표지형 전체 브랜드 마크 — 사이드바용 작은 로고는 맨 뒤 */
  function pickManualLogoRaw(brand) {
    var b = brand || {};
    /* 첫화면·URL결제 로고(전체 마크) 우선. 사이드바 로고는 작아서 PDF와 다름 */
    return b.firstLogoImageUrl || b.urlPayImageUrl || b.manualLogoImageUrl || '';
  }

  function brandFields(brand) {
    var b = brand || {};
    return {
      logo: resolveBrandLogoUrl(pickManualLogoRaw(b) || b.logoImageUrl || ''),
      site: b.siteName || b.compNm || 'ICOPAY',
      comp: b.compNm || b.siteName || 'ICOPAY',
      addr: b.addr || '',
      tel: b.compTel || '',
      email: b.email || '',
      copy: b.copyright || ''
    };
  }

  function arrayBufferToDataUrl(buf, contentType) {
    var u8 = new Uint8Array(buf || []);
    var chunk = 0x8000;
    var binary = '';
    for (var i = 0; i < u8.length; i += chunk) {
      binary += String.fromCharCode.apply(null, u8.subarray(i, Math.min(i + chunk, u8.length)));
    }
    return 'data:' + (contentType || 'image/png') + ';base64,' + btoa(binary);
  }

  /** blob 창에서 상대/쿠키 이슈 없이 보이도록 로고를 data URL 로 변환 */
  function fetchLogoDataUrl(logoUrl) {
    var abs = resolveBrandLogoUrl(logoUrl);
    if (!abs) return Promise.resolve('');
    if (/^data:/i.test(abs)) return Promise.resolve(abs);
    var headers = {};
    try {
      var tok = global.sessionStorage && global.sessionStorage.getItem('pg_admin_token');
      if (tok) headers.Authorization = 'Bearer ' + tok;
    } catch (eT) { /* ignore */ }
    return fetch(abs, { credentials: 'include', mode: 'cors', cache: 'no-store', headers: headers })
      .then(function (res) {
        if (!res.ok) throw new Error('logo http ' + res.status);
        return res.arrayBuffer().then(function (buf) {
          return arrayBufferToDataUrl(buf, res.headers.get('Content-Type') || 'image/png');
        });
      })
      .catch(function () { return ''; });
  }

  /** PDF 표지와 동일한 커버 로고 PNG만 사용(사이드바용 작은 로고 제외) */
  function resolveViewerLogoDataUrl(manualId) {
    if (!global.PG_API || typeof global.PG_API.hqPlatformManualsCoverLogo !== 'function') {
      return Promise.resolve('');
    }
    return global.PG_API.hqPlatformManualsCoverLogo(manualId).then(function (r) {
      if (!r || !r.buf || r.buf.byteLength < 24) throw new Error('empty');
      return arrayBufferToDataUrl(r.buf, r.contentType || 'image/png');
    }).catch(function () { return ''; });
  }

  function isManualHtml(text) {
    var t = String(text || '');
    if (t.length < 200) return false;
    var low = t.toLowerCase();
    if (low.indexOf('id="side-nav-ul"') >= 0 || low.indexOf("id='side-nav-ul'") >= 0) return false;
    if (low.indexOf('contentsmain') >= 0 || low.indexOf('pg_admin_user') >= 0) return false;
    return low.indexOf('page-wrap') >= 0 || low.indexOf('print-btn') >= 0 || low.indexOf('__brand_site_name__') >= 0;
  }

  function applyBrand(html, brand, logoOverride) {
    var b = brand || {};
    var f = brandFields(b);
    var logo = logoOverride || f.logo || '';
    var map = {
      '__BRAND_LOGO_URL__': logo,
      '__BRAND_SITE_NAME__': f.site,
      '__BRAND_COMP_NM__': f.comp,
      '__BRAND_ADDR__': f.addr,
      '__BRAND_TEL__': f.tel,
      '__BRAND_EMAIL__': f.email,
      '__BRAND_COPYRIGHT__': f.copy || ('© ' + f.site)
    };
    var out = String(html || '');
    Object.keys(map).forEach(function (k) {
      out = out.split(k).join(String(map[k]));
    });
    return out;
  }

  function loadingHtml(title) {
    return '<!DOCTYPE html><html><head><meta charset="utf-8"><title>' + esc(title || '') +
      '</title></head><body style="font-family:system-ui,sans-serif;padding:24px;color:#455a64">' +
      '<p>' + esc(uiT('매뉴얼을 불러오는 중…')) + '</p></body></html>';
  }

  function errorHtml(msg) {
    return '<!DOCTYPE html><html><head><meta charset="utf-8"><title>Error</title></head>' +
      '<body style="font-family:system-ui,sans-serif;padding:24px;color:#c62828"><p>' +
      esc(msg) + '</p></body></html>';
  }

  /**
   * 예제·PDF 표지와 같이 좌측 상단에 전체 브랜드 로고(마크) 표시
   * @param {string} logoDataUrl 이미 data:/https URL
   */
  function brandedPdfViewerHtml(title, brand, pdfBlobUrl, logoDataUrl) {
    var f = brandFields(brand);
    var logoSrc = logoDataUrl || f.logo;
    var contact = [f.tel, f.email].filter(Boolean).join(' · ');
    var logoBlock = logoSrc
      ? '<img class="logo" src="' + esc(logoSrc) + '" alt="' + esc(f.site) + '" onerror="this.style.display=\'none\';var n=this.nextElementSibling;if(n)n.style.display=\'flex\'">' +
        '<div class="logo-fallback" style="display:none">' + esc(f.site) + '</div>'
      : '<div class="logo-fallback">' + esc(f.site) + '</div>';
    /* 로고 이미지에 브랜드명이 포함되므로 상호명(OTL HQ)은 중복 표시하지 않음 */
    var metaHtml = logoSrc
      ? ('<div class="doc-title">' + esc(title) + '</div>' +
         '<div class="doc-ver">V' + esc(liveVersion()) + '</div>' +
         (f.addr ? '<div class="sub">' + esc(f.addr) + '</div>' : '') +
         (contact ? '<div class="sub">' + esc(contact) + '</div>' : ''))
      : ('<div class="nm">' + esc(f.comp) + '</div>' +
         '<div class="doc-title">' + esc(title) + '</div>' +
         '<div class="doc-ver">V' + esc(liveVersion()) + '</div>' +
         (f.addr ? '<div class="sub">' + esc(f.addr) + '</div>' : '') +
         (contact ? '<div class="sub">' + esc(contact) + '</div>' : ''));
    return '<!DOCTYPE html><html lang="ko"><head><meta charset="utf-8">' +
      '<meta name="viewport" content="width=device-width,initial-scale=1">' +
      '<title>' + esc(title) + '</title>' +
      '<style>' +
      'html,body{height:100%;margin:0;background:#e8ecf2;font-family:"Malgun Gothic","Segoe UI","Noto Sans",sans-serif}' +
      '.bar{display:flex;align-items:center;gap:20px;padding:12px 20px;background:#fff;border-bottom:1px solid #cfd8dc;' +
      'box-shadow:0 1px 4px rgba(0,0,0,.06);min-height:80px}' +
      '.brand-logo{flex:0 0 auto;display:flex;align-items:center;background:#fff;border:1px solid #e6eaf0;border-radius:10px;padding:8px 14px;box-shadow:0 1px 2px rgba(0,0,0,.04)}' +
      '.logo{height:48px;width:auto;max-width:280px;object-fit:contain;object-position:left center;display:block}' +
      '.logo-fallback{font-weight:800;font-size:18px;color:#1a3a5c;letter-spacing:.02em;display:flex;align-items:center}' +
      '.meta{flex:1;min-width:0}' +
      '.meta .nm{font-weight:800;font-size:15px;color:#1a3a5c}' +
      '.meta .doc-title{font-weight:700;font-size:14px;color:#263238}' +
      '.meta .doc-ver{font-size:11px;color:#78909c;margin-top:2px}' +
      '.meta .sub{font-size:11px;color:#546e7a;margin-top:2px;line-height:1.4;word-break:break-word}' +
      '.frame-wrap{height:calc(100% - 82px);background:#525659}' +
      'iframe{width:100%;height:100%;border:0;background:#525659}' +
      '@media print{.bar{box-shadow:none}}' +
      '</style></head><body>' +
      '<header class="bar">' +
      '<div class="brand-logo">' + logoBlock + '</div>' +
      '<div class="meta">' + metaHtml + '</div></header>' +
      '<div class="frame-wrap"><iframe title="' + esc(title) + '" src="' + esc(pdfBlobUrl) + '"></iframe></div>' +
      '</body></html>';
  }

  function showHtmlInWindow(win, html) {
    if (!win || win.closed) return;
    var blob = new Blob([html], { type: 'text/html;charset=utf-8' });
    var blobUrl = URL.createObjectURL(blob);
    try {
      win.location.replace(blobUrl);
    } catch (eLoc) {
      try { win.location.href = blobUrl; } catch (eHref) { /* ignore */ }
    }
    setTimeout(function () {
      try { URL.revokeObjectURL(blobUrl); } catch (eR) { /* ignore */ }
    }, 180000);
  }

  function renderListHtml(allowedAudiences) {
    var ver = liveVersion();
    var allowed = normalizeAllowedAudiences(allowedAudiences, sessionOrgLevel());
    var html = '';
    var any = false;
    AUDIENCE_ORDER.forEach(function (aud) {
      if (allowed.indexOf(aud) < 0) return;
      var rows = ITEMS.filter(function (it) { return it.audience === aud; });
      if (!rows.length) return;
      any = true;
      html += '<div class="mb-3 icopay-pm-aud" data-audience="' + esc(aud) + '">';
      html += '<div class="fw-semibold small text-uppercase text-muted mb-2" data-pg-ui-t="' +
        esc(AUDIENCE_LABEL[aud]) + '">' + esc(uiT(AUDIENCE_LABEL[aud])) + '</div>';
      html += '<div class="list-group list-group-flush border rounded">';
      rows.forEach(function (it) {
        html += '<button type="button" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center icopay-pm-open" data-id="' +
          esc(it.id) + '" title="' + esc(uiT('새 창에서 열기')) + '">' +
          '<span data-pg-ui-t="' + esc(it.title) + '">' + esc(uiT(it.title)) + '</span>' +
          '<span class="d-flex align-items-center gap-2">' +
          '<span class="badge text-bg-light">V' + esc(ver) + '</span>' +
          '<span class="badge text-bg-primary" data-pg-ui-t="PDF">' + esc(uiT('PDF')) + '</span>' +
          '<span class="badge text-bg-secondary" data-pg-ui-t="새 창">' + esc(uiT('새 창')) + '</span>' +
          '</span></button>';
      });
      html += '</div></div>';
    });
    if (!any) {
      html += '<div class="alert alert-light border small mb-0" data-pg-ui-t="이 조직 단계에서 열람 가능한 운영 메뉴얼이 없습니다.">' +
        esc(uiT('이 조직 단계에서 열람 가능한 운영 메뉴얼이 없습니다.')) + '</div>';
    }
    return html;
  }

  function renderShell(allowedAudiences) {
    var ver = liveVersion();
    var html = '';
    html += '<div class="icopay-platform-manuals" id="icopayPlatformManualsRoot">';
    html += '<div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">';
    html += '<div><div class="fw-semibold" data-pg-ui-t="운영 메뉴얼">' + esc(uiT('운영 메뉴얼')) + '</div>';
    html += '<div class="small text-muted" data-pg-ui-t="플랫폼 라이브 버전과 동일하게 관리됩니다.">' +
      esc(uiT('플랫폼 라이브 버전과 동일하게 관리됩니다.')) +
      ' · <span class="fw-semibold">V' + esc(ver) + '</span></div>';
    html += '<div class="small text-muted mt-1" data-pg-ui-t="로그인 조직 단계 이하의 매뉴얼만 표시됩니다.">' +
      esc(uiT('로그인 조직 단계 이하의 매뉴얼만 표시됩니다.')) + '</div>';
    html += '<div class="small text-muted mt-1" data-pg-ui-t="항목을 클릭하면 새 창에서 PDF 매뉴얼이 열립니다. 좌측 상단에 PDF 표지와 동일한 로고가 표시됩니다.">' +
      esc(uiT('항목을 클릭하면 새 창에서 PDF 매뉴얼이 열립니다. 좌측 상단에 PDF 표지와 동일한 로고가 표시됩니다.')) +
      '</div></div>';
    /* 관리자 셸 언어칩과 동일 순서·표기: JP KR EN CH TH (내부 코드는 ja/ko/en/zh/th) */
    html += '<div class="btn-group btn-group-sm" role="group" aria-label="lang">';
    [
      { code: 'ja', lab: 'JP' },
      { code: 'ko', lab: 'KR' },
      { code: 'en', lab: 'EN' },
      { code: 'zh', lab: 'CH' },
      { code: 'th', lab: 'TH' }
    ].forEach(function (lg) {
      html += '<button type="button" class="btn btn-outline-secondary icopay-pm-lang" data-lang="' + lg.code + '">' + lg.lab + '</button>';
    });
    html += '</div></div>';
    html += '<div id="icopayPmList">' + renderListHtml(allowedAudiences) + '</div>';
    html += '</div>';
    return html;
  }

  function bind(root) {
    if (!root || root._icopayPmBound) return;
    root._icopayPmBound = true;
    var state = {
      lang: adminLang(),
      brand: null,
      allowedAudiences: allowedAudiencesForOrgLevel(sessionOrgLevel()),
      _pdfBlobUrls: []
    };

    function setLangActive() {
      root.querySelectorAll('.icopay-pm-lang').forEach(function (btn) {
        var on = btn.getAttribute('data-lang') === state.lang;
        btn.classList.toggle('btn-secondary', on);
        btn.classList.toggle('btn-outline-secondary', !on);
      });
    }

    function refreshList() {
      var list = root.querySelector('#icopayPmList');
      if (list) list.innerHTML = renderListHtml(state.allowedAudiences);
      if (global.PG_UI_I18N && typeof global.PG_UI_I18N.applyDom === 'function') {
        try { global.PG_UI_I18N.applyDom(list || root); } catch (eUi) { /* ignore */ }
      }
    }

    function itemAllowed(id) {
      var item = ITEMS.filter(function (x) { return x.id === id; })[0];
      if (!item) return false;
      return state.allowedAudiences.indexOf(item.audience) >= 0;
    }

    function loadBrand() {
      if (!global.PG_API || typeof global.PG_API.hqPlatformManualsBrand !== 'function') {
        return Promise.resolve({});
      }
      return global.PG_API.hqPlatformManualsBrand().then(function (d) {
        state.brand = d || {};
        if (d && d.viewerOrgLevel) {
          state.allowedAudiences = normalizeAllowedAudiences(d.allowedAudiences, d.viewerOrgLevel);
        } else if (d && d.allowedAudiences) {
          state.allowedAudiences = normalizeAllowedAudiences(d.allowedAudiences, sessionOrgLevel());
        }
        refreshList();
        return state.brand;
      }).catch(function () {
        state.brand = {};
        return state.brand;
      });
    }

    function openManualInNewWindow(id) {
      if (!itemAllowed(id)) {
        try { global.alert(uiT('이 조직 단계에서는 해당 매뉴얼을 열 수 없습니다.')); } catch (eA) { /* ignore */ }
        return;
      }
      var item = ITEMS.filter(function (x) { return x.id === id; })[0];
      var title = uiT(item ? item.title : id);
      var win = global.open('about:blank', '_blank');
      if (!win) {
        try {
          global.alert(uiT('팝업이 차단되었습니다. 이 사이트에 대해 팝업을 허용한 뒤 다시 시도해 주세요.'));
        } catch (eP) { /* ignore */ }
        return;
      }
      try { win.opener = null; } catch (eO) { /* ignore */ }
      showHtmlInWindow(win, loadingHtml(title));

      var lang = state.lang || 'ko';

      Promise.resolve(state.brand || loadBrand()).then(function () {
        if (!global.PG_API || typeof global.PG_API.hqPlatformManualsPdf !== 'function') {
          throw new Error(uiT('매뉴얼 PDF API를 사용할 수 없습니다.'));
        }
        return Promise.all([
          global.PG_API.hqPlatformManualsPdf(id, lang),
          resolveViewerLogoDataUrl(id)
        ]);
      }).then(function (pair) {
        var buf = pair[0];
        var logoDataUrl = pair[1] || '';
        if (!buf || !(buf instanceof ArrayBuffer) || buf.byteLength < 8) {
          throw new Error(uiT('매뉴얼 PDF를 불러올 수 없습니다.'));
        }
        var u8 = new Uint8Array(buf);
        if (!(u8[0] === 0x25 && u8[1] === 0x50 && u8[2] === 0x44 && u8[3] === 0x46)) {
          throw new Error(uiT('매뉴얼 본문이 올바르지 않습니다.'));
        }
        var pdfBlob = new Blob([buf], { type: 'application/pdf' });
        var pdfUrl = URL.createObjectURL(pdfBlob);
        state._pdfBlobUrls.push(pdfUrl);
        showHtmlInWindow(win, brandedPdfViewerHtml(title, state.brand, pdfUrl, logoDataUrl));
        setTimeout(function () {
          try { URL.revokeObjectURL(pdfUrl); } catch (eR) { /* ignore */ }
        }, 300000);
      }).catch(function (err) {
        var msg = (err && err.message) ? String(err.message) : uiT('매뉴얼을 불러올 수 없습니다.');
        showHtmlInWindow(win, errorHtml(msg));
      });
    }

    setLangActive();
    loadBrand();

    root.addEventListener('click', function (ev) {
      var langBtn = ev.target && ev.target.closest ? ev.target.closest('.icopay-pm-lang') : null;
      if (langBtn) {
        state.lang = langBtn.getAttribute('data-lang') || 'ko';
        setLangActive();
        return;
      }
      var openBtn = ev.target && ev.target.closest ? ev.target.closest('.icopay-pm-open') : null;
      if (openBtn) {
        openManualInNewWindow(openBtn.getAttribute('data-id'));
      }
    });
  }

  function mount(root) {
    if (!root) return;
    root.innerHTML = renderShell(allowedAudiencesForOrgLevel(sessionOrgLevel()));
    bind(root.querySelector('#icopayPlatformManualsRoot') || root);
  }

  function renderHtml() {
    return renderShell(allowedAudiencesForOrgLevel(sessionOrgLevel()));
  }

  function ensureBound(root) {
    if (!root) return;
    if (!root.querySelector || !root.id) {
      var inner = root.querySelector ? root.querySelector('#icopayPlatformManualsRoot') : null;
      if (inner) bind(inner);
      else if (root.id === 'icopayPlatformManualsRoot') bind(root);
      return;
    }
    if (root.id === 'icopayPlatformManualsRoot') bind(root);
    else {
      var r = root.querySelector('#icopayPlatformManualsRoot');
      if (r) bind(r);
    }
  }

  global.ICOPAY_PLATFORM_MANUALS = {
    items: ITEMS,
    renderHtml: renderHtml,
    mount: mount,
    ensureBound: ensureBound,
    liveVersion: liveVersion,
    allowedAudiencesForOrgLevel: allowedAudiencesForOrgLevel,
    audienceMinOrgCode: AUDIENCE_MIN_ORG_CODE
  };
}(typeof window !== 'undefined' ? window : this));
