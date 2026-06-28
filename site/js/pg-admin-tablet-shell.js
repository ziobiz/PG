/**
 * 관리자 index — 헤더 태블릿 스위치, pg-admin--tablet 레이아웃, 메인 아이콘 런처.
 * sessionStorage pg_admin_tablet_shell (PG_LOGIN_TABLET_SHELL.SHELL_KEY 와 동일)
 */
(function (w) {
  'use strict';

  var SHELL_KEY = 'pg_admin_tablet_shell';

  var PG_TABLET_LAUNCH_MENU_ORDER = [
    '/comp/compReg',
    '/comp/compMngTree',
    '/commission/commisionList',
    '/calc/payList',
    '/calc/dailyPay',
    '/calc/feeList',
    '/calc/dailyFee',
    '/calc/settlementReport',
    '/chatbot/chatbotKbMng',
    '/chatbot/productMng',
    '/chatbot/orderMng',
    '/calc/chillPayTrList',
    '/pay/chatbotPay',
    '/pay/splitPay',
    /* 검수관리 */
    '/calc/integratedCheck',
    '/ops/agencyTxnList',
    '/calc/jpayTrList',
    '/calc/queryIntegrated',
    '/risk/list',
    '/ops/integratedReport',
    '/ops/verifyReport',
    '/user/userMng'
  ];

  /** Bootstrap Icons 1.10 호환 */
  var PG_TABLET_LAUNCH_TILE = {
    '/comp/compReg': { icon: 'bi-shop', tone: 'comp' },
    '/comp/compMngTree': { icon: 'bi-diagram-3-fill', tone: 'comp' },
    '/commission/commisionList': { icon: 'bi-percent', tone: 'comp' },
    '/calc/payList': { icon: 'bi-credit-card-fill', tone: 'pay' },
    '/calc/dailyPay': { icon: 'bi-calendar3', tone: 'pay' },
    '/calc/feeList': { icon: 'bi-receipt-cutoff', tone: 'settle' },
    '/calc/dailyFee': { icon: 'bi-calendar2-check-fill', tone: 'settle' },
    '/calc/settlementReport': { icon: 'bi-pie-chart-fill', tone: 'settle' },
    '/chatbot/chatbotKbMng': { icon: 'bi-journal-bookmark-fill', tone: 'bot' },
    '/chatbot/productMng': { icon: 'bi-box-seam-fill', tone: 'bot' },
    '/chatbot/orderMng': { icon: 'bi-bag-check-fill', tone: 'bot' },
    '/calc/chillPayTrList': { icon: 'bi-collection-fill', tone: 'pay' },
    '/pay/chatbotPay': { icon: 'bi-chat-square-text-fill', tone: 'bot' },
    '/pay/splitPay': { icon: 'bi-layers-half', tone: 'pay' },
    '/calc/integratedCheck': { icon: 'bi-check2-square-fill', tone: 'inspect' },
    '/ops/agencyTxnList': { icon: 'bi-cash-coin', tone: 'inspect' },
    '/calc/jpayTrList': { icon: 'bi-binoculars-fill', tone: 'inspect' },
    '/calc/queryIntegrated': { icon: 'bi-calendar2-week-fill', tone: 'inspect' },
    '/risk/list': { icon: 'bi-shield-exclamation-fill', tone: 'inspect' },
    '/ops/integratedReport': { icon: 'bi-bar-chart-line-fill', tone: 'ops' },
    '/ops/verifyReport': { icon: 'bi-clipboard2-check-fill', tone: 'ops' },
    '/user/userMng': { icon: 'bi-person-badge-fill', tone: 'user' }
  };

  /** 가맹점 전용 — 태블릿 메인 추가 단축 (본사·총판 등에는 미노출) */
  var MERCHANT_TABLET_SHORTCUTS = [
    { action: 'url-pay-open', labelKo: 'URL 결제 가기', icon: 'bi-box-arrow-up-right', tone: 'pay', flag: 'web' },
    { action: 'url-pay-copy', labelKo: 'URL 결제 복사', icon: 'bi-clipboard', tone: 'pay', flag: 'web' },
    { action: 'chatbot-pay-open', labelKo: '챗봇결제 가기', icon: 'bi-chat-dots-fill', tone: 'bot', flag: 'chatbot' },
    { action: 'chatbot-pay-copy', labelKo: '챗봇결제 복사', icon: 'bi-clipboard-check', tone: 'bot', flag: 'chatbot' }
  ];

  function getSessionUser() {
    if (typeof w.getPgSessionUser === 'function') return w.getPgSessionUser() || {};
    try { return JSON.parse(w.sessionStorage.getItem('pg_admin_user') || '{}') || {}; } catch (e) { return {}; }
  }

  function uiT(ko) {
    if (w.PG_UI_I18N && typeof w.PG_UI_I18N.t === 'function') return String(w.PG_UI_I18N.t(String(ko)));
    return String(ko);
  }

  function escHtml(s) {
    return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
  }

  function escAttr(s) {
    return escHtml(s);
  }

  function tabletShellCanActivate(u) {
    if (!u || typeof u !== 'object') return false;
    var urls = Array.isArray(u.tabletMenuUrls) ? u.tabletMenuUrls : [];
    var roleU = String(u.role != null ? u.role : '').toUpperCase();
    var orgTf = String(u.tabletFeatureUseYn != null ? u.tabletFeatureUseYn : 'Y').trim().toUpperCase();
    var orgOk = roleU === 'ADMIN' || orgTf === 'Y';
    if (!orgOk) return false;
    if (isMerchantUser(u)) return true;
    if (w.PG_LOGIN_TABLET_SHELL && typeof w.PG_LOGIN_TABLET_SHELL.tabletShellCanActivate === 'function') {
      return w.PG_LOGIN_TABLET_SHELL.tabletShellCanActivate(u);
    }
    return urls.length > 0;
  }

  function isMenuAllowed(url) {
    if (typeof w.isMenuAllowedForCurrentUser === 'function') {
      return w.isMenuAllowedForCurrentUser(url);
    }
    return true;
  }

  function isTabletShellMode() {
    try { return w.sessionStorage.getItem(SHELL_KEY) === '1'; } catch (e) { return false; }
  }

  function hideFlyout() {
    try {
      var fly = w.document.getElementById('flyout-submenu');
      if (fly) {
        fly.style.display = 'none';
        fly.style.visibility = 'hidden';
        fly.classList.remove('show');
      }
    } catch (eF) { /* ignore */ }
  }

  function syncTabletShellClass() {
    try { w.document.documentElement.classList.toggle('pg-admin--tablet', isTabletShellMode()); } catch (eC) { /* ignore */ }
    if (w.PG_TOPBAR_CONTRAST && typeof w.PG_TOPBAR_CONTRAST.schedule === 'function') {
      w.PG_TOPBAR_CONTRAST.schedule();
    }
  }

  function syncTabletShellToggleUi() {
    var toggle = w.document.getElementById('pgTabletShellToggle');
    var wrap = w.document.getElementById('pgTabletShellWrap');
    var u = getSessionUser();
    var can = tabletShellCanActivate(u);
    if (wrap) {
      if (can) wrap.classList.remove('d-none');
      else wrap.classList.add('d-none');
    }
    if (toggle) {
      toggle.disabled = !can;
      toggle.checked = can && isTabletShellMode();
    }
  }

  function getOrderedTabletLaunchUrls(u) {
    u = u || getSessionUser();
    var raw = Array.isArray(u.tabletMenuUrls) ? u.tabletMenuUrls : [];
    var allowedSet = {};
    raw.forEach(function (x) {
      if (x) allowedSet[String(x)] = true;
    });
    var out = [];
    PG_TABLET_LAUNCH_MENU_ORDER.forEach(function (url) {
      if (allowedSet[url] && isMenuAllowed(url) && out.indexOf(url) === -1) out.push(url);
    });
    raw.forEach(function (url) {
      if (!url || out.indexOf(url) !== -1) return;
      if (isMenuAllowed(url)) out.push(url);
    });
    return out;
  }

  function menuLabelForUrl(url) {
    var loc = 'KO';
    if (w.PG_PAY_LIST_I18N && typeof w.PG_PAY_LIST_I18N.getLocale === 'function') {
      loc = w.PG_PAY_LIST_I18N.getLocale();
    }
    var info = (w.PG_MENU_INFO && w.PG_MENU_INFO[url]) || {};
    var koLabel = info.label || '';
    if (w.PG_ADMIN_SHELL_I18N && typeof w.PG_ADMIN_SHELL_I18N.tUrlLabel === 'function') {
      return w.PG_ADMIN_SHELL_I18N.tUrlLabel(url, loc, koLabel);
    }
    return koLabel || url;
  }

  function tileMetaForUrl(url) {
    var meta = PG_TABLET_LAUNCH_TILE[url];
    if (meta) return meta;
    return { icon: 'bi-grid-fill', tone: 'default' };
  }

  function isMerchantUser(u) {
    if (!u || typeof u !== 'object') return false;
    if (String(u.role || '').toUpperCase() === 'ADMIN') return false;
    return String(u.orgLevel != null ? u.orgLevel : '').toUpperCase() === 'MERCHANT';
  }

  function ynIsY(v) {
    return String(v != null ? v : 'N').trim().toUpperCase() === 'Y';
  }

  function paymentPublicBase() {
    if (typeof w.pgResolvePaymentBaseUrl === 'function') return w.pgResolvePaymentBaseUrl();
    var base = (w.SITE_CONFIG && w.SITE_CONFIG.paymentBaseUrl) || (w.location.origin || '');
    return String(base).replace(/\/$/, '');
  }

  function buildUrlPayPublicUrl(compId) {
    if (!compId) return '';
    return paymentPublicBase() + '/pay/' + encodeURIComponent(String(compId).trim());
  }

  function buildChatbotPayPublicUrl(compId) {
    if (!compId) return '';
    return paymentPublicBase() + '/chatbot-pay/' + encodeURIComponent(String(compId).trim());
  }

  function isShortcutFeatureEnabled(u, flag) {
    if (flag === 'web') return ynIsY(u.webPaymentUseYn);
    if (flag === 'chatbot') return ynIsY(u.chatbotPaymentUseYn);
    return false;
  }

  function getMerchantShortcutTiles(u) {
    u = u || getSessionUser();
    if (!isMerchantUser(u)) return [];
    var compId = u.compId != null ? String(u.compId).trim() : '';
    var urlPay = buildUrlPayPublicUrl(compId);
    var urlCb = buildChatbotPayPublicUrl(compId);
    return MERCHANT_TABLET_SHORTCUTS.map(function (def) {
      var enabled = !!compId && isShortcutFeatureEnabled(u, def.flag);
      var payUrl = def.flag === 'web' ? urlPay : urlCb;
      return {
        action: def.action,
        label: uiT(def.labelKo),
        icon: def.icon,
        tone: def.tone,
        enabled: enabled,
        url: enabled ? payUrl : ''
      };
    });
  }

  function refreshMerchantPayFlagsThen(cb) {
    cb = typeof cb === 'function' ? cb : function () {};
    var u = getSessionUser();
    if (!isMerchantUser(u) || !u.compId || !w.PG_API || typeof w.PG_API.compDetail !== 'function') {
      cb();
      return;
    }
    w.PG_API.compDetail(String(u.compId).trim()).then(function (d) {
      if (d) {
        try {
          var prev = JSON.parse(w.sessionStorage.getItem('pg_admin_user') || '{}') || {};
          if (d.webPaymentUseYn != null) prev.webPaymentUseYn = String(d.webPaymentUseYn);
          if (d.chatbotPaymentUseYn != null) prev.chatbotPaymentUseYn = String(d.chatbotPaymentUseYn);
          if (d.apiJpaySubscriptionUseYn != null) prev.apiJpaySubscriptionUseYn = String(d.apiJpaySubscriptionUseYn);
          w.sessionStorage.setItem('pg_admin_user', JSON.stringify(prev));
        } catch (eSt) { /* ignore */ }
      }
      cb();
    }).catch(function () { cb(); });
  }

  function copyPayUrlToClipboard(url) {
    if (!url) return;
    function ok() { alert(uiT('복사되었습니다.')); }
    function fail() { alert(uiT('복사 실패')); }
    if (w.navigator.clipboard && typeof w.navigator.clipboard.writeText === 'function') {
      w.navigator.clipboard.writeText(url).then(ok).catch(fail);
      return;
    }
    try {
      var ta = w.document.createElement('textarea');
      ta.value = url;
      ta.setAttribute('readonly', '');
      ta.style.position = 'fixed';
      ta.style.left = '-9999px';
      w.document.body.appendChild(ta);
      ta.select();
      if (w.document.execCommand('copy')) ok();
      else fail();
      w.document.body.removeChild(ta);
    } catch (eCp) {
      fail();
    }
  }

  function handleMerchantShortcutAction(action, url) {
    if (!url) {
      alert(uiT('해당 결제 기능이 「사용」으로 설정되어 있지 않습니다. 업체정보에서 웹결제·챗봇결제 사용여부를 확인하세요.'));
      return;
    }
    if (action.indexOf('-open') !== -1) {
      w.open(url, '_blank', 'noopener,noreferrer');
      return;
    }
    if (action.indexOf('-copy') !== -1) {
      copyPayUrlToClipboard(url);
    }
  }

  function renderShortcutTileHtml(item) {
    var cls = 'pg-tablet-launch-tile pg-tablet-launch-tile--' + escAttr(item.tone);
    if (!item.enabled) cls += ' is-disabled';
    var dis = item.enabled ? '' : ' disabled';
    var ariaDis = item.enabled ? '' : ' aria-disabled="true"';
    var urlAttr = item.enabled && item.url
      ? ' data-pg-pay-url="' + escAttr(item.url) + '"'
      : '';
    return '<div class="pg-tablet-launch-cell pg-tablet-launch-cell--merchant" role="listitem">' +
      '<button type="button" class="' + cls + '"' +
      ' data-pg-tablet-action="' + escAttr(item.action) + '"' + urlAttr + dis + ariaDis +
      ' aria-label="' + escAttr(item.label) + '">' +
      '<span class="pg-tablet-launch-icon-wrap" aria-hidden="true">' +
      '<i class="bi ' + escAttr(item.icon) + '"></i></span>' +
      '<span class="pg-tablet-launch-lbl">' + escHtml(item.label) + '</span>' +
      '</button></div>';
  }

  function syncContentsMainTabletClass(showLauncher) {
    var root = w.document.getElementById('contentsMain');
    if (!root) return;
    if (showLauncher) root.classList.add('pg-tablet-main-active');
    else root.classList.remove('pg-tablet-main-active');
  }

  function renderTabletLaunchBoard() {
    var board = w.document.getElementById('pgTabletLaunchBoard');
    if (!board) return;
    var u = getSessionUser();
    var urls = getOrderedTabletLaunchUrls(u);
    var merchantTiles = isMerchantUser(u) ? getMerchantShortcutTiles(u) : [];
    if (!urls.length && !merchantTiles.length) {
      board.innerHTML = '<div class="pg-tablet-launch-empty">' +
        '<p class="pg-tablet-launch-empty-title">' + escHtml(uiT('사용 가능한 메뉴가 없습니다')) + '</p>' +
        '<p class="pg-tablet-launch-empty-desc mb-0">' +
        escHtml(uiT('태블릿 모드에서 사용할 메뉴가 없습니다. 본사권한설정·태블릿설정을 확인하세요.')) + '</p></div>';
      return;
    }
    var h = '<div class="pg-tablet-launch-hero">' +
      '<h2 class="pg-tablet-launch-title">' + escHtml(uiT('태블릿 메뉴')) + '</h2>' +
      '<p class="pg-tablet-launch-subtitle mb-0">' +
      escHtml(uiT('아이콘을 눌러 업무 화면을 여세요.')) + '</p></div>';
    if (merchantTiles.length) {
      h += '<div class="pg-tablet-launch-section pg-tablet-launch-section--merchant">' +
        '<div class="pg-tablet-launch-section-heading">' +
        '<p class="pg-tablet-launch-section-title mb-0">' + escHtml(uiT('나의 결제')) + '</p>' +
        '<span class="pg-tablet-launch-section-note">' +
        escHtml(uiT('(별도 계약 시 지원 되는 서비스입니다,)')) + '</span></div>' +
        '<div class="pg-tablet-launch-grid pg-tablet-launch-grid--merchant" role="list">';
      merchantTiles.forEach(function (item) {
        h += renderShortcutTileHtml(item);
      });
      h += '</div></div>';
    }
    if (urls.length) {
      if (merchantTiles.length) {
        h += '<p class="pg-tablet-launch-section-title pg-tablet-launch-section-title--menus">' +
          escHtml(uiT('업무 메뉴')) + '</p>';
      }
      h += '<div class="pg-tablet-launch-grid" role="list">';
      urls.forEach(function (url) {
        var tm = tileMetaForUrl(url);
        var lab = menuLabelForUrl(url);
        h += '<div class="pg-tablet-launch-cell" role="listitem">' +
          '<button type="button" class="pg-tablet-launch-tile pg-tablet-launch-tile--' + escAttr(tm.tone) + '"' +
          ' data-pg-tablet-url="' + escAttr(url) + '"' +
          ' aria-label="' + escAttr(lab) + '">' +
          '<span class="pg-tablet-launch-icon-wrap" aria-hidden="true">' +
          '<i class="bi ' + escAttr(tm.icon) + '"></i></span>' +
          '<span class="pg-tablet-launch-lbl">' + escHtml(lab) + '</span>' +
          '</button></div>';
      });
      h += '</div>';
    }
    board.innerHTML = h;
    if (!board._pgTabletLaunchBound) {
      board._pgTabletLaunchBound = true;
      board.addEventListener('click', function (ev) {
        var btn = ev.target && ev.target.closest
          ? ev.target.closest('[data-pg-tablet-url], [data-pg-tablet-action]')
          : null;
        if (!btn || btn.disabled || btn.classList.contains('is-disabled')) return;
        var action = btn.getAttribute('data-pg-tablet-action');
        if (action) {
          handleMerchantShortcutAction(action, btn.getAttribute('data-pg-pay-url') || '');
          return;
        }
        var u0 = btn.getAttribute('data-pg-tablet-url');
        if (u0 && typeof w.fnTopMenuMove === 'function') w.fnTopMenuMove(u0);
      });
    }
    if (w.PG_UI_I18N && typeof w.PG_UI_I18N.applyDom === 'function') {
      try { w.PG_UI_I18N.applyDom(board); } catch (eDom) { /* ignore */ }
    }
  }

  function syncTabletMainHomePane() {
    var showLauncher = w.PG_shouldShowTabletLaunchBoard();
    var board = w.document.getElementById('pgTabletLaunchBoard');
    var dash = w.document.getElementById('pgHomeDashboardMount');
    syncContentsMainTabletClass(showLauncher);
    if (board) {
      if (showLauncher) {
        board.classList.remove('d-none');
        var uPane = getSessionUser();
        if (isMerchantUser(uPane)) {
          refreshMerchantPayFlagsThen(function () {
            renderTabletLaunchBoard();
          });
        } else {
          renderTabletLaunchBoard();
        }
      } else {
        board.classList.add('d-none');
      }
    }
    if (dash) {
      if (showLauncher) dash.classList.add('d-none');
      else dash.classList.remove('d-none');
    }
  }

  function setTabletShellMode(on) {
    try {
      if (on) w.sessionStorage.setItem(SHELL_KEY, '1');
      else w.sessionStorage.removeItem(SHELL_KEY);
    } catch (eSet) { /* ignore */ }
    syncTabletShellClass();
    hideFlyout();
    syncTabletShellToggleUi();
    syncTabletMainHomePane();
    if (w.PG_LOGIN_TABLET_SHELL && typeof w.PG_LOGIN_TABLET_SHELL.syncLastLoginModeFromSessionShell === 'function') {
      w.PG_LOGIN_TABLET_SHELL.syncLastLoginModeFromSessionShell();
    }
    if (typeof w.pgRefreshMainDashboardIfActive === 'function') {
      w.pgRefreshMainDashboardIfActive({ invalidate: true });
    }
  }

  w.PG_shouldShowTabletLaunchBoard = function () {
    if (!isTabletShellMode()) return false;
    var u = getSessionUser();
    if (!tabletShellCanActivate(u)) return false;
    if (isMerchantUser(u) && getMerchantShortcutTiles(u).length > 0) return true;
    return getOrderedTabletLaunchUrls(u).length > 0;
  };

  w.PG_syncTabletLaunchBoard = function () {
    syncTabletShellClass();
    syncTabletShellToggleUi();
    syncTabletMainHomePane();
  };

  function bindToggle() {
    var toggle = w.document.getElementById('pgTabletShellToggle');
    if (!toggle || toggle._pgTabletBound) return;
    toggle._pgTabletBound = true;
    toggle.addEventListener('change', function () {
      var u = getSessionUser();
      if (toggle.checked && !tabletShellCanActivate(u)) {
        toggle.checked = false;
        alert(uiT('태블릿 모드에서 사용할 메뉴가 없습니다. 본사권한설정·태블릿설정을 확인하세요.'));
        return;
      }
      var urls = Array.isArray(u.tabletMenuUrls) ? u.tabletMenuUrls : [];
      if (toggle.checked && urls.length === 0 && !isMerchantUser(u)) {
        toggle.checked = false;
        alert(uiT('태블릿 모드에서 사용할 메뉴가 없습니다. 본사권한설정·태블릿설정을 확인하세요.'));
        return;
      }
      setTabletShellMode(!!toggle.checked);
    });
  }

  function init() {
    bindToggle();
    w.PG_syncTabletLaunchBoard();
  }

  w.PG_ADMIN_TABLET_SHELL = {
    SHELL_KEY: SHELL_KEY,
    isTabletShellMode: isTabletShellMode,
    setTabletShellMode: setTabletShellMode,
    tabletShellCanActivate: tabletShellCanActivate,
    init: init,
    sync: w.PG_syncTabletLaunchBoard
  };

  if (w.document.readyState === 'loading') {
    w.document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})(typeof window !== 'undefined' ? window : globalThis);
