/**
 * B안 허브 shell — 탭/위저드 UI + 기존 화면 embed.
 */
(function (global) {
  'use strict';

  var layoutB = global.PG_MENU_LAYOUT_B;

  function uiT(s) {
    if (global.PG_UI_I18N && typeof global.PG_UI_I18N.t === 'function') return global.PG_UI_I18N.t(String(s));
    return String(s);
  }

  function escHtml(s) {
    return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
  }

  function hubConfig(hubUrl) {
    if (!layoutB || !layoutB.hubByUrl) return null;
    var path = String(hubUrl || '').split('?')[0];
    return layoutB.hubByUrl[path] || null;
  }

  function firstAllowedTab(hub, hubUrl) {
    var tabs = hub.wizard ? hub.steps : hub.tabs;
    if (!tabs || !tabs.length) return null;
    var q = layoutB.parseHubQuery(hubUrl || '');
    var want = hub.wizard ? q.step : q.tab;
    if (want) {
      for (var i = 0; i < tabs.length; i++) {
        var k = hub.wizard ? tabs[i].step : tabs[i].tab;
        if (k === want) {
          if (typeof global.isMenuAllowedForCurrentUser === 'function' && !global.isMenuAllowedForCurrentUser(tabs[i].url)) continue;
          return tabs[i];
        }
      }
    }
    if (typeof global.isMenuAllowedForCurrentUser === 'function') {
      for (var j = 0; j < tabs.length; j++) {
        if (global.isMenuAllowedForCurrentUser(tabs[j].url)) return tabs[j];
      }
      return null;
    }
    return tabs[0];
  }

  function getScreenHtml(hubUrl, tabId) {
    var hub = hubConfig(hubUrl);
    if (!hub) {
      return '<div class="card"><div class="card-body"><p class="text-muted mb-0">' + escHtml(uiT('허브 정보가 없습니다.')) + '</p></div></div>';
    }
    var activeTab = firstAllowedTab(hub, hubUrl);
    var activeKey = activeTab ? (hub.wizard ? activeTab.step : activeTab.tab) : '';
    var tabs = hub.wizard ? hub.steps : hub.tabs;
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

    var helpHtml = '';
    if (hub.helpPanels && hub.helpPanels.length) {
      helpHtml = '<aside class="pg-hub-help col-lg-4 border-start ps-3">'
        + '<div class="fw-semibold mb-2" data-pg-ui-t="출시 가이드">' + escHtml(uiT('출시 가이드')) + '</div>'
        + '<div class="list-group list-group-flush small pg-hub-help-list">';
      hub.helpPanels.forEach(function (p) {
        helpHtml += '<button type="button" class="list-group-item list-group-item-action py-2 pg-hub-help-btn"'
          + ' data-pg-help-panel="' + escHtml(p.panel) + '" data-pg-help-url="' + escHtml(p.deployUrl) + '">'
          + escHtml(uiT(p.label)) + '</button>';
      });
      helpHtml += '</div><div class="pg-hub-help-body mt-2 small text-muted" id="pgHubHelpBody_' + escHtml(tabId) + '"></div></aside>';
    }

    var bodyClass = hub.helpPanels ? 'row g-0' : '';
    return '<div class="content pg-hub-shell" id="screenContent_' + escHtml(tabId) + '" data-pg-hub-url="' + escHtml(hub.hubUrl) + '">'
      + '<div class="card mb-0"><div class="card-body pb-2">'
      + '<div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-2">'
      + '<div class="text-muted small" data-pg-ui-t="탭을 전환해도 동일 허브 안에서 설정합니다.">'
      + escHtml(uiT('탭을 전환해도 동일 허브 안에서 설정합니다.')) + '</div>'
      + '<div class="small"><span class="badge bg-secondary-subtle text-secondary pg-hub-layout-badge" data-pg-ui-t="ICOPAY V2.0">'
      + escHtml(uiT('ICOPAY V2.0')) + '</span></div></div>'
      + '<ul class="nav nav-tabs pg-hub-tabs flex-nowrap overflow-auto" role="tablist">' + navHtml + '</ul>'
      + '</div>'
      + '<div class="card-body pt-3 ' + bodyClass + '">'
      + '<div class="' + (hub.helpPanels ? 'col-lg-8 pe-lg-3' : 'col-12') + ' pg-hub-panel-wrap">'
      + '<div class="pg-hub-panel" id="pgHubPanel_' + escHtml(tabId) + '"></div>'
      + '</div>' + helpHtml
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
      '/hq/settlementAdmin': ['_hqSettlementAdminBound'],
      '/hq/receivableRecoverySettings': ['_hqRecvRecoveryBound'],
      '/hq/domainConfig': ['_hqDomainOrgTableDelBound'],
      '/hq/serverManage': ['_hqSrvPaneChangeBound', '_hqUsageGrainBound'],
      '/hq/merchantApiGenerate': ['_merchantApiDeployKitBound'],
      '/hq/merchantApiDeployDocs': ['_merchantApiDeployDocsBound'],
      '/hq/apiMerchantDeployReg': ['_apiMerchRegBound']
    };
    (resets[leafUrl] || []).forEach(function (f) { pane[f] = false; });
  }

  function loadLeafIntoHub(hubPane, leafUrl, innerTabId) {
    if (!hubPane || !leafUrl) return;
    var panel = hubPane.querySelector('.pg-hub-panel');
    if (!panel) return;
    resetPaneFlags(hubPane, leafUrl);
    panel.innerHTML = '';
    var inner = document.createElement('div');
    inner.className = 'pg-hub-leaf-pane';
    inner.setAttribute('data-pg-hub-leaf-url', leafUrl);
    inner.setAttribute('formurl', leafUrl);
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

  function loadDeployHelp(panelBody, deployUrl) {
    if (!panelBody || !deployUrl) return;
    if (global.PG_SCREENS && typeof global.PG_SCREENS.getScreenHtml === 'function') {
      panelBody.innerHTML = global.PG_SCREENS.getScreenHtml(deployUrl, 'hub_help');
      if (global.PG_UI_I18N && typeof global.PG_UI_I18N.applyDom === 'function') {
        try { global.PG_UI_I18N.applyDom(panelBody); } catch (eH) { /* ignore */ }
      }
      if (typeof global.bindScreenEvents === 'function') {
        try { global.bindScreenEvents(panelBody, 'hub_help'); } catch (eHb) { /* ignore */ }
      }
    }
  }

  function bindHubShell(pane, tabId, hubUrl) {
    if (!pane) return;
    var hub = hubConfig(hubUrl);
    if (!hub) return;
    var first = firstAllowedTab(hub, hubUrl);
    if (first) {
      loadLeafIntoHub(pane, first.url, tabId + '_leaf');
    } else {
      var panel = pane.querySelector('.pg-hub-panel');
      if (panel) {
        panel.innerHTML = '<p class="text-muted mb-0">' + escHtml(uiT('이 허브에 접근 가능한 탭이 없습니다. 본사권한설정을 확인하세요.')) + '</p>';
      }
    }

    if (pane._pgHubShellBound) return;
    pane._pgHubShellBound = true;

    var qInit = layoutB.parseHubQuery(hubUrl || '');
    if (qInit.panel && hub.helpPanels) {
      global.requestAnimationFrame(function () {
        var helpBtn = pane.querySelector('[data-pg-help-panel="' + qInit.panel + '"]');
        if (helpBtn) helpBtn.click();
      });
    }

    pane.addEventListener('click', function (ev) {
      var tabBtn = ev.target && ev.target.closest ? ev.target.closest('[data-pg-hub-tab]') : null;
      if (tabBtn) {
        ev.preventDefault();
        var leaf = tabBtn.getAttribute('data-pg-hub-leaf') || '';
        if (tabBtn.getAttribute('data-pg-hub-external') === '1') {
          var extLeaf = tabBtn.getAttribute('data-pg-hub-leaf') || '';
          if (extLeaf) {
            pane.querySelectorAll('.pg-hub-tabs .nav-link').forEach(function (b) { b.classList.remove('active'); });
            tabBtn.classList.add('active');
            loadLeafIntoHub(pane, extLeaf, tabId + '_leaf_' + (tabBtn.getAttribute('data-pg-hub-tab') || ''));
          }
          return;
        }
        pane.querySelectorAll('.pg-hub-tabs .nav-link').forEach(function (b) { b.classList.remove('active'); });
        tabBtn.classList.add('active');
        loadLeafIntoHub(pane, leaf, tabId + '_leaf_' + (tabBtn.getAttribute('data-pg-hub-tab') || ''));
        return;
      }
      var helpBtn = ev.target && ev.target.closest ? ev.target.closest('.pg-hub-help-btn') : null;
      if (helpBtn) {
        ev.preventDefault();
        pane.querySelectorAll('.pg-hub-help-btn').forEach(function (b) { b.classList.remove('active'); });
        helpBtn.classList.add('active');
        var helpBody = pane.querySelector('[id^="pgHubHelpBody_"]');
        loadDeployHelp(helpBody, helpBtn.getAttribute('data-pg-help-url') || '');
      }
    });
  }

  global.PG_HUB_SHELL = {
    isHubUrl: function (url) { return layoutB && layoutB.isHubUrl && layoutB.isHubUrl(url); },
    getScreenHtml: getScreenHtml,
    bindHubShell: bindHubShell,
    hubConfig: hubConfig
  };
})(typeof window !== 'undefined' ? window : this);
