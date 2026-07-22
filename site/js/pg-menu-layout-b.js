/**
 * B안 메뉴 — 허브 정의·사이드바 교체·URL 리다이렉트·권한 alias.
 */
(function (global) {
  'use strict';

  var cfg = global.PG_MENU_LAYOUT_CONFIG;
  if (!cfg || !cfg.isLayoutB()) {
    global.PG_MENU_LAYOUT_B = {
      enabled: false,
      resolveNavUrl: function (url) { return url; },
      hubForUrl: function () { return null; },
      leafUrlsForHub: function () { return []; },
      canonicalPermissionUrlOrder: function () { return []; },
      leafDisplayMeta: function () { return null; },
      leafLabelKo: function () { return null; },
      supplementLeafUrlOrder: function (urlIdx, startG) { return startG; },
      applyCanonicalPermissionUrlOrder: function () { return 0; },
      permissionAliases: function (url) { return [url]; },
      applySidebar: function () { /* noop */ },
      parentGroupForUrl: function (url, fallback) { return fallback; }
    };
    return;
  }

  var HUBS = [
    {
      id: 'policy-fees',
      hubUrl: '/hq/hub/policy-fees',
      menuId: 'M0151',
      sidebarLabel: '수수료·리스크',
      parent: '본사정책',
      icon: 'bi-percent',
      tabs: [
        { tab: 'commission', url: '/hq/defaultCommission', menuId: 'M0102', label: '수수료' },
        { tab: 'agency-cost', url: '/hq/pgAgencyCostPolicy', menuId: 'M0128', label: '대행수수료' },
        { tab: 'chargeback', url: '/hq/chargebackPolicy', menuId: 'M0117', label: '차지백' },
        { tab: 'risk', url: '/hq/riskCardPolicy', menuId: 'M0129', label: '리스크' }
      ]
    },
    {
      id: 'payment-channel',
      hubUrl: '/hq/hub/payment-channel',
      menuId: 'M0152',
      sidebarLabel: '결제·URL',
      parent: '본사정책',
      icon: 'bi-credit-card-2-front',
      tabs: [
        { tab: 'routing', url: '/hq/paymentOrchestration', menuId: 'M0118', label: '결제 라우팅' },
        { tab: 'urlpay', url: '/hq/urlPayDeploy', menuId: 'M0122', label: 'URL결제' },
        { tab: 'tablet-ux', url: '/hq/opsModeMng', menuId: 'M0127', label: '태블릿 UX' }
      ]
    },
    {
      id: 'notify',
      hubUrl: '/hq/hub/notify',
      menuId: 'M0153',
      sidebarLabel: '노티 센터',
      parent: '본사정책',
      icon: 'bi-broadcast',
      tabs: [
        { tab: 'env', url: '/hq/notifyEnv', menuId: 'M0105', label: '노티 구성' },
        { tab: 'mapping', url: '/hq/notifyMapping', menuId: 'M0107', label: '필드 매핑' },
        { tab: 'inbound', url: '/hq/notifyInbound', menuId: 'M0121', label: '수령 로그' }
      ]
    },
    {
      id: 'settlement',
      hubUrl: '/hq/hub/settlement',
      menuId: 'M0154',
      sidebarLabel: '정산·영업일',
      parent: '본사정책',
      icon: 'bi-calendar-check',
      tabs: [
        { tab: 'cycle', url: '/hq/settlementAdmin', menuId: 'M0123', label: '정산주기' },
        { tab: 'recovery', url: '/hq/receivableRecoverySettings', menuId: 'M0124', label: '환수·미수금' },
        { tab: 'bizday', url: '/hq/businessDaySetting', menuId: 'M0109', label: '영업일' }
      ]
    },
    {
      id: 'org-view',
      hubUrl: '/hq/hub/org-view',
      menuId: 'M0155',
      sidebarLabel: '조직·화면',
      parent: '본사정책',
      icon: 'bi-layout-three-columns',
      tabs: [
        { tab: 'columns', url: '/hq/orgViewColumnAllowance', menuId: 'M0108', label: '조직항목' },
        { tab: 'grid-order', url: '/set/gridSetMng', menuId: 'M0505', label: '항목순서' }
      ]
    },
    {
      id: 'access',
      hubUrl: '/hq/hub/access',
      menuId: 'M0157',
      sidebarLabel: '접근·권한',
      parent: '본사정책',
      icon: 'bi-shield-lock',
      tabs: [
        { tab: 'permission', url: '/hq/permissionMng', menuId: 'M0104', label: '본사 권한' },
        { tab: 'user', url: '/hq/userSettings', menuId: 'M0120', label: '사용자' },
        { tab: 'account', url: '/hq/accountMng', menuId: 'M0106', label: '업체 접근' }
      ]
    }
  ];

  /** 플랫폼 — 본사정책 핵심 베이스 설정(AI·챗봇 바로 아래) */
  var PLATFORM_HUB = {
    id: 'platform',
    hubUrl: '/hq/hub/platform',
    menuId: 'M0156',
    sidebarLabel: '플랫폼',
    parent: '본사정책',
    icon: 'bi-hdd-stack',
    tabs: [
      { tab: 'ledger', url: '/hq/ledgerSysSettings', menuId: 'M0119', label: '전산·동기화' },
      { tab: 'domain', url: '/hq/domainConfig', menuId: 'M0115', label: '도메인·SSL' },
      { tab: 'server', url: '/hq/serverManage', menuId: 'M0116', label: '서버' },
      { tab: 'releases', url: '/hq/platformReleaseNotes', menuId: 'M0156', label: '업데이트 내용' }
    ]
  };

  var STANDALONE_HQ = [
    { url: '/hq/chatbotAiSettings', menuId: 'M0126', label: 'AI·챗봇', parent: '본사정책' }
  ];

  var DEPLOY_STANDALONE = [
    { url: '/hq/pgApiMng', menuId: 'M0101', label: 'PG사 연동', parent: '연동·배포' }
  ];

  var MERCHANT_API_HUB = {
    id: 'merchant-api',
    hubUrl: '/hq/hub/merchant-api',
    menuId: 'M0158',
    sidebarLabel: '가맹 API 출시',
    parent: '연동·배포',
    wizard: true,
    steps: [
      { step: 'common', url: '/hq/apiConfig', menuId: 'M0103', label: '① 공통설정' },
      { step: 'register', url: '/hq/apiMerchantDeployReg', menuId: 'M0906', label: '② 가맹 등록' },
      { step: 'issue', url: '/hq/merchantApiGenerate', menuId: 'M0905', label: '③ 키·문서' },
      { step: 'docs', url: '/hq/merchantApiDeployDocs', menuId: 'M0907', label: 'API 문서' },
      { step: 'guide', url: '/deploy/launchGuide', menuId: 'M0904', label: '출시 가이드' }
    ],
    /** 출시 가이드 탭 내부 서브패널 (우측 사이드바 대신 전폭 탭) */
    guidePanels: [
      { panel: 'checklist', deployUrl: '/deploy/launchChecklist', menuId: 'M0904', label: '배포 체크리스트' },
      { panel: 'integration', deployUrl: '/deploy/integrationPlan', menuId: 'M0901', label: '연동 진행안' },
      { panel: 'jpay', deployUrl: '/deploy/jpayWorkPlan', menuId: 'M0902', label: 'JPAY 전용 연동' },
      { panel: 'policy', deployUrl: '/deploy/merchantApiPolicy', menuId: 'M0903', label: 'API 배포 정책' }
    ]
  };

  var hubByUrl = {};
  var leafToHub = {};
  var leafRedirect = {};

  function registerHub(h) {
    hubByUrl[h.hubUrl] = h;
    (h.tabs || []).forEach(function (t) {
      leafToHub[t.url] = { hub: h, tab: t.tab, step: t.step };
      leafRedirect[t.url] = h.hubUrl + (h.wizard ? '?step=' + encodeURIComponent(t.step || t.tab) : '?tab=' + encodeURIComponent(t.tab));
    });
    (h.steps || []).forEach(function (s) {
      leafToHub[s.url] = { hub: h, step: s.step };
      leafRedirect[s.url] = h.hubUrl + '?step=' + encodeURIComponent(s.step);
    });
  }

  HUBS.forEach(registerHub);
  registerHub(PLATFORM_HUB);
  registerHub(MERCHANT_API_HUB);

  leafRedirect['/deploy/launchChecklist'] = '/hq/hub/merchant-api?step=guide&panel=checklist';
  leafRedirect['/ops/launchChecklist'] = '/hq/hub/merchant-api?step=guide&panel=checklist';
  leafRedirect['/deploy/integrationPlan'] = '/hq/hub/merchant-api?step=guide&panel=integration';
  leafRedirect['/ops/integrationPlan'] = '/hq/hub/merchant-api?step=guide&panel=integration';
  leafRedirect['/deploy/jpayWorkPlan'] = '/hq/hub/merchant-api?step=guide&panel=jpay';
  leafRedirect['/ops/jpayWorkPlan'] = '/hq/hub/merchant-api?step=guide&panel=jpay';
  leafRedirect['/deploy/merchantApiPolicy'] = '/hq/hub/merchant-api?step=guide&panel=policy';
  leafRedirect['/ops/merchantApiPolicy'] = '/hq/hub/merchant-api?step=guide&panel=policy';
  leafRedirect['/deploy/launchGuide'] = '/hq/hub/merchant-api?step=guide';
  leafRedirect['/hq/platformOpsManuals'] = '/ops/opsManuals';

  function uiT(s) {
    if (global.PG_UI_I18N && typeof global.PG_UI_I18N.t === 'function') return global.PG_UI_I18N.t(String(s));
    return String(s);
  }

  function escAttr(s) {
    return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
  }

  function buildChildLi(entry) {
    var label = uiT(entry.sidebarLabel || entry.label);
    return '<li class="child-li" data-url="' + escAttr(entry.hubUrl || entry.url) + '"'
      + (entry.menuId ? ' data-menu_id="' + escAttr(entry.menuId) + '"' : '')
      + ' data-pg-hub="' + (entry.hubUrl ? '1' : '0') + '">'
      + '<a href="#" data-menu_id="' + escAttr(entry.menuId || '') + '" data-pg-ui-t="' + escAttr(entry.sidebarLabel || entry.label) + '">'
      + escAttr(label) + '</a></li>';
  }

  function buildNavGroup(parentLabel, parentIcon, items) {
    var html = '<li class="side-nav-item" data-pg-nav-b="1">'
      + '<a href="javascript:void(0)" class="side-nav-link" aria-expanded="false">'
      + '<i class="bi ' + escAttr(parentIcon) + '"></i>'
      + '<span data-pg-ui-t="' + escAttr(parentLabel) + '"> ' + escAttr(uiT(parentLabel)) + ' </span>'
      + '<span class="menu-arrow"></span></a>'
      + '<ul class="side-nav-second-level">';
    items.forEach(function (it) { html += buildChildLi(it); });
    html += '</ul></li>';
    return html;
  }

  var LEGACY_BACKUP = { hq: null, deploy: null };

  function backupLegacyNav() {
    var root = document.querySelector('#side-nav-ul');
    if (!root) return;
    root.querySelectorAll(':scope > .side-nav-item').forEach(function (item) {
      var span = item.querySelector('.side-nav-link span[data-pg-ui-t]');
      var key = span ? String(span.getAttribute('data-pg-ui-t') || '').trim() : '';
      if (key === '본사설정' && !LEGACY_BACKUP.hq) LEGACY_BACKUP.hq = item.outerHTML;
      if (key === '배포설정' && !LEGACY_BACKUP.deploy) LEGACY_BACKUP.deploy = item.outerHTML;
    });
    try {
      global.sessionStorage.setItem('pg_nav_legacy_hq', LEGACY_BACKUP.hq || '');
      global.sessionStorage.setItem('pg_nav_legacy_deploy', LEGACY_BACKUP.deploy || '');
    } catch (eBk) { /* ignore */ }
  }

  function restoreLegacyNav() {
    var hqHtml = LEGACY_BACKUP.hq;
    var depHtml = LEGACY_BACKUP.deploy;
    try {
      if (!hqHtml) hqHtml = global.sessionStorage.getItem('pg_nav_legacy_hq');
      if (!depHtml) depHtml = global.sessionStorage.getItem('pg_nav_legacy_deploy');
    } catch (eRs) { /* ignore */ }
    var root = document.querySelector('#side-nav-ul');
    if (!root || !hqHtml || !depHtml) return false;
    var bItems = root.querySelectorAll(':scope > .side-nav-item[data-pg-nav-b="1"]');
    bItems.forEach(function (el) { el.remove(); });
    var firstB = root.querySelector('[data-pg-nav-b="1"]');
    var temp = document.createElement('div');
    temp.innerHTML = hqHtml + depHtml;
    while (temp.firstChild) {
      root.insertBefore(temp.firstChild, firstB || null);
    }
    return true;
  }

  function applySidebar() {
    if (!cfg.isLayoutB()) {
      restoreLegacyNav();
      return;
    }
    backupLegacyNav();
    var root = document.querySelector('#side-nav-ul');
    if (!root) return;

    root.querySelectorAll(':scope > .side-nav-item').forEach(function (item) {
      var span = item.querySelector('.side-nav-link span[data-pg-ui-t]');
      var key = span ? String(span.getAttribute('data-pg-ui-t') || '').trim() : '';
      if (key === '본사설정' || key === '배포설정') item.remove();
    });

    var hqItems = HUBS.map(function (h) {
      return { hubUrl: h.hubUrl, menuId: h.menuId, sidebarLabel: h.sidebarLabel, label: h.sidebarLabel };
    }).concat(STANDALONE_HQ.map(function (s) {
      return { url: s.url, menuId: s.menuId, sidebarLabel: s.label, label: s.label };
    })).concat([{
      hubUrl: PLATFORM_HUB.hubUrl,
      menuId: PLATFORM_HUB.menuId,
      sidebarLabel: PLATFORM_HUB.sidebarLabel,
      label: PLATFORM_HUB.sidebarLabel
    }]);

    var deployItems = DEPLOY_STANDALONE.map(function (s) {
      return { url: s.url, menuId: s.menuId, sidebarLabel: s.label, label: s.label };
    }).concat([{
      hubUrl: MERCHANT_API_HUB.hubUrl,
      menuId: MERCHANT_API_HUB.menuId,
      sidebarLabel: MERCHANT_API_HUB.sidebarLabel,
      label: MERCHANT_API_HUB.sidebarLabel
    }]);

    var hqNav = buildNavGroup('본사정책', 'bi-gear-wide-connected', hqItems);
    var depNav = buildNavGroup('연동·배포', 'bi-cloud-arrow-up', deployItems);

    var ref = root.querySelector('.side-nav-item');
    var wrap = document.createElement('div');
    wrap.innerHTML = hqNav + depNav;
    while (wrap.firstChild) {
      root.insertBefore(wrap.firstChild, ref);
    }

    if (global.PG_UI_I18N && typeof global.PG_UI_I18N.applyDom === 'function') {
      try { global.PG_UI_I18N.applyDom(root); } catch (eUi) { /* ignore */ }
    }
    if (typeof global._pgClearOrgPermSidebarCache === 'function') {
      try { global._pgClearOrgPermSidebarCache(); } catch (eCache) { /* ignore */ }
    }
  }

  function resolveNavUrl(url) {
    if (!url || !cfg.isLayoutB()) return url;
    if (hubByUrl[url]) return url;
    if (leafRedirect[url]) return leafRedirect[url];
    return url;
  }

  function hubForUrl(url) {
    if (!url) return null;
    var path = String(url).split('?')[0];
    if (hubByUrl[path]) return hubByUrl[path];
    if (leafToHub[path]) return leafToHub[path].hub;
    return null;
  }

  function parseHubQuery(url) {
    var q = {};
    try {
      var i = String(url || '').indexOf('?');
      if (i < 0) return q;
      new URLSearchParams(String(url).substring(i + 1)).forEach(function (v, k) { q[k] = v; });
    } catch (eP) { /* ignore */ }
    return q;
  }

  function leafUrlsForHub(hubUrl) {
    var h = hubByUrl[hubUrl];
    if (!h) return [];
    var out = [];
    (h.tabs || []).forEach(function (t) { out.push(t.url); });
    (h.steps || []).forEach(function (s) { out.push(s.url); });
    (h.guidePanels || []).forEach(function (p) {
      if (p && p.deployUrl) out.push(p.deployUrl);
    });
    return out;
  }

  /** 사이드바(왼쪽)와 동일한 leaf URL 순서 — 권한·업체접근 매트릭스 정렬 기준 */
  function canonicalPermissionUrlOrder() {
    var list = [];
    function add(url) {
      var u = String(url || '').split('?')[0];
      if (!u || list.indexOf(u) >= 0) return;
      list.push(u);
    }
    HUBS.forEach(function (h) {
      leafUrlsForHub(h.hubUrl).forEach(add);
    });
    STANDALONE_HQ.forEach(function (s) { add(s.url); });
    leafUrlsForHub(PLATFORM_HUB.hubUrl).forEach(add);
    DEPLOY_STANDALONE.forEach(function (s) { add(s.url); });
    leafUrlsForHub(MERCHANT_API_HUB.hubUrl).forEach(add);
    return list;
  }

  /**
   * 권한 매트릭스용 표시 메타(한국어 키).
   * 허브 탭은 「허브 › 탭」으로 사이드바와 직관적으로 맞춘다.
   */
  function leafDisplayMeta(url) {
    var path = String(url || '').split('?')[0];
    if (!path) return null;
    var i;
    for (i = 0; i < STANDALONE_HQ.length; i++) {
      if (STANDALONE_HQ[i].url === path) {
        return { kind: 'standalone', labelKo: STANDALONE_HQ[i].label, hubUrl: null, hubLabelKo: null };
      }
    }
    for (i = 0; i < DEPLOY_STANDALONE.length; i++) {
      if (DEPLOY_STANDALONE[i].url === path) {
        return { kind: 'standalone', labelKo: DEPLOY_STANDALONE[i].label, hubUrl: null, hubLabelKo: null };
      }
    }
    var leaf = leafToHub[path];
    if (leaf && leaf.hub) {
      var h = leaf.hub;
      var tabLabel = null;
      (h.tabs || []).forEach(function (t) { if (t.url === path) tabLabel = t.label; });
      (h.steps || []).forEach(function (s) { if (s.url === path) tabLabel = s.label; });
      if (tabLabel) {
        return {
          kind: 'hub-tab',
          labelKo: tabLabel,
          hubUrl: h.hubUrl,
          hubLabelKo: h.sidebarLabel || h.label || ''
        };
      }
    }
    var gp = MERCHANT_API_HUB.guidePanels || [];
    for (i = 0; i < gp.length; i++) {
      if (gp[i].deployUrl === path) {
        return {
          kind: 'hub-tab',
          labelKo: gp[i].label,
          hubUrl: MERCHANT_API_HUB.hubUrl,
          hubLabelKo: MERCHANT_API_HUB.sidebarLabel
        };
      }
    }
    return null;
  }

  function leafLabelKo(url) {
    var m = leafDisplayMeta(url);
    return m ? m.labelKo : null;
  }

  /** 사이드바 DOM에 없는 leaf URL — 권한 매트릭스 정렬 보조 (기존 index 유지) */
  function supplementLeafUrlOrder(urlIdx, startG) {
    if (!cfg.isLayoutB()) return startG;
    var g = startG;
    function reg(url) {
      if (url && urlIdx[url] === undefined) urlIdx[url] = g++;
    }
    canonicalPermissionUrlOrder().forEach(reg);
    return g;
  }

  /**
   * 레이아웃 B leaf 를 사이드바 정의 순서로 재배치하고,
   * 그 외 URL은 기존 상대 순서를 유지한 채 뒤에 붙인다.
   */
  function applyCanonicalPermissionUrlOrder(urlIdx) {
    if (!cfg.isLayoutB() || !urlIdx) return 0;
    var canon = canonicalPermissionUrlOrder();
    var others = Object.keys(urlIdx)
      .filter(function (u) { return canon.indexOf(u) < 0; })
      .sort(function (a, b) { return (urlIdx[a] || 0) - (urlIdx[b] || 0); });
    Object.keys(urlIdx).forEach(function (k) { delete urlIdx[k]; });
    var g = 0;
    canon.forEach(function (u) { urlIdx[u] = g++; });
    others.forEach(function (u) { urlIdx[u] = g++; });
    return g;
  }

  function permissionAliases(url) {
    var list = [url];
    if (!cfg.isLayoutB()) return list;
    var path = String(url || '').split('?')[0];
    if (path === '/ops/opsManuals' || path === '/hq/platformOpsManuals') {
      ['/ops/opsManuals', '/hq/platformOpsManuals'].forEach(function (u) {
        if (list.indexOf(u) < 0) list.push(u);
      });
    }
    if (hubByUrl[path]) {
      leafUrlsForHub(path).forEach(function (u) {
        if (list.indexOf(u) < 0) list.push(u);
      });
      return list;
    }
    Object.keys(hubByUrl).forEach(function (hu) {
      if (leafUrlsForHub(hu).indexOf(path) >= 0 && list.indexOf(hu) < 0) list.push(hu);
    });
    if (leafRedirect[path]) {
      var hubPath = leafRedirect[path].split('?')[0];
      if (list.indexOf(hubPath) < 0) list.push(hubPath);
    }
    return list;
  }

  function parentGroupForUrl(url, fallback) {
    if (!cfg.isLayoutB()) return fallback;
    var path = String(url || '').split('?')[0];
    var h = hubForUrl(path) || hubByUrl[path];
    if (h && h.parent) return h.parent;
    STANDALONE_HQ.concat(DEPLOY_STANDALONE).forEach(function (s) {
      if (s.url === path) return h = s;
    });
    if (path === MERCHANT_API_HUB.hubUrl) return MERCHANT_API_HUB.parent;
    var leaf = leafToHub[path];
    if (leaf && leaf.hub && leaf.hub.parent) return leaf.hub.parent;
    if (fallback === '본사설정' || fallback === '본사 정책') return '본사정책';
    if (fallback === '배포설정') return '연동·배포';
    return fallback;
  }

  function isHubUrl(url) {
    var path = String(url || '').split('?')[0];
    return !!hubByUrl[path];
  }

  global.PG_MENU_LAYOUT_B = {
    enabled: true,
    HUBS: HUBS,
    PLATFORM_HUB: PLATFORM_HUB,
    MERCHANT_API_HUB: MERCHANT_API_HUB,
    hubByUrl: hubByUrl,
    leafRedirect: leafRedirect,
    resolveNavUrl: resolveNavUrl,
    hubForUrl: hubForUrl,
    parseHubQuery: parseHubQuery,
    leafUrlsForHub: leafUrlsForHub,
    canonicalPermissionUrlOrder: canonicalPermissionUrlOrder,
    leafDisplayMeta: leafDisplayMeta,
    leafLabelKo: leafLabelKo,
    supplementLeafUrlOrder: supplementLeafUrlOrder,
    applyCanonicalPermissionUrlOrder: applyCanonicalPermissionUrlOrder,
    permissionAliases: permissionAliases,
    applySidebar: applySidebar,
    restoreLegacyNav: restoreLegacyNav,
    parentGroupForUrl: parentGroupForUrl,
    isHubUrl: isHubUrl
  };
})(typeof window !== 'undefined' ? window : this);
