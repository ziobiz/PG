/**
 * 운영관리 > 운영매뉴얼
 * 목록 클릭 → 새 창(탭)에서 HTML 열기 → 브라우저에서 열람·인쇄/PDF 저장
 * 버전 = ICOPAY_PLATFORM_RELEASE.currentLiveVersion
 * 브랜드 = GET /api/hq/platformManuals/brand (총본사 기본정보)
 * 노출 = 로그인 조직 단계 이하 audience 만
 */
(function (global) {
  'use strict';

  var FALLBACK_VERSION = '2.47';

  var AUDIENCE_ORDER = ['super', 'hqdist', 'merchant'];
  var AUDIENCE_LABEL = {
    super: '총본사용',
    hqdist: '본사 및 총판용',
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

  var ITEMS = [
    { id: 'super-ops', audience: 'super', title: '총본사 운영 메뉴얼' },
    { id: 'super-org-reg', audience: 'super', title: '신규 조직 등록 메뉴얼' },
    { id: 'super-risk', audience: 'super', title: '리스크관리 메뉴얼' },
    { id: 'hqdist-ops', audience: 'hqdist', title: '본사 및 총판 운영 메뉴얼' },
    { id: 'hqdist-merchant-add', audience: 'hqdist', title: '신규가맹점 추가 메뉴얼' },
    { id: 'hqdist-chatbot', audience: 'hqdist', title: '챗봇결제 운영 메뉴얼' },
    { id: 'hqdist-subscription', audience: 'hqdist', title: '정기결제 운영 메뉴얼' },
    { id: 'hqdist-split', audience: 'hqdist', title: '분할결제 운영 메뉴얼' },
    { id: 'hqdist-risk-intro', audience: 'hqdist', title: '리스크 트리거 발동 소개 안내' },
    { id: 'merchant-user', audience: 'merchant', title: '가맹점 유저 메뉴얼' }
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

  function manualUrl(id, lang) {
    var rel = 'manuals/generated/' + id + '-' + lang + '.html';
    try {
      return new URL(rel, global.location.href).href;
    } catch (e) {
      return rel;
    }
  }

  function applyBrand(html, brand) {
    var b = brand || {};
    var logo = b.logoImageUrl || b.firstLogoImageUrl || '';
    var site = b.siteName || b.compNm || 'ICOPAY';
    var comp = b.compNm || site;
    var addr = b.addr || '';
    var tel = b.compTel || '';
    var email = b.email || '';
    var copy = b.copyright || ('© ' + site);
    var map = {
      '__BRAND_LOGO_URL__': logo,
      '__BRAND_SITE_NAME__': site,
      '__BRAND_COMP_NM__': comp,
      '__BRAND_ADDR__': addr,
      '__BRAND_TEL__': tel,
      '__BRAND_EMAIL__': email,
      '__BRAND_COPYRIGHT__': copy
    };
    var out = String(html || '');
    Object.keys(map).forEach(function (k) {
      out = out.split(k).join(String(map[k]));
    });
    return out;
  }

  function writeWindowHtml(win, html) {
    if (!win || win.closed) return;
    try {
      win.document.open();
      win.document.write(html);
      win.document.close();
    } catch (eW) {
      try {
        var blob = new Blob([html], { type: 'text/html;charset=utf-8' });
        var blobUrl = URL.createObjectURL(blob);
        win.location.href = blobUrl;
        setTimeout(function () {
          try { URL.revokeObjectURL(blobUrl); } catch (eR) { /* ignore */ }
        }, 60000);
      } catch (eB) { /* ignore */ }
    }
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
          '<span class="badge text-bg-secondary" data-pg-ui-t="새 창">' + esc(uiT('새 창')) + '</span>' +
          '</span></button>';
      });
      html += '</div></div>';
    });
    if (!any) {
      html += '<div class="alert alert-light border small mb-0" data-pg-ui-t="이 조직 단계에서 열람 가능한 운영매뉴얼이 없습니다.">' +
        esc(uiT('이 조직 단계에서 열람 가능한 운영매뉴얼이 없습니다.')) + '</div>';
    }
    return html;
  }

  function renderShell(allowedAudiences) {
    var ver = liveVersion();
    var html = '';
    html += '<div class="icopay-platform-manuals" id="icopayPlatformManualsRoot">';
    html += '<div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">';
    html += '<div><div class="fw-semibold" data-pg-ui-t="운영매뉴얼">' + esc(uiT('운영매뉴얼')) + '</div>';
    html += '<div class="small text-muted" data-pg-ui-t="플랫폼 라이브 버전과 동일하게 관리됩니다.">' +
      esc(uiT('플랫폼 라이브 버전과 동일하게 관리됩니다.')) +
      ' · <span class="fw-semibold">V' + esc(ver) + '</span></div>';
    html += '<div class="small text-muted mt-1" data-pg-ui-t="로그인 조직 단계 이하의 매뉴얼만 표시됩니다.">' +
      esc(uiT('로그인 조직 단계 이하의 매뉴얼만 표시됩니다.')) + '</div>';
    html += '<div class="small text-muted mt-1" data-pg-ui-t="항목을 클릭하면 새 창에서 HTML 매뉴얼이 열립니다. 브라우저에서 인쇄하거나 PDF로 저장할 수 있습니다.">' +
      esc(uiT('항목을 클릭하면 새 창에서 HTML 매뉴얼이 열립니다. 브라우저에서 인쇄하거나 PDF로 저장할 수 있습니다.')) +
      '</div></div>';
    html += '<div class="btn-group btn-group-sm" role="group" aria-label="lang">';
    ['ko', 'en', 'ja', 'zh', 'th'].forEach(function (lg) {
      var lab = lg === 'ko' ? 'KO' : lg === 'en' ? 'EN' : lg === 'ja' ? 'JP' : lg === 'zh' ? 'CH' : 'TH';
      html += '<button type="button" class="btn btn-outline-secondary icopay-pm-lang" data-lang="' + lg + '">' + lab + '</button>';
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
      allowedAudiences: allowedAudiencesForOrgLevel(sessionOrgLevel())
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
      var title = uiT(item ? item.title : id) + ' · V' + liveVersion();
      /* 클릭 동기 시점에 창을 열어 팝업 차단을 피함 */
      var win = global.open('about:blank', '_blank');
      if (!win) {
        try {
          global.alert(uiT('팝업이 차단되었습니다. 이 사이트에 대해 팝업을 허용한 뒤 다시 시도해 주세요.'));
        } catch (eP) { /* ignore */ }
        return;
      }
      try { win.opener = null; } catch (eO) { /* ignore */ }
      writeWindowHtml(win, loadingHtml(title));

      var url = manualUrl(id, state.lang);
      Promise.resolve(state.brand || loadBrand()).then(function () {
        return fetch(url, { credentials: 'same-origin', cache: 'no-store' }).then(function (r) {
          if (!r.ok) throw new Error('HTTP ' + r.status);
          return r.text();
        });
      }).then(function (text) {
        var branded = applyBrand(text, state.brand);
        writeWindowHtml(win, branded);
        try { win.document.title = title; } catch (eT) { /* ignore */ }
      }).catch(function () {
        writeWindowHtml(win, errorHtml(
          uiT('매뉴얼을 불러올 수 없습니다.') + ' (' + url + ')'
        ));
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
