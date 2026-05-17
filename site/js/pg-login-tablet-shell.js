/**
 * 로그인 직후 태블릿 UI(pg_admin_tablet_shell) — 기본은「최근 로그인 방식」, 변경이 필요할 때만 로그인 화면에서 선택.
 * index의 sessionStorage 키와 동일합니다.
 */
(function (g) {
  'use strict';

  var LAST_KEY = 'pg_admin_login_tablet_last';
  var LEGACY_PREF_KEY = 'pg_admin_login_tablet_preference';
  var SHELL_KEY = 'pg_admin_tablet_shell';
  var CHOICE_KEY = 'pg_login_ui_mode_choice';
  var SELECT_ID = 'pgLoginUiModeSelect';

  function tabletShellCanActivate(res) {
    if (!res || typeof res !== 'object') return false;
    var urls = Array.isArray(res.tabletMenuUrls) ? res.tabletMenuUrls : [];
    var roleU = String(res.role != null ? res.role : '').toUpperCase();
    var orgTf = String(res.tabletFeatureUseYn != null ? res.tabletFeatureUseYn : 'Y').trim().toUpperCase();
    var orgOk = roleU === 'ADMIN' || orgTf === 'Y';
    if (!orgOk) return false;
    if (roleU !== 'ADMIN' && String(res.orgLevel != null ? res.orgLevel : '').toUpperCase() === 'MERCHANT') {
      return true;
    }
    return urls.length > 0;
  }

  /** iPad·Android 태블릿 등 — iPhone은 첫 방문 fallback에서 제외 */
  function detectLikelyTabletDevice() {
    var ua = (g.navigator.userAgent || '').toLowerCase();
    var platform = g.navigator.platform || '';
    var maxTouch = g.navigator.maxTouchPoints > 0 ? g.navigator.maxTouchPoints : 0;
    if (ua.indexOf('iphone') !== -1) return false;
    if (ua.indexOf('ipad') !== -1) return true;
    if (platform === 'MacIntel' && maxTouch > 1) return true;
    if (ua.indexOf('android') !== -1) {
      if (ua.indexOf('mobile') === -1) return true;
      try {
        var sw = g.screen && g.screen.width ? g.screen.width : 0;
        var sh = g.screen && g.screen.height ? g.screen.height : 0;
        var shortSide = sw && sh ? Math.min(sw, sh) : 0;
        if (shortSide >= 600) return true;
      } catch (e0) { /* ignore */ }
    }
    try {
      if (g.matchMedia && g.matchMedia('(pointer: coarse) and (min-width: 768px)').matches) return true;
    } catch (e1) { /* ignore */ }
    return false;
  }

  /** 마지막으로 적용된 로그인 UI: tablet | desktop (없으면 null) */
  function getLastLoginMode() {
    try {
      var v = g.localStorage.getItem(LAST_KEY);
      if (v === 'tablet' || v === 'desktop') return v;
      var leg = g.localStorage.getItem(LEGACY_PREF_KEY);
      if (leg === 'tablet' || leg === 'desktop') return leg;
    } catch (e) { /* ignore */ }
    return null;
  }

  function persistLastLoginMode() {
    try {
      var on = g.sessionStorage.getItem(SHELL_KEY) === '1';
      g.localStorage.setItem(LAST_KEY, on ? 'tablet' : 'desktop');
    } catch (e2) { /* ignore */ }
  }

  /** 관리자 화면에서 태블릿 스위치를 바꾼 뒤에도「최근 방식」과 맞춤 */
  function syncLastLoginModeFromSessionShell() {
    persistLastLoginMode();
  }

  function getSelectEl() {
    try {
      return g.document.getElementById(SELECT_ID);
    } catch (eSel) { return null; }
  }

  function captureLoginUiChoice() {
    try {
      var sel = getSelectEl();
      var v = sel && sel.value ? String(sel.value) : '';
      if (v === 'tablet' || v === 'desktop' || v === 'same') {
        g.sessionStorage.setItem(CHOICE_KEY, v);
      }
    } catch (eCap) { /* ignore */ }
  }

  function clearLoginUiChoice() {
    try { g.sessionStorage.removeItem(CHOICE_KEY); } catch (eClr) { /* ignore */ }
  }

  function getSelectedLoginUiMode() {
    try {
      var sel = getSelectEl();
      var v = sel && sel.value ? String(sel.value) : '';
      if (v === 'tablet' || v === 'desktop') return v;
      if (v === 'same') {
        var pend = g.sessionStorage.getItem(CHOICE_KEY);
        if (pend === 'tablet' || pend === 'desktop') return pend;
        return 'same';
      }
      var pend2 = g.sessionStorage.getItem(CHOICE_KEY);
      if (pend2 === 'tablet' || pend2 === 'desktop' || pend2 === 'same') return pend2;
    } catch (e3) { /* ignore */ }
    return 'same';
  }

  /**
   * 로그인 응답(또는 user 객체) 기준으로 태블릿 셸 설정 후, 실제 결과를 LAST_KEY에 반영.
   */
  function applyShellAfterAuth(resData) {
    try {
      var choice = getSelectedLoginUiMode();
      if (!tabletShellCanActivate(resData)) {
        g.sessionStorage.removeItem(SHELL_KEY);
        persistLastLoginMode();
        return;
      }
      if (choice === 'tablet') {
        g.sessionStorage.setItem(SHELL_KEY, '1');
      } else if (choice === 'desktop') {
        g.sessionStorage.removeItem(SHELL_KEY);
      } else {
        var last = getLastLoginMode();
        if (last === 'tablet') g.sessionStorage.setItem(SHELL_KEY, '1');
        else if (last === 'desktop') g.sessionStorage.removeItem(SHELL_KEY);
        else if (detectLikelyTabletDevice()) g.sessionStorage.setItem(SHELL_KEY, '1');
        else g.sessionStorage.removeItem(SHELL_KEY);
      }
      persistLastLoginMode();
      clearLoginUiChoice();
    } catch (e4) { /* ignore */ }
  }

  function tUi(ko, en) {
    try {
      if (g.PG_UI_I18N && typeof g.PG_UI_I18N.t === 'function') return String(g.PG_UI_I18N.t(String(ko)));
    } catch (e) { /* ignore */ }
    return en != null ? String(en) : String(ko);
  }

  function updateHint(root) {
    if (!root) return;
    var hint = root.querySelector('[data-pg-login-tablet-hint]');
    if (!hint) return;
    var mode = getSelectedLoginUiMode();
    var last = getLastLoginMode();
    if (mode === 'tablet') {
      hint.textContent = tUi('이번 로그인만 태블릿 보드 UI로 시작합니다.', 'This sign-in will start with the tablet board UI.');
    } else if (mode === 'desktop') {
      hint.textContent = tUi('이번 로그인만 일반(좌측 메뉴) 화면으로 시작합니다.', 'This sign-in will start with the standard layout.');
    } else if (last === 'tablet') {
      hint.textContent = tUi('기본: 최근에 태블릿 UI로 접속했습니다. 바꾸려면 아래에서 선택하세요.', 'Default: your last sign-in used tablet UI. Change below only if needed.');
    } else if (last === 'desktop') {
      hint.textContent = tUi('기본: 최근에 일반 화면으로 접속했습니다. 바꾸려면 아래에서 선택하세요.', 'Default: your last sign-in used the standard layout. Change below only if needed.');
    } else {
      hint.textContent = tUi('첫 로그인: 기기에 따라 태블릿 또는 일반으로 시작합니다. 이후에는 최근 접속 방식이 기본입니다.', 'First sign-in: starts by device hint; later visits default to your last mode.');
    }
  }

  function initLoginPanel(panelRoot) {
    if (!panelRoot) return;
    var sel = getSelectEl();
    if (panelRoot.getAttribute('data-pg-tablet-pref-init') !== '1') {
      panelRoot.setAttribute('data-pg-tablet-pref-init', '1');
      clearLoginUiChoice();
      if (sel) sel.value = 'same';
      panelRoot.addEventListener('change', function (ev) {
        var t = ev.target;
        if (!t || t.id !== SELECT_ID) return;
        captureLoginUiChoice();
        updateHint(panelRoot);
      });
    }
    updateHint(panelRoot);
  }

  g.PG_LOGIN_TABLET_SHELL = {
    SHELL_KEY: SHELL_KEY,
    LAST_KEY: LAST_KEY,
    tabletShellCanActivate: tabletShellCanActivate,
    detectLikelyTabletDevice: detectLikelyTabletDevice,
    getLastLoginMode: getLastLoginMode,
    applyShellAfterAuth: applyShellAfterAuth,
    captureLoginUiChoice: captureLoginUiChoice,
    clearLoginUiChoice: clearLoginUiChoice,
    initLoginPanel: initLoginPanel,
    syncLastLoginModeFromSessionShell: syncLastLoginModeFromSessionShell
  };
})(typeof window !== 'undefined' ? window : globalThis);
