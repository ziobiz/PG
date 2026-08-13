/**
 * B안 허브 shell — 탭/위저드 UI + 기존 화면 embed.
 * 플랫폼 버전(ICOPAY Vx.y) 배지는 여기에 두지 않는다. 버전 표기는 본사정책 > 플랫폼 > 업데이트 내용만.
 * 허브 서브탭은 sessionStorage + 상단 탭 data-pg-full-url 로 기억해, 다른 상단 탭 왕복 후에도 복원한다.
 */
(function (global) {
  'use strict';

  var layoutB = global.PG_MENU_LAYOUT_B;
  var LAST_TAB_KEY = 'pg_hub_last_tab';

  function uiT(s) {
    if (global.PG_UI_I18N && typeof global.PG_UI_I18N.t === 'function') return global.PG_UI_I18N.t(String(s));
    return String(s);
  }

  function escHtml(s) {
    return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
  }

  function hubPathOnly(hubUrl) {
    return String(hubUrl || '').split('?')[0];
  }

  function rememberHubTab(hubUrl, key) {
    var path = hubPathOnly(hubUrl);
    if (!path || !key) return;
    try {
      var map = {};
      try { map = JSON.parse(global.sessionStorage.getItem(LAST_TAB_KEY) || '{}') || {}; } catch (e0) { map = {}; }
      map[path] = String(key);
      global.sessionStorage.setItem(LAST_TAB_KEY, JSON.stringify(map));
    } catch (e1) { /* ignore */ }
  }

  function rememberedHubTab(hubUrl) {
    var path = hubPathOnly(hubUrl);
    if (!path) return '';
    try {
      var map = JSON.parse(global.sessionStorage.getItem(LAST_TAB_KEY) || '{}') || {};
      return map[path] ? String(map[path]) : '';
    } catch (e2) {
      return '';
    }
  }

  function hubFullUrl(hubUrl, key, isWizard) {
    var path = hubPathOnly(hubUrl);
    if (!path) return '';
    if (!key) return path;
    return path + (isWizard ? '?step=' : '?tab=') + encodeURIComponent(key);
  }

  /** 상단 활성 탭 li 의 data-pg-full-url 을 허브 서브탭까지 포함해 갱신 */
  function syncTopTabFullUrl(hubUrl, key, isWizard) {
    var path = hubPathOnly(hubUrl);
    var full = hubFullUrl(path, key, isWizard);
    if (!path || !full) return;
    try {
      var ul = global.document.getElementById('copyTopTabUl');
      if (!ul) return;
      var li = ul.querySelector('.copyTopTab[top_tab_url="' + path + '"]');
      if (li) li.setAttribute('data-pg-full-url', full);
    } catch (e3) { /* ignore */ }
  }

  function hubConfig(hubUrl) {
    if (!layoutB || !layoutB.hubByUrl) return null;
    var path = hubPathOnly(hubUrl);
    return layoutB.hubByUrl[path] || null;
  }

  function isLeafAllowed(url) {
    if (!url) return false;
    if (typeof global.isMenuAllowedForCurrentUser === 'function') {
      return !!global.isMenuAllowedForCurrentUser(url);
    }
    return true;
  }

  function allowedTabs(hub) {
    var tabs = hub.wizard ? hub.steps : hub.tabs;
    if (!tabs || !tabs.length) return [];
    return tabs.filter(function (t) { return isLeafAllowed(t.url); });
  }

  function normalizeWantKey(hub, want) {
    if (!want) return '';
    var w = String(want);
    /* 구 탭 id 호환: 결제 UX → 태블릿 UX */
    if (!hub.wizard && w === 'checkout-ux') w = 'tablet-ux';
    return w;
  }

  function findTabByKey(tabs, hub, key) {
    if (!key || !tabs) return null;
    for (var i = 0; i < tabs.length; i++) {
      var k = hub.wizard ? tabs[i].step : tabs[i].tab;
      if (k === key) return tabs[i];
    }
    return null;
  }

  function firstAllowedTab(hub, hubUrl) {
    var tabs = allowedTabs(hub);
    if (!tabs.length) return null;
    var q = layoutB.parseHubQuery(hubUrl || '');
    var want = normalizeWantKey(hub, hub.wizard ? q.step : q.tab);
    if (!want) {
      want = normalizeWantKey(hub, rememberedHubTab(hub.hubUrl || hubPathOnly(hubUrl)));
    }
    var found = findTabByKey(tabs, hub, want);
    if (found) return found;
    return tabs[0];
  }

  function denyHtml() {
    return '<p class="text-muted mb-0">' + escHtml(uiT('이 화면에 대한 접근 권한이 없습니다. 본사권한설정을 확인하세요.')) + '</p>';
  }

  function getScreenHtml(hubUrl, tabId) {
    var hub = hubConfig(hubUrl);
    if (!hub) {
      return '<div class="card"><div class="card-body"><p class="text-muted mb-0">' + escHtml(uiT('허브 정보가 없습니다.')) + '</p></div></div>';
    }
    var tabs = allowedTabs(hub);
    var activeTab = firstAllowedTab(hub, hubUrl);
    var activeKey = activeTab ? (hub.wizard ? activeTab.step : activeTab.tab) : '';
    var navHtml = '';
    tabs.forEach(function (t) {
      var key = hub.wizard ? t.step : t.tab;
      var active = activeKey === key;
      var label = uiT(t.label);
      navHtml += '<li class="nav-item" role="presentation">'
        + '<button type="button" class="nav-link' + (active ? ' active' : '') + '" role="tab"'
        + ' data-pg-hub-tab="' + escHtml(key) + '" data-pg-hub-leaf="' + escHtml(t.url) + '"'
        + (t.external ? ' data-pg-hub-external="1"' : '')
        + '>' + escHtml(label) + '</button></li>';
    });

    /* 우측 helpPanels 사이드바는 제거 — 출시 가이드는 전폭 탭으로만 제공 */
    return '<div class="content pg-hub-shell" id="screenContent_' + escHtml(tabId) + '" data-pg-hub-url="' + escHtml(hub.hubUrl) + '">'
      + '<div class="card mb-0"><div class="card-body pb-2">'
      + '<div class="text-muted small mb-2" data-pg-ui-t="탭을 전환해도 동일 허브 안에서 설정합니다. 상단 탭으로 다른 메뉴에 갔다가 돌아와도 마지막 서브탭을 유지합니다.">'
      + escHtml(uiT('탭을 전환해도 동일 허브 안에서 설정합니다. 상단 탭으로 다른 메뉴에 갔다가 돌아와도 마지막 서브탭을 유지합니다.')) + '</div>'
      + (navHtml
        ? '<ul class="nav nav-tabs pg-hub-tabs" role="tablist">' + navHtml + '</ul>'
        : '<p class="text-muted mb-0">' + escHtml(uiT('이 허브에 접근 가능한 탭이 없습니다. 본사권한설정을 확인하세요.')) + '</p>')
      + '</div>'
      + '<div class="card-body pt-3">'
      + '<div class="pg-hub-panel-wrap w-100">'
      + '<div class="pg-hub-panel" id="pgHubPanel_' + escHtml(tabId) + '"></div>'
      + '</div>'
      + '</div></div></div>';
  }

  function resetPaneFlags(pane, leafUrl) {
    if (!pane || !leafUrl) return;
    var resets = {
      '/hq/orgViewColumnAllowance': ['_hqOrgColAllowBound', '_hqOrgAllowBulkBound', '_hqOrgAllowColMoveBound', '_hqOrgAllowPolicyRowBound', '_hqOrgVccDelegated'],
      '/set/gridSetMng': [],
      '/hq/notifyMapping': ['_hqNotifyMappingBound'],
      '/hq/notifyEnv': ['_hqNotifyTargetTableActionDelegated'],
      '/hq/notifyInbound': ['_hqNotifyInboundBound'],
      '/hq/ledgerSysSettings': ['_hqLedgerPaneInit'],
      '/hq/platformReleaseNotes': ['_icopayReleaseNotesMounted'],
      '/ops/opsManuals': ['_icopayPmBound'],
      '/hq/platformOpsManuals': ['_icopayPmBound'],
      '/hq/settlementAdmin': ['_hqSettlementAdminBound'],
      '/hq/receivableRecoverySettings': ['_hqRecvRecoveryBound'],
      '/hq/domainConfig': ['_hqDomainOrgTableDelBound'],
      '/hq/serverManage': ['_hqSrvPaneChangeBound', '_hqUsageGrainBound'],
      '/hq/merchantApiGenerate': ['_merchantApiDeployKitBound'],
      '/hq/merchantApiDeployDocs': ['_merchantApiDeployDocsBound'],
      '/hq/apiMerchantDeployReg': ['_apiMerchRegBound'],
      '/deploy/launchGuide': ['_launchGuideBound']
    };
    (resets[leafUrl] || []).forEach(function (f) { pane[f] = false; });
  }

  function loadLeafIntoHub(hubPane, leafUrl, innerTabId, preferPanel) {
    if (!hubPane || !leafUrl) return;
    var panel = hubPane.querySelector('.pg-hub-panel');
    if (!panel) return;
    if (!isLeafAllowed(leafUrl)) {
      panel.innerHTML = denyHtml();
      return;
    }
    resetPaneFlags(hubPane, leafUrl);
    panel.innerHTML = '';
    var inner = document.createElement('div');
    inner.className = 'pg-hub-leaf-pane';
    inner.setAttribute('data-pg-hub-leaf-url', leafUrl);
    inner.setAttribute('formurl', leafUrl);
    if (preferPanel) {
      inner.setAttribute('data-pg-guide-panel-pref', preferPanel);
    }
    panel.appendChild(inner);
    if (global.PG_SCREENS && typeof global.PG_SCREENS.getScreenHtml === 'function') {
      inner.innerHTML = global.PG_SCREENS.getScreenHtml(leafUrl, innerTabId);
    }
    if (global.PG_UI_I18N && typeof global.PG_UI_I18N.applyDom === 'function') {
      try { global.PG_UI_I18N.applyDom(inner); } catch (eUi) { /* ignore */ }
    }
    if (typeof global.bindScreenEvents === 'function') {
      try { global.bindScreenEvents(inner, innerTabId); } catch (eBind) {
        try { console.error('[PG] hub bindScreenEvents', leafUrl, eBind); } catch (eLog) { /* ignore */ }
      }
    }
    if (global.PG_TABLE_COL_RESIZE) {
      if (typeof global.PG_TABLE_COL_RESIZE.ensureObserver === 'function') global.PG_TABLE_COL_RESIZE.ensureObserver(inner);
      if (typeof global.PG_TABLE_COL_RESIZE.refreshInSync === 'function') global.PG_TABLE_COL_RESIZE.refreshInSync(inner);
      else if (typeof global.PG_TABLE_COL_RESIZE.refreshIn === 'function') global.PG_TABLE_COL_RESIZE.refreshIn(inner);
    }
  }

  /** 출시 가이드 탭 권한이 있으면 내부 서브패널도 허용(서브행 접근불가여도 가이드 본문 표시) */
  function isGuidePanelAllowed(deployUrl) {
    if (isLeafAllowed('/deploy/launchGuide')) return true;
    return !!(deployUrl && isLeafAllowed(deployUrl));
  }

  function firstAllowedGuidePanel(hub) {
    if (!hub || !hub.guidePanels || !hub.guidePanels.length) return null;
    for (var i = 0; i < hub.guidePanels.length; i++) {
      var p = hub.guidePanels[i];
      if (p && p.deployUrl && isGuidePanelAllowed(p.deployUrl)) return p;
    }
    if (isLeafAllowed('/deploy/launchGuide')) return hub.guidePanels[0];
    return null;
  }

  function currentActiveKey(pane) {
    if (!pane) return '';
    var activeBtn = pane.querySelector('.pg-hub-tabs .nav-link.active');
    return activeBtn ? (activeBtn.getAttribute('data-pg-hub-tab') || '') : '';
  }

  function switchHubTab(pane, tabId, hub, stepKey, leaf, preferPanel) {
    if (!pane || !hub || !leaf) return;
    if (!isLeafAllowed(leaf)) {
      var denyPanel = pane.querySelector('.pg-hub-panel');
      if (denyPanel) denyPanel.innerHTML = denyHtml();
      return;
    }
    pane.querySelectorAll('.pg-hub-tabs .nav-link').forEach(function (b) { b.classList.remove('active'); });
    var matchBtn = null;
    pane.querySelectorAll('.pg-hub-tabs .nav-link[data-pg-hub-tab]').forEach(function (b) {
      if (b.getAttribute('data-pg-hub-tab') === stepKey) matchBtn = b;
    });
    if (matchBtn) matchBtn.classList.add('active');
    var pref = preferPanel || '';
    if (stepKey === 'guide' && hub.guidePanels && hub.guidePanels.length && !pref) {
      var firstGpClick = firstAllowedGuidePanel(hub);
      pref = firstGpClick ? firstGpClick.panel : '';
    }
    loadLeafIntoHub(pane, leaf, tabId + '_leaf_' + stepKey, pref);
    rememberHubTab(hub.hubUrl, stepKey);
    syncTopTabFullUrl(hub.hubUrl, stepKey, !!hub.wizard);
    pane.setAttribute('data-pg-hub-active-tab', stepKey);
  }

  /**
   * 상단 탭 재진입 시: URL 쿼리(또는 기억값)가 현재 활성 서브탭과 다르면 전환.
   * 쿼리가 없으면 현재 DOM 상태를 유지하고 full-url 만 동기화.
   */
  function ensureActiveTab(pane, tabId, hubUrl) {
    var hub = hubConfig(hubUrl);
    if (!hub || !pane) return;
    var q = layoutB.parseHubQuery(hubUrl || '');
    var want = normalizeWantKey(hub, hub.wizard ? q.step : q.tab);
    var cur = currentActiveKey(pane);
    if (!want) {
      if (cur) {
        rememberHubTab(hub.hubUrl, cur);
        syncTopTabFullUrl(hub.hubUrl, cur, !!hub.wizard);
      }
      return;
    }
    if (want === cur) {
      rememberHubTab(hub.hubUrl, cur);
      syncTopTabFullUrl(hub.hubUrl, cur, !!hub.wizard);
      return;
    }
    var tabs = allowedTabs(hub);
    var found = findTabByKey(tabs, hub, want);
    if (!found) return;
    var leaf = found.url;
    var pref = q.panel || '';
    switchHubTab(pane, tabId, hub, want, leaf, pref);
  }

  function bindHubShell(pane, tabId, hubUrl) {
    if (!pane) return;
    var hub = hubConfig(hubUrl);
    if (!hub) return;
    var qInit = layoutB.parseHubQuery(hubUrl || '');
    var first = firstAllowedTab(hub, hubUrl);
    if (first) {
      var firstKey = hub.wizard ? first.step : first.tab;
      var prefInit = qInit.panel || '';
      if (firstKey === 'guide' && hub.guidePanels && hub.guidePanels.length) {
        if (prefInit) {
          var prefOk = false;
          for (var gi = 0; gi < hub.guidePanels.length; gi++) {
            var gp = hub.guidePanels[gi];
            if (gp && gp.panel === prefInit && isGuidePanelAllowed(gp.deployUrl)) { prefOk = true; break; }
          }
          if (!prefOk) prefInit = '';
        }
        if (!prefInit) {
          var firstGp = firstAllowedGuidePanel(hub);
          prefInit = firstGp ? firstGp.panel : '';
        }
      }
      loadLeafIntoHub(pane, first.url, tabId + '_leaf', prefInit);
      rememberHubTab(hub.hubUrl, firstKey);
      syncTopTabFullUrl(hub.hubUrl, firstKey, !!hub.wizard);
      pane.setAttribute('data-pg-hub-active-tab', firstKey);
    } else {
      var panel = pane.querySelector('.pg-hub-panel');
      if (panel) {
        panel.innerHTML = '<p class="text-muted mb-0">' + escHtml(uiT('이 허브에 접근 가능한 탭이 없습니다. 본사권한설정을 확인하세요.')) + '</p>';
      }
    }

    if (pane._pgHubShellBound) return;
    pane._pgHubShellBound = true;

    pane.addEventListener('click', function (ev) {
      var tabBtn = ev.target && ev.target.closest ? ev.target.closest('[data-pg-hub-tab]') : null;
      if (!tabBtn) return;
      ev.preventDefault();
      var leaf = tabBtn.getAttribute('data-pg-hub-leaf') || '';
      var stepKey = tabBtn.getAttribute('data-pg-hub-tab') || '';
      switchHubTab(pane, tabId, hub, stepKey, leaf, '');
    });
  }

  global.PG_HUB_SHELL = {
    isHubUrl: function (url) { return layoutB && layoutB.isHubUrl && layoutB.isHubUrl(url); },
    getScreenHtml: getScreenHtml,
    bindHubShell: bindHubShell,
    hubConfig: hubConfig,
    ensureActiveTab: ensureActiveTab,
    rememberHubTab: rememberHubTab,
    rememberedHubTab: rememberedHubTab,
    hubFullUrl: hubFullUrl
  };
})(typeof window !== 'undefined' ? window : this);
