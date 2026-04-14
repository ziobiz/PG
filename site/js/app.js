/**
 * PG 솔루션 - fxhj와 동일한 메뉴 구성, 화면은 하나씩 구현
 * fnTopMenuMove(url): 메뉴 클릭 시 탭 추가/전환, 우리 페이지 또는 placeholder 표시
 */

(function () {
  'use strict';

  (function initPgUiGlobalModals() {
    if (window._pgUiGlobalModalsInited) return;
    window._pgUiGlobalModalsInited = true;
    window._pgCompLoginIdPending = { loginIdEl: null };
    window._pgHqAccountAccessPending = { pane: null, tabId: null };
    var submitLogin = document.getElementById('pgCompLoginIdChangeSubmitBtn');
    if (submitLogin) {
      submitLogin.addEventListener('click', function () {
        var compId = (document.getElementById('pgCompLoginIdChangeCompId') || {}).value || '';
        var newId = (document.getElementById('pgCompLoginIdChangeNewId') || {}).value || '';
        newId = String(newId).trim();
        if (!compId || !newId) return;
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.compChangeLoginId(compId, newId).then(function () {
          var el = window._pgCompLoginIdPending && window._pgCompLoginIdPending.loginIdEl;
          if (el) el.value = newId;
          if (window.PG_UI && window.PG_UI.closeModal) {
            window.PG_UI.closeModal(document.getElementById('pgCompLoginIdChangeModal'));
          }
        }).catch(function (err) {
          alert(err && err.message ? err.message : 'ID 변경 실패');
        }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      });
    }
    var submitAcc = document.getElementById('pgHqAccountAccessAddSubmitBtn');
    if (submitAcc) {
      submitAcc.addEventListener('click', function () {
        var uid = (document.getElementById('pgHqAccountAccessUsername') || {}).value || '';
        var cc = (document.getElementById('pgHqAccountAccessCompSelect') || {}).value || '';
        var editIdRaw = (document.getElementById('pgHqAccountAccessEditId') || {}).value || '';
        uid = String(uid).trim();
        cc = String(cc).trim();
        if (!uid) {
          alert('사용자 ID를 선택하세요.');
          return;
        }
        if (!cc) {
          alert('허용 업체코드(업체)를 선택하세요.');
          return;
        }
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        var editId = parseInt(String(editIdRaw).trim(), 10);
        var savePromise = (!isNaN(editId) && editId > 0 && window.PG_API && typeof window.PG_API.hqAccountAccessUpdate === 'function')
          ? window.PG_API.hqAccountAccessUpdate(editId, { username: uid, compCode: cc })
          : window.PG_API.hqAccountAccessAdd({ username: uid, compCode: cc });
        savePromise.then(function () {
          if (window.PG_UI && window.PG_UI.closeModal) {
            window.PG_UI.closeModal(document.getElementById('pgHqAccountAccessAddModal'));
          }
          var pend = window._pgHqAccountAccessPending;
          if (pend && pend.pane && typeof pend.pane._pgRunListSearch === 'function') {
            pend.pane._pgRunListSearch(pend.pane, pend.tabId, 1);
          }
        }).catch(function (err) {
          alert(err && err.message ? err.message : '저장 실패');
        }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      });
    }
    var accAccessModalEl = document.getElementById('pgHqAccountAccessAddModal');
    if (accAccessModalEl && !accAccessModalEl._pgHqAccModalResetBound) {
      accAccessModalEl._pgHqAccModalResetBound = true;
      accAccessModalEl.addEventListener('hidden.bs.modal', function () {
        var idEl = document.getElementById('pgHqAccountAccessEditId');
        var titleEl = document.getElementById('pgHqAccountAccessModalTitle');
        if (idEl) idEl.value = '';
        if (titleEl) titleEl.textContent = '접근권한 추가';
      });
    }
    (function initHqAccountAccessAddModalSelects() {
      var compSel = document.getElementById('pgHqAccountAccessCompSelect');
      var userSel = document.getElementById('pgHqAccountAccessUsername');
      if (!compSel || !userSel || userSel._pgHqAccUserFlowBound) return;
      userSel._pgHqAccUserFlowBound = true;
      function syncCompEnabledForUser() {
        var hasUser = String(userSel.value || '').trim();
        compSel.disabled = !hasUser;
      }
      function fillHqAccUserSelectFromCache() {
        var all = window._pgHqAccountAccessAllUsers || [];
        userSel.innerHTML = '';
        var ph = document.createElement('option');
        ph.value = '';
        ph.disabled = true;
        ph.selected = true;
        ph.textContent = '사용자를 선택하세요';
        userSel.appendChild(ph);
        if (!all.length) {
          userSel.disabled = true;
          compSel.disabled = true;
          return;
        }
        userSel.disabled = false;
        all.slice().sort(function (a, b) {
          var ua = String((a && a.username) || '').toLowerCase();
          var ub = String((b && b.username) || '').toLowerCase();
          if (ua < ub) return -1;
          if (ua > ub) return 1;
          return 0;
        }).forEach(function (u) {
          var v = String(u && u.username != null ? u.username : '').trim();
          if (!v) return;
          var nm = String(u && u.name != null ? u.name : '').trim();
          var ouc = String(u && u.orgUnitCode != null ? u.orgUnitCode : '').trim();
          var opt = document.createElement('option');
          opt.value = v;
          opt.textContent = nm ? (nm + ' · ' + v + (ouc ? ' · 소속 ' + ouc : '')) : (v + (ouc ? ' · 소속 ' + ouc : ''));
          userSel.appendChild(opt);
        });
        syncCompEnabledForUser();
      }
      window._pgFillHqAccountAccessUserSelect = fillHqAccUserSelectFromCache;
      userSel.addEventListener('change', syncCompEnabledForUser);
    })();
  })();

  /**
   * %·건당·고정액 공통: 소수 첫째 자리까지, 소수부가 0이면 정수만 (300.0→300, 5.6→5.6).
   * 빈 값은 빈 문자열 유지(입력 필드용).
   */
  function pgFmtOneDecimalStripWhole(v) {
    if (v == null || v === '') return '';
    var s = String(v).replace(/,/g, '.').trim();
    if (s === '') return '';
    var n = parseFloat(s);
    if (!isFinite(n)) return String(v);
    var x = Math.round(n * 10) / 10;
    if (Math.abs(x - Math.round(x)) < 1e-9) {
      return String(Math.round(x));
    }
    return x.toFixed(1);
  }

  /** % 필드: 소수 첫째 자리까지, 정수일 때는 .0 생략 */
  function pgFmtPctOneDecimal(v) {
    var out = pgFmtOneDecimalStripWhole(v == null || v === '' ? '0' : v);
    return out === '' ? '0' : out;
  }
  function pgFmtPctOneDecimalInput(v) {
    return pgFmtOneDecimalStripWhole(v);
  }

  /** 본사 정산주기 API 결과로 가맹 정산주기 셀렉트·검색 옵션 갱신 */
  function pgInvalidateCalcCycleOptionsCache() {
    try { window._pgCalcCycleOptsCache = null; } catch (e0) { /* ignore */ }
    try { window._pgCalcCycleOptsScopedCache = {}; } catch (e0b) { /* ignore */ }
  }
  /** SettlementPeriodResolver.normalizeCalcCycle 과 동일(총판 슬롯·셀렉트 value 매칭). */
  function pgNormCalcCycleCode(raw) {
    if (raw == null) return '';
    var u = String(raw).trim().toUpperCase().replace(/\+/g, '');
    if (u === 'TM05') u = 'TM5';
    switch (u) {
      case '1D': return 'H1';
      case '2D': return 'H2';
      case '4D': return 'H4';
      case '6D': return 'H6';
      case '8D': return 'H8';
      case '12D': return 'H12';
      default: return u;
    }
  }
  /** 상위 조직(parent) 변경 시 총판별 정산주기 스코프 캐시만 비움(동일 parentId라도 본사 설정 반영 재조회) */
  function pgClearCalcCycleScopedCache() {
    try { window._pgCalcCycleOptsScopedCache = {}; } catch (e) { /* ignore */ }
  }

  /**
   * 가맹 정산주기 스코프: 상위검색에서 내려온 총판 org id(dataset) 우선, 없으면 parentId(조직 단위 id).
   * 목록 행의 masterDistScopeOrgId를 쓰면 지사·대리점 등을 상위로 둘 때도 총판 슬롯·대표주기와 일치합니다.
   */
  function pgMerchantCalcCycleScopeKeyForForm(form) {
    if (!form) return null;
    try {
      var ds = form.dataset && form.dataset.masterDistScopeOrgId;
      if (ds != null && String(ds).trim() !== '') return String(ds).trim();
      var p = form.querySelector('input[name="parentId"]');
      if (p && p.value != null && String(p.value).trim() !== '') return String(p.value).trim();
    } catch (e0) { /* ignore */ }
    return null;
  }
  try { window.pgMerchantCalcCycleScopeKeyForForm = pgMerchantCalcCycleScopeKeyForForm; } catch (eMd) { /* ignore */ }

  function pgEscapeHtml(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/"/g, '&quot;');
  }

  function pgEscapeAttr(s) {
    return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
  }

  function pgCalcCycleScopedCacheKey(scopeKey, ensureCode) {
    var a = (scopeKey != null && String(scopeKey).trim() !== '') ? String(scopeKey).trim() : '';
    var b = (ensureCode != null && String(ensureCode).trim() !== '') ? String(ensureCode).trim() : '';
    return a + '\u0000' + b;
  }
  /** 가맹 총판 한정 셀렉트: API 라벨이 CODE(설명) 형태일 때 괄호 안 설명만 표시. 코드는 value·title(설명)에 유지 */
  function pgMerchantCalcCycleOptionLabel(o) {
    var val = o.v != null ? String(o.v).trim() : '';
    var lab = o.t != null ? String(o.t) : val;
    if (!val) return lab;
    if (lab.indexOf(val + '(') === 0) {
      var inner = lab.slice(val.length + 1).replace(/\)\s*$/g, '');
      return inner || lab;
    }
    return lab;
  }
  function pgRefreshCalcCycleSelects(rootEl, preferredValue, scopeFromOrgUnitId, ensureCycleCode) {
    var hasGlobal = !!(window.PG_API && window.PG_API.hqSettlementCycleOptions);
    var hasScoped = !!(window.PG_API && window.PG_API.hqSettlementCycleOptionsScoped);
    if (!window.PG_API || (!hasGlobal && !hasScoped)) return Promise.resolve();
    var scopeKey = (scopeFromOrgUnitId != null && String(scopeFromOrgUnitId).trim() !== '')
      ? String(scopeFromOrgUnitId).trim() : '';
    var ensure = (ensureCycleCode != null && String(ensureCycleCode).trim() !== '') ? String(ensureCycleCode).trim() : '';
    function applyOptsFromPack(pack) {
      var opts = (pack && pack.options) ? pack.options : [];
      if (!Array.isArray(opts) || !opts.length) return;
      var def = pack && pack.defaultCalcCycle != null ? String(pack.defaultCalcCycle).trim() : '';
      var scopedOn = !!(pack && pack.scoped);
      var mdNm = pack && pack.masterDistCompNm != null ? String(pack.masterDistCompNm).trim() : '';
      var mdId = pack && pack.masterDistCompId != null ? String(pack.masterDistCompId).trim() : '';
      window.PG_CALC_CYCLE_OPTIONS = opts;
      window.PG_CALC_CYCLE_SEARCH_OPTIONS = [{ v: '', t: '전체' }].concat(opts.filter(function (o) { return String(o.v || '') !== ''; }));
      var roots = [];
      if (rootEl) roots.push(rootEl);
      else roots.push(document);
      roots.forEach(function (root) {
        root.querySelectorAll('select[name="calcCycle"]').forEach(function (sel) {
          var v;
          if (preferredValue !== undefined && preferredValue !== null && String(preferredValue).trim() !== '') {
            v = String(preferredValue).trim();
          } else if (preferredValue !== undefined && (preferredValue === null || String(preferredValue).trim() === '')) {
            v = '';
          } else if (def) {
            v = def;
          } else {
            v = String(sel.value || '').trim();
          }
          var html = opts.map(function (o) {
            var val = o.v != null ? String(o.v) : '';
            var lab = o.t != null ? String(o.t) : val;
            if (scopedOn && val) lab = pgMerchantCalcCycleOptionLabel(o);
            var ttl = o.d ? ' title="' + pgEscapeAttr(val + (val && o.d ? ' — ' : '') + String(o.d)) + '"' : (val ? ' title="' + pgEscapeAttr(val) + '"' : '');
            var selAttr = (val === v) ? ' selected' : '';
            return '<option value="' + pgEscapeAttr(val) + '"' + ttl + selAttr + '>' + pgEscapeHtml(lab) + '</option>';
          }).join('');
          sel.innerHTML = html;
          var has = Array.prototype.some.call(sel.options, function (op) { return op.value === v; });
          if (v && !has) {
            var o = document.createElement('option');
            o.value = v;
            o.textContent = v + ' (저장값)';
            o.selected = true;
            sel.appendChild(o);
          } else if (v) {
            sel.value = v;
          }
          if (scopedOn) {
            var nonEmpty = opts.filter(function (o) { return String(o.v || '').trim() !== ''; });
            var parts = nonEmpty.map(function (o) {
              return String(o.v || '') + ': ' + pgMerchantCalcCycleOptionLabel(o);
            });
            var who = mdNm || mdId || '소속 총판';
            var hint = '「' + who + '」총판 설정: 선택 가능 ' + parts.length + '종' + (parts.length ? ' (' + parts.join(', ') + ')' : '');
            if (def) hint += ' · 대표(기본 선택): ' + def;
            sel.title = hint;
            sel.setAttribute('data-pg-calc-cycle-scoped', '1');
            if (pack && String(pack.orphanSavedCycleYn || '').toUpperCase() === 'Y' && !sel._pgOrphanCycleWarned) {
              sel._pgOrphanCycleWarned = true;
              try {
                alert('저장된 정산주기가 현재 총판 허용 목록에 없어, 목록에 임시로 한 줄 추가되었습니다. 정산관리설정에서 해당 총판 허용 주기를 보정하거나, 가맹 주기를 허용값으로 변경해 주세요.');
              } catch (eW) { /* ignore */ }
            }
          } else {
            if (sel.getAttribute('data-pg-calc-cycle-scoped') === '1') {
              sel.removeAttribute('data-pg-calc-cycle-scoped');
              sel.removeAttribute('title');
            }
          }
        });
      });
    }
    if (scopeKey && window.PG_API.hqSettlementCycleOptionsScoped) {
      if (!window._pgCalcCycleOptsScopedCache) window._pgCalcCycleOptsScopedCache = {};
      var scopedCacheKey = pgCalcCycleScopedCacheKey(scopeKey, ensure);
      if (window._pgCalcCycleOptsScopedCache[scopedCacheKey]) {
        applyOptsFromPack(window._pgCalcCycleOptsScopedCache[scopedCacheKey]);
        return Promise.resolve();
      }
      var scopedParams = { fromOrgUnitId: scopeKey };
      if (ensure) scopedParams.ensureCycleCode = ensure;
      return window.PG_API.hqSettlementCycleOptionsScoped(scopedParams).then(function (pack) {
        window._pgCalcCycleOptsScopedCache[scopedCacheKey] = pack;
        applyOptsFromPack(pack);
      }).catch(function () {
        if (!hasGlobal) return Promise.resolve();
        return window.PG_API.hqSettlementCycleOptions().then(function (opts) {
          window._pgCalcCycleOptsCache = opts;
          applyOptsFromPack({ options: opts, defaultCalcCycle: null, scoped: false });
        });
      });
    }
    if (!scopeKey && window._pgCalcCycleOptsCache) {
      applyOptsFromPack({ options: window._pgCalcCycleOptsCache, defaultCalcCycle: null, scoped: false });
      return Promise.resolve();
    }
    if (!hasGlobal) return Promise.resolve();
    return window.PG_API.hqSettlementCycleOptions().then(function (opts) {
      window._pgCalcCycleOptsCache = opts;
      applyOptsFromPack({ options: opts, defaultCalcCycle: null, scoped: false });
    }).catch(function () { /* 업체 화면은 기본 CALC_CYCLE_OPTIONS 유지 */ });
  }
  try { window.pgInvalidateCalcCycleOptionsCache = pgInvalidateCalcCycleOptionsCache; } catch (e1) { /* ignore */ }
  try { window.pgClearCalcCycleScopedCache = pgClearCalcCycleScopedCache; } catch (e1b) { /* ignore */ }
  try { window.pgRefreshCalcCycleSelects = pgRefreshCalcCycleSelects; } catch (e2) { /* ignore */ }

  /** 헬로 타임라인: 전역(분) 동기 vs 페이지별 — 전산설정 helloTimelineEnabledYn */
  var PG_HELLO_TIMELINE_SS = 'pg_hello_timeline_until_ms';
  function pgHelloTimelineInvalidateConfigCache() {
    try { window._pgHelloTimelineFromLedger = null; } catch (e0) { /* ignore */ }
  }
  function pgHelloTimelineFetchConfig() {
    if (window._pgHelloTimelineFromLedger) return Promise.resolve(window._pgHelloTimelineFromLedger);
    if (!window.PG_API || !window.PG_API.hqLedgerSysSettings) return Promise.resolve(null);
    return window.PG_API.hqLedgerSysSettings().then(function (d) {
      if (!d) return null;
      var yn = String(d.helloTimelineEnabledYn || 'N').toUpperCase() === 'Y' ? 'Y' : 'N';
      var dm = parseInt(String(d.helloTimelineDurationMin != null ? d.helloTimelineDurationMin : '10'), 10);
      if (isNaN(dm) || dm < 1) dm = 10;
      if (dm > 1440) dm = 1440;
      window._pgHelloTimelineFromLedger = { helloTimelineEnabledYn: yn, helloTimelineDurationMin: dm };
      return window._pgHelloTimelineFromLedger;
    }).catch(function () { return null; });
  }
  function pgHelloTimelineIsEnabled() {
    var c = window._pgHelloTimelineFromLedger;
    return !!(c && String(c.helloTimelineEnabledYn || '').toUpperCase() === 'Y');
  }
  function pgHelloTimelineUntilMs() {
    try {
      var v = parseInt(sessionStorage.getItem(PG_HELLO_TIMELINE_SS) || '0', 10);
      return isNaN(v) ? 0 : v;
    } catch (e1) { return 0; }
  }
  function pgHelloTimelineIsGloballyShowing() {
    if (!pgHelloTimelineIsEnabled()) return false;
    return pgHelloTimelineUntilMs() > Date.now();
  }
  function pgHelloTimelineSetUntilMs(ms) {
    try {
      if (ms > 0) sessionStorage.setItem(PG_HELLO_TIMELINE_SS, String(ms));
      else sessionStorage.removeItem(PG_HELLO_TIMELINE_SS);
    } catch (e2) { /* ignore */ }
  }
  function pgHelloZonesSetHidden(pane, hidden) {
    if (!pane || !pane.querySelectorAll) return;
    pane.querySelectorAll('.pg-hello-toggle-zone').forEach(function (el) {
      if (hidden) el.classList.add('d-none');
      else el.classList.remove('d-none');
    });
  }
  function pgHelloSyncButtonForPane(pane, hidden) {
    if (!pane || !pane.id) return;
    var viewHelloBtn = document.getElementById('viewSettingHelloBtn_' + pane.id);
    if (!viewHelloBtn) return;
    viewHelloBtn.classList.toggle('btn-view-setting-hello--restore', !!hidden);
    viewHelloBtn.setAttribute('aria-pressed', hidden ? 'true' : 'false');
    var tlOn = pgHelloTimelineIsEnabled();
    var baseHidden = hidden
      ? '클릭 시 안내(파스텔)·VIEW SETTING을 다시 표시합니다.'
      : '클릭 시 안내(파스텔)·VIEW SETTING을 숨깁니다.';
    if (tlOn) {
      var u = pgHelloTimelineUntilMs();
      if (u > Date.now()) {
        var sec = Math.ceil((u - Date.now()) / 1000);
        var mm = Math.floor(sec / 60);
        var ss = sec % 60;
        viewHelloBtn.title = (hidden ? '안내 숨김' : '안내 표시') + ' — 헬로 타임라인 약 ' + mm + '분 ' + ss + '초 남음 (전 페이지 동기)';
      } else {
        viewHelloBtn.title = (hidden ? '안내 숨김(기본)' : '안내 표시') + ' — 헬로 타임라인: 클릭 시 전 페이지에 ' +
          ((window._pgHelloTimelineFromLedger && window._pgHelloTimelineFromLedger.helloTimelineDurationMin) || 10) + '분 동안 표시';
      }
    } else {
      viewHelloBtn.title = baseHidden;
    }
  }
  function pgHelloApplyTimelineToAllPanes(hidden) {
    document.querySelectorAll('.tab-pane').forEach(function (p) {
      if (!p || !p.querySelector) return;
      if (!p.querySelector('[id^="viewSettingHelloBtn_"]')) return;
      p._viewSettingHelloHidden = !!hidden;
      pgHelloZonesSetHidden(p, hidden);
      pgHelloSyncButtonForPane(p, hidden);
    });
    try {
      if (window.PG_TABLE_COL_RESIZE && typeof window.PG_TABLE_COL_RESIZE.refreshInSync === 'function') {
        document.querySelectorAll('.tab-pane').forEach(function (p) { window.PG_TABLE_COL_RESIZE.refreshInSync(p); });
      } else if (window.PG_TABLE_COL_RESIZE && typeof window.PG_TABLE_COL_RESIZE.refreshIn === 'function') {
        document.querySelectorAll('.tab-pane').forEach(function (p) { window.PG_TABLE_COL_RESIZE.refreshIn(p); });
      }
    } catch (eR) { /* ignore */ }
  }
  function pgHelloTimelineTick() {
    if (!pgHelloTimelineIsEnabled()) return;
    var u = pgHelloTimelineUntilMs();
    if (u > 0 && u <= Date.now()) {
      pgHelloTimelineSetUntilMs(0);
      pgHelloApplyTimelineToAllPanes(true);
      return;
    }
    if (u > Date.now()) {
      document.querySelectorAll('.tab-pane').forEach(function (p) {
        if (p.querySelector && p.querySelector('[id^="viewSettingHelloBtn_"]') && !p._viewSettingHelloHidden) {
          pgHelloSyncButtonForPane(p, false);
        }
      });
    }
  }
  if (!window._pgHelloTimelineTickStarted) {
    window._pgHelloTimelineTickStarted = true;
    setInterval(pgHelloTimelineTick, 15000);
  }
  try {
    window.pgHelloTimelineInvalidateConfigCache = pgHelloTimelineInvalidateConfigCache;
    window.pgHelloTimelineFetchConfig = pgHelloTimelineFetchConfig;
    window.pgHelloApplyTimelineToAllPanes = pgHelloApplyTimelineToAllPanes;
  } catch (eHT) { /* ignore */ }

  /**
   * 가맹점 수수료 정책 목록 등: 건당·고정액·% 셀 표시(통화 구분 없이 동일 규칙).
   * currencyCode는 호환용으로만 받으며 포맷에는 사용하지 않음.
   */
  function pgFmtPolicyListAmount(v, currencyCode) {
    var out = pgFmtOneDecimalStripWhole(v == null || v === '' ? '0' : v);
    return out === '' ? '0' : out;
  }

  /** 업체 상세 등: 수수료·보류 관련 숫자 필드만 동일 표기 규칙 적용. 해당 없으면 null. */
  function pgFmtCompDetailNumericField(fieldName, v) {
    var pctFields = { payRate: 1, feeUsdt: 1, feeFx: 1, rollingPct: 1, holdRate: 1 };
    var amtFields = { failFee: 1, usageRate: 1, cancelRate: 1, voidFeePerTx: 1, manualVoidFeePerTx: 1, refundRate: 1, feeSettlementPerTx: 1, remittanceTransferFee: 1, usdtTransferFeeUsd: 1, chargebackFeePerTx: 1, perTxFee: 1 };
    var dayFields = { rollingDays: 1, holdDays: 1 };
    if (pctFields[fieldName]) return pgFmtPctOneDecimalInput(v);
    if (amtFields[fieldName]) return pgFmtOneDecimalStripWhole(v);
    if (dayFields[fieldName]) {
      var n = parseFloat(String(v).replace(/,/g, '.'));
      return isFinite(n) ? String(Math.round(n)) : String(v);
    }
    return null;
  }

  /** 본사 수수료 템플릿: 배포(Y)만. 기준통화가 있으면 정책 통화와 일치하거나 정책 통화가 비어 있으면(레거시) 전체 허용 */
  function pgFilterDeployedTemplatesForMerchant(templates, baseCurrency) {
    var bc = (baseCurrency || '').trim().toUpperCase();
    var out = [];
    (templates || []).forEach(function (t) {
      if (String(t.deployYn || '').toUpperCase() !== 'Y') return;
      var tc = (t.currencyCode != null && String(t.currencyCode).trim() !== '') ? String(t.currencyCode).trim().toUpperCase() : '';
      if (bc && tc && tc !== bc) return;
      out.push(t);
    });
    return out;
  }
  /** 필터 결과에 없는데 DB에 저장된 scope가 있으면 전체 템플릿에서 찾아 옵션에 포함(미배포·통화 필터 제외 등). 없으면 scope 코드만 표시. */
  function pgAugmentFilteredHqTemplates(filt, allTemplates, savedScope) {
    var s = savedScope != null ? String(savedScope).trim() : '';
    if (!s) return filt || [];
    var arr = (filt || []).slice();
    var found = false;
    var i;
    for (i = 0; i < arr.length; i++) {
      if (String(arr[i].scope || '').trim() === s) {
        found = true;
        break;
      }
    }
    if (found) return arr;
    var full = (allTemplates || []).filter(function (t) {
      return String(t.scope || '').trim() === s;
    });
    if (full.length) {
      arr.push(full[0]);
      return arr;
    }
    arr.push({ scope: s, policyName: s + ' (저장된 정책·목록에 없음)', deployYn: 'N', currencyCode: '' });
    return arr;
  }
  function pgHqPolicyScopeOptionsHtml(filteredTemplates) {
    var opts = '<option value="">본사 기본 템플릿 (DEFAULT)</option>';
    (filteredTemplates || []).forEach(function (t) {
      var scope = t.scope || '';
      var name = t.policyName || scope;
      var cc = (t.currencyCode != null && String(t.currencyCode).trim() !== '') ? String(t.currencyCode).trim().toUpperCase() : '';
      var lab = name + (cc ? ' (' + cc + ')' : '');
      opts += '<option value="' + String(scope).replace(/"/g, '&quot;') + '">' +
        String(lab).replace(/</g, '&lt;').replace(/"/g, '&quot;') + '</option>';
    });
    return opts;
  }

  function pgToggleUsdDependentCommissionFields(rootEl, baseCurrency) {
    if (!rootEl) return;
    var isUsd = String(baseCurrency || '').trim().toUpperCase() === 'USD';
    ['usdtTransferFeeUsd', 'feeUsdt'].forEach(function (name) {
      var el = rootEl.querySelector('[name="' + name + '"]');
      if (!el) return;
      if (isUsd) {
        el.disabled = false;
      } else {
        el.disabled = true;
        el.value = '';
      }
    });
  }

  var COUNTRY_OTHER_TOP = ['CHINA', 'HONGKONG', 'INDONESIA', 'JAPAN', 'KOREA', 'MALAYSIA', 'SINGAPORE', 'THAILAND', 'USA', 'VIETNAM'];
  var COUNTRY_OTHER_REST = ['AFGHANISTAN', 'ALBANIA', 'ALGERIA', 'ARGENTINA', 'ARMENIA', 'AUSTRALIA', 'AUSTRIA', 'AZERBAIJAN', 'BAHRAIN', 'BANGLADESH', 'BELARUS', 'BELGIUM', 'BOLIVIA', 'BOSNIA', 'BRAZIL', 'BULGARIA', 'CAMBODIA', 'CAMEROON', 'CANADA', 'CHILE', 'COLOMBIA', 'COSTA RICA', 'CROATIA', 'CUBA', 'CYPRUS', 'CZECH REPUBLIC', 'DENMARK', 'ECUADOR', 'EGYPT', 'ESTONIA', 'ETHIOPIA', 'FINLAND', 'FRANCE', 'GEORGIA', 'GERMANY', 'GHANA', 'GREECE', 'GUATEMALA', 'HONDURAS', 'HUNGARY', 'ICELAND', 'INDIA', 'IRAN', 'IRAQ', 'IRELAND', 'ISRAEL', 'ITALY', 'JORDAN', 'KAZAKHSTAN', 'KENYA', 'KUWAIT', 'KYRGYZSTAN', 'LAOS', 'LATVIA', 'LEBANON', 'LIBYA', 'LITHUANIA', 'LUXEMBOURG', 'MACEDONIA', 'MADAGASCAR', 'MALAWI', 'MALTA', 'MAURITIUS', 'MEXICO', 'MOLDOVA', 'MONGOLIA', 'MONTENEGRO', 'MOROCCO', 'MOZAMBIQUE', 'MYANMAR', 'NEPAL', 'NETHERLANDS', 'NEW ZEALAND', 'NICARAGUA', 'NIGERIA', 'NORTH KOREA', 'NORWAY', 'OMAN', 'PAKISTAN', 'PALESTINE', 'PANAMA', 'PARAGUAY', 'PERU', 'PHILIPPINES', 'POLAND', 'PORTUGAL', 'QATAR', 'ROMANIA', 'RUSSIA', 'RWANDA', 'SAUDI ARABIA', 'SERBIA', 'SLOVAKIA', 'SLOVENIA', 'SOUTH AFRICA', 'SPAIN', 'SRI LANKA', 'SUDAN', 'SWEDEN', 'SWITZERLAND', 'SYRIA', 'TAIWAN', 'TANZANIA', 'TUNISIA', 'TURKEY', 'TURKMENISTAN', 'UGANDA', 'UKRAINE', 'UAE', 'UK', 'URUGUAY', 'UZBEKISTAN', 'VENEZUELA', 'YEMEN', 'ZAMBIA', 'ZIMBABWE'];
  window.PG_COUNTRY_OTHER_OPTIONS = (function () {
    var rest = COUNTRY_OTHER_REST.filter(function (c) { return COUNTRY_OTHER_TOP.indexOf(c) === -1; }).sort();
    var html = '<option value="">선택</option>';
    COUNTRY_OTHER_TOP.forEach(function (c) { html += '<option value="' + c + '">' + c + '</option>'; });
    html += '<option value="" disabled>-------------------</option>';
    rest.forEach(function (c) { html += '<option value="' + c + '">' + c + '</option>'; });
    return html;
  })();
  var PHONE_PRIORITY_ORDER = ['JP', 'KR', 'TH', 'US', 'CN', 'SG', 'HK'];
  var PHONE_DIAL_BY_ISO = {
    AF: '+93', AL: '+355', DZ: '+213', AR: '+54', AU: '+61', AT: '+43', BD: '+880', BE: '+32', BR: '+55', BN: '+673', BG: '+359', KH: '+855',
    CA: '+1', CL: '+56', CN: '+86', CO: '+57', HR: '+385', CY: '+357', CZ: '+420', DK: '+45', EG: '+20', FI: '+358', FR: '+33', DE: '+49',
    GR: '+30', HK: '+852', HU: '+36', IN: '+91', ID: '+62', IE: '+353', IL: '+972', IT: '+39', JP: '+81', JO: '+962', KZ: '+7', KE: '+254',
    KR: '+82', KW: '+965', LA: '+856', LU: '+352', MO: '+853', MY: '+60', MX: '+52', MM: '+95', NL: '+31', NZ: '+64', NG: '+234', NO: '+47',
    PK: '+92', PH: '+63', PL: '+48', PT: '+351', QA: '+974', RO: '+40', RU: '+7', SA: '+966', RS: '+381', SG: '+65', SK: '+421', SI: '+386',
    ZA: '+27', ES: '+34', LK: '+94', SE: '+46', CH: '+41', TW: '+886', TH: '+66', TR: '+90', AE: '+971', GB: '+44', US: '+1', VN: '+84'
  };
  /** Priority countries: English labels only (no Korean in dropdown). */
  var PHONE_EN_LABEL_BY_ISO = {
    JP: 'Japan', KR: 'South Korea', TH: 'Thailand', US: 'United States', CN: 'China', SG: 'Singapore', HK: 'Hong Kong'
  };
  var PG_INTL_PHONE_OPTIONS_CACHE_VER = 2;
  function phoneLabelAlphaGroup(label) {
    var c = String(label || '').trim().charAt(0).toUpperCase();
    return /^[A-Z]$/.test(c) ? c : '#';
  }
  function buildIntlPhoneOptionsHtml() {
    if (window.PG_INTL_PHONE_OPTIONS_CACHE_VER === PG_INTL_PHONE_OPTIONS_CACHE_VER && window.PG_INTL_PHONE_OPTIONS) {
      return window.PG_INTL_PHONE_OPTIONS;
    }
    var regionCodes = Object.keys(PHONE_DIAL_BY_ISO);
    if (typeof Intl !== 'undefined' && Intl.DisplayNames && Intl.supportedValuesOf) {
      try {
        var supported = Intl.supportedValuesOf('region') || [];
        regionCodes = supported.filter(function (iso) { return !!PHONE_DIAL_BY_ISO[iso]; });
      } catch (e) {}
    }
    var regionName = null;
    try {
      regionName = new Intl.DisplayNames(['en'], { type: 'region' });
    } catch (e) {}
    var byIso = {};
    regionCodes.forEach(function (iso) {
      byIso[iso] = {
        iso: iso,
        dial: PHONE_DIAL_BY_ISO[iso],
        label: PHONE_EN_LABEL_BY_ISO[iso] || (regionName && regionName.of(iso)) || iso
      };
    });
    var priorityRows = PHONE_PRIORITY_ORDER.map(function (iso) { return byIso[iso]; }).filter(Boolean);
    var prioritySet = {};
    PHONE_PRIORITY_ORDER.forEach(function (iso) { prioritySet[iso] = true; });
    var restRows = regionCodes.filter(function (iso) { return !prioritySet[iso]; }).map(function (iso) { return byIso[iso]; });
    restRows.sort(function (a, b) { return String(a.label).localeCompare(String(b.label), 'en'); });
    var parts = [];
    priorityRows.forEach(function (r) {
      parts.push('<option value="' + r.dial + '">' + r.label + ' (' + r.dial + ')</option>');
    });
    parts.push('<option value="" disabled>---------------</option>');
    var lastGroup = null;
    restRows.forEach(function (r) {
      var g = phoneLabelAlphaGroup(r.label);
      if (g !== lastGroup) {
        lastGroup = g;
        parts.push('<option value="" disabled>---------- ' + g + ' ----------</option>');
      }
      parts.push('<option value="' + r.dial + '">' + r.label + ' (' + r.dial + ')</option>');
    });
    window.PG_INTL_PHONE_OPTIONS_CACHE_VER = PG_INTL_PHONE_OPTIONS_CACHE_VER;
    window.PG_INTL_PHONE_OPTIONS = parts.join('');
    return window.PG_INTL_PHONE_OPTIONS;
  }
  /** 노티 대상 피커: CALLBACK/RESULT 구분·쌍 선택 */
  function pgEscHtml(s) {
    return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
  }
  /** 브랜딩: 저장 URL에서 파일명만 (표시 입력창) */
  function pgBrandingBasenameFromStoredUrl(url) {
    if (url == null) return '';
    var s = String(url).trim();
    if (!s) return '';
    var q = s.indexOf('?');
    if (q >= 0) s = s.slice(0, q);
    var slash = s.lastIndexOf('/');
    return slash >= 0 ? s.slice(slash + 1) : s;
  }
  function pgBrandingDisplayNameAfterUpload(data, fallbackFile) {
    var fromApi = data && data.originalFileName != null ? String(data.originalFileName).trim() : '';
    if (fromApi) return fromApi;
    if (data && data.url) return pgBrandingBasenameFromStoredUrl(data.url);
    if (fallbackFile && fallbackFile.name) return String(fallbackFile.name);
    return '';
  }
  function pgBrandingSetImageDisplayInput(rootEl, imageType, data, fallbackFile) {
    if (!rootEl) return;
    var id = imageType === 'logo'
      ? 'brandingLogoImageUrl'
      : (imageType === 'first'
        ? 'brandingFirstLogoImageUrl'
        : (imageType === 'popcon' ? 'brandingPopconImageUrl' : 'brandingMainImageUrl'));
    var el = rootEl.querySelector('#' + id);
    if (!el) return;
    var label = pgBrandingDisplayNameAfterUpload(data, fallbackFile);
    if (label) el.value = label;
  }
  var PG_ATTACH_ALLOWED_EXT = {
    png: 1, jpg: 1, jpeg: 1, gif: 1, webp: 1, bmp: 1,
    pdf: 1, doc: 1, docx: 1, hwp: 1, hwpx: 1, txt: 1, xls: 1, xlsx: 1, ppt: 1, pptx: 1
  };
  function pgGetFileExt(name) {
    var s = String(name || '').trim();
    var dot = s.lastIndexOf('.');
    if (dot < 0) return '';
    return s.slice(dot + 1).toLowerCase();
  }
  function pgAttachmentRenderTable(section, list) {
    var tbody = section.querySelector('[data-attach-table] tbody');
    if (!tbody) return;
    if (!list || !list.length) {
      tbody.innerHTML = '<tr data-empty-row><td colspan="5" class="text-center text-muted py-2">첨부된 파일이 없습니다.</td></tr>';
      return;
    }
    var rows = '';
    list.forEach(function (it, idx) {
      rows += '<tr data-idx="' + idx + '">' +
        '<td>' + (idx + 1) + '</td>' +
        '<td>' + pgEscHtml(String(it.displayName || '')) + '</td>' +
        '<td>' + pgEscHtml(String(it.fileName || '')) + '</td>' +
        '<td><button type="button" class="btn btn-outline-primary btn-sm" data-attach-edit="' + idx + '">수정</button></td>' +
        '<td><button type="button" class="btn btn-outline-danger btn-sm" data-attach-del="' + idx + '">삭제</button></td>' +
        '</tr>';
    });
    tbody.innerHTML = rows;
  }
  function initAttachmentSection(rootEl) {
    if (!rootEl) return;
    rootEl.querySelectorAll('[data-attach-section="1"]').forEach(function (section) {
      if (section._attachInitDone) return;
      section._attachInitDone = true;
      var jsonEl = section.querySelector('[data-attach-json]');
      var nameEl = section.querySelector('[data-attach-display-name]');
      var fileEl = section.querySelector('[data-attach-file]');
      var fileLabelEl = section.querySelector('[data-attach-file-label]');
      var addBtn = section.querySelector('[data-attach-add]');
      var list = [];
      section._attachEditIndex = null;
      try {
        list = jsonEl && jsonEl.value ? JSON.parse(jsonEl.value) : [];
        if (!Array.isArray(list)) list = [];
      } catch (e) { list = []; }
      pgAttachmentRenderTable(section, list);
      function pgAttachResetFileLabel() {
        if (fileLabelEl) fileLabelEl.textContent = '파일 선택';
      }
      function pgAttachClearEditMode() {
        section._attachEditIndex = null;
        if (addBtn) addBtn.textContent = '추가';
      }
      if (fileEl) {
        fileEl.addEventListener('change', function () {
          if (!fileLabelEl) return;
          var fn = this.files && this.files[0] ? String(this.files[0].name || '') : '';
          if (!fn) {
            fileLabelEl.textContent = '파일 선택';
            return;
          }
          fileLabelEl.textContent = fn.length > 20 ? fn.slice(0, 17) + '…' : fn;
        });
      }
      if (addBtn) {
        addBtn.addEventListener('click', function () {
          var displayName = nameEl && nameEl.value ? String(nameEl.value).trim() : '';
          if (!displayName) { alert('파일이름을 입력하세요.'); return; }
          var f = fileEl && fileEl.files && fileEl.files[0] ? fileEl.files[0] : null;
          var editIdx = section._attachEditIndex;
          if (editIdx != null && isFinite(editIdx) && editIdx >= 0 && editIdx < list.length) {
            if (!f) {
              list[editIdx].displayName = displayName;
            } else {
              var ext = pgGetFileExt(f.name);
              if (!PG_ATTACH_ALLOWED_EXT[ext]) {
                alert('허용되지 않은 파일 형식입니다. 이미지/PDF/문서 파일만 업로드할 수 있습니다.');
                return;
              }
              list[editIdx].displayName = displayName;
              list[editIdx].fileName = String(f.name || '');
              list[editIdx].ext = ext;
            }
            if (jsonEl) jsonEl.value = JSON.stringify(list);
            pgAttachmentRenderTable(section, list);
            if (nameEl) nameEl.value = '';
            if (fileEl) fileEl.value = '';
            pgAttachResetFileLabel();
            pgAttachClearEditMode();
            return;
          }
          if (!f) { alert('첨부할 파일을 선택하세요.'); return; }
          var ext = pgGetFileExt(f.name);
          if (!PG_ATTACH_ALLOWED_EXT[ext]) {
            alert('허용되지 않은 파일 형식입니다. 이미지/PDF/문서 파일만 업로드할 수 있습니다.');
            return;
          }
          list.push({ displayName: displayName, fileName: String(f.name || ''), ext: ext });
          if (jsonEl) jsonEl.value = JSON.stringify(list);
          pgAttachmentRenderTable(section, list);
          if (nameEl) nameEl.value = '';
          if (fileEl) fileEl.value = '';
          pgAttachResetFileLabel();
        });
      }
      section.addEventListener('click', function (ev) {
        var ed = ev.target && ev.target.closest ? ev.target.closest('[data-attach-edit]') : null;
        if (ed && section.contains(ed)) {
          var eidx = parseInt(ed.getAttribute('data-attach-edit') || '-1', 10);
          if (!isFinite(eidx) || eidx < 0 || eidx >= list.length) return;
          section._attachEditIndex = eidx;
          if (nameEl) nameEl.value = String(list[eidx].displayName || '');
          if (fileEl) fileEl.value = '';
          pgAttachResetFileLabel();
          if (addBtn) addBtn.textContent = '적용';
          return;
        }
        var del = ev.target && ev.target.closest ? ev.target.closest('[data-attach-del]') : null;
        if (!del || !section.contains(del)) return;
        var idx = parseInt(del.getAttribute('data-attach-del') || '-1', 10);
        if (!isFinite(idx) || idx < 0 || idx >= list.length) return;
        var eix = section._attachEditIndex;
        if (eix === idx) pgAttachClearEditMode();
        else if (eix != null && eix > idx) section._attachEditIndex = eix - 1;
        list.splice(idx, 1);
        if (jsonEl) jsonEl.value = JSON.stringify(list);
        pgAttachmentRenderTable(section, list);
      });
    });
  }
  var PG_DEFAULT_COPYRIGHT_TEXT = 'Copyright © 2023 ICOPAY Service by Ontheline Co., Ltd.';
  function pgApplyFooterCopyright(rootEl, data) {
    var txt = PG_DEFAULT_COPYRIGHT_TEXT;
    var div = data && data.compDiv ? String(data.compDiv).toUpperCase() : '';
    var c = data && data.copyright != null ? String(data.copyright).trim() : '';
    if ((div === 'HEADQUARTERS' || div === 'REGIONAL' || div === 'MASTER_DIST') && c) {
      txt = c;
    }
    var scope = rootEl || document;
    scope.querySelectorAll('.page-footer').forEach(function (el) { el.textContent = txt; });
    var footerText = document.getElementById('footerText');
    if (footerText) footerText.textContent = txt;
  }
  function pgBrandingFillImageDisplayFromFetch(rootEl, b) {
    if (!rootEl || !b) return;
    var mainEl = rootEl.querySelector('#brandingMainImageUrl');
    var firstEl = rootEl.querySelector('#brandingFirstLogoImageUrl');
    var logoEl = rootEl.querySelector('#brandingLogoImageUrl');
    var popconEl = rootEl.querySelector('#brandingPopconImageUrl');
    if (mainEl) mainEl.value = b.mainImageUrl ? pgBrandingBasenameFromStoredUrl(b.mainImageUrl) : '';
    if (firstEl) firstEl.value = b.firstLogoImageUrl ? pgBrandingBasenameFromStoredUrl(b.firstLogoImageUrl) : '';
    if (logoEl) logoEl.value = b.logoImageUrl ? pgBrandingBasenameFromStoredUrl(b.logoImageUrl) : '';
    if (popconEl) popconEl.value = b.popconImageUrl ? pgBrandingBasenameFromStoredUrl(b.popconImageUrl) : '';
  }
  function pgBindBrandingBrowse(rootEl) {
    if (!rootEl) return;
    var pairs = [
      { imageType: 'main', browse: '#brandingMainImageBrowse', del: '#brandingMainImageDelete', file: '#brandingMainImageFile', text: '#brandingMainImageUrl' },
      { imageType: 'first', browse: '#brandingFirstLogoImageBrowse', del: '#brandingFirstLogoImageDelete', file: '#brandingFirstLogoImageFile', text: '#brandingFirstLogoImageUrl' },
      { imageType: 'logo', browse: '#brandingLogoImageBrowse', del: '#brandingLogoImageDelete', file: '#brandingLogoImageFile', text: '#brandingLogoImageUrl' },
      { imageType: 'popcon', browse: '#brandingPopconImageBrowse', del: '#brandingPopconImageDelete', file: '#brandingPopconImageFile', text: '#brandingPopconImageUrl' }
    ];
    pairs.forEach(function (p) {
      var b = rootEl.querySelector(p.browse);
      var d = rootEl.querySelector(p.del);
      var f = rootEl.querySelector(p.file);
      var t = rootEl.querySelector(p.text);
      if (b && !b._brandingBrowseBound) {
        b._brandingBrowseBound = true;
        b.addEventListener('click', function () { if (f) f.click(); });
      }
      if (f && !f._brandingFileBound) {
        f._brandingFileBound = true;
        f.addEventListener('change', function () {
          if (t && this.files && this.files[0]) t.value = this.files[0].name;
        });
      }
      if (d && !d._brandingDeleteBound) {
        d._brandingDeleteBound = true;
        d.addEventListener('click', function () {
          if (!window.PG_API || !window.PG_API.orgBrandingDeleteImage) return;
          var form = rootEl.querySelector('form') || rootEl.closest('form') || rootEl;
          var compEl = form.querySelector('[name="compId"]');
          var compId = compEl && compEl.value ? String(compEl.value).trim() : '';
          if (!compId) { alert('업체코드가 없어 삭제할 수 없습니다.'); return; }
          if (!confirm('선택한 이미지를 삭제하시겠습니까?')) return;
          window.PG_API.orgBrandingDeleteImage(compId, p.imageType).then(function () {
            if (t) t.value = '';
            if (f) f.value = '';
            alert('삭제되었습니다.');
          }).catch(function (e) {
            alert(e && e.message ? e.message : '삭제 실패');
          });
        });
      }
    });
  }
  function pgShortNotifyChannel(t) {
    var ch = String((t && t.channelType) || '').toUpperCase();
    return ch === 'RESULT' ? 'RESULT' : 'CALLBACK';
  }
  function pgNotifyPairKey(code) {
    var c = String(code || '');
    if (/^cb/i.test(c) || /^rs/i.test(c)) return 'p:' + c.substring(2).toLowerCase();
    return 'legacy:' + c;
  }
  function pgGroupNotifyTargetsByPairKey(list) {
    var byKey = {};
    var keys = [];
    (list || []).forEach(function (t) {
      var k = pgNotifyPairKey(t.targetCode);
      if (!byKey[k]) {
        byKey[k] = [];
        keys.push(k);
      }
      byKey[k].push(t);
    });
    keys.sort(function (ka, kb) {
      var maxA = Math.max.apply(null, byKey[ka].map(function (x) { return Number(x.id) || 0; }));
      var maxB = Math.max.apply(null, byKey[kb].map(function (x) { return Number(x.id) || 0; }));
      return maxB - maxA;
    });
    return keys.map(function (k) {
      var g = byKey[k].slice();
      g.sort(function (a, b) {
        var oa = pgShortNotifyChannel(a) === 'CALLBACK' ? 0 : 1;
        var ob = pgShortNotifyChannel(b) === 'CALLBACK' ? 0 : 1;
        if (oa !== ob) return oa - ob;
        return (Number(a.id) || 0) - (Number(b.id) || 0);
      });
      return g;
    });
  }
  function pgNotifyTargetsFilterKeyword(list, kw) {
    var k = kw ? String(kw).trim().toLowerCase() : '';
    return (list || []).filter(function (t) {
      if (!k) return true;
      return String(t.targetName || '').toLowerCase().indexOf(k) >= 0 || String(t.targetUrl || '').toLowerCase().indexOf(k) >= 0;
    });
  }
  buildIntlPhoneOptionsHtml();

  function parseIntlPhone(rawValue) {
    var raw = (rawValue == null) ? '' : String(rawValue).trim();
    if (!raw) return { code: '+82', number: '' };
    var m = raw.match(/^(\+\d{1,4})[\s-]*(.*)$/);
    if (!m) return { code: '+82', number: raw };
    return { code: m[1], number: (m[2] || '').trim() };
  }

  function syncIntlPhoneHidden(form, fieldName) {
    if (!form || !fieldName) return;
    var hidden = form.querySelector('[name="' + fieldName + '"]');
    var codeSel = form.querySelector('[data-intl-phone-code-for="' + fieldName + '"]');
    var numInp = form.querySelector('[data-intl-phone-number-for="' + fieldName + '"]');
    if (!hidden || !codeSel || !numInp) return;
    var code = String(codeSel.value || '').trim();
    var number = String(numInp.value || '').trim();
    hidden.value = number ? ((code || '+82') + ' ' + number) : '';
  }

  function initIntlPhoneFields(root) {
    if (!root) return;
    root.querySelectorAll('[data-intl-phone-group]').forEach(function (group) {
      if (group._intlPhoneInit) return;
      group._intlPhoneInit = true;
      var fieldName = group.getAttribute('data-intl-phone-group') || '';
      var hidden = group.querySelector('input[type="hidden"][name="' + fieldName + '"]');
      var codeSel = group.querySelector('[data-intl-phone-code-for="' + fieldName + '"]');
      var numInp = group.querySelector('[data-intl-phone-number-for="' + fieldName + '"]');
      if (!hidden || !codeSel || !numInp) return;
      if (!codeSel.querySelector('option')) codeSel.innerHTML = buildIntlPhoneOptionsHtml();
      var parsed = parseIntlPhone(hidden.value);
      if (codeSel.querySelector('option[value="' + parsed.code + '"]')) codeSel.value = parsed.code;
      else codeSel.value = '+82';
      numInp.value = parsed.number;
      codeSel.addEventListener('change', function () { syncIntlPhoneHidden(root, fieldName); });
      numInp.addEventListener('input', function () { syncIntlPhoneHidden(root, fieldName); });
    });
  }

  window.SITE_CONFIG = {
    contentBaseUrl: '',
    contentMode: 'placeholder',  // placeholder: 탭에 HTML 직접 삽입 → index.html 모달(parentCompSearchModal 등) 접근 가능
    paymentBaseUrl: ''  // 간편결제 URL 베이스 (예: https://api.example.com) - 비어있으면 현재 origin 사용
  };

  // 메뉴별 URL → 라벨, parent (브레드크럼/탭 제목용) - FXHJ + 본사설정 + 리스크 통합
  var MENU_INFO = {
    '/hq/pgApiMng': { label: 'API연동설정', parent: '배포설정' },
    '/hq/defaultCommission': { label: '수수료설정', parent: '본사설정' },
    '/hq/chargebackPolicy': { label: '차지백설정', parent: '본사설정' },
    '/hq/businessDaySetting': { label: '영업일설정', parent: '본사설정' },
    '/hq/apiConfig': { label: 'API배포설정', parent: '배포설정' },
    '/hq/urlPayDeploy': { label: 'URL결제설정', parent: '배포설정' },
    '/hq/paymentOrchestration': { label: '결제로직설정', parent: '본사설정' },
    '/hq/domainConfig': { label: '도메인구성설정', parent: '본사설정' },
    '/hq/serverManage': { label: '서버운영관리', parent: '본사설정' },
    '/hq/permissionMng': { label: '본사권한설정', parent: '본사설정' },
    '/hq/userSettings': { label: '사용자설정', parent: '본사설정' },
    '/hq/notifyEnv': { label: '노티구성설정', parent: '본사설정' },
    '/hq/notifyMapping': { label: '노티매핑설정', parent: '본사설정' },
    '/hq/notifyInbound': { label: '노티수령정보', parent: '본사설정' },
    '/hq/ledgerSysSettings': { label: '전산설정관리', parent: '본사설정' },
    '/hq/settlementAdmin': { label: '정산관리설정', parent: '본사설정' },
    '/hq/orgViewColumnAllowance': { label: '조직항목설정', parent: '본사설정' },
    '/hq/accountMng': { label: '업체접근설정', parent: '본사설정' },
    '/system/noticeList': { label: '공지사항', parent: '업체관리' },
    '/comp/myCompMng': { label: '업체정보조회', parent: '업체관리' },
    '/comp/compMngTree': { label: '업체관리', parent: '업체관리' },
    '/comp/compDetail': { label: '업체정보', parent: '업체관리' },
    '/commission/commisionList': { label: '수수료관리', parent: '업체관리' },
    '/comp/compInfoHistList': { label: '업체변경이력', parent: '업체관리' },
    '/comp/compReg': { label: '업체등록', parent: '업체관리' },
    '/calc/payList': { label: '결제내역', parent: '결제관리' },
    '/calc/chillPayTrList': { label: '통합내역', parent: '결제관리' },
    '/calc/chillPaySettlementList': { label: '통합정산', parent: '정산관리' },
    '/calc/payNotiList': { label: '노티내역', parent: '결제관리 > 결제내역' },
    '/calc/paySuccessList': { label: '성공내역', parent: '결제관리 > 결제내역' },
    '/calc/payFailList': { label: '실패내역', parent: '결제관리 > 결제내역' },
    '/calc/payRefundList': { label: '환불내역', parent: '결제관리 > 결제내역' },
    '/calc/payForceRefundList': { label: '강제환불내역', parent: '결제관리 > 결제내역' },
    '/calc/payCancelList': { label: '취소내역', parent: '결제관리 > 결제내역' },
    '/calc/payVoidList': { label: '무효내역', parent: '결제관리 > 결제내역' },
    '/calc/offsetCancList': { label: '상계취소내역', parent: '결제관리 > 결제내역' },
    '/pay/easyPay': { label: 'URL결제내역', parent: '결제관리 > 결제내역' },
    '/pay/chatbotPay': { label: '챗봇결제내역', parent: '결제관리 > 결제내역' },
    '/calc/calcList': { label: '유통망정산내역', parent: '정산관리' },
    '/calc/calcGmList': { label: '가맹점정산내역', parent: '정산관리' },
    '/settlement/franchiseList': { label: '가맹점정산내역', parent: '정산관리' },
    '/calc/paySettlementHoldList': { label: '정산보류내역', parent: '정산관리' },
    '/settlement/paySettlementHoldList': { label: '정산보류내역', parent: '정산관리' },
    '/calc/feeList': { label: '수수료내역', parent: '정산관리' },
    '/settlement/feeList': { label: '수수료내역', parent: '정산관리' },
    '/calc/compPointMngList': { label: '환수금관리', parent: '정산관리' },
    '/calc/balanceList': { label: '잔액내역', parent: '정산관리' },
    '/calc/unpaidMng': { label: '미수금관리', parent: '정산관리' },
    '/calc/exCalcList': { label: '정산실행', parent: '정산관리' },
    '/calc/settlementReport': { label: '정산리포트', parent: '정산관리' },
    '/pay/payHoldList': { label: '보증금내역', parent: '정산관리' },
    '/settlement/holdList': { label: '보증금내역', parent: '정산관리' },
    '/calc/collateralList': { label: '담보금내역', parent: '정산관리' },
    '/settlement/collateralList': { label: '담보금내역', parent: '정산관리' },
    '/noti/notiUrlMng': { label: '결제통보 URL관리', parent: '통보관리' },
    '/noti/notiSendMngList': { label: '결제통보 전송관리', parent: '통보관리' },
    '/noti/notiCashReceiptUrlMng': { label: '현금영수증통보 URL관리', parent: '통보관리' },
    '/noti/notiCashReceiptSendMngList': { label: '현금영수증통보 전송관리', parent: '통보관리' },
    '/user/userMng': { label: '사용자관리', parent: '사용자관리' },
    '/set/gridSetMng': { label: '메뉴별항목순서관리', parent: '사용자관리' },
    '/risk/list': { label: '리스크 현황', parent: '리스크관리' },
    '/deploy/integrationPlan': { label: '연동 진행안', parent: '배포설정' },
    '/deploy/jpayWorkPlan': { label: 'JPAY 단계 계획', parent: '배포설정' },
    '/deploy/merchantApiPolicy': { label: '가맹점 API 배포', parent: '배포설정' },
    '/deploy/launchChecklist': { label: '배포 체크리스트', parent: '배포설정' }
  };

  var config = window.SITE_CONFIG;
  var TAB_UL = 'copyTopTabUl';
  var TAB_MAIN = 'topTapMain';
  var CONTENT_FRAME = 'content-frame';
  var PLACEHOLDER_ID = 'content-placeholder';
  var TABLE_ROW_PADDING_KEY = 'pg_table_row_padding_y';

  function getTableRowPaddingY() {
    var v = parseInt(localStorage.getItem(TABLE_ROW_PADDING_KEY), 10);
    return (isNaN(v) || v < 4 || v > 40) ? 6 : v;
  }
  function setTableRowPaddingY(px) {
    var v = Math.max(4, Math.min(40, px));
    document.documentElement.style.setProperty('--table-row-padding-y', v + 'px');
    localStorage.setItem(TABLE_ROW_PADDING_KEY, String(v));
  }
  /** 예전: 행 사이 점선 리사이즈 줄 삽입. UX상 제거(그리드·목록 테이블 공통). */
  function injectTableRowResizeHandles(/* tbody, colCount */) {
    return;
  }

  function getTabIdFromUrl(url) {
    if (!url || url === '/main') return 'main';
    var path = (url || '').replace(/^\//, '').replace(/\//g, '_');
    return path || 'main';
  }

  function getSessionUser() {
    try { return JSON.parse(sessionStorage.getItem('pg_admin_user') || '{}') || {}; } catch (e) { return {}; }
  }

  // 업체관리는 내 조직 기준 하위만 노출(상위 조직 미노출). 편집 가능 여부는 권한(OBSERVER/MODIFY/DELETE)로 별도 제어.
  // API가 본인 행을 빼고 하위만 줄 때도(영업점 등) orgUnitId로 하위만 남긴다. me 없을 때 전체 목록을 그대로 두지 않는다.
  function applyObserverCompTreeScope(list, url) {
    if (url !== '/comp/compMngTree') return list || [];
    if (!Array.isArray(list) || list.length === 0) return list || [];
    var u = getSessionUser();
    if (u && String(u.role || '').toUpperCase() === 'ADMIN') return list;

    var myCompId = String((u && u.compId) || '').trim();
    var myOuId = u && u.orgUnitId != null && u.orgUnitId !== '' ? String(u.orgUnitId) : '';
    if (!myCompId && !myOuId) return list;

    var byCompId = {};
    list.forEach(function (r) {
      if (!r) return;
      var cid = r.compId != null ? String(r.compId).trim() : '';
      if (cid) byCompId[cid] = r;
    });
    var me = myCompId ? byCompId[myCompId] : null;

    var keepIds = {};
    var seedParents = [];

    if (me && me.id != null) {
      var myId = String(me.id);
      keepIds[myId] = true;
      seedParents.push(myId);
    } else if (myOuId) {
      seedParents.push(myOuId);
    } else {
      return [];
    }

    seedParents.forEach(function (rootParent) {
      var q = [rootParent];
      var qi = 0;
      while (qi < q.length) {
        var parent = q[qi++];
        list.forEach(function (r) {
          var rid = r && r.id != null ? String(r.id) : '';
          var rpid = r && r.parentId != null ? String(r.parentId) : '';
          if (!rid || !rpid) return;
          if (rpid === parent && !keepIds[rid]) {
            keepIds[rid] = true;
            q.push(rid);
          }
        });
      }
    });

    if (Object.keys(keepIds).length === 0) return [];
    return list.filter(function (r) {
      var rid = r && r.id != null ? String(r.id) : '';
      return !!keepIds[rid];
    });
  }

  /** 업체관리 상세: 본인 소속 업체(compId) 레코드는 조회만(저장 버튼 비활성). API도 READ_ONLY_SELF_COMP. */
  window.applyCompDetailReadOnlyIfOwnNonMerchant = function (pane) {
    if (!pane) return;
    pane.classList.remove('pg-comp-detail-self-readonly');
    var form = pane.querySelector('#compDetailForm');
    if (!form) return;
    var u = getSessionUser();
    if (u && String(u.role || '').toUpperCase() === 'ADMIN') return;
    var mine = String((u && u.compId) || '').trim();
    var cidEl = form.querySelector('[name="compId"]');
    var cid = cidEl && cidEl.value ? String(cidEl.value).trim() : '';
    if (!mine || !cid || mine !== cid) return;
    pane.classList.add('pg-comp-detail-self-readonly');
    form.querySelectorAll('input, select, textarea').forEach(function (el) {
      if (el.type === 'hidden') return;
      el.disabled = true;
      if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') el.readOnly = true;
    });
    var saveBtn = pane.querySelector('#compDetailSaveBtn');
    if (saveBtn) {
      saveBtn.disabled = true;
      saveBtn.setAttribute('aria-disabled', 'true');
      saveBtn.classList.remove('btn-primary', 'btn-success', 'btn-danger', 'btn-info', 'btn-warning');
      saveBtn.classList.add('btn-secondary');
      saveBtn.style.pointerEvents = 'none';
    }
  };

  /**
   * 조직별 권한 세팅(pagePermissions)만 사용 — 가맹점 전용 하드코딩 제거.
   * NONE / OBSERVER / MODIFY / DELETE. ADMIN·미연결 → DELETE(무제한).
   */
  function getPagePermissionForUrl(url) {
    if (!url || url === '/main') return 'DELETE';
    var u = getSessionUser();
    if (u && String(u.role || '').toUpperCase() === 'ADMIN') return 'DELETE';
    var pp = u.pagePermissions;
    if (pp && typeof pp === 'object') {
      var aliases = [url];
      // 업체정보조회 권한은 compInfo/myCompMng 중 어느 키로 저장돼도 동일 적용
      if (url === '/comp/myCompMng') aliases.push('/comp/compInfo');
      else if (url === '/comp/compInfo') aliases.push('/comp/myCompMng');
      // 정산: /settlement/unpaidMng 는 메뉴·권한 카탈로그의 /calc/unpaidMng 와 동일
      if (url === '/settlement/unpaidMng') aliases.push('/calc/unpaidMng');
      else if (url === '/calc/unpaidMng') aliases.push('/settlement/unpaidMng');
      // 업체상세(compDetail)는 업체관리(compMngTree) 권한을 따라간다.
      if (url === '/comp/compDetail') aliases.push('/comp/compMngTree');
      else if (url === '/comp/compMngTree') aliases.push('/comp/compDetail');
      for (var i = 0; i < aliases.length; i++) {
        var key = aliases[i];
        if (Object.prototype.hasOwnProperty.call(pp, key)) {
          var pv = pp[key];
          return pv != null && String(pv).trim() !== '' ? String(pv).trim().toUpperCase() : 'DELETE';
        }
      }
    }
    return 'DELETE';
  }

  function isMenuAllowedForCurrentUser(url) {
    return getPagePermissionForUrl(url) !== 'NONE';
  }

  function applyAdminOnlyMenuItems() {
    var u = getSessionUser();
    if (u && String(u.role || '').toUpperCase() === 'ADMIN') return;
    var ol = String(u && u.orgLevel != null ? u.orgLevel : '').toUpperCase();
    var showPerm = ol === 'HEADQUARTERS' || ol === 'REGIONAL' || ol === 'MASTER_DIST';
    document.querySelectorAll('.child-li[data-url="/hq/permissionMng"]').forEach(function (li) {
      if (showPerm) {
        li.style.display = '';
      } else {
        li.style.display = 'none';
        li.classList.remove('mm-active');
      }
    });
  }

  /** 조직별 권한 세팅(pagePermissions): NONE 이면 메뉴 숨김 */
  function applyMenuVisibilityByPagePermissions() {
    document.querySelectorAll('.side-nav .child-li[data-url]').forEach(function (li) {
      var u = li.getAttribute('data-url') || '';
      if (!u) return;
      if (getPagePermissionForUrl(u) === 'NONE') {
        li.style.display = 'none';
        li.classList.remove('mm-active');
      } else {
        li.style.display = '';
      }
    });
    // 하위 메뉴가 모두 숨김이면 상위 큰메뉴도 숨김
    document.querySelectorAll('.side-nav .side-nav-item').forEach(function (item) {
      var children = item.querySelectorAll('.side-nav-second-level .child-li[data-url]');
      if (!children || children.length === 0) return;
      var hasVisibleChild = false;
      children.forEach(function (li) {
        if (li.style.display !== 'none') hasVisibleChild = true;
      });
      if (!hasVisibleChild) {
        item.style.display = 'none';
        item.classList.remove('mm-active');
        var sub = item.querySelector('.side-nav-second-level');
        if (sub) sub.classList.remove('mm-show');
        var link = item.querySelector('.side-nav-link');
        if (link) link.setAttribute('aria-expanded', 'false');
      } else {
        item.style.display = '';
      }
    });
  }

  /** 현재 강조 메뉴가 접근불가면 사이드바만 허용된 첫 메뉴로 맞춤(탭 자동 오픈은 하지 않음) */
  function redirectIfActiveMenuForbidden() {
    var cur = document.querySelector('.side-nav .child-li.mm-active');
    var cu = cur ? (cur.getAttribute('data-url') || '') : '';
    if (!cu || isMenuAllowedForCurrentUser(cu)) return;
    var items = document.querySelectorAll('.side-nav .child-li[data-url]');
    for (var i = 0; i < items.length; i++) {
      var u = items[i].getAttribute('data-url') || '';
      if (!u) continue;
      if (isMenuAllowedForCurrentUser(u)) {
        setActiveMenuByUrl(u);
        return;
      }
    }
    setActiveMenuByUrl('/main');
  }

  function getFullUrl(path) {
    path = (path || '').replace(/^\//, '');
    if (config.contentBaseUrl) {
      return config.contentBaseUrl.replace(/\/$/, '') + '/' + path;
    }
    return '/' + path;
  }

  function ensureContentArea() {
    var frame = document.getElementById(CONTENT_FRAME);
    if (frame) return frame;
    var container = document.getElementById('contentsMain');
    if (!container) return null;
    frame = document.createElement('iframe');
    frame.id = CONTENT_FRAME;
    frame.name = 'content-frame';
    frame.setAttribute('style', 'width:100%;height:calc(100vh - 140px);min-height:400px;border:0;display:none');
    container.appendChild(frame);
    return frame;
  }

  var MAX_TOP_TABS = 12;

  function initBankByCountry(pane) {
    if (!pane) return;
    var countrySel = pane.querySelector('select[data-load-countries="true"]');
    var bankSel = pane.querySelector('select[data-bank-by-country="true"]');
    if (!countrySel || !bankSel || bankSel._bankByCountryInit) return;
    bankSel._bankByCountryInit = true;
    function loadCountries() {
      window.PG_API.bankCountries().then(function (list) {
        var cur = countrySel.value;
        countrySel.innerHTML = '<option value="">선택</option>' + (list || []).map(function (c) {
          return '<option value="' + (c.code || '') + '">' + (c.name || c.code) + '</option>';
        }).join('');
        if (cur) countrySel.value = cur;
      }).catch(function () {});
    }
    function loadBanks(countryCd, preserveBank) {
      bankSel.innerHTML = '<option value="">국가 선택 후</option>';
      if (!countryCd || !countryCd.trim()) return;
      window.PG_API.bankListByCountry(countryCd).then(function (list) {
        bankSel.innerHTML = '<option value="">선택</option>' + (list || []).map(function (b) {
          return '<option value="' + (b.code || '') + '">' + (b.name || b.code) + '</option>';
        }).join('');
        if (preserveBank) bankSel.value = preserveBank;
      }).catch(function () {});
    }
    loadCountries();
    countrySel.addEventListener('change', function () {
      loadBanks(this.value);
    });
    if (countrySel.value) loadBanks(countrySel.value, bankSel.value);
  }

  var BANK_FALLBACK = {
    KR: [{ code: '02', name: '산업은행' }, { code: '03', name: '기업은행' }, { code: '04', name: '국민' }, { code: '07', name: '수협' }, { code: '11', name: 'NH농협' }, { code: '12', name: '지역농·축협' }, { code: '20', name: '우리' }, { code: '23', name: 'SC제일은행' }, { code: '27', name: '한국씨티' }, { code: '31', name: '대구은행' }, { code: '32', name: '부산은행' }, { code: '34', name: '광주은행' }, { code: '35', name: '제주은행' }, { code: '37', name: '전북은행' }, { code: '39', name: '경남은행' }, { code: '45', name: '새마을금고' }, { code: '48', name: '신협' }, { code: '50', name: '상호저축은행' }, { code: '64', name: '산림조합' }, { code: '71', name: '우체국' }, { code: '81', name: 'KEB하나' }, { code: '88', name: '신한' }, { code: '89', name: '케이뱅크' }, { code: '90', name: '카카오뱅크' }, { code: '92', name: '토스뱅크' }],
    JP: [{ code: '0001', name: 'みずほ銀行' }, { code: '0005', name: '三菱UFJ銀行' }, { code: '0009', name: '三井住友銀行' }, { code: '0010', name: 'りそな銀行' }, { code: '0017', name: '埼玉りそな銀行' }, { code: '0033', name: 'ジャパンネット銀行' }, { code: '0034', name: 'セブン銀行' }, { code: '0036', name: '楽天銀行' }, { code: '0038', name: 'ソニー銀行' }, { code: '0039', name: 'auじぶん銀行' }, { code: '0040', name: 'イオン銀行' }, { code: '9900', name: 'ゆうちょ銀行' }, { code: '0116', name: '横浜銀行' }, { code: '0117', name: '静岡銀行' }, { code: '0118', name: '北陸銀行' }],
    TH: [{ code: '002', name: 'Bangkok Bank' }, { code: '004', name: 'Kasikorn Bank' }, { code: '006', name: 'Krung Thai Bank' }, { code: '009', name: 'HSBC Thailand' }, { code: '011', name: 'TMBThanachart Bank' }, { code: '014', name: 'Siam Commercial Bank' }, { code: '022', name: 'Standard Chartered' }, { code: '024', name: 'UOB Thailand' }, { code: '025', name: 'Bank of Ayudhya (Krungsri)' }, { code: '030', name: 'Government Savings Bank' }, { code: '034', name: 'Government Housing Bank' }, { code: '067', name: 'ICBC Thai' }, { code: '069', name: 'Kiatnakin Phatra Bank' }, { code: '073', name: 'Land and Houses Bank' }, { code: '076', name: 'Thanachart Bank' }]
  };
  function applyBankOptions(bankSel, list, preserveBank) {
    bankSel.innerHTML = '<option value="">선택</option>' + (list || []).map(function (b) {
      return '<option value="' + (b.code || '') + '">' + (b.name || b.code) + '</option>';
    }).join('');
    if (preserveBank) bankSel.value = preserveBank;
  }
  function loadBanksForCountry(countryCd, bankSel, preserveBank) {
    if (!bankSel || !countryCd || !countryCd.trim()) return;
    bankSel.innerHTML = '<option value="">국가 선택 후</option>';
    var fallback = BANK_FALLBACK[countryCd.toUpperCase()];
    window.PG_API.bankListByCountry(countryCd).then(function (list) {
      applyBankOptions(bankSel, list && list.length ? list : fallback, preserveBank);
    }).catch(function (err) {
      if (fallback) applyBankOptions(bankSel, fallback, preserveBank);
      else if (typeof console !== 'undefined' && console.error) console.error('은행 목록 조회 실패:', err);
    });
  }
  function refreshCountryBankAfterFill(pane, bankCdVal) {
    if (!pane) return;
    pane.querySelectorAll('.country-bank-row').forEach(function (row) {
      var cs = row.querySelector('select[data-country-select]');
      var bs = row.querySelector('select[data-bank-select]');
      if (!cs || !bs || (cs.value !== 'JP' && cs.value !== 'KR' && cs.value !== 'TH')) return;
      loadBanksForCountry(cs.value, bs, bankCdVal);
    });
  }

  function initCountryAddressGroup(pane) {
    if (!pane) return;
    pane.querySelectorAll('.country-address-row').forEach(function (row) {
      if (row._countryAddressInit) return;
      row._countryAddressInit = true;
      var countrySel = row.querySelector('select[data-addr-country-select]');
      var countryOtherWrap = row.querySelector('.addr-country-other-wrap');
      var zipSearchWrap = row.querySelector('[data-zip-search-wrap]');
      var zipInput = row.querySelector('input[name="zipCode"]');
      var addrInput = row.querySelector('input[name="addr"]');
      var searchBtn = row.querySelector('[data-addr-zip-search]');
      if (!countrySel || !zipInput) return;
      function toggleByCountry() {
        var v = countrySel.value;
        if (v === 'OTHER') {
          if (countryOtherWrap) countryOtherWrap.classList.remove('d-none');
          if (searchBtn) searchBtn.style.display = 'none';
          if (zipInput) zipInput.placeholder = '직접입력';
        } else {
          if (countryOtherWrap) countryOtherWrap.classList.add('d-none');
          if (v === 'KR') {
            if (searchBtn) searchBtn.style.display = '';
            if (zipInput) zipInput.placeholder = '검색';
          } else {
            if (searchBtn) searchBtn.style.display = 'none';
            if (zipInput) zipInput.placeholder = '직접입력';
          }
        }
      }
      countrySel.addEventListener('change', toggleByCountry);
      toggleByCountry();
      if (searchBtn) {
        searchBtn.addEventListener('click', function () {
          if (countrySel.value !== 'KR') return;
          if (typeof daum === 'undefined') { alert('우편번호 서비스를 불러올 수 없습니다.'); return; }
          new daum.Postcode({
            oncomplete: function (data) {
              zipInput.value = data.zonecode || '';
              addrInput.value = data.roadAddress || data.jibunAddress || '';
            }
          }).open();
        });
      }
    });
  }

  function initCountryBankGroup(pane) {
    if (!pane) return;
    pane.querySelectorAll('.country-bank-row').forEach(function (row) {
      if (row._countryBankInit) return;
      row._countryBankInit = true;
      var countrySel = row.querySelector('select[data-country-select]');
      var countryOtherWrap = row.querySelector('.country-other-wrap');
      var bankSelectWrap = row.querySelector('.bank-select-wrap');
      var bankTextWrap = row.querySelector('.bank-text-wrap');
      var bankSel = row.querySelector('select[data-bank-select]');
      var bankCdText = row.querySelector('input[name="bankCdText"]');
      if (!countrySel || !bankSel) return;
      function loadBanks(countryCd, preserveBank) {
        loadBanksForCountry(countryCd, bankSel, preserveBank);
      }
      function toggleByCountry() {
        var v = countrySel.value;
        if (v === 'OTHER') {
          if (countryOtherWrap) countryOtherWrap.classList.remove('d-none');
          if (bankSelectWrap) bankSelectWrap.classList.add('d-none');
          if (bankTextWrap) bankTextWrap.classList.remove('d-none');
          bankSel.value = '';
        } else {
          if (countryOtherWrap) countryOtherWrap.classList.add('d-none');
          if (bankSelectWrap) bankSelectWrap.classList.remove('d-none');
          if (bankTextWrap) bankTextWrap.classList.add('d-none');
          if (bankCdText) bankCdText.value = '';
          if (v === 'JP' || v === 'KR' || v === 'TH') {
            loadBanks(v);
          } else {
            bankSel.innerHTML = '<option value="">국가 선택 후</option>';
          }
        }
      }
      countrySel.addEventListener('change', toggleByCountry);
      toggleByCountry();
      if (countrySel.value && countrySel.value !== 'OTHER') loadBanks(countrySel.value, bankSel.value);
    });
  }

  function pgDoubleConfirm(msg1, msg2) {
    return window.confirm(msg1) && window.confirm(msg2);
  }
  window.pgDoubleConfirm = pgDoubleConfirm;

  /**
   * 더블확인: 저장(또는 서버 반영) 직전 연속 확인 두 번. 첫·둘 중 취소 시 false.
   * @param {string} [message] 첫 번째 안내
   * @param {string} [secondMessage] 두 번째 안내 (생략 시 저장 반영용 기본 문구)
   */
  function pgConfirmBeforeSave(message, secondMessage) {
    var m1 = message || '저장하시겠습니까?\n취소하면 저장되지 않습니다.';
    var m2 = (secondMessage !== undefined && secondMessage !== null && String(secondMessage) !== '')
      ? String(secondMessage)
      : '서버에 반영합니다. 정말 진행할까요?';
    return window.confirm(m1) && window.confirm(m2);
  }
  window.pgConfirmBeforeSave = pgConfirmBeforeSave;

  /** 가맹점 결제대행사 테이블. opts.rowActionMode=true 이면 업체정보(상세)에서 행별 저장/삭제·2중 확인 */
  function initPgBindingList(pane, initialBindings, opts) {
    opts = opts || {};
    var rowActionMode = !!opts.rowActionMode;
    var getCompId = opts.getCompId || function () { return ''; };

    var tbody = pane.querySelector('#pgBindingTbody');
    var addBtn = pane.querySelector('#pgBindingAddBtn');
    if (!tbody || !addBtn || addBtn._pgBindingInit) return;
    addBtn._pgBindingInit = true;

    function syncPgBindingRowStyles() {
      tbody.querySelectorAll('tr').forEach(function (t) {
        var r = t.querySelector('input[name="pgOperational"]');
        var on = r && r.checked;
        t.classList.toggle('pg-binding-row--operational', !!on);
        t.classList.toggle('pg-binding-row--inactive', !on);
      });
    }
    /** 결제대행사가 한 줄뿐이면 운영 라디오가 비어 있을 수 없음 — 미선택 시 자동 선택(빨강 표시·저장 시 Y 반영). */
    function ensureSingleRowOperationalRadio() {
      var rows = tbody.querySelectorAll('tr');
      if (rows.length !== 1) return;
      if (tbody.querySelector('input[name="pgOperational"]:checked')) return;
      var inp = rows[0].querySelector('input[name="pgOperational"]');
      if (inp && !inp.disabled) inp.checked = true;
      syncPgBindingRowStyles();
    }
    if (!tbody._pgOperationalStyleBound) {
      tbody._pgOperationalStyleBound = true;
      tbody.addEventListener('change', function (ev) {
        if (ev.target && ev.target.getAttribute('name') === 'pgOperational') {
          syncPgBindingRowStyles();
        }
      });
    }

    var payMethodOpts = '<option value="">선택</option><option value="WEB">WEB</option><option value="OFFLINE">오프라인</option><option value="APM">APM</option>';
    var activationOpts = '<option value="Y">사용</option><option value="N">미사용</option>';
    var installmentOpts = '<option value="N">미사용</option><option value="Y">사용</option>';

    function rowSnapshot(tr) {
      var sel = function (f) {
        var e = tr.querySelector('[data-field="' + f + '"]');
        return e ? e.value : '';
      };
      var opInp = tr.querySelector('input[name="pgOperational"]');
      return {
        pgCd: sel('pgCd'),
        activationYn: sel('activationYn'),
        payMethod: sel('payMethod'),
        mid: sel('mid'),
        rootNo: sel('rootNo'),
        apiKey: sel('apiKey'),
        ivKey: sel('ivKey'),
        installmentYn: sel('installmentYn'),
        maxInstallmentMonths: sel('maxInstallmentMonths'),
        operationalChecked: opInp ? opInp.checked : false
      };
    }

    function applySnapshot(tr, snap) {
      if (!snap) return;
      var setSel = function (f, v) {
        var e = tr.querySelector('[data-field="' + f + '"]');
        if (e) e.value = v != null && v !== undefined ? String(v) : '';
      };
      setSel('pgCd', snap.pgCd);
      setSel('activationYn', snap.activationYn);
      setSel('payMethod', snap.payMethod);
      setSel('mid', snap.mid);
      setSel('rootNo', snap.rootNo);
      setSel('apiKey', snap.apiKey);
      setSel('ivKey', snap.ivKey);
      setSel('installmentYn', snap.installmentYn);
      setSel('maxInstallmentMonths', snap.maxInstallmentMonths);
      var opInp = tr.querySelector('input[name="pgOperational"]');
      if (opInp) opInp.checked = !!snap.operationalChecked;
    }

    function setRowReadonly(tr, ro) {
      tr.querySelectorAll('[data-field]').forEach(function (el) { el.disabled = ro; });
      tr.querySelectorAll('input[name="pgOperational"]').forEach(function (el) { el.disabled = ro; });
    }

    function toggleRowEditUi(tr, editing) {
      var editBtn = tr.querySelector('.pg-binding-edit');
      var delBtn = tr.querySelector('.pg-binding-del');
      var saveBtn = tr.querySelector('.pg-binding-save');
      var cancelBtn = tr.querySelector('.pg-binding-cancel');
      if (editBtn) editBtn.classList.toggle('d-none', editing);
      if (delBtn) delBtn.classList.toggle('d-none', editing);
      if (saveBtn) saveBtn.classList.toggle('d-none', !editing);
      if (cancelBtn) cancelBtn.classList.toggle('d-none', !editing);
    }

    function reindexRows() {
      tbody.querySelectorAll('tr').forEach(function (t, i) {
        t.dataset.idx = i;
        var r = t.querySelector('input[name="pgOperational"]');
        if (r) r.value = i;
      });
      syncPgBindingRowStyles();
      ensureSingleRowOperationalRadio();
    }

    function escPgOpt(s) {
      return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
    }

    function wireRow(tr) {
      var delBtn = tr.querySelector('.pg-binding-del');
      var editBtn = tr.querySelector('.pg-binding-edit');
      var saveBtn = tr.querySelector('.pg-binding-save');
      var cancelBtn = tr.querySelector('.pg-binding-cancel');

      var pgSel = tr.querySelector('[data-field="pgCd"]');
      if (pgSel && !pgSel._pgAgencyTemplateBound) {
        pgSel._pgAgencyTemplateBound = true;
        pgSel.addEventListener('change', function () {
          applyPgAgencyTemplateDefaults(tr);
        });
      }

      if (delBtn) {
        delBtn.addEventListener('click', function () {
          var bid = tr.dataset.bindingId || '';
          if (rowActionMode && bid) {
            if (!pgDoubleConfirm('이 결제대행사 연동을 삭제하시겠습니까?', '삭제하면 복구할 수 없습니다. 정말 삭제하시겠습니까?')) return;
            var compId = getCompId();
            if (!compId) { alert('업체코드가 없습니다.'); return; }
            var dimm = document.getElementById('dimm');
            if (dimm) dimm.style.display = 'flex';
            window.PG_API.compPgBindingDelete(compId, bid).then(function () {
              alert('삭제되었습니다.');
              tr.remove();
              reindexRows();
            }).catch(function (e) { alert(e && e.message ? e.message : '삭제 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
          } else {
            if (rowActionMode && !bid) {
              if (!pgDoubleConfirm('추가 중인 행을 취소하시겠습니까?', '입력 내용이 사라집니다. 계속하시겠습니까?')) return;
            }
            tr.remove();
            reindexRows();
          }
        });
      }

      if (editBtn) {
        editBtn.addEventListener('click', function () {
          if (!pgDoubleConfirm('이 연동 정보를 수정하시겠습니까?', '입력란이 활성화됩니다. 계속하시겠습니까?')) return;
          tr.dataset.editBackup = tr.dataset.snapshot;
          setRowReadonly(tr, false);
          toggleRowEditUi(tr, true);
        });
      }

      if (cancelBtn) {
        cancelBtn.addEventListener('click', function () {
          var bid = tr.dataset.bindingId || '';
          if (rowActionMode && !bid) {
            if (!pgDoubleConfirm('추가를 취소하시겠습니까?', '입력 내용이 버려집니다. 계속하시겠습니까?')) return;
            tr.remove();
            reindexRows();
            return;
          }
          if (!pgDoubleConfirm('수정을 취소하시겠습니까?', '저장되지 않은 변경이 사라집니다. 계속하시겠습니까?')) return;
          try { applySnapshot(tr, JSON.parse(tr.dataset.editBackup || tr.dataset.snapshot || '{}')); } catch (e) {}
          setRowReadonly(tr, true);
          toggleRowEditUi(tr, false);
        });
      }

      if (saveBtn && rowActionMode) {
        saveBtn.addEventListener('click', function () {
          if (!pgDoubleConfirm('이 결제대행사 연동을 저장하시겠습니까?', '저장을 진행합니다. 계속하시겠습니까?')) return;
          var compId = getCompId();
          if (!compId) { alert('업체코드가 없습니다.'); return; }
          var sel = function (f) {
            var e = tr.querySelector('[data-field="' + f + '"]');
            return e ? e.value : '';
          };
          var pgCd = sel('pgCd');
          if (!pgCd) { alert('결제대행사(PG)를 선택하세요. 배포설정 > API연동설정에 먼저 등록해야 목록에 나타납니다.'); return; }
          var trs = Array.prototype.slice.call(tbody.querySelectorAll('tr'));
          var myIdx = trs.indexOf(tr);
          var formRoot = pane.querySelector('#compDetailForm') || pane.querySelector('#compRegForm');
          var opRadio = formRoot ? formRoot.querySelector('input[name="pgOperational"]:checked') : null;
          var opIdx = opRadio ? parseInt(opRadio.value, 10) : -1;
          var operationalYn = (myIdx === opIdx) ? 'Y' : 'N';
          var body = {
            compId: compId,
            pgCd: pgCd,
            payMethod: sel('payMethod') || 'WEB',
            mid: sel('mid'),
            rootNo: sel('rootNo'),
            apiKey: sel('apiKey'),
            ivKey: sel('ivKey'),
            activationYn: sel('activationYn') || 'Y',
            operationalYn: operationalYn,
            installmentYn: sel('installmentYn') || 'N',
            maxInstallmentMonths: sel('maxInstallmentMonths')
          };
          if (tr.dataset.bindingId) body.id = tr.dataset.bindingId;
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.compPgBindingSave(body).then(function (saved) {
            alert('저장되었습니다.');
            if (saved && saved.id != null) tr.dataset.bindingId = String(saved.id);
            tr.dataset.snapshot = JSON.stringify(rowSnapshot(tr));
            setRowReadonly(tr, true);
            toggleRowEditUi(tr, false);
            var actionsTd = tr.querySelector('.pg-binding-actions');
            if (actionsTd && tr.dataset.bindingId) {
              actionsTd.innerHTML = '<button type="button" class="btn btn-outline-primary btn-sm pg-binding-edit">수정</button> ' +
                '<button type="button" class="btn btn-outline-danger btn-sm pg-binding-del">삭제</button> ' +
                '<button type="button" class="btn btn-success btn-sm pg-binding-save d-none">저장</button> ' +
                '<button type="button" class="btn btn-secondary btn-sm pg-binding-cancel d-none">취소</button>';
              wireRow(tr);
            }
          }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
      }
    }

    function addRow(idx, data, pgAgencyOptsHtml) {
      data = data || {};
      var bid = data.id != null && data.id !== '' ? String(data.id) : '';
      var hasId = !!bid;
      var actionsCell;
      if (!rowActionMode) {
        actionsCell = '<button type="button" class="btn btn-outline-danger btn-sm pg-binding-del">삭제</button>';
      } else if (hasId) {
        actionsCell = '<button type="button" class="btn btn-outline-primary btn-sm pg-binding-edit">수정</button> ' +
          '<button type="button" class="btn btn-outline-danger btn-sm pg-binding-del">삭제</button> ' +
          '<button type="button" class="btn btn-success btn-sm pg-binding-save d-none">저장</button> ' +
          '<button type="button" class="btn btn-secondary btn-sm pg-binding-cancel d-none">취소</button>';
      } else {
        actionsCell = '<button type="button" class="btn btn-success btn-sm pg-binding-save">저장</button> ' +
          '<button type="button" class="btn btn-secondary btn-sm pg-binding-cancel">취소</button>';
      }

      var tr = document.createElement('tr');
      tr.dataset.bindingId = bid;
      tr.dataset.idx = idx;
      tr.innerHTML = '<td><input type="radio" name="pgOperational" value="' + idx + '"' + (data.operationalYn === 'Y' ? ' checked' : '') + ' title="운영대상"></td>' +
        '<td><select class="form-control form-control-sm" data-field="activationYn">' + activationOpts + '</select></td>' +
        '<td><select class="form-control form-control-sm" data-field="pgCd">' + pgAgencyOptsHtml + '</select></td>' +
        '<td><select class="form-control form-control-sm" data-field="payMethod">' + payMethodOpts + '</select></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="mid" placeholder="MID" autocomplete="off"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="rootNo" placeholder="루트(노티구분)" autocomplete="off"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="apiKey" placeholder="API KEY" autocomplete="off"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="ivKey" placeholder="IV KEY" autocomplete="off"></td>' +
        '<td><select class="form-control form-control-sm" data-field="installmentYn">' + installmentOpts + '</select></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="maxInstallmentMonths" placeholder="12" autocomplete="off"></td>' +
        '<td class="pg-binding-actions">' + actionsCell + '</td>';

      tr.querySelector('[data-field="activationYn"]').value = data.activationYn || 'Y';
      tr.querySelector('[data-field="pgCd"]').value = data.pgCd || '';
      tr.querySelector('[data-field="payMethod"]').value = data.payMethod || 'WEB';
      tr.querySelector('[data-field="mid"]').value = data.mid || '';
      tr.querySelector('[data-field="rootNo"]').value = data.rootNo || '';
      tr.querySelector('[data-field="apiKey"]').value = data.apiKey || '';
      tr.querySelector('[data-field="ivKey"]').value = data.ivKey || '';
      tr.querySelector('[data-field="installmentYn"]').value = data.installmentYn || 'N';
      tr.querySelector('[data-field="maxInstallmentMonths"]').value = data.maxInstallmentMonths != null ? String(data.maxInstallmentMonths) : '';

      tr.dataset.snapshot = JSON.stringify(rowSnapshot(tr));
      if (rowActionMode && hasId) setRowReadonly(tr, true);

      tbody.appendChild(tr);
      applyPgAgencyTemplateDefaults(tr);
      wireRow(tr);
      syncPgBindingRowStyles();
    }

    function applyPgAgencyTemplateDefaults(tr) {
      if (!tr || !pgAgencyCatalog) return;
      var selEl = tr.querySelector('[data-field="pgCd"]');
      if (!selEl || !selEl.value) return;
      var p = pgAgencyCatalog[String(selEl.value).toUpperCase()];
      if (!p) return;
      var midEl = tr.querySelector('[data-field="mid"]');
      var rootEl = tr.querySelector('[data-field="rootNo"]');
      if (midEl && !String(midEl.value || '').trim() && p.defaultMid) midEl.value = String(p.defaultMid);
      if (rootEl && !String(rootEl.value || '').trim() && p.routeNo != null && String(p.routeNo) !== '') rootEl.value = String(p.routeNo);
    }

    var pgAgencyCatalog = null;

    window.PG_API.pgAgencyList().then(function (list) {
      pgAgencyCatalog = {};
      (list || []).forEach(function (p) {
        var cd = (p.pgCd || '').trim();
        if (cd) pgAgencyCatalog[cd.toUpperCase()] = p;
      });
      var pgAgencyOptsHtml = '<option value="">선택</option>';
      (list || []).forEach(function (p) {
        var cd = (p.pgCd || '').trim();
        var lab = (p.pgNm || cd) + ' (' + cd + ')';
        var kLab = (p.integKindLabel || '').trim();
        if (kLab) lab += ' · ' + kLab;
        if (p.hqOperationalYn === 'N') lab += ' · 본사미운영';
        pgAgencyOptsHtml += '<option value="' + escPgOpt(cd) + '">' + escPgOpt(lab) + '</option>';
      });
      var bindings = initialBindings || [];
      if (bindings.length > 0) {
        bindings.forEach(function (b, i) {
          var opRaw = b.operationalYn;
          var opYn;
          if (opRaw == null || String(opRaw).trim() === '') {
            opYn = i === 0 ? 'Y' : 'N';
          } else {
            opYn = String(opRaw).toUpperCase() === 'Y' ? 'Y' : 'N';
          }
          addRow(i, {
            id: b.id,
            pgCd: b.pgCd,
            activationYn: b.activationYn || 'Y',
            operationalYn: opYn,
            payMethod: b.payMethod || 'WEB',
            mid: b.mid,
            rootNo: b.rootNo,
            apiKey: b.apiKey,
            ivKey: b.ivKey,
            installmentYn: b.installmentYn || 'N',
            maxInstallmentMonths: b.maxInstallmentMonths != null ? String(b.maxInstallmentMonths) : ''
          }, pgAgencyOptsHtml);
        });
        ensureSingleRowOperationalRadio();
      }
      addBtn.addEventListener('click', function () {
        var idx = tbody.querySelectorAll('tr').length;
        if (rowActionMode) {
          addRow(idx, { operationalYn: idx === 0 ? 'Y' : 'N' }, pgAgencyOptsHtml);
        } else {
          addRow(idx, idx === 0 ? { operationalYn: 'Y' } : {}, pgAgencyOptsHtml);
        }
        reindexRows();
      });
    }).catch(function () {
      pgAgencyCatalog = {};
      var pgAgencyOptsHtml = '<option value="">선택</option>';
      addBtn.addEventListener('click', function () {
        var idx = tbody.querySelectorAll('tr').length;
        addRow(idx, idx === 0 ? { operationalYn: 'Y' } : {}, pgAgencyOptsHtml);
        reindexRows();
      });
    });
  }

  window.openPgAgencyModal = function (preset) {
    preset = preset || {};
    function resolvePresetIntegKind(p) {
      if (p.integKind && String(p.integKind).toUpperCase() !== 'MULTI') return String(p.integKind).toUpperCase();
      if (String(p.integNotiYn || '').toUpperCase() === 'Y') return 'NOTI';
      if (String(p.integUrlPayYn || '').toUpperCase() === 'Y') return 'URL_PAY';
      if (String(p.integWebChatbotYn || '').toUpperCase() === 'Y') return 'WEB_CHATBOT';
      if (String(p.integApiYn || '').toUpperCase() === 'Y') return 'API';
      return '';
    }
    function presetIntegrationEndpoint(p, kind) {
      if (p.primaryEndpoint != null && String(p.primaryEndpoint).trim()) return String(p.primaryEndpoint).trim();
      if (kind === 'NOTI') return p.endpointNoti || '';
      if (kind === 'URL_PAY') return p.endpointUrlPay || '';
      if (kind === 'WEB_CHATBOT' || kind === 'API') return p.endpointApi || p.apiEndpoint || '';
      return '';
    }
    var idEl = document.getElementById('pgAgencyEditId');
    var cdEl = document.getElementById('pgAgencyEditPgCd');
    var nmEl = document.getElementById('pgAgencyEditPgNm');
    var epEl = document.getElementById('pgAgencyEditEndpoint');
    var uyEl = document.getElementById('pgAgencyEditUseYn');
    var midEl = document.getElementById('pgAgencyEditMid');
    var rnEl = document.getElementById('pgAgencyEditRouteNo');
    var sbEl = document.getElementById('pgAgencyEditSandboxYn');
    var akEl = document.getElementById('pgAgencyEditApiKey');
    var mkEl = document.getElementById('pgAgencyEditMd5Key');
    var exEl = document.getElementById('pgAgencyEditCredentialsExtra');
    var kindEl = document.getElementById('pgAgencyEditIntegKind');
    var intEpEl = document.getElementById('pgAgencyEditIntegrationEndpoint');
    var modeWrap = document.getElementById('pgAgencyEditUrlPayAmountModeWrap');
    var modeSel = document.getElementById('pgAgencyEditUrlPayAmountMode');
    if (kindEl && !kindEl._pgUrlPayAmBound) {
      kindEl._pgUrlPayAmBound = true;
      kindEl.addEventListener('change', function () {
        var w = document.getElementById('pgAgencyEditUrlPayAmountModeWrap');
        if (w) w.classList.toggle('d-none', kindEl.value !== 'URL_PAY');
      });
    }
    if (!idEl || !cdEl || !nmEl) return;
    idEl.value = preset.id != null ? String(preset.id) : '';
    cdEl.value = preset.pgCd || '';
    nmEl.value = preset.pgNm || '';
    if (epEl) epEl.value = preset.apiEndpoint || '';
    var pgNewBlank = (preset.id == null || String(preset.id).trim() === '') && (!preset.pgCd || !String(preset.pgCd).trim());
    var resolvedKind = resolvePresetIntegKind(preset);
    if (kindEl) kindEl.value = pgNewBlank ? '' : resolvedKind;
    if (intEpEl) intEpEl.value = pgNewBlank ? '' : presetIntegrationEndpoint(preset, resolvedKind);
    if (uyEl) uyEl.value = (preset.useYn === 'N') ? 'N' : 'Y';
    if (midEl) midEl.value = preset.merchantMid != null ? String(preset.merchantMid) : '';
    if (rnEl) rnEl.value = preset.routeNo != null && preset.routeNo !== '' ? String(preset.routeNo) : '';
    if (sbEl) sbEl.value = (preset.sandboxYn === 'N') ? 'N' : 'Y';
    if (exEl) exEl.value = preset.credentialsExtraJson != null ? String(preset.credentialsExtraJson) : '';
    if (akEl) akEl.value = '';
    if (mkEl) mkEl.value = '';
    if (modeSel) {
      var um = preset.urlPayAmountMode != null ? String(preset.urlPayAmountMode).trim().toUpperCase() : '';
      modeSel.value = (um === 'DISPLAY') ? 'DISPLAY' : 'STANDARD';
    }
    if (modeWrap) modeWrap.classList.toggle('d-none', !(kindEl && kindEl.value === 'URL_PAY'));
    cdEl.readOnly = !!(preset.id);
    var el = document.getElementById('pgAgencyEditModal');
    if (el && window.bootstrap && bootstrap.Modal) {
      bootstrap.Modal.getOrCreateInstance(el).show();
    }
  };

  function initRegionalCardLimitTable(pane, initialData) {
    var tbody = pane.querySelector('#regionalCardLimitTbody');
    var addBtn = pane.querySelector('#regionalCardLimitAddBtn');
    var delBtn = pane.querySelector('#regionalCardLimitDelBtn');
    var emptyMsg = pane.querySelector('#regionalCardLimitEmpty');
    if (!tbody || !addBtn || addBtn._regionalCardLimitInit) return;
    addBtn._regionalCardLimitInit = true;
    function addRow(idx, data) {
      data = data || {};
      var tr = document.createElement('tr');
      tr.dataset.idx = idx;
      tr.innerHTML = '<td><input type="checkbox" class="regional-card-limit-row-check"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="payMethod" placeholder="결제구분" value="' + (data.payMethod || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="cardIssuer" placeholder="카드사" value="' + (data.cardIssuer || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="dayLimit" placeholder="일" value="' + (data.dayLimit || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="timesLimit" placeholder="회" value="' + (data.timesLimit || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="amtLimit" placeholder="원" value="' + (data.amtLimit || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="regReason" placeholder="등록사유" value="' + (data.regReason || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="regDt" readonly placeholder="등록일자" value="' + (data.regDt || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="modDt" readonly placeholder="수정일자" value="' + (data.modDt || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="remark" placeholder="비고" value="' + (data.remark || '') + '"></td>';
      tbody.appendChild(tr);
    }
    function updateEmpty() {
      if (emptyMsg) emptyMsg.style.display = tbody.querySelectorAll('tr').length ? 'none' : 'block';
    }
    (initialData || []).forEach(function (d, i) { addRow(i, d); });
    addBtn.addEventListener('click', function () {
      addRow(tbody.querySelectorAll('tr').length, {});
      updateEmpty();
    });
    if (delBtn) delBtn.addEventListener('click', function () {
      tbody.querySelectorAll('.regional-card-limit-row-check:checked').forEach(function (cb) { cb.closest('tr').remove(); });
      updateEmpty();
    });
    var checkAll = pane.querySelector('.regional-card-limit-check-all');
    if (checkAll) checkAll.addEventListener('change', function () {
      tbody.querySelectorAll('.regional-card-limit-row-check').forEach(function (cb) { cb.checked = checkAll.checked; });
    });
    updateEmpty();
  }

  function initRegionalTerminalTable(pane, initialData) {
    var tbody = pane.querySelector('#regionalTerminalTbody');
    var addBtn = pane.querySelector('#regionalTerminalAddBtn');
    var emptyMsg = pane.querySelector('#regionalTerminalEmpty');
    if (!tbody || !addBtn || addBtn._regionalTerminalInit) return;
    addBtn._regionalTerminalInit = true;
    function addRow(idx, data) {
      data = data || {};
      var tr = document.createElement('tr');
      tr.dataset.idx = idx;
      tr.innerHTML = '<td>' + (idx + 1) + '</td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="pgAgency" placeholder="결제대행사" value="' + (data.pgAgency || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="terminalId" placeholder="터미널ID" value="' + (data.terminalId || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="remark" placeholder="비고" value="' + (data.remark || '') + '"></td>';
      tbody.appendChild(tr);
    }
    function updateEmpty() {
      if (emptyMsg) emptyMsg.style.display = tbody.querySelectorAll('tr').length ? 'none' : 'block';
    }
    (initialData || []).forEach(function (d, i) { addRow(i, d); });
    addBtn.addEventListener('click', function () {
      var n = tbody.querySelectorAll('tr').length;
      addRow(n, {});
      tbody.querySelectorAll('tr').forEach(function (tr, i) { tr.querySelector('td:first-child').textContent = i + 1; });
      updateEmpty();
    });
    updateEmpty();
  }

  function initRegionalBusinessHolidayRanges(pane, initialData) {
    var tbody = pane.querySelector('#bizHolidayRangeTbody');
    var addBtn = pane.querySelector('#bizHolidayAddBtn');
    var fromEl = pane.querySelector('#bizHolidayFromDate');
    var toEl = pane.querySelector('#bizHolidayToDate');
    var reasonEl = pane.querySelector('#bizHolidayReason');
    var writerEl = pane.querySelector('#bizHolidayWriter');
    var jsonEl = pane.querySelector('#businessHolidayRangesJson');
    if (!tbody || !addBtn || addBtn._bizHolidayInit) return;
    addBtn._bizHolidayInit = true;

    function todayYmd() {
      var d = new Date();
      var m = String(d.getMonth() + 1).padStart(2, '0');
      var day = String(d.getDate()).padStart(2, '0');
      return d.getFullYear() + '-' + m + '-' + day;
    }
    function esc(s) {
      return String(s == null ? '' : s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
    }
    function readRows() {
      var rows = [];
      tbody.querySelectorAll('tr[data-row="1"]').forEach(function (tr) {
        rows.push({
          fromDate: tr.getAttribute('data-from') || '',
          toDate: tr.getAttribute('data-to') || '',
          reason: tr.getAttribute('data-reason') || '',
          addedDate: tr.getAttribute('data-added') || '',
          writer: tr.getAttribute('data-writer') || ''
        });
      });
      return rows;
    }
    function syncHidden() {
      if (jsonEl) jsonEl.value = JSON.stringify(readRows());
    }
    function clearEditor() {
      if (fromEl) fromEl.value = '';
      if (toEl) toEl.value = '';
      if (reasonEl) reasonEl.value = '';
      if (writerEl) writerEl.value = '';
      addBtn.textContent = '추가';
      delete addBtn.dataset.editingIdx;
    }
    function renderRows(rows) {
      tbody.innerHTML = '';
      if (!rows || rows.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-muted text-center">추가된 기간이 없습니다.</td></tr>';
        syncHidden();
        return;
      }
      rows.forEach(function (r, idx) {
        var tr = document.createElement('tr');
        tr.setAttribute('data-row', '1');
        tr.setAttribute('data-from', r.fromDate || '');
        tr.setAttribute('data-to', r.toDate || '');
        tr.setAttribute('data-reason', r.reason || '');
        tr.setAttribute('data-added', r.addedDate || todayYmd());
        tr.setAttribute('data-writer', r.writer || '');
        tr.innerHTML =
          '<td>' + esc(r.fromDate || '') + '</td>' +
          '<td>' + esc(r.toDate || '') + '</td>' +
          '<td>' + esc(r.reason || '') + '</td>' +
          '<td>' + esc(r.addedDate || todayYmd()) + '</td>' +
          '<td>' + esc(r.writer || '') + '</td>' +
          '<td>' +
          '<button type="button" class="btn btn-sm btn-outline-secondary me-1 biz-holiday-edit" data-idx="' + idx + '">수정</button>' +
          '<button type="button" class="btn btn-sm btn-success me-1 biz-holiday-confirm" data-idx="' + idx + '">확인</button>' +
          '<button type="button" class="btn btn-sm btn-outline-danger biz-holiday-delete" data-idx="' + idx + '">삭제</button>' +
          '</td>';
        tbody.appendChild(tr);
      });
      syncHidden();
    }
    function normalizeInitialRows(data) {
      var rows = [];
      (data || []).forEach(function (r) {
        var from = r.fromDate || r.from || r.date || '';
        var to = r.toDate || r.to || r.date || from;
        if (!from) return;
        rows.push({
          fromDate: from,
          toDate: to || from,
          reason: r.reason || r.content || '',
          addedDate: r.addedDate || r.regDt || todayYmd(),
          writer: r.writer || r.createdBy || ''
        });
      });
      return rows;
    }
    renderRows(normalizeInitialRows(initialData));

    addBtn.addEventListener('click', function () {
      var from = fromEl && fromEl.value ? fromEl.value : '';
      var to = toEl && toEl.value ? toEl.value : '';
      var reason = reasonEl && reasonEl.value ? reasonEl.value.trim() : '';
      var writer = writerEl && writerEl.value ? writerEl.value.trim() : '';
      if (!from) { alert('언제부터 날짜를 입력하세요.'); return; }
      if (!to) { alert('언제까지 날짜를 입력하세요.'); return; }
      if (from > to) { alert('시작일은 종료일보다 클 수 없습니다.'); return; }
      var rows = readRows();
      var editingIdx = addBtn.dataset.editingIdx;
      var newRow = { fromDate: from, toDate: to, reason: reason, addedDate: todayYmd(), writer: writer };
      if (editingIdx != null && editingIdx !== '') {
        var old = rows[Number(editingIdx)] || {};
        newRow.addedDate = old.addedDate || todayYmd();
        rows[Number(editingIdx)] = newRow;
      } else {
        rows.push(newRow);
      }
      renderRows(rows);
      clearEditor();
    });

    tbody.addEventListener('click', function (e) {
      var editBtn = e.target.closest('.biz-holiday-edit');
      var confirmBtn = e.target.closest('.biz-holiday-confirm');
      var deleteBtn = e.target.closest('.biz-holiday-delete');
      if (!editBtn && !confirmBtn && !deleteBtn) return;
      var idx = Number((editBtn || confirmBtn || deleteBtn).getAttribute('data-idx'));
      var rows = readRows();
      if (!rows[idx]) return;
      if (editBtn) {
        if (fromEl) fromEl.value = rows[idx].fromDate || '';
        if (toEl) toEl.value = rows[idx].toDate || '';
        if (reasonEl) reasonEl.value = rows[idx].reason || '';
        if (writerEl) writerEl.value = rows[idx].writer || '';
        addBtn.dataset.editingIdx = String(idx);
        addBtn.textContent = '수정확인';
        return;
      }
      if (confirmBtn) {
        alert('확인 완료: ' + (rows[idx].fromDate || '') + ' ~ ' + (rows[idx].toDate || ''));
        return;
      }
      if (deleteBtn) {
        if (!confirm('해당 영업일 기간을 삭제하시겠습니까?')) return;
        rows.splice(idx, 1);
        renderRows(rows);
        clearEditor();
      }
    });
  }

  function initRegionalHolidayProfileSelector(pane, form, data) {
    if (!pane || !form) return;
    var profileSel = form.querySelector('[name="holidayProfileName"]');
    if (!profileSel || profileSel._holidayProfileInit) return;
    profileSel._holidayProfileInit = true;
    var countryView = form.querySelector('[name="holidayProfileCountry"]');
    var hiddenCountryCode = form.querySelector('[name="holidayCountryCode"]');
    var hiddenCountryCodes = form.querySelector('[name="holidayCountryCodes"]');
    var hiddenDates = form.querySelector('[name="businessHolidayExtraDates"]');
    var hiddenRanges = form.querySelector('[name="businessHolidayRangesJson"]');
    var lockHint = form.querySelector('.holiday-profile-lock-hint');
    if (!lockHint && profileSel && profileSel.parentElement) {
      lockHint = document.createElement('small');
      lockHint.className = 'holiday-profile-lock-hint text-danger d-none';
      lockHint.textContent = '총본사에서 상위 본사 영업일을 지정하여 이 총판의 영업일 설정은 상속 고정됩니다.';
      profileSel.parentElement.appendChild(lockHint);
    }
    function applyLockState() {
      var locked = (data && String(data.holidayLockedByHeadquartersYn || '').toUpperCase() === 'Y');
      if (profileSel) profileSel.disabled = !!locked;
      if (lockHint) lockHint.classList.toggle('d-none', !locked);
    }
    function applySelectedProfile() {
      var name = profileSel.value || '';
      var list = pane._hqBizdayProfiles || [];
      var profile = list.find(function (x) { return (x.name || '') === name; }) || null;
      if (!profile) {
        if (countryView) countryView.value = '';
        if (hiddenCountryCode) hiddenCountryCode.value = '';
        if (hiddenCountryCodes) hiddenCountryCodes.value = '';
        if (hiddenDates) hiddenDates.value = '';
        if (hiddenRanges) hiddenRanges.value = '[]';
        return;
      }
      var cc = profile.countryCode || 'KR';
      if (countryView) countryView.value = cc;
      if (hiddenCountryCode) hiddenCountryCode.value = cc;
      if (hiddenCountryCodes) hiddenCountryCodes.value = cc;
      if (hiddenDates) hiddenDates.value = profile.businessHolidayExtraDates || '';
      if (hiddenRanges) {
        var rows = String(profile.businessHolidayExtraDates || '').split(/\r?\n/)
          .map(function (d) { return d.trim(); })
          .filter(function (d) { return !!d; })
          .map(function (d) { return { fromDate: d, toDate: d, reason: '', addedDate: '', writer: '' }; });
        hiddenRanges.value = JSON.stringify(rows);
      }
    }
    window.PG_API.hqBusinessDaySettings().then(function (list) {
      pane._hqBizdayProfiles = Array.isArray(list) ? list : [];
      var opts = '<option value="">선택</option>';
      pane._hqBizdayProfiles.forEach(function (it) {
        opts += '<option value="' + (it.name || '') + '">' + (it.name || '') + '</option>';
      });
      profileSel.innerHTML = opts;
      var initName = (data && data.holidayProfileName) ? data.holidayProfileName : (profileSel.value || '');
      if (initName) profileSel.value = initName;
      applySelectedProfile();
      applyLockState();
    }).catch(function () {
      profileSel.innerHTML = '<option value="">선택</option>';
      applySelectedProfile();
      applyLockState();
    });
    profileSel.addEventListener('change', applySelectedProfile);
  }

  /** 결제내역 계열: 사이드 메뉴를 다시 눌렀을 때 목록 새로고침(재조회) */
  var PAY_LIST_MENU_RECLICK_REFRESH_URLS = ['/calc/payList', '/calc/chillPayTrList', '/calc/chillPaySettlementList', '/calc/payNotiList', '/calc/paySuccessList', '/calc/payFailList', '/calc/payRefundList', '/calc/payForceRefundList', '/calc/payCancelList', '/calc/payVoidList', '/calc/offsetCancList', '/pay/easyPay', '/pay/chatbotPay'];

  function refreshPayListPaneIfMenuRepeated(url, tabId) {
    if (PAY_LIST_MENU_RECLICK_REFRESH_URLS.indexOf(url) === -1) return;
    var pane = document.getElementById(tabId);
    if (!pane) return;
    setTimeout(function () {
      loadViewSetting().finally(function () {
        if (typeof pane._pgRunListSearch === 'function') pane._pgRunListSearch(pane, tabId, 1);
      });
    }, 0);
  }

  function addTabAndSwitch(url, menuId, label) {
    var tabId = getTabIdFromUrl(url);
    var ul = document.getElementById(TAB_UL);
    if (!ul) return;

    var existing = ul.querySelector('[top_tab_url="' + url + '"]');
    if (existing) {
      existing.querySelector('.tab-a').click();
      refreshPayListPaneIfMenuRepeated(url, tabId);
      return;
    }

    if (ul.querySelectorAll('.nav-item.copyTopTab').length >= MAX_TOP_TABS) {
      alert('메뉴는 최대 12탭 입니다. 추가 시 처음 탭이 자동 삭제됩니다.');
    }

    while (ul.querySelectorAll('.nav-item.copyTopTab').length >= MAX_TOP_TABS) {
      var firstNonMain = ul.querySelector('.nav-item.copyTopTab:not([top_tab_url="/main"])');
      if (!firstNonMain) break;
      var removedUrl = firstNonMain.getAttribute('top_tab_url');
      var removedTabId = getTabIdFromUrl(removedUrl);
      if (firstNonMain.classList.contains('active')) {
        var mainA = ul.querySelector('.copyTopTab[top_tab_url="/main"] .tab-a');
        if (mainA) mainA.click();
      }
      firstNonMain.remove();
      var pane = document.getElementById(removedTabId);
      if (pane && pane.parentNode) pane.parentNode.removeChild(pane);
    }

    var li = document.createElement('li');
    li.className = 'nav-item copyTopTab';
    li.setAttribute('top_tab_url', url);
    li.innerHTML = '<a href="#' + tabId + '" data-toggle="tab" class="nav-link tab-a" menu_id="' + (menuId || '') + '">' + (label || tabId) + '</a>' +
      '<button type="button" class="tab-close-button" tab_id="' + tabId + '">×</button>';
    ul.appendChild(li);

    ul.querySelectorAll('.tab-a').forEach(function (x) { x.classList.remove('active'); });
    li.querySelector('.tab-a').classList.add('active');

    setActiveMenuByUrl(url);
    loadContent(url, menuId, label);

    li.querySelector('.tab-close-button').addEventListener('click', function () {
      li.remove();
      var firstTab = ul.querySelector('.copyTopTab .tab-a');
      if (firstTab) firstTab.click();
    });
  }

  function setActiveMenuByUrl(url) {
    document.querySelectorAll('.side-nav .child-li').forEach(function (el) {
      el.classList.remove('mm-active');
      if (el.getAttribute('data-url') === url) el.classList.add('mm-active');
    });
    document.querySelectorAll('.side-nav-item').forEach(function (el) { el.classList.remove('mm-active'); });
    var activeChild = document.querySelector('.side-nav .child-li.mm-active');
    if (activeChild) {
      var parent = activeChild.closest('.side-nav-item');
      if (parent) {
        parent.classList.add('mm-active');
        var sub = parent.querySelector('.side-nav-second-level');
        if (sub) { sub.classList.add('mm-show'); parent.querySelector('.side-nav-link').setAttribute('aria-expanded', 'true'); }
      }
    }
    var menuIdEl = document.getElementById('_menuId');
    var active = document.querySelector('.side-nav .child-li.mm-active a');
    if (menuIdEl && active) menuIdEl.value = active.getAttribute('data-menu_id') || '';
  }

  function escapeCsvField(val) {
    var s = val == null ? '' : String(val);
    if (/[",\n\r]/.test(s)) return '"' + s.replace(/"/g, '""') + '"';
    return s;
  }

  function downloadTextAsFile(filename, text, mime) {
    var blob = new Blob([text], { type: mime || 'text/csv;charset=utf-8;' });
    var a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(a.href);
  }

  /** 그리드 엑셀다운로드 — 서버에서 헤더색·가운데정렬·테두리·텍스트열 서식 적용 xlsx */
  function downloadGridExcelCsv(pane) {
    var list = pane._lastGridList;
    var cols = pane._lastGridCols;
    if (!cols || !cols.length || list === undefined) {
      alert('다운로드할 데이터가 없습니다. [검색]으로 조회한 후 다시 시도하세요.');
      return;
    }
    var dataCols = cols.filter(function (c) { return c.type !== 'checkbox'; });
    if (!dataCols.length) {
      alert('보낼 컬럼이 없습니다.');
      return;
    }
    var url = pane.getAttribute('formurl') || '';
    var menu = (MENU_INFO[url] && MENU_INFO[url].label) ? MENU_INFO[url].label : '목록';
    var sheetName = menu.length > 31 ? menu.substring(0, 31) : menu;
    var headers = dataCols.map(function (c) { return String(c.label || c.key); });
    var rows = list.map(function (row) {
      return dataCols.map(function (c) {
        var v = row[c.key];
        if (v === undefined || v === null) return '';
        return String(v);
      });
    });
    var textKeys = {
      compId: 1, regNo: 1, contact: 1, accountNo: 1, zipCode: 1, loginId: 1,
      ceoMobile: 1, compTel: 1, bankNm: 1, rowNo: 1, terminalCountTerminal: 1, terminalCountWeb: 1,
      transferType: 1, calcProcType: 1, calcCycle: 1, calcExcludeYn: 1, payHoldYn: 1, useYn: 1, compDivNm: 1,
      siteRoot: 1
    };
    var textColumnIndexes = [];
    for (var ti = 0; ti < dataCols.length; ti++) {
      if (textKeys[dataCols[ti].key]) textColumnIndexes.push(ti);
    }
    var d = new Date();
    var ymd = d.getFullYear() + String(d.getMonth() + 1).padStart(2, '0') + String(d.getDate()).padStart(2, '0');
    var dimm = document.getElementById('dimm');
    if (dimm) dimm.style.display = 'flex';
    window.PG_API.exportStyledExcel({
      sheetName: sheetName,
      headers: headers,
      rows: rows,
      textColumnIndexes: textColumnIndexes
    }).then(function (blob) {
      var a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = menu + '_' + ymd + '.xlsx';
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(a.href);
    }).catch(function (e) {
      alert(e && e.message ? e.message : '엑셀 다운로드에 실패했습니다.');
    }).finally(function () {
      if (dimm) dimm.style.display = 'none';
    });
  }

  /** 업체 엑셀등록용 SAMPLE — 서버 생성 xlsx (헤더 색·표선·가운데 정렬·계좌번호 텍스트) */
  function downloadCompExcelSampleFile() {
    var dimm = document.getElementById('dimm');
    if (dimm) dimm.style.display = 'flex';
    window.PG_API.compExcelSample().then(function (blob) {
      var d = new Date();
      var ymd = d.getFullYear() + String(d.getMonth() + 1).padStart(2, '0') + String(d.getDate()).padStart(2, '0');
      var a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = '업체등록_SAMPLE_' + ymd + '.xlsx';
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(a.href);
    }).catch(function (e) {
      alert(e && e.message ? e.message : '샘플 다운로드에 실패했습니다.');
    }).finally(function () {
      if (dimm) dimm.style.display = 'none';
    });
  }

  /** 노티매핑 — 결제내역 계열(/calc/payList 변형) 화면 URL */
  var PAY_LIST_NOTIFY_LAYOUT_URLS = ['/calc/payList', '/calc/payNotiList', '/calc/paySuccessList', '/calc/payFailList', '/calc/payRefundList', '/calc/payForceRefundList', '/calc/payCancelList', '/calc/payVoidList', '/calc/offsetCancList', '/pay/easyPay', '/pay/chatbotPay'];

  function payListIntegratedToggleKeyList() {
    var P = typeof window !== 'undefined' ? window.PG_PAY_LIST_INTEGRATED : null;
    if (!P || !P.columns) return [];
    var fixed = P.columnGuideFixedKeys || [];
    return P.columns.map(function (c) { return c.key; }).filter(function (k) {
      return k && k !== '_chk' && k !== 'payActions' && fixed.indexOf(k) === -1;
    });
  }

  function chillPayTrToggleKeyList() {
    var screens = window.PG_SCREENS && typeof window.PG_SCREENS.getMenuScreens === 'function' ? window.PG_SCREENS.getMenuScreens() : null;
    var cfg = screens && screens['/calc/chillPayTrList'];
    if (!cfg || !cfg.columns) return [];
    var fixed = cfg.columnGuideFixedKeys || ['rowNo', 'transactionId', 'compNm', 'compId', 'trnDate', 'trnTime', 'routeNo'];
    return cfg.columns.map(function (c) { return c.key; }).filter(function (k) {
      return k && fixed.indexOf(k) === -1;
    });
  }

  function chillPaySettlementToggleKeyList() {
    var screens = window.PG_SCREENS && typeof window.PG_SCREENS.getMenuScreens === 'function' ? window.PG_SCREENS.getMenuScreens() : null;
    var cfg = screens && screens['/calc/chillPaySettlementList'];
    if (!cfg || !cfg.columns) return [];
    var fixed = cfg.columnGuideFixedKeys || ['rowNo'];
    return cfg.columns.map(function (c) { return c.key; }).filter(function (k) {
      return k && fixed.indexOf(k) === -1;
    });
  }

  /** VIEW SETTING 패널에서 실제로 보이는 체크 항목만 골라 기본 키 목록을 필터 */
  function payListFilterGuideKeysVisibleInPane(pane, keys) {
    if (!pane || !keys || !keys.length) return keys || [];
    var vis = {};
    pane.querySelectorAll('#tableColumnGuide .column-guide-item').forEach(function (item) {
      if (item.style && item.style.display === 'none') return;
      var cb = item.querySelector('.column-guide-check');
      var k = cb ? cb.getAttribute('data-key') : '';
      if (k) vis[k] = 1;
    });
    return keys.filter(function (k) { return vis[k]; });
  }

  /** 저장 전·초기화용: 결제관리 통합·통합내역·통합정산 VIEW SETTING 기본 선택 열 */
  function resolvePayListUserViewDefaultKeys(pageUrl, pane) {
    var def = [];
    if (pageUrl === '/calc/chillPayTrList') {
      var C = window.PG_CHILL_PAY_TR_VIEW_DEFAULTS;
      def = (C && C.viewSettingDefaultSelectedKeys) ? C.viewSettingDefaultSelectedKeys.slice() : [];
    } else if (pageUrl === '/calc/chillPaySettlementList') {
      var S = window.PG_CHILL_PAY_SETTLEMENT_VIEW_DEFAULTS;
      def = (S && S.viewSettingDefaultSelectedKeys) ? S.viewSettingDefaultSelectedKeys.slice() : [];
    } else if (PAY_LIST_NOTIFY_LAYOUT_URLS.indexOf(pageUrl) !== -1) {
      var P = window.PG_PAY_LIST_INTEGRATED;
      def = (P && P.viewSettingDefaultSelectedKeys) ? P.viewSettingDefaultSelectedKeys.slice() : [];
    } else if (pageUrl === '/calc/feeList' || pageUrl === '/settlement/feeList') {
      var screensFee = window.PG_SCREENS && typeof window.PG_SCREENS.getMenuScreens === 'function' ? window.PG_SCREENS.getMenuScreens() : null;
      var feeCfg = screensFee && screensFee['/calc/feeList'];
      def = (feeCfg && feeCfg.viewSettingDefaultSelectedKeys) ? feeCfg.viewSettingDefaultSelectedKeys.slice() : [];
    } else {
      return null;
    }
    return payListFilterGuideKeysVisibleInPane(pane, def);
  }

  /**
   * 조직항목설정 허용 열 체크 기본안. 결제관리 통합·통합내역·통합정산만 적용, 그 외 화면은 null(기존처럼 전체 체크).
   */
  function hqOrgAllowanceDefaultKeySet(pageUrl, viewerScope) {
    var vs = viewerScope || 'REGIONAL';
    if (pageUrl === '/calc/chillPayTrList') {
      var C = window.PG_CHILL_PAY_TR_VIEW_DEFAULTS;
      if (!C || !C.orgAllowanceDefaultKeysByScope) return null;
      if (vs === 'REGIONAL') return chillPayTrToggleKeyList();
      var c2 = C.orgAllowanceDefaultKeysByScope[vs];
      return Array.isArray(c2) ? c2 : null;
    }
    if (pageUrl === '/calc/chillPaySettlementList') {
      var S = window.PG_CHILL_PAY_SETTLEMENT_VIEW_DEFAULTS;
      if (!S || !S.orgAllowanceDefaultKeysByScope) return null;
      if (vs === 'REGIONAL') return chillPaySettlementToggleKeyList();
      var s2 = S.orgAllowanceDefaultKeysByScope[vs];
      return Array.isArray(s2) ? s2 : null;
    }
    if (PAY_LIST_NOTIFY_LAYOUT_URLS.indexOf(pageUrl) !== -1) {
      var P = window.PG_PAY_LIST_INTEGRATED;
      if (!P || !P.orgAllowanceDefaultKeysByScope) return null;
      if (vs === 'REGIONAL') return payListIntegratedToggleKeyList();
      var row = P.orgAllowanceDefaultKeysByScope[vs];
      return Array.isArray(row) ? row : null;
    }
    return null;
  }

  function mergePayListScreenLayout(baseCfg, layout) {
    if (!layout || layout.error || !Array.isArray(layout.columns) || layout.columns.length === 0) return null;
    var baseCols = baseCfg.columns || [];
    var byKey = {};
    baseCols.forEach(function (c) {
      if (c && c.key) byKey[c.key] = c;
    });
    var out = [];
    layout.columns.forEach(function (lc) {
      var k = lc && lc.key != null ? String(lc.key) : '';
      if (!k) return;
      var b = byKey[k];
      if (!b) return;
      var lab = lc.label != null && String(lc.label).trim() !== '' ? String(lc.label) : b.label;
      out.push(Object.assign({}, b, { label: lab }));
    });
    if (!out.length) return null;
    var merged = Object.assign({}, baseCfg, { columns: out });
    if (layout.headerGroups && layout.headerGroups.length) {
      merged.headerGroups = layout.headerGroups;
    }
    return merged;
  }

  /** 성공내역 등: 무효·수동·환불·강제 후속조치 열은 통합 결제내역(/calc/payList) 전용 */
  function isPayListPayFollowActionColumn(c) {
    if (!c) return false;
    if (c.type === 'payActions') return true;
    var k = c.key != null ? String(c.key) : '';
    if (k === 'payActions') return true;
    if (/^payFollow/i.test(k) || /^payAction/i.test(k)) return true;
    return false;
  }

  function stripPayFollowColumnsFromPayListCfg(cfg) {
    if (!cfg || !Array.isArray(cfg.columns)) return cfg;
    var next = Object.assign({}, cfg, {
      columns: cfg.columns.filter(function (col) { return !isPayListPayFollowActionColumn(col); })
    });
    return next;
  }

  function escPayCatalogHtml(s) {
    return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
  }

  function rebuildPayListColumnGuide(pane, cfg) {
    if (!cfg || !cfg.columns || cfg.tableColumnGuide === false) return;
    var guide = pane.querySelector('#tableColumnGuide .column-guide-list');
    if (!guide) return;
    var defaultFixed = ['rowNo', 'compId', 'compNm', 'compDivNm', 'trnDate', 'trnTime', 'routeNo'];
    var fixedKeys = (cfg.columnGuideFixedKeys && cfg.columnGuideFixedKeys.length) ? cfg.columnGuideFixedKeys : defaultFixed;
    var cols = cfg.columns.filter(function (c) {
      if (!c || c.type === 'checkbox' || c.type === 'payActions' || c.type === 'commissionInlineActions' || c.type === 'accountAccessActions' || c.type === 'accountAccessDelete' || c.type === 'userResetPassword' || c.type === 'userDelete') return false;
      return fixedKeys.indexOf(c.key) === -1;
    });
    var html = cols.map(function (c) {
      var key = c.key || '';
      var lab = c.columnGuideLabel || c.label || c.key;
      return '<label class="column-guide-item column-guide-item--on"><input type="checkbox" class="column-guide-check" data-key="' + escPayCatalogHtml(key) + '" checked> <span class="column-guide-label">' + escPayCatalogHtml(lab) + '</span></label>';
    }).join('');
    var defs = pane._viewCustomColumnDefs;
    if (defs && defs.length) {
      var seen = {};
      cols.forEach(function (c) { if (c && c.key) seen[c.key] = 1; });
      defs.forEach(function (cc) {
        var k = cc.columnKey || cc.column_key;
        var lab = cc.displayName || cc.display_name || k;
        if (!k || seen[k]) return;
        seen[k] = 1;
        html += '<label class="column-guide-item column-guide-item--on"><input type="checkbox" class="column-guide-check" data-key="' + escPayCatalogHtml(k) + '" data-hq-custom="1"> <span class="column-guide-label">' + escPayCatalogHtml(lab) + '</span></label>';
      });
    }
    guide.innerHTML = html;
    pane._columnGuideDefaultOrder = null;
  }

  function applyPayListCatalogTitleToTab(tabId, pageUrl, catalogTitle) {
    if (!catalogTitle || !tabId || !pageUrl) return;
    var parent = (MENU_INFO[pageUrl] && MENU_INFO[pageUrl].parent) ? MENU_INFO[pageUrl].parent : '결제관리';
    if (MENU_INFO[pageUrl]) MENU_INFO[pageUrl].label = catalogTitle;
    var tabA = document.querySelector('#' + TAB_UL + ' a[href="#' + tabId + '"]');
    if (tabA) tabA.textContent = catalogTitle;
    var breadcrumb = document.querySelector('.breadcrumb-item.navi, li.navi');
    if (breadcrumb) breadcrumb.textContent = parent + ' > ' + catalogTitle;
    var titleEl = document.getElementById('common__header__title');
    if (titleEl) titleEl.innerHTML = '<i class="bi bi-chevron-right"></i> ' + escPayCatalogHtml(catalogTitle);
  }

  function loadPayListNotifyLayout(pane, tabId, pageUrl, done) {
    if (typeof done !== 'function') done = function () {};
    if (PAY_LIST_NOTIFY_LAYOUT_URLS.indexOf(pageUrl) === -1) {
      done();
      return;
    }
    if (!window.PG_API || typeof window.PG_API.payListScreenLayout !== 'function') {
      done();
      return;
    }
    var prefetchCustom = Promise.resolve();
    if (window.PG_API.userViewSetting) {
      prefetchCustom = window.PG_API.userViewSetting(pageUrl).then(function (vd) {
        pane._viewCustomColumnDefs = (vd && vd.customViewColumns) ? vd.customViewColumns : [];
      }).catch(function () { pane._viewCustomColumnDefs = []; });
    }
    prefetchCustom.then(function () {
      return window.PG_API.payListScreenLayout(pageUrl);
    }).then(function (data) {
      pane._payListMergedCfg = null;
      if (!data || data.error) {
        done();
        return;
      }
      var base = window.PG_SCREENS && window.PG_SCREENS.getMenuScreens && window.PG_SCREENS.getMenuScreens()[pageUrl];
      if (!base || !base.payListVariant) {
        done();
        return;
      }
      var merged = mergePayListScreenLayout(base, data);
      if (merged && pageUrl === '/calc/paySuccessList') {
        merged = stripPayFollowColumnsFromPayListCfg(merged);
      }
      if (merged) pane._payListMergedCfg = merged;
      var title = data.catalogDisplayTitle;
      if (title && String(title).trim()) applyPayListCatalogTitleToTab(tabId, pageUrl, String(title).trim());
      try { rebuildPayListColumnGuide(pane, merged || base); } catch (eNm1) { /* ignore */ }
      done();
    }).catch(function () {
      pane._payListMergedCfg = null;
      done();
    });
  }

  function initHqNotifyMappingEditor(pane) {
    var rootEl = pane.querySelector('#hqNotifyMappingUiRoot');
    var hidTa = pane.querySelector('#hqNotifyMappingJsonTa') || pane.querySelector('[name="mappingDefinitionJson"]');
    var visTa = pane.querySelector('#hqNotifyMappingJsonVisible');
    var jsonWrap = pane.querySelector('#hqNotifyMappingJsonEditorWrap');
    if (!rootEl || !hidTa) return;
    var state = { root: {} };
    var NM_PAGE_LABELS = {
      '/calc/payList': '결제내역',
      '/calc/payNotiList': '노티내역',
      '/calc/paySuccessList': '성공내역',
      '/calc/payFailList': '실패내역',
      '/calc/payCancelList': '취소내역',
      '/calc/payVoidList': '무효내역',
      '/calc/payRefundList': '환불내역',
      '/calc/payForceRefundList': '강제환불내역',
      '/pay/easyPay': 'URL결제내역',
      '/pay/chatbotPay': '챗봇결제내역',
      '/calc/offsetCancList': '상계취소내역'
    };
    function escNmAttr(s) {
      return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
    }
    function escNmTa(s) {
      return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }
    function parseRootFromTextarea() {
      try {
        var t = String(hidTa.value || '').trim();
        if (!t) {
          state.root = { version: 2, memo: '', columnCatalogs: [], pageCatalogAssignments: [], vendors: [] };
          return true;
        }
        state.root = JSON.parse(t);
        if (!state.root || typeof state.root !== 'object') throw new Error('not object');
        if (!Array.isArray(state.root.columnCatalogs)) state.root.columnCatalogs = [];
        if (!Array.isArray(state.root.pageCatalogAssignments)) state.root.pageCatalogAssignments = [];
        if (!Array.isArray(state.root.vendors)) state.root.vendors = [];
        if (state.root.version == null) state.root.version = 2;
        return true;
      } catch (e1) {
        state.root = { version: 2, memo: '', columnCatalogs: [], pageCatalogAssignments: [], vendors: [] };
        try {
          hidTa.value = JSON.stringify(state.root, null, 2);
        } catch (eTa) { /* ignore */ }
        alert('저장된 매핑 JSON을 파싱할 수 없어 빈 설정으로 표시합니다. [전문가용: JSON 직접 편집]에서 고치거나 [기본 카탈로그·화면연결 삽입]을 사용하세요.');
        return true;
      }
    }
    function syncStateToTextareas() {
      var s = JSON.stringify(state.root, null, 2);
      hidTa.value = s;
      if (visTa && jsonWrap && !jsonWrap.classList.contains('d-none')) visTa.value = s;
    }
    function defaultChannels() {
      return [
        { channelCode: 'CALLBACK', channelName: 'CALLBACK (서버 노티)', targetPageUrl: '/calc/payList', targetPageLabel: '통합 결제내역', fieldMappings: [] },
        { channelCode: 'RESULT', channelName: 'RESULT (브라우저 리다이렉트)', targetPageUrl: '/pay/pay.html', targetPageLabel: '결제(리다이렉트) 화면', fieldMappings: [] },
        { channelCode: 'RETURN', channelName: 'RETURN (동기 응답·return_url)', targetPageUrl: '', targetPageLabel: '', fieldMappings: [] }
      ];
    }
    function catalogSelectOptionsSelected(selectedId) {
      var sel = selectedId ? String(selectedId).trim() : '';
      var opts = '<option value="">' + escNmAttr('— 선택 —') + '</option>';
      (state.root.columnCatalogs || []).forEach(function (c) {
        var id = String(c.catalogId || '').trim();
        if (!id) return;
        var isSel = sel === id ? ' selected' : '';
        opts += '<option value="' + escNmAttr(id) + '"' + isSel + '>' + escNmAttr(c.displayTitle || id) + '</option>';
      });
      return opts;
    }
    function nmAllCatalogKeys(root) {
      var keys = [];
      var seen = {};
      (root.columnCatalogs || []).forEach(function (c) {
        (c.columns || []).forEach(function (col) {
          var k = col && col.key != null ? String(col.key).trim() : '';
          if (!k || k === '_chk' || k === 'payActions') return;
          if (col.visible === false) return;
          if (!seen[k]) {
            seen[k] = 1;
            keys.push(k);
          }
        });
      });
      keys.sort();
      return keys;
    }
    function nmCatalogLabelForKey(rootObj, catalogId, colKey) {
      var cid = catalogId ? String(catalogId).trim() : '';
      var k = colKey ? String(colKey).trim() : '';
      if (!cid || !k) return k;
      var cata = (rootObj.columnCatalogs || []).filter(function (c) { return String(c.catalogId || '').trim() === cid; })[0];
      if (!cata || !cata.columns) return k;
      var hit = cata.columns.filter(function (col) { return col && col.key === k; })[0];
      return (hit && hit.label != null && String(hit.label).trim()) ? String(hit.label).trim() : k;
    }
    /** 마법사 "우리 표시명"을 columnCatalogs[].columns[].label 에 반영 */
    function nmApplyDisplayLabelsToCatalog(rootRef, catalogId, rows) {
      var cid = String(catalogId || '').trim();
      if (!cid || !rootRef) return;
      (rows || []).forEach(function (r) {
        var k = r.internalKey ? String(r.internalKey).trim() : '';
        var lab = r.displayLabel != null ? String(r.displayLabel).trim() : '';
        if (!k || !lab) return;
        (rootRef.columnCatalogs || []).forEach(function (cat) {
          if (String(cat.catalogId || '').trim() !== cid) return;
          (cat.columns || []).forEach(function (col) {
            if (col && col.key === k) col.label = lab;
          });
        });
      });
    }
    function ensureVendorChannelsForVendor(v) {
      var defs = defaultChannels();
      var byCode = {};
      (v.channels || []).forEach(function (ch) {
        if (ch && ch.channelCode) byCode[String(ch.channelCode).toUpperCase()] = ch;
      });
      return defs.map(function (d) {
        var u = String(d.channelCode).toUpperCase();
        var ex = byCode[u];
        if (!ex) {
          return { channelCode: d.channelCode, channelName: d.channelName, targetPageUrl: d.targetPageUrl, targetPageLabel: d.targetPageLabel, fieldMappings: [] };
        }
        return {
          channelCode: ex.channelCode || d.channelCode,
          channelName: ex.channelName != null ? ex.channelName : d.channelName,
          targetPageUrl: ex.targetPageUrl != null ? ex.targetPageUrl : d.targetPageUrl,
          targetPageLabel: ex.targetPageLabel != null ? ex.targetPageLabel : d.targetPageLabel,
          fieldMappings: Array.isArray(ex.fieldMappings) ? ex.fieldMappings.slice() : []
        };
      });
    }
    function nmInternalKeyOptions(catKeys, selected, rootForLabels, catalogIdForLabels) {
      var ik = selected ? String(selected) : '';
      var h = '<option value="">' + escNmAttr('— 열 선택 —') + '</option>';
      if (ik && catKeys.indexOf(ik) === -1) {
        h += '<option value="' + escNmAttr(ik) + '" selected>' + escNmAttr(ik + ' (카탈로그外)') + '</option>';
      }
      catKeys.forEach(function (k) {
        var sel = ik === k ? ' selected' : '';
        var lab = (rootForLabels && catalogIdForLabels) ? nmCatalogLabelForKey(rootForLabels, catalogIdForLabels, k) : k;
        var disp = (lab && lab !== k) ? (k + ' — ' + lab) : k;
        h += '<option value="' + escNmAttr(k) + '"' + sel + '>' + escNmAttr(disp) + '</option>';
      });
      return h;
    }
    var NM_CHILL_PRESET_PARAMS = ['TransactionId', 'transactionId', 'OrderNo', 'orderNo', 'Amount', 'amount', 'RouteNo', 'routeNo', 'PaymentStatus', 'paymentStatus', 'Status', 'status', 'MerchantCode', 'merchantCode', 'Currency', 'currency', 'CustomerId', 'customerId', 'CustomerName', 'customerName', 'PaymentChannel', 'paymentChannel', 'PaymentDate', 'paymentDate', 'Fee', 'fee', 'TotalAmount', 'totalAmount', 'Icopay', 'icopay', 'Description', 'description'];
    function nmCollectJsonKeys(jsonStr) {
      var keys = [];
      try {
        var o = JSON.parse(String(jsonStr || '').trim());
        if (!o || typeof o !== 'object') return keys;
        function collect(obj) {
          Object.keys(obj).forEach(function (k) {
            if (keys.indexOf(k) === -1) keys.push(k);
          });
        }
        collect(o);
        if (o.data && typeof o.data === 'object' && !Array.isArray(o.data)) collect(o.data);
      } catch (eK) { /* ignore */ }
      return keys;
    }
    function nmBuildPgSelectOptions(selectedPg) {
      var opts = '<option value="">' + escNmAttr('— 결제대행사 선택 —') + '</option>';
      (state.root.vendors || []).forEach(function (v) {
        var cd = String(v.vendorCode || '').trim();
        if (!cd) return;
        var nm = String(v.vendorName || cd).trim();
        var sel = selectedPg === cd ? ' selected' : '';
        opts += '<option value="' + escNmAttr(cd) + '"' + sel + '>' + escNmAttr(nm + ' (' + cd + ')') + '</option>';
      });
      return opts;
    }
    function nmReadGuiRowsFromTbody(tb) {
      var rows = [];
      if (!tb) return rows;
      tb.querySelectorAll('.hq-nm-gui-row').forEach(function (tr) {
        var pf = tr.querySelector('.hq-nm-gui-pf');
        var ik = tr.querySelector('.hq-nm-gui-ik');
        var nt = tr.querySelector('.hq-nm-gui-note');
        var lb = tr.querySelector('.hq-nm-gui-lbl');
        var lk = tr.querySelector('.hq-nm-gui-lock');
        var a = pf ? String(pf.value || '').trim() : '';
        var b = ik ? String(ik.value || '').trim() : '';
        if (!a && !b && !(nt && String(nt.value || '').trim()) && !(lb && String(lb.value || '').trim())) return;
        rows.push({
          pgField: a,
          internalKey: b,
          note: nt ? String(nt.value || '').trim() : '',
          displayLabel: lb ? String(lb.value || '').trim() : '',
          lockAi: !!(lk && lk.checked)
        });
      });
      return rows;
    }
    function nmApplyGuiRowsToVendor(pgCd, chCode, rows) {
      var pgc = String(pgCd || '').trim();
      var chc = String(chCode || 'CALLBACK').trim();
      if (!pgc) return false;
      (state.root.vendors || []).forEach(function (vx) {
        if (String(vx.vendorCode || '').trim() !== pgc) return;
        var chs = ensureVendorChannelsForVendor(vx);
        chs.forEach(function (ch) {
          if (String(ch.channelCode || '').trim() !== chc) return;
          ch.fieldMappings = rows.filter(function (r) { return r.pgField && r.internalKey; }).map(function (r) {
            var o = { pgField: r.pgField, internalKey: r.internalKey, note: r.note || '' };
            if (r.lockAi) o.lockAi = true;
            return o;
          });
        });
        vx.channels = chs;
      });
      return true;
    }
    function nmCaptureGuiScratch() {
      var pgEl = rootEl.querySelector('#hqNmSelPg');
      var chEl = rootEl.querySelector('#hqNmSelCh');
      var tb = rootEl.querySelector('#hqNmGuiTbody');
      if (!pgEl || !chEl || !tb) {
        pane._hqNmGuiScratch = null;
        return;
      }
      pane._hqNmGuiScratch = {
        pg: String(pgEl.value || '').trim(),
        ch: String(chEl.value || 'CALLBACK').trim(),
        rows: nmReadGuiRowsFromTbody(tb)
      };
    }
    function nmRestoreGuiScratch(catKeys) {
      var sc = pane._hqNmGuiScratch;
      var tb = rootEl.querySelector('#hqNmGuiTbody');
      var pgEl = rootEl.querySelector('#hqNmSelPg');
      var chEl = rootEl.querySelector('#hqNmSelCh');
      if (!sc || !tb || !pgEl || !chEl) return;
      var hasPg = false;
      for (var oi = 0; oi < pgEl.options.length; oi++) {
        if (pgEl.options[oi].value === sc.pg) { hasPg = true; break; }
      }
      if (hasPg) pgEl.value = sc.pg;
      chEl.value = sc.ch || 'CALLBACK';
      var wcidL = pane._hqNmWizardCatalogSel;
      if (!wcidL && (state.root.columnCatalogs || []).length && (state.root.columnCatalogs[0] || {}).catalogId) {
        wcidL = String(state.root.columnCatalogs[0].catalogId || '').trim();
      }
      if (!wcidL) wcidL = 'cat_pay_integrated_default';
      tb.innerHTML = (sc.rows || []).map(function (r) {
        return '<tr class="hq-nm-gui-row"><td><input type="text" class="form-control form-control-sm hq-nm-gui-pf" value="' + escNmAttr(r.pgField || '') + '" placeholder="PG 필드명"></td>' +
          '<td><select class="form-select form-select-sm hq-nm-gui-ik">' + nmInternalKeyOptions(catKeys, r.internalKey, state.root, wcidL) + '</select></td>' +
          '<td><input type="text" class="form-control form-control-sm hq-nm-gui-lbl" value="' + escNmAttr(r.displayLabel || '') + '" placeholder="카탈로그 기본"></td>' +
          '<td class="text-center align-middle"><input type="checkbox" class="form-check-input hq-nm-gui-lock" title="AI·자동 제안 시 이 행 유지"' + (r.lockAi ? ' checked' : '') + '></td>' +
          '<td><input type="text" class="form-control form-control-sm hq-nm-gui-note" value="' + escNmAttr(r.note || '') + '"></td>' +
          '<td class="text-nowrap"><button type="button" class="btn btn-sm btn-outline-danger hq-nm-gui-row-del">삭제</button></td></tr>';
      }).join('');
    }
    function nmBuildSummaryRowsHtml(root, catKeys) {
      var lines = [];
      (root.vendors || []).forEach(function (v) {
        var vcd = String(v.vendorCode || '').trim();
        if (!vcd) return;
        ensureVendorChannelsForVendor(v).forEach(function (ch) {
          var ccd = String(ch.channelCode || '').trim();
          (ch.fieldMappings || []).forEach(function (fm, idx) {
            lines.push('<tr class="hq-nm-sum-row" data-sum-v="' + escNmAttr(vcd) + '" data-sum-c="' + escNmAttr(ccd) + '" data-sum-i="' + idx + '">' +
              '<td class="font-monospace small">' + escNmAttr(vcd) + '</td>' +
              '<td class="small">' + escNmAttr(ccd) + '</td>' +
              '<td class="font-monospace small">' + escNmAttr(fm.pgField || '') + '</td>' +
              '<td class="font-monospace small">' + escNmAttr(fm.internalKey || '') + '</td>' +
              '<td class="small">' + escNmAttr(fm.note || '') + '</td>' +
              '<td class="text-nowrap"><button type="button" class="btn btn-sm btn-outline-primary hq-nm-sum-edit">편집</button> ' +
              '<button type="button" class="btn btn-sm btn-outline-danger hq-nm-sum-del">삭제</button></td></tr>');
          });
        });
      });
      if (!lines.length) {
        return '<tr><td colspan="6" class="text-muted text-center py-3">등록된 필드 매핑이 없습니다. 위 <strong>매핑 작업 표</strong>에서 추가하거나 PG 목록 동기화 후 설정하세요.</td></tr>';
      }
      return lines.join('');
    }
    function applyGuiTableToSelectedVendorOnSave() {
      var pgEl = rootEl.querySelector('#hqNmSelPg');
      var chEl = rootEl.querySelector('#hqNmSelCh');
      var tb = rootEl.querySelector('#hqNmGuiTbody');
      if (!pgEl || !chEl || !tb) return;
      var pgc = String(pgEl.value || '').trim();
      if (!pgc) return;
      var rows = nmReadGuiRowsFromTbody(tb).filter(function (r) { return r.pgField && r.internalKey; });
      /* 매핑 작업 표가 비어 있으면 저장 시 기존(고급 PG 상세·JSON) 데이터를 덮어쓰지 않음 */
      if (!rows.length) return;
      nmApplyGuiRowsToVendor(pgc, chEl.value || 'CALLBACK', rows);
      syncStateToTextareas();
    }
    function readUiIntoState() {
      var memo = '';
      try {
        var prev = JSON.parse(String(hidTa.value || '{}'));
        if (prev && typeof prev === 'object' && prev.memo != null) memo = String(prev.memo);
      } catch (ePrev) { /* ignore */ }
      state.root = { version: 2, memo: memo, columnCatalogs: [], pageCatalogAssignments: [], vendors: [] };
      var cats = [];
      rootEl.querySelectorAll('#hqNmCatTbody tr[data-cat-idx]').forEach(function (tr) {
        var idInp = tr.querySelector('.hq-nm-cat-id');
        var tiInp = tr.querySelector('.hq-nm-cat-title');
        var hgTa = tr.querySelector('.hq-nm-cat-hg');
        var colTa = tr.querySelector('.hq-nm-cat-cols');
        var cid = idInp ? String(idInp.value || '').trim() : '';
        if (!cid) return;
        var obj = { catalogId: cid, displayTitle: tiInp ? String(tiInp.value || '').trim() : '', columns: [] };
        try {
          obj.headerGroups = hgTa && String(hgTa.value || '').trim() ? JSON.parse(hgTa.value) : [];
          if (!Array.isArray(obj.headerGroups)) obj.headerGroups = [];
        } catch (e2) {
          alert('카탈로그 ' + cid + ' 의 headerGroups JSON이 올바르지 않습니다.');
          throw e2;
        }
        try {
          obj.columns = colTa && String(colTa.value || '').trim() ? JSON.parse(colTa.value) : [];
          if (!Array.isArray(obj.columns)) obj.columns = [];
        } catch (e3) {
          alert('카탈로그 ' + cid + ' 의 columns JSON이 올바르지 않습니다.');
          throw e3;
        }
        cats.push(obj);
      });
      state.root.columnCatalogs = cats;
      var assigns = [];
      rootEl.querySelectorAll('.hq-nm-page-cat').forEach(function (sel) {
        var pu = sel.getAttribute('data-page-url') || '';
        if (!pu) return;
        var v = String(sel.value || '').trim();
        if (v) assigns.push({ pageUrl: pu, catalogId: v });
      });
      state.root.pageCatalogAssignments = assigns;
      var vendors = [];
      rootEl.querySelectorAll('.hq-nm-vendor-block').forEach(function (block) {
        var code = String(block.getAttribute('data-vendor-code') || '').trim();
        if (!code) return;
        var vn = block.querySelector('.hq-nm-vendor-name');
        var channels = [];
        block.querySelectorAll('.hq-nm-ch-block').forEach(function (chEl) {
          var chCode = String(chEl.getAttribute('data-ch-code') || '').trim();
          var def = defaultChannels().filter(function (d) { return String(d.channelCode).toUpperCase() === String(chCode).toUpperCase(); })[0];
          if (!def) def = { channelCode: chCode, channelName: chCode, targetPageUrl: '', targetPageLabel: '', fieldMappings: [] };
          var fieldMappings = [];
          chEl.querySelectorAll('.hq-nm-fm-row').forEach(function (row) {
            var pf = row.querySelector('.hq-nm-pg-field');
            var sk = row.querySelector('.hq-nm-internal-key');
            var nt = row.querySelector('.hq-nm-note');
            var lk = row.querySelector('.hq-nm-lock-ai');
            var pgf = pf ? String(pf.value || '').trim() : '';
            var ikv = sk ? String(sk.value || '').trim() : '';
            if (!pgf || !ikv) return;
            var fmObj = { pgField: pgf, internalKey: ikv, note: nt ? String(nt.value || '').trim() : '' };
            if (lk && lk.checked) fmObj.lockAi = true;
            fieldMappings.push(fmObj);
          });
          channels.push({
            channelCode: def.channelCode,
            channelName: def.channelName,
            targetPageUrl: def.targetPageUrl,
            targetPageLabel: def.targetPageLabel,
            fieldMappings: fieldMappings
          });
        });
        var dmTa = block.querySelector('.hq-nm-display-maps');
        var displayMaps = {};
        if (dmTa && String(dmTa.value || '').trim()) {
          try {
            displayMaps = JSON.parse(dmTa.value);
            if (!displayMaps || typeof displayMaps !== 'object' || Array.isArray(displayMaps)) throw new Error('not object');
          } catch (eDm) {
            alert('PG ' + code + ' 의 displayMaps JSON이 올바르지 않습니다.');
            throw eDm;
          }
        }
        vendors.push({ vendorCode: code, vendorName: vn ? String(vn.value || '').trim() : '', channels: channels, displayMaps: displayMaps });
      });
      state.root.vendors = vendors;
      return true;
    }
    function refreshNmAiBadge() {
      if (!window.PG_API || !window.PG_API.hqNotifyMappingAiStatus) return;
      window.PG_API.hqNotifyMappingAiStatus().then(function (d) {
        var el = rootEl.querySelector('#hqNmAiBadge');
        if (!el) return;
        if (d && d.aiConfigured) {
          el.textContent = 'AI 분석 가능';
          el.className = 'badge bg-success ms-1';
        } else {
          el.textContent = 'AI 미설정(규칙만)';
          el.className = 'badge bg-secondary ms-1';
        }
      }).catch(function () {
        var el2 = rootEl.querySelector('#hqNmAiBadge');
        if (el2) {
          el2.textContent = 'AI 상태 확인 실패';
          el2.className = 'badge bg-warning text-dark ms-1';
        }
      });
    }
    function render() {
      if (!parseRootFromTextarea()) return;
      nmCaptureGuiScratch();
      var cats = state.root.columnCatalogs || [];
      var catRows = cats.map(function (c, i) {
        var hgJson = JSON.stringify(c.headerGroups != null ? c.headerGroups : [], null, 2);
        var colsJson = JSON.stringify(c.columns != null ? c.columns : [], null, 2);
        return '<tr data-cat-idx="' + i + '"><td><input type="text" class="form-control form-control-sm hq-nm-cat-id" value="' + escNmAttr(c.catalogId || '') + '"></td>' +
          '<td><input type="text" class="form-control form-control-sm hq-nm-cat-title" value="' + escNmAttr(c.displayTitle || '') + '"></td>' +
          '<td><textarea class="form-control form-control-sm font-monospace small hq-nm-cat-hg" rows="3" placeholder="headerGroups JSON">' + escNmTa(hgJson) + '</textarea></td>' +
          '<td><textarea class="form-control form-control-sm font-monospace small hq-nm-cat-cols" rows="6" placeholder="columns JSON">' + escNmTa(colsJson) + '</textarea></td>' +
          '<td class="text-center"><button type="button" class="btn btn-sm btn-outline-danger hq-nm-cat-del">삭제</button></td></tr>';
      }).join('');
      var pageRows = PAY_LIST_NOTIFY_LAYOUT_URLS.map(function (pu) {
        var cur = '';
        (state.root.pageCatalogAssignments || []).forEach(function (a) {
          if (a && a.pageUrl === pu) cur = String(a.catalogId || '').trim();
        });
        return '<tr><td class="small">' + escNmAttr(NM_PAGE_LABELS[pu] || pu) + '<div class="text-muted font-monospace" style="font-size:0.75rem">' + escNmAttr(pu) + '</div></td>' +
          '<td><select class="form-select form-select-sm hq-nm-page-cat" data-page-url="' + escNmAttr(pu) + '">' + catalogSelectOptionsSelected(cur) + '</select></td></tr>';
      }).join('');
      var catKeys = nmAllCatalogKeys(state.root);
      var nmCatIdForIk = '';
      if ((state.root.columnCatalogs || []).length && (state.root.columnCatalogs[0] || {}).catalogId) {
        nmCatIdForIk = String(state.root.columnCatalogs[0].catalogId || '').trim();
      }
      var vendorBlocks = (state.root.vendors || []).map(function (v) {
        var code = String(v.vendorCode || '').trim();
        if (!code) return '';
        var chs = ensureVendorChannelsForVendor(v);
        var chHtml = chs.map(function (ch) {
          var cc = String(ch.channelCode || '').trim();
          var maps = Array.isArray(ch.fieldMappings) ? ch.fieldMappings : [];
          var rows = maps.map(function (fm) {
            return '<tr class="hq-nm-fm-row"><td><input type="text" class="form-control form-control-sm hq-nm-pg-field" value="' + escNmAttr(fm.pgField || '') + '" placeholder="예: TransactionId"></td>' +
              '<td><select class="form-select form-select-sm hq-nm-internal-key">' + nmInternalKeyOptions(catKeys, fm.internalKey, state.root, nmCatIdForIk) + '</select></td>' +
              '<td class="text-center align-middle"><input type="checkbox" class="form-check-input hq-nm-lock-ai" title="AI·자동 제안 시 유지"' + (fm.lockAi ? ' checked' : '') + '></td>' +
              '<td><input type="text" class="form-control form-control-sm hq-nm-note" value="' + escNmAttr(fm.note || '') + '"></td>' +
              '<td class="text-nowrap"><button type="button" class="btn btn-sm btn-outline-danger hq-nm-fm-del">삭제</button></td></tr>';
          }).join('');
          if (!rows) {
            rows = '<tr class="hq-nm-fm-empty"><td colspan="5" class="text-muted small">매핑 행이 없습니다. [행 추가] 또는 CALLBACK 샘플로 자동 제안</td></tr>';
          }
          var sampleTa = cc === 'CALLBACK'
            ? '<div class="mb-2"><label class="form-label small text-muted mb-0">샘플 CALLBACK JSON (자동 제안)</label><textarea class="form-control form-control-sm font-monospace small hq-nm-sample-json" rows="3" spellcheck="false" placeholder="{ ... }"></textarea>' +
              '<button type="button" class="btn btn-sm btn-outline-secondary mt-1 hq-nm-suggest-btn" data-vendor-code="' + escNmAttr(code) + '">파라미터 자동 매핑 제안</button></div>'
            : '';
          return '<div class="hq-nm-ch-block border rounded p-2 mb-2 bg-light bg-opacity-50" data-ch-code="' + escNmAttr(cc) + '">' +
            '<div class="fw-bold small mb-2">' + escNmAttr(ch.channelName || cc) + ' <span class="text-muted font-monospace">' + escNmAttr(cc) + '</span></div>' + sampleTa +
            '<div class="table-responsive"><table class="table table-sm align-middle mb-2">' +
            '<thead class="table-light"><tr><th style="min-width:7rem">PG 파라미터</th><th style="min-width:10rem">우리 항목 (열 key)</th><th style="width:3rem" class="text-center">AI잠금</th><th>비고</th><th style="width:3rem"></th></tr></thead>' +
            '<tbody class="hq-nm-fm-tbody">' + rows + '</tbody></table></div>' +
            '<button type="button" class="btn btn-sm btn-outline-primary hq-nm-fm-add" data-vendor-code="' + escNmAttr(code) + '" data-ch-code="' + escNmAttr(cc) + '">행 추가</button></div>';
        }).join('');
        var dmObj = v.displayMaps && typeof v.displayMaps === 'object' && !Array.isArray(v.displayMaps) ? v.displayMaps : {};
        var dmJson = JSON.stringify(dmObj, null, 2);
        return '<div class="hq-nm-vendor-block border rounded p-3 mb-3" data-vendor-code="' + escNmAttr(code) + '">' +
          '<div class="d-flex flex-wrap align-items-center gap-2 mb-2"><span class="badge bg-secondary font-monospace">' + escNmAttr(code) + '</span>' +
          '<input type="text" class="form-control form-control-sm hq-nm-vendor-name" style="max-width:16rem" value="' + escNmAttr(v.vendorName || '') + '" placeholder="표시명"></div>' + chHtml +
          '<div class="border-top pt-2 mt-3">' +
          '<h6 class="small fw-bold text-secondary mb-1">④ 결제대행사 추가설정 (표시값)</h6>' +
          '<p class="text-muted small mb-1">그리드 <strong>열 key</strong>마다 <code>{ "원문": "표시문자" }</code> JSON. <code>currency</code>·<code>chillPaymentStatus</code>는 <strong>PG displayMaps가 우선</strong>이고, 키가 없으면 서버 전역(통화: 숫자·알파 → JPY/KRW/THB/USD 짧은 표기; 상태: 성공·취소·실패·무효·이메일무효·요청·환불 등 한글)이 적용됩니다.</p>' +
          '<textarea class="form-control form-control-sm font-monospace small hq-nm-display-maps" rows="7" spellcheck="false" placeholder="{ &quot;currency&quot;: { &quot;392&quot;: &quot;JPY&quot; }, &quot;chillPaymentStatus&quot;: { &quot;Paid&quot;: &quot;성공&quot; } }">' + escNmTa(dmJson) + '</textarea></div></div>';
      }).join('');
      if (!vendorBlocks) {
        vendorBlocks = '<p class="text-muted small mb-0" id="hqNmVendorEmpty">등록된 PG가 없습니다. <strong>PG 목록 동기화</strong>를 실행하세요.</p>';
      }
      var selPgVal = (pane._hqNmGuiScratch && pane._hqNmGuiScratch.pg) ? pane._hqNmGuiScratch.pg : '';
      var wCatSel = pane._hqNmWizardCatalogSel;
      if (!wCatSel && cats.length && cats[0].catalogId) wCatSel = String(cats[0].catalogId || '').trim();
      var wizardCard =
        '<div class="card border-primary shadow-sm mb-3" id="hqNmWizardCard">' +
        '<div class="card-header py-2 bg-primary text-white"><strong>매핑 작업 표</strong> <span class="small fw-normal opacity-90">PG 수신 파라미터 → 우리 항목(열)</span> <span id="hqNmAiBadge" class="badge bg-light text-dark ms-1">…</span></div>' +
        '<div class="card-body">' +
        '<div class="row g-3 mb-3">' +
        '<div class="col-md-4">' +
        '<div class="hq-nm-step-badge mb-1">1</div>' +
        '<label class="form-label small fw-semibold mb-0">결제대행사 선택</label>' +
        '<select id="hqNmSelPg" class="form-select form-select-sm">' + nmBuildPgSelectOptions(selPgVal) + '</select>' +
        '<div class="form-check mt-2 mb-0"><input class="form-check-input" type="checkbox" id="hqNmAutoLoadInbound" checked>' +
        '<label class="form-check-label small" for="hqNmAutoLoadInbound">선택 시 실제 수신 노티에서 본 <strong>파라미터 이름</strong>을 자동으로 표에 합칩니다</label></div></div>' +
        '<div class="col-md-3">' +
        '<div class="hq-nm-step-badge mb-1">2</div>' +
        '<label class="form-label small fw-semibold mb-0">노티 채널</label>' +
        '<select id="hqNmSelCh" class="form-select form-select-sm">' +
        (function () {
          var ch = pane._hqNmGuiScratch && pane._hqNmGuiScratch.ch ? String(pane._hqNmGuiScratch.ch) : 'CALLBACK';
          return '<option value="CALLBACK"' + (ch === 'CALLBACK' ? ' selected' : '') + '>CALLBACK (서버 노티)</option>' +
            '<option value="RESULT"' + (ch === 'RESULT' ? ' selected' : '') + '>RESULT (브라우저)</option>' +
            '<option value="RETURN"' + (ch === 'RETURN' ? ' selected' : '') + '>RETURN (동기 응답)</option>';
        })() +
        '</select></div>' +
        '<div class="col-md-5">' +
        '<div class="hq-nm-step-badge mb-1">3</div>' +
        '<label class="form-label small fw-semibold mb-0">표시명이 반영될 카탈로그</label>' +
        '<select id="hqNmWizardCatalogId" class="form-select form-select-sm">' + catalogSelectOptionsSelected(wCatSel) + '</select>' +
        '<div class="form-text small mb-0">「우리 표시명」을 저장·적용하면 이 카탈로그의 열 이름이 바뀌고, 조직항목설정·결제 그리드와 동일하게 쓰입니다.</div></div></div>' +
        '<div class="d-flex flex-wrap gap-1 mb-2">' +
        '<button type="button" class="btn btn-sm btn-outline-secondary" id="hqNmGuiLoad" title="서버에 저장된 이 PG·채널 매핑을 표에 불러옵니다">저장분 불러오기</button>' +
        '<button type="button" class="btn btn-sm btn-primary" id="hqNmGuiApply">표 내용 → 매핑 반영</button>' +
        '<button type="button" class="btn btn-sm btn-outline-info" id="hqNmGuiInboundKeys">수신 노티에서 파라미터 다시 불러오기</button>' +
        '<button type="button" class="btn btn-sm btn-outline-primary" id="hqNmGuiSuggestApi" title="AI 가능 시 우선">AI·자동 제안</button>' +
        '<button type="button" class="btn btn-sm btn-outline-secondary" id="hqNmGuiSuggestRule">규칙만 제안</button>' +
        '<button type="button" class="btn btn-sm btn-outline-secondary" id="hqNmGuiRowAdd">행 추가</button>' +
        '</div>' +
        '<details class="mb-3 border rounded px-2 py-1 bg-light"><summary class="small fw-semibold user-select-none py-1">샘플 JSON으로 키 추출·제안 (선택)</summary>' +
        '<div class="pt-2 pb-1">' +
        '<label class="form-label small mb-0">샘플 노티 JSON</label>' +
        '<textarea id="hqNmGuiSampleJson" class="form-control form-control-sm font-monospace" rows="3" spellcheck="false" placeholder="노티 본문 예시를 붙여 넣으면 키 목록·제안에 사용합니다"></textarea>' +
        '<div class="form-check form-check-inline small mt-1"><input class="form-check-input" type="checkbox" id="hqNmAutoAiAfterJson" checked><label class="form-check-label" for="hqNmAutoAiAfterJson">JSON에서 키 만든 뒤 자동 매핑( AI 가능하면 우선 )</label></div>' +
        '<div class="d-flex flex-wrap gap-1 mt-1">' +
        '<button type="button" class="btn btn-sm btn-outline-secondary" id="hqNmGuiFromJson">JSON에서 파라미터 키 목록</button>' +
        '<button type="button" class="btn btn-sm btn-outline-secondary" id="hqNmGuiChillPreset">CHILLPAY 일반 파라미터 넣기</button>' +
        '</div></div></details>' +
        '<div class="table-responsive border rounded"><table class="table table-sm align-middle mb-0">' +
        '<thead class="table-light"><tr><th style="min-width:9rem">PG에서 온 파라미터 이름</th><th style="min-width:12rem">우리 항목 (열 key)</th><th style="min-width:8rem">우리 표시명</th><th style="width:3rem" class="text-center" title="자동·AI 제안 시 이 줄 유지">AI잠금</th><th>비고</th><th style="width:3.5rem"></th></tr></thead>' +
        '<tbody id="hqNmGuiTbody"></tbody></table></div>' +
        '<p class="text-muted small mt-2 mb-0"><strong>표 내용 → 매핑 반영</strong> 후 아래 고급 영역의 PG 상세와 동기화됩니다. 서버에 남기려면 화면 맨 아래 <strong>저장</strong>을 누르세요.</p>' +
        '</div></div>';
      var summaryHtml =
        '<h6 class="text-secondary small fw-bold mt-0 mb-1">등록된 매핑 전체 목록</h6>' +
        '<p class="text-muted small mb-2">PG·채널별로 쌓인 내역입니다. <strong>편집</strong>은 위 매핑 표에서 해당 PG를 고른 뒤 [저장분 불러오기]를 사용하세요.</p>' +
        '<div class="table-responsive border rounded mb-0"><table class="table table-sm align-middle mb-0">' +
        '<thead class="table-light"><tr><th>PG코드</th><th>채널</th><th>PG 파라미터</th><th>우리 열(key)</th><th>비고</th><th style="min-width:7rem">작업</th></tr></thead>' +
        '<tbody id="hqNmSummaryTbody">' + nmBuildSummaryRowsHtml(state.root, catKeys) + '</tbody></table></div>';
      var introBlock =
        '<div class="alert alert-light border hq-nm-intro mb-3">' +
        '<div class="fw-semibold mb-2 text-body">GUI로 설정하는 순서</div>' +
        '<ol class="small mb-0 ps-3 text-body">' +
        '<li><strong>PG 목록 동기화</strong> — 배포설정 > API연동설정에 등록된 결제대행사 줄이 생깁니다.</li>' +
        '<li><strong>매핑 작업 표</strong>에서 PG·채널을 고릅니다. (선택) 실제 수신 노티에서 관찰된 파라미터 이름이 자동으로 붙습니다.</li>' +
        '<li>각 줄마다 <strong>우리 항목(열 key)</strong>을 고르고, 필요하면 <strong>우리 표시명</strong>을 고칩니다. 열 key만 고르면 표시명은 카탈로그 기본으로 자동 채웁니다. <strong>AI 잠금</strong>은 자동 제안이 그 줄을 바꾸지 못하게 합니다.</li>' +
        '<li><strong>표 내용 → 매핑 반영</strong> 후 화면 하단 <strong>저장</strong> — 표시명은 카탈로그에 반영되어 조직항목설정·결제 그리드 열 이름과 같아집니다.</li>' +
        '<li class="text-muted">채널을 바꾸면 서버에 저장된 해당 채널 매핑을 표에 불러옵니다. 표에서만 수정한 내용은 채널 변경 전에 <strong>표 내용 → 매핑 반영</strong>을 권장합니다.</li>' +
        '</ol></div>';
      var advancedCatalogHtml =
        '<h6 class="text-secondary small fw-bold mt-0 mb-1">컬럼 카탈로그 (열 정의 · JSON)</h6>' +
        '<p class="text-muted small mb-2">일반적으로 기본 삽입만 하면 됩니다. 열 구조를 직접 바꿀 때만 편집하세요.</p>' +
        '<div class="table-responsive border rounded mb-3"><table class="table table-sm align-middle mb-0">' +
        '<thead class="table-light"><tr><th style="min-width:10rem">catalogId</th><th style="min-width:10rem">표시 제목</th><th style="min-width:12rem">headerGroups JSON</th><th>columns JSON</th><th style="width:4rem">삭제</th></tr></thead>' +
        '<tbody id="hqNmCatTbody">' + (catRows || '<tr><td colspan="5" class="text-muted text-center py-2">카탈로그 없음. [기본 삽입] 또는 [추가]</td></tr>') + '</tbody></table></div>' +
        '<h6 class="text-secondary small fw-bold mt-2 mb-1">결제관리 화면별 카탈로그 연결</h6>' +
        '<div class="table-responsive border rounded mb-0"><table class="table table-sm align-middle mb-0">' +
        '<thead class="table-light"><tr><th style="width:40%">메뉴</th><th>카탈로그</th></tr></thead>' +
        '<tbody id="hqNmPageTbody">' + pageRows + '</tbody></table></div>';
      rootEl.innerHTML =
        '<div class="d-flex flex-wrap gap-2 mb-2 align-items-center">' +
        '<button type="button" class="btn btn-sm btn-primary" id="hqNmBtnSyncPg">PG 목록 동기화</button>' +
        '<button type="button" class="btn btn-sm btn-outline-primary" id="hqNmBtnDefaults">기본 카탈로그·화면연결 삽입</button>' +
        '<button type="button" class="btn btn-sm btn-outline-success" id="hqNmBtnAddCat">카탈로그 추가</button>' +
        '</div>' +
        introBlock +
        wizardCard +
        '<details class="card mb-3 hq-nm-details-advanced"><summary class="card-header py-2 fw-semibold small user-select-none">고급: 카탈로그·화면별 연결 (JSON 편집)</summary><div class="card-body border-top">' +
        advancedCatalogHtml +
        '</div></details>' +
        '<details class="card mb-3 hq-nm-details-advanced"><summary class="card-header py-2 fw-semibold small user-select-none">고급: PG별 채널 표 · 표시값(displayMaps) · 전체 매핑 목록</summary><div class="card-body border-top">' +
        '<p class="text-muted small mb-2">위 <strong>매핑 작업 표</strong>와 같은 데이터입니다. 표에서 반영한 뒤 여기서 검토하거나 드물게 직접 고칠 수 있습니다.</p>' +
        '<div id="hqNmVendorRoot">' + vendorBlocks + '</div>' +
        summaryHtml +
        '</div></details>';
      nmRestoreGuiScratch(catKeys);
      setTimeout(refreshNmAiBadge, 0);
    }
    if (!rootEl._hqNmCatalogChangeBound) {
      rootEl._hqNmCatalogChangeBound = true;
      rootEl.addEventListener('change', function (ev) {
        var t = ev.target;
        if (t && t.id === 'hqNmWizardCatalogId') pane._hqNmWizardCatalogSel = String(t.value || '').trim();
        if (t && t.classList && t.classList.contains('hq-nm-gui-ik')) {
          var trIk = t.closest('tr');
          var lbIk = trIk && trIk.querySelector('.hq-nm-gui-lbl');
          if (!lbIk || String(lbIk.value || '').trim()) return;
          var catElIk = rootEl.querySelector('#hqNmWizardCatalogId');
          var wcidIk = catElIk ? String(catElIk.value || '').trim() : '';
          var vKey = String(t.value || '').trim();
          if (!vKey || !wcidIk) return;
          if (!parseRootFromTextarea()) return;
          var lab0 = nmCatalogLabelForKey(state.root, wcidIk, vKey);
          if (lab0 && lab0 !== vKey) lbIk.value = lab0;
          return;
        }
        if (t && t.id === 'hqNmSelPg') {
          var autoL = rootEl.querySelector('#hqNmAutoLoadInbound');
          if (!autoL || !autoL.checked) return;
          var pgi = String(t.value || '').trim();
          if (!pgi || !window.PG_API || !window.PG_API.hqNotifyMappingInboundKeys) return;
          clearTimeout(rootEl._hqNmPgInboundTimer);
          rootEl._hqNmPgInboundTimer = setTimeout(function () {
            try {
              readUiIntoState();
            } catch (ePgCh) { return; }
            var chEl0 = rootEl.querySelector('#hqNmSelCh');
            var ch0 = chEl0 ? String(chEl0.value || 'CALLBACK').trim() : 'CALLBACK';
            var fms0 = [];
            (state.root.vendors || []).forEach(function (vx) {
              if (String(vx.vendorCode || '').trim() !== pgi) return;
              ensureVendorChannelsForVendor(vx).forEach(function (chob) {
                if (String(chob.channelCode || '').trim() !== ch0) return;
                (chob.fieldMappings || []).forEach(function (x) {
                  fms0.push({
                    pgField: x.pgField || '',
                    internalKey: x.internalKey || '',
                    note: x.note || '',
                    displayLabel: '',
                    lockAi: !!x.lockAi
                  });
                });
              });
            });
            window.PG_API.hqNotifyMappingInboundKeys(pgi, 200).then(function (d) {
              var keys = d && Array.isArray(d.keys) ? d.keys : [];
              var exM = {};
              fms0.forEach(function (r) { if (r.pgField) exM[r.pgField] = r; });
              var merged = fms0.slice();
              keys.forEach(function (k) {
                if (!exM[k]) merged.push({ pgField: k, internalKey: '', note: '', displayLabel: '', lockAi: false });
              });
              var seenPf = {};
              merged.forEach(function (r) { if (r.pgField) seenPf[r.pgField] = 1; });
              if (/CHILL/i.test(pgi)) {
                NM_CHILL_PRESET_PARAMS.forEach(function (pk) {
                  if (!seenPf[pk]) {
                    seenPf[pk] = 1;
                    merged.push({ pgField: pk, internalKey: '', note: '', displayLabel: '', lockAi: false });
                  }
                });
              }
              pane._hqNmGuiScratch = { pg: pgi, ch: ch0, rows: merged };
              syncStateToTextareas();
              render();
            }).catch(function () {
              var mergedE = fms0.slice();
              var seenE = {};
              mergedE.forEach(function (r) { if (r.pgField) seenE[r.pgField] = 1; });
              if (/CHILL/i.test(pgi)) {
                NM_CHILL_PRESET_PARAMS.forEach(function (pk) {
                  if (!seenE[pk]) {
                    seenE[pk] = 1;
                    mergedE.push({ pgField: pk, internalKey: '', note: '', displayLabel: '', lockAi: false });
                  }
                });
              }
              pane._hqNmGuiScratch = { pg: pgi, ch: ch0, rows: mergedE };
              syncStateToTextareas();
              render();
            });
          }, 400);
          return;
        }
        if (t && t.id === 'hqNmSelCh') {
          var pgCh = rootEl.querySelector('#hqNmSelPg');
          var pgx = pgCh ? String(pgCh.value || '').trim() : '';
          if (!pgx) return;
          try {
            readUiIntoState();
          } catch (eChSel) { return; }
          var chx = String(t.value || 'CALLBACK').trim();
          var fmsC = [];
          (state.root.vendors || []).forEach(function (vx) {
            if (String(vx.vendorCode || '').trim() !== pgx) return;
            ensureVendorChannelsForVendor(vx).forEach(function (chob) {
              if (String(chob.channelCode || '').trim() !== chx) return;
              (chob.fieldMappings || []).forEach(function (x) {
                fmsC.push({
                  pgField: x.pgField || '',
                  internalKey: x.internalKey || '',
                  note: x.note || '',
                  displayLabel: '',
                  lockAi: !!x.lockAi
                });
              });
            });
          });
          pane._hqNmGuiScratch = { pg: pgx, ch: chx, rows: fmsC };
          syncStateToTextareas();
          render();
        }
      });
    }
    if (!rootEl._hqNmClickDeleg) {
      rootEl._hqNmClickDeleg = true;
      rootEl.addEventListener('click', function (e) {
        var btn = e.target && e.target.closest ? e.target.closest('button') : null;
        if (!btn || !rootEl.contains(btn)) return;
        if (btn.id === 'hqNmGuiLoad') {
          try {
            if (!readUiIntoState()) return;
          } catch (eL0) { return; }
          var pgEl = rootEl.querySelector('#hqNmSelPg');
          var chEl = rootEl.querySelector('#hqNmSelCh');
          var pg = pgEl ? String(pgEl.value || '').trim() : '';
          var ch = chEl ? String(chEl.value || 'CALLBACK').trim() : 'CALLBACK';
          if (!pg) {
            alert('결제대행사를 선택하세요. 없으면 [PG 목록 동기화]를 실행합니다.');
            return;
          }
          var fms = [];
          (state.root.vendors || []).forEach(function (vx) {
            if (String(vx.vendorCode || '').trim() !== pg) return;
            ensureVendorChannelsForVendor(vx).forEach(function (chob) {
              if (String(chob.channelCode || '').trim() !== ch) return;
              (chob.fieldMappings || []).forEach(function (x) {
                fms.push({
                  pgField: x.pgField || '',
                  internalKey: x.internalKey || '',
                  note: x.note || '',
                  displayLabel: '',
                  lockAi: !!x.lockAi
                });
              });
            });
          });
          pane._hqNmGuiScratch = { pg: pg, ch: ch, rows: fms };
          syncStateToTextareas();
          render();
          return;
        }
        if (btn.id === 'hqNmGuiApply') {
          try {
            if (!readUiIntoState()) return;
          } catch (eAp0) { return; }
          var pgE2 = rootEl.querySelector('#hqNmSelPg');
          var chE2 = rootEl.querySelector('#hqNmSelCh');
          var tbAp = rootEl.querySelector('#hqNmGuiTbody');
          var pg2 = pgE2 ? String(pgE2.value || '').trim() : '';
          var ch2 = chE2 ? String(chE2.value || 'CALLBACK').trim() : 'CALLBACK';
          if (!pg2) {
            alert('결제대행사를 선택하세요.');
            return;
          }
          var rowsAp = nmReadGuiRowsFromTbody(rootEl.querySelector('#hqNmGuiTbody'));
          if (!rowsAp.length && !window.confirm('매핑 행이 없습니다. 이 채널의 매핑을 모두 비울까요?')) return;
          var catElApply = rootEl.querySelector('#hqNmWizardCatalogId');
          if (catElApply) pane._hqNmWizardCatalogSel = String(catElApply.value || '').trim();
          var wcidAp = pane._hqNmWizardCatalogSel || '';
          if (wcidAp) nmApplyDisplayLabelsToCatalog(state.root, wcidAp, rowsAp);
          nmApplyGuiRowsToVendor(pg2, ch2, rowsAp);
          pane._hqNmGuiScratch = { pg: pg2, ch: ch2, rows: rowsAp.slice() };
          syncStateToTextareas();
          render();
          alert('매핑이 반영되었습니다. 서버에 남기려면 화면 하단 [저장]을 누르세요.');
          return;
        }
        if (btn.id === 'hqNmGuiSuggestApi' || btn.id === 'hqNmGuiSuggestRule') {
          if (!window.PG_API || !window.PG_API.hqNotifyMappingSuggest) return;
          var useAiSuggest = btn.id === 'hqNmGuiSuggestApi';
          var pgE3 = rootEl.querySelector('#hqNmSelPg');
          var pg3 = pgE3 ? String(pgE3.value || '').trim() : '';
          if (!pg3) {
            alert('결제대행사를 선택하세요.');
            return;
          }
          var taS = rootEl.querySelector('#hqNmGuiSampleJson');
          var sampleS = taS ? String(taS.value || '').trim() : '';
          if (!sampleS) {
            alert('샘플 JSON을 입력하세요.');
            return;
          }
          var tbPreSg = rootEl.querySelector('#hqNmGuiTbody');
          var preRowsSg = nmReadGuiRowsFromTbody(tbPreSg);
          var lockedSg = preRowsSg.filter(function (r) { return r.lockAi && r.pgField && r.internalKey; }).map(function (r) {
            return { pgField: r.pgField, internalKey: r.internalKey, note: r.note || '', lockAi: true };
          });
          var prevDispSg = {};
          preRowsSg.forEach(function (r) { if (r.pgField) prevDispSg[r.pgField] = r; });
          var cidS = '';
          try {
            if (!readUiIntoState()) return;
            var catElSg = rootEl.querySelector('#hqNmWizardCatalogId');
            if (catElSg && catElSg.value) pane._hqNmWizardCatalogSel = String(catElSg.value || '').trim();
            cidS = pane._hqNmWizardCatalogSel;
            if (!cidS) {
              var c0s = (state.root.columnCatalogs || [])[0];
              cidS = c0s && c0s.catalogId ? String(c0s.catalogId).trim() : '';
            }
          } catch (eSg2) { return; }
          var dimmGs = document.getElementById('dimm');
          if (dimmGs) dimmGs.style.display = 'flex';
          window.PG_API.hqNotifyMappingSuggest({
            vendorCode: pg3,
            catalogId: cidS || 'cat_pay_integrated_default',
            sampleJson: sampleS,
            useAi: useAiSuggest,
            lockedFieldMappings: lockedSg
          }).then(function (d) {
            var fm = d && d.fieldMappings ? d.fieldMappings : [];
            var src = d && d.source != null ? String(d.source) : '';
            if (!fm.length) {
              alert('제안된 매핑이 없습니다.' + (src ? ' (출처: ' + src + ')' : ''));
              return;
            }
            var cmsg = '매핑 작업 표를 제안 결과 ' + fm.length + '건으로 채울까요? (AI잠금 행은 유지·병합됩니다)';
            if (src) cmsg += ' 출처: ' + src + '.';
            if (!window.confirm(cmsg)) return;
            pane._hqNmGuiScratch = {
              pg: pg3,
              ch: String(rootEl.querySelector('#hqNmSelCh') && rootEl.querySelector('#hqNmSelCh').value || 'CALLBACK'),
              rows: fm.map(function (x) {
                var pr = prevDispSg[x.pgField];
                return {
                  pgField: x.pgField || '',
                  internalKey: x.internalKey || '',
                  note: x.note || '',
                  displayLabel: pr && pr.displayLabel ? pr.displayLabel : '',
                  lockAi: !!x.lockAi
                };
              })
            };
            syncStateToTextareas();
            render();
          }).catch(function () { alert('자동 제안 API 호출에 실패했습니다.'); }).finally(function () { if (dimmGs) dimmGs.style.display = 'none'; });
          return;
        }
        if (btn.id === 'hqNmGuiInboundKeys') {
          var pgIk = rootEl.querySelector('#hqNmSelPg');
          var pgi = pgIk ? String(pgIk.value || '').trim() : '';
          if (!pgi) {
            alert('결제대행사를 선택하세요.');
            return;
          }
          if (!window.PG_API || !window.PG_API.hqNotifyMappingInboundKeys) return;
          var dimmIk = document.getElementById('dimm');
          if (dimmIk) dimmIk.style.display = 'flex';
          window.PG_API.hqNotifyMappingInboundKeys(pgi, 120).then(function (d) {
            var keys = d && Array.isArray(d.keys) ? d.keys : [];
            var srcIk = d && d.source != null ? String(d.source) : '';
            var nIk = d && d.inboundRowsScanned != null ? d.inboundRowsScanned : 0;
            if (!keys.length) {
              alert('수집된 키가 없습니다. 노티 적재 이력이 없거나 MID 필터에 맞는 건이 없을 수 있습니다.' + (srcIk ? ' (' + srcIk + ')' : ''));
              return;
            }
            try {
              if (!readUiIntoState()) return;
            } catch (eIk0) { return; }
            var tbIk = rootEl.querySelector('#hqNmGuiTbody');
            var exIk = {};
            nmReadGuiRowsFromTbody(tbIk).forEach(function (r) { if (r.pgField) exIk[r.pgField] = 1; });
            var newIk = nmReadGuiRowsFromTbody(tbIk);
            keys.forEach(function (k) {
              if (!exIk[k]) newIk.push({ pgField: k, internalKey: '', note: '', displayLabel: '', lockAi: false });
            });
            var chIk = rootEl.querySelector('#hqNmSelCh');
            pane._hqNmGuiScratch = {
              pg: pgi,
              ch: chIk ? String(chIk.value || 'CALLBACK').trim() : 'CALLBACK',
              rows: newIk
            };
            syncStateToTextareas();
            render();
            alert('파라미터 키 ' + keys.length + '개를 표에 추가했습니다. (스캔 노티 ' + nIk + '건, ' + srcIk + ')');
          }).catch(function () { alert('수신 노티 키 API 호출에 실패했습니다.'); }).finally(function () { if (dimmIk) dimmIk.style.display = 'none'; });
          return;
        }
        if (btn.id === 'hqNmGuiFromJson') {
          var taJ = rootEl.querySelector('#hqNmGuiSampleJson');
          var keys = nmCollectJsonKeys(taJ ? taJ.value : '');
          if (!keys.length) {
            alert('JSON에서 키를 읽을 수 없습니다.');
            return;
          }
          var tbJ = rootEl.querySelector('#hqNmGuiTbody');
          try {
            if (!readUiIntoState()) return;
          } catch (eJ0) { return; }
          var existing = {};
          nmReadGuiRowsFromTbody(tbJ).forEach(function (r) { if (r.pgField) existing[r.pgField] = 1; });
          var newRows = nmReadGuiRowsFromTbody(tbJ);
          keys.forEach(function (k) {
            if (!existing[k]) newRows.push({ pgField: k, internalKey: '', note: '', displayLabel: '', lockAi: false });
          });
          var pgEj = rootEl.querySelector('#hqNmSelPg');
          var chEj = rootEl.querySelector('#hqNmSelCh');
          var pgSel = pgEj ? String(pgEj.value || '').trim() : '';
          var chSel = chEj ? String(chEj.value || 'CALLBACK').trim() : 'CALLBACK';
          var autoCb = rootEl.querySelector('#hqNmAutoAiAfterJson');
          var doAutoMap = autoCb && autoCb.checked && pgSel && window.PG_API && window.PG_API.hqNotifyMappingSuggest;
          var cidJ = '';
          try {
            var c0j = (state.root.columnCatalogs || [])[0];
            cidJ = c0j && c0j.catalogId ? String(c0j.catalogId).trim() : '';
          } catch (eCj) { cidJ = ''; }
          var sampleJ = taJ ? String(taJ.value || '').trim() : '';
          if (doAutoMap) {
            var dimmJ = document.getElementById('dimm');
            if (dimmJ) dimmJ.style.display = 'flex';
            var lockedJ = newRows.filter(function (r) { return r.lockAi && r.pgField && r.internalKey; }).map(function (r) {
              return { pgField: r.pgField, internalKey: r.internalKey, note: r.note || '', lockAi: true };
            });
            window.PG_API.hqNotifyMappingSuggest({
              vendorCode: pgSel,
              catalogId: cidJ || 'cat_pay_integrated_default',
              sampleJson: sampleJ,
              paramNames: keys,
              useAi: true,
              lockedFieldMappings: lockedJ
            }).then(function (dj) {
              var byPf = {};
              (dj && dj.fieldMappings ? dj.fieldMappings : []).forEach(function (m) {
                if (m && m.pgField) byPf[String(m.pgField)] = m.internalKey || '';
              });
              newRows.forEach(function (r) {
                if (r.pgField && byPf[r.pgField] && !String(r.internalKey || '').trim()) {
                  r.internalKey = byPf[r.pgField];
                }
              });
              pane._hqNmGuiScratch = { pg: pgSel, ch: chSel, rows: newRows };
              syncStateToTextareas();
              render();
              var sj = dj && dj.source != null ? String(dj.source) : '';
              alert('키 목록을 반영했습니다.' + (sj ? ' 자동 매핑 출처: ' + sj + '.' : '') + ' 표를 확인한 뒤 [매핑 적용]·[저장] 하세요.');
            }).catch(function () {
              pane._hqNmGuiScratch = { pg: pgSel, ch: chSel, rows: newRows };
              syncStateToTextareas();
              render();
              alert('자동 매핑 API 호출에 실패했습니다. 키 목록만 반영했습니다.');
            }).finally(function () { if (dimmJ) dimmJ.style.display = 'none'; });
            return;
          }
          pane._hqNmGuiScratch = { pg: pgSel, ch: chSel, rows: newRows };
          syncStateToTextareas();
          render();
          return;
        }
        if (btn.id === 'hqNmGuiChillPreset') {
          try {
            if (!readUiIntoState()) return;
          } catch (eCp0) { return; }
          var pgCp = rootEl.querySelector('#hqNmSelPg');
          var pgc = pgCp ? String(pgCp.value || '').trim() : '';
          if (!pgc) {
            alert('결제대행사를 먼저 선택하세요.');
            return;
          }
          var curRows = nmReadGuiRowsFromTbody(rootEl.querySelector('#hqNmGuiTbody'));
          var ex = {};
          curRows.forEach(function (r) { if (r.pgField) ex[r.pgField] = 1; });
          NM_CHILL_PRESET_PARAMS.forEach(function (k) {
            if (!ex[k]) curRows.push({ pgField: k, internalKey: '', note: '', displayLabel: '', lockAi: false });
          });
          var chCp = rootEl.querySelector('#hqNmSelCh');
          pane._hqNmGuiScratch = { pg: pgc, ch: chCp ? String(chCp.value || 'CALLBACK').trim() : 'CALLBACK', rows: curRows };
          syncStateToTextareas();
          render();
          return;
        }
        if (btn.id === 'hqNmGuiRowAdd') {
          try {
            if (!readUiIntoState()) return;
          } catch (eRa0) { return; }
          var pgRa = rootEl.querySelector('#hqNmSelPg');
          var chRa = rootEl.querySelector('#hqNmSelCh');
          var curR = nmReadGuiRowsFromTbody(rootEl.querySelector('#hqNmGuiTbody'));
          curR.push({ pgField: '', internalKey: '', note: '', displayLabel: '', lockAi: false });
          pane._hqNmGuiScratch = {
            pg: pgRa ? String(pgRa.value || '').trim() : '',
            ch: chRa ? String(chRa.value || 'CALLBACK').trim() : 'CALLBACK',
            rows: curR
          };
          syncStateToTextareas();
          render();
          return;
        }
        if (btn.classList.contains('hq-nm-gui-row-del')) {
          var trG = btn.closest('tr');
          var tbG = trG && trG.parentElement;
          if (!tbG || !trG.classList.contains('hq-nm-gui-row')) return;
          try {
            if (!readUiIntoState()) return;
          } catch (eGrd) { return; }
          var pgGd = rootEl.querySelector('#hqNmSelPg');
          var chGd = rootEl.querySelector('#hqNmSelCh');
          var curG = [];
          tbG.querySelectorAll('.hq-nm-gui-row').forEach(function (tr) {
            if (tr === trG) return;
            var pf = tr.querySelector('.hq-nm-gui-pf');
            var ik = tr.querySelector('.hq-nm-gui-ik');
            var nt = tr.querySelector('.hq-nm-gui-note');
            var lb = tr.querySelector('.hq-nm-gui-lbl');
            var lk = tr.querySelector('.hq-nm-gui-lock');
            curG.push({
              pgField: pf ? String(pf.value || '').trim() : '',
              internalKey: ik ? String(ik.value || '').trim() : '',
              note: nt ? String(nt.value || '').trim() : '',
              displayLabel: lb ? String(lb.value || '').trim() : '',
              lockAi: !!(lk && lk.checked)
            });
          });
          pane._hqNmGuiScratch = {
            pg: pgGd ? String(pgGd.value || '').trim() : '',
            ch: chGd ? String(chGd.value || 'CALLBACK').trim() : 'CALLBACK',
            rows: curG
          };
          syncStateToTextareas();
          render();
          return;
        }
        if (btn.classList.contains('hq-nm-sum-edit')) {
          var trSe = btn.closest('tr');
          if (!trSe || !trSe.classList.contains('hq-nm-sum-row')) return;
          try {
            if (!readUiIntoState()) return;
          } catch (eSe) { return; }
          var vcdE = String(trSe.getAttribute('data-sum-v') || '').trim();
          var ccdE = String(trSe.getAttribute('data-sum-c') || '').trim();
          var fmsE = [];
          (state.root.vendors || []).forEach(function (vx) {
            if (String(vx.vendorCode || '').trim() !== vcdE) return;
            ensureVendorChannelsForVendor(vx).forEach(function (chx) {
              if (String(chx.channelCode || '').trim() !== ccdE) return;
              (chx.fieldMappings || []).forEach(function (x) {
                fmsE.push({
                  pgField: x.pgField || '',
                  internalKey: x.internalKey || '',
                  note: x.note || '',
                  displayLabel: '',
                  lockAi: !!x.lockAi
                });
              });
            });
          });
          pane._hqNmGuiScratch = { pg: vcdE, ch: ccdE, rows: fmsE };
          syncStateToTextareas();
          render();
          var wz = rootEl.querySelector('#hqNmWizardCard');
          if (wz && wz.scrollIntoView) wz.scrollIntoView({ behavior: 'smooth', block: 'start' });
          return;
        }
        if (btn.classList.contains('hq-nm-sum-del')) {
          var trSd = btn.closest('tr');
          if (!trSd || !trSd.classList.contains('hq-nm-sum-row')) return;
          try {
            if (!readUiIntoState()) return;
          } catch (eSd) { return; }
          var vcdD = String(trSd.getAttribute('data-sum-v') || '').trim();
          var ccdD = String(trSd.getAttribute('data-sum-c') || '').trim();
          var idxD = parseInt(trSd.getAttribute('data-sum-i') || '-1', 10);
          if (idxD < 0) return;
          (state.root.vendors || []).forEach(function (vx) {
            if (String(vx.vendorCode || '').trim() !== vcdD) return;
            var chsD = ensureVendorChannelsForVendor(vx);
            chsD.forEach(function (chd) {
              if (String(chd.channelCode || '').trim() !== ccdD) return;
              if (Array.isArray(chd.fieldMappings) && idxD < chd.fieldMappings.length) chd.fieldMappings.splice(idxD, 1);
            });
            vx.channels = chsD;
          });
          pane._hqNmGuiScratch = null;
          syncStateToTextareas();
          render();
          return;
        }
        if (btn.id === 'hqNmBtnDefaults') {
          if (!window.PG_API || !window.PG_API.hqNotifyMappingDefaults) return;
          var dimmD = document.getElementById('dimm');
          if (dimmD) dimmD.style.display = 'flex';
          window.PG_API.hqNotifyMappingDefaults().then(function (d) {
            if (!d) return;
            try { readUiIntoState(); } catch (eDef1) { return; }
            if (window.confirm('기본 columnCatalogs·pageCatalogAssignments 를 덮어씁니다. vendors 는 유지됩니다. 계속할까요?')) {
              state.root.columnCatalogs = Array.isArray(d.columnCatalogs) ? JSON.parse(JSON.stringify(d.columnCatalogs)) : state.root.columnCatalogs;
              state.root.pageCatalogAssignments = Array.isArray(d.pageCatalogAssignments) ? JSON.parse(JSON.stringify(d.pageCatalogAssignments)) : state.root.pageCatalogAssignments;
              syncStateToTextareas();
              render();
            }
          }).catch(function () { alert('기본값을 불러오지 못했습니다.'); }).finally(function () { if (dimmD) dimmD.style.display = 'none'; });
          return;
        }
        if (btn.id === 'hqNmBtnSyncPg') {
          if (!window.PG_API || !window.PG_API.pgAgencyList) return;
          var dimmP = document.getElementById('dimm');
          if (dimmP) dimmP.style.display = 'flex';
          window.PG_API.pgAgencyList().then(function (list) {
            try { readUiIntoState(); } catch (ePg1) { return; }
            var byCode = {};
            (state.root.vendors || []).forEach(function (v) {
              if (v && v.vendorCode) byCode[String(v.vendorCode).trim()] = v;
            });
            (list || []).forEach(function (ag) {
              var cd = String(ag.pgCd || '').trim();
              if (!cd) return;
              if (!byCode[cd]) {
                byCode[cd] = {
                  vendorCode: cd,
                  vendorName: String(ag.pgNm || cd).trim(),
                  channels: defaultChannels(),
                  displayMaps: {}
                };
              }
            });
            state.root.vendors = Object.keys(byCode).sort().map(function (k) { return byCode[k]; });
            syncStateToTextareas();
            render();
          }).catch(function () { alert('PG 목록을 불러오지 못했습니다.'); }).finally(function () { if (dimmP) dimmP.style.display = 'none'; });
          return;
        }
        if (btn.id === 'hqNmBtnAddCat') {
          try { readUiIntoState(); } catch (e5) { return; }
          state.root.columnCatalogs.push({
            catalogId: 'cat_' + Date.now().toString(36),
            displayTitle: '새 카탈로그',
            headerGroups: [],
            columns: []
          });
          syncStateToTextareas();
          render();
          return;
        }
        if (btn.classList.contains('hq-nm-cat-del')) {
          var tr = btn.closest('tr');
          var idx = tr ? parseInt(tr.getAttribute('data-cat-idx') || '-1', 10) : -1;
          try { readUiIntoState(); } catch (e6) { return; }
          if (idx >= 0) state.root.columnCatalogs.splice(idx, 1);
          syncStateToTextareas();
          render();
        }
        if (btn.classList.contains('hq-nm-fm-add')) {
          var vcd = String(btn.getAttribute('data-vendor-code') || '').trim();
          var ccd = String(btn.getAttribute('data-ch-code') || '').trim();
          try { readUiIntoState(); } catch (eFa) { return; }
          (state.root.vendors || []).forEach(function (vx) {
            if (String(vx.vendorCode || '').trim() !== vcd) return;
            var chs = ensureVendorChannelsForVendor(vx);
            chs.forEach(function (ch) {
              if (String(ch.channelCode || '').trim() !== ccd) return;
              if (!Array.isArray(ch.fieldMappings)) ch.fieldMappings = [];
              ch.fieldMappings.push({ pgField: '', internalKey: '', note: '', lockAi: false });
            });
            vx.channels = chs;
          });
          syncStateToTextareas();
          render();
        }
        if (btn.classList.contains('hq-nm-fm-del')) {
          var row = btn.closest('tr');
          var tbody = row && row.parentElement;
          try { readUiIntoState(); } catch (eFd) { return; }
          if (tbody && row && row.classList.contains('hq-nm-fm-row')) {
            var chBlock = tbody.closest('.hq-nm-ch-block');
            var vBlock = tbody.closest('.hq-nm-vendor-block');
            var vcd2 = vBlock ? String(vBlock.getAttribute('data-vendor-code') || '').trim() : '';
            var ccd2 = chBlock ? String(chBlock.getAttribute('data-ch-code') || '').trim() : '';
            var idxFm = Array.prototype.indexOf.call(tbody.querySelectorAll('.hq-nm-fm-row'), row);
            (state.root.vendors || []).forEach(function (vx) {
              if (String(vx.vendorCode || '').trim() !== vcd2) return;
              var chs2 = ensureVendorChannelsForVendor(vx);
              chs2.forEach(function (ch) {
                if (String(ch.channelCode || '').trim() !== ccd2) return;
                if (idxFm >= 0 && Array.isArray(ch.fieldMappings)) ch.fieldMappings.splice(idxFm, 1);
              });
              vx.channels = chs2;
            });
          }
          syncStateToTextareas();
          render();
        }
        if (btn.classList.contains('hq-nm-suggest-btn')) {
          if (!window.PG_API || !window.PG_API.hqNotifyMappingSuggest) return;
          var vcd3 = String(btn.getAttribute('data-vendor-code') || '').trim();
          var chWrap = btn.closest('.hq-nm-ch-block');
          var ta = chWrap ? chWrap.querySelector('.hq-nm-sample-json') : null;
          var sample = ta ? String(ta.value || '').trim() : '';
          if (!sample) {
            alert('CALLBACK 샘플 JSON을 붙여 넣은 뒤 다시 시도하세요.');
            return;
          }
          var lockedCh = [];
          if (chWrap) {
            chWrap.querySelectorAll('.hq-nm-fm-row').forEach(function (tr) {
              var pf = tr.querySelector('.hq-nm-pg-field');
              var ik = tr.querySelector('.hq-nm-internal-key');
              var nt = tr.querySelector('.hq-nm-note');
              var lk = tr.querySelector('.hq-nm-lock-ai');
              if (!(lk && lk.checked) || !pf || !ik) return;
              var pfv = String(pf.value || '').trim();
              var ikv = String(ik.value || '').trim();
              if (pfv && ikv) lockedCh.push({ pgField: pfv, internalKey: ikv, note: nt ? String(nt.value || '').trim() : '', lockAi: true });
            });
          }
          var cid0 = '';
          try {
            readUiIntoState();
            var c0 = (state.root.columnCatalogs || [])[0];
            cid0 = c0 && c0.catalogId ? String(c0.catalogId).trim() : '';
          } catch (eSg0) { return; }
          var dimmS = document.getElementById('dimm');
          if (dimmS) dimmS.style.display = 'flex';
          window.PG_API.hqNotifyMappingSuggest({
            vendorCode: vcd3,
            catalogId: cid0 || 'cat_pay_integrated_default',
            sampleJson: sample,
            useAi: true,
            lockedFieldMappings: lockedCh
          }).then(function (d) {
            var fm = d && d.fieldMappings ? d.fieldMappings : [];
            if (!fm.length) {
              alert('제안된 매핑이 없습니다. JSON 키·카탈로그 열을 확인하세요.');
              return;
            }
            if (!window.confirm('이 CALLBACK 채널의 매핑을 제안 결과 ' + fm.length + '건으로 덮어씁니다. 계속할까요?')) return;
            try { readUiIntoState(); } catch (eSg1) { return; }
            (state.root.vendors || []).forEach(function (vx) {
              if (String(vx.vendorCode || '').trim() !== vcd3) return;
              var chs3 = ensureVendorChannelsForVendor(vx);
              chs3.forEach(function (ch) {
                if (String(ch.channelCode || '').trim() !== 'CALLBACK') return;
                ch.fieldMappings = fm.map(function (x) {
                  var o = { pgField: x.pgField || '', internalKey: x.internalKey || '', note: x.note || '' };
                  if (x.lockAi) o.lockAi = true;
                  return o;
                });
              });
              vx.channels = chs3;
            });
            syncStateToTextareas();
            render();
          }).catch(function () { alert('자동 제안 API 호출에 실패했습니다.'); }).finally(function () { if (dimmS) dimmS.style.display = 'none'; });
        }
      });
    }
    var tgl = pane.querySelector('#hqNotifyMappingToggleJsonBtn');
    if (tgl && !tgl._hqNmBound) {
      tgl._hqNmBound = true;
      tgl.addEventListener('click', function () {
        if (!jsonWrap || !visTa) return;
        var on = jsonWrap.classList.toggle('d-none') === false;
        tgl.textContent = on ? '전문가용: JSON 편집 닫기' : '전문가용: JSON 직접 편집';
        if (on) {
          try {
            if (!readUiIntoState()) { jsonWrap.classList.add('d-none'); return; }
          } catch (e7) { jsonWrap.classList.add('d-none'); return; }
          syncStateToTextareas();
          visTa.value = JSON.stringify(state.root, null, 2);
          visTa.oninput = function () {
            hidTa.value = visTa.value;
          };
        }
      });
    }
    pane._hqNotifyMappingReadUi = function () {
      try {
        if (!readUiIntoState()) return false;
        applyGuiTableToSelectedVendorOnSave();
        syncStateToTextareas();
        return true;
      } catch (eRw) {
        return false;
      }
    };
    render();
  }

  function bindScreenEvents(pane, tabId) {
    if (!pane) return;
    var initFormUrl = pane.getAttribute('formurl') || '';
    if (initFormUrl === '/comp/compMngTree' && window.PG_SCREENS && typeof window.PG_SCREENS.getCompMngSearchCompDivOptions === 'function') {
      var selCompDiv = pane.querySelector('select[name="searchCompDiv"]');
      if (selCompDiv) {
        var uSearch = {};
        try { uSearch = JSON.parse(sessionStorage.getItem('pg_admin_user') || '{}'); } catch (e1) { uSearch = {}; }
        var isAdminSearch = String(uSearch.role || '').toUpperCase() === 'ADMIN';
        var divOpts = window.PG_SCREENS.getCompMngSearchCompDivOptions(uSearch.orgLevel, isAdminSearch);
        var prevDiv = selCompDiv.value;
        var escOpt = function (s) { return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;'); };
        selCompDiv.innerHTML = divOpts.map(function (o) { return '<option value="' + escOpt(o.v) + '">' + escOpt(o.t) + '</option>'; }).join('');
        var keepPrev = false;
        for (var di = 0; di < selCompDiv.options.length; di++) {
          if (selCompDiv.options[di].value === prevDiv) { keepPrev = true; break; }
        }
        selCompDiv.value = keepPrev ? prevDiv : '';
      }
    }
    var payListSearchUrls = ['/calc/payList', '/calc/chillPayTrList', '/calc/chillPaySettlementList', '/calc/payNotiList', '/calc/paySuccessList', '/calc/payFailList', '/calc/payRefundList', '/calc/payForceRefundList', '/calc/payCancelList', '/calc/payVoidList', '/calc/offsetCancList', '/pay/easyPay', '/pay/chatbotPay'];
    /** 결제관리 메뉴: 순서(내림·오름차순) 클릭 시 곧바로 doSearch (통합정산 등 정산 쪽 URL은 제외) */
    var payMgmtSortDirAutoRefreshUrls = [
      '/calc/chillPayTrList',
      '/calc/payList', '/calc/payNotiList', '/calc/paySuccessList', '/calc/payFailList',
      '/calc/payRefundList', '/calc/payForceRefundList', '/calc/payCancelList', '/calc/payVoidList',
      '/calc/offsetCancList', '/pay/easyPay', '/pay/chatbotPay'
    ];
    /** 정산관리 그리드: collectSearchParams·행번호 등에서 기본 size 25 (결제관리 통합·분류 화면은 payListSearchUrls → 50) */
    var settlementListDefaultPageSize25Urls = [
      '/calc/chillPaySettlementList',
      '/calc/calcList', '/settlement/distributionList',
      '/calc/calcGmList', '/settlement/franchiseList',
      '/calc/paySettlementHoldList', '/settlement/paySettlementHoldList',
      '/calc/feeList', '/settlement/feeList',
      '/calc/compPointMngList', '/settlement/recallMng',
      '/calc/exCalcList', '/settlement/execute',
      '/calc/settlementReport', '/settlement/settlementReport',
      '/calc/collateralList', '/settlement/collateralList',
      '/pay/payHoldList', '/settlement/holdList'
    ];
    function defaultListPageSizeForUrl(url) {
      if (payListSearchUrls.indexOf(url) !== -1) return 50;
      if (settlementListDefaultPageSize25Urls.indexOf(url) !== -1) return 25;
      return 20;
    }
    function ensurePayListDefaultSearchDates(pane) {
      if (!pane) return;
      var fromEl = pane.querySelector('#searchFromDate');
      var toEl = pane.querySelector('#searchToDate');
      if (!fromEl || !toEl) return;
      if (String(fromEl.value || '').trim() || String(toEl.value || '').trim()) return;
      var d = new Date();
      var y = d.getFullYear();
      var m = String(d.getMonth() + 1).padStart(2, '0');
      var day = String(d.getDate()).padStart(2, '0');
      var s = y + '-' + m + '-' + day;
      fromEl.value = s;
      toEl.value = s;
    }
    function fmtYmd(d) {
      return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
    }
    /** 통합내역: 빈 날짜 → 전산설정 「최근 동기화 범위」일 */
    function ensureChillPayTrListDefaultSearchDates(pane, done) {
      if (!pane) {
        if (typeof done === 'function') done();
        return;
      }
      var fromEl = pane.querySelector('#searchFromDate');
      var toEl = pane.querySelector('#searchToDate');
      if (!fromEl || !toEl) {
        if (typeof done === 'function') done();
        return;
      }
      if (String(fromEl.value || '').trim() || String(toEl.value || '').trim()) {
        if (typeof done === 'function') done();
        return;
      }
      function applyRecent(rd) {
        var n = Math.max(1, rd);
        var toD = new Date();
        var fromD = new Date();
        fromD.setDate(toD.getDate() - (n - 1));
        fromEl.value = fmtYmd(fromD);
        toEl.value = fmtYmd(toD);
      }
      if (window.__pgChillPaySearchDefaults && window.__pgChillPaySearchDefaults.recentDays) {
        applyRecent(window.__pgChillPaySearchDefaults.recentDays);
        if (typeof done === 'function') done();
        return;
      }
      if (!window.PG_API || !window.PG_API.hqLedgerSysSettings) {
        applyRecent(2);
        if (typeof done === 'function') done();
        return;
      }
      window.PG_API.hqLedgerSysSettings().then(function (res) {
        var d = res && res.data ? res.data : res;
        var rd = parseInt(String(d && d.chillpayTrRecentSyncDays != null ? d.chillpayTrRecentSyncDays : '2'), 10);
        var im = parseInt(String(d && d.chillpayTrInitSyncMonths != null ? d.chillpayTrInitSyncMonths : '3'), 10);
        if (isNaN(rd) || rd < 1) rd = 2;
        if (isNaN(im) || im < 1) im = 3;
        window.__pgChillPaySearchDefaults = { recentDays: rd, initMonths: im };
        applyRecent(rd);
      }).catch(function () {
        window.__pgChillPaySearchDefaults = { recentDays: 2, initMonths: 3 };
        applyRecent(2);
      }).finally(function () {
        if (typeof done === 'function') done();
      });
    }
    /** 통합내역 [검색 초기화]: 피지거래내역 초기화 동기화(개월)만큼 넓은 from~to 로 맞춘 뒤 콜백 */
    function applyChillPayTrListInitMonthSearchRange(pane, done) {
      if (!pane) {
        if (typeof done === 'function') done();
        return;
      }
      var fromEl = pane.querySelector('#searchFromDate');
      var toEl = pane.querySelector('#searchToDate');
      if (!fromEl || !toEl) {
        if (typeof done === 'function') done();
        return;
      }
      function applyMonths(m) {
        var months = Math.max(1, parseInt(String(m), 10) || 3);
        var toD = new Date();
        var fromD = new Date();
        fromD.setMonth(fromD.getMonth() - months);
        fromEl.value = fmtYmd(fromD);
        toEl.value = fmtYmd(toD);
        if (typeof done === 'function') done();
      }
      if (window.__pgChillPaySearchDefaults && window.__pgChillPaySearchDefaults.initMonths != null) {
        applyMonths(window.__pgChillPaySearchDefaults.initMonths);
        return;
      }
      if (!window.PG_API || !window.PG_API.hqLedgerSysSettings) {
        applyMonths(3);
        return;
      }
      window.PG_API.hqLedgerSysSettings().then(function (res) {
        var d = res && res.data ? res.data : res;
        var im = parseInt(String(d && d.chillpayTrInitSyncMonths != null ? d.chillpayTrInitSyncMonths : '3'), 10);
        var rd = parseInt(String(d && d.chillpayTrRecentSyncDays != null ? d.chillpayTrRecentSyncDays : '2'), 10);
        if (isNaN(im) || im < 1) im = 3;
        if (isNaN(rd) || rd < 1) rd = 2;
        window.__pgChillPaySearchDefaults = window.__pgChillPaySearchDefaults || {};
        window.__pgChillPaySearchDefaults.initMonths = im;
        window.__pgChillPaySearchDefaults.recentDays = rd;
        applyMonths(im);
      }).catch(function () {
        window.__pgChillPaySearchDefaults = window.__pgChillPaySearchDefaults || {};
        window.__pgChillPaySearchDefaults.initMonths = 3;
        applyMonths(3);
      });
    }
    if (payListSearchUrls.indexOf(initFormUrl) !== -1) {
      var pgCdSel = pane.querySelector('select[name="searchPgCd"]');
      if (pgCdSel && window.PG_API && typeof window.PG_API.pgAgencyList === 'function') {
        window.PG_API.pgAgencyList().then(function (list) {
          var keepPg = pgCdSel.value;
          function escPgList(s) { return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;'); }
          var optH = '<option value="">' + escPgList('전체') + '</option>';
          (list || []).forEach(function (ag) {
            var cd = String(ag.pgCd || '').trim();
            if (!cd) return;
            var nm = String(ag.pgNm || cd).trim();
            optH += '<option value="' + escPgList(cd) + '">' + escPgList(nm + ' (' + cd + ')') + '</option>';
          });
          pgCdSel.innerHTML = optH;
          var keepOk = false;
          for (var pi = 0; pi < pgCdSel.options.length; pi++) {
            if (pgCdSel.options[pi].value === keepPg) { keepOk = true; break; }
          }
          if (keepOk) pgCdSel.value = keepPg;
        }).catch(function () {});
      }
    }
    if (pane.classList) {
      pane.classList.toggle('screen-distribution-list', initFormUrl === '/calc/calcList' || initFormUrl === '/settlement/distributionList');
    }
    function syncColumnGuideUiState() {
      pane.querySelectorAll('.column-guide-item').forEach(function (item) {
        var cb = item.querySelector('.column-guide-check');
        var on = !!(cb && cb.checked);
        item.classList.toggle('column-guide-item--on', on);
        item.classList.toggle('column-guide-item--off', !on);
      });
    }
    function reorderColumnGuideListByKeys(keys) {
      var listEl = pane.querySelector('.column-guide-list');
      if (!listEl || !Array.isArray(keys) || !keys.length) return;
      var byKey = {};
      listEl.querySelectorAll('.column-guide-item').forEach(function (item) {
        var cb = item.querySelector('.column-guide-check');
        var k = cb ? cb.getAttribute('data-key') : '';
        if (k) byKey[k] = item;
      });
      keys.forEach(function (k) {
        if (byKey[k]) listEl.appendChild(byKey[k]);
      });
    }
    function rememberColumnGuideDefaultOrder() {
      if (pane._columnGuideDefaultOrder && pane._columnGuideDefaultOrder.length) return;
      var listEl = pane.querySelector('.column-guide-list');
      if (!listEl) return;
      var keys = [];
      listEl.querySelectorAll('.column-guide-check').forEach(function (cb) {
        if (cb.disabled) return;
        var k = cb.getAttribute('data-key');
        if (k) keys.push(k);
      });
      pane._columnGuideDefaultOrder = keys;
    }
    function resetColumnGuideToDefault() {
      rememberColumnGuideDefaultOrder();
      var listEl = pane.querySelector('.column-guide-list');
      if (!listEl) return;
      var order = pane._columnGuideDefaultOrder || [];
      if (order.length) reorderColumnGuideListByKeys(order);
      var payDef = resolvePayListUserViewDefaultKeys(url, pane);
      if (payDef != null) {
        if (payDef.length) applySelectedGuideKeys(payDef);
        else applySelectedGuideKeys([]);
        return;
      }
      pane.querySelectorAll('.column-guide-check').forEach(function (cb) {
        if (!cb.disabled) cb.checked = true;
      });
      pane._selectedColumns = null;
      syncColumnGuideUiState();
    }
    function bindColumnGuideDrag() {
      if (pane._columnGuideDragBound) return;
      pane._columnGuideDragBound = true;
      pane.addEventListener('dragstart', function (e) {
        var item = e.target && e.target.closest ? e.target.closest('.column-guide-item') : null;
        if (!item || !pane.contains(item)) return;
        if (item.classList.contains('column-guide-item--fixed')) {
          if (e.preventDefault) e.preventDefault();
          return;
        }
        pane._dragGuideItem = item;
        item.classList.add('column-guide-item--dragging');
        if (e.dataTransfer) {
          e.dataTransfer.effectAllowed = 'move';
          e.dataTransfer.setData('text/plain', 'drag');
        }
      });
      pane.addEventListener('dragend', function (e) {
        var item = e.target && e.target.closest ? e.target.closest('.column-guide-item') : null;
        if (item) item.classList.remove('column-guide-item--dragging');
        pane._dragGuideItem = null;
      });
      pane.addEventListener('dragover', function (e) {
        var target = e.target && e.target.closest ? e.target.closest('.column-guide-item') : null;
        var dragItem = pane._dragGuideItem;
        if (!target || !dragItem || target === dragItem) return;
        e.preventDefault();
        var rect = target.getBoundingClientRect();
        var before = e.clientY < rect.top + rect.height / 2;
        if (before) target.parentNode.insertBefore(dragItem, target);
        else target.parentNode.insertBefore(dragItem, target.nextSibling);
      });
      pane.addEventListener('mousedown', function (e) {
        var item = e.target && e.target.closest ? e.target.closest('.column-guide-item') : null;
        if (!item || !pane.contains(item)) return;
        if (item.classList.contains('column-guide-item--fixed')) return;
        item.setAttribute('draggable', 'true');
      });
    }
    function getSelectedGuideKeys() {
      var keys = [];
      pane.querySelectorAll('.column-guide-check:checked').forEach(function (cb) {
        if (cb.disabled) return;
        var k = cb.getAttribute('data-key');
        if (k) keys.push(k);
      });
      return keys;
    }
    function getVisibleGuideKeys() {
      var keys = [];
      pane.querySelectorAll('.column-guide-item').forEach(function (item) {
        if (item.style && item.style.display === 'none') return;
        var cb = item.querySelector('.column-guide-check');
        if (cb && cb.disabled) return;
        var k = cb ? cb.getAttribute('data-key') : '';
        if (k) keys.push(k);
      });
      return keys;
    }
    function mergeCustomViewColumnsIntoGuide(customList) {
      var listEl = pane.querySelector('#tableColumnGuide .column-guide-list');
      if (!listEl || !customList || !customList.length) return;
      var existing = {};
      listEl.querySelectorAll('.column-guide-check').forEach(function (cb) {
        var kk = cb.getAttribute('data-key');
        if (kk) existing[kk] = 1;
      });
      function escG(s) {
        return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
      }
      customList.forEach(function (cc) {
        var k = cc.columnKey || cc.column_key;
        var lab = cc.displayName || cc.display_name || k;
        if (!k || existing[k]) return;
        existing[k] = 1;
        var lbl = document.createElement('label');
        lbl.className = 'column-guide-item column-guide-item--on';
        lbl.innerHTML = '<input type="checkbox" class="column-guide-check" data-key="' + escG(k) + '" data-hq-custom="1"> <span class="column-guide-label">' + escG(lab) + '</span>';
        listEl.appendChild(lbl);
      });
    }
    function applySelectedGuideKeys(keys) {
      var set = {};
      (keys || []).forEach(function (k) { set[k] = 1; });
      var hasAny = (keys || []).length > 0;
      if (hasAny) reorderColumnGuideListByKeys(keys);
      pane.querySelectorAll('.column-guide-check').forEach(function (cb) {
        if (cb.disabled) return;
        var k = cb.getAttribute('data-key') || '';
        if (hasAny) {
          cb.checked = !!set[k];
        } else {
          cb.checked = false;
        }
      });
      pane._selectedColumns = hasAny ? (keys || []) : null;
      syncColumnGuideUiState();
    }
    /** VIEW SETTING: [복원]용 — 직전 서버 저장 상태(또는 불러오기 직후 상태) */
    function captureViewSettingLastSavedSnapshot() {
      if (!pane.querySelector('.column-guide-check')) return;
      pane._viewSettingLastSavedKeys = getSelectedGuideKeys().slice();
    }
    function loadViewSetting() {
      var hasGuide = !!pane.querySelector('.column-guide-check');
      pane._columnAllowanceRestricted = false;
      pane._allowedColumnKeys = null;
      if (!hasGuide || !window.PG_API || !window.PG_API.userViewSetting) return Promise.resolve();
      return window.PG_API.userViewSetting(url).then(function (data) {
        var customList = data && data.customViewColumns ? data.customViewColumns : [];
        pane._viewCustomColumnDefs = customList;
        mergeCustomViewColumnsIntoGuide(customList);
        var restricted = data && data.columnAllowanceRestricted === true;
        var allowedRaw = data && data.allowedKeysJson != null ? data.allowedKeysJson : null;
        var allowedList = null;
        if (restricted && allowedRaw != null) {
          try { allowedList = JSON.parse(String(allowedRaw)); } catch (e1) { allowedList = []; }
          if (!Array.isArray(allowedList)) allowedList = [];
        }
        pane._columnAllowanceRestricted = !!restricted;
        pane._allowedColumnKeys = (restricted && allowedList) ? allowedList : null;

        if (pane._columnAllowanceRestricted && pane._allowedColumnKeys) {
          pane.querySelectorAll('.column-guide-check').forEach(function (cb) {
            var item = cb.closest('.column-guide-item');
            if (item && item.classList.contains('column-guide-item--fixed')) {
              if (item) item.style.display = '';
              return;
            }
            var k = cb.getAttribute('data-key') || '';
            var ok = pane._allowedColumnKeys.indexOf(k) !== -1;
            if (item) item.style.display = ok ? '' : 'none';
          });
        } else {
          pane.querySelectorAll('.column-guide-item').forEach(function (item) { item.style.display = ''; });
        }

        if (!data || data.hasSetting !== true) {
          var payDef0 = resolvePayListUserViewDefaultKeys(url, pane);
          if (payDef0 != null) {
            if (pane._columnAllowanceRestricted && pane._allowedColumnKeys && pane._allowedColumnKeys.length) {
              payDef0 = payDef0.filter(function (k) { return pane._allowedColumnKeys.indexOf(k) !== -1; });
              if (!payDef0.length) payDef0 = pane._allowedColumnKeys.slice();
            }
            if (payDef0.length) applySelectedGuideKeys(payDef0);
            else applySelectedGuideKeys(getVisibleGuideKeys());
            captureViewSettingLastSavedSnapshot();
            return;
          }
          if (pane._columnAllowanceRestricted && pane._allowedColumnKeys && pane._allowedColumnKeys.length) {
            applySelectedGuideKeys(pane._allowedColumnKeys.slice());
          } else {
            applySelectedGuideKeys(getVisibleGuideKeys());
          }
          captureViewSettingLastSavedSnapshot();
          return;
        }
        var json = data && data.selectedKeysJson ? String(data.selectedKeysJson) : '[]';
        var keys = [];
        try { keys = JSON.parse(json); } catch (e2) { keys = []; }
        if (!Array.isArray(keys)) keys = [];
        applySelectedGuideKeys(keys);
        captureViewSettingLastSavedSnapshot();
      }).catch(function () {
        syncColumnGuideUiState();
        captureViewSettingLastSavedSnapshot();
      });
    }
    if (!pane._pgGridCheckboxDelegated) {
      pane._pgGridCheckboxDelegated = true;
      pane.addEventListener('change', function (e) {
        var t = e.target;
        if (!t || !t.classList) return;
        if (t.classList.contains('grid-check-all')) {
          var table = t.closest('table');
          if (!table) return;
          var on = !!t.checked;
          table.querySelectorAll('tbody .grid-row-check').forEach(function (cb) { cb.checked = on; });
          t.indeterminate = false;
          return;
        }
        if (t.classList.contains('grid-row-check')) {
          var table2 = t.closest('table');
          if (!table2) return;
          var all = table2.querySelectorAll('tbody .grid-row-check');
          var n = all.length;
          var checked = table2.querySelectorAll('tbody .grid-row-check:checked').length;
          var master = table2.querySelector('thead .grid-check-all');
          if (master && n) {
            master.checked = checked === n;
            master.indeterminate = checked > 0 && checked < n;
          }
        }
      });
    }
    if (!pane._pgExcelDownloadDelegated) {
      pane._pgExcelDownloadDelegated = true;
      pane.addEventListener('click', function (e) {
        var btn = e.target && e.target.closest ? e.target.closest('#excelBtn, #excelDownBtn') : null;
        if (!btn) return;
        e.preventDefault();
        downloadGridExcelCsv(pane);
      });
    }
    if (!pane._pgPrintDelegated) {
      pane._pgPrintDelegated = true;
      pane.addEventListener('click', function (e) {
        var pb = e.target && e.target.closest ? e.target.closest('#printBtn') : null;
        if (!pb) return;
        e.preventDefault();
        try { window.print(); } catch (err) {}
      });
    }
    var today = new Date();
    function fmt(d) { return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); }
    pane.querySelectorAll('.quick-date').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var range = this.getAttribute('data-range');
        var from = new Date(today);
        var to = new Date(today);
        if (range === 'day' || range === 'month') { from.setHours(0, 0, 0, 0); to.setHours(23, 59, 59, 999); }
        if (range === 'prevDay') { from.setDate(from.getDate() - 1); to.setTime(from.getTime()); from.setHours(0, 0, 0, 0); to.setHours(23, 59, 59, 999); }
        if (range === 'prevMonth') { from.setMonth(from.getMonth() - 1); from.setDate(1); to = new Date(from.getFullYear(), from.getMonth() + 1, 0); }
        if (range === 'month') { from.setDate(1); to = new Date(from.getFullYear(), from.getMonth() + 1, 0); }
        if (range === 'week') {
          from.setDate(from.getDate() - 6);
          from.setHours(0, 0, 0, 0);
          to.setHours(23, 59, 59, 999);
        }
        if (range === 'week2') {
          from.setDate(from.getDate() - 13);
          from.setHours(0, 0, 0, 0);
          to.setHours(23, 59, 59, 999);
        }
        /** 금주: 이번 주 월~일, 전주: 직전 주 월~일 */
        if (range === 'weekCal') {
          var wd = today.getDay();
          var toMon = wd === 0 ? -6 : 1 - wd;
          from = new Date(today);
          from.setDate(from.getDate() + toMon);
          from.setHours(0, 0, 0, 0);
          to = new Date(from);
          to.setDate(to.getDate() + 6);
          to.setHours(23, 59, 59, 999);
        }
        if (range === 'prevWeekCal') {
          var wd2 = today.getDay();
          var toMon2 = wd2 === 0 ? -6 : 1 - wd2;
          from = new Date(today);
          from.setDate(from.getDate() + toMon2 - 7);
          from.setHours(0, 0, 0, 0);
          to = new Date(from);
          to.setDate(to.getDate() + 6);
          to.setHours(23, 59, 59, 999);
        }
        var fromEl = pane.querySelector('#searchFromDate');
        var toEl = pane.querySelector('#searchToDate');
        if (fromEl) fromEl.value = fmt(from);
        if (toEl) toEl.value = fmt(to);
      });
    });
    function collectSearchParams(p) {
      var formUrlP = p.getAttribute('formurl') || '';
      var payMngDefaultSize = defaultListPageSizeForUrl(formUrlP);
      var params = { page: 1, size: payMngDefaultSize };
      var sizeEl = p.querySelector('#recordsPerPage');
      if (sizeEl) params.size = Math.max(1, parseInt(sizeEl.value, 10) || payMngDefaultSize);
      p.querySelectorAll('input, select').forEach(function (el) {
        var name = el.name || el.id;
        if (!name) return;
        if (el.type === 'checkbox') {
          if (!el.classList.contains('grid-check-all') && name.indexOf('search') === 0) params[name] = el.checked ? 'true' : 'false';
          return;
        }
        if (el.type === 'radio' && !el.checked) return;
        var v = el.value;
        if (v === undefined || v === null) return;
        if (name.indexOf('search') === 0) params[name] = v;
      });
      var pageEl = p.querySelector('#pageCnt');
      if (pageEl) params.page = parseInt(pageEl.value, 10) || 1;
      return params;
    }
    function applyTreeVisibility(pane, tbody, list) {
      var expanded = pane._treeExpanded;
      if (!expanded) return;
      var idSet = {};
      for (var h = 0; h < list.length; h++) {
        var hid = list[h].id != null ? String(list[h].id) : '';
        if (hid) idSet[hid] = true;
      }
      var visible = {};
      for (var i = 0; i < list.length; i++) {
        var row = list[i];
        var id = row.id != null ? String(row.id) : '';
        var pid = row.parentId != null ? String(row.parentId) : '';
        var parentInList = pid && idSet[pid];
        var isVisible = !parentInList ? true : (visible[pid] && (expanded.has ? expanded.has(pid) : pid in expanded));
        visible[id] = isVisible;
      }
      var rows = tbody.querySelectorAll('tr[data-id]');
      for (var j = 0; j < rows.length; j++) {
        var tr = rows[j];
        var rid = tr.getAttribute('data-id') || '';
        tr.style.display = visible[rid] !== false ? '' : 'none';
      }
      tbody.querySelectorAll('.table-row-resize-handle').forEach(function (handle) {
        var next = handle.nextElementSibling;
        handle.style.display = (next && next.style.display === 'none') ? 'none' : '';
      });
      tbody.querySelectorAll('.tree-toggle.expanded, .tree-toggle.collapsed').forEach(function (span) {
        var sid = span.getAttribute('data-id') || '';
        var isExp = expanded.has ? expanded.has(sid) : sid in expanded;
        var dl = span.getAttribute('data-org-level') || '';
        var ocl = 'tree-org-unknown';
        if (dl && /^[A-Z][A-Z0-9_]*$/.test(dl)) {
          ocl = 'tree-org-' + dl;
        } else {
          var parts = String(span.className || '').split(/\s+/);
          for (var pi = 0; pi < parts.length; pi++) {
            if (parts[pi].indexOf('tree-org-') === 0) {
              ocl = parts[pi];
              break;
            }
          }
        }
        var mt = span.getAttribute('data-merch-tone') || '';
        var merchCls = (mt && /^[a-z0-9_-]+$/.test(mt)) ? (' tree-merch-folder--' + mt) : '';
        span.className = 'tree-toggle ' + ocl + merchCls + ' ' + (isExp ? 'expanded' : 'collapsed');
        span.textContent = isExp ? '\u25BC' : '\u25B6';
        span.title = isExp ? '접기' : '펼치기';
      });
    }
    function buildGroupedTheadFromConfig(groups, cols) {
      var keyToGroup = {};
      (groups || []).forEach(function (g, gi) {
        (g.keys || []).forEach(function (k) { keyToGroup[k] = gi; });
      });
      var groupColCount = (groups || []).map(function () { return 0; });
      cols.forEach(function (c) {
        if (c.type === 'checkbox') return;
        var gi = keyToGroup[c.key];
        if (gi !== undefined) groupColCount[gi] += 1;
      });
      var top = '';
      var sub = '';
      var startedGroups = {};
      cols.forEach(function (c) {
        if (c.type === 'checkbox') {
          top += '<th data-key="_chk" rowspan="2" style="width:40px"><input type="checkbox" class="grid-check-all" title="전체선택"></th>';
          return;
        }
        var gi = keyToGroup[c.key];
        if (gi === undefined) {
          top += '<th data-key="' + (c.key || '') + '" rowspan="2">' + (c.label || c.key) + '</th>';
        } else {
          if (!startedGroups[gi] && groupColCount[gi] > 0) {
            startedGroups[gi] = true;
            top += '<th colspan="' + groupColCount[gi] + '">' + (groups[gi].label || '') + '</th>';
          }
          sub += '<th data-key="' + (c.key || '') + '">' + (c.label || c.key) + '</th>';
        }
      });
      return '<tr>' + top + '</tr><tr>' + sub + '</tr>';
    }
    function loadCommissionHistoryGrid(pane, tid) {
      var cfg = window.PG_SCREENS && window.PG_SCREENS.getMenuScreens && window.PG_SCREENS.getMenuScreens()['/commission/commisionList'];
      if (!cfg || !cfg.commissionHistory) return;
      var hcfg = cfg.commissionHistory;
      var table = pane.querySelector('#grid_commissionHist_' + tid);
      if (!table) return;
      var thead = table.querySelector('thead');
      var tbody = table.querySelector('tbody');
      var groups = hcfg.headerGroups || [];
      var cols = hcfg.columns || [];
      if (thead) thead.innerHTML = buildGroupedTheadFromConfig(groups, cols);
      var compId = pane._commissionHistCompId || '';
      var subEl = pane.querySelector('#commissionHistSubtitle_' + tid);
      if (subEl) subEl.textContent = compId ? ('표시 중: ' + compId + ' — No.1은 현재 적용 중인 수수료, 이후 행은 과거 구간입니다.') : '목록에서 가맹점 행을 클릭하면 해당 업체의 변경 이력이 표시됩니다.';
      var thLen = Math.max(cols.length, 8);
      if (!compId || !window.PG_API || !window.PG_API.commissionHistory) {
        if (tbody) tbody.innerHTML = '<tr><td colspan="' + thLen + '" class="text-center align-middle text-muted py-3">업체를 선택하세요.</td></tr>';
        return;
      }
      window.PG_API.commissionHistory(compId, { page: 1, size: 200 }).then(function (data) {
        var list = data && data.list ? data.list : [];
        if (!tbody) return;
        if (list.length === 0) {
          tbody.innerHTML = '<tr><td colspan="' + thLen + '" class="text-center align-middle text-muted py-3">조회 결과가 없습니다. 업체코드를 확인하세요.</td></tr>';
          return;
        }
        var html = '';
        list.forEach(function (row) {
          html += '<tr>';
          cols.forEach(function (c) {
            if (c.type === 'checkbox') {
              html += '<td class="text-center align-middle"></td>';
              return;
            }
            var raw = row[c.key];
            var val = '';
            if (raw !== undefined && raw !== null) {
              val = typeof raw === 'object' ? JSON.stringify(raw) : String(raw);
            }
            html += '<td class="text-center align-middle text-nowrap">' + val + '</td>';
          });
          html += '</tr>';
        });
        tbody.innerHTML = html;
        if (window.PG_TABLE_COL_RESIZE && typeof window.PG_TABLE_COL_RESIZE.refresh === 'function' && table) {
          window.PG_TABLE_COL_RESIZE.refresh(table);
        }
      }).catch(function () {
        if (tbody) tbody.innerHTML = '<tr><td colspan="' + thLen + '" class="text-center align-middle text-danger py-3">히스토리 조회 실패</td></tr>';
        if (window.PG_TABLE_COL_RESIZE && typeof window.PG_TABLE_COL_RESIZE.refresh === 'function' && table) {
          window.PG_TABLE_COL_RESIZE.refresh(table);
        }
      });
    }
    function appendCustomViewColumnsToCfg(orig, paneCtx) {
      if (!orig || !paneCtx) return orig;
      var defs = paneCtx._viewCustomColumnDefs;
      if (!defs || !defs.length) return orig;
      var cols = orig.columns || [];
      var keys = {};
      cols.forEach(function (c) { if (c && c.key) keys[c.key] = 1; });
      var added = [];
      defs.forEach(function (cc) {
        var k = cc.columnKey || cc.column_key;
        if (!k || keys[k]) return;
        keys[k] = 1;
        added.push({ key: k, label: cc.displayName || cc.display_name || k });
      });
      if (!added.length) return orig;
      var copy = Object.assign({}, orig);
      copy.columns = cols.concat(added);
      return copy;
    }
    function appendCustomColsToAllColsArray(allCols, defs) {
      if (!defs || !defs.length) return allCols || [];
      var cols = allCols || [];
      var keys = {};
      cols.forEach(function (c) { if (c && c.key) keys[c.key] = 1; });
      var out = cols.slice();
      defs.forEach(function (cc) {
        var k = cc.columnKey || cc.column_key;
        if (!k || keys[k]) return;
        keys[k] = 1;
        out.push({ key: k, label: cc.displayName || cc.display_name || k });
      });
      return out;
    }
    /** 결제내역·통합내역 상단: 성공/실패/무효/환불/기타 · 통화별 금액 (서버 meta.payListStatusBar) */
    /** 참고 UI: JPY·KRW 천단위 `.`, USD 등 소수 `,`(de-DE), 서버 plain 숫자 기준 */
    function formatPayListStatusAmount(cur, plainStr) {
      var n = parseFloat(String(plainStr == null ? '0' : plainStr).replace(/,/g, ''));
      if (isNaN(n)) n = 0;
      var c = String(cur || 'KRW').toUpperCase();
      if (c === 'KRW' || c === 'JPY') {
        return Math.round(n).toLocaleString('de-DE', { maximumFractionDigits: 0 });
      }
      if (c === 'USD') {
        return n.toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
      }
      return n.toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 8 });
    }
    function payListStatusBarCurrencySortKey(c) {
      var u = String(c || '').toUpperCase();
      /* 서버 PayListStatusBarBuckets.currencyRank 와 유사 */
      var order = {
        JPY: 0, USD: 1, THB: 2, EUR: 3, GBP: 4, SGD: 5, HKD: 6, CNY: 7, MYR: 8, CHF: 9, AUD: 10, NZD: 11, KRW: 12
      };
      return order.hasOwnProperty(u) ? order[u] : 40;
    }
    function renderPayListStatusBarHtml(bar, meta) {
      if (!bar || !bar.buckets || !bar.buckets.length) return '';
      var labels = { SUCCESS: '성공', FAIL: '실패', VOID: '무효', REFUND: '환불', FORCE_REFUND: '강제환불', CANCEL: '취소', OTHER: '기타' };
      var pillCls = {
        SUCCESS: 'pay-list-status-bar__pill pay-list-status-bar__pill--success',
        FAIL: 'pay-list-status-bar__pill pay-list-status-bar__pill--fail',
        VOID: 'pay-list-status-bar__pill pay-list-status-bar__pill--void',
        REFUND: 'pay-list-status-bar__pill pay-list-status-bar__pill--refund',
        FORCE_REFUND: 'pay-list-status-bar__pill pay-list-status-bar__pill--force-refund',
        CANCEL: 'pay-list-status-bar__pill pay-list-status-bar__pill--cancel',
        OTHER: 'pay-list-status-bar__pill pay-list-status-bar__pill--other'
      };
      var barOrder = bar.currencyOrder && bar.currencyOrder.length ? bar.currencyOrder : null;
      function pillHtmlForBucket(b) {
        var label = labels[b.key] || b.key;
        var pClass = pillCls[b.key] || pillCls.OTHER;
        var cnt = b.count != null ? Number(b.count) : 0;
        if (isNaN(cnt)) cnt = 0;
        var am = b.amountsByCurrency || {};
        var cc = b.countsByCurrency || null;
        var curKeys = barOrder ? barOrder.slice() : Object.keys(am);
        if (!barOrder) {
          curKeys.sort(function (a, b2) {
            var ra = payListStatusBarCurrencySortKey(a);
            var rb = payListStatusBarCurrencySortKey(b2);
            if (ra !== rb) return ra - rb;
            return String(a).localeCompare(String(b2));
          });
        }
        var amtInner;
        if (bar.multiCurrency && cc) {
          var curParts = [];
          curKeys.forEach(function (ck) {
            var escC = String(ck).replace(/</g, '&lt;').replace(/&/g, '&amp;');
            var ncc = Object.prototype.hasOwnProperty.call(cc, ck) ? Number(cc[ck]) : 0;
            if (isNaN(ncc)) ncc = 0;
            var per = formatPayListStatusAmount(ck, am[ck]);
            curParts.push('<span class="pay-list-status-bar__cur">' + escC + ' [' + ncc + '] / ' + per + '</span>');
          });
          amtInner = curParts.length
            ? curParts.join(' <span class="pay-list-status-bar__sep">|</span> ')
            : '<span class="pay-list-status-bar__dash">—</span>';
        } else if (bar.multiCurrency) {
          var curParts2 = [];
          curKeys.forEach(function (ck) {
            var escC = String(ck).replace(/</g, '&lt;').replace(/&/g, '&amp;');
            curParts2.push('<span class="pay-list-status-bar__cur">' + escC + ' ' + formatPayListStatusAmount(ck, am[ck]) + '</span>');
          });
          amtInner = curParts2.length
            ? curParts2.join(' <span class="pay-list-status-bar__sep">|</span> ')
            : '<span class="pay-list-status-bar__dash">—</span>';
        } else {
          var pk = Object.keys(am);
          var hqFb = meta && meta.hqPayDisplayCurrencyCode ? String(meta.hqPayDisplayCurrencyCode).trim().toUpperCase() : '';
          var ck0 = pk.length ? pk[0] : String(bar.primaryCurrency || hqFb || 'KRW');
          var esc0 = String(ck0).replace(/</g, '&lt;').replace(/&/g, '&amp;');
          var per0 = formatPayListStatusAmount(ck0, am[ck0]);
          amtInner = '<span class="pay-list-status-bar__cur">' + esc0 + ' [' + cnt + '] / ' + per0 + '</span>';
        }
        return (
          '<span class="' + pClass + '">' +
          '<span class="pay-list-status-bar__lbl">' + label + '</span> ' +
          '<span class="pay-list-status-bar__amt">' + amtInner + '</span></span>'
        );
      }
      var rowsHtml = '';
      var list = bar.buckets || [];
      for (var bi = 0; bi < list.length; bi += 3) {
        var chunk = list.slice(bi, bi + 3);
        var cells = chunk.map(function (b0) { return pillHtmlForBucket(b0); }).join('');
        rowsHtml += '<div class="pay-list-status-bar__row">' + cells + '</div>';
      }
      var note = bar.partial ? '<div class="pay-list-status-bar__note-row"><span class="pay-list-status-bar__note text-muted fw-normal">(전체 중 일부만 집계)</span></div>' : '';
      return (
        '<div class="pay-list-status-bar__inner pay-list-status-bar__inner--grid">' +
        rowsHtml +
        note +
        '</div>'
      );
    }
    /** 검색 조건 전체 집계: 건수=승인 건수, 금액=통화별(본사·총본사 다통화 / 총판·하위 단일 통화) */
    function renderPayListFinancialSummaryHtml(fin) {
      if (!fin) return '';
      var order = fin.currencyOrder && fin.currencyOrder.length ? fin.currencyOrder : null;
      function orderedKeys(map) {
        if (order) return order.slice();
        var keys = Object.keys(map || {});
        keys.sort(function (a, b2) {
          var ra = payListStatusBarCurrencySortKey(a);
          var rb = payListStatusBarCurrencySortKey(b2);
          if (ra !== rb) return ra - rb;
          return String(a).localeCompare(String(b2));
        });
        return keys;
      }
      function fmtMetricAmountOnly(label, map) {
        var keys = orderedKeys(map);
        if (!keys.length) return '<span class="pay-list-financial__metric"><span class="pay-list-financial__lbl">' + label + '</span>: —</span>';
        var curParts = keys.map(function (ck) {
          var escC = String(ck).replace(/</g, '&lt;').replace(/&/g, '&amp;');
          return '<span class="pay-list-financial__cur">' + escC + ' ' + formatPayListStatusAmount(ck, map[ck]) + '</span>';
        });
        return '<span class="pay-list-financial__metric"><span class="pay-list-financial__lbl">' + label + '</span>: [' +
          curParts.join(' <span class="pay-list-status-bar__sep">|</span> ') + ']</span>';
      }
      function fmtMetricWithCounts(label, amountMap, countMap) {
        var keys = orderedKeys(amountMap);
        if (!keys.length) return '<span class="pay-list-financial__metric"><span class="pay-list-financial__lbl">' + label + '</span>: —</span>';
        var curParts = keys.map(function (ck) {
          var escC = String(ck).replace(/</g, '&lt;').replace(/&/g, '&amp;');
          var nc = countMap && Object.prototype.hasOwnProperty.call(countMap, ck) ? Number(countMap[ck]) : 0;
          if (isNaN(nc)) nc = 0;
          return '<span class="pay-list-financial__cur">' + escC + ' [' + nc + '] ' +
            formatPayListStatusAmount(ck, amountMap[ck]) + '</span>';
        });
        return '<span class="pay-list-financial__metric"><span class="pay-list-financial__lbl">' + label + '</span>: [' +
          curParts.join(' <span class="pay-list-status-bar__sep">|</span> ') + ']</span>';
      }
      var pipe = ' <span class="pay-list-status-bar__pipe" aria-hidden="true">ㅣ</span> ';
      var segs = [];
      segs.push(fmtMetricWithCounts('승인금액', fin.approveByCurrency, fin.approveCountByCurrency));
      segs.push(fmtMetricWithCounts('취소금액', fin.cancelByCurrency, fin.cancelCountByCurrency));
      segs.push(fmtMetricAmountOnly('결제금액', fin.paymentByCurrency));
      segs.push(fmtMetricAmountOnly('총수수료', fin.feeByCurrency));
      segs.push(fmtMetricAmountOnly('보류금액', fin.holdByCurrency));
      segs.push(fmtMetricAmountOnly('지급액', fin.payoutByCurrency));
      return '<div class="pay-list-financial-summary__inner">' + segs.join(pipe) + '</div>';
    }
    function updatePayListAggregateBars(pane, tabId, meta) {
      var fid = 'payListFinancialSummary_' + tabId;
      var finEl = pane && pane.querySelector && pane.querySelector('#' + fid);
      if (finEl) {
        var fin = meta && meta.payListFinancialSummary;
        if (fin) {
          finEl.innerHTML = renderPayListFinancialSummaryHtml(fin);
          finEl.classList.remove('pay-list-financial-summary--empty');
        } else {
          finEl.innerHTML = '';
          finEl.classList.add('pay-list-financial-summary--empty');
        }
      }
      var sid = 'payListStatusBar_' + tabId;
      var el = pane && pane.querySelector && pane.querySelector('#' + sid);
      if (!el) return;
      var bar = meta && meta.payListStatusBar;
      if (!bar || !bar.buckets) {
        el.innerHTML = '';
        el.classList.add('pay-list-status-bar--empty');
        return;
      }
      el.classList.remove('pay-list-status-bar--empty');
      el.innerHTML = renderPayListStatusBarHtml(bar, meta);
    }
    function doSearch(p, tid, pageOverride) {
      var url = p.getAttribute('formurl') || '';
      var api = window.PG_API;
      if (!url || url === '/main') return;
      if (p && p.classList) {
        p.classList.toggle('screen-calc-gm-list', url === '/calc/calcGmList' || url === '/settlement/franchiseList'
          || url === '/calc/paySettlementHoldList' || url === '/settlement/paySettlementHoldList'
          || url === '/pay/payHoldList' || url === '/settlement/holdList');
        p.classList.toggle('screen-calc-fee-list', url === '/calc/feeList' || url === '/settlement/feeList');
        p.classList.toggle('screen-pay-list', url === '/calc/payList' || url === '/calc/chillPayTrList' || url === '/calc/chillPaySettlementList' || url === '/calc/payNotiList' || url === '/calc/paySuccessList' || url === '/calc/payFailList' || url === '/calc/payRefundList' || url === '/calc/payForceRefundList' || url === '/calc/payCancelList' || url === '/calc/payVoidList' || url === '/calc/offsetCancList' || url === '/pay/easyPay' || url === '/pay/chatbotPay');
        p.classList.toggle('screen-chill-pay-tr-list', url === '/calc/chillPayTrList');
        p.classList.toggle('screen-comp-mng-tree', url === '/comp/compMngTree');
        p.classList.toggle('screen-distribution-list', url === '/calc/calcList' || url === '/settlement/distributionList');
        p.classList.toggle('screen-user-mng', url === '/user/userMng');
      }
      var cfg = window.PG_SCREENS && window.PG_SCREENS.getMenuScreens && window.PG_SCREENS.getMenuScreens()[url];
      /* 조직별 권한 세팅: 그리드 조건/캐시와 무관하게 URL로만 매트릭스 로드 */
      if (url === '/hq/permissionMng') {
        var dimmPerm = document.getElementById('dimm');
        if (dimmPerm) dimmPerm.style.display = 'flex';
        if (!api || !api.hqPermissionMng) {
          if (dimmPerm) dimmPerm.style.display = 'none';
          return;
        }
        var permTimeoutMs = 25000;
        var permTimeout = new Promise(function (_, reject) {
          setTimeout(function () {
            reject(new Error('응답 시간이 초과되었습니다. PG START로 서버를 재시작한 뒤 다시 시도하세요.'));
          }, permTimeoutMs);
        });
        Promise.race([api.hqPermissionMng({}), permTimeout]).then(function (data) {
          var payload = data;
          if (data && data.success === true && data.data && typeof data.data === 'object'
              && data.catalog == null && data.data.catalog != null) {
            payload = data.data;
          }
          if (window.initOrgPagePermissionMatrix) window.initOrgPagePermissionMatrix(p, tid, payload);
        }).catch(function (err) {
          var msg = (err && err.message) ? err.message : '권한 설정을 불러오지 못했습니다.';
          alert(msg);
          var tbErr = p.querySelector('#orgPermTbody_' + tid);
          if (tbErr) {
            var safe = String(msg).replace(/</g, '&lt;').replace(/&/g, '&amp;');
            tbErr.innerHTML = '<tr><td colspan="4" class="text-center text-danger py-4">' + safe + '</td></tr>';
          }
        }).finally(function () {
          if (dimmPerm) dimmPerm.style.display = 'none';
        });
        return;
      }
      if (url === '/hq/notifyInbound') {
        var pcNi = p.querySelector('#pageCnt');
        if (pageOverride != null && pageOverride !== '' && pageOverride !== false && pcNi) {
          pcNi.value = String(pageOverride);
        }
        if (typeof p._hqNotifyInboundReload === 'function') {
          p._hqNotifyInboundReload();
        }
        return;
      }
      if (!cfg || ((!cfg.columns || cfg.columns.length === 0) && !cfg.columnsBySub && !cfg.columnsRegionalPayout && !cfg.orgPagePermissionMatrix)) return;
      if (p._payListMergedCfg && cfg.payListVariant) {
        cfg = p._payListMergedCfg;
      }
      cfg = appendCustomViewColumnsToCfg(cfg, p) || cfg;
      if (url === '/calc/paySuccessList' && cfg && cfg.columns) {
        cfg = stripPayFollowColumnsFromPayListCfg(cfg);
      }
      var params = collectSearchParams(p);
      if (pageOverride !== undefined && pageOverride !== null && String(pageOverride).trim() !== '') {
        var pgo = parseInt(String(pageOverride), 10);
        if (!isNaN(pgo) && pgo >= 1) params.page = pgo;
      }
      if (url === '/commission/commisionList') {
        params.useYn = (params.searchUseYn != null && String(params.searchUseYn).trim() !== '') ? String(params.searchUseYn).trim() : 'Y';
      }
      if ((url === '/calc/settlementReport' || url === '/settlement/settlementReport') && !params.searchReportSub) {
        params.searchReportSub = 'AGG';
      }
      if ((url === '/calc/settlementReport' || url === '/settlement/settlementReport') && !params.searchReportKind) {
        params.searchReportKind = 'MERCHANT_STMT';
      }
      if (url === '/calc/settlementReport' || url === '/settlement/settlementReport') {
        var rkCoerce = params.searchReportKind || 'MERCHANT_STMT';
        if (rkCoerce === 'REGIONAL_PAYOUT' && params.searchReportSub === 'RST') {
          params.searchReportSub = 'AGG';
          var subCoerceEl = p.querySelector('[name="searchReportSub"]');
          if (subCoerceEl) subCoerceEl.value = 'AGG';
        }
      }
      if (cfg.payListVariant) params.payListVariant = cfg.payListVariant;
      p.setAttribute('data-last-url', url);
      p.setAttribute('data-last-page', String(params.page));
      var dimm = document.getElementById('dimm');
      if (dimm) dimm.style.display = 'flex';
      var promise = null;
      if (url === '/system/noticeList') promise = api.noticeList(params);
      else if (url === '/calc/chillPayTrList') promise = api.chillPayTrSearch(params);
      else if (url === '/calc/chillPaySettlementList') promise = api.chillPaySettlementSearch(params);
      else if (url === '/calc/payList' || url === '/calc/payFailList' || url === '/calc/offsetCancList' || url === '/pay/easyPay' || url === '/pay/chatbotPay' || url === '/calc/payNotiList' || url === '/calc/paySuccessList' || url === '/calc/payRefundList' || url === '/calc/payForceRefundList' || url === '/calc/payCancelList' || url === '/calc/payVoidList') promise = api.payList(params);
      else if (url === '/comp/compMngTree' || url === '/comp/myCompMng' || url === '/comp/compMng' || url === '/comp/compInfo') {
        if (url === '/comp/myCompMng') params.myOrgOnly = true;
        promise = api.compList(params);
      }
      else if (url === '/comp/compInfoHistList' || url === '/comp/compChangeHistory') promise = api.compChangeHistory(params);
      else if (url === '/commission/commisionList') promise = api.commissionList(params);
      else if (url === '/user/userMng') promise = api.userList(params);
      else if (url === '/set/gridSetMng' || url === '/user/menuOrderMng') promise = api.menuOrderMng(params);
      else if (url === '/calc/calcList' || url === '/settlement/distributionList') promise = api.settlementDistributionList(params);
      else if (url === '/calc/calcGmList' || url === '/settlement/franchiseList') promise = api.settlementFranchiseList(params);
      else if (url === '/calc/paySettlementHoldList' || url === '/settlement/paySettlementHoldList'
          || url === '/pay/payHoldList' || url === '/settlement/holdList') promise = api.settlementPayoutHoldList(params);
      else if (url === '/calc/feeList' || url === '/settlement/feeList') promise = api.settlementFeeList(params);
      else if (url === '/calc/compPointMngList') promise = api.settlementRecoveryList(params);
      else if (url === '/settlement/recallMng') promise = api.settlementRecallMng(params);
      else if (url === '/calc/balanceList' || url === '/settlement/balanceList') promise = api.settlementBalanceList(params);
      else if (url === '/calc/unpaidMng' || url === '/settlement/unpaidMng') promise = api.settlementUnpaidMng(params);
      else if (url === '/calc/collateralList' || url === '/settlement/collateralList') promise = api.settlementCollateralList(params);
      else if (url === '/calc/exCalcList' || url === '/settlement/execute') promise = api.settlementExecute(params);
      else if (url === '/calc/settlementReport' || url === '/settlement/settlementReport') {
        var sub = params.searchReportSub || 'AGG';
        var repKindFetch = params.searchReportKind || 'MERCHANT_STMT';
        if (sub === 'RST' && repKindFetch !== 'REGIONAL_PAYOUT') promise = api.settlementReportConfirmedRuns(params);
        else if (sub === 'EXE') promise = api.settlementReportExecute(params);
        else if (sub === 'SUM') promise = api.settlementReportSummary(params);
        else promise = api.settlementReportAggregate(params);
      }
      else if (url === '/noti/notiUrlMng' || url === '/notify/payUrlMng') promise = api.notifyPayUrlMng(params);
      else if (url === '/noti/notiSendMngList' || url === '/notify/paySendMng') promise = api.notifyPaySendMng(params);
      else if (url === '/noti/notiCashReceiptUrlMng' || url === '/notify/cashReceiptUrlMng') promise = api.notifyCashReceiptUrlMng(params);
      else if (url === '/noti/notiCashReceiptSendMngList' || url === '/notify/cashReceiptSendMng') promise = api.notifyCashReceiptSendMng(params);
      else if (url === '/hq/pgApiMng') promise = api.hqPgApiMng(params);
      else if (url === '/hq/permissionMng') promise = api.hqPermissionMng(params);
      else if (url === '/hq/accountMng') promise = api.hqAccountAccessList(params);
      else if (url === '/risk/list') promise = Promise.resolve({ list: [], totalElements: 0, totalPages: 1, page: params.page || 1, size: params.size || 20 });
      if (!promise) {
        if (dimm) dimm.style.display = 'none';
        p._lastGridList = [];
        p._lastGridCols = null;
        var tbody = p.querySelector('#grid_' + tid + ' tbody');
        var rk0 = params.searchReportKind || 'MERCHANT_STMT';
        var sub0 = params.searchReportSub || 'AGG';
        var emptyCols = (cfg.columns && cfg.columns.length) ? cfg.columns.length
          : (rk0 === 'REGIONAL_PAYOUT' && cfg.columnsRegionalPayout && cfg.columnsRegionalPayout[sub0] ? cfg.columnsRegionalPayout[sub0].length
            : (cfg.columnsBySub && cfg.columnsBySub[sub0] ? cfg.columnsBySub[sub0].length
              : (cfg.columnsBySub && cfg.columnsBySub.AGG ? cfg.columnsBySub.AGG.length : 8)));
        if (tbody) tbody.innerHTML = '<tr><td colspan="' + emptyCols + '" class="empty-state-cell text-center text-muted">조회된 데이터가 없습니다.</td></tr>';
        var cntEl = p.querySelector('#summary_건수, .summary-count, [data-summary="건수"]');
        if (cntEl) cntEl.textContent = (cntEl.id === 'summary_건수' ? '건수: ' : '') + '0';
        if (payListSearchUrls.indexOf(url) !== -1) updatePayListAggregateBars(p, tid, null);
        return;
      }
      promise.then(function (data) {
        var feeFmtByCur = data && data.meta && data.meta.feeCurrencyFormatByCur ? data.meta.feeCurrencyFormatByCur : null;
        if (p) p._feeCurrencyFormatByCur = feeFmtByCur;
        if (url === '/hq/accountMng') {
          p._hqAccountAccessUsers = Array.isArray(data && data.users) ? data.users : [];
          p._hqAccountAccessComps = Array.isArray(data && data.comps) ? data.comps : [];
          p._hqAccountAccessPickersReady = true;
        }
        var list = data && data.list ? data.list : [];
        if (url === '/user/userMng' && p._userMngDraftRows && p._userMngDraftRows.length) {
          list = p._userMngDraftRows.concat(list);
        }
        list = applyObserverCompTreeScope(list, url);
        var total = data && data.totalElements !== undefined ? data.totalElements : list.length;
        var totalPages = data && data.totalPages !== undefined ? data.totalPages : 1;
        var repKind = params.searchReportKind || 'MERCHANT_STMT';
        var sub = params.searchReportSub || 'AGG';
        var allCols;
        if (repKind === 'REGIONAL_PAYOUT' && cfg.columnsRegionalPayout && cfg.columnsRegionalPayout[sub]) {
          allCols = cfg.columnsRegionalPayout[sub];
        } else {
          allCols = (cfg.columnsBySub && cfg.columnsBySub[sub])
            ? cfg.columnsBySub[sub]
            : (cfg.columns || []);
        }
        allCols = appendCustomColsToAllColsArray(allCols, p._viewCustomColumnDefs);
        var selCols = p._selectedColumns;
        var fixedKeys = ['rowNo', 'compNm', 'compId', 'trnDate', 'trnTime', 'routeNo', 'chillTransactionId', 'compDivNm', 'merchantNm', '_pgCredEdit'];
        if (url === '/calc/chillPayTrList') {
          fixedKeys = ['rowNo', 'transactionId', 'compNm', 'compId', 'trnDate', 'trnTime', 'routeNo'];
        } else if (url === '/calc/chillPaySettlementList') {
          fixedKeys = ['rowNo'];
        }
        if (url === '/hq/pgApiMng') {
          /** 번호·관리(_pgRowAct는 아래 별도 처리)만 고정. Route·엔드포인트 등은 VIEW SETTING과 동일 */
          fixedKeys = ['rowNo'];
        }
        if (url === '/commission/commisionList') {
          /** 체크·No·가맹점·업체코드·처리(인라인 저장)만 항상 표시. 나머지는 VIEW SETTING·조직항목설정과 동일 키로 토글 */
          fixedKeys = ['_chk', 'rowNo', 'compNm', 'compId', 'inlineActions'];
        } else if (url === '/calc/feeList' || url === '/settlement/feeList') {
          /** 체크·번호·업체·거래일·통화 고정. 거래시간·루트·승인번호·거래번호(우리) 등은 VIEW SETTING과 동일하게 토글 */
          fixedKeys = ['_chk', 'rowNo', 'compNm', 'compId', 'trnDate', 'curType'];
        } else if (url === '/calc/calcGmList' || url === '/settlement/franchiseList'
            || url === '/calc/exCalcList' || url === '/settlement/execute'
            || url === '/calc/paySettlementHoldList' || url === '/settlement/paySettlementHoldList'
            || url === '/pay/payHoldList' || url === '/settlement/holdList'
            || url === '/calc/unpaidMng' || url === '/settlement/unpaidMng'
            || url === '/calc/compPointMngList' || url === '/settlement/recallMng') {
          fixedKeys = ['_chk', 'rowNo', 'compNm', 'compId', 'curType'];
        }
        var restrictCols = p._columnAllowanceRestricted === true;
        var allowKeys = p._allowedColumnKeys;
        var cols = allCols.filter(function (c) {
          if (c.type === 'checkbox' || fixedKeys.indexOf(c.key) !== -1) return true;
          // API연동설정: [관리]는 VIEW에서 숨겨도 항상 표시(가맹점 행 수정·삭제)
          if (url === '/hq/pgApiMng' && (c.key === '_pgRowAct' || c.type === 'pgApiMngRowActions')) return true;
          if (restrictCols) {
            if (!allowKeys || allowKeys.length === 0) return false;
            if (allowKeys.indexOf(c.key) === -1) return false;
          }
          if (!selCols || selCols.length === 0) return true;
          return selCols.indexOf(c.key) !== -1;
        });
        if (selCols && selCols.length) {
          var selectedOrder = {};
          selCols.forEach(function (k, i) { selectedOrder[k] = i; });
          cols = cols.slice().sort(function (a, b) {
            var aFixed = a.type === 'checkbox' || fixedKeys.indexOf(a.key) !== -1;
            var bFixed = b.type === 'checkbox' || fixedKeys.indexOf(b.key) !== -1;
            if (aFixed || bFixed) return 0;
            var ai = selectedOrder[a.key];
            var bi = selectedOrder[b.key];
            if (ai == null && bi == null) return 0;
            if (ai == null) return 1;
            if (bi == null) return -1;
            return ai - bi;
          });
        }
        // API연동설정: [관리]는 행의 가장 오른쪽(체크박스 제외 마지막 열). VIEW 정렬로 regDt 뒤에 다른 열이 있어도 관리가 끝에 오도록 분리 후 push.
        if (url === '/hq/pgApiMng') {
          var hqPgActCol = null;
          cols = cols.filter(function (c) {
            if (c.key === '_pgRowAct' || c.type === 'pgApiMngRowActions') {
              hqPgActCol = c;
              return false;
            }
            return true;
          });
          if (hqPgActCol) {
            cols.push(hqPgActCol);
          }
        }
        p._lastGridList = list;
        p._lastGridCols = cols;
        var thead = p.querySelector('#grid_' + tid + ' thead');
        if (thead && cfg.distributionThreeRowHeader && window.PG_SCREENS && typeof window.PG_SCREENS.buildDistributionListTheadHtml === 'function') {
          thead.innerHTML = window.PG_SCREENS.buildDistributionListTheadHtml();
        } else if (thead) {
          var groups = cfg.headerGroups || [];
          if (!groups.length) {
            var theadTr = thead.querySelector('tr') || document.createElement('tr');
            theadTr.innerHTML = cols.map(function (c) {
              if (c.type === 'checkbox') return '<th data-key="_chk" style="width:40px"><input type="checkbox" class="grid-check-all" title="전체선택"></th>';
              var thT = c.title ? (' title="' + String(c.title).replace(/&/g, '&amp;').replace(/"/g, '&quot;') + '"') : '';
              var thClassParts = [];
              if (c.align === 'center') thClassParts.push('text-center');
              if (c.thClass) thClassParts.push(String(c.thClass));
              var thCls = thClassParts.length ? (' class="' + thClassParts.join(' ') + '"') : '';
              return '<th data-key="' + (c.key || '') + '"' + thT + thCls + '>' + (c.label || c.key) + '</th>';
            }).join('');
            if (!theadTr.parentNode) thead.appendChild(theadTr);
          } else {
            var keyToGroup = {};
            groups.forEach(function (g, gi) {
              (g.keys || []).forEach(function (k) { keyToGroup[k] = gi; });
            });
            var groupColCount = groups.map(function () { return 0; });
            cols.forEach(function (c) {
              if (c.type === 'checkbox') return;
              var gi = keyToGroup[c.key];
              if (gi !== undefined) groupColCount[gi] += 1;
            });
            var top = '';
            var sub = '';
            var startedGroups = {};
            cols.forEach(function (c) {
              if (c.type === 'checkbox') {
                top += '<th data-key="_chk" rowspan="2" style="width:40px"><input type="checkbox" class="grid-check-all" title="전체선택"></th>';
                return;
              }
              var gi = keyToGroup[c.key];
              if (gi === undefined) {
                top += '<th data-key="' + (c.key || '') + '" rowspan="2">' + (c.label || c.key) + '</th>';
              } else {
                if (!startedGroups[gi] && groupColCount[gi] > 0) {
                  startedGroups[gi] = true;
                  top += '<th colspan="' + groupColCount[gi] + '">' + (groups[gi].label || '') + '</th>';
                }
                sub += '<th data-key="' + (c.key || '') + '">' + (c.label || c.key) + '</th>';
              }
            });
            thead.innerHTML = '<tr>' + top + '</tr><tr>' + sub + '</tr>';
          }
        }
        var thLen = cols.length;
        var tbody = p.querySelector('#grid_' + tid + ' tbody');
        if (!tbody) {
          if (dimm) dimm.style.display = 'none';
          return;
        }
        if (list.length === 0) {
          tbody.innerHTML = '<tr><td colspan="' + (thLen || (cfg.columns && cfg.columns.length)) + '" class="empty-state-cell text-center text-muted py-4">조회된 데이터가 없습니다.</td></tr>';
        } else {
          var html = '';
          var isCompMngTree = (url === '/comp/compMngTree');
          var hasChildrenMap = {};
          if (isCompMngTree) {
            list.forEach(function (r) {
              var pid = r.parentId != null ? String(r.parentId) : null;
              if (pid) hasChildrenMap[pid] = true;
            });
            var SetOrFallback = window.Set || function () { var o = {}; return { add: function (k) { o[k] = 1; }, has: function (k) { return k in o; }, delete: function (k) { delete o[k]; } }; };
            p._treeExpanded = new SetOrFallback();
            list.forEach(function (r) {
              var id = r.id != null ? String(r.id) : '';
              if (id && hasChildrenMap[id]) p._treeExpanded.add(id);
            });
          }
          var groupKey = cfg.rowGroupByKey;
          var prevGroup = null;
          var pageNum = data && data.page != null ? parseInt(data.page, 10) : (params.page || 1);
          if (isNaN(pageNum) || pageNum < 1) pageNum = 1;
          var pageSizeFallback = defaultListPageSizeForUrl(url);
          var pageSize = data && data.size != null ? parseInt(data.size, 10) : pageSizeFallback;
          if (isNaN(pageSize) || pageSize < 1) pageSize = pageSizeFallback;
          var rowNoBase = (pageNum - 1) * pageSize;
          list.forEach(function (row, idx) {
            var escAttr = function (s) {
              return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
            };
            var escHtmlBody = function (s) {
              return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
            };
            var escHtmlTitle = function (s) {
              return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
            };
            var rowId = row.id != null ? String(row.id) : '';
            var parentId = row.parentId != null ? String(row.parentId) : '';
            var hasChildren = isCompMngTree && rowId && hasChildrenMap[rowId];
            var trExtraClass = '';
            if (groupKey) {
              var gk = row[groupKey] !== undefined && row[groupKey] !== null ? String(row[groupKey]) : '';
              if (gk !== prevGroup) {
                trExtraClass = ' tr-user-group-start';
                prevGroup = gk;
              }
            }
            if (url === '/hq/pgApiMng') {
              var hqOp = String(row.operationalYn || '') === 'Y';
              var hqUse = String(row.useYn || 'Y') !== 'N';
              trExtraClass = (trExtraClass || '') + (hqOp && hqUse ? ' hq-pg-row-operational' : ' hq-pg-row-inactive');
            }
            /* 상태별 파스텔 행 배경·Status 뱃지: 통합내역·결제내역·수수료내역 (분리 화면은 줄무늬·일반 셀) */
            var payPastelRowUrl = url === '/calc/payList' || url === '/calc/chillPayTrList' || url === '/calc/chillPaySettlementList' || url === '/calc/feeList' || url === '/settlement/feeList';
            if (payPastelRowUrl && window.PG_UI) {
              var toneTr = url === '/calc/chillPayTrList' || url === '/calc/chillPaySettlementList'
                ? (typeof window.PG_UI.resolveChillTrRowTone === 'function' ? window.PG_UI.resolveChillTrRowTone(row) : 'neutral')
                : (typeof window.PG_UI.resolvePayRowTone === 'function' ? window.PG_UI.resolvePayRowTone(row) : 'neutral');
              if (toneTr && toneTr !== 'neutral') {
                trExtraClass = (trExtraClass || '') + ' pay-row-tone--' + toneTr;
              }
            }
            var dataCompAttr = (url === '/commission/commisionList' && row.compId) ? (' data-comp-id="' + String(row.compId).replace(/"/g, '&quot;') + '"') : '';
            var draftAttr = (url === '/user/userMng' && row._draft) ? (' data-draft="1" data-temp-id="' + escAttr(row._tempId) + '"') : '';
            html += '<tr' + (trExtraClass ? ' class="' + trExtraClass.trim() + '"' : '') + ' data-row-idx="' + idx + '" data-id="' + (rowId || '') + '" data-parent-id="' + (parentId || '') + '" data-row="' + (isCompMngTree ? encodeURIComponent(JSON.stringify(row)) : '') + '"' + dataCompAttr + draftAttr + '>';
            cols.forEach(function (c) {
              if (c.type === 'checkbox') html += '<td><input type="checkbox" class="grid-row-check"></td>';
              else if (c.type === 'payActions') {
                if (url === '/calc/paySuccessList') {
                  html += '<td class="text-muted text-center small">—</td>';
                } else {
                  var seq = row.paySeq != null ? String(row.paySeq) : '';
                  var esc = function (s) {
                    return String(s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
                  };
                  var es = esc(seq);
                  var pfa = (data && data.meta && data.meta.payFollowAllowed) || {};
                  var pfr = row.payFollowRow || {};
                  function payFollowBtn(act, label) {
                    var metaOk = pfa[act] === true;
                    var rowOk = pfr[act];
                    if (rowOk === undefined || rowOk === null) rowOk = true;
                    var ok = metaOk && rowOk === true;
                    var dis = ok ? '' : ' disabled';
                    var cls = ok ? 'btn btn-link btn-sm p-0 pay-follow' : 'btn btn-link btn-sm p-0 pay-follow pay-follow--off text-muted';
                    return '<button type="button" class="' + cls + '"' + dis + ' data-act="' + act + '" data-trn="' + es + '">' + label + '</button>';
                  }
                  html += '<td class="small text-nowrap pay-actions-cell">' +
                    payFollowBtn('AUTO_VOID', '자동무효') + ' ' +
                    payFollowBtn('EMAIL_VOID', '이메일무효') + ' ' +
                    payFollowBtn('AUTO_REFUND', '자동환불') + ' ' +
                    payFollowBtn('FORCE_REFUND', '강제환불') + '</td>';
                }
              } else if (c.type === 'accountAccessActions') {
                var actId = row.id != null ? String(row.id) : '';
                var actUn = row.username != null ? String(row.username) : '';
                var actCc = row.compCode != null ? String(row.compCode) : '';
                html += '<td class="text-nowrap">' +
                  '<button type="button" class="btn btn-link btn-sm p-0 hq-acc-edit" data-id="' + escAttr(actId) + '" data-username="' + escAttr(actUn) + '" data-compcode="' + escAttr(actCc) + '">수정</button> ' +
                  '<button type="button" class="btn btn-link btn-sm p-0 text-danger hq-acc-del" data-id="' + escAttr(actId) + '">삭제</button></td>';
              } else if (c.type === 'accountAccessDelete') {
                var delId = row.id != null ? String(row.id) : '';
                html += '<td><button type="button" class="btn btn-link btn-sm p-0 text-danger hq-acc-del" data-id="' + delId + '">삭제</button></td>';
              } else if (url === '/user/userMng' && c.type && String(c.type).indexOf('userMng') === 0) {
                if (c.type === 'userMngUserId') {
                  if (row._draft) {
                    html += '<td><input type="text" class="form-control form-control-sm user-mng-inp" data-field="userId" value="' + escAttr(row.userId || '') + '" autocomplete="off" /></td>';
                  } else {
                    html += '<td>' + escAttr(row.userId || '') + '</td>';
                  }
                } else if (c.type === 'userMngUserNm') {
                  if (row._draft) {
                    html += '<td><input type="text" class="form-control form-control-sm user-mng-inp" data-field="userNm" value="' + escAttr(row.userNm || '') + '" /></td>';
                  } else {
                    html += '<td>' + escAttr(row.userNm || '') + '</td>';
                  }
                } else if (c.type === 'userMngMobile') {
                  html += '<td><input type="text" class="form-control form-control-sm user-mng-inp" data-field="mobile" value="' + escAttr(row.mobile || '') + '" /></td>';
                } else if (c.type === 'userMngAssistantRole') {
                  var ar = row.assistantRoleType || 'MANAGER';
                  var arOpts = ['MANAGER', 'OPERATOR', 'SETTLEMENT', 'TECH'];
                  html += '<td><select class="form-select form-select-sm user-mng-sel" data-field="assistantRoleType">';
                  arOpts.forEach(function (r) {
                    html += '<option value="' + r + '"' + (ar === r ? ' selected' : '') + '>' + r + '</option>';
                  });
                  html += '</select></td>';
                } else if (c.type === 'userMngRoleNm') {
                  html += '<td>' + escAttr(row.roleNm || '') + '</td>';
                } else if (c.type === 'userMngPassword') {
                  var pwdRid = row.id != null ? String(row.id) : '';
                  var canResetPwd = String(row.canResetPassword || 'N') === 'Y';
                  if (row._draft) {
                    html += '<td><input type="password" class="form-control form-control-sm user-mng-pwd" data-field="password" placeholder="8자 이상" autocomplete="new-password" /></td>';
                  } else {
                    html += '<td class="text-nowrap user-mng-pwd-cell"><div class="dropdown d-inline-block">' +
                      '<button type="button" class="btn btn-sm btn-outline-secondary dropdown-toggle user-mng-pwd-menu-toggle py-0 px-2" data-bs-toggle="dropdown" data-bs-auto-close="true" aria-expanded="false">비밀번호</button>' +
                      '<ul class="dropdown-menu dropdown-menu-end user-mng-pwd-dropdown shadow-sm">';
                    if (canResetPwd) {
                      html += '<li><button type="button" class="dropdown-item py-1 small user-reset-pwd-btn" data-id="' + pwdRid + '">초기화</button></li>';
                    } else {
                      html += '<li><span class="dropdown-item disabled py-1 small text-muted mb-0">초기화 권한 없음</span></li>';
                    }
                    html += '</ul></div></td>';
                  }
                } else if (c.type === 'userMngOtp') {
                  var oid = row.id != null ? String(row.id) : '';
                  var canResetO = String(row.canResetPassword || 'N') === 'Y';
                  var oy = String(row.otpRegisteredYn || 'N') === 'Y';
                  html += '<td class="text-nowrap"><span class="badge rounded-pill user-mng-otp-badge ' + (oy ? 'bg-success' : 'bg-secondary') + '">' + (oy ? '등록' : '미등록') + '</span>';
                  if (!row._draft && canResetO) {
                    html += ' <button type="button" class="btn btn-link btn-sm p-0 text-info user-otp-reset-btn" data-id="' + oid + '">초기화</button>';
                  }
                  html += '</td>';
                } else if (c.type === 'userMngStatus') {
                  var us = row.userStatus || 'ACTIVE';
                  html += '<td><select class="form-select form-select-sm user-mng-sel" data-field="userStatus">';
                  html += '<option value="ACTIVE"' + (us === 'ACTIVE' ? ' selected' : '') + '>사용</option>';
                  html += '<option value="INACTIVE"' + (us === 'INACTIVE' ? ' selected' : '') + '>미사용</option>';
                  html += '<option value="SUSPENDED"' + (us === 'SUSPENDED' ? ' selected' : '') + '>영구정지</option>';
                  html += '</select></td>';
                } else if (c.type === 'userMngDraftDelete') {
                  if (row._draft) {
                    html += '<td><button type="button" class="btn btn-sm btn-outline-danger user-mng-draft-remove" data-temp-id="' + escAttr(row._tempId) + '">삭제</button></td>';
                  } else {
                    html += '<td></td>';
                  }
                } else if (c.type === 'userMngInactiveReason') {
                  html += '<td><input type="text" class="form-control form-control-sm user-mng-inp" data-field="inactiveReason" value="' + escAttr(row.inactiveReason || '') + '" /></td>';
                } else {
                  html += '<td></td>';
                }
              } else if (c.type === 'userResetPassword') {
                var resetId = row.id != null ? String(row.id) : '';
                var canReset = String(row.canResetPassword || 'N') === 'Y';
                html += '<td>' + (canReset
                  ? ('<button type="button" class="btn btn-link btn-sm p-0 text-info user-reset-pwd-btn" data-id="' + resetId + '">초기화</button>')
                  : '<span class="text-muted">-</span>') + '</td>';
              } else if (c.type === 'userDelete') {
                var userDelId = row.id != null ? String(row.id) : '';
                var canManageUsers = String(row.canManageUsers || 'N') === 'Y';
                html += '<td>' + (canManageUsers
                  ? ('<button type="button" class="btn btn-link btn-sm p-0 text-danger user-del-btn" data-id="' + userDelId + '">삭제</button>')
                  : '<span class="text-muted">-</span>') + '</td>';
              } else if (c.type === 'commissionInlineActions' && url === '/commission/commisionList') {
                html += '<td class="text-nowrap"><button type="button" class="btn btn-sm btn-primary commission-inline-save me-1">저장</button><button type="button" class="btn btn-sm btn-outline-danger commission-inline-clear">삭제</button></td>';
              } else if (c.type === 'pgApiMngRowActions' && url === '/hq/pgApiMng') {
                html += '<td class="text-center text-nowrap">' +
                  '<button type="button" class="btn btn-sm btn-outline-primary hq-pg-row-edit py-0 px-1 me-1" data-row-idx="' + idx + '" title="연동 자격 수정">수정</button>' +
                  '<button type="button" class="btn btn-sm btn-outline-danger hq-pg-row-del py-0 px-1" data-row-idx="' + idx + '" title="PG 연동 삭제">삭제</button></td>';
              } else if (url === '/hq/pgApiMng' && c.key === 'integrationScopeLabel') {
                var ik = String(row.integKind || '');
                var scopeHtml;
                if (ik === 'MULTI') {
                  var badges = [];
                  if (String(row.integNotiYn || '').toUpperCase() === 'Y') badges.push('<span class="badge rounded-pill pg-scope-pastel-noti">노티</span>');
                  if (String(row.integUrlPayYn || '').toUpperCase() === 'Y') badges.push('<span class="badge rounded-pill pg-scope-pastel-url">URL</span>');
                  if (String(row.integWebChatbotYn || '').toUpperCase() === 'Y') badges.push('<span class="badge rounded-pill pg-scope-pastel-chatbot">챗봇</span>');
                  if (String(row.integApiYn || '').toUpperCase() === 'Y') badges.push('<span class="badge rounded-pill pg-scope-pastel-api">API</span>');
                  scopeHtml = badges.length
                    ? ('<span class="pg-api-mng-scope-badges">' + badges.join('') + '</span>')
                    : '<span class="text-muted">—</span>';
                } else if (row.integKindLabel) {
                  var kl = String(row.integKindLabel);
                  var ikSingle = String(row.integKind || '').toUpperCase();
                  var pastelSingle = 'pg-scope-pastel-multi';
                  if (ikSingle === 'NOTI') pastelSingle = 'pg-scope-pastel-noti';
                  else if (ikSingle === 'URL_PAY') pastelSingle = 'pg-scope-pastel-url';
                  else if (ikSingle === 'WEB_CHATBOT') pastelSingle = 'pg-scope-pastel-chatbot';
                  else if (ikSingle === 'API') pastelSingle = 'pg-scope-pastel-api';
                  scopeHtml = '<span class="badge rounded-pill ' + pastelSingle + '">' + escAttr(kl) + '</span>';
                } else {
                  scopeHtml = '<span class="text-muted">—</span>';
                }
                html += '<td class="pg-api-mng-scope-cell text-center">' + scopeHtml + '</td>';
              } else if (url === '/hq/pgApiMng' && c.key === 'endpointsSummary') {
                var pe = row.primaryEndpoint != null ? String(row.primaryEndpoint) : '';
                if (!pe.trim()) {
                  html += '<td class="pg-api-mng-ep-cell"><span class="text-muted">—</span></td>';
                } else {
                  var fullPe = pe.trim();
                  var escPe = escAttr(fullPe);
                  html += '<td class="pg-api-mng-ep-cell" title="' + escPe + '"><span class="font-monospace pg-api-mng-ep-ellipsis">' + escAttr(fullPe) + '</span></td>';
                }
              } else if (url === '/hq/pgApiMng' && c.key === 'operationalYn') {
                var pgCdEsc = escAttr(row.pgCd || '');
                var opChecked = String(row.operationalYn || '') === 'Y' ? ' checked' : '';
                var useInactive = String(row.useYn || 'Y') === 'N';
                var useDisabled = useInactive ? ' disabled' : '';
                var opTdClass = 'text-center' + (useInactive ? ' hq-pg-operational-cell--inactive' : '');
                var opTitle = useInactive ? '미사용 PG는 운영 지정 불가' : '결제 운영(가맹점 연동·PG 선택 노출)';
                html += '<td class="' + opTdClass + '"><input type="checkbox" class="form-check-input hq-pg-operational-cb" data-pg-cd="' + pgCdEsc + '" title="' + escAttr(opTitle) + '"' + opChecked + useDisabled + '></td>';
              } else if (url === '/hq/pgApiMng' && c.key === 'sandboxYn') {
                var sbRaw = String(row.sandboxYn || 'Y').toUpperCase();
                var sbLong = (sbRaw === 'N') ? 'Production' : 'Sandbox';
                var sbShort = (sbRaw === 'N') ? 'Prd' : 'Sbx';
                html += '<td class="text-center text-nowrap" title="' + escAttr(sbLong) + '">' + sbShort + '</td>';
              } else if (url === '/hq/pgApiMng' && (c.key === 'hasApiKey' || c.key === 'hasMd5Key')) {
                var ynKey = String(row[c.key] || 'N').toUpperCase() === 'Y' ? 'Y' : 'N';
                html += '<td class="text-center text-nowrap fw-semibold">' + ynKey + '</td>';
              } else {
                var val = row[c.key] !== undefined && row[c.key] !== null ? String(row[c.key]) : '';
                var cellClass = '';
                var isPayScr = url === '/calc/payList' || url === '/calc/payNotiList' || url === '/calc/paySuccessList' || url === '/calc/payFailList' || url === '/calc/payRefundList' || url === '/calc/payForceRefundList' || url === '/calc/payCancelList' || url === '/calc/payVoidList' || url === '/calc/offsetCancList' || url === '/pay/easyPay' || url === '/pay/chatbotPay';
                if (url === '/calc/calcGmList' || url === '/settlement/franchiseList'
                    || url === '/calc/paySettlementHoldList' || url === '/settlement/paySettlementHoldList'
                    || url === '/pay/payHoldList' || url === '/settlement/holdList') {
                  var gmCls = [];
                  if (['amount', 'feeCnt', 'feeRate', 'feeAmt', 'feeVat', 'holdRate', 'holdAmt', 'settleAmt', 'perTxFeeAmt', 'settlementPerTxFeeAmt', 'extraFeesAmt'].indexOf(c.key) >= 0) gmCls.push('text-end');
                  if (c.key === 'curType') gmCls.push('text-center', 'text-nowrap');
                  if (['calcDt', 'approveDt', 'cancelDt'].indexOf(c.key) >= 0) gmCls.push('text-nowrap');
                  if (['compNm', 'merchantNm', 'payoutHoldRemark'].indexOf(c.key) >= 0) gmCls.push('text-start');
                  if (c.key === 'payoutHoldRemark') gmCls.push('small', 'text-break');
                  if (gmCls.length) cellClass = ' class="' + gmCls.join(' ') + '"';
                } else if (url === '/commission/commisionList') {
                  var commCls = [];
                  var commNameKeys = ['compNm', 'hqNm', 'regionalNm', 'masterNm', 'branchNm', 'agencyNm', 'salesOfficeNm'];
                  if (['hqRate', 'regionalRate', 'masterRate', 'branchRate', 'agencyRate', 'salesOfficeRate', 'totalRate', 'hqPerTxFee', 'regionalPerTxFee', 'masterPerTxFee', 'branchPerTxFee', 'agencyPerTxFee', 'salesOfficePerTxFee', 'totalPerTxFee'].indexOf(c.key) >= 0) commCls.push('text-center');
                  if (commNameKeys.indexOf(c.key) >= 0 || c.key === 'compId' || c.key === 'policyCur') commCls.push('text-center', 'commission-grid-wrapcell');
                  if (c.key === 'totalNm') commCls.push('text-center', 'text-nowrap');
                  if (commCls.length) cellClass = ' class="' + commCls.join(' ') + '"';
                } else if (isPayScr) {
                  var payCls = [];
                  if (['pgApproveAmt', 'payAmount', 'feeCnt', 'feeRate', 'feeAmt', 'feeVat', 'holdRate', 'holdAmt', 'settleAmt', 'chillAmount', 'icopayAmt', 'chillFeeAmt', 'totalAmt'].indexOf(c.key) >= 0) payCls.push('text-end');
                  var payDualTimeKeys = ['trnTime', 'payDttm', 'payAprv', 'holdDttm', 'calcDt', 'approveDt', 'cancelDt', 'payCompletedAt'];
                  if (['payAprv', 'holdDttm', 'calcDt', 'payDttm', 'trnDate', 'trnTime', 'payCompletedAt', 'trnId', 'chillTransactionId', 'routeNo', 'notifyChannelType'].indexOf(c.key) >= 0) {
                    if (payDualTimeKeys.indexOf(c.key) < 0) {
                      payCls.push('text-nowrap');
                    }
                  }
                  if (payDualTimeKeys.indexOf(c.key) >= 0) {
                    payCls.push('pay-grid-time-dual');
                  }
                  if (['compNm', 'merchantNm', 'compDivCode9', 'chillCustomer', 'productNm'].indexOf(c.key) >= 0) payCls.push('text-start');
                  if (c.key === 'chillPaymentStatus' && url === '/calc/payList') payCls.push('text-center');
                  if (payCls.length) cellClass = ' class="' + payCls.join(' ') + '"';
                } else if (url === '/calc/chillPayTrList' || url === '/calc/chillPaySettlementList') {
                  var trCls = [];
                  if (['amount', 'fee', 'discount', 'totalAmount', 'refundAmount'].indexOf(c.key) >= 0) trCls.push('text-end');
                  var trNowrap = ['transactionDate', 'paymentDate', 'transactionId', 'orderNo'];
                  if (url === '/calc/chillPayTrList') {
                    trNowrap = trNowrap.concat(['trnDate', 'routeNo', 'compId']);
                  }
                  var trDualTimeKeys = (url === '/calc/chillPayTrList' || url === '/calc/chillPaySettlementList') ? ['trnTime', 'payCompletedAt'] : [];
                  if (trNowrap.indexOf(c.key) >= 0) trCls.push('text-nowrap');
                  if (trDualTimeKeys.indexOf(c.key) >= 0) trCls.push('pay-grid-time-dual');
                  var trStart = ['merchant', 'customer', 'description', 'paymentChannel'];
                  if (url === '/calc/chillPayTrList') {
                    trStart = ['compNm'].concat(trStart);
                  }
                  if (trStart.indexOf(c.key) >= 0) trCls.push('text-start');
                  if (c.key === 'status') trCls.push('text-center');
                  if (trCls.length) cellClass = ' class="' + trCls.join(' ') + '"';
                } else if (url === '/calc/calcList' || url === '/settlement/distributionList') {
                  var distCls = [];
                  if (['aprvCnt', 'aprvAmt', 'aprvFeeCnt', 'aprvFeePct', 'aprvFeeSum', 'aprvFeeVat', 'canCnt', 'canAmt', 'canFeeCnt', 'canFeePct', 'canFeeSum', 'canFeeVat', 'settleAmt'].indexOf(c.key) >= 0) distCls.push('text-end');
                  if (['settleMonth', 'orgDivNm', 'hqNm', 'regionalNm', 'masterNm', 'branchNm', 'agencyNm', 'compId'].indexOf(c.key) >= 0) distCls.push('text-nowrap');
                  if (distCls.length) cellClass = ' class="' + distCls.join(' ') + '"';
                } else if (url === '/calc/collateralList' || url === '/settlement/collateralList') {
                  var collCls = [];
                  if (['reserveAmt', 'remainingBizDays', 'holdBusinessDays'].indexOf(c.key) >= 0) collCls.push('text-end');
                  if (collCls.length) cellClass = ' class="' + collCls.join(' ') + '"';
                } else if (url === '/calc/unpaidMng' || url === '/settlement/unpaidMng') {
                  var upCls = [];
                  if (['totalAmount', 'deductCnt', 'appliedAmount'].indexOf(c.key) >= 0) upCls.push('text-end');
                  if (c.key === 'curType') upCls.push('text-center', 'text-nowrap');
                  if (upCls.length) cellClass = ' class="' + upCls.join(' ') + '"';
                } else if (url === '/calc/feeList' || url === '/settlement/feeList') {
                  var feeCls = [];
                  if (['amount', 'txnFixedFeesSum', 'pctFeesSum', 'usdtFee', 'fxFee', 'fee3dsFee', 'failFee', 'cancelFee', 'voidFee', 'manualVoidFee', 'refundFee', 'chargebackFee', 'rollingPctPlain', 'rollingDays', 'rollingHoldEst', 'totalFee', 'feeVat', 'expectedPayout', 'settlementAmt'].indexOf(c.key) >= 0) feeCls.push('text-end');
                  if (['payCur', 'policyCur', 'curType', 'vatAppliedYn'].indexOf(c.key) >= 0) feeCls.push('text-center');
                  if (c.key === 'statusNm') feeCls.push('text-center');
                  if (c.key === 'trnTime') feeCls.push('pay-grid-time-dual');
                  if (['trnDate', 'trnId', 'chillTransactionId', 'routeNo'].indexOf(c.key) >= 0) feeCls.push('text-nowrap');
                  if (feeCls.length) cellClass = ' class="' + feeCls.join(' ') + '"';
                } else if (url === '/calc/compPointMngList') {
                  var recCls2 = [];
                  if (['recallAmount', 'remainingAmount', 'appliedAmount'].indexOf(c.key) >= 0) recCls2.push('text-end');
                  if (c.key === 'curType') recCls2.push('text-center', 'text-nowrap');
                  if (recCls2.length) cellClass = ' class="' + recCls2.join(' ') + '"';
                } else if (url === '/settlement/recallMng') {
                  var recCls = [];
                  if (['settleAmt', 'recallAmt', 'deductAmt'].indexOf(c.key) >= 0) recCls.push('text-end');
                  if (c.key === 'curType') recCls.push('text-center', 'text-nowrap');
                  if (recCls.length) cellClass = ' class="' + recCls.join(' ') + '"';
                } else if (url === '/calc/exCalcList' || url === '/settlement/execute') {
                  var exCalcCls = [];
                  if (c.key === 'targetPeriodText') exCalcCls.push('text-nowrap');
                  if (c.key === 'curType') exCalcCls.push('text-center', 'text-nowrap');
                  if (exCalcCls.length) cellClass = ' class="' + exCalcCls.join(' ') + '"';
                }
                if (isCompMngTree && c.key === 'rowNo') {
                  html += '<td' + cellClass + '>' + (val || '') + '</td>';
                } else if (c.key === 'rowNo') {
                  html += '<td' + cellClass + '>' + (rowNoBase + idx + 1) + '</td>';
                } else if (c.key === 'otpRegisteredYn') {
                  var oy = val === 'Y';
                  html += '<td' + cellClass + '><span class="badge rounded-pill ' + (oy ? 'bg-success' : 'bg-secondary') + '">' + (oy ? '등록' : '미등록') + '</span></td>';
                } else if (isCompMngTree && c.key === 'compId') {
                  var depth = (row.depth != null && row.depth !== '') ? parseInt(row.depth, 10) : 0;
                  if (isNaN(depth)) depth = 0;
                  var px = 18 + depth * 18;
                  var expanded = p._treeExpanded && p._treeExpanded.has(rowId);
                  var compDivRaw = row.compDiv != null ? String(row.compDiv).trim() : '';
                  var orgLevelClass = 'tree-org-unknown';
                  if (/^[A-Z][A-Z0-9_]*$/.test(compDivRaw)) {
                    orgLevelClass = 'tree-org-' + compDivRaw;
                  }
                  var merchFolderTone = '';
                  var merchToneSlug = '';
                  if (compDivRaw === 'MERCHANT' && row.merchantTreeFolderTone) {
                    var toneRaw = String(row.merchantTreeFolderTone).trim().toUpperCase();
                    if (/^(DIRECT|SALES|AGENCY|BRANCH|OTHER|REGIONAL|HEADQUARTERS)$/.test(toneRaw)) {
                      merchToneSlug = toneRaw.toLowerCase();
                      merchFolderTone = ' tree-merch-folder--' + merchToneSlug;
                    }
                  }
                  var folderSvg = '<svg class="tree-icon tree-icon-folder ' + orgLevelClass + merchFolderTone + '" width="16" height="16" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M10 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"/></svg>';
                  var docSvg = '<svg class="tree-icon tree-icon-doc ' + orgLevelClass + merchFolderTone + '" width="16" height="16" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6z"/></svg>';
                  var icon = hasChildren ? folderSvg : docSvg;
                  var escTreeAttr = function (s) {
                    return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;');
                  };
                  var merchToneData = merchToneSlug ? (' data-merch-tone="' + escTreeAttr(merchToneSlug) + '"') : '';
                  var toggle = hasChildren
                    ? '<span class="tree-toggle ' + orgLevelClass + merchFolderTone + ' ' + (expanded ? 'expanded' : 'collapsed') + '" data-id="' + escTreeAttr(rowId) + '" data-org-level="' + escTreeAttr(compDivRaw) + '"' + merchToneData + ' title="' + (expanded ? '접기' : '펼치기') + '">' + (expanded ? '\u25BC' : '\u25B6') + '</span>'
                    : '<span class="tree-toggle-placeholder"></span>';
                  var compTreeValEsc = escHtmlBody(val || '');
                  html += '<td class="tree-comp-cell" style="padding-left:' + px + 'px"><div class="tree-comp-cell-inner">' + icon + toggle + '<span class="pg-comp-grid-clamp" title="' + escHtmlTitle(val || '') + '">' + compTreeValEsc + '</span></div></td>';
                } else if (isCompMngTree && c.key === 'compNm') {
                  var compNmPlain = val == null ? '' : String(val);
                  html += '<td class="text-start align-middle"><span class="pg-comp-grid-clamp" title="' + escHtmlTitle(compNmPlain) + '">' + escHtmlBody(compNmPlain) + '</span></td>';
                } else if (isCompMngTree && c.key === 'siteRoot') {
                  var sr0 = (val == null || val === '' || val === '-') ? '-' : String(val);
                  if (sr0 === '-') {
                    html += '<td class="text-center align-middle small">-</td>';
                  } else {
                    html += '<td class="text-center align-middle small"><span class="pg-comp-grid-clamp" title="' + escHtmlTitle(sr0) + '">' + escHtmlBody(sr0) + '</span></td>';
                  }
                } else if (url === '/comp/compMng' && c.key === 'compNm') {
                  html += '<td class="text-start align-middle">' + (val || '') + '</td>';
                } else if (url === '/calc/calcList' || url === '/settlement/distributionList') {
                  var distFmtKeys = ['aprvCnt', 'aprvAmt', 'aprvFeeCnt', 'aprvFeeSum', 'aprvFeeVat', 'canCnt', 'canAmt', 'canFeeCnt', 'canFeeSum', 'canFeeVat', 'settleAmt'];
                  var distPctKeys = ['aprvFeePct', 'canFeePct'];
                  var showVal = val;
                  if (distFmtKeys.indexOf(c.key) >= 0) showVal = fmtNum(row[c.key]);
                  else if (distPctKeys.indexOf(c.key) >= 0) showVal = val;
                  html += '<td' + cellClass + '>' + showVal + '</td>';
                } else if (url === '/calc/collateralList' || url === '/settlement/collateralList') {
                  var collShow = val;
                  if (c.key === 'reserveAmt') collShow = fmtNum(row[c.key]);
                  html += '<td' + cellClass + '>' + collShow + '</td>';
                } else if (url === '/calc/unpaidMng' || url === '/settlement/unpaidMng') {
                  var upShow = val;
                  if (['totalAmount', 'deductCnt', 'appliedAmount'].indexOf(c.key) >= 0) upShow = fmtNum(row[c.key]);
                  html += '<td' + cellClass + '>' + upShow + '</td>';
                } else if (url === '/calc/feeList' || url === '/settlement/feeList') {
                  var feeShow = val;
                  var feeCurCode = row.curType != null && String(row.curType).trim() !== '' ? String(row.curType).trim()
                    : (row.payCur != null && String(row.payCur).trim() !== '' ? String(row.payCur).trim() : 'KRW');
                  if (c.key === 'usdtFee' || c.key === 'fxFee' || c.key === 'fee3dsFee') {
                    var feeSpecNum = asNum(row[c.key]);
                    feeShow = feeSpecNum !== 0 ? fmtLedgerAmount(row[c.key], feeCurCode, feeFmtByCur) : '—';
                  } else if (['amount', 'txnFixedFeesSum', 'pctFeesSum', 'failFee', 'cancelFee', 'voidFee', 'manualVoidFee', 'refundFee', 'chargebackFee', 'rollingHoldEst', 'totalFee', 'feeVat', 'expectedPayout', 'settlementAmt'].indexOf(c.key) >= 0) {
                    feeShow = fmtLedgerAmount(row[c.key], feeCurCode, feeFmtByCur);
                  } else if (['rollingPctPlain'].indexOf(c.key) >= 0) {
                    feeShow = (val != null && val !== '') ? String(val) : '0';
                  } else if (c.key === 'rollingDays') {
                    feeShow = (val != null && val !== '') ? String(val) : '0';
                  }
                  var feeTdInner = feeShow;
                  if (c.key === 'trnTime' && val && String(val).indexOf('\n') !== -1) {
                    function escFeeGridLine(s) {
                      return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
                    }
                    var feeTimeLines = String(val).split('\n');
                    if (feeTimeLines.length === 2 && /^\s*JP\s/.test(feeTimeLines[0]) && /^\s*TH\s/.test(feeTimeLines[1])) {
                      feeTdInner = '<span class="pay-grid-time-line pay-grid-time-line--jp">' + escFeeGridLine(feeTimeLines[0]) + '</span><br>' +
                        '<span class="pay-grid-time-line pay-grid-time-line--th">' + escFeeGridLine(feeTimeLines[1]) + '</span>';
                    } else {
                      feeTdInner = feeTimeLines.map(escFeeGridLine).join('<br>');
                    }
                  }
                  if (c.key === 'statusNm') {
                    var pgUiFee = window.PG_UI || {};
                    if (typeof pgUiFee.payGridStatusBadge === 'function' && typeof pgUiFee.resolvePayRowTone === 'function') {
                      feeTdInner = pgUiFee.payGridStatusBadge(row.statusNm != null ? String(row.statusNm) : '', pgUiFee.resolvePayRowTone(row));
                    }
                  }
                  html += '<td' + cellClass + '>' + feeTdInner + '</td>';
                } else if (url === '/calc/compPointMngList') {
                  var recShow2 = val;
                  if (['recallAmount', 'remainingAmount', 'appliedAmount'].indexOf(c.key) >= 0) recShow2 = fmtNum(row[c.key]);
                  html += '<td' + cellClass + '>' + recShow2 + '</td>';
                } else if (url === '/settlement/recallMng') {
                  var recShow = val;
                  if (['settleAmt', 'recallAmt', 'deductAmt'].indexOf(c.key) >= 0) recShow = fmtNum(row[c.key]);
                  html += '<td' + cellClass + '>' + recShow + '</td>';
                } else if (url === '/commission/commisionList') {
                  var cClickEditable = ['hqRate', 'regionalRate', 'masterRate', 'branchRate', 'agencyRate', 'salesOfficeRate', 'totalRate', 'hqPerTxFee', 'regionalPerTxFee', 'masterPerTxFee', 'branchPerTxFee', 'agencyPerTxFee', 'salesOfficePerTxFee', 'totalPerTxFee'];
                  if (cClickEditable.indexOf(c.key) >= 0) {
                    var cShowVal = (val || '0');
                    var cEscVal = String(cShowVal).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
                    html += '<td class="commission-inline-cell text-center" data-key="' + c.key + '" data-value="' + cEscVal + '" title="클릭하여 수정">' +
                      '<span class="commission-inline-view">' + cEscVal + '</span></td>';
                  } else if (c.key === 'applyDt') {
                    var cDateVal = val ? String(val).substring(0, 10) : '';
                    html += '<td><input type="date" class="form-control form-control-sm commission-inline-input text-center" data-key="applyDt" value="' + String(cDateVal).replace(/"/g, '&quot;') + '"></td>';
                  } else {
                    if ((c.key === 'hqNm' || c.key === 'regionalNm' || c.key === 'masterNm' || c.key === 'branchNm' || c.key === 'agencyNm' || c.key === 'salesOfficeNm') && (!val || !String(val).trim())) {
                      var idMap = { hqNm: 'hqId', regionalNm: 'regionalId', masterNm: 'masterId', branchNm: 'branchId', agencyNm: 'agencyId', salesOfficeNm: 'salesOfficeId' };
                      var fb = row[idMap[c.key]];
                      val = (fb != null && String(fb).trim() !== '') ? String(fb) : '-';
                    }
                    html += '<td' + cellClass + '>' + val + '</td>';
                  }
                } else {
                  var cellInner = val;
                  var pgUi = window.PG_UI || {};
                  function escPayGridLine(s) {
                    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
                  }
                  /** JP/TH 이중 시각: 2줄 + JP 줄 파란색(결제관리·통합내역 공통) */
                  function formatPayGridDualTimeHtml(raw) {
                    if (raw == null || raw === '') return '';
                    var s = String(raw);
                    if (s.indexOf('\n') === -1) return escPayGridLine(s);
                    var lines = s.split('\n');
                    if (lines.length === 2 && /^\s*JP\s/.test(lines[0]) && /^\s*TH\s/.test(lines[1])) {
                      return '<span class="pay-grid-time-line pay-grid-time-line--jp">' + escPayGridLine(lines[0]) + '</span><br>' +
                        '<span class="pay-grid-time-line pay-grid-time-line--th">' + escPayGridLine(lines[1]) + '</span>';
                    }
                    return lines.map(escPayGridLine).join('<br>');
                  }
                  var payDualTimeKeys = ['trnTime', 'payDttm', 'payAprv', 'holdDttm', 'calcDt', 'approveDt', 'cancelDt', 'payCompletedAt'];
                  var chillDualTimeKeys = ['trnTime', 'payCompletedAt'];
                  if (val && String(val).indexOf('\n') !== -1) {
                    if (isPayScr && payDualTimeKeys.indexOf(c.key) >= 0) {
                      cellInner = formatPayGridDualTimeHtml(val);
                    } else if ((url === '/calc/chillPayTrList' || url === '/calc/chillPaySettlementList') && chillDualTimeKeys.indexOf(c.key) >= 0) {
                      cellInner = formatPayGridDualTimeHtml(val);
                    }
                  }
                  if (url === '/calc/payList' && c.key === 'chillPaymentStatus' && typeof pgUi.payGridStatusBadge === 'function' && typeof pgUi.resolvePayRowTone === 'function') {
                    cellInner = pgUi.payGridStatusBadge(val, pgUi.resolvePayRowTone(row));
                  } else if ((url === '/calc/chillPayTrList' || url === '/calc/chillPaySettlementList') && c.key === 'status' && typeof pgUi.payGridStatusBadge === 'function' && typeof pgUi.resolveChillTrRowTone === 'function') {
                    cellInner = pgUi.payGridStatusBadge(val, pgUi.resolveChillTrRowTone(row));
                  }
                  if (isCompMngTree && c.key === 'calcProcType') {
                    var cpDisp = val != null ? String(val).trim() : '';
                    var cpShow = cpDisp || '-';
                    var cpEsc = escHtmlBody(cpShow);
                    if (cpDisp === '자동') {
                      cellInner = '<span class="pg-calc-proc--auto fw-semibold" title="' + escHtmlTitle(cpShow) + '">' + cpEsc + '</span>';
                    } else if (cpDisp === '펌뱅킹') {
                      cellInner = '<span class="pg-calc-proc--fumbanking fw-semibold" title="' + escHtmlTitle(cpShow) + '">' + cpEsc + '</span>';
                    } else {
                      cellInner = '<span title="' + escHtmlTitle(cpShow) + '">' + cpEsc + '</span>';
                    }
                  }
                  if (isCompMngTree && typeof cellInner === 'string' && cellInner.indexOf('<') === -1 && c.key) {
                    cellInner = '<span class="pg-comp-grid-clamp" title="' + escHtmlTitle(cellInner) + '">' + escHtmlBody(cellInner) + '</span>';
                  }
                  html += '<td' + cellClass + '>' + cellInner + '</td>';
                }
              }
            });
            html += '</tr>';
          });
          tbody.innerHTML = html;
          if (url !== '/user/userMng') {
            injectTableRowResizeHandles(tbody, thLen);
          }
          if (isCompMngTree && list.length) {
            p._treeList = list;
            applyTreeVisibility(p, tbody, list);
          }
        }
        var cntEl = p.querySelector('#summary_건수, .summary-count, [data-summary="건수"]');
        if (cntEl) cntEl.textContent = (cntEl.id === 'summary_건수' ? '건수: ' : '') + total;
        if (url === '/calc/calcGmList' || url === '/settlement/franchiseList'
            || url === '/calc/paySettlementHoldList' || url === '/settlement/paySettlementHoldList'
            || url === '/pay/payHoldList' || url === '/settlement/holdList') {
          var sum = { amount: 0, feeAmt: 0, feeVat: 0, holdAmt: 0, settleAmt: 0 };
          list.forEach(function (r) {
            sum.amount += asNum(r.amount);
            sum.feeAmt += asNum(r.feeAmt);
            sum.feeVat += asNum(r.feeVat);
            sum.holdAmt += asNum(r.holdAmt);
            sum.settleAmt += asNum(r.settleAmt);
          });
          setSummaryText(p, '금액', fmtNum(sum.amount));
          setSummaryText(p, '수수료금액', fmtNum(sum.feeAmt));
          setSummaryText(p, '수수료부가세', fmtNum(sum.feeVat));
          setSummaryText(p, '보류금액', fmtNum(sum.holdAmt));
          setSummaryText(p, '정산금액', fmtNum(sum.settleAmt));
        }
        if (url === '/calc/calcList' || url === '/settlement/distributionList') {
          var distSum = { settleAmt: 0, feeSum: 0 };
          list.forEach(function (r) {
            distSum.settleAmt += asNum(r.settleAmt);
            distSum.feeSum += asNum(r.hqFee) + asNum(r.regionalFee) + asNum(r.masterFee) + asNum(r.branchFee) + asNum(r.agencyFee);
          });
          setSummaryText(p, 'Total', String(total));
          setSummaryText(p, '정산금액', fmtNum(distSum.settleAmt));
          setSummaryText(p, '수수료', fmtNum(distSum.feeSum));
          setSummaryText(p, '지급액', fmtNum(distSum.settleAmt));
        }
        if (url === '/calc/feeList' || url === '/settlement/feeList') {
          var feeSum = { totalFee: 0, vat: 0, expected: 0, settlement: 0 };
          list.forEach(function (r) {
            feeSum.totalFee += asNum(r.totalFee);
            feeSum.vat += asNum(r.feeVat);
            feeSum.expected += asNum(r.expectedPayout);
            feeSum.settlement += asNum(r.settlementAmt);
          });
          setSummaryText(p, '총수수료', fmtNum(feeSum.totalFee));
          setSummaryText(p, '부가세', fmtNum(feeSum.vat));
          setSummaryText(p, '지급예상합', fmtNum(feeSum.expected));
          setSummaryText(p, '정산액합', fmtNum(feeSum.settlement));
        }
        if (url === '/calc/compPointMngList') {
          var rc2 = { recall: 0, rem: 0 };
          list.forEach(function (r) { rc2.recall += asNum(r.recallAmount); rc2.rem += asNum(r.remainingAmount); });
          setSummaryText(p, '환수금액', fmtNum(rc2.recall));
          setSummaryText(p, '잔여', fmtNum(rc2.rem));
        }
        if (url === '/settlement/recallMng') {
          var rc = { recall: 0 };
          list.forEach(function (r) { rc.recall += asNum(r.recallAmt); });
          setSummaryText(p, '환수금액', fmtNum(rc.recall));
        }
        if (url === '/calc/settlementReport' || url === '/settlement/settlementReport') {
          var repSub = params.searchReportSub || 'AGG';
          if (repSub === 'SUM' && list.length) {
            var r0 = list[0];
            setSummaryText(p, '건수', '1');
            setSummaryText(p, '결제액', fmtNum(r0.grossPay));
            setSummaryText(p, '환불', fmtNum(r0.refundAmt));
            setSummaryText(p, '순액', fmtNum(r0.netPay));
            setSummaryText(p, '정산금', fmtNum(r0.settlementAmt));
          } else {
            var tr = { gross: 0, ref: 0, net: 0, st: 0 };
            list.forEach(function (r) {
              if (repSub === 'EXE' || repSub === 'RST') {
                tr.gross += asNum(r.approveAmt);
                tr.ref += asNum(r.cancelAmt);
                tr.net += asNum(r.netPay);
                tr.st += asNum(r.payAmount);
              } else {
                tr.gross += asNum(r.grossPay);
                tr.ref += asNum(r.refundAmt);
                tr.net += asNum(r.netPay);
                tr.st += asNum(r.settlementAmt);
              }
            });
            setSummaryText(p, '건수', String(total));
            setSummaryText(p, '결제액', fmtNum(tr.gross));
            setSummaryText(p, '환불', fmtNum(tr.ref));
            setSummaryText(p, '순액', fmtNum(tr.net));
            setSummaryText(p, '정산금', fmtNum(tr.st));
          }
        }
        if (url === '/calc/chillPayTrList' || url === '/calc/chillPaySettlementList') {
          setSummaryText(p, '건수', String(total));
        }
        if (payListSearchUrls.indexOf(url) !== -1) {
          updatePayListAggregateBars(p, tid, data && data.meta ? data.meta : null);
        }
        if (url === '/calc/payList' || url === '/calc/payNotiList' || url === '/calc/paySuccessList' || url === '/calc/payFailList' || url === '/calc/payRefundList' || url === '/calc/payForceRefundList' || url === '/calc/payCancelList' || url === '/calc/payVoidList' || url === '/calc/offsetCancList' || url === '/pay/easyPay' || url === '/pay/chatbotPay') {
          var finSrv = data && data.meta && data.meta.payListFinancialSummary;
          if (!finSrv) {
            var ps = { aprv: 0, canc: 0, fee: 0, vat: 0, pay: 0, hold: 0 };
            list.forEach(function (r) {
              var amt = asNum(r.pgApproveAmt != null ? r.pgApproveAmt : r.payAmount);
              var div = String(r.payDivNm || '');
              if (div === '결제') ps.aprv += amt;
              else if (div === '취소') ps.canc += amt;
              ps.fee += asNum(r.feeAmt);
              ps.vat += asNum(r.feeVat);
              ps.pay += asNum(r.settleAmt);
              ps.hold += asNum(r.holdAmt);
            });
            setSummaryText(p, '승인금액', fmtNum(ps.aprv));
            setSummaryText(p, '취소금액', fmtNum(ps.canc));
            setSummaryText(p, '결제금액', fmtNum(ps.aprv - ps.canc));
            setSummaryText(p, '총수수료', fmtNum(ps.fee + ps.vat));
            setSummaryText(p, '보류금액', fmtNum(ps.hold));
            setSummaryText(p, '지급액', fmtNum(ps.pay));
          }
        }
        if (url === '/calc/collateralList' || url === '/settlement/collateralList') {
          var coll = { holdAmt: 0 };
          list.forEach(function (r) {
            if (String(r.status || '').toUpperCase() === 'HOLD') coll.holdAmt += asNum(r.reserveAmt);
          });
          setSummaryText(p, '담보금액', fmtNum(coll.holdAmt));
        }
        if (data && data.size != null && (payListSearchUrls.indexOf(url) !== -1 || settlementListDefaultPageSize25Urls.indexOf(url) !== -1)) {
          var rpSync = p.querySelector('#recordsPerPage');
          if (rpSync) {
            var nsz = parseInt(data.size, 10);
            if (!isNaN(nsz) && nsz > 0) rpSync.value = String(nsz);
          }
        }
        if (window.updatePaging) window.updatePaging(tid, params.page, totalPages, total);
        p.setAttribute('data-last-total-pages', String(totalPages));
        if (url === '/commission/commisionList') {
          p.querySelectorAll('#grid_' + tid + ' tbody tr.table-active').forEach(function (x) { x.classList.remove('table-active'); });
          var prevCompId = p._commissionHistCompId ? String(p._commissionHistCompId).trim() : '';
          if (prevCompId) {
            p._commissionHistCompId = prevCompId;
            var activeTr = tbody ? tbody.querySelector('tr[data-comp-id="' + String(prevCompId).replace(/"/g, '&quot;') + '"]') : null;
            if (activeTr) activeTr.classList.add('table-active');
          } else {
            p._commissionHistCompId = '';
          }
          loadCommissionHistoryGrid(p, tid);
        }
        if (url === '/comp/compMngTree') {
          var returnCompId = '';
          try { returnCompId = sessionStorage.getItem('pg_comp_detail_return_compId') || ''; } catch (e) {}
          if (returnCompId && tbody) {
            try { sessionStorage.removeItem('pg_comp_detail_return_compId'); } catch (e) {}
            var targetTr = null;
            var rows = tbody.querySelectorAll('tr[data-row]');
            for (var r = 0; r < rows.length; r++) {
              var dr = rows[r].getAttribute('data-row');
              if (dr) {
                try {
                  var rowData = JSON.parse(decodeURIComponent(dr));
                  if (rowData.compId === returnCompId) {
                    targetTr = rows[r];
                    break;
                  }
                } catch (e) {}
              }
            }
            if (targetTr) {
              var chk = targetTr.querySelector('.grid-row-check');
              if (chk) chk.checked = true;
              targetTr.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
              targetTr.classList.add('table-warning');
              setTimeout(function () { if (targetTr) targetTr.classList.remove('table-warning'); }, 2000);
            }
          }
        }
      }).catch(function (err) {
        p._lastGridList = [];
        p._lastGridCols = null;
        var tbody = p.querySelector('#grid_' + tid + ' tbody');
        var rkErr = params.searchReportKind || 'MERCHANT_STMT';
        var colFallback = (cfg.columns && cfg.columns.length) ? cfg.columns.length
          : (rkErr === 'REGIONAL_PAYOUT' && cfg.columnsRegionalPayout && cfg.columnsRegionalPayout.AGG ? cfg.columnsRegionalPayout.AGG.length
            : (cfg.columnsBySub && cfg.columnsBySub.AGG ? cfg.columnsBySub.AGG.length : 8));
        if (tbody) tbody.innerHTML = '<tr><td colspan="' + colFallback + '" class="empty-state-cell text-center text-danger">' + (err && err.message ? err.message : '조회 실패') + '</td></tr>';
        if (payListSearchUrls.indexOf(url) !== -1) updatePayListAggregateBars(p, tid, null);
      }).finally(function () {
        if (dimm) dimm.style.display = 'none';
        if (typeof window.applyPagePermissionToPane === 'function') window.applyPagePermissionToPane(p, url);
        if (window.PG_TABLE_COL_RESIZE && typeof window.PG_TABLE_COL_RESIZE.refreshIn === 'function') {
          window.PG_TABLE_COL_RESIZE.refreshIn(p);
        }
      });
    }
    function asNum(v) {
      if (v == null) return 0;
      if (typeof v === 'number') return v;
      var n = Number(String(v).replace(/,/g, ''));
      return isNaN(n) ? 0 : n;
    }
    function fmtNum(v) {
      try { return Number(v || 0).toLocaleString('ko-KR'); } catch (e) { return String(v || 0); }
    }
    /** 정산·수수료 목록 meta.feeCurrencyFormatByCur 기준 통화별 소수 표시 */
    function fmtLedgerAmount(v, cur, fmtByCur) {
      if (!fmtByCur || typeof fmtByCur !== 'object') return fmtNum(v);
      var k = cur != null && String(cur).trim() !== '' ? String(cur).trim().toUpperCase() : 'KRW';
      var spec = fmtByCur[k];
      var dp = spec && spec.decimalPlaces != null ? parseInt(String(spec.decimalPlaces), 10) : 2;
      if (isNaN(dp) || dp < 0) dp = 2;
      if (dp > 8) dp = 8;
      var n = Number(v);
      if (isNaN(n)) n = 0;
      try {
        return n.toLocaleString('ko-KR', { minimumFractionDigits: dp, maximumFractionDigits: dp });
      } catch (e) {
        return fmtNum(v);
      }
    }
    function setSummaryText(paneEl, key, text) {
      if (!paneEl) return;
      var el = paneEl.querySelector('[data-summary="' + key + '"]');
      if (!el) el = paneEl.querySelector('[id="summary_' + key + '"]');
      if (el) el.textContent = key + ': ' + text;
    }
    pane.querySelectorAll('#searchBtn, .screen-search-btn').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var pageEl = pane.querySelector('#pageCnt');
        if (pageEl) pageEl.value = 1;
        doSearch(pane, tabId, 1);
      });
    });
    pane.querySelectorAll('#receivableRegBtn').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var fuR = pane.getAttribute('formurl') || '';
        if (fuR !== '/calc/unpaidMng' && fuR !== '/settlement/unpaidMng') return;
        var compId = window.prompt('가맹점 코드(compId)', '');
        if (!compId || !String(compId).trim()) return;
        var amtStr = window.prompt('미수금액(원)', '');
        var amount = parseInt(String(amtStr || '').replace(/,/g, '').trim(), 10);
        if (!amount || amount <= 0) { window.alert('금액은 0보다 커야 합니다.'); return; }
        var title = window.prompt('제목(선택)', '미수금') || '미수금';
        var reasonCode = window.prompt('사유코드(선택, 예: CHARGEBACK)', 'MANUAL') || 'MANUAL';
        var memo = window.prompt('메모(선택)', '') || '';
        if (!window.PG_API || !window.PG_API.settlementReceivableCreate) { window.alert('API 미구성'); return; }
        window.PG_API.settlementReceivableCreate({ compId: String(compId).trim(), amount: amount, title: title, reasonCode: reasonCode, memo: memo })
          .then(function () {
            window.alert('등록되었습니다.');
            doSearch(pane, tabId, 1);
          })
          .catch(function (e) { window.alert((e && e.message) ? e.message : '등록 요청 실패'); });
      });
    });
    pane.querySelectorAll('#payoutHoldReleaseBtn').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var fuH = pane.getAttribute('formurl') || '';
        if (fuH !== '/calc/paySettlementHoldList' && fuH !== '/settlement/paySettlementHoldList'
            && fuH !== '/pay/payHoldList' && fuH !== '/settlement/holdList') return;
        var grid = pane.querySelector('#grid_' + tabId);
        if (!grid) return;
        var checked = grid.querySelectorAll('tbody .grid-row-check:checked');
        if (!checked.length) {
          window.alert('해제할 행을 체크하세요.');
          return;
        }
        var ids = [];
        var list = pane._lastGridList || [];
        for (var ci = 0; ci < checked.length; ci++) {
          var trR = checked[ci].closest('tr');
          var idxR = trR ? parseInt(trR.getAttribute('data-row-idx') || '-1', 10) : -1;
          var rowR = (idxR >= 0 && idxR < list.length) ? list[idxR] : null;
          var rid = rowR && rowR.settlementRunId != null && rowR.settlementRunId !== '' ? Number(rowR.settlementRunId) : NaN;
          if (!isNaN(rid) && rid > 0) ids.push(rid);
        }
        if (!ids.length) {
          window.alert('선택한 행에서 정산 실행 ID(settlementRunId)를 읽을 수 없습니다.');
          return;
        }
        if (!window.pgDoubleConfirm || !window.pgDoubleConfirm(
          '선택한 ' + ids.length + '건을 가맹점정산내역(및 유통 집계)에 반영(해제)합니다.\n가맹점의 지급보류 설정은 그대로입니다. 계속할까요?',
          '정말 해제합니다. 서버에 반영됩니다.'
        )) return;
        if (!window.PG_API || !window.PG_API.settlementPayoutHoldRelease) {
          window.alert('API 미구성');
          return;
        }
        var dimmPh = document.getElementById('dimm');
        if (dimmPh) dimmPh.style.display = 'flex';
        window.PG_API.settlementPayoutHoldRelease({ settlementRunIds: ids })
          .then(function (d) {
            var n = (d && d.releasedCount != null) ? d.releasedCount : 0;
            window.alert('처리 완료: ' + n + '건 해제되었습니다.');
            doSearch(pane, tabId, 1);
          })
          .catch(function (e) { window.alert((e && e.message) ? e.message : '해제 요청 실패'); })
          .finally(function () { if (dimmPh) dimmPh.style.display = 'none'; });
      });
    });
    if (!pane._payListRefreshClickDelegated) {
      pane._payListRefreshClickDelegated = true;
      pane.addEventListener('click', function (e) {
        var sdb = e.target && e.target.closest ? e.target.closest('.screen-list-sort-dir-btn') : null;
        if (sdb && pane.contains(sdb)) {
          var fuSort = pane.getAttribute('formurl') || '';
          if (payMgmtSortDirAutoRefreshUrls.indexOf(fuSort) === -1) return;
          e.preventDefault();
          var dir = String(sdb.getAttribute('data-search-order-dir') || 'DESC').trim().toUpperCase();
          if (dir !== 'ASC' && dir !== 'DESC') dir = 'DESC';
          var hid = pane.querySelector('input.screen-list-sort-dir-hidden[name="searchOrderDir"]');
          if (hid) hid.value = dir;
          pane.querySelectorAll('.screen-list-sort-dir-btn').forEach(function (b) {
            var d = String(b.getAttribute('data-search-order-dir') || '').trim().toUpperCase();
            var on = d === dir;
            b.classList.toggle('btn-secondary', on);
            b.classList.toggle('btn-outline-secondary', !on);
          });
          var pgKeepSort = parseInt(pane.getAttribute('data-last-page') || '1', 10) || 1;
          doSearch(pane, tabId, pgKeepSort);
          return;
        }
        var rfb = e.target && e.target.closest ? e.target.closest('#payListRefreshBtn') : null;
        if (!rfb || !pane.contains(rfb)) return;
        e.preventDefault();
        var fu = pane.getAttribute('formurl') || '';
        if (payListSearchUrls.indexOf(fu) === -1) return;
        var pgKeep = parseInt(pane.getAttribute('data-last-page') || '1', 10) || 1;
        doSearch(pane, tabId, pgKeep);
      });
    }
    pane.querySelectorAll('.search-reset-btn').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var fuReset = pane.getAttribute('formurl') || '';
        pane.querySelectorAll('select[name^="search"]').forEach(function (el) { el.selectedIndex = 0; });
        pane.querySelectorAll('input[name^="search"]').forEach(function (el) {
          if (el.type === 'text') el.value = '';
          else if (el.type === 'checkbox') el.checked = false;
          else if (el.type === 'date') el.value = '';
        });
        var pageEl = pane.querySelector('#pageCnt');
        if (pageEl) pageEl.value = 1;
        if (fuReset === '/calc/chillPayTrList') {
          applyChillPayTrListInitMonthSearchRange(pane, function () { doSearch(pane, tabId, 1); });
          return;
        }
        doSearch(pane, tabId, 1);
      });
    });
    if (!pane._pgGridPagingChangeDelegated) {
      pane._pgGridPagingChangeDelegated = true;
      pane.addEventListener('paging-change', function (e) {
        var detail = e.detail || {};
        var pg = detail.page != null ? parseInt(String(detail.page), 10) : NaN;
        doSearch(pane, tabId, !isNaN(pg) && pg >= 1 ? pg : undefined);
      });
    }
    var url = pane.getAttribute('formurl') || '';
    if (url === '/calc/unpaidMng' || url === '/settlement/unpaidMng') {
      var permUnpaid = getPagePermissionForUrl('/calc/unpaidMng');
      var canRecvReg = permUnpaid === 'MODIFY' || permUnpaid === 'DELETE';
      pane.querySelectorAll('#receivableRegBtn').forEach(function (b) {
        b.classList.toggle('d-none', !canRecvReg);
        b.disabled = !canRecvReg;
        b.setAttribute('aria-disabled', canRecvReg ? 'false' : 'true');
      });
    }
    var commissionSignal = undefined;
    /** 수수료 화면: 행 클릭·인라인 편집 시 체크 자동(다중 행 저장) */
    var commissionEnsureRowCheckSelected = function () {};
    if (url === '/commission/commisionList') {
      if (!pane._commissionListenersAbort) {
        pane._commissionListenersAbort = new AbortController();
      }
      commissionSignal = pane._commissionListenersAbort.signal;
      commissionEnsureRowCheckSelected = function (tr0) {
        if (!tr0) return;
        var chk = tr0.querySelector('.grid-row-check');
        if (chk && !chk.checked) chk.checked = true;
      };
    }
    function ensureNotifyTargetPickerModal() {
      var modalEl = document.getElementById('notifyTargetPickerModal');
      if (modalEl) return modalEl;
      var wrap = document.createElement('div');
      wrap.innerHTML =
        '<div class="modal fade" id="notifyTargetPickerModal" tabindex="-1" aria-hidden="true">' +
        '<div class="modal-dialog modal-lg"><div class="modal-content">' +
        '<div class="modal-header"><h5 class="modal-title">노티 대상 선택</h5><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="닫기"></button></div>' +
        '<div class="modal-body">' +
        '<div class="d-flex gap-2 mb-2"><input type="text" class="form-control form-control-sm" id="notifyTargetKeyword" placeholder="대상명/URL 검색">' +
        '<button type="button" class="btn btn-sm btn-primary" id="notifyTargetSearchBtn">검색</button></div>' +
        '<div class="table-responsive"><table class="table table-sm table-bordered mb-0 notify-target-picker-table"><thead><tr id="notifyTargetPickerTheadRow"><th class="notify-picker-select-cell align-middle">선택</th><th class="text-center" style="width:118px">구분</th><th>대상명</th><th>URL</th></tr></thead><tbody id="notifyTargetPickerTbody"></tbody></table></div>' +
        '</div></div></div></div>';
      document.body.appendChild(wrap.firstChild);
      var created = document.getElementById('notifyTargetPickerModal');
      var sb = created && created.querySelector('#notifyTargetSearchBtn');
      if (created && sb && !created._notifySearchBound && window.PG_API && window.PG_API.hqNotifyTargets) {
        created._notifySearchBound = true;
        sb.addEventListener('click', function () {
          window.PG_API.hqNotifyTargets().then(function (list2) {
            var mode = created.getAttribute('data-picker-mode') || 'single';
            if (mode === 'pair' && typeof created._renderPairRows === 'function') {
              created._renderPairRows(list2 || []);
            } else if (typeof created._renderSingleRows === 'function') {
              created._renderSingleRows(list2 || []);
            }
          }).catch(function () {});
        });
      }
      return created;
    }
    function bindNotifyTargetPicker() {
      if (!window.PG_API || !window.PG_API.hqNotifyTargets) return;
      var singleBtns = pane.querySelectorAll('button[data-action="노티선택"]');
      var pairBtns = pane.querySelectorAll('button[data-action="노티쌍선택"]');
      if (singleBtns.length === 0 && pairBtns.length === 0) return;
      function setNotifyPickerThead(modalEl, mode) {
        var theadRow = modalEl.querySelector('#notifyTargetPickerTheadRow');
        if (!theadRow) return;
        modalEl.setAttribute('data-picker-mode', mode);
        if (mode === 'pair') {
          theadRow.innerHTML =
            '<th class="notify-picker-select-cell align-middle">선택</th>' +
            '<th class="align-middle">대상명</th>' +
            '<th class="text-center align-middle notify-picker-channel-th-cb">CALLBACK <span class="text-muted fw-normal small">(서버)</span></th>' +
            '<th class="text-center align-middle notify-picker-channel-th-rs">RESULT <span class="text-muted fw-normal small">(브라우저)</span></th>';
        } else {
          theadRow.innerHTML =
            '<th class="notify-picker-select-cell align-middle">선택</th>' +
            '<th class="text-center align-middle" style="width:118px">구분</th>' +
            '<th class="align-middle">대상명</th>' +
            '<th class="align-middle">URL</th>';
        }
      }
      singleBtns.forEach(function (btn) {
        if (btn._notifyPickerBound) return;
        btn._notifyPickerBound = true;
        btn.addEventListener('click', function () {
          var field = btn.getAttribute('data-field') || '';
          var selectEl = pane.querySelector('[name="' + field + '"][data-load-notify-targets="true"]');
          if (!selectEl) return;
          var modalEl = ensureNotifyTargetPickerModal();
          var keywordEl = modalEl.querySelector('#notifyTargetKeyword');
          var tbody = modalEl.querySelector('#notifyTargetPickerTbody');
          modalEl._pickerSelectEl = selectEl;
          modalEl._pickerSelCallback = null;
          modalEl._pickerSelResult = null;
          setNotifyPickerThead(modalEl, 'single');
          function renderSingleRows(list) {
            var rows = pgNotifyTargetsFilterKeyword(list || [], keywordEl && keywordEl.value);
            tbody.innerHTML = '';
            if (rows.length === 0) {
              tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">조회 결과가 없습니다.</td></tr>';
              return;
            }
            var sel = modalEl._pickerSelectEl;
            rows.forEach(function (t) {
              var ch = pgShortNotifyChannel(t);
              var badgeCls = ch === 'RESULT' ? 'notify-picker-badge notify-picker-badge--result' : 'notify-picker-badge notify-picker-badge--callback';
              var tr = document.createElement('tr');
              tr.innerHTML =
                '<td class="notify-picker-select-cell align-middle"><button type="button" class="btn btn-sm btn-outline-primary">선택</button></td>' +
                '<td class="text-center align-middle"><span class="' + badgeCls + '">' + ch + '</span></td>' +
                '<td class="align-middle">' + pgEscHtml(t.targetName || t.targetCode || '') + '</td>' +
                '<td class="align-middle"><code class="small notify-picker-url-code">' + pgEscHtml(t.targetUrl || '') + '</code></td>';
              tr.querySelector('button').addEventListener('click', function () {
                if (sel) sel.value = t.targetUrl || '';
                if (window.bootstrap && bootstrap.Modal) {
                  var mm = bootstrap.Modal.getInstance(modalEl);
                  if (mm) mm.hide();
                }
              });
              tbody.appendChild(tr);
            });
          }
          modalEl._renderSingleRows = renderSingleRows;
          modalEl._renderPairRows = null;
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.hqNotifyTargets().then(function (list) {
            renderSingleRows(list || []);
            if (window.bootstrap && bootstrap.Modal) {
              new bootstrap.Modal(modalEl).show();
            }
          }).catch(function (e) {
            alert(e && e.message ? e.message : '노티 대상 조회 실패');
          }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
      });
      pairBtns.forEach(function (btn) {
        if (btn._notifyPairPickerBound) return;
        btn._notifyPairPickerBound = true;
        btn.addEventListener('click', function () {
          var cbName = btn.getAttribute('data-callback-field') || 'notifyUrl1';
          var rsName = btn.getAttribute('data-result-field') || 'notifyUrl2';
          var selCb = pane.querySelector('[name="' + cbName + '"][data-load-notify-targets="true"]');
          var selRs = pane.querySelector('[name="' + rsName + '"][data-load-notify-targets="true"]');
          if (!selCb || !selRs) return;
          var modalEl = ensureNotifyTargetPickerModal();
          var keywordEl = modalEl.querySelector('#notifyTargetKeyword');
          var tbody = modalEl.querySelector('#notifyTargetPickerTbody');
          modalEl._pickerSelectEl = null;
          modalEl._pickerSelCallback = selCb;
          modalEl._pickerSelResult = selRs;
          setNotifyPickerThead(modalEl, 'pair');
          function renderPairRows(list) {
            var filtered = pgNotifyTargetsFilterKeyword(list || [], keywordEl && keywordEl.value);
            var groups = pgGroupNotifyTargetsByPairKey(filtered);
            var pairs = groups.filter(function (g) {
              var cb = g.filter(function (x) { return pgShortNotifyChannel(x) === 'CALLBACK'; })[0];
              var rs = g.filter(function (x) { return pgShortNotifyChannel(x) === 'RESULT'; })[0];
              return cb && rs;
            });
            tbody.innerHTML = '';
            if (pairs.length === 0) {
              tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">CALLBACK·RESULT 쌍이 없습니다. 본사설정 &gt; 노티구성설정에서 [노티자동생성]으로 등록하세요.</td></tr>';
              return;
            }
            var scb = modalEl._pickerSelCallback;
            var srs = modalEl._pickerSelResult;
            pairs.forEach(function (g) {
              var cb = g.filter(function (x) { return pgShortNotifyChannel(x) === 'CALLBACK'; })[0];
              var rs = g.filter(function (x) { return pgShortNotifyChannel(x) === 'RESULT'; })[0];
              var name = (cb && cb.targetName) || (rs && rs.targetName) || '';
              var tr = document.createElement('tr');
              tr.innerHTML =
                '<td class="notify-picker-select-cell align-middle"><button type="button" class="btn btn-sm btn-outline-primary">쌍 선택</button></td>' +
                '<td class="align-middle">' + pgEscHtml(name) + '</td>' +
                '<td class="align-middle notify-picker-url-cell notify-picker-url-cell--callback"><code class="small notify-picker-url-code">' + pgEscHtml((cb && cb.targetUrl) || '') + '</code></td>' +
                '<td class="align-middle notify-picker-url-cell notify-picker-url-cell--result"><code class="small notify-picker-url-code">' + pgEscHtml((rs && rs.targetUrl) || '') + '</code></td>';
              tr.querySelector('button').addEventListener('click', function () {
                if (scb) scb.value = (cb && cb.targetUrl) || '';
                if (srs) srs.value = (rs && rs.targetUrl) || '';
                if (window.bootstrap && bootstrap.Modal) {
                  var mm = bootstrap.Modal.getInstance(modalEl);
                  if (mm) mm.hide();
                }
              });
              tbody.appendChild(tr);
            });
          }
          modalEl._renderPairRows = renderPairRows;
          modalEl._renderSingleRows = null;
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.hqNotifyTargets().then(function (list) {
            renderPairRows(list || []);
            if (window.bootstrap && bootstrap.Modal) {
              new bootstrap.Modal(modalEl).show();
            }
          }).catch(function (e) {
            alert(e && e.message ? e.message : '노티 대상 조회 실패');
          }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
      });
    }
    if (pane.querySelector('select[data-load-notify-targets="true"]') && window.PG_API && window.PG_API.hqNotifyTargets) {
      window.PG_API.hqNotifyTargets().then(function (list) {
        var arr = Array.isArray(list) ? list : [];
        pane.querySelectorAll('select[data-load-notify-targets="true"]').forEach(function (sel) {
          var current = sel.value || '';
          var html = '<option value="">선택</option>';
          arr.forEach(function (t) {
            var ch = pgShortNotifyChannel(t);
            var url = String(t.targetUrl || '');
            var label = (t.targetName || t.targetCode || '노티') + ' [' + ch + ']';
            html += '<option value="' + url.replace(/&/g, '&amp;').replace(/"/g, '&quot;') + '">' + pgEscHtml(label) + '</option>';
          });
          sel.innerHTML = html;
          if (current) sel.value = current;
        });
      }).catch(function () {});
    }
    bindNotifyTargetPicker();
    if (url === '/system/noticeList') {
      var actRowNw = pane.querySelector('.screen-action-buttons');
      if (actRowNw && !pane.querySelector('[data-notice-write-btn]')) {
        var wb = document.createElement('button');
        wb.type = 'button';
        wb.className = 'btn btn-success btn-sm';
        wb.setAttribute('data-notice-write-btn', '1');
        wb.classList.add('d-none');
        wb.textContent = '공지 등록';
        wb.addEventListener('click', function () {
          var u = getSessionUser();
          if (!u || !u.canWriteNotice) { alert('공지 등록 권한이 없습니다. 조직 등급(총본사·본사·총판)과 [공지사항] 화면 권한을 확인하세요.'); return; }
          openNoticeWriteModal(function () {
            doSearch(pane, tabId, 1);
          });
        });
        actRowNw.insertBefore(wb, actRowNw.firstChild);
      }
      syncAllNoticeWriteButtons();
    }
    var autoSearchUrls = ['/system/noticeList', '/calc/payList', '/calc/chillPayTrList', '/calc/chillPaySettlementList', '/calc/payNotiList', '/calc/paySuccessList', '/calc/payFailList', '/calc/payRefundList', '/calc/payForceRefundList', '/calc/payCancelList', '/calc/payVoidList', '/calc/offsetCancList', '/pay/easyPay', '/pay/chatbotPay',
      '/comp/compMngTree', '/comp/compInfoHistList', '/commission/commisionList',
      '/user/userMng', '/set/gridSetMng',
      '/calc/calcList', '/calc/calcGmList', '/calc/paySettlementHoldList', '/settlement/paySettlementHoldList', '/settlement/franchiseList', '/calc/feeList', '/settlement/feeList', '/calc/compPointMngList', '/settlement/recallMng', '/calc/balanceList', '/calc/unpaidMng', '/calc/exCalcList', '/pay/payHoldList', '/settlement/holdList', '/calc/collateralList', '/settlement/collateralList',
      '/noti/notiUrlMng', '/noti/notiSendMngList', '/noti/notiCashReceiptUrlMng', '/noti/notiCashReceiptSendMngList',
      '/hq/pgApiMng', '/hq/permissionMng', '/hq/accountMng', '/risk/list'];
    function applySettlementReportAccessThenSearch() {
      function runSearch() {
        loadViewSetting().finally(function () {
          doSearch(pane, tabId, 1);
        });
      }
      if (!window.PG_API || !window.PG_API.settlementReportAccess) {
        runSearch();
        return;
      }
      window.PG_API.settlementReportAccess().then(function (data) {
        var rk = pane.querySelector('select[name="searchReportKind"]');
        if (rk && data) {
          var mer = data.merchantStmt !== false;
          var reg = data.regionalPayout === true;
          var html = '';
          if (mer) html += '<option value="MERCHANT_STMT">가맹점 정산 리포트</option>';
          if (reg) html += '<option value="REGIONAL_PAYOUT">본사 지급 리포트(총본사→본사)</option>';
          rk.innerHTML = html || '<option value="MERCHANT_STMT">가맹점 정산 리포트</option>';
          if (!rk.value && rk.options.length) rk.selectedIndex = 0;
        }
      }).catch(function () {}).finally(function () {
        runSearch();
      });
    }
    if (url === '/calc/settlementReport' || url === '/settlement/settlementReport') {
      setTimeout(function () {
        if (window.PG_LAST_REGISTERED_COMP && url === '/commission/commisionList') {
          var sid = pane.querySelector('input[name="searchCompId"]');
          if (sid) sid.value = window.PG_LAST_REGISTERED_COMP;
          window.PG_LAST_REGISTERED_COMP = null;
        }
        applySettlementReportAccessThenSearch();
      }, 100);
    } else if (autoSearchUrls.indexOf(url) !== -1) {
      setTimeout(function () {
        if (window.PG_LAST_REGISTERED_COMP && url === '/commission/commisionList') {
          var sid = pane.querySelector('input[name="searchCompId"]');
          if (sid) sid.value = window.PG_LAST_REGISTERED_COMP;
          window.PG_LAST_REGISTERED_COMP = null;
        }
        function runPayListInitialSearch() {
          loadViewSetting().finally(function () {
            doSearch(pane, tabId, 1);
          });
        }
        if (payListSearchUrls.indexOf(url) !== -1) {
          if (url === '/calc/chillPayTrList') {
            ensureChillPayTrListDefaultSearchDates(pane, runPayListInitialSearch);
          } else {
            ensurePayListDefaultSearchDates(pane);
            runPayListInitialSearch();
          }
        } else if (PAY_LIST_NOTIFY_LAYOUT_URLS.indexOf(url) !== -1) {
          loadPayListNotifyLayout(pane, tabId, url, function () {
            runPayListInitialSearch();
          });
        } else {
          runPayListInitialSearch();
        }
      }, 100);
    }
    if ((url === '/calc/settlementReport' || url === '/settlement/settlementReport') && !pane._settlementReportKindBound) {
      pane._settlementReportKindBound = true;
      pane.addEventListener('change', function (ev) {
        if (ev.target && ev.target.name === 'searchReportKind') {
          var rkSel = pane.querySelector('[name="searchReportKind"]');
          var subSel = pane.querySelector('[name="searchReportSub"]');
          if (rkSel && subSel && rkSel.value === 'REGIONAL_PAYOUT' && subSel.value === 'RST') {
            subSel.value = 'AGG';
          }
          var pageEl = pane.querySelector('#pageCnt');
          if (pageEl) pageEl.value = 1;
          doSearch(pane, tabId, 1);
        }
        if (ev.target && ev.target.name === 'searchReportSub') {
          var pageEl2 = pane.querySelector('#pageCnt');
          if (pageEl2) pageEl2.value = 1;
          doSearch(pane, tabId, 1);
        }
      });
    }
    if ((url === '/calc/settlementReport' || url === '/settlement/settlementReport') && !pane._settlementReportRstDbl) {
      pane._settlementReportRstDbl = true;
      pane.addEventListener('dblclick', function (e) {
        var tr = e.target && e.target.closest ? e.target.closest('#grid_' + tabId + ' tbody tr[data-row-idx]') : null;
        if (!tr || !pane.contains(tr) || tr.querySelector('.empty-state-cell')) return;
        var subSel = pane.querySelector('[name="searchReportSub"]');
        var rkSel = pane.querySelector('[name="searchReportKind"]');
        if (!subSel || subSel.value !== 'RST' || (rkSel && rkSel.value === 'REGIONAL_PAYOUT')) return;
        var idx = parseInt(tr.getAttribute('data-row-idx') || '-1', 10);
        var row = (pane._lastGridList || [])[idx];
        var rid = row && row.settlementRunId != null && row.settlementRunId !== '' ? Number(row.settlementRunId) : NaN;
        if (!rid || isNaN(rid)) {
          window.alert('정산 실행 ID(settlementRunId)를 찾을 수 없습니다.');
          return;
        }
        if (!window.PG_API || !window.PG_API.settlementReportConfirmedRunDetail) {
          window.alert('API 미구성');
          return;
        }
        var modalEl = document.getElementById('pgSettlementReportRstDetail');
        if (!modalEl) {
          var wrap = document.createElement('div');
          wrap.innerHTML = '<div class="modal fade" id="pgSettlementReportRstDetail" tabindex="-1" aria-hidden="true">' +
            '<div class="modal-dialog modal-lg modal-dialog-scrollable"><div class="modal-content">' +
            '<div class="modal-header"><h5 class="modal-title" id="pgSettlementReportRstDetailTitle">정산 확정 리포트</h5>' +
            '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="닫기"></button></div>' +
            '<div class="modal-body"><div id="pgSettlementReportRstDetailInner" class="pg-settlement-report-print-root border rounded p-3 bg-white small"></div>' +
            '<p class="small text-muted mt-2 mb-0">거래 구간 집계는 노티 거래 상태(승인·취소·환불 등) 기준이며, 정산금·지급액은 정산 실행 시 저장된 값입니다.</p></div>' +
            '<div class="modal-footer"><button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">닫기</button>' +
            '<button type="button" class="btn btn-primary" id="pgSettlementReportRstDetailPrint">인쇄</button></div></div></div></div>';
          document.body.appendChild(wrap.firstElementChild);
          modalEl = document.getElementById('pgSettlementReportRstDetail');
          var printBtn = document.getElementById('pgSettlementReportRstDetailPrint');
          if (printBtn) {
            printBtn.addEventListener('click', function () { try { window.print(); } catch (pe) {} });
          }
        }
        var inner = document.getElementById('pgSettlementReportRstDetailInner');
        var titleEl = document.getElementById('pgSettlementReportRstDetailTitle');
        if (inner) inner.innerHTML = '<div class="text-center py-4 text-muted">불러오는 중…</div>';
        var dimmR = document.getElementById('dimm');
        if (dimmR) dimmR.style.display = 'flex';
        window.PG_API.settlementReportConfirmedRunDetail({ settlementRunId: rid })
          .then(function (payload) {
            var lr = (payload && payload.listRow) ? payload.listRow : {};
            var tx = (payload && payload.txBreakdown) ? payload.txBreakdown : {};
            var run = (payload && payload.runTotals) ? payload.runTotals : {};
            function esc(s) {
              return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
            }
            var compNm = lr.compNm != null ? String(lr.compNm) : '';
            var compId = lr.compId != null ? String(lr.compId) : '';
            if (titleEl) titleEl.textContent = '정산 확정 리포트 — ' + (compNm || compId || '');
            var period = lr.targetPeriodText != null ? String(lr.targetPeriodText) : '-';
            var cycle = lr.calcCycle != null ? String(lr.calcCycle) : '-';
            var calcDt = lr.calcDt != null ? String(lr.calcDt) : '-';
            var h = '';
            h += '<div class="text-center border-bottom pb-2 mb-3"><div class="fs-5 fw-semibold">' + esc(compNm) + '</div>';
            h += '<div class="text-muted">' + esc(compId) + ' · 정산주기 ' + esc(cycle) + '</div>';
            h += '<div class="mt-1">정산일시 <strong>' + esc(calcDt) + '</strong></div>';
            h += '<div class="mt-1 small">정산대상기간 <strong>' + esc(period) + '</strong></div></div>';
            h += '<div class="row g-2 mb-3"><div class="col-md-6"><div class="fw-semibold mb-1">집계 구간 거래(건수·금액)</div>';
            h += '<table class="table table-sm table-bordered mb-0"><tbody>';
            h += '<tr><th class="bg-light w-50">승인(매출)</th><td class="text-end">' + fmtNum(tx.approveAmt) + ' <span class="text-muted">(' + (tx.approveCnt != null ? tx.approveCnt : 0) + '건)</span></td></tr>';
            h += '<tr><th class="bg-light">취소</th><td class="text-end">' + fmtNum(tx.cancelAmt) + ' <span class="text-muted">(' + (tx.cancelCnt != null ? tx.cancelCnt : 0) + '건)</span></td></tr>';
            h += '<tr><th class="bg-light">환불</th><td class="text-end">' + fmtNum(tx.refundAmt) + ' <span class="text-muted">(' + (tx.refundCnt != null ? tx.refundCnt : 0) + '건)</span></td></tr>';
            h += '<tr><th class="bg-light">기타</th><td class="text-end">' + fmtNum(tx.otherAmt) + ' <span class="text-muted">(' + (tx.otherCnt != null ? tx.otherCnt : 0) + '건)</span></td></tr>';
            h += '<tr><th class="bg-light">거래총건수</th><td class="text-end">' + (tx.txnTotalCnt != null ? tx.txnTotalCnt : 0) + '건</td></tr>';
            h += '</tbody></table></div>';
            h += '<div class="col-md-6"><div class="fw-semibold mb-1">정산 실행 확정값</div>';
            h += '<table class="table table-sm table-bordered mb-0"><tbody>';
            h += '<tr><th class="bg-light w-50">승인(매출)합</th><td class="text-end">' + fmtNum(run.approveAmt) + '</td></tr>';
            h += '<tr><th class="bg-light">취소합</th><td class="text-end">' + fmtNum(run.cancelAmt) + '</td></tr>';
            h += '<tr><th class="bg-light">순액</th><td class="text-end">' + fmtNum(run.netPay) + '</td></tr>';
            h += '<tr><th class="bg-light">공제수수료</th><td class="text-end">' + fmtNum(run.totalFee) + '</td></tr>';
            h += '<tr><th class="bg-light">롤링보류</th><td class="text-end">' + fmtNum(run.rollingReserveAmt) + '</td></tr>';
            h += '<tr><th class="bg-light">지급액</th><td class="text-end fw-semibold text-primary">' + fmtNum(run.payAmount) + '</td></tr>';
            h += '<tr><th class="bg-light">상태</th><td class="text-end">' + esc(run.status) + '</td></tr>';
            h += '</tbody></table></div></div>';
            h += '<p class="small text-muted mb-0">가맹점 전달용 요약입니다. 세부 수수료·보류 해제 일정은 수수료내역·담보금내역에서 확인하세요.</p>';
            if (inner) inner.innerHTML = h;
            if (window.bootstrap && bootstrap.Modal) {
              bootstrap.Modal.getOrCreateInstance(modalEl).show();
            } else {
              modalEl.classList.add('show');
              modalEl.style.display = 'block';
            }
          })
          .catch(function (err) {
            window.alert((err && err.message) ? err.message : '상세 조회 실패');
          })
          .finally(function () { if (dimmR) dimmR.style.display = 'none'; });
      });
    }
    if (url === '/commission/commisionList' && commissionSignal) {
      pane.addEventListener('click', function (e) {
        var tr = e.target && e.target.closest ? e.target.closest('#grid_' + tabId + ' tbody tr') : null;
        if (!tr || tr.querySelector('.empty-state-cell')) return;
        if (e.target && e.target.closest && e.target.closest('.grid-row-check, .grid-check-all, .commission-inline-save, .commission-inline-clear, .commission-inline-cell, .commission-inline-input')) return;
        var cid = tr.getAttribute('data-comp-id');
        if (!cid) return;
        pane._commissionHistCompId = cid;
        pane.querySelectorAll('#grid_' + tabId + ' tbody tr.table-active').forEach(function (x) { x.classList.remove('table-active'); });
        tr.classList.add('table-active');
        commissionEnsureRowCheckSelected(tr);
        loadCommissionHistoryGrid(pane, tabId);
      }, { signal: commissionSignal });
      pane.addEventListener('change', function (e) {
        var t = e.target;
        if (!t || !t.classList || !t.classList.contains('grid-row-check')) return;
        var grid = pane.querySelector('#grid_' + tabId + ' tbody');
        if (!grid || !grid.contains(t)) return;
        var trCh = t.closest('tr');
        function syncHistoryToTr(tr0) {
          if (!tr0) return;
          var cid0 = (tr0.getAttribute('data-comp-id') || '').trim();
          if (!cid0) return;
          pane._commissionHistCompId = cid0;
          pane.querySelectorAll('#grid_' + tabId + ' tbody tr.table-active').forEach(function (x) { x.classList.remove('table-active'); });
          tr0.classList.add('table-active');
          try { loadCommissionHistoryGrid(pane, tabId); } catch (eH) {}
        }
        var allChecked = grid.querySelectorAll('tr .grid-row-check:checked');
        if (t.checked && trCh) {
          syncHistoryToTr(trCh);
        } else if (!t.checked) {
          if (allChecked.length === 1) syncHistoryToTr(allChecked[0].closest('tr'));
          else if (allChecked.length === 0) {
            pane._commissionHistCompId = '';
            pane.querySelectorAll('#grid_' + tabId + ' tbody tr.table-active').forEach(function (x) { x.classList.remove('table-active'); });
            try { loadCommissionHistoryGrid(pane, tabId); } catch (eH2) {}
          } else {
            syncHistoryToTr(allChecked[0].closest('tr'));
          }
        }
      }, { signal: commissionSignal });
      pane.addEventListener('dblclick', function (e) {
        var tr = e.target && e.target.closest ? e.target.closest('#grid_' + tabId + ' tbody tr') : null;
        if (!tr || tr.querySelector('.empty-state-cell')) return;
        if (e.target && e.target.closest && e.target.closest('.grid-row-check, .commission-inline-save, .commission-inline-clear, .commission-inline-input, input, button')) return;
        var idx = parseInt(tr.getAttribute('data-row-idx') || '-1', 10);
        var list = pane._lastGridList || [];
        if (idx < 0 || idx >= list.length) return;
        var row = list[idx] || {};
        var compId = row.compId != null ? String(row.compId).trim() : '';
        if (!compId) return;
        try {
          sessionStorage.setItem('pg_comp_detail_compId', compId);
          if (row.compDiv != null) sessionStorage.setItem('pg_comp_detail_compDiv', String(row.compDiv));
        } catch (err) {}
        fnTopMenuMove('/comp/compDetail', null, '업체정보');
      }, { signal: commissionSignal });
    }
    if (url === '/commission/commisionList') {
      var commissionSettingBtn = pane.querySelector('#commissionSettingBtn');
      function parseNum(v) {
        if (v == null) return 0;
        var n = parseFloat(String(v).replace(/,/g, '.').trim());
        return isFinite(n) ? n : 0;
      }
      function fmtNum(v) {
        var n = parseNum(v);
        return pgFmtOneDecimalStripWhole(String(n));
      }
      function recalcCommissionRowTotals(tr) {
        if (!tr) return;
        var rateKeys = ['hqRate', 'regionalRate', 'masterRate', 'branchRate', 'agencyRate', 'salesOfficeRate'];
        var feeKeys = ['hqPerTxFee', 'regionalPerTxFee', 'masterPerTxFee', 'branchPerTxFee', 'agencyPerTxFee', 'salesOfficePerTxFee'];
        function sumBy(keys) {
          var s = 0;
          keys.forEach(function (k) {
            var td = tr.querySelector('.commission-inline-cell[data-key="' + k + '"]');
            var v = td ? (td.getAttribute('data-value') || '') : '';
            s += parseNum(v);
          });
          return s;
        }
        var totalRate = fmtNum(sumBy(rateKeys));
        var totalFee = fmtNum(sumBy(feeKeys));
        var tdRate = tr.querySelector('.commission-inline-cell[data-key="totalRate"]');
        var tdFee = tr.querySelector('.commission-inline-cell[data-key="totalPerTxFee"]');
        if (tdRate) {
          tdRate.setAttribute('data-value', totalRate);
          tdRate.innerHTML = '<span class="commission-inline-view">' + totalRate + '</span>';
        }
        if (tdFee) {
          tdFee.setAttribute('data-value', totalFee);
          tdFee.innerHTML = '<span class="commission-inline-view">' + totalFee + '</span>';
        }
      }
      function commissionInlineCellClose(cell) {
        if (!cell) return;
        var inp = cell.querySelector('.commission-inline-input[data-key]');
        if (!inp) return;
        var cellKey = (cell.getAttribute('data-key') || '').trim();
        var v = (inp.value || '').trim();
        if (!v) v = '0';
        var esc = String(v).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
        cell.setAttribute('data-value', v);
        cell.innerHTML = '<span class="commission-inline-view">' + esc + '</span>';

        // 합계(요율/건당)를 직접 수정하는 경우: HQ에 몰아주고 나머지는 0으로 맞춘다.
        // 사용자가 "합계만" 바꾸고 저장했을 때 저장/히스토리가 안 되는 혼선을 방지.
        if (cellKey === 'totalRate' || cellKey === 'totalPerTxFee') {
          var rateKeys = ['hqRate', 'regionalRate', 'masterRate', 'branchRate', 'agencyRate', 'salesOfficeRate'];
          var feeKeys = ['hqPerTxFee', 'regionalPerTxFee', 'masterPerTxFee', 'branchPerTxFee', 'agencyPerTxFee', 'salesOfficePerTxFee'];
          var keys = cellKey === 'totalRate' ? rateKeys : feeKeys;
          var tr = cell.closest('tr');
          keys.forEach(function (k, i) {
            var td = tr ? tr.querySelector('.commission-inline-cell[data-key="' + k + '"]') : null;
            if (!td) return;
            var nv = (i === 0) ? v : '0';
            td.setAttribute('data-value', nv);
            td.innerHTML = '<span class="commission-inline-view">' + String(nv).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;') + '</span>';
          });
        }
        recalcCommissionRowTotals(cell.closest('tr'));
      }
      function flushOpenCommissionInlineCells(tr0) {
        if (!tr0) return;
        tr0.querySelectorAll('.commission-inline-cell[data-key]').forEach(function (cell) {
          if (cell.querySelector('input.commission-inline-input[data-key]')) commissionInlineCellClose(cell);
        });
      }
      /** 인라인 수수료 셀 값: input → data-value → 화면 텍스트 순 (data-value 누락·구 DOM 대비) */
      function readCommissionEditableValue(tr0, key) {
        if (!tr0 || !key) return '';
        var inp0 = tr0.querySelector('.commission-inline-input[data-key="' + key + '"]');
        if (inp0) return String(inp0.value != null ? inp0.value : '').trim();
        var td0 = tr0.querySelector('.commission-inline-cell[data-key="' + key + '"]');
        if (!td0) return '';
        var dv = String(td0.getAttribute('data-value') != null ? td0.getAttribute('data-value') : '').trim();
        if (dv !== '') return dv;
        var sp0 = td0.querySelector('.commission-inline-view');
        if (sp0 && sp0.textContent != null) return String(sp0.textContent).replace(/\s/g, '').trim();
        return '';
      }
      /** 상세/목록 JSON 값 → 폼 인코딩용 문자열(객체·배열은 서버 파싱 오류를 피하도록 정규화) */
      function commissionScalarToSaveString(x) {
        if (x === undefined || x === null) return '';
        if (typeof x === 'number' && isFinite(x)) return String(x);
        if (typeof x === 'boolean') return x ? 'true' : 'false';
        if (typeof x === 'string') return x;
        if (Array.isArray(x) && x.length >= 3) {
          var y = parseInt(x[0], 10);
          var mo = parseInt(x[1], 10);
          var d = parseInt(x[2], 10);
          if (isFinite(y) && isFinite(mo) && isFinite(d)) {
            var ms = mo < 10 ? '0' + mo : String(mo);
            var ds = d < 10 ? '0' + d : String(d);
            return String(y) + '-' + ms + '-' + ds;
          }
        }
        return '';
      }
      /** 상세 API 결과 → /api/commission/save 파라미터명 (모달 저장과 동일 스키마) */
      function commissionDetailToSaveFd(detail) {
        var fd = {};
        if (!detail || typeof detail !== 'object') return fd;
        var keys = [
          'perTxFee', 'cancelRate', 'voidFeePerTx', 'manualVoidFeePerTx', 'usageRate', 'failFee', 'payRate', 'refundRate', 'rollingPct', 'rollingDays',
          'feeAccountActivation', 'feeAnnual', 'feeTechService', 'feeSettlementPerTx', 'feeRefund',
          'fee3dsRate', 'chargebackFeePerTx', 'chargebackPolicyId', 'remittanceTransferFee', 'usdtTransferFeeUsd', 'feeUsdt', 'feeFx',
          'hqRate', 'regionalRate', 'masterRate', 'branchRate', 'agencyRate', 'salesOfficeRate',
          'hqPerTxFee', 'regionalPerTxFee', 'masterPerTxFee', 'branchPerTxFee', 'agencyPerTxFee', 'salesOfficePerTxFee'
        ];
        keys.forEach(function (k) { fd[k] = commissionScalarToSaveString(detail[k]); });
        var ap = detail.applyStartDateStr || detail.applyStartDate || '';
        var apStr = commissionScalarToSaveString(ap) || (typeof ap === 'string' ? ap : '');
        if (apStr) {
          fd.applyStartDate = apStr.length >= 10 ? apStr.substring(0, 10) : apStr;
        } else {
          fd.applyStartDate = '';
        }
        return fd;
      }
      /**
       * @param {boolean} isClear 행 [삭제] 초기화
       * @param {{ skipConfirm?: boolean, silent?: boolean, batchDimm?: boolean }} [saveOpts] silent=batch 시 알림·목록갱신 생략
       * @returns {Promise<{ skipped?: boolean, ok?: boolean, compId?: string }>}
       */
      function runCommissionRowInlineSave(tr, isClear, saveOpts) {
        saveOpts = saveOpts || {};
        var skipConfirm = !!saveOpts.skipConfirm;
        var silent = !!saveOpts.silent;
        var batchDimm = !!saveOpts.batchDimm;
        function cancelled() {
          return Promise.resolve({ skipped: true });
        }
        if (!tr) return cancelled();
        flushOpenCommissionInlineCells(tr);
        var idx = parseInt(tr.getAttribute('data-row-idx') || '-1', 10);
        var list = pane._lastGridList || [];
        var compIdVal = (tr.getAttribute('data-comp-id') || '').trim();
        if (!compIdVal && idx >= 0 && idx < list.length && list[idx] && list[idx].compId != null && list[idx].compId !== '') {
          compIdVal = String(list[idx].compId).trim();
        }
        if (!compIdVal) {
          alert('업체코드를 찾을 수 없습니다. 목록을 다시 검색해 주세요.');
          return Promise.reject(new Error('업체코드 없음'));
        }
        if (idx >= 0 && idx < list.length && list[idx] && list[idx].compId != null) {
          var listCid = String(list[idx].compId).trim();
          if (listCid && listCid !== compIdVal) {
            alert('행 정보가 목록과 맞지 않습니다. 검색을 다시 해 주세요.');
            return Promise.reject(new Error('행 불일치'));
          }
        }
        var editableKeys = ['hqRate', 'regionalRate', 'masterRate', 'branchRate', 'agencyRate', 'salesOfficeRate', 'hqPerTxFee', 'regionalPerTxFee', 'masterPerTxFee', 'branchPerTxFee', 'agencyPerTxFee', 'salesOfficePerTxFee', 'applyDt'];
        if (!skipConfirm) {
          if (isClear) {
            if (!pgConfirmBeforeSave(
              '[' + compIdVal + '] 수수료 배분·건당 수수료를 0으로 초기화합니다. 계속할까요?',
              '초기화 내용이 서버에 반영됩니다. 정말 진행할까요?'
            )) return cancelled();
          } else {
            if (!pgConfirmBeforeSave(
              '[' + compIdVal + '] 그리드에서 수정한 수수료를 저장합니다. 계속할까요?',
              '기존 상세 수수료와 병합되어 서버에 반영됩니다. 정말 저장할까요?'
            )) return cancelled();
          }
        }
        var dimmI = document.getElementById('dimm');
        if (!batchDimm) {
          if (dimmI) dimmI.style.display = 'flex';
        }
        return window.PG_API.commissionDetail(compIdVal).catch(function (errD) {
          console.warn('[commission] detail merge skipped:', errD && errD.message ? errD.message : errD);
          return null;
        }).then(function (d) {
          var raw = d;
          if (raw && typeof raw === 'object' && raw.data != null && typeof raw.data === 'object' && raw.success !== false) {
            raw = raw.data;
          }
          if (!raw || typeof raw !== 'object') raw = {};
          var fd = commissionDetailToSaveFd(raw);
          editableKeys.forEach(function (k) {
            if (k === 'applyDt') {
              var hasDateInp = !!tr.querySelector('.commission-inline-input[data-key="applyDt"]');
              if (hasDateInp) {
                var vDt = readCommissionEditableValue(tr, k);
                fd.applyStartDate = vDt;
              }
              return;
            }
            var hasCell = !!tr.querySelector('.commission-inline-cell[data-key="' + k + '"]');
            if (isClear) {
              fd[k] = '0';
              return;
            }
            if (!hasCell) {
              return;
            }
            var v = readCommissionEditableValue(tr, k);
            fd[k] = v === '' ? '0' : v;
          });
          return window.PG_API.commissionSave(compIdVal, fd);
        }).then(function () {
          if (!silent) {
            alert(isClear ? '수수료를 0으로 초기화했습니다.' : '수수료가 저장되었습니다.');
            pane._commissionHistCompId = compIdVal;
            doSearch(pane, tabId, parseInt(pane.getAttribute('data-last-page') || '1', 10) || 1);
            try { loadCommissionHistoryGrid(pane, tabId); } catch (e0) {}
            setTimeout(function () { try { loadCommissionHistoryGrid(pane, tabId); } catch (e1) {} }, 600);
          }
          return { ok: true, compId: compIdVal };
        }).catch(function (err) {
          if (!silent) {
            alert(err && err.message ? err.message : '수수료 저장 실패');
          }
          throw err;
        }).finally(function () {
          if (!batchDimm) {
            if (dimmI) dimmI.style.display = 'none';
          }
        });
      }
      if (commissionSignal) {
        // mousedown에서 먼저 편집 중인 셀을 닫아 blur/click 순서에 따른 값 유실 방지
        pane.addEventListener('mousedown', function (e) {
          var actM = e.target && e.target.closest ? e.target.closest('.commission-inline-save, .commission-inline-clear') : null;
          if (!actM || !pane.contains(actM)) return;
          var gridM = pane.querySelector('#grid_' + tabId + ' tbody');
          if (!gridM || !gridM.contains(actM)) return;
          var trM = actM.closest('tr');
          if (!trM || trM.querySelector('.empty-state-cell')) return;
          flushOpenCommissionInlineCells(trM);
        }, { capture: true, signal: commissionSignal });
        pane.addEventListener('click', function (e) {
          var act = e.target && e.target.closest ? e.target.closest('.commission-inline-save, .commission-inline-clear') : null;
          if (!act || !pane.contains(act)) return;
          var gridInner = pane.querySelector('#grid_' + tabId + ' tbody');
          if (!gridInner || !gridInner.contains(act)) return;
          var trS = act.closest('tr');
          if (!trS || trS.querySelector('.empty-state-cell')) return;
          e.preventDefault();
          e.stopPropagation();
          runCommissionRowInlineSave(trS, act.classList.contains('commission-inline-clear')).catch(function () {});
        }, { capture: true, signal: commissionSignal });
      }
      if (commissionSettingBtn) {
        commissionSettingBtn.addEventListener('click', function () {
          var grid = pane.querySelector('#grid_' + tabId + ' tbody');
          var tr = null;
          if (grid) {
            var ta = grid.querySelector('tr.table-active');
            if (ta && ta.querySelector('.grid-row-check:checked')) tr = ta;
            if (!tr) {
              var ch = grid.querySelector('tr .grid-row-check:checked');
              if (ch) tr = ch.closest('tr');
            }
          }
          if (!tr) {
            alert('그리드에서 행을 클릭하거나 체크한 뒤 [수수료설정]을 눌러주세요.');
            return;
          }
          var compId = (tr.getAttribute('data-comp-id') || '').trim();
          if (!compId) {
            var tds = tr.querySelectorAll('td');
            var cfgM = window.PG_SCREENS && window.PG_SCREENS.getMenuScreens()['/commission/commisionList'];
            var colsM = (cfgM && cfgM.columns) || [];
            var idxM = colsM.findIndex(function (c) { return c.key === 'compId'; });
            if (idxM < 0) idxM = 1;
            compId = (tds[idxM] && tds[idxM].textContent) ? tds[idxM].textContent.trim() : '';
          }
          if (!compId) { alert('업체코드를 찾을 수 없습니다.'); return; }
          var modalEl = document.getElementById('commissionSettingModal');
          var compIdHidden = document.getElementById('commissionSettingCompId');
          if (compIdHidden) compIdHidden.value = compId;
          window._commissionSettingPane = pane;
          window._commissionSettingTabId = tabId;
          pane._commissionHistCompId = compId;
          pane.querySelectorAll('#grid_' + tabId + ' tbody tr.table-active').forEach(function (x) { x.classList.remove('table-active'); });
          if (tr) tr.classList.add('table-active');
          try { loadCommissionHistoryGrid(pane, tabId); } catch (eHist) {}
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.commissionDetail(compId).then(function (data) {
            function setAmt(id, val) {
              var el = document.getElementById(id);
              if (el && val != null) el.value = pgFmtOneDecimalStripWhole(val);
            }
            function setPct(id, val) {
              var el = document.getElementById(id);
              if (el && val != null) el.value = pgFmtPctOneDecimalInput(val);
            }
            function setDay(id, val) {
              var el = document.getElementById(id);
              if (!el || val == null) return;
              var n = parseFloat(String(val).replace(/,/g, '.'));
              el.value = isFinite(n) ? String(Math.round(n)) : String(val);
            }
            var set = function (id, val) { var el = document.getElementById(id); if (el && val != null) el.value = String(val); };
            setAmt('commissionPerTxFee', data.perTxFee);
            setAmt('commissionCancelRate', data.cancelRate);
            setAmt('commissionVoidFeePerTx', data.voidFeePerTx);
            setAmt('commissionManualVoidFeePerTx', data.manualVoidFeePerTx);
            setAmt('commissionFailFee', data.failFee);
            setPct('commissionPayRate', data.payRate);
            setAmt('commissionRefundRate', data.refundRate);
            setPct('commissionRollingPct', data.rollingPct);
            setDay('commissionRollingDays', data.rollingDays);
            setAmt('commissionFeeAnnual', data.feeAnnual);
            setAmt('commissionFeeSettlementPerTx', data.feeSettlementPerTx);
            setAmt('commissionRemittanceTransferFee', data.remittanceTransferFee);
            setAmt('commissionUsdtTransferFeeUsd', data.usdtTransferFeeUsd);
            setPct('commissionFeeUsdt', data.feeUsdt);
            setPct('commissionFeeFx', data.feeFx);
            setAmt('commissionFee3dsRate', data.fee3dsRate);
            setAmt('commissionChargebackFeePerTx', data.chargebackFeePerTx);
            setPct('commissionHqRate', data.hqRate);
            setPct('commissionRegionalRate', data.regionalRate);
            setPct('commissionMasterRate', data.masterRate);
            setPct('commissionBranchRate', data.branchRate);
            setPct('commissionAgencyRate', data.agencyRate);
            setPct('commissionSalesOfficeRate', data.salesOfficeRate);
            setAmt('commissionHqPerTxFee', data.hqPerTxFee);
            setAmt('commissionRegionalPerTxFee', data.regionalPerTxFee);
            setAmt('commissionMasterPerTxFee', data.masterPerTxFee);
            setAmt('commissionBranchPerTxFee', data.branchPerTxFee);
            setAmt('commissionAgencyPerTxFee', data.agencyPerTxFee);
            setAmt('commissionSalesOfficePerTxFee', data.salesOfficePerTxFee);
            set('commissionHqPolicyScopeView', data.hqPolicyScope || '');
            setPct('commissionHoldRateView', data.holdRate);
            setDay('commissionHoldDaysView', data.holdDays);
            set('commissionApplyStartDate', data.applyStartDateStr || data.applyStartDate || '');
            fillChargebackPolicySelectsInRoot(modalEl, data.chargebackPolicyId).finally(function () {
              if (modalEl && window.bootstrap && bootstrap.Modal) {
                var modal = new bootstrap.Modal(modalEl);
                modal.show();
              }
            });
          }).catch(function (e) { alert(e && e.message ? e.message : '수수료 조회 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
      }
      [pane.querySelector('#commissionInlineTopSaveBtn'), pane.querySelector('#commissionPaginationSaveBtn')].forEach(function (commissionSaveToolbarBtn) {
        if (!commissionSaveToolbarBtn || commissionSaveToolbarBtn._commissionToolbarSaveBound) return;
        commissionSaveToolbarBtn._commissionToolbarSaveBound = true;
        commissionSaveToolbarBtn.addEventListener('click', function () {
          var grid = pane.querySelector('#grid_' + tabId + ' tbody');
          var trs = [];
          var seenC = {};
          if (grid) {
            grid.querySelectorAll('tr .grid-row-check:checked').forEach(function (cb) {
              var r = cb.closest('tr');
              if (!r || r.querySelector('.empty-state-cell')) return;
              var cidK = (r.getAttribute('data-comp-id') || '').trim() || ('_idx_' + String(r.getAttribute('data-row-idx') || ''));
              if (seenC[cidK]) return;
              seenC[cidK] = true;
              trs.push(r);
            });
          }
          if (trs.length === 0) {
            var tr = null;
            var activeInput = pane.querySelector('.commission-inline-cell .commission-inline-input[data-key]');
            if (activeInput) tr = activeInput.closest('tr');
            if (!tr && grid) {
              var dateFocus = grid.querySelector('.commission-inline-input[data-key="applyDt"]:focus');
              if (dateFocus) tr = dateFocus.closest('tr');
            }
            if (!tr && grid) {
              var marked = grid.querySelector('tr.table-active');
              var mc = marked && (marked.getAttribute('data-comp-id') || '').trim();
              if (mc) tr = marked;
            }
            if (!tr && grid) {
              var histCid = String(pane._commissionHistCompId || '').trim();
              if (histCid) {
                var rows = grid.querySelectorAll('tr[data-comp-id]');
                var nMatch = 0;
                var lastMatch = null;
                for (var ri = 0; ri < rows.length; ri++) {
                  if ((rows[ri].getAttribute('data-comp-id') || '').trim() === histCid) {
                    nMatch++;
                    lastMatch = rows[ri];
                  }
                }
                if (nMatch === 1) tr = lastMatch;
              }
            }
            if (tr) trs.push(tr);
          }
          if (trs.length === 0) {
            alert('저장할 행을 먼저 클릭하거나(또는 체크·셀 편집)한 뒤 [저장]을 눌러주세요.');
            return;
          }
          for (var vi = 0; vi < trs.length; vi++) {
            var trV = trs[vi];
            var canSaveCommissionRow = trV.querySelector('.commission-inline-save')
              || trV.querySelector('.commission-inline-cell[data-key]')
              || trV.querySelector('.commission-inline-input[data-key="applyDt"]');
            if (!canSaveCommissionRow) {
              alert('선택된 행 중 수수료 인라인 열이 없는 행이 있습니다. 해당 행의 체크를 해제하거나 목록을 확인해 주세요.');
              return;
            }
          }
          trs.forEach(function (t) { flushOpenCommissionInlineCells(t); });
          if (trs.length === 1) {
            runCommissionRowInlineSave(trs[0], false).catch(function () {});
            return;
          }
          if (!pgConfirmBeforeSave(
            '체크된 ' + trs.length + '건의 수수료를 한꺼번에 저장합니다. 계속할까요?',
            '각 행의 그리드 값이 서버에 순서대로 반영됩니다. 정말 저장할까요?'
          )) return;
          var dimmBatch = document.getElementById('dimm');
          if (dimmBatch) dimmBatch.style.display = 'flex';
          var bi = 0;
          function runNextBatch() {
            if (bi >= trs.length) {
              if (dimmBatch) dimmBatch.style.display = 'none';
              alert(trs.length + '건 저장을 완료했습니다.');
              var lastTr = trs[trs.length - 1];
              var lc = lastTr && (lastTr.getAttribute('data-comp-id') || '').trim();
              if (lc) pane._commissionHistCompId = lc;
              doSearch(pane, tabId, parseInt(pane.getAttribute('data-last-page') || '1', 10) || 1);
              try { loadCommissionHistoryGrid(pane, tabId); } catch (e0) {}
              setTimeout(function () { try { loadCommissionHistoryGrid(pane, tabId); } catch (e1) {} }, 600);
              return;
            }
            var trB = trs[bi++];
            runCommissionRowInlineSave(trB, false, { skipConfirm: true, silent: true, batchDimm: true })
              .then(function (res) {
                if (res && res.skipped) {
                  if (dimmBatch) dimmBatch.style.display = 'none';
                  return;
                }
                runNextBatch();
              })
              .catch(function (err) {
                if (dimmBatch) dimmBatch.style.display = 'none';
                alert(err && err.message ? err.message : '수수료 저장 실패');
              });
          }
          runNextBatch();
        });
      });
      if (commissionSignal) {
        pane.addEventListener('click', function (e) {
          var activeInput = pane.querySelector('.commission-inline-cell .commission-inline-input[data-key]');
          if (activeInput && (!e.target || !e.target.closest || !e.target.closest('.commission-inline-cell[data-key]'))) {
            commissionInlineCellClose(activeInput.closest('.commission-inline-cell[data-key]'));
          }
          var editCell = e.target && e.target.closest ? e.target.closest('.commission-inline-cell[data-key]') : null;
          if (editCell && pane.contains(editCell)) {
            if (!editCell.querySelector('.commission-inline-input')) {
              var trEdit = editCell.closest('tr');
              var cidEdit = trEdit ? (trEdit.getAttribute('data-comp-id') || '').trim() : '';
              if (cidEdit && String(pane._commissionHistCompId || '').trim() !== cidEdit) {
                pane._commissionHistCompId = cidEdit;
                try { loadCommissionHistoryGrid(pane, tabId); } catch (eHistSync) {}
              }
              pane.querySelectorAll('#grid_' + tabId + ' tbody tr.table-active').forEach(function (x) { x.classList.remove('table-active'); });
              if (trEdit) trEdit.classList.add('table-active');
              commissionEnsureRowCheckSelected(trEdit);
              var key = editCell.getAttribute('data-key') || '';
              var cur = editCell.getAttribute('data-value') || '';
              var safe = String(cur).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
              editCell.innerHTML = '<input type="text" class="form-control form-control-sm commission-inline-input text-center" data-key="' + key + '" value="' + safe + '">';
              var ip = editCell.querySelector('.commission-inline-input');
              if (ip) {
                // 셀 실제 너비(px) 기준으로 입력창 폭을 고정해 과확장 방지
                var cw = Math.max(36, Math.floor(editCell.getBoundingClientRect().width) - 2);
                ip.style.width = cw + 'px';
                ip.style.maxWidth = cw + 'px';
                ip.style.minWidth = '0';
                ip.focus();
                ip.select();
                ip.addEventListener('blur', function () {
                  commissionInlineCellClose(editCell);
                }, { once: true });
              }
            }
            return;
          }
        }, { signal: commissionSignal });
        pane.addEventListener('focusin', function (e) {
          var t = e.target;
          if (!t || !t.getAttribute || t.getAttribute('data-key') !== 'applyDt') return;
          if (!t.classList || !t.classList.contains('commission-inline-input')) return;
          var tbody = pane.querySelector('#grid_' + tabId + ' tbody');
          if (!tbody || !t.closest || !tbody.contains(t)) return;
          var trDt = t.closest('tr');
          if (!trDt) return;
          var cidDt = (trDt.getAttribute('data-comp-id') || '').trim();
          if (cidDt && String(pane._commissionHistCompId || '').trim() !== cidDt) {
            pane._commissionHistCompId = cidDt;
            try { loadCommissionHistoryGrid(pane, tabId); } catch (eFi) {}
          }
          pane.querySelectorAll('#grid_' + tabId + ' tbody tr.table-active').forEach(function (x) { x.classList.remove('table-active'); });
          trDt.classList.add('table-active');
          commissionEnsureRowCheckSelected(trDt);
        }, { signal: commissionSignal });
      }
    }
    var commissionSettingSaveBtn = document.getElementById('commissionSettingSaveBtn');
    if (commissionSettingSaveBtn && !commissionSettingSaveBtn._bound) {
      commissionSettingSaveBtn._bound = true;
      commissionSettingSaveBtn.addEventListener('click', function () {
        var cid = document.getElementById('commissionSettingCompId');
        var compIdVal = (cid && cid.value) ? cid.value.trim() : '';
        if (!compIdVal) return;
        var modalEl = document.getElementById('commissionSettingModal');
        var fd = {};
        if (modalEl) {
          modalEl.querySelectorAll('input[name], select[name]').forEach(function (el) {
            if (el.name) fd[el.name] = el.value;
          });
        }
        if (!pgConfirmBeforeSave(
          '[' + compIdVal + '] 수수료 설정(모달)을 저장합니다. 계속할까요?',
          '입력한 내용이 서버에 반영됩니다. 정말 저장할까요?'
        )) return;
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.commissionSave(compIdVal, fd).then(function () {
          alert('저장되었습니다.');
          if (modalEl && window.bootstrap && bootstrap.Modal) { var m = bootstrap.Modal.getInstance(modalEl); if (m) m.hide(); }
          var pane = window._commissionSettingPane;
          var tabId = window._commissionSettingTabId;
          if (pane && tabId && typeof doSearch === 'function') {
            pane._commissionHistCompId = compIdVal;
            doSearch(pane, tabId, 1);
            try { loadCommissionHistoryGrid(pane, tabId); } catch (e0) {}
          }
        }).catch(function (err) { alert(err && err.message ? err.message : '저장 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      });
    }
    if (url === '/comp/compMngTree' && !pane._treeToggleBound) {
      pane._treeToggleBound = true;
      pane.addEventListener('click', function (e) {
        var tg = e.target && e.target.closest ? e.target.closest('.tree-toggle') : null;
        if (!tg || tg.classList.contains('tree-toggle-placeholder')) return;
        var id = tg.getAttribute('data-id') || '';
        if (!id) return;
        var tbody = pane.querySelector('#grid_' + tabId + ' tbody');
        var list = pane._treeList;
        if (!tbody || !list) return;
        var expanded = pane._treeExpanded;
        if (!expanded) return;
        if (expanded.has) {
          if (expanded.has(id)) expanded.delete(id); else expanded.add(id);
        } else {
          if (id in expanded) delete expanded[id]; else expanded[id] = 1;
        }
        applyTreeVisibility(pane, tbody, list);
      });
    }
    var compMngSaveColumnsBtn = pane.querySelector('#compMngSaveColumnsBtn');
    if (compMngSaveColumnsBtn && !compMngSaveColumnsBtn._bound) {
      bindColumnGuideDrag();
      rememberColumnGuideDefaultOrder();
      pane.querySelectorAll('.column-guide-check').forEach(function (cb) {
        cb.addEventListener('change', syncColumnGuideUiState);
      });
      syncColumnGuideUiState();
      compMngSaveColumnsBtn._bound = true;
      compMngSaveColumnsBtn.addEventListener('click', function () {
        if (!confirm('해당 설정값을 적용하시겠습니까?')) return;
        var keys = getSelectedGuideKeys();
        applySelectedGuideKeys(keys);
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.userViewSettingSave(url, JSON.stringify(keys)).then(function () {
          captureViewSettingLastSavedSnapshot();
          doSearch(pane, tabId, 1);
        }).catch(function (e) {
          alert(e && e.message ? e.message : 'VIEW SETTING 저장 실패');
        }).finally(function () {
          if (dimm) dimm.style.display = 'none';
        });
      });
    }
    var compMngRestoreColumnsBtn = pane.querySelector('#compMngRestoreColumnsBtn');
    if (compMngRestoreColumnsBtn && !compMngRestoreColumnsBtn._bound) {
      compMngRestoreColumnsBtn._bound = true;
      compMngRestoreColumnsBtn.addEventListener('click', function () {
        if (pane._viewSettingLastSavedKeys == null) {
          alert('복원할 저장 상태가 없습니다. [저장] 후 다시 시도하거나 화면을 새로고침한 뒤 이용하세요.');
          return;
        }
        if (!confirm('바로 직전에 저장된 열 구성으로 되돌릴까요? (저장하지 않은 체크·순서 변경은 취소됩니다.)')) return;
        applySelectedGuideKeys(pane._viewSettingLastSavedKeys.slice());
        doSearch(pane, tabId, 1);
      });
    }
    var compMngReleaseColumnsBtn = pane.querySelector('#compMngReleaseColumnsBtn');
    if (compMngReleaseColumnsBtn && !compMngReleaseColumnsBtn._bound) {
      compMngReleaseColumnsBtn._bound = true;
      compMngReleaseColumnsBtn.addEventListener('click', function () {
        pane._selectedColumns = null;
        pane.querySelectorAll('.column-guide-check').forEach(function (cb) { if (!cb.disabled) cb.checked = false; });
        syncColumnGuideUiState();
      });
    }
    var compMngSelectAllColumnsBtn = pane.querySelector('#compMngSelectAllColumnsBtn');
    if (compMngSelectAllColumnsBtn && !compMngSelectAllColumnsBtn._bound) {
      compMngSelectAllColumnsBtn._bound = true;
      compMngSelectAllColumnsBtn.addEventListener('click', function () {
        pane.querySelectorAll('#tableColumnGuide .column-guide-check').forEach(function (cb) {
          var item = cb.closest('.column-guide-item');
          if (item && item.style.display === 'none') return;
          if (cb.disabled) return;
          cb.checked = true;
        });
        pane._selectedColumns = getSelectedGuideKeys();
        syncColumnGuideUiState();
      });
    }
    var compMngDefaultColumnsBtn = pane.querySelector('#compMngDefaultColumnsBtn');
    if (compMngDefaultColumnsBtn && !compMngDefaultColumnsBtn._bound) {
      compMngDefaultColumnsBtn._bound = true;
      compMngDefaultColumnsBtn.addEventListener('click', function () {
        if (!confirm('VIEW SETTING을 기본(AI·카탈로그 기준 순서·노출)으로 적용·저장할까요?')) return;
        resetColumnGuideToDefault();
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        var payloadJson = resolvePayListUserViewDefaultKeys(url, pane) != null
          ? JSON.stringify(getSelectedGuideKeys())
          : '[]';
        window.PG_API.userViewSettingSave(url, payloadJson).then(function () {
          captureViewSettingLastSavedSnapshot();
          doSearch(pane, tabId, 1);
        }).catch(function (e) {
          alert(e && e.message ? e.message : 'VIEW SETTING 저장 실패');
        }).finally(function () {
          if (dimm) dimm.style.display = 'none';
        });
      });
    }
    var viewHelloBtn = pane.querySelector('#viewSettingHelloBtn_' + tabId);
    if (viewHelloBtn && !viewHelloBtn._viewHelloBound) {
      viewHelloBtn._viewHelloBound = true;
      function setHelloToggleZonesHidden(hidden) {
        pane.querySelectorAll('.pg-hello-toggle-zone').forEach(function (el) {
          if (hidden) el.classList.add('d-none');
          else el.classList.remove('d-none');
        });
      }
      function syncViewHelloAppearance() {
        var hidden = !!pane._viewSettingHelloHidden;
        viewHelloBtn.classList.toggle('btn-view-setting-hello--restore', hidden);
        viewHelloBtn.setAttribute('aria-pressed', hidden ? 'true' : 'false');
        if (typeof pgHelloTimelineIsEnabled === 'function' && pgHelloTimelineIsEnabled()) {
          var u = typeof pgHelloTimelineUntilMs === 'function' ? pgHelloTimelineUntilMs() : 0;
          if (u > Date.now()) {
            var sec = Math.ceil((u - Date.now()) / 1000);
            var mm = Math.floor(sec / 60);
            var ss = sec % 60;
            viewHelloBtn.title = (hidden ? '안내 숨김' : '안내 표시') + ' — 헬로 타임라인 약 ' + mm + '분 ' + ss + '초 남음 (전 페이지 동기)';
          } else {
            var dm = (window._pgHelloTimelineFromLedger && window._pgHelloTimelineFromLedger.helloTimelineDurationMin) || 10;
            viewHelloBtn.title = (hidden ? '안내 숨김(기본)' : '안내 표시') + ' — 헬로 타임라인: 클릭 시 전 페이지에 ' + dm + '분 동안 표시';
          }
        } else {
          viewHelloBtn.title = hidden
            ? '클릭 시 안내(파스텔)·VIEW SETTING을 다시 표시합니다.'
            : '클릭 시 안내(파스텔)·VIEW SETTING을 숨깁니다.';
        }
      }
      pgHelloTimelineFetchConfig().then(function (cfg) {
        var tlOn = !!(cfg && String(cfg.helloTimelineEnabledYn || '').toUpperCase() === 'Y');
        if (tlOn) {
          var u = pgHelloTimelineUntilMs();
          var show = u > Date.now();
          pane._viewSettingHelloHidden = !show;
          setHelloToggleZonesHidden(!show);
        } else {
          if (typeof pane._viewSettingHelloHidden === 'undefined') pane._viewSettingHelloHidden = false;
          setHelloToggleZonesHidden(!!pane._viewSettingHelloHidden);
        }
        syncViewHelloAppearance();
        try {
          if (window.PG_TABLE_COL_RESIZE && typeof window.PG_TABLE_COL_RESIZE.refreshIn === 'function') {
            window.PG_TABLE_COL_RESIZE.refreshIn(pane);
          }
        } catch (eH0) { /* ignore */ }
      });
      viewHelloBtn.addEventListener('click', function () {
        pgHelloTimelineFetchConfig().then(function (cfg) {
          var tlOn = !!(cfg && String(cfg.helloTimelineEnabledYn || '').toUpperCase() === 'Y');
          if (tlOn) {
            var u0 = pgHelloTimelineUntilMs();
            var now = Date.now();
            if (u0 > now) {
              pgHelloTimelineSetUntilMs(0);
              pgHelloApplyTimelineToAllPanes(true);
            } else {
              var mins = parseInt(String(cfg.helloTimelineDurationMin != null ? cfg.helloTimelineDurationMin : '10'), 10);
              if (isNaN(mins) || mins < 1) mins = 10;
              if (mins > 1440) mins = 1440;
              pgHelloTimelineSetUntilMs(now + mins * 60000);
              pgHelloApplyTimelineToAllPanes(false);
            }
          } else {
            pane._viewSettingHelloHidden = !pane._viewSettingHelloHidden;
            setHelloToggleZonesHidden(pane._viewSettingHelloHidden);
            syncViewHelloAppearance();
            try {
              if (window.PG_TABLE_COL_RESIZE && typeof window.PG_TABLE_COL_RESIZE.refreshIn === 'function') {
                window.PG_TABLE_COL_RESIZE.refreshIn(pane);
              }
            } catch (eH) { /* ignore */ }
          }
        });
      });
    }
    // TEMP_REMOVE_AFTER_DEV — [업체전체초기화] 버튼·핸들러 (ApiCompController admin-reset·플래그와 함께 제거)
    var compAdminResetOrgBtn = pane.querySelector('#compAdminResetOrgBtn');
    if (compAdminResetOrgBtn && url === '/comp/compMngTree') {
      var uReset = getSessionUser();
      if (!uReset || String(uReset.role || '').toUpperCase() !== 'ADMIN') {
        compAdminResetOrgBtn.classList.add('d-none');
      } else {
        compAdminResetOrgBtn.classList.remove('d-none');
      }
      if (!compAdminResetOrgBtn._bound) {
      compAdminResetOrgBtn._bound = true;
      compAdminResetOrgBtn.addEventListener('click', function () {
        if (getPagePermissionForUrl(url) === 'OBSERVER') return;
        if (!confirm('【업체 전체 초기화】\n\n· 모든 조직(총판·본사·가맹점 등) 및 거래·정산·가맹 연관 데이터가 삭제됩니다.\n· ADMIN 계정은 유지되나, 모든 로그인 세션이 끊깁니다.\n· 총본사 코드 0000000000 만 다시 생성됩니다.\n\n정말 실행할까요?')) return;
        if (!confirm('마지막 확인: 되돌릴 수 없습니다. 서버에서 allow-org-hierarchy-reset 이 켜져 있어야 성공합니다. 계속?')) return;
        if (!window.PG_API || !window.PG_API.compAdminResetOrgHierarchy) {
          alert('API를 사용할 수 없습니다. site/js/api.js 를 반영했는지 확인하세요.');
          return;
        }
        var dimmR = document.getElementById('dimm');
        if (dimmR) dimmR.style.display = 'flex';
        window.PG_API.compAdminResetOrgHierarchy().then(function (r) {
          if (r && r.success === false) {
            alert(r.message || '실패했습니다. 서버 설정 app.features.allow-org-hierarchy-reset=true 및 재시작 여부를 확인하세요.');
            return;
          }
          var d = r && r.data ? r.data : r;
          var msg = (d && d.message) ? d.message : '초기화되었습니다.';
          alert(msg);
          try {
            if (window.PG_API.clearAuth) window.PG_API.clearAuth();
          } catch (e0) {}
          try {
            if (window.location) window.location.replace((window.location.origin || '') + '/login.html');
          } catch (e1) {}
        }).catch(function (e) {
          alert(e && e.message ? e.message : '초기화 요청 실패');
        }).finally(function () {
          if (dimmR) dimmR.style.display = 'none';
        });
      });
      }
    }
    // TEMP_REMOVE_AFTER_DEV — [삭제(개발)] 버튼·핸들러 (dev-tree-remove·플래그와 함께 제거)
    var compDevTreeRemoveBtn = pane.querySelector('#compDevTreeRemoveBtn');
    if (compDevTreeRemoveBtn && url === '/comp/compMngTree') {
      var uDev = getSessionUser();
      if (!uDev || String(uDev.role || '').toUpperCase() !== 'ADMIN') {
        compDevTreeRemoveBtn.classList.add('d-none');
      } else {
        compDevTreeRemoveBtn.classList.remove('d-none');
      }
      compDevTreeRemoveBtn.addEventListener('click', function () {
        if (getPagePermissionForUrl(url) === 'OBSERVER') return;
        var grid = pane.querySelector('#grid_' + tabId);
        if (!grid) return;
        var checked = grid.querySelectorAll('tbody .grid-row-check:checked');
        if (checked.length !== 1) {
          alert('그리드에서 정확히 한 건만 체크한 뒤 [삭제(개발)]을 눌러주세요.');
          return;
        }
        var tr = checked[0].closest('tr');
        var dr = tr && tr.getAttribute('data-row');
        var compId = '';
        if (dr) {
          try {
            var row = JSON.parse(decodeURIComponent(dr));
            compId = row.compId ? String(row.compId).trim() : '';
          } catch (e1) {}
        }
        if (!compId) {
          alert('선택한 행에서 업체코드를 읽을 수 없습니다.');
          return;
        }
        if (!confirm('[' + compId + '] 및 그 하위 전체 조직의 업체 프로필을 미사용(N)으로 바꿉니다.\n(개발용 — DB 행·테이블 물리 삭제 없음. 총본사는 불가)\n계속할까요?')) return;
        if (!window.PG_API || !window.PG_API.compDevTreeRemove) {
          alert('API를 사용할 수 없습니다.');
          return;
        }
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.compDevTreeRemove(compId).then(function (r) {
          var d = r && r.data ? r.data : r;
          var msg = (d && d.message) ? d.message : '처리되었습니다.';
          if (r && r.success === false) alert(r.message || msg);
          else { alert(msg); doSearch(pane, tabId, 1); }
        }).catch(function (e) { alert(e && e.message ? e.message : '처리 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      });
    }
    var excelRegBtn = pane.querySelector('#excelRegBtn');
    if (excelRegBtn && url === '/comp/compMngTree' && !excelRegBtn._bound) {
      excelRegBtn._bound = true;
      var excelRegInput = document.createElement('input');
      excelRegInput.type = 'file';
      excelRegInput.accept = '.xlsx,.xls';
      excelRegInput.style.display = 'none';
      pane.appendChild(excelRegInput);
      excelRegBtn.addEventListener('click', function () {
        excelRegInput.value = '';
        excelRegInput.click();
      });
      excelRegInput.addEventListener('change', function () {
        var file = this.files && this.files[0];
        if (!file) return;
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.compExcelRegister(file).then(function (r) {
          var d = r && r.data ? r.data : r;
          var created = (d.createdCount || 0);
          var errCnt = (d.errorCount || 0);
          var msg = '등록 완료: ' + created + '건';
          if (errCnt > 0) msg += ', 오류: ' + errCnt + '건';
          if (d.errors && d.errors.length > 0) msg += '\n\n오류:\n' + d.errors.slice(0, 5).join('\n') + (d.errors.length > 5 ? '\n...외 ' + (d.errors.length - 5) + '건' : '');
          alert(msg);
          doSearch(pane, tabId, 1);
        }).catch(function (e) { alert(e && e.message ? e.message : '엑셀 등록 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      });
    }
    var excelSampleBtn = pane.querySelector('#excelSampleBtn');
    if (excelSampleBtn && url === '/comp/compMngTree' && !excelSampleBtn._bound) {
      excelSampleBtn._bound = true;
      excelSampleBtn.addEventListener('click', function () {
        downloadCompExcelSampleFile();
      });
    }
    var compRegBtn = pane.querySelector('#compRegBtn');
    if (compRegBtn) {
      compRegBtn.addEventListener('click', function () { fnTopMenuMove('/comp/compReg'); });
    }
    var compRegSaveBtn = pane.querySelector('#compRegSaveBtn');
    if (compRegSaveBtn) {
      compRegSaveBtn.addEventListener('click', function () {
        if (getPagePermissionForUrl(url) === 'OBSERVER') return;
        var form = pane.querySelector('#compRegForm');
        if (!form) return;
        initIntlPhoneFields(form);
        ['ceoMobile', 'compTel', 'fax', 'settleTelNo', 'contactTel'].forEach(function (n) { syncIntlPhoneHidden(form, n); });
        var fd = {};
        form.querySelectorAll('input, select, textarea').forEach(function (el) {
          if (el.name && el.type !== 'file' && el.name !== 'pgOperational') {
            if (el.name.indexOf('__phone_') === 0) return;
            var card = el.closest('.card');
            if (card && card.classList.contains('d-none')) return;
            fd[el.name] = el.value;
          }
        });
        if (fd.countryCd === 'OTHER') { fd.bankCd = fd.bankCdText || fd.bankCd; delete fd.bankCdText; }
        if (fd.addrCountryCd === 'OTHER') { fd.addrCountryCd = fd.addrCountryCdOther || ''; delete fd.addrCountryCdOther; }
        fd.compNm = fd.compNm || fd.comp_name;
        fd.compDiv = fd.compDiv || fd.comp_div || 'MERCHANT';
        if (!fd.compNm || !fd.compNm.trim()) { alert('업체명을 입력하세요.'); return; }
        if (!fd.compDiv || fd.compDiv === '') { alert('업체구분을 선택하세요.'); return; }
        if (!fd.loginId || !String(fd.loginId).trim()) { alert('로그인ID를 입력하세요.'); return; }
        if (!fd.pwd || !String(fd.pwd).trim()) { alert('비밀번호를 입력하세요.'); return; }
        if ((form.getAttribute('data-password-confirmed') || '') !== 'Y') {
          alert('비밀번호 입력 후 옆의 [저장] 버튼으로 비밀번호를 확정하세요.');
          return;
        }
        var checkedId = form.getAttribute('data-login-id-checked') || '';
        if (checkedId !== String(fd.loginId).trim()) {
          alert('로그인ID 중복확인을 먼저 진행하세요.');
          return;
        }
        var needsParent = ['MASTER_DIST', 'BRANCH', 'AGENCY', 'SALES_OFFICE', 'MERCHANT'].indexOf((fd.compDiv || '').toUpperCase()) >= 0;
        if (needsParent && (!fd.parentId || !String(fd.parentId).trim())) { alert('상위 지점을 선택하세요. [검색] 버튼으로 상위업체를 선택해주세요.'); return; }
        if (fd.parentId) fd.parentComp = '';
        if (fd.regType != null) { fd.regNo = (fd.regType || 'CORP') + '|' + (fd.regNo || ''); delete fd.regType; }
        var tbody = form.querySelector('#pgBindingTbody');
        if (tbody) {
          var operationalVal = form.querySelector('input[name="pgOperational"]:checked');
          var operationalIdx = operationalVal ? parseInt(operationalVal.value, 10) : 0;
          var bindings = [];
          tbody.querySelectorAll('tr').forEach(function (tr, i) {
            var sel = function (f) { var e = tr.querySelector('[data-field="' + f + '"]'); return e ? e.value : ''; };
            var pgCd = sel('pgCd');
            if (pgCd) {
              bindings.push({
                pgCd: pgCd,
                activationYn: sel('activationYn') || 'Y',
                operationalYn: i === operationalIdx ? 'Y' : 'N',
                payMethod: sel('payMethod') || 'WEB',
                mid: sel('mid'),
                rootNo: sel('rootNo'),
                apiKey: sel('apiKey'),
                ivKey: sel('ivKey'),
                installmentYn: sel('installmentYn') || 'N',
                maxInstallmentMonths: sel('maxInstallmentMonths')
              });
            }
          });
          fd.pgBindings = JSON.stringify(bindings);
        }
        if (fd.compDiv === 'REGIONAL') {
          var bc = [fd.baseCurrency1, fd.baseCurrency2, fd.baseCurrency3].filter(function (v) { return v && v.trim(); });
          if (bc.length === 0) { alert('본사는 기준 화폐를 1가지 이상 선택하세요.'); return; }
          fd.baseCurrency = bc.join(',');
          delete fd.baseCurrency1;
          delete fd.baseCurrency2;
          delete fd.baseCurrency3;
        } else if (fd.compDiv === 'MASTER_DIST') {
          var masterCur = (fd.baseCurrency || '').trim();
          if (!masterCur) { alert('총판은 상위 본사가 설정한 기준 화폐 중 1가지를 선택하세요.'); return; }
          fd.baseCurrency = masterCur;
          delete fd.baseCurrency1;
          delete fd.baseCurrency2;
          delete fd.baseCurrency3;
        }
        if (fd.compDiv === 'HEADQUARTERS' || fd.compDiv === 'REGIONAL' || fd.compDiv === 'MASTER_DIST') {
          var cardLimitTbody = form.querySelector('#regionalCardLimitTbody');
          if (cardLimitTbody) {
            var cardLimits = [];
            cardLimitTbody.querySelectorAll('tr').forEach(function (tr) {
              var sel = function (f) { var e = tr.querySelector('[data-field="' + f + '"]'); return e ? e.value : ''; };
              cardLimits.push({ payMethod: sel('payMethod'), cardIssuer: sel('cardIssuer'), dayLimit: sel('dayLimit'), timesLimit: sel('timesLimit'), amtLimit: sel('amtLimit'), regReason: sel('regReason'), regDt: sel('regDt'), modDt: sel('modDt'), remark: sel('remark') });
            });
            fd.regionalCardLimits = JSON.stringify(cardLimits);
          }
          var terminalTbody = form.querySelector('#regionalTerminalTbody');
          if (terminalTbody) {
            var terminals = [];
            terminalTbody.querySelectorAll('tr').forEach(function (tr) {
              var sel = function (f) { var e = tr.querySelector('[data-field="' + f + '"]'); return e ? e.value : ''; };
              terminals.push({ pgAgency: sel('pgAgency'), terminalId: sel('terminalId'), remark: sel('remark') });
            });
            fd.regionalTerminals = JSON.stringify(terminals);
          }
          if (fd.holidayCountryCode) fd.holidayCountryCodes = fd.holidayCountryCode;
          if (fd.businessHolidayRangesJson) {
            try {
              var _rows = JSON.parse(fd.businessHolidayRangesJson || '[]');
              fd.businessHolidayExtraDates = (_rows || []).map(function (r) { return r.fromDate || ''; }).filter(function (v) { return !!v; }).join('\n');
            } catch (e) {}
          }
          var regionalKeys = ['copyright'];
          if (fd.compDiv === 'REGIONAL' || fd.compDiv === 'MASTER_DIST') {
            regionalKeys = ['remitterName', 'balanceNotifyAmt', 'suspiciousNotifyAmt', 'overseasLoginNotifyAmt', 'tempPwdNotifyAmt', 'nonTranCriterionMonth', 'sameCardLimitWebDay', 'sameCardLimitWebTimes', 'sameCardLimitWebAmt', 'sameCardLimitTerminalDay', 'sameCardLimitTerminalTimes', 'sameCardLimitTerminalAmt', 'dailyUsageFee', 'depositNameLookup', 'transferAuthNo', 'autoConvertNewMemberLimit', 'newMemberDailyLimit', 'convertRefDate', 'convertDailyLimit', 'applyStartDate', 'pgFeeGeneral', 'transferFee', 'settleDiffMonthCnt', 'settleReportBankCd', 'pgFeeSamsung', 'smsFee', 'taxInvoiceEmail', 'settleAccountNo', 'directFee', 'solutionFee', 'settleAccountHolder', 'withdrawRestrictType', 'withdrawRestrictStartTime', 'withdrawRestrictEndTime', 'terminalPayRestrict', 'webPayRestrict', 'defaultFeeHq', 'defaultFeeDist', 'defaultFeeBranch', 'defaultFeeAgency', 'defaultFeeSalesOffice', 'defaultPayLimitPerTx', 'defaultPayLimitDay', 'defaultPayLimitMonth', 'defaultPayLimitYearCorp', 'defaultPayLimitYearInd', 'copyright', 'regionalCardLimits', 'regionalTerminals', 'holidayProfileName', 'holidayProfileCountry', 'holidayCountryCode', 'holidayCountryCodes', 'businessHolidayRangesJson', 'businessHolidayExtraDates'];
          }
          var regionalSettings = {};
          regionalKeys.forEach(function (k) { if (fd[k] !== undefined && fd[k] !== null && fd[k] !== '') regionalSettings[k] = fd[k]; });
          fd.regionalSettings = JSON.stringify(regionalSettings);
        }
        if (fd.compDiv === 'MASTER_DIST') {
          var n1elR = form.querySelector('[name="notifyUrl1"]');
          var mdMandatoryLockedR = n1elR && n1elR.disabled;
          var n1r = String(fd.notifyUrl1 || '').trim();
          var n2r = String(fd.notifyUrl2 || '').trim();
          var hasBackupR = !!(String(fd.notifyUrl3 || '').trim() || String(fd.notifyUrl4 || '').trim());
          if (!mdMandatoryLockedR) {
            if (!n1r) { alert('총판은 노티 CALLBACK(URL 1)을 입력해야 합니다.'); return; }
            if (!n2r) { alert('총판은 노티 RESULT(URL 2)를 입력해야 합니다.'); return; }
          }
          if (hasBackupR && (!n1r || !n2r)) {
            alert(mdMandatoryLockedR
              ? '보조 노티(URL 3·4)를 쓰려면 본사 노티구성설정에서 이 총판에 필수 노티(URL 1·2)가 연결되어 있어야 합니다.'
              : '노티 URL 3·4(보조)를 쓰려면 URL 1·2(CALLBACK·RESULT)가 모두 필요합니다.');
            return;
          }
        }
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        var mainFile = form.querySelector('#brandingMainImageFile');
        var firstFile = form.querySelector('#brandingFirstLogoImageFile');
        var logoFile = form.querySelector('#brandingLogoImageFile');
        var popconFile = form.querySelector('#brandingPopconImageFile');
        var themeEl = form.querySelector('#brandingTheme');
        var hostEl = form.querySelector('#brandingBrandHost');
        var siteNameEl = form.querySelector('#brandingSiteName');
        var isRegOrMaster = (fd.compDiv === 'HEADQUARTERS' || fd.compDiv === 'REGIONAL' || fd.compDiv === 'MASTER_DIST');
        window.PG_API.compRegister(fd).then(function (res) {
          var data = res && res.data ? res.data : res;
          var compId = data && data.compId ? data.compId : '';
          if (compId && isRegOrMaster && window.PG_API.orgBrandingUpload && window.PG_API.orgBrandingSave) {
            var chain = Promise.resolve();
            if (mainFile && mainFile.files && mainFile.files[0]) {
              var _regMainF = mainFile.files[0];
              chain = chain.then(function () {
                return window.PG_API.orgBrandingUpload(compId, 'main', _regMainF).then(function (data) {
                  pgBrandingSetImageDisplayInput(form, 'main', data, _regMainF);
                  return data;
                });
              });
            }
            if (logoFile && logoFile.files && logoFile.files[0]) {
              var _regLogoF = logoFile.files[0];
              chain = chain.then(function () {
                return window.PG_API.orgBrandingUpload(compId, 'logo', _regLogoF).then(function (data) {
                  pgBrandingSetImageDisplayInput(form, 'logo', data, _regLogoF);
                  try {
                    if (window.PG_applySidebarLogo && data && data.url) {
                      var _apiBase = (typeof window.PG_assetApiBase === 'function')
                        ? window.PG_assetApiBase()
                        : ((window.PG_API_BASE || '').replace(/\/$/, '') || window.location.origin);
                      var _src = /^https?:\/\//i.test(String(data.url)) ? String(data.url) : (_apiBase + String(data.url));
                      window.PG_applySidebarLogo(_src + (_src.indexOf('?') >= 0 ? '&' : '?') + 'v=' + Date.now());
                    }
                  } catch (eLogo0) {}
                  return data;
                });
              });
            }
            if (firstFile && firstFile.files && firstFile.files[0]) {
              var _regFirstF = firstFile.files[0];
              chain = chain.then(function () {
                return window.PG_API.orgBrandingUpload(compId, 'first', _regFirstF).then(function (data) {
                  pgBrandingSetImageDisplayInput(form, 'first', data, _regFirstF);
                  return data;
                });
              });
            }
            if (popconFile && popconFile.files && popconFile.files[0]) {
              var _regPopconF = popconFile.files[0];
              chain = chain.then(function () {
                return window.PG_API.orgBrandingUpload(compId, 'popcon', _regPopconF).then(function (data) {
                  pgBrandingSetImageDisplayInput(form, 'popcon', data, _regPopconF);
                  return data;
                });
              });
            }
            if (themeEl) {
              chain = chain.then(function () { return window.PG_API.orgBrandingSave(compId, themeEl.value || 'DEFAULT', hostEl ? hostEl.value : undefined, siteNameEl ? siteNameEl.value : undefined); });
            }
            return chain.then(function () { return res; });
          }
          return res;
        }).then(function (res) {
          var data = res && res.data ? res.data : res;
          if (data && data.compId) {
            window.PG_LAST_REGISTERED_COMP = data.compId;
            if (fd.compDiv !== 'MERCHANT') {
              try { sessionStorage.setItem('pg_comp_detail_return_compId', data.compId); } catch (e) {}
            }
          }
          alert('저장되었습니다.');
          if (fd.compDiv === 'MERCHANT') {
            fnTopMenuMove('/commission/commisionList', null, '수수료관리');
          } else {
            fnTopMenuMove('/comp/compMngTree', null, '업체관리');
          }
        }).catch(function (err) {
          alert(err && err.message ? err.message : '저장에 실패했습니다.');
        }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      });
    }
    var compRegCancelBtn = pane.querySelector('#compRegCancelBtn');
    if (compRegCancelBtn) {
      compRegCancelBtn.addEventListener('click', function () { fnTopMenuMove('/comp/compMngTree'); });
    }
    if (url === '/comp/compReg') {
      initBankByCountry(pane);
      initCountryBankGroup(pane);
      initCountryAddressGroup(pane);
      initIntlPhoneFields(pane);
      initAttachmentSection(pane);
      var form = pane.querySelector('#compRegForm');
      var loginIdElForCheck = form ? form.querySelector('[name="loginId"]') : null;
      if (loginIdElForCheck && !loginIdElForCheck._dupResetBound) {
        loginIdElForCheck._dupResetBound = true;
        loginIdElForCheck.addEventListener('input', function () {
          if (form) form.setAttribute('data-login-id-checked', '');
        });
      }
      var pwdElForConfirm = form ? form.querySelector('[name="pwd"]') : null;
      if (pwdElForConfirm && !pwdElForConfirm._pwdConfirmResetBound) {
        pwdElForConfirm._pwdConfirmResetBound = true;
        pwdElForConfirm.addEventListener('input', function () {
          if (form) form.removeAttribute('data-password-confirmed');
        });
      }
      if (form && !form._compRegPwdSaveDelegated) {
        form._compRegPwdSaveDelegated = true;
        form.addEventListener('click', function (ev) {
          var btn = ev.target && ev.target.closest ? ev.target.closest('button[data-field="pwd"][data-action="저장"]') : null;
          if (!btn || !form.contains(btn)) return;
          var pwdInput = form.querySelector('[name="pwd"]');
          var v = pwdInput && pwdInput.value ? String(pwdInput.value).trim() : '';
          if (!v) { alert('비밀번호를 입력하세요.'); return; }
          if (v.length < 8) { alert('비밀번호는 8자 이상 입력하세요.'); return; }
          form.setAttribute('data-password-confirmed', 'Y');
          alert('비밀번호가 확정되었습니다. 하단 [저장]으로 업체를 등록하세요.');
        });
      }
      var loginDupBtn = pane.querySelector('button[data-field="loginId"][data-action="중복확인"]');
      if (loginDupBtn && !loginDupBtn._bound) {
        loginDupBtn._bound = true;
        loginDupBtn.addEventListener('click', function () {
          var input = form ? form.querySelector('[name="loginId"]') : null;
          var loginId = input && input.value ? String(input.value).trim() : '';
          if (!loginId) { alert('로그인ID를 입력하세요.'); return; }
          var dimmDup = document.getElementById('dimm');
          if (dimmDup) dimmDup.style.display = 'flex';
          window.PG_API.compCheckLoginId(loginId).then(function (r) {
            var ok = !!(r && r.available);
            if (ok) {
              if (form) form.setAttribute('data-login-id-checked', loginId);
              alert('사용 가능한 로그인ID입니다.');
            } else {
              if (form) form.setAttribute('data-login-id-checked', '');
              alert('이미 사용 중인 로그인ID입니다.');
            }
          }).catch(function (e) {
            if (form) form.setAttribute('data-login-id-checked', '');
            alert(e && e.message ? e.message : '중복확인 실패');
          }).finally(function () { if (dimmDup) dimmDup.style.display = 'none'; });
        });
      }
      if (form && !form.querySelector('input[name="parentId"]')) {
        var hid = document.createElement('input');
        hid.type = 'hidden';
        hid.name = 'parentId';
        hid.id = 'compRegParentId';
        form.insertBefore(hid, form.firstChild);
      }
      function toggleByCompDiv(compDiv) {
        var isMerchant = compDiv === 'MERCHANT';
        var isHeadquarters = compDiv === 'HEADQUARTERS';
        var isRegional = compDiv === 'REGIONAL';
        var isMasterDist = compDiv === 'MASTER_DIST';
        var isBranchAgencySales = compDiv === 'BRANCH' || compDiv === 'AGENCY' || compDiv === 'SALES_OFFICE';
        var isDistributor = isMasterDist || isBranchAgencySales;
        var showAccount = isMerchant || isDistributor || isRegional;
        pane.querySelectorAll('.merchant-only-section').forEach(function (card) {
          if (isMerchant) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.regional-only-section').forEach(function (card) {
          if (isRegional) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.master-dist-only-section').forEach(function (card) {
          if (isMasterDist) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.distributor-only-section').forEach(function (card) {
          if (isDistributor) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.distributor-merchant-no-regional-section').forEach(function (card) {
          if (isMerchant || isDistributor) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.distributor-or-merchant-section').forEach(function (card) {
          if (showAccount) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.branch-agency-sales-hide-section').forEach(function (card) {
          if (isBranchAgencySales) card.classList.add('d-none'); else card.classList.remove('d-none');
        });
        var isRegionalOrMasterDist = isRegional || isMasterDist;
        pane.querySelectorAll('.regional-or-master-dist-only-section').forEach(function (card) {
          if (isRegionalOrMasterDist) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        var isHeadOfficeTier = isHeadquarters || isRegional || isMasterDist;
        pane.querySelectorAll('.head-office-tier-only-section').forEach(function (card) {
          if (isHeadOfficeTier) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.merchant-regional-master-commission-section').forEach(function (card) {
          if (isMerchant || isRegional || isMasterDist) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        var hint = pane.querySelector('.comp-div-hint');
        if (hint) hint.style.display = (!compDiv || compDiv === '') ? 'block' : 'none';
        if (isRegional && window.PG_HQ_HOLIDAY && typeof window.PG_HQ_HOLIDAY.init === 'function') {
          window.PG_HQ_HOLIDAY.init(pane);
        }
      }
      function applyMerchantSettlementDefaults() {
        var regForm = pane.querySelector('#compRegForm');
        if (!regForm) return;
        var cycleEl = regForm.querySelector('[name="calcCycle"]');
        var closeEl = regForm.querySelector('[name="calcCloseTime"]');
        var startEl = regForm.querySelector('[name="calcStartTime"]');
        if (cycleEl && (!cycleEl.value || String(cycleEl.value).trim() === '')) cycleEl.value = '';
        if (closeEl && (!closeEl.value || String(closeEl.value).trim() === '')) closeEl.value = '00:00';
        if (startEl && (!startEl.value || String(startEl.value).trim() === '')) startEl.value = '04:30';
      }
      var compDivEl = pane.querySelector('#compRegForm [name="compDiv"]');
      if (compDivEl && !compDivEl._merchantToggleBound) {
        compDivEl._merchantToggleBound = true;
        try {
          var u = JSON.parse(sessionStorage.getItem('pg_admin_user') || '{}');
          if (u.orgLevel && u.orgLevel !== 'HEADQUARTERS') {
            var regOpt = compDivEl.querySelector('option[value="REGIONAL"]');
            if (regOpt) regOpt.remove();
          }
        } catch (e) {}
        compDivEl.addEventListener('change', function () {
          toggleByCompDiv(this.value);
          if (this.value === 'MERCHANT') applyMerchantSettlementDefaults();
          var rf = pane.querySelector('#compRegForm');
          if (rf) {
            var pid = this.value === 'MERCHANT' ? pgMerchantCalcCycleScopeKeyForForm(rf) : null;
            if (this.value === 'MERCHANT' && window.pgClearCalcCycleScopedCache) window.pgClearCalcCycleScopedCache();
            pgRefreshCalcCycleSelects(rf, undefined, pid);
          }
        });
        // 업체등록 첫 진입 기본값은 '선택' 유지 (강제 총판 기본값 제거)
        toggleByCompDiv(compDivEl.value || '');
        if (compDivEl.value === 'MERCHANT') applyMerchantSettlementDefaults();
        (function () {
          if (!form) return;
          var pid = compDivEl.value === 'MERCHANT' ? pgMerchantCalcCycleScopeKeyForForm(form) : null;
          pgRefreshCalcCycleSelects(form, undefined, pid);
        })();
        initPgBindingList(pane);
        initRegionalCardLimitTable(pane);
        initRegionalTerminalTable(pane);
        initRegionalHolidayProfileSelector(pane, form, null);
        pgBindBrandingBrowse(pane);
      }
      var commissionFollowEl = pane.querySelector('[name="commissionFollowHq"]');
      if (commissionFollowEl && !commissionFollowEl._commissionToggleBound) {
        commissionFollowEl._commissionToggleBound = true;
        function toggleCommissionCustom(useHq) {
          pane.querySelectorAll('.commission-custom-only').forEach(function (el) {
            el.style.display = useHq === 'Y' ? 'none' : '';
          });
          pane.querySelectorAll('.hq-policy-only').forEach(function (el) {
            el.style.display = useHq === 'Y' ? '' : 'none';
          });
        }
        commissionFollowEl.addEventListener('change', function () { toggleCommissionCustom(this.value); });
        toggleCommissionCustom(commissionFollowEl.value || 'Y');
      }
      var hqPolicySel = pane.querySelector('#compRegForm [name="hqPolicyScope"]') || pane.querySelector('[name="hqPolicyScope"]');
      var baseCurEl = pane.querySelector('#compRegForm [name="baseCurrency"]');
      if (hqPolicySel && !hqPolicySel._hqPolicyRegBound) {
        hqPolicySel._hqPolicyRegBound = true;
        function syncRegChargebackFromPolicy() {
          var followVal = commissionFollowEl && commissionFollowEl.value ? String(commissionFollowEl.value).trim().toUpperCase() : 'Y';
          if (followVal !== 'Y') return;
          applyChargebackByHqPolicyScope(pane, pane._hqCommissionTemplatesCache || [], hqPolicySel.value);
        }
        function refreshHqPolicyReg(hqd) {
          var list = (hqd && hqd.templates) ? hqd.templates : [];
          pane._hqCommissionTemplatesCache = list;
          var bc = baseCurEl ? baseCurEl.value : '';
          pgToggleUsdDependentCommissionFields(pane, bc);
          var filt0 = pgFilterDeployedTemplatesForMerchant(list, bc);
          var prev = hqPolicySel.value;
          var filt = pgAugmentFilteredHqTemplates(filt0, list, prev);
          hqPolicySel.innerHTML = pgHqPolicyScopeOptionsHtml(filt);
          var ok = false;
          var j;
          for (j = 0; j < hqPolicySel.options.length; j++) {
            if (hqPolicySel.options[j].value === prev) {
              ok = true;
              break;
            }
          }
          if (ok) hqPolicySel.value = prev;
          syncRegChargebackFromPolicy();
        }
        window.PG_API.hqDefaultCommission().then(refreshHqPolicyReg).catch(function () {});
        hqPolicySel.addEventListener('change', function () {
          syncRegChargebackFromPolicy();
        });
        if (baseCurEl && !baseCurEl._hqPolicyBaseBoundReg) {
          baseCurEl._hqPolicyBaseBoundReg = true;
          baseCurEl.addEventListener('change', function () {
            pgToggleUsdDependentCommissionFields(pane, this.value);
            if (pane._hqCommissionTemplatesCache) {
              refreshHqPolicyReg({ templates: pane._hqCommissionTemplatesCache });
            } else {
              window.PG_API.hqDefaultCommission().then(refreshHqPolicyReg).catch(function () {});
            }
          });
        }
        pgToggleUsdDependentCommissionFields(pane, baseCurEl ? baseCurEl.value : '');
      }
      fillChargebackPolicySelectsInRoot(pane, null);
      var holdRateFollowEl = pane.querySelector('[name="holdRateFollowHq"]');
      if (holdRateFollowEl && !holdRateFollowEl._holdRateToggleBound) {
        holdRateFollowEl._holdRateToggleBound = true;
        function toggleHoldRateCustom(useHq) {
          pane.querySelectorAll('.hold-rate-custom-only').forEach(function (el) {
            el.style.display = useHq === 'Y' ? 'none' : '';
          });
        }
        holdRateFollowEl.addEventListener('change', function () { toggleHoldRateCustom(this.value); });
        toggleHoldRateCustom(holdRateFollowEl.value || 'Y');
      }
      (function bindFeeVatRateToggleReg() {
        var root = pane;
        var sel = root.querySelector('[name="feeVatApplyYn"]');
        if (!sel || sel._pgFeeVatToggleBound) return;
        sel._pgFeeVatToggleBound = true;
        function syncFv() {
          var on = String(sel.value || '').toUpperCase() === 'Y';
          root.querySelectorAll('.fee-vat-rate-only').forEach(function (el) { el.style.display = on ? '' : 'none'; });
          var rateInp = root.querySelector('[name="feeVatRatePct"]');
          if (rateInp) rateInp.disabled = !on;
        }
        sel.addEventListener('change', syncFv);
        syncFv();
      })();
      bindParentCompSearchModal(pane);
    }
    function applyCompInfoHeadquartersVisibility(form, compDiv) {
      if (!form) return;
      var hide = compDiv === 'HEADQUARTERS';
      form.querySelectorAll('.comp-info-hide-if-hq').forEach(function (el) {
        el.style.display = hide ? 'none' : '';
      });
    }
    /** 업체정보조회/업체정보 상세 첫 카드 제목: 조직 구분에 맞게 (가맹점 → 가맹점 정보 상세) */
    function compDivToInfoDetailTitle(compDiv) {
      var m = {
        HEADQUARTERS: '총본사 정보 상세',
        REGIONAL: '본사 정보 상세',
        MASTER_DIST: '총판 정보 상세',
        BRANCH: '지사 정보 상세',
        AGENCY: '대리점 정보 상세',
        SALES_OFFICE: '영업점 정보 상세',
        MERCHANT: '가맹점 정보 상세'
      };
      return m[compDiv] || '업체 정보 상세';
    }
    /** 업체정보조회·내업체: 본인 소속 업체 상세는 조회만 (ADMIN 제외). 총본사/본사/총판은 브랜딩·기타(COPYRIGHT 등) 카드는 직접 수정 가능 */
    function applyReadOnlyCompInfoDetailIfOwn(pane, data, formUrl) {
      if (!pane || !data || (formUrl !== '/comp/myCompMng' && formUrl !== '/comp/compInfo')) return;
      var form = pane.querySelector('#compInfoDetailForm');
      if (!form) return;
      var u = getSessionUser();
      if (u && String(u.role || '').toUpperCase() === 'ADMIN') return;
      var mine = String((u && u.compId) || '').trim();
      var cid = String(data.compId || '').trim();
      if (!mine || !cid || mine !== cid) return;
      var cd = String(data.compDiv || '').toUpperCase();
      var selfServeBranding = cd === 'HEADQUARTERS' || cd === 'REGIONAL' || cd === 'MASTER_DIST';
      form.querySelectorAll('input, select, textarea').forEach(function (el) {
        if (el.name === 'compId') return;
        if (selfServeBranding && el.closest && (el.closest('#brandingCard') || el.closest('#regionalMiscCard'))) return;
        el.disabled = true;
      });
      var updBtn = pane.querySelector('#compInfoUpdateBtn');
      if (updBtn) {
        updBtn.style.display = selfServeBranding ? '' : 'none';
      }
    }
    function applyCompInfoDetailMainCardHeader(pane, formUrl, compDiv) {
      if (!pane || (formUrl !== '/comp/myCompMng' && formUrl !== '/comp/compInfo')) return;
      var form = pane.querySelector('#compInfoDetailForm');
      if (!form) return;
      var header = form.querySelector('.card:first-child .card-header');
      if (!header) return;
      var base = compDivToInfoDetailTitle(compDiv);
      header.textContent = formUrl === '/comp/compInfo' ? (base + ' (업체정보조회)') : base;
      var notice = form.querySelector('.card:first-child .card-body > p.text-muted.small.mb-2');
      if (!notice) return;
      if (formUrl === '/comp/myCompMng') {
        if (compDiv === 'MERCHANT') {
          notice.textContent = '로그인한 계정에 연결된 가맹점 정보가 자동으로 표시됩니다. 아래에서 조회·수정합니다.';
        } else {
          notice.textContent = '로그인한 계정에 연결된 소속 업체 정보가 자동으로 표시됩니다. 아래에서 조회·수정합니다.';
        }
      } else if (formUrl === '/comp/compInfo') {
        if (compDiv === 'MERCHANT') {
          notice.textContent = '선택한 가맹점의 정보입니다. 그리드에서 한 건 선택 후 [상세] 버튼으로 조회·수정합니다.';
        } else {
          notice.textContent = '상위 본사(우리)가 권한을 준 회사의 정보입니다. 그리드에서 한 건 선택 후 [상세] 버튼으로 조회·수정합니다.';
        }
      }
    }
    /* 상위본사 표시값 정규화: "무언가 (이름)"이면 괄호 안 이름만 노출 */
    function normalizeParentCompDisplay(v) {
      var s = (v == null) ? '' : String(v).trim();
      if (!s) return '';
      var m = s.match(/\(([^()]*)\)\s*$/);
      if (m && m[1] != null && String(m[1]).trim() !== '') return String(m[1]).trim();
      return s;
    }
    /** 업체정보 상세: 본사·총판은 상위 변경 불가. 총판 산하는 동일 총판 하위만 상위 검색 목록에 표시 */
    function ensureChargebackPolicyPreviewHost(sel) {
      if (!sel) return null;
      var host = sel.parentElement ? sel.parentElement.querySelector('.pg-chargeback-policy-preview') : null;
      if (host) return host;
      host = document.createElement('div');
      host.className = 'pg-chargeback-policy-preview mt-2';
      if (sel.parentElement) sel.parentElement.appendChild(host);
      return host;
    }
    function renderChargebackPolicyStructure(host, detail) {
      if (!host) return;
      var tiers = detail && Array.isArray(detail.tiers) ? detail.tiers : [];
      if (!tiers.length) {
        host.innerHTML = '';
        return;
      }
      var rows = tiers.map(function (t, i) {
        var min = t && t.countMin != null ? String(t.countMin) : '0';
        var max = (t && t.countMax != null && String(t.countMax) !== '') ? String(t.countMax) : '이상';
        var fee = t && t.feePerCase != null ? pgFmtOneDecimalStripWhole(t.feePerCase) : '0';
        var rangeText = pgEscHtml(min + ' ~ ' + max + '건');
        return '<tr>' +
          '<td class="text-center">' + (i + 1) + '</td>' +
          '<td class="text-center">' + rangeText + '</td>' +
          '<td class="text-center">' + pgEscHtml(fee) + '</td>' +
          '</tr>';
      });
      host.innerHTML = '' +
        '<div class="pg-chargeback-policy-preview-title">차지백설정 구조</div>' +
        '<div class="table-responsive">' +
        '<table class="table table-sm table-bordered mb-0 pg-chargeback-policy-preview-table">' +
        '<thead><tr><th class="text-center">No</th><th class="text-center">구간</th><th class="text-center">건당수수료</th></tr></thead>' +
        '<tbody>' + rows.join('') + '</tbody></table></div>';
    }
    function bindChargebackPolicyStructurePreview(sel) {
      if (!sel || sel._cbStructureBound) return;
      sel._cbStructureBound = true;
      var host = ensureChargebackPolicyPreviewHost(sel);
      function refresh() {
        var v = sel.value != null ? String(sel.value).trim() : '';
        if (!v) {
          renderChargebackPolicyStructure(host, null);
          return;
        }
        window.PG_API.hqChargebackPolicyDetail(v).then(function (detail) {
          renderChargebackPolicyStructure(host, detail || {});
        }).catch(function () {
          renderChargebackPolicyStructure(host, null);
        });
      }
      sel.addEventListener('change', refresh);
      refresh();
    }
    function fillChargebackPolicySelectsInRoot(root, selectedId) {
      if (!root || !window.PG_API || !window.PG_API.hqChargebackPolicyList) return Promise.resolve();
      var sels = root.querySelectorAll('select[name="chargebackPolicyId"]');
      if (!sels.length) return Promise.resolve();
      var selStr = selectedId != null && String(selectedId) !== '' ? String(selectedId) : '';
      return window.PG_API.hqChargebackPolicyList().then(function (list) {
        var opts = '<option value="">(미사용) 건당 차지백만</option>';
        (list || []).forEach(function (row) {
          var id = row.id != null ? String(row.id) : '';
          if (!id) return;
          var nm = (row.name != null && String(row.name).trim()) ? String(row.name).trim() : id;
          opts += '<option value="' + id + '">' + nm.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;') + '</option>';
        });
        sels.forEach(function (sel) {
          sel.innerHTML = opts;
          if (selStr) sel.value = selStr;
          bindChargebackPolicyStructurePreview(sel);
        });
      }).catch(function () {});
    }
    function applyChargebackByHqPolicyScope(root, templates, scope) {
      if (!root || !Array.isArray(templates)) return Promise.resolve();
      var selScope = scope != null ? String(scope).trim() : '';
      if (!selScope) return fillChargebackPolicySelectsInRoot(root, null);
      var picked = null;
      for (var i = 0; i < templates.length; i++) {
        var t = templates[i] || {};
        if (String(t.scope || '') === selScope) {
          picked = t;
          break;
        }
      }
      if (!picked || picked.chargebackPolicyId == null || String(picked.chargebackPolicyId).trim() === '') {
        return fillChargebackPolicySelectsInRoot(root, null);
      }
      return fillChargebackPolicySelectsInRoot(root, String(picked.chargebackPolicyId));
    }
    function applyCompParentMoveRules(form, data) {
      if (!form) return;
      var locked = !!(data && data.parentOrgChangeLocked);
      var pc = form.querySelector('input[name="parentComp"]');
      var btn = form.querySelector('button[data-field="parentComp"][data-action="검색"]');
      if (pc) {
        pc.readOnly = locked;
        pc.title = locked ? '본사·총판은 상위 조직을 변경할 수 없습니다.' : '';
      }
      if (btn) {
        btn.disabled = locked;
        btn.title = locked ? '본사·총판은 상위 조직을 변경할 수 없습니다.' : '';
      }
      if (data && data.masterDistScopeOrgId != null && String(data.masterDistScopeOrgId) !== '') {
        form.dataset.masterDistScopeOrgId = String(data.masterDistScopeOrgId);
      } else {
        delete form.dataset.masterDistScopeOrgId;
      }
    }
    function filterParentCompCandidates(list, form) {
      if (!list || !form) return list || [];
      if (form.id !== 'compDetailForm') return list;
      var scope = form.dataset.masterDistScopeOrgId;
      if (!scope) return list;
      return list.filter(function (row) {
        var rid = row.id != null ? String(row.id) : '';
        var rscope = row.masterDistScopeOrgId != null ? String(row.masterDistScopeOrgId) : '';
        return rscope === scope || rid === scope;
      });
    }
    /** 상위로 부적합: 가맹점 행은 제외(가맹점의 상위는 영업점·대리점·지사·총판·본사·총본사 등). 총판 산하 한정은 filterParentCompCandidates 이후 적용. */
    function filterParentExcludeMerchantLeaf(list) {
      if (!list || !list.length) return list || [];
      return list.filter(function (row) {
        return String(row.compDiv || '').toUpperCase() !== 'MERCHANT';
      });
    }
    function resolveParentSearchForm(pane) {
      var reg = pane && pane.querySelector('#compRegForm');
      var det = pane && pane.querySelector('#compDetailForm');
      return reg || det;
    }
    /** 업체 등록·업체정보 상세 공통: 상위업체 검색 모달 */
    function bindParentCompSearchModal(pane) {
      var parentCompSearchBtn = pane.querySelector('button[data-field="parentComp"][data-action="검색"]');
      if (!parentCompSearchBtn || parentCompSearchBtn._parentModalBound) return;
      parentCompSearchBtn._parentModalBound = true;
      parentCompSearchBtn.addEventListener('click', function () {
        var modalEl = document.getElementById('parentCompSearchModal');
        if (!modalEl) return;
        var modal = window.bootstrap && bootstrap.Modal ? new bootstrap.Modal(modalEl) : null;
        if (modal) modal.show();
        var tbody = document.getElementById('parentCompSearchTbody');
        var kw = document.getElementById('parentCompSearchKeyword');
        function runSearch() {
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.compList({
            searchCompId: (kw && kw.value) || '',
            searchCompNm: (kw && kw.value) || '',
            searchUseYn: 'ALL',
            page: 1,
            size: 1000
          }).then(function (data) {
            var raw = (data && data.list) ? data.list : [];
            var activeForm = resolveParentSearchForm(pane);
            var list = filterParentExcludeMerchantLeaf(filterParentCompCandidates(raw, activeForm));
            if (!tbody) return;
            tbody.innerHTML = '';
            list.forEach(function (row) {
              var tr = document.createElement('tr');
              tr.style.cursor = 'pointer';
              tr.setAttribute('data-id', row.id != null ? row.id : '');
              tr.setAttribute('data-compId', row.compId != null ? row.compId : '');
              tr.setAttribute('data-compNm', row.compNm != null ? row.compNm : '');
              tr.setAttribute('data-master-dist-scope-org-id', row.masterDistScopeOrgId != null && String(row.masterDistScopeOrgId) !== '' ? String(row.masterDistScopeOrgId) : '');
              tr.innerHTML = '<td><button type="button" class="btn btn-sm btn-outline-primary">선택</button></td><td>' + (row.compId || '') + '</td><td>' + (row.compNm || '') + '</td><td>' + (row.compDivNm || row.compDiv || '') + '</td>';
              tr.addEventListener('click', function (e) {
                if (e.target.tagName === 'BUTTON') return;
                selectParentComp(tr);
              });
              tr.querySelector('button').addEventListener('click', function () { selectParentComp(tr); });
              tbody.appendChild(tr);
            });
            if (list.length === 0) tbody.innerHTML = '<tr><td colspan="4" class="text-muted text-center">' + (raw.length ? '동일 총판 산하로 선택 가능한 업체만 표시됩니다. 조건에 맞는 업체가 없습니다.' : '조회된 업체가 없습니다.') + '</td></tr>';
          }).catch(function (err) {
            if (tbody) tbody.innerHTML = '<tr><td colspan="4" class="text-danger text-center">' + (err && err.message ? err.message : '조회 실패') + '</td></tr>';
          }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        }
        function selectParentComp(tr) {
          var id = tr.getAttribute('data-id');
          var compId = tr.getAttribute('data-compId');
          var compNm = tr.getAttribute('data-compNm');
          var mdScopeRaw = tr.getAttribute('data-master-dist-scope-org-id');
          var val = (compNm && String(compNm).trim()) ? String(compNm).trim() : (compId || '');
          [pane.querySelector('#compRegForm'), pane.querySelector('#compDetailForm')].forEach(function (f) {
            if (f) {
              var pid = f.querySelector('input[name="parentId"]');
              if (pid) pid.value = id || '';
              else {
                var hid = document.createElement('input');
                hid.type = 'hidden';
                hid.name = 'parentId';
                hid.value = id || '';
                f.appendChild(hid);
              }
              var pc = f.querySelector('input[name="parentComp"]');
              if (pc) pc.value = val;
              if (mdScopeRaw != null && String(mdScopeRaw).trim() !== '') {
                f.dataset.masterDistScopeOrgId = String(mdScopeRaw).trim();
              } else {
                try { delete f.dataset.masterDistScopeOrgId; } catch (eDs) { f.removeAttribute('data-master-dist-scope-org-id'); }
              }
            }
          });
          if (modal) modal.hide();
          if (window.pgClearCalcCycleScopedCache) window.pgClearCalcCycleScopedCache();
          ['#compRegForm', '#compDetailForm'].forEach(function (fid) {
            var f = document.querySelector(fid);
            if (!f) return;
            var div = (f.querySelector('[name="compDiv"]') || {}).value;
            if (div === 'MERCHANT') {
              var sk = pgMerchantCalcCycleScopeKeyForForm(f);
              pgRefreshCalcCycleSelects(f, undefined, sk || null);
            }
          });
        }
        var modalSearchBtn = document.getElementById('parentCompSearchBtn');
        if (modalSearchBtn && !modalSearchBtn._parentCompBound) {
          modalSearchBtn._parentCompBound = true;
          modalSearchBtn.addEventListener('click', function () {
            var tbody2 = document.getElementById('parentCompSearchTbody');
            var dimm = document.getElementById('dimm');
            var kw2 = document.getElementById('parentCompSearchKeyword');
            if (!tbody2) return;
            if (dimm) dimm.style.display = 'flex';
            window.PG_API.compList({
              searchCompId: (kw2 && kw2.value) || '',
              searchCompNm: (kw2 && kw2.value) || '',
              searchUseYn: 'ALL',
              page: 1,
              size: 1000
            }).then(function (data) {
              var raw = (data && data.list) ? data.list : [];
              var activeForm = resolveParentSearchForm(pane) || document.querySelector('#compRegForm') || document.querySelector('#compDetailForm');
              var list = filterParentExcludeMerchantLeaf(filterParentCompCandidates(raw, activeForm));
              tbody2.innerHTML = '';
              list.forEach(function (row) {
                var tr = document.createElement('tr');
                tr.style.cursor = 'pointer';
                tr.setAttribute('data-id', row.id != null ? row.id : '');
                tr.setAttribute('data-compId', row.compId != null ? row.compId : '');
                tr.setAttribute('data-compNm', row.compNm != null ? row.compNm : '');
                tr.setAttribute('data-master-dist-scope-org-id', row.masterDistScopeOrgId != null && String(row.masterDistScopeOrgId) !== '' ? String(row.masterDistScopeOrgId) : '');
                tr.innerHTML = '<td><button type="button" class="btn btn-sm btn-outline-primary">선택</button></td><td>' + (row.compId || '') + '</td><td>' + (row.compNm || '') + '</td><td>' + (row.compDivNm || row.compDiv || '') + '</td>';
                tr.addEventListener('click', function (e) {
                  if (e.target.tagName === 'BUTTON') return;
                  var id = tr.getAttribute('data-id') || '';
                  var compNm0 = tr.getAttribute('data-compNm') || '';
                  var compId0 = tr.getAttribute('data-compId') || '';
                  var mdScopeRaw = tr.getAttribute('data-master-dist-scope-org-id');
                  var val = compNm0 ? String(compNm0).trim() : compId0;
                  ['#compRegForm', '#compDetailForm'].forEach(function (formId) {
                    var form = document.querySelector(formId);
                    if (form) {
                      var pid = form.querySelector('input[name="parentId"]');
                      if (pid) pid.value = id;
                      else { var hid = document.createElement('input'); hid.type = 'hidden'; hid.name = 'parentId'; hid.value = id; form.appendChild(hid); }
                      var pc = form.querySelector('input[name="parentComp"]');
                      if (pc) pc.value = val;
                      if (mdScopeRaw != null && String(mdScopeRaw).trim() !== '') {
                        form.dataset.masterDistScopeOrgId = String(mdScopeRaw).trim();
                      } else {
                        try { delete form.dataset.masterDistScopeOrgId; } catch (eDs2) { form.removeAttribute('data-master-dist-scope-org-id'); }
                      }
                    }
                  });
                  if (modalEl && window.bootstrap && bootstrap.Modal) { var m = bootstrap.Modal.getInstance(modalEl); if (m) m.hide(); }
                  if (window.pgClearCalcCycleScopedCache) window.pgClearCalcCycleScopedCache();
                  ['#compRegForm', '#compDetailForm'].forEach(function (fid) {
                    var f = document.querySelector(fid);
                    if (!f) return;
                    var div = (f.querySelector('[name="compDiv"]') || {}).value;
                    if (div === 'MERCHANT') {
                      var sk = pgMerchantCalcCycleScopeKeyForForm(f);
                      pgRefreshCalcCycleSelects(f, undefined, sk || null);
                    }
                  });
                });
                tr.querySelector('button').addEventListener('click', function () {
                  var id = tr.getAttribute('data-id') || '';
                  var compNm0 = tr.getAttribute('data-compNm') || '';
                  var compId0 = tr.getAttribute('data-compId') || '';
                  var mdScopeRaw = tr.getAttribute('data-master-dist-scope-org-id');
                  var val = compNm0 ? String(compNm0).trim() : compId0;
                  ['#compRegForm', '#compDetailForm'].forEach(function (formId) {
                    var form = document.querySelector(formId);
                    if (form) {
                      var pid = form.querySelector('input[name="parentId"]');
                      if (pid) pid.value = id;
                      else { var hid = document.createElement('input'); hid.type = 'hidden'; hid.name = 'parentId'; hid.value = id; form.appendChild(hid); }
                      var pc = form.querySelector('input[name="parentComp"]');
                      if (pc) pc.value = val;
                      if (mdScopeRaw != null && String(mdScopeRaw).trim() !== '') {
                        form.dataset.masterDistScopeOrgId = String(mdScopeRaw).trim();
                      } else {
                        try { delete form.dataset.masterDistScopeOrgId; } catch (eDs3) { form.removeAttribute('data-master-dist-scope-org-id'); }
                      }
                    }
                  });
                  if (modalEl && window.bootstrap && bootstrap.Modal) { var m = bootstrap.Modal.getInstance(modalEl); if (m) m.hide(); }
                  if (window.pgClearCalcCycleScopedCache) window.pgClearCalcCycleScopedCache();
                  ['#compRegForm', '#compDetailForm'].forEach(function (fid) {
                    var f = document.querySelector(fid);
                    if (!f) return;
                    var div = (f.querySelector('[name="compDiv"]') || {}).value;
                    if (div === 'MERCHANT') {
                      var sk = pgMerchantCalcCycleScopeKeyForForm(f);
                      pgRefreshCalcCycleSelects(f, undefined, sk || null);
                    }
                  });
                });
                tbody2.appendChild(tr);
              });
              if (list.length === 0) tbody2.innerHTML = '<tr><td colspan="4" class="text-muted text-center">' + (raw.length ? '동일 총판 산하로 선택 가능한 업체만 표시됩니다. 조건에 맞는 업체가 없습니다.' : '조회된 업체가 없습니다.') + '</td></tr>';
            }).catch(function (err) {
              tbody2.innerHTML = '<tr><td colspan="4" class="text-danger text-center">' + (err && err.message ? err.message : '조회 실패') + '</td></tr>';
            }).finally(function () { if (dimm) dimm.style.display = 'none'; });
          });
        }
        runSearch();
      });
    }
    function applyMyCompBrandingPermission(pane, form, compDiv, brandingEditAllowedYn) {
      if (!pane || !form) return;
      /** 업체정보조회(/comp/myCompMng) 전용: 총본사·본사·총판은 본인 조직 브랜딩을 상위 '배경/로고 변경' 플래그 없이 수정 */
      var allowedComp = (compDiv === 'HEADQUARTERS' || compDiv === 'REGIONAL' || compDiv === 'MASTER_DIST');
      var merchantBranding = compDiv === 'MERCHANT' && String(brandingEditAllowedYn || '').toUpperCase() === 'Y';
      var allowed = allowedComp || merchantBranding;
      var brandingCard = pane.querySelector('#brandingCard');
      if (brandingCard) {
        if (compDiv === 'MERCHANT') {
          brandingCard.style.display = merchantBranding ? '' : 'none';
        } else {
          brandingCard.style.display = allowedComp ? '' : 'none';
        }
      }
      var mainBrowse = pane.querySelector('#brandingMainImageBrowse');
      var firstBrowse = pane.querySelector('#brandingFirstLogoImageBrowse');
      var logoBrowse = pane.querySelector('#brandingLogoImageBrowse');
      var popconBrowse = pane.querySelector('#brandingPopconImageBrowse');
      var mainDelete = pane.querySelector('#brandingMainImageDelete');
      var firstDelete = pane.querySelector('#brandingFirstLogoImageDelete');
      var logoDelete = pane.querySelector('#brandingLogoImageDelete');
      var popconDelete = pane.querySelector('#brandingPopconImageDelete');
      var mainFile = pane.querySelector('#brandingMainImageFile');
      var firstFile = pane.querySelector('#brandingFirstLogoImageFile');
      var logoFile = pane.querySelector('#brandingLogoImageFile');
      var popconFile = pane.querySelector('#brandingPopconImageFile');
      var themeSel = pane.querySelector('#brandingTheme');
      [mainBrowse, firstBrowse, logoBrowse, popconBrowse, mainDelete, firstDelete, logoDelete, popconDelete].forEach(function (btn) {
        if (!btn) return;
        btn.style.display = allowed ? '' : 'none';
        btn.disabled = !allowed;
      });
      [mainFile, firstFile, logoFile, popconFile, themeSel].forEach(function (el) {
        if (!el) return;
        el.disabled = !allowed;
      });
    }
    function resetPgBindingPaneForReload(pane) {
      if (!pane) return;
      var tb = pane.querySelector('#pgBindingTbody');
      if (tb) tb.innerHTML = '';
      var btn = pane.querySelector('#pgBindingAddBtn');
      if (btn && btn.parentNode) {
        var nb = btn.cloneNode(true);
        btn.parentNode.replaceChild(nb, btn);
      }
    }
    /** 업체정보조회(#compInfoDetailForm) 카드 표시 — compDetail toggleByCompDiv 와 동일 규칙(필요 분만) */
    function applyCompInfoPaneByCompDiv(pane, compDiv) {
      if (!pane || !compDiv) return;
      var isMerchant = compDiv === 'MERCHANT';
      var isHeadquarters = compDiv === 'HEADQUARTERS';
      var isRegional = compDiv === 'REGIONAL';
      var isMasterDist = compDiv === 'MASTER_DIST';
      var isBranchAgencySales = compDiv === 'BRANCH' || compDiv === 'AGENCY' || compDiv === 'SALES_OFFICE';
      var isDistributor = isMasterDist || isBranchAgencySales;
      var showAccount = isMerchant || isDistributor || isRegional;
      pane.querySelectorAll('.merchant-only-section').forEach(function (card) {
        if (isMerchant) card.classList.remove('d-none'); else card.classList.add('d-none');
      });
      pane.querySelectorAll('.distributor-merchant-no-regional-section').forEach(function (card) {
        if (isMerchant || isDistributor) card.classList.remove('d-none'); else card.classList.add('d-none');
      });
      pane.querySelectorAll('.distributor-or-merchant-section').forEach(function (card) {
        if (showAccount) card.classList.remove('d-none'); else card.classList.add('d-none');
      });
      pane.querySelectorAll('.branch-agency-sales-hide-section').forEach(function (card) {
        if (isBranchAgencySales) card.classList.add('d-none'); else card.classList.remove('d-none');
      });
      pane.querySelectorAll('.merchant-regional-master-commission-section').forEach(function (card) {
        if (isMerchant || isRegional || isMasterDist) card.classList.remove('d-none'); else card.classList.add('d-none');
      });
      var isHeadOfficeTier = isHeadquarters || isRegional || isMasterDist;
      pane.querySelectorAll('.head-office-tier-only-section').forEach(function (card) {
        if (isHeadOfficeTier) card.classList.remove('d-none'); else card.classList.add('d-none');
      });
    }
    /** 가맹점 계정정보(대표/보조 아이디·비밀번호) 노출: 총본사/본사/총판만 허용 */
    function applyMerchantAccountFieldVisibility(form, targetCompDiv) {
      if (!form) return;
      var isMerchantTarget = String(targetCompDiv || '').toUpperCase() === 'MERCHANT';
      var user = getSessionUser();
      var viewerLevel = String((user && user.orgLevel) || '').toUpperCase();
      var allowedViewer = (viewerLevel === 'HEADQUARTERS' || viewerLevel === 'REGIONAL' || viewerLevel === 'MASTER_DIST');
      var hide = isMerchantTarget && !allowedViewer;

      var blocks = [];
      var candidates = [
        form.querySelector('[name="loginId"]'),
        form.querySelector('#compDetailPwdResetBtn'),
        form.querySelector('[name="assistantLoginId"]'),
        form.querySelector('#assistantPwd'),
        form.querySelector('#assistantPwdResetBtn')
      ];
      candidates.forEach(function (el) {
        if (!el || !el.closest) return;
        var block = el.closest('.form-field-block');
        if (block && blocks.indexOf(block) < 0) blocks.push(block);
      });
      blocks.forEach(function (b) { b.style.display = hide ? 'none' : ''; });
    }
    /** 가맹점 내 업체: 보조 비밀번호 — 최초 저장 전 입력+옆[저장], 이후 비밀번호 초기화만 */
    function applyAssistantPwdUi(form, data) {
      if (!form || !data) return;
      var initialRow = form.querySelector('#assistantPwdInitialRow');
      var resetRow = form.querySelector('#assistantPwdResetRow');
      var pwdInput = form.querySelector('[name="assistantPwd"]');
      if (!initialRow || !resetRow) return;
      if (data.assistantPwdSetYn === 'Y') {
        initialRow.classList.add('d-none');
        resetRow.classList.remove('d-none');
        if (pwdInput) pwdInput.value = '';
      } else {
        initialRow.classList.remove('d-none');
        resetRow.classList.add('d-none');
        if (pwdInput) pwdInput.value = '';
      }
      form.removeAttribute('data-assistant-pwd-confirmed');
    }
    function loadCompDetailIntoForm(pane, compId) {
      if (!compId) return;
      var dimm = document.getElementById('dimm');
      if (dimm) dimm.style.display = 'flex';
      window.PG_API.compDetail(compId).then(function (data) {
        var form = pane.querySelector('#compInfoDetailForm');
        if (!form || !data) return;
        initAttachmentSection(pane);
        var formUrl = pane.getAttribute('formurl') || '';
        form.querySelectorAll('input, select, textarea').forEach(function (el) { el.disabled = false; });
        var updBtnReset = pane.querySelector('#compInfoUpdateBtn');
        if (updBtnReset) updBtnReset.style.display = '';
        var allFieldsInfo = ['compId', 'parentComp', 'compNm', 'compDiv', 'regNo', 'bizType', 'industry', 'bizNature', 'product', 'homepage', 'settleName', 'settleTelNo', 'ceoNm', 'ceoMobile', 'compTel', 'fax', 'zipCode', 'addr', 'addrDetail', 'addrEtc', 'addrCountryCd', 'addrCountryCdOther', 'email', 'siteUrl', 'siteSummary', 'useYn', 'loginId', 'bankCd', 'transferFee', 'cryptoTransferFee', 'accountNo', 'accountHolder', 'commissionConfigAllowed', 'webPaymentUseYn', 'baseCurrency', 'remark', 'countryCd', 'countryCdOther', 'swift', 'branchName', 'branchAddr', 'contactTel', 'walletAddress', 'networkName', 'withdrawRestrictType', 'withdrawStartTime', 'withdrawEndTime', 'payLimitDefault', 'payLimitExtra', 'payLimitAlertSms', 'holdRateFollowHq', 'holdRate', 'holdDays', 'commissionFollowHq', 'hqPolicyScope', 'failFee', 'usageRate', 'payRate', 'cancelRate', 'voidFeePerTx', 'manualVoidFeePerTx', 'refundRate', 'commissionMemo', 'feeSettlementPerTx', 'remittanceTransferFee', 'usdtTransferFeeUsd', 'feeUsdt', 'feeFx', 'fee3dsRate', 'chargebackFeePerTx', 'chargebackPolicyId', 'payFollowMerchantUseYn', 'payFollowAutoVoidYn', 'payFollowEmailVoidYn', 'payFollowAutoRefundYn', 'payFollowForceRefundYn', 'calcCycle', 'calcProcType', 'calcCloseTime', 'transferType', 'transferCycleDays', 'autoTransferMin', 'calcMinAmt', 'transferExecTime', 'calcExcludeYn', 'calcExcludeTarget', 'calcStartTime', 'payHoldYn', 'feeVatApplyYn', 'feeVatRatePct', 'defaultProductName', 'defaultProductCode', 'defaultProductAmount', 'defaultProductDesc', 'notifyUrlBackground', 'notifyUrlResult', 'assistantLoginId', 'assistantPwd', 'assistantRoleType', 'brandingEditAllowedYn', 'copyright'];
        allFieldsInfo.forEach(function (k) {
          var el = form.querySelector('[name="' + k + '"]');
          if (el && data[k] != null) {
            var nf = pgFmtCompDetailNumericField(k, data[k]);
            el.value = nf != null ? nf : data[k];
          }
        });
        ['payFollowMerchantUseYn', 'payFollowAutoVoidYn', 'payFollowEmailVoidYn', 'payFollowAutoRefundYn', 'payFollowForceRefundYn'].forEach(function (k) {
          var el = form.querySelector('[name="' + k + '"]');
          if (el && el.tagName === 'SELECT' && (data[k] == null || data[k] === '')) el.value = '';
        });
        // 예외 상황으로 기본정보 카드가 중복 렌더링되었을 때, 값 없는 카드(유령 카드)를 제거한다.
        (function cleanupDuplicateBasicInfoCards() {
          var host = pane.querySelector('#compInfoDetailCard');
          if (!host) return;
          var basics = Array.prototype.slice.call(host.querySelectorAll('.card')).filter(function (card) {
            var h = card.querySelector('.card-header');
            return h && String(h.textContent || '').trim() === '기본정보';
          });
          if (basics.length <= 1) return;
          var keep = null;
          basics.forEach(function (card) {
            var idEl = card.querySelector('[name="compId"]');
            var v = idEl && idEl.value ? String(idEl.value).trim() : '';
            if (!keep && v) keep = card;
          });
          if (!keep) keep = basics[basics.length - 1];
          basics.forEach(function (card) {
            if (card !== keep) card.remove();
          });
        })();
        var parentCompEl0 = form.querySelector('[name="parentComp"]');
        if (parentCompEl0) parentCompEl0.value = normalizeParentCompDisplay(data.parentComp != null ? data.parentComp : parentCompEl0.value);
        var pidEl = form.querySelector('input[name="parentId"]');
        if (!pidEl) {
          pidEl = document.createElement('input');
          pidEl.type = 'hidden';
          pidEl.name = 'parentId';
          form.appendChild(pidEl);
        }
        if (data.parentId != null) pidEl.value = String(data.parentId);
        applyCompParentMoveRules(form, data);
        if (formUrl === '/comp/myCompMng') {
          var divSelReadonly = form.querySelector('[name="compDiv"]');
          if (divSelReadonly) divSelReadonly.disabled = true;
        }
        form.setAttribute('data-login-id-checked', data.loginId ? String(data.loginId).trim() : '');
        form.setAttribute('data-assistant-login-id-checked', data.assistantLoginId ? String(data.assistantLoginId).trim() : '');
        initIntlPhoneFields(form);
        if ((data.compDiv === 'REGIONAL' || data.compDiv === 'MASTER_DIST') && data.baseCurrency) {
          var parts = data.compDiv === 'REGIONAL' ? String(data.baseCurrency).split(/,\s*/) : [data.baseCurrency, '', ''];
          ['baseCurrency1', 'baseCurrency2', 'baseCurrency3'].forEach(function (n, i) {
            var el = form.querySelector('[name="' + n + '"]');
            if (el) el.value = parts[i] || '';
          });
        }
        var cc = data.countryCd;
        if (cc && cc !== 'JP' && cc !== 'KR' && cc !== 'TH') {
          var countrySel = form.querySelector('select[name="countryCd"]');
          var countryOther = form.querySelector('[name="countryCdOther"]');
          var bankText = form.querySelector('input[name="bankCdText"]');
          if (countrySel) { countrySel.value = 'OTHER'; countrySel.dispatchEvent(new Event('change')); }
          if (countryOther) countryOther.value = cc;
          if (bankText && data.bankCd != null) bankText.value = data.bankCd;
        } else if (cc === 'JP' || cc === 'KR' || cc === 'TH') {
          refreshCountryBankAfterFill(pane, data.bankCd);
        }
        var acc = data.addrCountryCd;
        if (acc && acc !== 'JP' && acc !== 'KR' && acc !== 'TH') {
          var addrCountrySel = form.querySelector('select[name="addrCountryCd"]');
          var addrCountryOther = form.querySelector('[name="addrCountryCdOther"]');
          if (addrCountrySel) { addrCountrySel.value = 'OTHER'; addrCountrySel.dispatchEvent(new Event('change')); }
          if (addrCountryOther) addrCountryOther.value = acc;
        }
        var rn = data.regNo;
        if (rn && rn.indexOf('|') >= 0) {
          var p = rn.split('|');
          var rt = form.querySelector('[name="regType"]');
          var rnEl = form.querySelector('[name="regNo"]');
          if (rt) rt.value = (p[0] === 'PERSONAL' || p[0] === 'CORP') ? p[0] : 'CORP';
          if (rnEl) rnEl.value = p.length > 1 ? p.slice(1).join('|') : '';
        }
        applyCompInfoPaneByCompDiv(pane, data.compDiv || '');
        if (formUrl === '/comp/myCompMng') {
          var commissionCard = pane.querySelector('#commissionPolicyCard');
          if (commissionCard) {
            commissionCard.style.display = (data.compDiv === 'HEADQUARTERS') ? 'none' : '';
          }
        }
        if (formUrl === '/comp/myCompMng') {
          var regMiscCard = pane.querySelector('#regionalMiscCard');
          if (regMiscCard) {
            var isHeadOfficeTier = data.compDiv === 'HEADQUARTERS' || data.compDiv === 'REGIONAL' || data.compDiv === 'MASTER_DIST';
            regMiscCard.style.display = isHeadOfficeTier ? '' : 'none';
          }
        }
        if (formUrl === '/comp/myCompMng' || formUrl === '/comp/compInfo') {
          applyMerchantAccountFieldVisibility(form, data.compDiv || '');
        }
        if (formUrl === '/comp/myCompMng' || formUrl === '/comp/compInfo') {
          var cdInfo = data.compDiv || '';
          if (cdInfo === 'MERCHANT') {
            resetPgBindingPaneForReload(pane);
            initPgBindingList(pane, data.pgBindings, {
              rowActionMode: true,
              getCompId: function () {
                var el = pane.querySelector('#compInfoDetailForm [name="compId"]');
                return el && el.value ? el.value.trim() : '';
              }
            });
          }
          if (cdInfo === 'MERCHANT' || cdInfo === 'REGIONAL' || cdInfo === 'MASTER_DIST') {
            var commissionFollowEl = pane.querySelector('#compInfoDetailForm [name="commissionFollowHq"]');
            if (commissionFollowEl && !commissionFollowEl._infoToggleBound) {
              commissionFollowEl._infoToggleBound = true;
              function toggleCommissionCustom(useHq) {
                pane.querySelectorAll('#compInfoDetailForm .commission-custom-only').forEach(function (el) {
                  el.style.display = useHq === 'Y' ? 'none' : '';
                });
                pane.querySelectorAll('#compInfoDetailForm .hq-policy-only').forEach(function (el) {
                  el.style.display = useHq === 'Y' ? '' : 'none';
                });
              }
              commissionFollowEl.addEventListener('change', function () { toggleCommissionCustom(this.value); });
              toggleCommissionCustom(commissionFollowEl.value || 'Y');
            }
            var hqPolicySel = pane.querySelector('#compInfoDetailForm [name="hqPolicyScope"]');
            var baseCurInfo = pane.querySelector('#compInfoDetailForm [name="baseCurrency"]');
            if (hqPolicySel) {
              function syncInfoChargebackFromPolicy() {
                var followVal = commissionFollowEl && commissionFollowEl.value ? String(commissionFollowEl.value).trim().toUpperCase() : 'Y';
                if (followVal !== 'Y') return;
                applyChargebackByHqPolicyScope(form, pane._hqCommissionTemplatesCacheInfo || [], hqPolicySel.value);
              }
              function refreshHqPolicyInfo(hqd) {
                var list = (hqd && hqd.templates) ? hqd.templates : [];
                pane._hqCommissionTemplatesCacheInfo = list;
                var bc = (baseCurInfo && String(baseCurInfo.value || '').trim() !== '')
                  ? baseCurInfo.value
                  : ((data && data.baseCurrency) ? data.baseCurrency : '');
                pgToggleUsdDependentCommissionFields(pane, bc);
                var filt0 = pgFilterDeployedTemplatesForMerchant(list, bc);
                var prev = (data != null && Object.prototype.hasOwnProperty.call(data, 'hqPolicyScope'))
                  ? (data.hqPolicyScope == null ? '' : String(data.hqPolicyScope))
                  : (hqPolicySel.value || '');
                var filt = pgAugmentFilteredHqTemplates(filt0, list, prev);
                hqPolicySel.innerHTML = pgHqPolicyScopeOptionsHtml(filt);
                var ok2 = false;
                var ji;
                for (ji = 0; ji < hqPolicySel.options.length; ji++) {
                  if (hqPolicySel.options[ji].value === prev) {
                    ok2 = true;
                    break;
                  }
                }
                if (ok2) hqPolicySel.value = prev;
                syncInfoChargebackFromPolicy();
              }
              if (!hqPolicySel._hqPolicyInfoListenersBound) {
                hqPolicySel._hqPolicyInfoListenersBound = true;
                hqPolicySel.addEventListener('change', function () {
                  syncInfoChargebackFromPolicy();
                });
              }
              if (baseCurInfo && !baseCurInfo._hqPolicyBaseBoundInfo) {
                baseCurInfo._hqPolicyBaseBoundInfo = true;
                baseCurInfo.addEventListener('change', function () {
                  pgToggleUsdDependentCommissionFields(pane, this.value);
                  if (pane._hqCommissionTemplatesCacheInfo) {
                    var list = pane._hqCommissionTemplatesCacheInfo;
                    var bc = baseCurInfo.value;
                    var filt0 = pgFilterDeployedTemplatesForMerchant(list, bc);
                    var prev = hqPolicySel.value;
                    var filt = pgAugmentFilteredHqTemplates(filt0, list, prev);
                    hqPolicySel.innerHTML = pgHqPolicyScopeOptionsHtml(filt);
                    var ok3 = false;
                    var k;
                    for (k = 0; k < hqPolicySel.options.length; k++) {
                      if (hqPolicySel.options[k].value === prev) {
                        ok3 = true;
                        break;
                      }
                    }
                    if (ok3) hqPolicySel.value = prev;
                    syncInfoChargebackFromPolicy();
                  } else {
                    window.PG_API.hqDefaultCommission().then(function (hqd) {
                      pane._hqCommissionTemplatesCacheInfo = (hqd && hqd.templates) ? hqd.templates : [];
                      var filt0 = pgFilterDeployedTemplatesForMerchant(pane._hqCommissionTemplatesCacheInfo, baseCurInfo.value);
                      var prevL = hqPolicySel.value;
                      var filt2 = pgAugmentFilteredHqTemplates(filt0, pane._hqCommissionTemplatesCacheInfo, prevL);
                      hqPolicySel.innerHTML = pgHqPolicyScopeOptionsHtml(filt2);
                      var okL = false;
                      var z;
                      for (z = 0; z < hqPolicySel.options.length; z++) {
                        if (hqPolicySel.options[z].value === prevL) {
                          okL = true;
                          break;
                        }
                      }
                      if (okL) hqPolicySel.value = prevL;
                      syncInfoChargebackFromPolicy();
                    }).catch(function () {});
                  }
                });
              }
              window.PG_API.hqDefaultCommission().then(refreshHqPolicyInfo).catch(function () {});
            }
          }
          if (cdInfo === 'MERCHANT') {
            fillChargebackPolicySelectsInRoot(form, data.chargebackPolicyId);
          }
          if (cdInfo === 'MERCHANT') {
            var holdRateFollowEl = pane.querySelector('#compInfoDetailForm [name="holdRateFollowHq"]');
            if (holdRateFollowEl && !holdRateFollowEl._infoHoldToggleBound) {
              holdRateFollowEl._infoHoldToggleBound = true;
              function toggleHoldRateCustom(useHq) {
                pane.querySelectorAll('#compInfoDetailForm .hold-rate-custom-only').forEach(function (el) {
                  el.style.display = useHq === 'Y' ? 'none' : '';
                });
              }
              holdRateFollowEl.addEventListener('change', function () { toggleHoldRateCustom(this.value); });
              toggleHoldRateCustom(holdRateFollowEl.value || 'Y');
            }
            (function bindFeeVatRateToggleInfo() {
              var root = pane.querySelector('#compInfoDetailForm') || pane;
              var sel = root.querySelector('[name="feeVatApplyYn"]');
              if (!sel || sel._pgFeeVatToggleBound) return;
              sel._pgFeeVatToggleBound = true;
              function syncFv() {
                var on = String(sel.value || '').toUpperCase() === 'Y';
                root.querySelectorAll('.fee-vat-rate-only').forEach(function (el) { el.style.display = on ? '' : 'none'; });
                var rateInp = root.querySelector('[name="feeVatRatePct"]');
                if (rateInp) rateInp.disabled = !on;
              }
              sel.addEventListener('change', syncFv);
              syncFv();
            })();
            initBankByCountry(pane);
            initCountryBankGroup(pane);
            initCountryAddressGroup(pane);
          }
          var allowBrandingFetch = (cdInfo === 'HEADQUARTERS' || cdInfo === 'REGIONAL' || cdInfo === 'MASTER_DIST')
            || (cdInfo === 'MERCHANT' && String(data.brandingEditAllowedYn || '').toUpperCase() === 'Y');
          if (allowBrandingFetch && window.PG_API.orgBranding) {
            window.PG_API.orgBranding(compId).then(function (b) {
              pgBrandingFillImageDisplayFromFetch(pane, b);
              var themeSel = pane.querySelector('#brandingTheme');
              var hostEl = pane.querySelector('#brandingBrandHost');
              var siteNameEl = pane.querySelector('#brandingSiteName');
              if (themeSel && b.theme) themeSel.value = b.theme || 'DEFAULT';
              if (hostEl) hostEl.value = (b.brandHost != null && b.brandHost !== undefined) ? b.brandHost : '';
              if (siteNameEl) siteNameEl.value = (b.siteName != null && b.siteName !== undefined) ? b.siteName : '';
            }).catch(function () {});
          }
        }
        var paymentInfoCard = pane.querySelector('#webPaymentCard') || pane.querySelector('#pgInfoCard');
        if (paymentInfoCard) {
          paymentInfoCard.style.display = (data.compDiv === 'MERCHANT') ? '' : 'none';
          var paymentUrlEl = pane.querySelector('#paymentUrlDisplay');
          if (paymentUrlEl && data.compId && data.compDiv === 'MERCHANT') {
            var base = (window.SITE_CONFIG && window.SITE_CONFIG.paymentBaseUrl) || (window.location.origin || '');
            paymentUrlEl.value = base.replace(/\/$/, '') + '/pay/' + encodeURIComponent(String(data.compId).trim());
          } else if (paymentUrlEl) paymentUrlEl.value = '';
        }
        (function () {
          var scopeId = null;
          var ensureCc = '';
          if (data.compDiv === 'MERCHANT') {
            if (data.masterDistScopeOrgId != null && String(data.masterDistScopeOrgId).trim() !== '') {
              scopeId = String(data.masterDistScopeOrgId).trim();
            } else if (data.orgUnitId != null && String(data.orgUnitId).trim() !== '') {
              scopeId = String(data.orgUnitId).trim();
            } else if (data.parentId != null && String(data.parentId).trim() !== '') {
              scopeId = String(data.parentId).trim();
            }
            if (data.calcCycle != null && String(data.calcCycle).trim() !== '') ensureCc = String(data.calcCycle).trim();
          }
          pgRefreshCalcCycleSelects(form, data.calcCycle, scopeId, ensureCc).catch(function () {});
        })();
        pane._lastCompInfoDetailData = data;
        pgBindBrandingBrowse(pane);
        var compDivSel = form.querySelector('[name="compDiv"]');
        if (formUrl === '/comp/myCompMng' && compDivSel) {
          compDivSel.disabled = true;
        }
        if (formUrl === '/comp/myCompMng') {
          applyMyCompBrandingPermission(pane, form, data.compDiv || '', data.brandingEditAllowedYn || 'N');
        }
        if (formUrl === '/comp/myCompMng' || formUrl === '/comp/compInfo') {
          applyCompInfoDetailMainCardHeader(pane, formUrl, data.compDiv || '');
        }
        var copyBtn = pane.querySelector('#paymentUrlCopyBtn');
        if (copyBtn && !copyBtn._bound) {
          copyBtn._bound = true;
          copyBtn.addEventListener('click', function () {
            var inp = pane.querySelector('#paymentUrlDisplay');
            if (inp && inp.value) {
              navigator.clipboard.writeText(inp.value).then(function () { alert('복사되었습니다.'); }).catch(function () { alert('복사 실패'); });
            }
          });
        }
        pgApplyFooterCopyright(pane, data);
        var card = pane.querySelector('#compInfoDetailCard');
        if (card) card.scrollIntoView({ behavior: 'smooth', block: 'start' });
        applyCompInfoHeadquartersVisibility(form, data.compDiv || '');
        applyAssistantPwdUi(form, data);
      }).catch(function (e) { alert(e && e.message ? e.message : '상세 조회 실패'); }).finally(function () {
        var formUrlFinal = pane.getAttribute('formurl') || '';
        if (typeof window.applyPagePermissionToPane === 'function') {
          window.applyPagePermissionToPane(pane, formUrlFinal);
        }
        if ((formUrlFinal === '/comp/myCompMng' || formUrlFinal === '/comp/compInfo') && pane._lastCompInfoDetailData) {
          applyReadOnlyCompInfoDetailIfOwn(pane, pane._lastCompInfoDetailData, formUrlFinal);
        }
        if (dimm) dimm.style.display = 'none';
      });
    }
    if (url === '/comp/compMngTree' && !pane._compMngTreeDblclickBound) {
      pane._compMngTreeDblclickBound = true;
      pane.addEventListener('dblclick', function (e) {
        var tr = e.target && e.target.closest ? e.target.closest('tr') : null;
        if (!tr || !tr.closest('#grid_' + tabId)) return;
        if (tr.classList.contains('empty-state-cell') || !tr.getAttribute('data-row')) return;
        if (e.target && e.target.closest && e.target.closest('.tree-toggle')) return;
        var dataRow = tr.getAttribute('data-row');
        var compId = '';
        if (dataRow) {
          try {
            var row = JSON.parse(decodeURIComponent(dataRow));
            compId = row.compId || '';
          } catch (e) {}
        }
        if (compId) {
          try {
            sessionStorage.setItem('pg_comp_detail_compId', compId);
            sessionStorage.setItem('pg_comp_detail_compDiv', row.compDiv || '');
          } catch (e) {}
          fnTopMenuMove('/comp/compDetail', null, '업체정보');
        }
      });
    }
    if (url === '/comp/compInfo' || url === '/comp/myCompMng') {
      initCountryAddressGroup(pane);
      initIntlPhoneFields(pane);
      var compInfoDetailBtn = pane.querySelector('#compInfoDetailBtn');
      if (compInfoDetailBtn) {
        compInfoDetailBtn.addEventListener('click', function () {
          // 업체정보조회(/comp/myCompMng)는 항상 로그인 소속 업체코드를 조회한다.
          if (url === '/comp/myCompMng') {
            var myCid = '';
            try {
              var su = JSON.parse(sessionStorage.getItem('pg_admin_user') || '{}');
              myCid = (su && su.compId) ? String(su.compId).trim() : '';
            } catch (e0) {}
            if (myCid) {
              loadCompDetailIntoForm(pane, myCid);
              return;
            }
            if (window.PG_API && window.PG_API.authMe) {
              window.PG_API.authMe().then(function (r) {
                var d0 = r && r.data ? r.data : r;
                var cid0 = d0 && d0.compId ? String(d0.compId).trim() : '';
                if (!cid0) { alert('소속 업체코드를 확인할 수 없습니다. 다시 로그인해 주세요.'); return; }
                try {
                  var prev0 = JSON.parse(sessionStorage.getItem('pg_admin_user') || '{}');
                  prev0.compId = cid0;
                  prev0.orgLevel = d0.orgLevel;
                  prev0.orgUnitId = d0.orgUnitId;
                  sessionStorage.setItem('pg_admin_user', JSON.stringify(prev0));
                } catch (e1) {}
                loadCompDetailIntoForm(pane, cid0);
              }).catch(function () { alert('소속 업체 정보를 불러오지 못했습니다.'); });
              return;
            }
            alert('소속 업체코드를 확인할 수 없습니다.');
            return;
          }
          var grid = pane.querySelector('#grid_' + tabId + ' tbody');
          if (!grid) return;
          var checked = grid.querySelector('tr .grid-row-check:checked');
          if (!checked) { alert('그리드에서 한 건을 선택한 뒤 [상세] 버튼을 눌러주세요.'); return; }
          var tr = checked.closest('tr');
          var compId = '';
          var dr = tr && tr.getAttribute('data-row');
          if (dr) {
            try {
              var row0 = JSON.parse(decodeURIComponent(dr));
              compId = row0 && row0.compId ? String(row0.compId).trim() : '';
            } catch (e2) {}
          }
          if (!compId) {
            var tds = tr.querySelectorAll('td');
            var cfg = window.PG_SCREENS && (window.PG_SCREENS.getMenuScreens()['/comp/compInfo'] || window.PG_SCREENS.getMenuScreens()['/comp/myCompMng']);
            var cols = (cfg && cfg.columns) || [];
            var compIdIdx = cols.findIndex(function (c) { return c.key === 'compId'; });
            if (compIdIdx < 0) compIdIdx = 1;
            compId = (tds[compIdIdx] && tds[compIdIdx].textContent) ? tds[compIdIdx].textContent.trim() : '';
          }
          if (!compId) { alert('업체코드를 찾을 수 없습니다.'); return; }
          loadCompDetailIntoForm(pane, compId);
        });
      }
      var compInfoUpdateBtn = pane.querySelector('#compInfoUpdateBtn');
      if (compInfoUpdateBtn) {
        compInfoUpdateBtn.addEventListener('click', function () {
          if (getPagePermissionForUrl(url) === 'OBSERVER') return;
          var form = pane.querySelector('#compInfoDetailForm');
          if (!form) return;
          initIntlPhoneFields(form);
          ['ceoMobile', 'compTel', 'fax', 'settleTelNo', 'contactTel'].forEach(function (n) { syncIntlPhoneHidden(form, n); });
          var compIdEl = form.querySelector('[name="compId"]');
          var compId = compIdEl && compIdEl.value ? compIdEl.value.trim() : '';
          if (!compId) { alert('업체코드가 없습니다. 먼저 [상세]로 조회하세요.'); return; }
          var fd = {};
          form.querySelectorAll('input, select, textarea').forEach(function (el) {
            if (el.name && el.type !== 'file' && el.name !== 'pgOperational') {
              if (el.name.indexOf('__phone_') === 0) return;
              fd[el.name] = el.value;
            }
          });
          if (fd.countryCd === 'OTHER') { fd.bankCd = fd.bankCdText || fd.bankCd; delete fd.bankCdText; }
          if (fd.addrCountryCd === 'OTHER') { fd.addrCountryCd = fd.addrCountryCdOther || ''; delete fd.addrCountryCdOther; }
          fd.compId = compId;
          if (url === '/comp/myCompMng') {
            var newLoginId = fd.loginId ? String(fd.loginId).trim() : '';
            var checkedLoginId = form.getAttribute('data-login-id-checked') || '';
            if (newLoginId && checkedLoginId !== newLoginId) {
              alert('대표 아이디 중복확인을 먼저 진행하세요.');
              return;
            }
            var newAssistantLoginId = fd.assistantLoginId ? String(fd.assistantLoginId).trim() : '';
            var checkedAssistantLoginId = form.getAttribute('data-assistant-login-id-checked') || '';
            if (newAssistantLoginId && checkedAssistantLoginId !== newAssistantLoginId) {
              alert('보조 아이디 중복확인을 먼저 진행하세요.');
              return;
            }
          }
          if (fd.regType != null) { fd.regNo = (fd.regType || 'CORP') + '|' + (fd.regNo || ''); delete fd.regType; }
          var compDivVal = fd.compDiv || '';
          if (compDivVal === 'REGIONAL') {
            var bc = [fd.baseCurrency1, fd.baseCurrency2, fd.baseCurrency3].filter(function (v) { return v && v.trim(); });
            fd.baseCurrency = bc.join(',');
          } else if (compDivVal === 'MASTER_DIST') {
            fd.baseCurrency = (fd.baseCurrency || '').trim();
          }
          if (compDivVal === 'REGIONAL' || compDivVal === 'MASTER_DIST') {
            delete fd.baseCurrency1;
            delete fd.baseCurrency2;
            delete fd.baseCurrency3;
          }
          if (compDivVal === 'MERCHANT') {
            var pgTbody = form.querySelector('#pgBindingTbody');
            if (pgTbody) {
              var operationalVal = form.querySelector('input[name="pgOperational"]:checked');
              var operationalIdx = operationalVal ? parseInt(operationalVal.value, 10) : 0;
              var bindings = [];
              pgTbody.querySelectorAll('tr').forEach(function (tr, i) {
                var sel = function (f) { var e = tr.querySelector('[data-field="' + f + '"]'); return e ? e.value : ''; };
                var pgCd = sel('pgCd');
                if (pgCd) {
                  bindings.push({
                    pgCd: pgCd,
                    activationYn: sel('activationYn') || 'Y',
                    operationalYn: i === operationalIdx ? 'Y' : 'N',
                    payMethod: sel('payMethod') || 'WEB',
                    mid: sel('mid'),
                    rootNo: sel('rootNo'),
                    apiKey: sel('apiKey'),
                    ivKey: sel('ivKey'),
                    installmentYn: sel('installmentYn') || 'N',
                    maxInstallmentMonths: sel('maxInstallmentMonths')
                  });
                }
              });
              fd.pgBindings = JSON.stringify(bindings);
            }
          }
          if (url === '/comp/myCompMng' && compDivVal === 'MERCHANT') {
            var astResetRow = form.querySelector('#assistantPwdResetRow');
            var inAstResetMode = astResetRow && !astResetRow.classList.contains('d-none');
            if (inAstResetMode) {
              delete fd.assistantPwd;
            } else {
              var apw0 = fd.assistantPwd ? String(fd.assistantPwd).trim() : '';
              if (apw0 && (form.getAttribute('data-assistant-pwd-confirmed') || '') !== 'Y') {
                alert('보조 비밀번호 입력 후 옆의 [저장]으로 확정하세요.');
                return;
              }
            }
          }
          if (!pgConfirmBeforeSave('저장하시겠습니까?')) return;
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.compUpdate(fd).then(function () {
            if (compDivVal === 'MERCHANT') {
              var settleFd = {};
              var settleKeys = ['withdrawRestrictType', 'withdrawStartTime', 'withdrawEndTime', 'payLimitDefault', 'payLimitExtra', 'holdRate', 'holdDays', 'calcCloseTime', 'calcStartTime', 'transferCycleDays', 'calcProcType', 'transferType', 'autoTransferMin', 'payHoldYn', 'calcExcludeYn', 'calcExcludeTarget', 'calcMinAmt', 'transferExecTime', 'feeVatApplyYn', 'feeVatRatePct'];
              settleKeys.splice(5, 0, 'calcCycle');
              settleKeys.forEach(function (k) {
                if (fd[k] !== undefined && fd[k] !== null && fd[k] !== '') settleFd[k] = fd[k];
              });
              if (Object.keys(settleFd).length > 0) {
                return window.PG_API.settlementSettingSave(compId, settleFd);
              }
            }
            return Promise.resolve();
          }).then(function () {
            var canBranding = (compDivVal === 'HEADQUARTERS' || compDivVal === 'REGIONAL' || compDivVal === 'MASTER_DIST')
              || (compDivVal === 'MERCHANT' && String(fd.brandingEditAllowedYn || '').toUpperCase() === 'Y');
            if (!canBranding) return Promise.resolve();
            if (!window.PG_API.orgBrandingUpload || !window.PG_API.orgBrandingSave) return Promise.resolve();
            var mainFile = form.querySelector('#brandingMainImageFile');
            var firstFile = form.querySelector('#brandingFirstLogoImageFile');
            var logoFile = form.querySelector('#brandingLogoImageFile');
            var popconFile = form.querySelector('#brandingPopconImageFile');
            var themeEl = form.querySelector('#brandingTheme');
            var hostEl = form.querySelector('#brandingBrandHost');
            var siteNameEl = form.querySelector('#brandingSiteName');
            var chain = Promise.resolve();
            if (mainFile && mainFile.files && mainFile.files[0]) {
              var _myMainF = mainFile.files[0];
              chain = chain.then(function () {
                return window.PG_API.orgBrandingUpload(compId, 'main', _myMainF).then(function (data) {
                  pgBrandingSetImageDisplayInput(form, 'main', data, _myMainF);
                  return data;
                });
              });
            }
            if (logoFile && logoFile.files && logoFile.files[0]) {
              var _myLogoF = logoFile.files[0];
              chain = chain.then(function () {
                return window.PG_API.orgBrandingUpload(compId, 'logo', _myLogoF).then(function (data) {
                  pgBrandingSetImageDisplayInput(form, 'logo', data, _myLogoF);
                  try {
                    if (window.PG_applySidebarLogo && data && data.url) {
                      var _apiBase2 = (typeof window.PG_assetApiBase === 'function')
                        ? window.PG_assetApiBase()
                        : ((window.PG_API_BASE || '').replace(/\/$/, '') || window.location.origin);
                      var _src2 = /^https?:\/\//i.test(String(data.url)) ? String(data.url) : (_apiBase2 + String(data.url));
                      window.PG_applySidebarLogo(_src2 + (_src2.indexOf('?') >= 0 ? '&' : '?') + 'v=' + Date.now());
                    }
                  } catch (eLogo1) {}
                  return data;
                });
              });
            }
            if (firstFile && firstFile.files && firstFile.files[0]) {
              var _myFirstF = firstFile.files[0];
              chain = chain.then(function () {
                return window.PG_API.orgBrandingUpload(compId, 'first', _myFirstF).then(function (data) {
                  pgBrandingSetImageDisplayInput(form, 'first', data, _myFirstF);
                  return data;
                });
              });
            }
            if (popconFile && popconFile.files && popconFile.files[0]) {
              var _myPopconF = popconFile.files[0];
              chain = chain.then(function () {
                return window.PG_API.orgBrandingUpload(compId, 'popcon', _myPopconF).then(function (data) {
                  pgBrandingSetImageDisplayInput(form, 'popcon', data, _myPopconF);
                  return data;
                });
              });
            }
            if (themeEl || hostEl) {
              chain = chain.then(function () { return window.PG_API.orgBrandingSave(compId, (themeEl && themeEl.value) ? themeEl.value : 'DEFAULT', hostEl ? hostEl.value : undefined, siteNameEl ? siteNameEl.value : undefined); });
            }
            return chain.catch(function (eBrand) {
              try { console.warn('branding save failed:', eBrand); } catch (e0) {}
              return Promise.reject(eBrand);
            });
          }).then(function () {
            pgApplyFooterCopyright(pane, fd);
            alert('저장되었습니다.');
            if (url === '/comp/myCompMng') {
              /* 가맹점 업체정보: 저장 후 같은 탭·같은 화면 유지(상세 재조회 시 스크롤/맥락이 바뀌는 문제 방지) */
            } else {
              doSearch(pane, tabId, 1);
            }
          }).catch(function (e) { alert(e && e.message ? e.message : '수정 저장 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
      }
      pane.addEventListener('click', function (e) {
        var dupBtn = e.target && e.target.closest ? e.target.closest('button[data-action="중복확인"]') : null;
        if (!dupBtn || !pane.contains(dupBtn)) return;
        var fld = dupBtn.getAttribute('data-field') || '';
        if (fld !== 'loginId' && fld !== 'assistantLoginId') return;
        var form = pane.querySelector('#compInfoDetailForm');
        var idEl = form ? form.querySelector('[name="' + fld + '"]') : null;
        var lid = idEl && idEl.value ? String(idEl.value).trim() : '';
        if (!lid) { alert('아이디를 입력하세요.'); return; }
        var dimmDup = document.getElementById('dimm');
        if (dimmDup) dimmDup.style.display = 'flex';
        window.PG_API.compCheckLoginId(lid).then(function (r) {
          var ok = !!(r && r.available);
          if (ok) {
            if (form) form.setAttribute(fld === 'loginId' ? 'data-login-id-checked' : 'data-assistant-login-id-checked', lid);
            alert('사용 가능한 로그인ID입니다.');
          } else {
            if (form) form.setAttribute(fld === 'loginId' ? 'data-login-id-checked' : 'data-assistant-login-id-checked', '');
            alert('이미 사용 중인 로그인ID입니다.');
          }
        }).catch(function (err) {
          if (form) form.setAttribute(fld === 'loginId' ? 'data-login-id-checked' : 'data-assistant-login-id-checked', '');
          alert(err && err.message ? err.message : '중복확인 실패');
        }).finally(function () { if (dimmDup) dimmDup.style.display = 'none'; });
      });
      pane.addEventListener('click', function (e) {
        var pwdResetBtn = e.target && e.target.closest ? e.target.closest('#compDetailPwdResetBtn, [data-action="비밀번호 초기화"]') : null;
        if (!pwdResetBtn || !pane.contains(pwdResetBtn)) return;
        var form = pane.querySelector('#compInfoDetailForm');
        if (!form || !form.contains(pwdResetBtn)) return;
        var compIdEl = form.querySelector('[name="compId"]');
        var compId = compIdEl && compIdEl.value ? compIdEl.value.trim() : '';
        if (!compId) { alert('업체코드가 없습니다.'); return; }
        if (!confirm('대표 계정 비밀번호를 초기화하시겠습니까? (임시: 로그인ID+1!)')) return;
        var dimmPw = document.getElementById('dimm');
        if (dimmPw) dimmPw.style.display = 'flex';
        window.PG_API.compResetPassword(compId).then(function (r) {
          var pwd = (r && r.data && r.data.tempPassword) ? r.data.tempPassword : (r && r.tempPassword) ? r.tempPassword : '';
          alert(pwd ? '비밀번호가 초기화되었습니다. 임시비밀번호: ' + pwd : '비밀번호가 초기화되었습니다.');
        }).catch(function (err) { alert(err && err.message ? err.message : '비밀번호 초기화 실패'); }).finally(function () { if (dimmPw) dimmPw.style.display = 'none'; });
      });
      pane.addEventListener('click', function (e) {
        var asstReset = e.target && e.target.closest ? e.target.closest('#assistantPwdResetBtn, [data-action="보조 비밀번호 초기화"]') : null;
        if (!asstReset || !pane.contains(asstReset)) return;
        var formAs = pane.querySelector('#compInfoDetailForm');
        if (!formAs || !formAs.contains(asstReset)) return;
        var compIdEl = formAs.querySelector('[name="compId"]');
        var compIdAs = compIdEl && compIdEl.value ? compIdEl.value.trim() : '';
        if (!compIdAs) { alert('업체코드가 없습니다.'); return; }
        if (!confirm('보조 계정 비밀번호를 초기화하시겠습니까? (임시: 보조로그인ID+1!)')) return;
        var dimmAs = document.getElementById('dimm');
        if (dimmAs) dimmAs.style.display = 'flex';
        window.PG_API.compResetAssistantPassword(compIdAs).then(function (r) {
          var pwd = (r && r.data && r.data.tempPassword) ? r.data.tempPassword : (r && r.tempPassword) ? r.tempPassword : '';
          alert(pwd ? '비밀번호가 초기화되었습니다. 임시비밀번호: ' + pwd : '비밀번호가 초기화되었습니다.');
        }).catch(function (err) { alert(err && err.message ? err.message : '비밀번호 초기화 실패'); }).finally(function () { if (dimmAs) dimmAs.style.display = 'none'; });
      });
      if (url === '/comp/myCompMng') {
        var myCompForm = pane.querySelector('#compInfoDetailForm');
        if (myCompForm) {
          var loginIdInput = myCompForm.querySelector('[name="loginId"]');
          var assistantLoginIdInput = myCompForm.querySelector('[name="assistantLoginId"]');
          if (loginIdInput && !loginIdInput._dupResetBound) {
            loginIdInput._dupResetBound = true;
            loginIdInput.addEventListener('input', function () {
              myCompForm.setAttribute('data-login-id-checked', '');
            });
          }
          if (assistantLoginIdInput && !assistantLoginIdInput._dupResetBound) {
            assistantLoginIdInput._dupResetBound = true;
            assistantLoginIdInput.addEventListener('input', function () {
              myCompForm.setAttribute('data-assistant-login-id-checked', '');
              myCompForm.removeAttribute('data-assistant-pwd-confirmed');
            });
          }
          if (myCompForm && !myCompForm._assistantPwdSaveBound) {
            myCompForm._assistantPwdSaveBound = true;
            myCompForm.addEventListener('click', function (ev) {
              var btn = ev.target && ev.target.closest ? ev.target.closest('button[data-field="assistantPwd"][data-action="저장"]') : null;
              if (!btn || !myCompForm.contains(btn)) return;
              var pwdInput = myCompForm.querySelector('[name="assistantPwd"]');
              var v = pwdInput && pwdInput.value ? String(pwdInput.value).trim() : '';
              if (!v) { alert('비밀번호를 입력하세요.'); return; }
              if (v.length < 8) { alert('비밀번호는 8자 이상 입력하세요.'); return; }
              myCompForm.setAttribute('data-assistant-pwd-confirmed', 'Y');
              alert('비밀번호가 확정되었습니다. 하단 [수정 저장]으로 반영하세요.');
            });
            var apwIn = myCompForm.querySelector('[name="assistantPwd"]');
            if (apwIn && !apwIn._assistantPwdInBound) {
              apwIn._assistantPwdInBound = true;
              apwIn.addEventListener('input', function () {
                myCompForm.removeAttribute('data-assistant-pwd-confirmed');
              });
            }
          }
        }
        function runMyCompAutoLoad() {
          var cid = '';
          try {
            var u = JSON.parse(sessionStorage.getItem('pg_admin_user') || '{}');
            cid = (u.compId || '').trim();
          } catch (e) {}
          if (cid) {
            loadCompDetailIntoForm(pane, cid);
            return;
          }
          if (window.PG_API && window.PG_API.authMe) {
            window.PG_API.authMe().then(function (r) {
              var d = r && r.data ? r.data : r;
              if (d && d.compId) {
                try {
                  var prev = JSON.parse(sessionStorage.getItem('pg_admin_user') || '{}');
                  prev.compId = d.compId;
                  prev.orgLevel = d.orgLevel;
                  prev.orgUnitId = d.orgUnitId;
                  sessionStorage.setItem('pg_admin_user', JSON.stringify(prev));
                } catch (e2) {}
                loadCompDetailIntoForm(pane, d.compId);
              } else {
                alert('소속 업체코드를 확인할 수 없습니다. 다시 로그인해 주세요.');
              }
            }).catch(function () { alert('소속 업체 정보를 불러오지 못했습니다.'); });
          }
        }
        setTimeout(runMyCompAutoLoad, 50);
      }
    }
    if (url === '/comp/compDetail') {
      if (typeof window.applyPagePermissionToPane === 'function') {
        window.applyPagePermissionToPane(pane, '/comp/compDetail');
      }
      bindParentCompSearchModal(pane);
      function toggleByCompDiv(compDiv) {
        var isMerchant = compDiv === 'MERCHANT';
        var isHeadquarters = compDiv === 'HEADQUARTERS';
        var isRegional = compDiv === 'REGIONAL';
        var isMasterDist = compDiv === 'MASTER_DIST';
        var isBranchAgencySales = compDiv === 'BRANCH' || compDiv === 'AGENCY' || compDiv === 'SALES_OFFICE';
        var isDistributor = isMasterDist || isBranchAgencySales;
        var showAccount = isMerchant || isDistributor || isRegional;
        pane.querySelectorAll('.merchant-only-section').forEach(function (card) {
          if (isMerchant) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.regional-only-section').forEach(function (card) {
          if (isRegional) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.master-dist-only-section').forEach(function (card) {
          if (isMasterDist) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.distributor-only-section').forEach(function (card) {
          if (isDistributor) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.distributor-merchant-no-regional-section').forEach(function (card) {
          if (isMerchant || isDistributor) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.distributor-or-merchant-section').forEach(function (card) {
          if (showAccount) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.branch-agency-sales-hide-section').forEach(function (card) {
          if (isBranchAgencySales) card.classList.add('d-none'); else card.classList.remove('d-none');
        });
        var isRegionalOrMasterDist = isRegional || isMasterDist;
        pane.querySelectorAll('.regional-or-master-dist-only-section').forEach(function (card) {
          if (isRegionalOrMasterDist) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        var isHeadOfficeTier = isHeadquarters || isRegional || isMasterDist;
        pane.querySelectorAll('.head-office-tier-only-section').forEach(function (card) {
          if (isHeadOfficeTier) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.merchant-regional-master-commission-section').forEach(function (card) {
          if (isMerchant || isRegional || isMasterDist) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
      }
      var compId = '';
      var storedCompDiv = '';
      try {
        compId = sessionStorage.getItem('pg_comp_detail_compId') || '';
        storedCompDiv = sessionStorage.getItem('pg_comp_detail_compDiv') || '';
      } catch (e) {}
      if (!compId) {
        pane.innerHTML = '<div class="card"><div class="card-body"><p class="text-muted">업체코드가 없습니다. 업체관리에서 행을 더블클릭하여 조회하세요.</p><button type="button" class="btn btn-secondary btn-sm" id="compDetailListBtn">목록</button></div></div>';
      } else {
      toggleByCompDiv(storedCompDiv);
      var dimm = document.getElementById('dimm');
      if (dimm) dimm.style.display = 'flex';
      window.PG_API.compDetail(compId).then(function (data) {
        if (!data) return;
        var form = pane.querySelector('#compDetailForm');
        if (!form) return;
        initAttachmentSection(pane);
        var allFields = ['compId', 'parentComp', 'compNm', 'compDiv', 'regNo', 'bizType', 'industry', 'bizNature', 'product', 'homepage', 'settleName', 'settleTelNo', 'ceoNm', 'ceoMobile', 'compTel', 'fax', 'zipCode', 'addr', 'addrDetail', 'addrEtc', 'addrCountryCd', 'addrCountryCdOther', 'email', 'siteUrl', 'siteSummary', 'useYn', 'loginId', 'bankCd', 'transferFee', 'cryptoTransferFee', 'accountNo', 'accountHolder', 'commissionConfigAllowed', 'webPaymentUseYn', 'baseCurrency', 'remark', 'settleType', 'commissionRate', 'limitAmt', 'countryCd', 'countryCdOther', 'swift', 'branchName', 'branchAddr', 'contactTel', 'walletAddress', 'networkName', 'withdrawRestrictType', 'withdrawStartTime', 'withdrawEndTime', 'payLimitDefault', 'payLimitExtra', 'payLimitAlertSms', 'holdRateFollowHq', 'holdRate', 'holdDays', 'commissionFollowHq', 'hqPolicyScope', 'failFee', 'usageRate', 'payRate', 'cancelRate', 'voidFeePerTx', 'manualVoidFeePerTx', 'refundRate', 'commissionMemo', 'feeSettlementPerTx', 'remittanceTransferFee', 'usdtTransferFeeUsd', 'feeUsdt', 'feeFx', 'fee3dsRate', 'chargebackFeePerTx', 'chargebackPolicyId', 'payFollowMerchantUseYn', 'payFollowAutoVoidYn', 'payFollowEmailVoidYn', 'payFollowAutoRefundYn', 'payFollowForceRefundYn', 'calcCycle', 'calcProcType', 'calcCloseTime', 'transferType', 'transferCycleDays', 'autoTransferMin', 'calcMinAmt', 'transferExecTime', 'calcExcludeYn', 'calcExcludeTarget', 'calcStartTime', 'payHoldYn', 'feeVatApplyYn', 'feeVatRatePct', 'defaultProductName', 'defaultProductCode', 'defaultProductAmount', 'defaultProductDesc', 'notifyUrlBackground', 'notifyUrlResult', 'notifyUrl1', 'notifyUrl2', 'notifyUrl3', 'notifyUrl4', 'remitterName', 'balanceNotifyAmt', 'suspiciousNotifyAmt', 'overseasLoginNotifyAmt', 'tempPwdNotifyAmt', 'nonTranCriterionMonth', 'sameCardLimitWebDay', 'sameCardLimitWebTimes', 'sameCardLimitWebAmt', 'sameCardLimitTerminalDay', 'sameCardLimitTerminalTimes', 'sameCardLimitTerminalAmt', 'dailyUsageFee', 'depositNameLookup', 'transferAuthNo', 'autoConvertNewMemberLimit', 'newMemberDailyLimit', 'convertRefDate', 'convertDailyLimit', 'applyStartDate', 'pgFeeGeneral', 'settleDiffMonthCnt', 'settleReportBankCd', 'pgFeeSamsung', 'smsFee', 'taxInvoiceEmail', 'settleAccountNo', 'directFee', 'solutionFee', 'settleAccountHolder', 'withdrawRestrictType', 'withdrawRestrictStartTime', 'withdrawRestrictEndTime', 'terminalPayRestrict', 'webPayRestrict', 'defaultFeeHq', 'defaultFeeDist', 'defaultFeeBranch', 'defaultFeeAgency', 'defaultFeeSalesOffice', 'defaultPayLimitPerTx', 'defaultPayLimitDay', 'defaultPayLimitMonth', 'defaultPayLimitYearCorp', 'defaultPayLimitYearInd', 'copyright', 'holidayProfileName', 'holidayProfileCountry', 'holidayCountryCode', 'holidayCountryCodes', 'businessHolidayRangesJson', 'businessHolidayExtraDates'];
        allFields.forEach(function (k) {
          var el = form.querySelector('[name="' + k + '"]');
          if (el && data[k] != null) {
            var nf2 = pgFmtCompDetailNumericField(k, data[k]);
            el.value = nf2 != null ? nf2 : data[k];
          }
        });
        ['payFollowMerchantUseYn', 'payFollowAutoVoidYn', 'payFollowEmailVoidYn', 'payFollowAutoRefundYn', 'payFollowForceRefundYn'].forEach(function (k) {
          var el = form.querySelector('[name="' + k + '"]');
          if (el && el.tagName === 'SELECT' && (data[k] == null || data[k] === '')) el.value = '';
        });
        pgApplyFooterCopyright(pane, data);
        var parentCompEl = form.querySelector('[name="parentComp"]');
        if (parentCompEl) parentCompEl.value = normalizeParentCompDisplay(parentCompEl.value);
        initIntlPhoneFields(form);
        var holidayCountryCodeEl = form.querySelector('[name="holidayCountryCode"]');
        if (holidayCountryCodeEl && !holidayCountryCodeEl.value) {
          var legacyCodes = data.holidayCountryCodes ? String(data.holidayCountryCodes).split(',') : [];
          holidayCountryCodeEl.value = (legacyCodes[0] || 'KR').trim();
        }
        if (data.compDiv === 'REGIONAL' && data.baseCurrency) {
          var parts = String(data.baseCurrency).split(/,\s*/);
          ['baseCurrency1', 'baseCurrency2', 'baseCurrency3'].forEach(function (n, i) {
            var el = form.querySelector('[name="' + n + '"]');
            if (el) el.value = parts[i] || '';
          });
        }
        var pidEl = form.querySelector('input[name="parentId"]');
        if (!pidEl) {
          pidEl = document.createElement('input');
          pidEl.type = 'hidden';
          pidEl.name = 'parentId';
          form.appendChild(pidEl);
        }
        if (data.parentId != null) pidEl.value = String(data.parentId);
        var cc = data.countryCd;
        if (cc && cc !== 'JP' && cc !== 'KR' && cc !== 'TH') {
          var countrySel = form.querySelector('select[name="countryCd"]');
          var countryOther = form.querySelector('[name="countryCdOther"]');
          var bankText = form.querySelector('input[name="bankCdText"]');
          if (countrySel) { countrySel.value = 'OTHER'; countrySel.dispatchEvent(new Event('change')); }
          if (countryOther) countryOther.value = cc;
          if (bankText && data.bankCd != null) bankText.value = data.bankCd;
        } else if (cc === 'JP' || cc === 'KR' || cc === 'TH') {
          refreshCountryBankAfterFill(pane, data.bankCd);
        }
        var acc = data.addrCountryCd;
        if (acc && acc !== 'JP' && acc !== 'KR' && acc !== 'TH') {
          var addrCountrySel = form.querySelector('select[name="addrCountryCd"]');
          var addrCountryOther = form.querySelector('[name="addrCountryCdOther"]');
          if (addrCountrySel) { addrCountrySel.value = 'OTHER'; addrCountrySel.dispatchEvent(new Event('change')); }
          if (addrCountryOther) addrCountryOther.value = acc;
        }
        var regNoVal = data.regNo;
        if (regNoVal && regNoVal.indexOf('|') >= 0) {
          var parts = regNoVal.split('|');
          var rt = form.querySelector('[name="regType"]');
          var rn = form.querySelector('[name="regNo"]');
          if (rt) rt.value = (parts[0] === 'PERSONAL' || parts[0] === 'CORP') ? parts[0] : 'CORP';
          if (rn) rn.value = parts.length > 1 ? parts.slice(1).join('|') : '';
        }
        var apiCompDiv = (data.compDiv && data.compDiv !== '-') ? data.compDiv : storedCompDiv;
        toggleByCompDiv(apiCompDiv || storedCompDiv);
        if (apiCompDiv === 'REGIONAL' && window.PG_HQ_HOLIDAY && typeof window.PG_HQ_HOLIDAY.init === 'function') {
          window.PG_HQ_HOLIDAY.init(pane, { force: true });
        }
        initPgBindingList(pane, data.pgBindings, {
          rowActionMode: true,
          getCompId: function () {
            var el = pane.querySelector('#compDetailForm [name="compId"]');
            return el && el.value ? el.value.trim() : '';
          }
        });
        initRegionalCardLimitTable(pane, data.regionalCardLimits || []);
        initRegionalTerminalTable(pane, data.regionalTerminals || []);
        initRegionalHolidayProfileSelector(pane, form, data);
        applyCompParentMoveRules(form, data);
        var commissionFollowEl = pane.querySelector('[name="commissionFollowHq"]');
        if (commissionFollowEl && !commissionFollowEl._commissionToggleBound) {
          commissionFollowEl._commissionToggleBound = true;
          function toggleCommissionCustom(useHq) {
            pane.querySelectorAll('.commission-custom-only').forEach(function (el) {
              el.style.display = useHq === 'Y' ? 'none' : '';
            });
            pane.querySelectorAll('.hq-policy-only').forEach(function (el) {
              el.style.display = useHq === 'Y' ? '' : 'none';
            });
          }
          commissionFollowEl.addEventListener('change', function () { toggleCommissionCustom(this.value); });
          toggleCommissionCustom(commissionFollowEl.value || 'Y');
        }
        var hqPolicySelDetail = pane.querySelector('#compDetailForm [name="hqPolicyScope"]');
        var baseCurDetail = pane.querySelector('#compDetailForm [name="baseCurrency"]');
        if (hqPolicySelDetail) {
          function syncDetailChargebackFromPolicy() {
            var followVal = commissionFollowEl && commissionFollowEl.value ? String(commissionFollowEl.value).trim().toUpperCase() : 'Y';
            if (followVal !== 'Y') return;
            applyChargebackByHqPolicyScope(form, pane._hqCommissionTemplatesCacheDetail || [], hqPolicySelDetail.value);
          }
          function refreshHqPolicyDetail(hqd) {
            var list = (hqd && hqd.templates) ? hqd.templates : [];
            pane._hqCommissionTemplatesCacheDetail = list;
            var bc = (baseCurDetail && String(baseCurDetail.value || '').trim() !== '')
              ? baseCurDetail.value
              : ((data && data.baseCurrency) ? data.baseCurrency : '');
            pgToggleUsdDependentCommissionFields(pane, bc);
            var filt0 = pgFilterDeployedTemplatesForMerchant(list, bc);
            var prev = (data != null && Object.prototype.hasOwnProperty.call(data, 'hqPolicyScope'))
              ? (data.hqPolicyScope == null ? '' : String(data.hqPolicyScope))
              : (hqPolicySelDetail.value || '');
            var filt = pgAugmentFilteredHqTemplates(filt0, list, prev);
            hqPolicySelDetail.innerHTML = pgHqPolicyScopeOptionsHtml(filt);
            var okd = false;
            var di;
            for (di = 0; di < hqPolicySelDetail.options.length; di++) {
              if (hqPolicySelDetail.options[di].value === prev) {
                okd = true;
                break;
              }
            }
            if (okd) hqPolicySelDetail.value = prev;
            syncDetailChargebackFromPolicy();
          }
          if (!hqPolicySelDetail._hqPolicyDetailListenersBound) {
            hqPolicySelDetail._hqPolicyDetailListenersBound = true;
            hqPolicySelDetail.addEventListener('change', function () {
              syncDetailChargebackFromPolicy();
            });
          }
          if (baseCurDetail && !baseCurDetail._hqPolicyBaseBoundDetail) {
            baseCurDetail._hqPolicyBaseBoundDetail = true;
            baseCurDetail.addEventListener('change', function () {
              pgToggleUsdDependentCommissionFields(pane, this.value);
              if (pane._hqCommissionTemplatesCacheDetail) {
                var list = pane._hqCommissionTemplatesCacheDetail;
                var bc = baseCurDetail.value;
                var filt0 = pgFilterDeployedTemplatesForMerchant(list, bc);
                var prevD = hqPolicySelDetail.value;
                var filtD = pgAugmentFilteredHqTemplates(filt0, list, prevD);
                hqPolicySelDetail.innerHTML = pgHqPolicyScopeOptionsHtml(filtD);
                var ok4 = false;
                var d2;
                for (d2 = 0; d2 < hqPolicySelDetail.options.length; d2++) {
                  if (hqPolicySelDetail.options[d2].value === prevD) {
                    ok4 = true;
                    break;
                  }
                }
                if (ok4) hqPolicySelDetail.value = prevD;
                syncDetailChargebackFromPolicy();
              } else {
                window.PG_API.hqDefaultCommission().then(refreshHqPolicyDetail).catch(function () {});
              }
            });
          }
          window.PG_API.hqDefaultCommission().then(refreshHqPolicyDetail).catch(function () {});
          pgToggleUsdDependentCommissionFields(pane, (baseCurDetail && baseCurDetail.value) ? baseCurDetail.value : ((data && data.baseCurrency) ? data.baseCurrency : ''));
        }
        if (apiCompDiv === 'MERCHANT') {
          fillChargebackPolicySelectsInRoot(form, data.chargebackPolicyId);
        }
        var holdRateFollowEl = pane.querySelector('[name="holdRateFollowHq"]');
        if (holdRateFollowEl && !holdRateFollowEl._holdRateToggleBound) {
          holdRateFollowEl._holdRateToggleBound = true;
          function toggleHoldRateCustom(useHq) {
            pane.querySelectorAll('.hold-rate-custom-only').forEach(function (el) {
              el.style.display = useHq === 'Y' ? 'none' : '';
            });
          }
          holdRateFollowEl.addEventListener('change', function () { toggleHoldRateCustom(this.value); });
          toggleHoldRateCustom(holdRateFollowEl.value || 'Y');
        }
        (function bindFeeVatRateToggleDetail() {
          var root = pane;
          var sel = root.querySelector('[name="feeVatApplyYn"]');
          if (!sel || sel._pgFeeVatToggleBound) return;
          sel._pgFeeVatToggleBound = true;
          function syncFv() {
            var on = String(sel.value || '').toUpperCase() === 'Y';
            root.querySelectorAll('.fee-vat-rate-only').forEach(function (el) { el.style.display = on ? '' : 'none'; });
            var rateInp = root.querySelector('[name="feeVatRatePct"]');
            if (rateInp) rateInp.disabled = !on;
          }
          sel.addEventListener('change', syncFv);
          syncFv();
        })();
        var paymentInfoCard = pane.querySelector('#pgInfoCard') || pane.querySelector('#webPaymentCard');
        if (paymentInfoCard) {
          var divForPg = apiCompDiv || storedCompDiv;
          paymentInfoCard.style.display = (divForPg === 'MERCHANT') ? '' : 'none';
          var paymentUrlEl = pane.querySelector('#paymentUrlDisplay');
          if (paymentUrlEl && data.compId && divForPg === 'MERCHANT') {
            var base = (window.SITE_CONFIG && window.SITE_CONFIG.paymentBaseUrl) || (window.location.origin || '');
            paymentUrlEl.value = base.replace(/\/$/, '') + '/pay/' + encodeURIComponent(String(data.compId).trim());
          } else if (paymentUrlEl) paymentUrlEl.value = '';
        }
        var copyBtn = pane.querySelector('#paymentUrlCopyBtn');
        if (copyBtn && !copyBtn._bound) {
          copyBtn._bound = true;
          copyBtn.addEventListener('click', function () {
            var inp = pane.querySelector('#paymentUrlDisplay');
            if (inp && inp.value) {
              navigator.clipboard.writeText(inp.value).then(function () { alert('복사되었습니다.'); }).catch(function () { alert('복사 실패'); });
            }
          });
        }
        initBankByCountry(pane);
        initCountryBankGroup(pane);
        initCountryAddressGroup(pane);
        if ((apiCompDiv === 'HEADQUARTERS' || apiCompDiv === 'REGIONAL' || apiCompDiv === 'MASTER_DIST') && window.PG_API.orgBranding) {
          window.PG_API.orgBranding(compId).then(function (b) {
            pgBrandingFillImageDisplayFromFetch(pane, b);
            var themeSel = pane.querySelector('#brandingTheme');
            var hostEl = pane.querySelector('#brandingBrandHost');
            var siteNameEl = pane.querySelector('#brandingSiteName');
            if (themeSel && b.theme) themeSel.value = b.theme || 'DEFAULT';
            if (hostEl) hostEl.value = (b.brandHost != null && b.brandHost !== undefined) ? b.brandHost : '';
            if (siteNameEl) siteNameEl.value = (b.siteName != null && b.siteName !== undefined) ? b.siteName : '';
          }).catch(function () {});
        }
        pgBindBrandingBrowse(pane);
        (function () {
          var scopeId = null;
          var ensureCc = '';
          if (apiCompDiv === 'MERCHANT') {
            if (data.masterDistScopeOrgId != null && String(data.masterDistScopeOrgId).trim() !== '') {
              scopeId = String(data.masterDistScopeOrgId).trim();
            } else if (data.orgUnitId != null && String(data.orgUnitId).trim() !== '') {
              scopeId = String(data.orgUnitId).trim();
            } else if (data.parentId != null && String(data.parentId).trim() !== '') {
              scopeId = String(data.parentId).trim();
            }
            if (data.calcCycle != null && String(data.calcCycle).trim() !== '') ensureCc = String(data.calcCycle).trim();
          }
          pgRefreshCalcCycleSelects(form, data.calcCycle, scopeId, ensureCc).catch(function () {});
        })();
      }).catch(function (e) {
        pane.innerHTML = '<div class="card"><div class="card-body"><p class="text-danger">' + (e && e.message ? e.message : '조회 실패') + '</p><button type="button" class="btn btn-secondary btn-sm" id="compDetailListBtn">목록</button></div></div>';
      }).finally(function () {
        if (dimm) dimm.style.display = 'none';
        if (typeof window.applyPagePermissionToPane === 'function') {
          window.applyPagePermissionToPane(pane, '/comp/compDetail');
        }
        if (typeof window.applyCompDetailReadOnlyIfOwnNonMerchant === 'function') {
          window.applyCompDetailReadOnlyIfOwnNonMerchant(pane);
        }
      });
      }
      var compDetailSaveBtn = pane.querySelector('#compDetailSaveBtn');
      if (compDetailSaveBtn) {
        compDetailSaveBtn.addEventListener('click', function () {
          if (getPagePermissionForUrl('/comp/compDetail') === 'OBSERVER') return;
          if (pane.classList && pane.classList.contains('pg-comp-detail-self-readonly')) return;
          var form = pane.querySelector('#compDetailForm');
          if (!form) return;
          initIntlPhoneFields(form);
          ['ceoMobile', 'compTel', 'fax', 'settleTelNo', 'contactTel'].forEach(function (n) { syncIntlPhoneHidden(form, n); });
          var compIdEl = form.querySelector('[name="compId"]');
          var compId = compIdEl && compIdEl.value ? compIdEl.value.trim() : '';
          if (!compId) { alert('업체코드가 없습니다.'); return; }
          var fd = {};
          form.querySelectorAll('input, select, textarea').forEach(function (el) {
            if (el.name && el.type !== 'file' && el.name !== 'pgOperational') {
              if (el.name.indexOf('__phone_') === 0) return;
              fd[el.name] = el.value;
            }
          });
          if (fd.countryCd === 'OTHER') { fd.bankCd = fd.bankCdText || fd.bankCd; delete fd.bankCdText; }
          if (fd.addrCountryCd === 'OTHER') { fd.addrCountryCd = fd.addrCountryCdOther || ''; delete fd.addrCountryCdOther; }
          fd.compId = compId;
          
          if (fd.regType != null) { fd.regNo = (fd.regType || 'CORP') + '|' + (fd.regNo || ''); delete fd.regType; }
          var tbodyPg = form.querySelector('#pgBindingTbody');
          if (tbodyPg) {
            var operationalVal = form.querySelector('input[name="pgOperational"]:checked');
            var operationalIdx = operationalVal ? parseInt(operationalVal.value, 10) : 0;
            var bindings = [];
            tbodyPg.querySelectorAll('tr').forEach(function (tr, i) {
              var sel = function (f) { var e = tr.querySelector('[data-field="' + f + '"]'); return e ? e.value : ''; };
              var pgCd = sel('pgCd');
              if (pgCd) {
                bindings.push({
                  pgCd: pgCd,
                  activationYn: sel('activationYn') || 'Y',
                  operationalYn: i === operationalIdx ? 'Y' : 'N',
                  payMethod: sel('payMethod') || 'WEB',
                  mid: sel('mid'),
                  rootNo: sel('rootNo'),
                  apiKey: sel('apiKey'),
                  ivKey: sel('ivKey'),
                  installmentYn: sel('installmentYn') || 'N',
                  maxInstallmentMonths: sel('maxInstallmentMonths')
                });
              }
            });
            fd.pgBindings = JSON.stringify(bindings);
          }
          var compDivVal = form.querySelector('[name="compDiv"]') ? form.querySelector('[name="compDiv"]').value : '';
          if (compDivVal === 'MASTER_DIST') {
            var n1elD = form.querySelector('[name="notifyUrl1"]');
            var mdMandatoryLockedD = n1elD && n1elD.disabled;
            var dn1 = String(fd.notifyUrl1 || '').trim();
            var dn2 = String(fd.notifyUrl2 || '').trim();
            var dHasBackup = !!(String(fd.notifyUrl3 || '').trim() || String(fd.notifyUrl4 || '').trim());
            if (!mdMandatoryLockedD) {
              if (!dn1) { alert('총판은 노티 CALLBACK(URL 1)을 입력해야 합니다.'); return; }
              if (!dn2) { alert('총판은 노티 RESULT(URL 2)를 입력해야 합니다.'); return; }
            }
            if (dHasBackup && (!dn1 || !dn2)) {
              alert(mdMandatoryLockedD
                ? '보조 노티(URL 3·4)를 쓰려면 본사 노티구성설정에서 이 총판에 필수 노티(URL 1·2)가 연결되어 있어야 합니다.'
                : '노티 URL 3·4(보조)를 쓰려면 URL 1·2(CALLBACK·RESULT)가 모두 필요합니다.');
              return;
            }
          }
          if (compDivVal === 'HEADQUARTERS' || compDivVal === 'REGIONAL' || compDivVal === 'MASTER_DIST') {
            if (compDivVal === 'REGIONAL') {
              var bc = [fd.baseCurrency1, fd.baseCurrency2, fd.baseCurrency3].filter(function (v) { return v && v.trim(); });
              fd.baseCurrency = bc.join(',');
            }
            delete fd.baseCurrency1;
            delete fd.baseCurrency2;
            delete fd.baseCurrency3;
            var cardLimitTbody = form.querySelector('#regionalCardLimitTbody');
            if (cardLimitTbody) {
              var cardLimits = [];
              cardLimitTbody.querySelectorAll('tr').forEach(function (tr) {
                var sel = function (f) { var e = tr.querySelector('[data-field="' + f + '"]'); return e ? e.value : ''; };
                cardLimits.push({ payMethod: sel('payMethod'), cardIssuer: sel('cardIssuer'), dayLimit: sel('dayLimit'), timesLimit: sel('timesLimit'), amtLimit: sel('amtLimit'), regReason: sel('regReason'), regDt: sel('regDt'), modDt: sel('modDt'), remark: sel('remark') });
              });
              fd.regionalCardLimits = JSON.stringify(cardLimits);
            }
            var terminalTbody = form.querySelector('#regionalTerminalTbody');
            if (terminalTbody) {
              var terminals = [];
              terminalTbody.querySelectorAll('tr').forEach(function (tr) {
                var sel = function (f) { var e = tr.querySelector('[data-field="' + f + '"]'); return e ? e.value : ''; };
                terminals.push({ pgAgency: sel('pgAgency'), terminalId: sel('terminalId'), remark: sel('remark') });
            });
              fd.regionalTerminals = JSON.stringify(terminals);
            }
            if (fd.holidayCountryCode) fd.holidayCountryCodes = fd.holidayCountryCode;
            if (fd.businessHolidayRangesJson) {
              try {
                var _rows2 = JSON.parse(fd.businessHolidayRangesJson || '[]');
                fd.businessHolidayExtraDates = (_rows2 || []).map(function (r) { return r.fromDate || ''; }).filter(function (v) { return !!v; }).join('\n');
              } catch (e) {}
            }
            var regionalKeys = ['copyright'];
            if (compDivVal === 'REGIONAL' || compDivVal === 'MASTER_DIST') {
              regionalKeys = ['remitterName', 'balanceNotifyAmt', 'suspiciousNotifyAmt', 'overseasLoginNotifyAmt', 'tempPwdNotifyAmt', 'nonTranCriterionMonth', 'sameCardLimitWebDay', 'sameCardLimitWebTimes', 'sameCardLimitWebAmt', 'sameCardLimitTerminalDay', 'sameCardLimitTerminalTimes', 'sameCardLimitTerminalAmt', 'dailyUsageFee', 'depositNameLookup', 'transferAuthNo', 'autoConvertNewMemberLimit', 'newMemberDailyLimit', 'convertRefDate', 'convertDailyLimit', 'applyStartDate', 'pgFeeGeneral', 'transferFee', 'settleDiffMonthCnt', 'settleReportBankCd', 'pgFeeSamsung', 'smsFee', 'taxInvoiceEmail', 'settleAccountNo', 'directFee', 'solutionFee', 'settleAccountHolder', 'withdrawRestrictType', 'withdrawRestrictStartTime', 'withdrawRestrictEndTime', 'terminalPayRestrict', 'webPayRestrict', 'defaultFeeHq', 'defaultFeeDist', 'defaultFeeBranch', 'defaultFeeAgency', 'defaultFeeSalesOffice', 'defaultPayLimitPerTx', 'defaultPayLimitDay', 'defaultPayLimitMonth', 'defaultPayLimitYearCorp', 'defaultPayLimitYearInd', 'copyright', 'regionalCardLimits', 'regionalTerminals', 'holidayProfileName', 'holidayProfileCountry', 'holidayCountryCode', 'holidayCountryCodes', 'businessHolidayRangesJson', 'businessHolidayExtraDates'];
            }
            var regionalSettings = {};
            regionalKeys.forEach(function (k) { if (fd[k] !== undefined && fd[k] !== null && fd[k] !== '') regionalSettings[k] = fd[k]; });
            fd.regionalSettings = JSON.stringify(regionalSettings);
          }
          if (!pgConfirmBeforeSave('저장하시겠습니까?')) return;
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          var mainFile = form.querySelector('#brandingMainImageFile');
          var firstFile = form.querySelector('#brandingFirstLogoImageFile');
          var logoFile = form.querySelector('#brandingLogoImageFile');
          var popconFile = form.querySelector('#brandingPopconImageFile');
          var themeEl = form.querySelector('#brandingTheme');
          var hostEl = form.querySelector('#brandingBrandHost');
          var siteNameEl = form.querySelector('#brandingSiteName');
          var brandingCard = form.closest('.tab-pane') && form.closest('.tab-pane').querySelector('#brandingCard');
          var isRegOrMaster = brandingCard && !brandingCard.classList.contains('d-none');
          window.PG_API.compUpdate(fd).then(function () {
            var settleFd = {};
            var settleKeys = ['withdrawRestrictType', 'withdrawStartTime', 'withdrawEndTime', 'payLimitDefault', 'payLimitExtra', 'holdRate', 'holdDays', 'calcCloseTime', 'calcStartTime', 'transferCycleDays', 'calcProcType', 'transferType', 'autoTransferMin', 'payHoldYn', 'calcExcludeYn', 'calcExcludeTarget', 'calcMinAmt', 'transferExecTime', 'feeVatApplyYn', 'feeVatRatePct'];
            if (compDivVal === 'MERCHANT') settleKeys.splice(5, 0, 'calcCycle');
            settleKeys.forEach(function (k) {
              if (fd[k] !== undefined && fd[k] !== null && fd[k] !== '') settleFd[k] = fd[k];
            });
            if (Object.keys(settleFd).length > 0) {
              return window.PG_API.settlementSettingSave(compId, settleFd);
            }
            return Promise.resolve();
          }).then(function () {
            if (isRegOrMaster && window.PG_API.orgBrandingUpload && window.PG_API.orgBrandingSave) {
              var chain = Promise.resolve();
              if (mainFile && mainFile.files && mainFile.files[0]) {
                var _detMainF = mainFile.files[0];
                chain = chain.then(function () {
                  return window.PG_API.orgBrandingUpload(compId, 'main', _detMainF).then(function (data) {
                    pgBrandingSetImageDisplayInput(form, 'main', data, _detMainF);
                    return data;
                  });
                });
              }
              if (logoFile && logoFile.files && logoFile.files[0]) {
                var _detLogoF = logoFile.files[0];
                chain = chain.then(function () {
                  return window.PG_API.orgBrandingUpload(compId, 'logo', _detLogoF).then(function (data) {
                    pgBrandingSetImageDisplayInput(form, 'logo', data, _detLogoF);
                    try {
                      if (window.PG_applySidebarLogo && data && data.url) {
                        var _apiBase3 = (typeof window.PG_assetApiBase === 'function')
                          ? window.PG_assetApiBase()
                          : ((window.PG_API_BASE || '').replace(/\/$/, '') || window.location.origin);
                        var _src3 = /^https?:\/\//i.test(String(data.url)) ? String(data.url) : (_apiBase3 + String(data.url));
                        window.PG_applySidebarLogo(_src3 + (_src3.indexOf('?') >= 0 ? '&' : '?') + 'v=' + Date.now());
                      }
                    } catch (eLogo2) {}
                    return data;
                  });
                });
              }
              if (firstFile && firstFile.files && firstFile.files[0]) {
                var _detFirstF = firstFile.files[0];
                chain = chain.then(function () {
                  return window.PG_API.orgBrandingUpload(compId, 'first', _detFirstF).then(function (data) {
                    pgBrandingSetImageDisplayInput(form, 'first', data, _detFirstF);
                    return data;
                  });
                });
              }
              if (popconFile && popconFile.files && popconFile.files[0]) {
                var _detPopconF = popconFile.files[0];
                chain = chain.then(function () {
                  return window.PG_API.orgBrandingUpload(compId, 'popcon', _detPopconF).then(function (data) {
                    pgBrandingSetImageDisplayInput(form, 'popcon', data, _detPopconF);
                    return data;
                  });
                });
              }
              if (themeEl || hostEl) {
                chain = chain.then(function () { return window.PG_API.orgBrandingSave(compId, (themeEl && themeEl.value) ? themeEl.value : 'DEFAULT', hostEl ? hostEl.value : undefined, siteNameEl ? siteNameEl.value : undefined); });
              }
              return chain.catch(function (eBrand2) {
                try { console.warn('branding save failed:', eBrand2); } catch (e0) {}
                return Promise.reject(eBrand2);
              });
            }
            return Promise.resolve();
          }).then(function () {
            pgApplyFooterCopyright(pane, fd);
            alert('저장되었습니다.');
          }).catch(function (e) { alert(e && e.message ? e.message : '수정 저장 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
      }
      pane.addEventListener('click', function (e) {
        var listBtn = e.target && e.target.closest ? e.target.closest('#compDetailListBtn') : null;
        var idChangeBtn = e.target && e.target.closest ? e.target.closest('[data-action="ID변경"]') : null;
        var pwdResetBtn = e.target && e.target.closest ? e.target.closest('#compDetailPwdResetBtn, [data-action="비밀번호 초기화"]') : null;
        var form = pane.querySelector('#compDetailForm');
        var compIdEl = form && form.querySelector('[name="compId"]');
        var compId = compIdEl && compIdEl.value ? compIdEl.value.trim() : '';
        if (listBtn) {
          if (compId) { try { sessionStorage.setItem('pg_comp_detail_return_compId', compId); } catch (e) {} }
          fnTopMenuMove('/comp/compMngTree', null, '업체관리');
          return;
        }
        if (idChangeBtn && compId) {
          var loginIdEl = form && form.querySelector('[name="loginId"]');
          var currentId = loginIdEl ? String(loginIdEl.value || '') : '';
          var hid = document.getElementById('pgCompLoginIdChangeCompId');
          var lab = document.getElementById('pgCompLoginIdChangeCompIdLabel');
          var inp = document.getElementById('pgCompLoginIdChangeNewId');
          var modal = document.getElementById('pgCompLoginIdChangeModal');
          if (hid && lab && inp && modal && window.PG_UI && window.PG_UI.openModal) {
            window._pgCompLoginIdPending = window._pgCompLoginIdPending || {};
            window._pgCompLoginIdPending.loginIdEl = loginIdEl;
            hid.value = compId;
            lab.textContent = compId;
            inp.value = currentId;
            window.PG_UI.openModal(modal);
            setTimeout(function () { try { inp.focus(); inp.select(); } catch (e1) {} }, 400);
          } else {
            var newIdLegacy = window.prompt('새 로그인 ID를 입력하세요.', currentId);
            if (newIdLegacy != null && newIdLegacy.trim()) {
              var dimmL = document.getElementById('dimm');
              if (dimmL) dimmL.style.display = 'flex';
              window.PG_API.compChangeLoginId(compId, newIdLegacy.trim()).then(function () {
                alert('로그인 ID가 변경되었습니다.');
                if (loginIdEl) loginIdEl.value = newIdLegacy.trim();
              }).catch(function (err) { alert(err && err.message ? err.message : 'ID 변경 실패'); }).finally(function () { if (dimmL) dimmL.style.display = 'none'; });
            }
          }
          return;
        }
        if (pwdResetBtn && compId) {
          if (!confirm('해당 업체의 비밀번호를 초기화하시겠습니까?')) return;
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.compResetPassword(compId).then(function (r) {
            var pwd = (r && r.data && r.data.tempPassword) ? r.data.tempPassword : (r && r.tempPassword) ? r.tempPassword : '';
            alert(pwd ? '비밀번호가 초기화되었습니다. 임시비밀번호: ' + pwd : '비밀번호가 초기화되었습니다.');
          }).catch(function (err) { alert(err && err.message ? err.message : '비밀번호 초기화 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
          return;
        }
      });
    }
    if (url === '/hq/defaultCommission') {
      var dimm = document.getElementById('dimm');
      function hqDefFlash(variant, msg) {
        if (window.PG_UI && window.PG_UI.showBanner) {
          window.PG_UI.showBanner(pane, 'hqDefaultCommissionFlash', variant, msg, 6500);
        }
      }
      function syncHqDefCommTemplateScopeDisplay() {
        var hid = pane.querySelector('#hqDefCommTemplateScope');
        var disp = pane.querySelector('#hqDefCommTemplateScopeDisplay');
        if (!hid || !disp) return;
        var v = hid.value || '';
        var i;
        for (i = 0; i < disp.options.length; i++) {
          if (disp.options[i].value === v) {
            disp.selectedIndex = i;
            return;
          }
        }
        disp.selectedIndex = 0;
      }
      function renderTemplateSelect(data) {
        var hid = pane.querySelector('#hqDefCommTemplateScope');
        var disp = pane.querySelector('#hqDefCommTemplateScopeDisplay');
        if (!hid || !disp) return;
        var prev = hid.value || '';
        var templates = (data && data.templates) ? data.templates : [];
        var h = '<option value="">(신규) 저장 시 자동 부여</option>';
        templates.forEach(function (t) {
          var s = t.scope || '';
          var nm = t.policyName || s.replace('HQPOL:', '');
          h += '<option value="' + String(s).replace(/&/g, '&amp;').replace(/"/g, '&quot;') + '">' + String(nm).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;') + '</option>';
        });
        disp.innerHTML = h;
        function applyScope(v) {
          hid.value = v != null ? String(v) : '';
          syncHqDefCommTemplateScopeDisplay();
        }
        function optSet(v) {
          if (v === '' || v == null) {
            applyScope('');
            return true;
          }
          for (var oi = 0; oi < disp.options.length; oi++) {
            if (disp.options[oi].value === v) {
              applyScope(v);
              return true;
            }
          }
          return false;
        }
        if (!optSet(prev)) {
          if (data && data.deployedTemplateScope) optSet(data.deployedTemplateScope);
          else if (templates.length > 0) optSet(templates[0].scope || '');
          else optSet('');
        }
      }
      function hqDefCommResetFormForNew() {
        pane._hqDefCommIsNew = true;
        var defs = {
          rollingPct: '5', rollingDays: '180', policyName: '', deployYn: 'N', currencyCode: 'KRW', policyRemark: ''
        };
        Object.keys(defs).forEach(function (k) {
          var el = pane.querySelector('[name="' + k + '"]');
          if (el) el.value = defs[k];
        });
        pane.querySelectorAll('.hq-tier-cell').forEach(function (inp) { inp.value = ''; });
        var mdef = {
          payRate: '2.5', perTxFee: '0', failFee: '0', cancelRate: '0', voidFeePerTx: '0', manualVoidFeePerTx: '0', refundRate: '0',
          feeSettlementPerTx: '0', feeUsdt: '0', feeFx: '0', usageRate: '0', fee3dsRate: '0', chargebackFeePerTx: '0'
        };
        Object.keys(mdef).forEach(function (fk) {
          hqTierSumLevels.forEach(function (lv) {
            var el = pane.querySelector('.hq-tier-cell[data-fee="' + fk + '"][data-level="' + lv + '"]');
            if (el) el.value = '';
          });
          var hqC = pane.querySelector('.hq-tier-cell[data-fee="' + fk + '"][data-level="hq"]');
          if (hqC) hqC.value = mdef[fk];
        });
        hqRecalcMerchantAll(pane);
        var ei;
        for (ei = 1; ei <= 4; ei++) {
          var nm = pane.querySelector('[name="extraFee' + ei + 'Name"]');
          var md = pane.querySelector('[name="extraFee' + ei + 'Mode"]');
          if (nm) nm.value = '';
          if (md) md.value = '';
          pane.querySelectorAll('.hq-tier-extra-cell[data-slot="' + ei + '"]').forEach(function (inp) { inp.value = ''; });
        }
        var cb = pane.querySelector('[name="chargebackPolicyId"]');
        if (cb) cb.value = '';
        var hid = pane.querySelector('#hqDefCommTemplateScope');
        if (hid) hid.value = '';
        syncHqDefCommTemplateScopeDisplay();
        renderChargebackPolicySelect(pane._hqDefCommLastData);
        renderPolicyTemplateTable(pane._hqDefCommLastData);
        hqDefFlash('info', '신규 정책 입력 모드입니다. 내용을 입력한 뒤 [저장]하면 코드가 자동 부여되고 목록에 반영됩니다.');
      }
      function renderPolicyTemplateTable(data) {
        var tb = pane.querySelector('#hqDefaultCommissionPolicyList');
        var emptyEl = pane.querySelector('#hqDefaultCommissionPolicyListEmpty');
        if (!tb) return;
        function escH(s) {
          return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
        }
        function nzStr(obj, key, def) {
          var v = obj[key];
          if (v == null || v === '') return def != null ? def : '0';
          return String(v);
        }
        function escAttr(s) {
          return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        }
        function isBlankValue(v) {
          return v == null || String(v).trim() === '';
        }
        function getExtraMerchantValue(t, idx) {
          var tc = t && t.tierCommission ? t.tierCommission : null;
          var extras = tc && Array.isArray(tc.extras) ? tc.extras : null;
          var slot = extras && extras[idx - 1] ? extras[idx - 1] : null;
          var mv = slot && slot.tiers ? slot.tiers.merchant : null;
          if (mv != null && String(mv).trim() !== '') return String(mv);
          return nzStr(t, 'extraFee' + idx + 'Value', '');
        }
        function formatExtraSlot(t, idx, cc) {
          var emd = (t['extraFee' + idx + 'Mode'] != null ? String(t['extraFee' + idx + 'Mode']) : '').trim().toUpperCase();
          var enm = nzStr(t, 'extraFee' + idx + 'Name', '');
          var evv = getExtraMerchantValue(t, idx);
          var bs = idx === 1 ? ' border-start' : '';
          if (isBlankValue(evv)) {
            return '<td class="hq-def-comm-policy-td-extra-slot small text-center' + bs + '"></td>';
          }
          if (!enm) enm = '이름';
          var evFmt = pgFmtPolicyListAmount(evv, cc);
          var unit = emd === 'PCT' ? '(%)' : '(건)';
          var raw = enm + unit + ' ' + evFmt;
          var disp = escH(enm) + unit + ' ' + escH(evFmt);
          return '<td class="hq-def-comm-policy-td-extra-slot small text-truncate text-center' + bs + '" title="' + escAttr(raw) + '">' + disp + '</td>';
        }
        function formatExtraHeader(name, mode, fallback) {
          var nm = name != null ? String(name).trim() : '';
          if (!nm) return fallback;
          var md = mode != null ? String(mode).trim().toUpperCase() : '';
          return nm + (md === 'PCT' ? '(%)' : '(건)');
        }
        var templates = (data && data.templates) ? data.templates : [];
        var curSel = (pane.querySelector('[name="templateScope"]') || {}).value || '';
        var selected = null;
        for (var ti = 0; ti < templates.length; ti++) {
          if (String(templates[ti].scope || '') === String(curSel || '')) {
            selected = templates[ti];
            break;
          }
        }
        if (!selected && templates.length) selected = templates[0];
        var h1 = pane.querySelector('#hqDefCommExtraHead1');
        var h2 = pane.querySelector('#hqDefCommExtraHead2');
        var h3 = pane.querySelector('#hqDefCommExtraHead3');
        var h4 = pane.querySelector('#hqDefCommExtraHead4');
        if (h1) h1.textContent = formatExtraHeader(selected && selected.extraFee1Name, selected && selected.extraFee1Mode, '기타1');
        if (h2) h2.textContent = formatExtraHeader(selected && selected.extraFee2Name, selected && selected.extraFee2Mode, '기타2');
        if (h3) h3.textContent = formatExtraHeader(selected && selected.extraFee3Name, selected && selected.extraFee3Mode, '기타3');
        if (h4) h4.textContent = formatExtraHeader(selected && selected.extraFee4Name, selected && selected.extraFee4Mode, '기타4');
        var h = '';
        templates.forEach(function (t) {
          var scope = t.scope || '';
          var shortCode = scope.indexOf('HQPOL:') === 0 ? scope.substring(6) : scope;
          var rawName = String(t.policyName || shortCode || '');
          var nm = rawName.replace(/&/g, '&amp;').replace(/</g, '&lt;');
          var dep = (t.deployYn === 'Y')
            ? '<span class="badge bg-success">배포</span>'
            : '<span class="badge bg-light text-dark border">미배포</span>';
          var cur = t.currencyCode != null && String(t.currencyCode).trim() !== '' ? String(t.currencyCode).trim().toUpperCase() : '';
          var ccForFmt = (t.currencyCode != null && String(t.currencyCode).trim() !== '') ? String(t.currencyCode).trim().toUpperCase() : 'KRW';
          var ua = t.updatedAt ? String(t.updatedAt).replace('T', ' ').replace(/\.\d+Z?$/, '') : '';
          var active = (scope === curSel) ? ' table-active' : '';
          var esc = String(scope).replace(/&/g, '&amp;').replace(/"/g, '&quot;');
          var cbPolRaw = (t.chargebackPolicyName != null && String(t.chargebackPolicyName).trim() !== '') ? String(t.chargebackPolicyName).trim() : '';
          var cbFeeRaw = nzStr(t, 'chargebackFeePerTx', '');
          var cbFeeAmt = isBlankValue(cbFeeRaw) ? '' : pgFmtPolicyListAmount(cbFeeRaw, ccForFmt);
          var cbTd = '<td class="hq-def-comm-policy-td-num">' + escH(cbFeeAmt) + '</td>';
          function tdAmt(raw, cls) {
            var rv = raw == null ? '' : String(raw).trim();
            var out = rv === '' ? '' : pgFmtPolicyListAmount(rv, ccForFmt);
            return '<td class="hq-def-comm-policy-td-num' + (cls ? ' ' + cls : '') + '">' + escH(out) + '</td>';
          }
          function tdPctRow(raw, cls) {
            var rv = raw == null ? '' : String(raw).trim();
            var out = rv === '' ? '' : pgFmtPolicyListAmount(rv, ccForFmt);
            return '<td class="hq-def-comm-policy-td-num' + (cls ? ' ' + cls : '') + '">' + escH(out) + '</td>';
          }
          function tdDays(raw, cls) {
            var rv = raw == null ? '' : String(raw).trim();
            if (rv === '') return '<td class="hq-def-comm-policy-td-num' + (cls ? ' ' + cls : '') + '"></td>';
            var nd = parseFloat(rv.replace(/,/g, '.'));
            if (!isFinite(nd)) return '<td class="hq-def-comm-policy-td-num' + (cls ? ' ' + cls : '') + '"></td>';
            return '<td class="hq-def-comm-policy-td-num' + (cls ? ' ' + cls : '') + '">' + escH(String(Math.round(nd))) + '</td>';
          }
          var cbZoneTd = cbPolRaw
            ? '<td class="hq-def-comm-policy-td-cbzone small text-truncate" title="' + escAttr(cbPolRaw) + '">' + escH(cbPolRaw) + '</td>'
            : '<td class="hq-def-comm-policy-td-cbzone small"></td>';
          h += '<tr class="hq-default-comm-policy-row' + active + '" data-scope="' + esc + '" style="cursor:pointer" title="클릭하여 이 정책 불러오기">';
          h += '<td class="text-center align-middle hq-def-comm-chk-cell"><input type="checkbox" class="form-check-input hq-def-comm-row-chk m-0 align-middle" data-scope="' + esc + '" aria-label="행 선택"></td>';
          h += '<td class="font-monospace hq-def-comm-policy-td-code text-nowrap">' + String(shortCode).replace(/&/g, '&amp;').replace(/</g, '&lt;') + '</td><td class="hq-def-comm-policy-td-name small align-middle" title="' + escAttr(rawName) + '">' + nm + '</td>' + cbZoneTd + '<td class="hq-def-comm-policy-td-deploy text-center align-middle">' + dep + '</td>';
          h += '<td class="font-monospace small hq-def-comm-policy-td-cur text-center align-middle">' + escH(cur) + '</td>';
          h += tdAmt(nzStr(t, 'perTxFee', ''), 'border-start');
          h += tdAmt(nzStr(t, 'failFee', ''));
          h += tdAmt(nzStr(t, 'feeSettlementPerTx', ''));
          h += tdAmt(nzStr(t, 'remittanceTransferFee', ''));
          h += tdAmt(nzStr(t, 'usdtTransferFeeUsd', ''));
          h += cbTd;
          h += tdAmt(nzStr(t, 'cancelRate', ''));
          h += tdAmt(nzStr(t, 'voidFeePerTx', ''));
          h += tdAmt(nzStr(t, 'manualVoidFeePerTx', ''));
          h += tdAmt(nzStr(t, 'refundRate', ''));
          h += tdAmt(nzStr(t, 'fee3dsRate', ''));
          h += tdPctRow(nzStr(t, 'payRate', ''), 'border-start');
          h += tdPctRow(nzStr(t, 'feeUsdt', ''));
          h += tdPctRow(nzStr(t, 'feeFx', ''));
          h += tdPctRow(nzStr(t, 'rollingPct', ''), 'border-start');
          h += tdDays(nzStr(t, 'rollingDays', ''));
          h += tdAmt(nzStr(t, 'usageRate', ''), 'border-start');
          h += formatExtraSlot(t, 1, ccForFmt);
          h += formatExtraSlot(t, 2, ccForFmt);
          h += formatExtraSlot(t, 3, ccForFmt);
          h += formatExtraSlot(t, 4, ccForFmt);
          h += '<td class="text-nowrap small hq-def-comm-policy-td-upd" title="' + escAttr(ua) + '">' + escH(ua) + '</td></tr>';
        });
        tb.innerHTML = h;
        var selAll = pane.querySelector('#hqDefCommSelectAll');
        if (selAll) selAll.checked = false;
        if (emptyEl) {
          if (templates.length === 0) emptyEl.classList.remove('d-none');
          else emptyEl.classList.add('d-none');
        }
      }
      function hqDefCommGetCheckedScopes() {
        var out = [];
        var seen = {};
        pane.querySelectorAll('#hqDefaultCommissionPolicyList .hq-def-comm-row-chk:checked').forEach(function (cb) {
          var s = cb.getAttribute('data-scope');
          if (s && !seen[s]) {
            seen[s] = true;
            out.push(s);
          }
        });
        return out;
      }
      function hqDefCommLoadScopeIntoForm(scope) {
        if (!scope) return;
        pane._hqDefCommIsNew = false;
        var hid = pane.querySelector('#hqDefCommTemplateScope');
        if (hid) hid.value = scope;
        syncHqDefCommTemplateScopeDisplay();
        fillDefaultCommissionForm(currentTemplateData(pane._hqDefCommLastData));
        renderPolicyTemplateTable(pane._hqDefCommLastData);
        hqDefFlash('info', '「' + scope.replace(/^HQPOL:/, '') + '」정책을 불러왔습니다. 수정 후 [저장]하세요.');
      }
      function renderChargebackPolicySelect(fullData) {
        var sel = pane.querySelector('[name="chargebackPolicyId"]');
        if (!sel) return;
        var opts = (fullData && fullData.chargebackPolicyOptions) ? fullData.chargebackPolicyOptions : [];
        var tmpl = currentTemplateData(fullData || {});
        var cur = (tmpl && tmpl.chargebackPolicyId != null && String(tmpl.chargebackPolicyId) !== '') ? String(tmpl.chargebackPolicyId) : '';
        function escA(s) {
          return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
        }
        var html = '<option value="">(미사용) 건당 차지백만</option>';
        opts.forEach(function (o) {
          if (!o || o.id == null) return;
          var cc = o.currencyCode != null && String(o.currencyCode).trim() !== '' ? String(o.currencyCode).trim().toUpperCase() : '';
          var nm = o.name != null ? o.name : ('#' + o.id);
          var lab = nm + (cc ? ' (' + cc + ')' : '');
          html += '<option value="' + escA(String(o.id)) + '">' + escA(lab) + '</option>';
        });
        sel.innerHTML = html;
        if (cur) sel.value = cur;
        else sel.value = '';
      }
      var hqTierSumLevels = ['hq', 'regional', 'master', 'branch', 'agency', 'salesOffice'];
      var hqPctFeeKeys = { payRate: 1, feeUsdt: 1, feeFx: 1 };
      function hqParseTierCellNumber(s) {
        if (s == null || String(s).trim() === '') return 0;
        var n = parseFloat(String(s).replace(/,/g, '.').trim());
        return isFinite(n) ? n : 0;
      }
      function hqRecalcMerchantAll(paneRef) {
        var feeKeys = ['payRate', 'perTxFee', 'failFee', 'cancelRate', 'voidFeePerTx', 'manualVoidFeePerTx', 'refundRate', 'feeSettlementPerTx', 'remittanceTransferFee', 'usdtTransferFeeUsd', 'fee3dsRate', 'feeUsdt', 'feeFx', 'usageRate', 'chargebackFeePerTx'];
        feeKeys.forEach(function (fk) {
          var sum = 0;
          hqTierSumLevels.forEach(function (lv) {
            var el = paneRef.querySelector('.hq-tier-cell[data-fee="' + fk + '"][data-level="' + lv + '"]');
            sum += hqParseTierCellNumber(el && el.value);
          });
          var mcel = paneRef.querySelector('.hq-tier-cell[data-fee="' + fk + '"][data-level="merchant"]');
          if (!mcel) return;
          if (hqPctFeeKeys[fk]) {
            mcel.value = pgFmtPctOneDecimalInput(String(sum));
          } else {
            mcel.value = pgFmtOneDecimalStripWhole(String(sum));
          }
        });
        var si;
        for (si = 1; si <= 4; si++) {
          var modeEl = paneRef.querySelector('[name="extraFee' + si + 'Mode"]');
          var mode = modeEl ? String(modeEl.value).trim().toUpperCase() : '';
          var pct = mode === 'PCT' || mode === '%';
          var sum = 0;
          hqTierSumLevels.forEach(function (lv) {
            var el = paneRef.querySelector('.hq-tier-extra-cell[data-slot="' + si + '"][data-level="' + lv + '"]');
            sum += hqParseTierCellNumber(el && el.value);
          });
          var mcel = paneRef.querySelector('.hq-tier-extra-cell[data-slot="' + si + '"][data-level="merchant"]');
          if (!mcel) continue;
          if (pct) {
            mcel.value = pgFmtPctOneDecimalInput(String(sum));
          } else {
            mcel.value = pgFmtOneDecimalStripWhole(String(sum));
          }
        }
      }
      function hqFillTierCommissionMatrix(paneRef, tmpl) {
        var levels = ['hq', 'regional', 'master', 'branch', 'agency', 'salesOffice', 'merchant'];
        var feeKeys = ['payRate', 'perTxFee', 'failFee', 'cancelRate', 'voidFeePerTx', 'manualVoidFeePerTx', 'refundRate', 'feeSettlementPerTx', 'remittanceTransferFee', 'usdtTransferFeeUsd', 'fee3dsRate', 'feeUsdt', 'feeFx', 'usageRate', 'chargebackFeePerTx'];
        function setTierCell(fk, lv, v) {
          var el = paneRef.querySelector('.hq-tier-cell[data-fee="' + fk + '"][data-level="' + lv + '"]');
          if (!el) return;
          el.value = v != null && String(v) !== '' ? String(v) : '';
        }
        var tc = tmpl.tierCommission;
        if (tc && tc.rows && typeof tc.rows === 'object') {
          feeKeys.forEach(function (fk) {
            var row = tc.rows[fk];
            if (!row || typeof row !== 'object') return;
            levels.forEach(function (lv) {
              setTierCell(fk, lv, row[lv]);
            });
          });
        } else {
          var pctRowKeys = { payRate: 1, feeUsdt: 1, feeFx: 1 };
          feeKeys.forEach(function (fk) {
            levels.forEach(function (lv) { setTierCell(fk, lv, ''); });
            if (tmpl[fk] == null || tmpl[fk] === '') return;
            var mv = pctRowKeys[fk] ? pgFmtPctOneDecimalInput(tmpl[fk]) : pgFmtOneDecimalStripWhole(tmpl[fk]);
            setTierCell(fk, 'hq', mv);
          });
        }
        var extras = (tc && tc.extras) ? tc.extras : [];
        var si;
        for (si = 1; si <= 4; si++) {
          var slot = extras[si - 1];
          var modeEl = paneRef.querySelector('[name="extraFee' + si + 'Mode"]');
          var nameEl = paneRef.querySelector('[name="extraFee' + si + 'Name"]');
          if (slot && slot.name) {
            if (nameEl) nameEl.value = String(slot.name);
            if (modeEl) modeEl.value = slot.mode ? String(slot.mode).toUpperCase() : '';
            levels.forEach(function (lv) {
              var ex = paneRef.querySelector('.hq-tier-extra-cell[data-slot="' + si + '"][data-level="' + lv + '"]');
              if (ex && slot.tiers) ex.value = slot.tiers[lv] != null ? String(slot.tiers[lv]) : '';
            });
          } else {
            if (nameEl) nameEl.value = tmpl['extraFee' + si + 'Name'] != null ? String(tmpl['extraFee' + si + 'Name']) : '';
            if (modeEl) modeEl.value = tmpl['extraFee' + si + 'Mode'] != null ? String(tmpl['extraFee' + si + 'Mode']) : '';
            paneRef.querySelectorAll('.hq-tier-extra-cell[data-slot="' + si + '"]').forEach(function (inp) { inp.value = ''; });
            var md = tmpl['extraFee' + si + 'Mode'];
            var mcel = paneRef.querySelector('.hq-tier-extra-cell[data-slot="' + si + '"][data-level="merchant"]');
            if (mcel && tmpl['extraFee' + si + 'Value'] != null && tmpl['extraFee' + si + 'Value'] !== '') {
              var hqEx = paneRef.querySelector('.hq-tier-extra-cell[data-slot="' + si + '"][data-level="hq"]');
              if (hqEx) {
                hqEx.value = md && String(md).toUpperCase() === 'PCT' ? pgFmtPctOneDecimalInput(tmpl['extraFee' + si + 'Value']) : pgFmtOneDecimalStripWhole(tmpl['extraFee' + si + 'Value']);
              }
            }
          }
        }
        hqRecalcMerchantAll(paneRef);
      }
      function hqCollectTierCommissionPayload(paneRef) {
        var levels = ['hq', 'regional', 'master', 'branch', 'agency', 'salesOffice', 'merchant'];
        var feeKeys = ['payRate', 'perTxFee', 'failFee', 'cancelRate', 'voidFeePerTx', 'manualVoidFeePerTx', 'refundRate', 'feeSettlementPerTx', 'remittanceTransferFee', 'usdtTransferFeeUsd', 'fee3dsRate', 'feeUsdt', 'feeFx', 'usageRate', 'chargebackFeePerTx'];
        var rows = {};
        feeKeys.forEach(function (fk) {
          rows[fk] = {};
          levels.forEach(function (lv) {
            var el = paneRef.querySelector('.hq-tier-cell[data-fee="' + fk + '"][data-level="' + lv + '"]');
            rows[fk][lv] = el && el.value != null ? String(el.value).trim() : '';
          });
        });
        var extras = [];
        var si;
        for (si = 1; si <= 4; si++) {
          var modeEl = paneRef.querySelector('[name="extraFee' + si + 'Mode"]');
          var nameEl = paneRef.querySelector('[name="extraFee' + si + 'Name"]');
          var mode = modeEl ? modeEl.value.trim() : '';
          var name = nameEl ? nameEl.value.trim() : '';
          var tiers = {};
          levels.forEach(function (lv) {
            var el = paneRef.querySelector('.hq-tier-extra-cell[data-slot="' + si + '"][data-level="' + lv + '"]');
            tiers[lv] = el && el.value != null ? String(el.value).trim() : '';
          });
          if (!name || !mode) {
            extras.push({ name: '', mode: '', tiers: { hq: '', regional: '', master: '', branch: '', agency: '', salesOffice: '', merchant: '' } });
          } else {
            extras.push({ name: name, mode: mode, tiers: tiers });
          }
        }
        return { rows: rows, extras: extras };
      }
      function fillDefaultCommissionForm(tmpl) {
        if (!(tmpl && pane.querySelector('.hq-tier-cell'))) return;
        ['policyName', 'deployYn', 'templateScope', 'deployedTemplateScope', 'currencyCode', 'policyRemark', 'rollingPct', 'rollingDays'].forEach(function (k) {
          var el = pane.querySelector('[name="' + k + '"]');
          if (!el || tmpl[k] == null) return;
          if (k === 'rollingDays') {
            var rd = parseFloat(String(tmpl[k]).replace(/,/g, '.'));
            el.value = isFinite(rd) ? String(Math.round(rd)) : String(tmpl[k]);
            return;
          }
          if (k === 'rollingPct') {
            el.value = pgFmtPctOneDecimalInput(tmpl[k]);
            return;
          }
          el.value = tmpl[k];
        });
        hqFillTierCommissionMatrix(pane, tmpl);
        var selCb = pane.querySelector('[name="chargebackPolicyId"]');
        if (selCb) {
          if (tmpl.chargebackPolicyId != null && String(tmpl.chargebackPolicyId) !== '') selCb.value = String(tmpl.chargebackPolicyId);
          else selCb.value = '';
        }
        var hidTs = pane.querySelector('#hqDefCommTemplateScope');
        if (hidTs && tmpl && tmpl.scope != null && String(tmpl.scope) !== '') {
          hidTs.value = tmpl.scope;
          syncHqDefCommTemplateScopeDisplay();
        }
      }
      function currentTemplateData(raw) {
        var scopeEl = pane.querySelector('[name="templateScope"]');
        var scope = scopeEl ? scopeEl.value : '';
        var templates = raw && raw.templates ? raw.templates : [];
        for (var i = 0; i < templates.length; i++) {
          if ((templates[i].scope || '') === scope) return templates[i];
        }
        return raw || {};
      }
      function applyLoadedData(data) {
        pane._hqDefCommLastData = data;
        renderTemplateSelect(data);
        renderChargebackPolicySelect(data);
        fillDefaultCommissionForm(currentTemplateData(data));
        renderPolicyTemplateTable(data);
      }
      function reloadHqDefaultCommission() {
        if (dimm) dimm.style.display = 'flex';
        return window.PG_API.hqDefaultCommission().then(function (data) {
          applyLoadedData(data);
        }).catch(function () {
          hqDefFlash('danger', '정책 목록을 불러오지 못했습니다.');
        }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      }
      /* pane은 탭 재진입 시 유지되고 innerHTML만 갈아끼워지므로, 행 클릭 위임만 1회 등록하고 버튼은 매번 새 DOM에 바인딩한다. */
      if (!pane._hqDefCommRowClickBound) {
        pane._hqDefCommRowClickBound = true;
        pane.addEventListener('click', function (ev) {
          if (ev.target && ev.target.closest && ev.target.closest('.hq-def-comm-chk-cell')) return;
          if (ev.target && ev.target.classList && ev.target.classList.contains('hq-def-comm-row-chk')) return;
          var tr = ev.target.closest && ev.target.closest('tr.hq-default-comm-policy-row');
          if (!tr || !pane.contains(tr)) return;
          var scope = tr.getAttribute('data-scope');
          if (!scope) return;
          hqDefCommLoadScopeIntoForm(scope);
        });
      }
      if (!pane._hqDefCommTierInputBound) {
        pane._hqDefCommTierInputBound = true;
        pane.addEventListener('input', function (ev) {
          var t = ev.target;
          if (!t || !t.classList) return;
          if (t.classList.contains('hq-tier-cell') && t.getAttribute('data-level') !== 'merchant') {
            hqRecalcMerchantAll(pane);
            return;
          }
          if (t.classList.contains('hq-tier-extra-cell') && t.getAttribute('data-level') !== 'merchant') {
            hqRecalcMerchantAll(pane);
            return;
          }
          if (t.name && /^extraFee[1-4]Mode$/.test(t.name)) {
            hqRecalcMerchantAll(pane);
          }
        });
      }
      var selAllEl = pane.querySelector('#hqDefCommSelectAll');
      if (selAllEl && !selAllEl._hqDefCommBound) {
        selAllEl._hqDefCommBound = true;
        selAllEl.addEventListener('change', function () {
          var on = selAllEl.checked;
          pane.querySelectorAll('#hqDefaultCommissionPolicyList .hq-def-comm-row-chk').forEach(function (cb) { cb.checked = on; });
        });
      }
      var hqDefEdit = pane.querySelector('#hqDefaultCommissionEditBtn');
      if (hqDefEdit && !hqDefEdit._hqDefCommBound) {
        hqDefEdit._hqDefCommBound = true;
        hqDefEdit.addEventListener('click', function () {
          var scopes = hqDefCommGetCheckedScopes();
          if (scopes.length === 0) {
            hqDefFlash('warning', '수정할 정책을 목록에서 한 건 체크하세요.');
            return;
          }
          if (scopes.length > 1) {
            hqDefFlash('warning', '[수정]은 한 번에 한 건만 선택할 수 있습니다.');
            return;
          }
          if (!window.confirm('선택한 정책을 폼에 불러와 수정할 수 있습니다. 진행할까요?')) return;
          if (!window.confirm('불러온 뒤 반영하려면 [저장]을 눌러야 합니다. 계속하시겠습니까?')) return;
          hqDefCommLoadScopeIntoForm(scopes[0]);
        });
      }
      function collectHqDefCommFd() {
        hqRecalcMerchantAll(pane);
        var fd = {};
        pane.querySelectorAll('input, select, textarea').forEach(function (el) {
          if (el.name && !el.disabled) fd[el.name] = el.value;
        });
        try {
          fd.tierCommission = JSON.stringify(hqCollectTierCommissionPayload(pane));
        } catch (e) {
          fd.tierCommission = '{"rows":{},"extras":[]}';
        }
        return fd;
      }
      function hqDefCommDoSave() {
        if (!window.confirm('입력한 정책 내용을 서버에 저장하시겠습니까?')) return;
        if (!window.confirm('저장 후 목록이 갱신됩니다. 정말 저장할까요?')) return;
        function finishSaveOk() {
          hqDefFlash('success', '저장되었습니다. 아래 목록이 갱신되었습니다.');
          return reloadHqDefaultCommission();
        }
        if (pane._hqDefCommIsNew) {
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.hqDefaultCommissionTemplateAdd({ templateCode: '' }).then(function (res) {
            var hid = pane.querySelector('#hqDefCommTemplateScope');
            if (hid && res && res.scope) hid.value = res.scope;
            pane._hqDefCommIsNew = false;
            syncHqDefCommTemplateScopeDisplay();
            return window.PG_API.hqDefaultCommissionSave(collectHqDefCommFd()).then(function () { return finishSaveOk(); });
          }).catch(function (e) {
            hqDefFlash('danger', (e && e.message) ? e.message : '저장 또는 정책 추가에 실패했습니다.');
          }).finally(function () { if (dimm) dimm.style.display = 'none'; });
          return;
        }
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.hqDefaultCommissionSave(collectHqDefCommFd()).then(function () { return finishSaveOk(); }).catch(function (e) {
          hqDefFlash('danger', (e && e.message) ? e.message : '저장 실패');
        }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      }
      var hqDefCommFormSave = pane.querySelector('#hqDefCommFormSaveBtn');
      if (hqDefCommFormSave && !hqDefCommFormSave._hqDefCommBound) {
        hqDefCommFormSave._hqDefCommBound = true;
        hqDefCommFormSave.addEventListener('click', function () { hqDefCommDoSave(); });
      }
      var hqDefCommNewBtn = pane.querySelector('#hqDefCommNewPolicyBtn');
      if (hqDefCommNewBtn && !hqDefCommNewBtn._hqDefCommBound) {
        hqDefCommNewBtn._hqDefCommBound = true;
        hqDefCommNewBtn.addEventListener('click', function () {
          if (!window.confirm('신규 정책 입력 모드로 전환합니다. 계속하시겠습니까?')) return;
          if (!window.confirm('폼이 초기값으로 바뀝니다. 진행할까요?')) return;
          hqDefCommResetFormForNew();
        });
      }
      var delTplBtn = pane.querySelector('#hqDefaultCommissionTemplateDeleteBtn');
      if (delTplBtn) {
        delTplBtn.addEventListener('click', function () {
          var scopes = hqDefCommGetCheckedScopes();
          if (scopes.length === 0) {
            hqDefFlash('warning', '삭제할 정책을 목록에서 체크하세요.');
            return;
          }
          if (!window.confirm(scopes.length + '건을 삭제 절차를 시작합니다. 삭제 확인 단계로 진행할까요?')) return;
          if (!window.confirm('삭제는 되돌리기 어렵습니다. 계속하시겠습니까?')) return;
          var lines = scopes.map(function (sc) {
            return '· 「' + sc.replace(/^HQPOL:/, '') + '」(' + sc + ')';
          });
          var body = pane.querySelector('#hqDefaultCommissionDeleteModalText');
          if (body) {
            body.innerHTML = '<span class="d-block mb-2">아래 ' + scopes.length + '건 템플릿을 삭제합니다. 배포 중이면 가맹점 기본 부여에 영향이 있을 수 있습니다.</span>' +
              '<span class="small text-break" style="white-space:pre-line">' + lines.join('\n') + '</span>';
          }
          pane._hqDefDeletePendingScopes = scopes.slice();
          var delModal = pane.querySelector('#hqDefaultCommissionDeleteModal');
          if (delModal && window.PG_UI && window.PG_UI.openModal) {
            window.PG_UI.openModal(delModal);
          } else if (window.confirm('모달을 열 수 없어 바로 삭제 확인을 진행합니다. 선택한 ' + scopes.length + '건을 삭제할까요?') &&
            window.confirm('삭제를 최종 확인합니다. 실행할까요?')) {
            if (dimm) dimm.style.display = 'flex';
            (function delNext(i) {
              if (i >= scopes.length) {
                hqDefFlash('success', '선택한 정책이 삭제되었습니다.');
                return reloadHqDefaultCommission();
              }
              return window.PG_API.hqDefaultCommissionTemplateDelete(scopes[i]).then(function () { return delNext(i + 1); });
            }(0)).catch(function (e) {
              hqDefFlash('danger', (e && e.message) ? e.message : '정책 삭제 실패');
            }).finally(function () { if (dimm) dimm.style.display = 'none'; });
          }
        });
      }
      var delConf = pane.querySelector('#hqDefaultCommissionDeleteConfirmBtn');
      if (delConf) {
        delConf.addEventListener('click', function () {
          var scopes = pane._hqDefDeletePendingScopes;
          if (!scopes || scopes.length === 0) return;
          if (!window.confirm('선택한 ' + scopes.length + '건 템플릿을 서버에서 영구 삭제합니다. 진행할까요?')) return;
          if (!window.confirm('삭제 후에는 복구할 수 없습니다. 정말 실행하시겠습니까?')) return;
          if (dimm) dimm.style.display = 'flex';
          (function delNext(i) {
            if (i >= scopes.length) {
              if (window.PG_UI && window.PG_UI.closeModal) {
                window.PG_UI.closeModal(pane.querySelector('#hqDefaultCommissionDeleteModal'));
              }
              pane._hqDefDeletePendingScopes = [];
              hqDefFlash('success', '선택한 정책이 삭제되었습니다. 목록을 갱신했습니다.');
              return reloadHqDefaultCommission();
            }
            return window.PG_API.hqDefaultCommissionTemplateDelete(scopes[i]).then(function () { return delNext(i + 1); });
          }(0)).catch(function (e) {
            hqDefFlash('danger', (e && e.message) ? e.message : '정책 삭제 실패');
          }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
      }
      reloadHqDefaultCommission();
    }
    if (url === '/hq/chargebackPolicy') {
      var dimmCb = document.getElementById('dimm');
      function hqCbFlash(variant, msg) {
        if (window.PG_UI && window.PG_UI.showBanner) {
          window.PG_UI.showBanner(pane, 'hqChargebackPolicyFlash', variant, msg, 6500);
        }
      }
      function hqCbDefaultTiers() {
        return [
          { sortOrder: 0, countMin: 0, countMax: 4, feePerCase: '4500' },
          { sortOrder: 1, countMin: 6, countMax: 9, feePerCase: '9000' },
          { sortOrder: 2, countMin: 10, countMax: 14, feePerCase: '12000' },
          { sortOrder: 3, countMin: 15, countMax: '', feePerCase: '15000' },
          { sortOrder: 4, countMin: 31, countMax: '', feePerCase: '19000' }
        ];
      }
      function escCbAttr(s) {
        return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
      }
      function hqCbRenderTierRow(row) {
        var sortOrder = row.sortOrder != null ? row.sortOrder : 0;
        var cmin = row.countMin != null ? row.countMin : 0;
        var cmax = row.countMax != null && row.countMax !== '' ? row.countMax : '';
        var fee = row.feePerCase != null ? row.feePerCase : '0';
        return '<tr class="hq-cb-tier-row">' +
          '<td><input type="number" class="form-control form-control-sm hq-cb-sort" value="' + escCbAttr(String(sortOrder)) + '" /></td>' +
          '<td><input type="number" class="form-control form-control-sm hq-cb-min" min="0" value="' + escCbAttr(String(cmin)) + '" /></td>' +
          '<td><input type="number" class="form-control form-control-sm hq-cb-max" min="0" placeholder="무제한" value="' + escCbAttr(String(cmax)) + '" /></td>' +
          '<td><input type="text" class="form-control form-control-sm hq-cb-fee" value="' + escCbAttr(String(fee)) + '" /></td>' +
          '<td><button type="button" class="btn btn-sm btn-outline-danger hq-cb-tier-del py-0" aria-label="행 삭제">×</button></td></tr>';
      }
      function hqCbCollectTiers() {
        var tiers = [];
        pane.querySelectorAll('#hqCbPolTierTbody tr.hq-cb-tier-row').forEach(function (tr) {
          var so = (tr.querySelector('.hq-cb-sort') || {}).value;
          var mn = (tr.querySelector('.hq-cb-min') || {}).value;
          var mx = (tr.querySelector('.hq-cb-max') || {}).value;
          var fee = (tr.querySelector('.hq-cb-fee') || {}).value;
          var countMax = null;
          if (mx !== '' && mx != null && String(mx).trim() !== '') {
            var n = parseInt(String(mx).trim(), 10);
            if (!isNaN(n)) countMax = n;
          }
          tiers.push({
            sortOrder: so !== '' && so != null ? parseInt(String(so).trim(), 10) || 0 : 0,
            countMin: mn !== '' && mn != null ? Math.max(0, parseInt(String(mn).trim(), 10) || 0) : 0,
            countMax: countMax,
            feePerCase: fee !== '' && fee != null ? String(fee).trim() : '0'
          });
        });
        return tiers;
      }
      function hqCbClearForm() {
        var idEl = pane.querySelector('#hqCbPolId');
        if (idEl) idEl.value = '';
        var nm = pane.querySelector('#hqCbPolName');
        if (nm) nm.value = '';
        var cur = pane.querySelector('#hqCbPolCurrencyCode');
        if (cur) cur.value = 'KRW';
        var rm = pane.querySelector('#hqCbPolRemark');
        if (rm) rm.value = '';
        var tb = pane.querySelector('#hqCbPolTierTbody');
        if (tb) {
          tb.innerHTML = '';
          hqCbDefaultTiers().forEach(function (row) {
            tb.insertAdjacentHTML('beforeend', hqCbRenderTierRow(row));
          });
        }
      }
      function hqCbLoadDetail(id) {
        if (dimmCb) dimmCb.style.display = 'flex';
        return window.PG_API.hqChargebackPolicyDetail(id).then(function (d) {
          var idEl = pane.querySelector('#hqCbPolId');
          if (idEl) idEl.value = String(d.id || '');
          var nm = pane.querySelector('#hqCbPolName');
          if (nm) nm.value = d.name || '';
          var cur = pane.querySelector('#hqCbPolCurrencyCode');
          if (cur) {
            var cc = (d.currencyCode != null && String(d.currencyCode).trim() !== '') ? String(d.currencyCode).trim().toUpperCase() : 'KRW';
            cur.value = cc;
            var has = false;
            for (var ci = 0; ci < cur.options.length; ci++) {
              if (cur.options[ci].value === cc) { has = true; break; }
            }
            if (!has) {
              var opt = document.createElement('option');
              opt.value = cc;
              opt.textContent = cc;
              cur.appendChild(opt);
              cur.value = cc;
            }
          }
          var rm = pane.querySelector('#hqCbPolRemark');
          if (rm) rm.value = d.remark || '';
          var tb = pane.querySelector('#hqCbPolTierTbody');
          if (tb) {
            tb.innerHTML = '';
            var tiers = (d.tiers && d.tiers.length) ? d.tiers : hqCbDefaultTiers();
            tiers.forEach(function (t) {
              tb.insertAdjacentHTML('beforeend', hqCbRenderTierRow({
                sortOrder: t.sortOrder,
                countMin: t.countMin,
                countMax: t.countMax != null ? t.countMax : '',
                feePerCase: t.feePerCase
              }));
            });
          }
        }).catch(function (e) {
          hqCbFlash('danger', (e && e.message) ? e.message : '불러오기 실패');
        }).finally(function () { if (dimmCb) dimmCb.style.display = 'none'; });
      }
      function hqCbRenderList(rows) {
        var tb = pane.querySelector('#hqChargebackPolicyListTbody');
        if (!tb) return;
        if (!rows || !rows.length) {
          tb.innerHTML = '<tr><td colspan="4" class="text-muted text-center small">등록된 정책이 없습니다.</td></tr>';
          return;
        }
        tb.innerHTML = '';
        rows.forEach(function (r) {
          var tr = document.createElement('tr');
          tr.style.cursor = 'pointer';
          tr.setAttribute('data-id', String(r.id));
          var cc = r.currencyCode != null && String(r.currencyCode).trim() !== '' ? String(r.currencyCode).trim().toUpperCase() : 'KRW';
          var rem = r.remark != null ? String(r.remark) : '';
          tr.innerHTML = '<td class="font-monospace small">' + escCbAttr(String(r.id)) + '</td><td>' + escCbAttr(String(r.name || '')) + '</td>' +
            '<td class="text-nowrap small">' + escCbAttr(cc) + '</td><td class="small hq-cb-list-remark">' +
            (rem ? escCbAttr(rem) : '—') + '</td>';
          tr.addEventListener('click', function () { hqCbLoadDetail(r.id); });
          tb.appendChild(tr);
        });
      }
      function hqCbReloadList() {
        if (dimmCb) dimmCb.style.display = 'flex';
        return window.PG_API.hqChargebackPolicyList().then(function (list) {
          hqCbRenderList(list);
        }).catch(function () {
          hqCbRenderList([]);
          hqCbFlash('danger', '목록을 불러오지 못했습니다.');
        }).finally(function () { if (dimmCb) dimmCb.style.display = 'none'; });
      }
      function hqCbRebind(el, evt, storageKey, fn) {
        if (!el) return;
        var prev = el[storageKey];
        if (prev) el.removeEventListener(evt, prev);
        el[storageKey] = fn;
        el.addEventListener(evt, fn);
      }
      if (pane._hqCbTierDelDelegate) {
        pane.removeEventListener('click', pane._hqCbTierDelDelegate);
      }
      pane._hqCbTierDelDelegate = function (ev) {
        var delBtn = ev.target.closest && ev.target.closest('.hq-cb-tier-del');
        if (delBtn && pane.contains(delBtn)) {
          var tr = delBtn.closest('tr');
          if (tr && tr.parentNode) tr.parentNode.removeChild(tr);
        }
      };
      pane.addEventListener('click', pane._hqCbTierDelDelegate);
      hqCbRebind(pane.querySelector('#hqCbPolAddTierBtn'), 'click', '_hqCbAddTier', function () {
        var tb = pane.querySelector('#hqCbPolTierTbody');
        if (tb) {
          var n = tb.querySelectorAll('tr.hq-cb-tier-row').length;
          tb.insertAdjacentHTML('beforeend', hqCbRenderTierRow({ sortOrder: n, countMin: 0, countMax: '', feePerCase: '0' }));
        }
      });
      hqCbRebind(pane.querySelector('#hqChargebackPolicyNewBtn'), 'click', '_hqCbNew', function () {
        hqCbClearForm();
        hqCbFlash('info', '새 유형을 입력한 뒤 [저장]하세요.');
      });
      hqCbRebind(pane.querySelector('#hqChargebackPolicySaveBtn'), 'click', '_hqCbSave', function () {
        var idEl = pane.querySelector('#hqCbPolId');
        var idRaw = idEl && idEl.value ? idEl.value.trim() : '';
        var curEl = pane.querySelector('#hqCbPolCurrencyCode');
        var body = {
          name: (pane.querySelector('#hqCbPolName') || {}).value || '',
          currencyCode: (curEl && curEl.value) ? String(curEl.value).trim().toUpperCase() : 'KRW',
          remark: (pane.querySelector('#hqCbPolRemark') || {}).value || '',
          tiers: hqCbCollectTiers()
        };
        if (idRaw !== '') body.id = idRaw;
        if (dimmCb) dimmCb.style.display = 'flex';
        window.PG_API.hqChargebackPolicySave(body).then(function (res) {
          hqCbFlash('success', '저장되었습니다.');
          if (res && res.id != null && idEl && !idRaw) idEl.value = String(res.id);
          return hqCbReloadList();
        }).catch(function (e) {
          hqCbFlash('danger', (e && e.message) ? e.message : '저장 실패');
        }).finally(function () { if (dimmCb) dimmCb.style.display = 'none'; });
      });
      hqCbRebind(pane.querySelector('#hqChargebackPolicyDeleteBtn'), 'click', '_hqCbDel', function () {
        var idEl = pane.querySelector('#hqCbPolId');
        var idRaw = idEl && idEl.value ? idEl.value.trim() : '';
        if (!idRaw) {
          hqCbFlash('warning', '삭제할 항목을 목록에서 선택하세요.');
          return;
        }
        if (!window.confirm('이 차지백 정책을 삭제하시겠습니까?')) return;
        if (dimmCb) dimmCb.style.display = 'flex';
        window.PG_API.hqChargebackPolicyDelete(idRaw).then(function () {
          hqCbFlash('success', '삭제되었습니다.');
          hqCbClearForm();
          return hqCbReloadList();
        }).catch(function (e) {
          hqCbFlash('danger', (e && e.message) ? e.message : '삭제 실패');
        }).finally(function () { if (dimmCb) dimmCb.style.display = 'none'; });
      });
      hqCbRebind(pane.querySelector('#hqChargebackPolicyReloadBtn'), 'click', '_hqCbRel', function () { hqCbReloadList(); });
      hqCbReloadList().then(function () { hqCbClearForm(); });
    }
    if (url === '/hq/notifyEnv') {
      var dimmN = document.getElementById('dimm');
      function escNt(s) {
        return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
      }
      function pairKeyFromNotifyCode(code) {
        var c = String(code || '');
        if (/^cb/i.test(c) || /^rs/i.test(c)) return 'p:' + c.substring(2).toLowerCase();
        return 'legacy:' + c;
      }
      function shortNotifyChannel(t) {
        var ch = String(t.channelType || '').toUpperCase();
        return ch === 'RESULT' ? 'RESULT' : 'CALLBACK';
      }
      function groupNotifyTargetsForTable(list) {
        var byKey = {};
        var keys = [];
        (list || []).forEach(function (t) {
          var k = pairKeyFromNotifyCode(t.targetCode);
          if (!byKey[k]) {
            byKey[k] = [];
            keys.push(k);
          }
          byKey[k].push(t);
        });
        keys.sort(function (ka, kb) {
          var maxA = Math.max.apply(null, byKey[ka].map(function (x) { return Number(x.id) || 0; }));
          var maxB = Math.max.apply(null, byKey[kb].map(function (x) { return Number(x.id) || 0; }));
          return maxB - maxA;
        });
        return keys.map(function (k) {
          var g = byKey[k].slice();
          g.sort(function (a, b) {
            var oa = shortNotifyChannel(a) === 'CALLBACK' ? 0 : 1;
            var ob = shortNotifyChannel(b) === 'CALLBACK' ? 0 : 1;
            if (oa !== ob) return oa - ob;
            return (Number(a.id) || 0) - (Number(b.id) || 0);
          });
          return g;
        });
      }
      function renderHqNotifyTargetTable(arr) {
        var tbody = pane.querySelector('#hqNotifyTargetTbody');
        var emptyEl = pane.querySelector('#hqNotifyTargetEmpty');
        if (!tbody) return;
        var list = Array.isArray(arr) ? arr : [];
        if (list.length === 0) {
          tbody.innerHTML = '';
          if (emptyEl) { emptyEl.classList.remove('d-none'); }
          return;
        }
        if (emptyEl) { emptyEl.classList.add('d-none'); }
        var groups = groupNotifyTargetsForTable(list);
        var html = '';
        var no = 0;
        groups.forEach(function (g) {
          no += 1;
          var rs = g.length;
          var name = (g[0] && g[0].targetName) ? g[0].targetName : '';
          var createdTimes = g.map(function (x) { return x.createdAt ? String(x.createdAt) : ''; }).filter(Boolean);
          createdTimes.sort();
          var createdDisp = createdTimes.length ? escNt(createdTimes[0]) : '—';
          var boundDisp = '—';
          var b0 = g[0];
          if (b0 && (b0.boundOrgUnitCode || b0.boundOrgUnitName)) {
            boundDisp = escNt((b0.boundOrgUnitCode || '').trim());
            if (b0.boundOrgUnitName) {
              boundDisp += '<br><span class="text-muted small">' + escNt(String(b0.boundOrgUnitName).substring(0, 48)) + '</span>';
            }
          }
          var bindIds = g.map(function (gg) { return gg.id != null ? String(gg.id) : ''; }).filter(function (s) { return s !== ''; });
          var bindIdsAttr = bindIds.length ? bindIds.join(',') : '';
          var bindBtn = bindIdsAttr
            ? '<div class="mt-1"><button type="button" class="btn btn-xs btn-outline-primary hq-notify-pair-bind-open" data-bind-ids="' + escNt(bindIdsAttr) + '">연결수정</button></div>'
            : '';
          var boundCellInner = ((boundDisp === '—') ? '<span class="text-muted">—</span>' : boundDisp) + bindBtn;
          g.forEach(function (t, i) {
            var id = t.id != null ? String(t.id) : '';
            var ch = shortNotifyChannel(t);
            var url = t.targetUrl || '';
            html += '<tr>';
            if (i === 0) {
              html += '<td class="text-center align-middle" rowspan="' + rs + '">' + no + '</td>';
              html += '<td class="text-center align-middle text-nowrap hq-notify-created-cell" rowspan="' + rs + '">' + createdDisp + '</td>';
              html += '<td class="align-middle" rowspan="' + rs + '">' + escNt(name) + '</td>';
              html += '<td class="text-center align-middle hq-notify-bound-org-cell" rowspan="' + rs + '">' + boundCellInner + '</td>';
            }
            var urlCls = ch === 'RESULT' ? 'hq-notify-url-cell--result' : 'hq-notify-url-cell--callback';
            html += '<td class="hq-notify-url-cell align-middle ' + urlCls + '"><code class="hq-notify-url-code">' + escNt(url) + '</code></td>';
            html += '<td class="hq-notify-copy-cell text-center align-middle">' +
              '<button type="button" class="btn btn-sm btn-outline-secondary hq-notify-copy-url" data-url="' + escNt(url) + '">복사</button></td>';
            var chCls = ch === 'RESULT' ? 'hq-notify-channel-cell--result' : 'hq-notify-channel-cell--callback';
            html += '<td class="hq-notify-channel-cell align-middle ' + chCls + '"><span class="hq-notify-channel-badge">' + escNt(ch) + '</span></td>';
            html += '<td class="align-middle text-center">' +
              (id ? '<button type="button" class="btn btn-sm btn-outline-danger hq-notify-target-del" data-id="' + escNt(id) + '">삭제</button>' : '-') + '</td>';
            html += '</tr>';
          });
        });
        tbody.innerHTML = html;
      }
      function fillMasterDistSelect(options) {
        var sel = pane.querySelector('[name="notifyTargetBoundOrgUnitId"]');
        if (!sel) return;
        var cur = sel.value || '';
        var arr = Array.isArray(options) ? options : [];
        var html = '<option value="">선택하세요</option>';
        arr.forEach(function (o) {
          var id = o.id != null ? String(o.id) : '';
          if (!id) return;
          var code = o.code != null ? String(o.code) : '';
          var nm = o.name != null ? String(o.name) : '';
          var lab = (code ? code + ' — ' : '') + nm;
          html += '<option value="' + escNt(id) + '">' + escNt(lab) + '</option>';
        });
        sel.innerHTML = html;
        if (cur) {
          var has = false;
          for (var qi = 0; qi < sel.options.length; qi++) {
            if (sel.options[qi].value === cur) { has = true; break; }
          }
          if (has) sel.value = cur;
        }
      }
      function ensureHqNotifyPairBindModal() {
        if (document.getElementById('hqNotifyPairBindModal')) return;
        var wrap = document.createElement('div');
        wrap.innerHTML = '<div class="modal fade" id="hqNotifyPairBindModal" tabindex="-1" aria-hidden="true">' +
          '<div class="modal-dialog modal-dialog-centered">' +
          '<div class="modal-content">' +
          '<div class="modal-header"><h5 class="modal-title">연결 총판</h5><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="닫기"></button></div>' +
          '<div class="modal-body">' +
          '<input type="hidden" id="hqNotifyPairBindIds" value="">' +
          '<label class="form-label" for="hqNotifyPairBindOrgSelect">총판 선택</label>' +
          '<select class="form-select form-select-sm" id="hqNotifyPairBindOrgSelect"></select>' +
          '<p class="text-muted small mb-0 mt-2">CALLBACK·RESULT 쌍에 동일 연결이 반영됩니다.</p>' +
          '</div>' +
          '<div class="modal-footer">' +
          '<button type="button" class="btn btn-primary btn-sm" id="hqNotifyPairBindSaveBtn">저장</button>' +
          '<button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">취소</button>' +
          '</div></div></div></div>';
        document.body.appendChild(wrap.firstElementChild);
        document.getElementById('hqNotifyPairBindSaveBtn').addEventListener('click', function () {
          var idsCsv = (document.getElementById('hqNotifyPairBindIds') || {}).value || '';
          var arr = idsCsv.split(',').map(function (x) { return parseInt(String(x).trim(), 10); }).filter(function (n) { return !isNaN(n) && n > 0; });
          var sel = document.getElementById('hqNotifyPairBindOrgSelect');
          var oid = sel && sel.value ? String(sel.value).trim() : '';
          if (!arr.length) { alert('대상이 없습니다.'); return; }
          if (!oid) { alert('연결 총판을 선택하세요.'); return; }
          if (!window.PG_API || !window.PG_API.hqNotifyTargetsBindBoundOrg) { alert('API를 사용할 수 없습니다.'); return; }
          if (dimmN) dimmN.style.display = 'flex';
          window.PG_API.hqNotifyTargetsBindBoundOrg(arr, oid).then(function () {
            var modalEl = document.getElementById('hqNotifyPairBindModal');
            if (modalEl && window.bootstrap && window.bootstrap.Modal) {
              var inst = window.bootstrap.Modal.getInstance(modalEl) || new window.bootstrap.Modal(modalEl);
              inst.hide();
            }
            return window.PG_API.hqNotifyTargets();
          }).then(function (list) {
            fillNotifyTargets(list);
            alert('연결되었습니다.');
          }).catch(function (e) { alert(e && e.message ? e.message : '연결 실패'); }).finally(function () { if (dimmN) dimmN.style.display = 'none'; });
        });
      }
      function openHqNotifyPairBindModal(idsCsv) {
        ensureHqNotifyPairBindModal();
        var hid = document.getElementById('hqNotifyPairBindIds');
        if (hid) hid.value = idsCsv || '';
        if (!window.PG_API || !window.PG_API.hqNotifyMasterDistOptions) return;
        window.PG_API.hqNotifyMasterDistOptions().then(function (opts) {
          var modalSel = document.getElementById('hqNotifyPairBindOrgSelect');
          if (!modalSel) return;
          var html = '<option value="">선택하세요</option>';
          (opts || []).forEach(function (o) {
            var oid = o.id != null ? String(o.id) : '';
            if (!oid) return;
            var code = o.code != null ? String(o.code) : '';
            var nm = o.name != null ? String(o.name) : '';
            var lab = (code ? code + ' — ' : '') + nm;
            html += '<option value="' + escNt(oid) + '">' + escNt(lab) + '</option>';
          });
          modalSel.innerHTML = html;
          var modalEl = document.getElementById('hqNotifyPairBindModal');
          if (modalEl && window.bootstrap && window.bootstrap.Modal) {
            var m = window.bootstrap.Modal.getInstance(modalEl) || new window.bootstrap.Modal(modalEl);
            m.show();
          }
        }).catch(function () { alert('총판 목록을 불러오지 못했습니다.'); });
      }
      function fillNotifyTargets(list) {
        var arr = Array.isArray(list) ? list : [];
        pane.querySelectorAll('select[data-load-notify-targets="true"]').forEach(function (sel) {
          var cur = sel.value || '';
          var html = '<option value="">선택</option>';
          arr.forEach(function (t) {
            var ch = shortNotifyChannel(t);
            var label = (t.targetName || t.targetCode || '노티') + ' [' + ch + '] ' + (t.targetUrl || '');
            html += '<option value="' + escNt(t.targetUrl || '') + '" data-id="' + escNt(t.id != null ? String(t.id) : '') + '">' + escNt(label) + '</option>';
          });
          sel.innerHTML = html;
          if (cur) sel.value = cur;
        });
        renderHqNotifyTargetTable(arr);
      }
      function fillNotifyEnv(data) {
        if (!data) return;
        ['notifyIngressUrl', 'ingressToken', 'publicBaseUrl', 'notifyOkResponse'].forEach(function (k) {
          var el = pane.querySelector('[name="' + k + '"]');
          if (el && data[k] != null && data[k] !== undefined) el.value = data[k];
        });
      }
      if (dimmN) dimmN.style.display = 'flex';
      Promise.all([
        window.PG_API.hqNotifyEnv(),
        window.PG_API.hqNotifyTargets(),
        window.PG_API.hqNotifyMasterDistOptions ? window.PG_API.hqNotifyMasterDistOptions() : Promise.resolve([])
      ]).then(function (res) {
        fillNotifyEnv(res[0]);
        fillNotifyTargets(res[1]);
        fillMasterDistSelect(res[2]);
      }).catch(function () {}).finally(function () { if (dimmN) dimmN.style.display = 'none'; });
      var hqNotifySave = pane.querySelector('#hqNotifyEnvSaveBtn');
      if (hqNotifySave && !hqNotifySave._hqNeBound) {
        hqNotifySave._hqNeBound = true;
        hqNotifySave.addEventListener('click', function () {
          var fd = {};
          ['publicBaseUrl', 'notifyOkResponse'].forEach(function (k) {
            var el = pane.querySelector('[name="' + k + '"]');
            if (el) fd[k] = el.value;
          });
          if (dimmN) dimmN.style.display = 'flex';
          window.PG_API.hqNotifyEnvSave(fd).then(function (data) { fillNotifyEnv(data); alert('저장되었습니다.'); }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimmN) dimmN.style.display = 'none'; });
        });
      }
      var hqNotifyRegen = pane.querySelector('#hqNotifyRegenTokenBtn');
      if (hqNotifyRegen && !hqNotifyRegen._hqNeBound) {
        hqNotifyRegen._hqNeBound = true;
        hqNotifyRegen.addEventListener('click', function () {
          if (!window.confirm('노티 URL 토큰이 바뀝니다. NOTI/칠페이에 등록된 URL도 함께 바꿔야 합니다. 계속하시겠습니까?')) return;
          if (dimmN) dimmN.style.display = 'flex';
          window.PG_API.hqNotifyEnvRegenerateToken().then(function (data) { fillNotifyEnv(data); alert('토큰이 재발급되었습니다. 새 URL을 NOTI에 반영하세요.'); }).catch(function (e) { alert(e && e.message ? e.message : '실패'); }).finally(function () { if (dimmN) dimmN.style.display = 'none'; });
        });
      }
      var createBtn = pane.querySelector('button[data-field="newNotifyTargetName"][data-action="노티자동생성"]');
      if (createBtn && !createBtn._bound) {
        createBtn._bound = true;
        createBtn.addEventListener('click', function () {
          var orgSel = pane.querySelector('[name="notifyTargetBoundOrgUnitId"]');
          var boundId = orgSel && orgSel.value ? String(orgSel.value).trim() : '';
          if (!boundId) { alert('연결 총판을 선택하세요.'); return; }
          var nameEl = pane.querySelector('[name="newNotifyTargetName"]');
          var name = nameEl && nameEl.value ? String(nameEl.value).trim() : '';
          if (!name) { alert('노티 대상명을 입력하세요.'); return; }
          if (dimmN) dimmN.style.display = 'flex';
          window.PG_API.hqNotifyTargetCreate(name, boundId).then(function () {
            return window.PG_API.hqNotifyTargets();
          }).then(function (list) {
            fillNotifyTargets(list);
            if (nameEl) nameEl.value = '';
            alert('CALLBACK·RESULT 노티 URL이 자동 생성되었습니다. 아래 목록에서 확인하세요.');
          }).catch(function (e) { alert(e && e.message ? e.message : '노티 자동생성 실패'); }).finally(function () { if (dimmN) dimmN.style.display = 'none'; });
        });
      }
      if (!pane._hqNotifyTargetTableActionDelegated) {
        pane._hqNotifyTargetTableActionDelegated = true;
        pane.addEventListener('click', function (ev) {
          var bindOpen = ev.target && ev.target.closest ? ev.target.closest('.hq-notify-pair-bind-open') : null;
          if (bindOpen && pane.contains(bindOpen)) {
            var csv = bindOpen.getAttribute('data-bind-ids') || '';
            openHqNotifyPairBindModal(csv);
            return;
          }
          var copyB = ev.target && ev.target.closest ? ev.target.closest('.hq-notify-copy-url') : null;
          if (copyB && pane.contains(copyB)) {
            var u = copyB.getAttribute('data-url') || '';
            if (!u) return;
            var fail = function () { alert('복사에 실패했습니다. 주소를 직접 선택해 복사하세요.'); };
            if (navigator.clipboard && navigator.clipboard.writeText) {
              navigator.clipboard.writeText(u).catch(function () {
                var ta = document.createElement('textarea');
                ta.value = u;
                document.body.appendChild(ta);
                ta.select();
                try { if (!document.execCommand('copy')) fail(); } catch (e2) { fail(); }
                document.body.removeChild(ta);
              });
            } else {
              var ta2 = document.createElement('textarea');
              ta2.value = u;
              document.body.appendChild(ta2);
              ta2.select();
              try { if (!document.execCommand('copy')) fail(); } catch (e3) { fail(); }
              document.body.removeChild(ta2);
            }
            return;
          }
          var delB = ev.target && ev.target.closest ? ev.target.closest('.hq-notify-target-del') : null;
          if (!delB || !pane.contains(delB)) return;
          var rid = delB.getAttribute('data-id') || '';
          if (!rid) return;
          if (!window.confirm('이 노티 URL을 삭제하시겠습니까?')) return;
          if (dimmN) dimmN.style.display = 'flex';
          window.PG_API.hqNotifyTargetDelete(rid).then(function () {
            return window.PG_API.hqNotifyTargets();
          }).then(function (list) {
            fillNotifyTargets(list);
            alert('삭제되었습니다.');
          }).catch(function (e) { alert(e && e.message ? e.message : '삭제 실패'); }).finally(function () { if (dimmN) dimmN.style.display = 'none'; });
        });
      }
    }
    if (url === '/hq/userSettings') {
      var dimmUs = document.getElementById('dimm');
      var userPolicyKeys = ['otpRequiredYn', 'otpPolicyMode', 'passwordPolicyMode', 'forgotPasswordEnabledYn', 'managerUserControlEnabledYn', 'managerPasswordResetEnabledYn'];
      function fillHqUserSettings(data) {
        if (!data) return;
        userPolicyKeys.forEach(function (k) {
          var el = pane.querySelector('[name="' + k + '"]');
          if (el && data[k] != null && data[k] !== undefined) el.value = data[k];
        });
      }
      if (dimmUs) dimmUs.style.display = 'flex';
      window.PG_API.hqNotifyEnv().then(function (data) {
        fillHqUserSettings(data);
      }).catch(function () {}).finally(function () { if (dimmUs) dimmUs.style.display = 'none'; });
      var hqUserStSave = pane.querySelector('#hqUserSettingsSaveBtn');
      if (hqUserStSave && !hqUserStSave._hqUsBound) {
        hqUserStSave._hqUsBound = true;
        hqUserStSave.addEventListener('click', function () {
          var fd = {};
          userPolicyKeys.forEach(function (k) {
            var el = pane.querySelector('[name="' + k + '"]');
            if (el) fd[k] = el.value;
          });
          if (dimmUs) dimmUs.style.display = 'flex';
          window.PG_API.hqNotifyEnvSave(fd).then(function (data) {
            fillHqUserSettings(data);
            alert('저장되었습니다.');
          }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimmUs) dimmUs.style.display = 'none'; });
        });
      }
    }
    if (url === '/hq/orgViewColumnAllowance' && !pane._hqOrgColAllowBound) {
      pane._hqOrgColAllowBound = true;
      var dimmO = document.getElementById('dimm');
      var defaultFixedGuideKeys = ['rowNo', 'compId', 'compNm', 'compDivNm', 'trnDate', 'trnTime', 'routeNo'];
      function hqOrgAllowFixedKeysForPage(pageUrl) {
        var u = pageUrl || '';
        if (u === '/calc/chillPayTrList') {
          return ['rowNo', 'transactionId', 'compNm', 'compId', 'trnDate', 'trnTime', 'routeNo'];
        }
        if (u === '/calc/chillPaySettlementList') {
          return ['rowNo'];
        }
        if (u === '/commission/commisionList') {
          return ['rowNo', 'compNm', 'compId'];
        }
        return defaultFixedGuideKeys;
      }
      var hqOrgAllowPageLabels = {
        '/calc/chillPayTrList': '통합내역',
        '/calc/chillPaySettlementList': '통합정산',
        '/calc/payList': '결제내역',
        '/calc/paySuccessList': '성공내역',
        '/calc/payFailList': '실패내역',
        '/calc/payCancelList': '취소내역',
        '/calc/payVoidList': '무효내역',
        '/calc/payRefundList': '환불내역',
        '/calc/payForceRefundList': '강제환불내역',
        '/pay/easyPay': 'URL결제내역',
        '/pay/chatbotPay': '챗봇결제내역',
        '/calc/offsetCancList': '상계취소내역',
        '/comp/compMngTree': '업체관리',
        '/commission/commisionList': '수수료관리',
        '/calc/calcList': '유통망정산내역',
        '/calc/calcGmList': '가맹점정산내역',
        '/calc/paySettlementHoldList': '정산보류내역',
        '/calc/feeList': '수수료내역',
        '/calc/compPointMngList': '환수금관리',
        '/calc/exCalcList': '정산실행',
        '/calc/settlementReport': '정산리포트',
        '/calc/collateralList': '담보금내역',
        '/pay/payHoldList': '보증금내역'
      };
      var hqOrgAllowScopeLabels = { REGIONAL: '본사', MASTER_DIST: '총판', BRANCH_GROUP: '지사·대리점·영업점', MERCHANT: '가맹점' };
      function pageUrlVal() {
        var sel = pane.querySelector('[name="targetPageUrl"]');
        return sel && sel.value ? String(sel.value) : '/calc/payList';
      }
      function regionalVal() {
        var sel = pane.querySelector('[name="regionalOrgCode"]');
        return sel && sel.value ? String(sel.value).trim() : '';
      }
      function viewerScopeVal() {
        var sel = pane.querySelector('[name="viewerScope"]');
        return sel && sel.value ? String(sel.value).trim() : 'REGIONAL';
      }
      function resolveColumnsForHqOrgAllow(cfg, pageUrl) {
        if (!cfg) return [];
        if (cfg.columns && cfg.columns.length) return cfg.columns;
        var u = pageUrl || '';
        if (u === '/calc/settlementReport' || u === '/settlement/settlementReport') {
          var byKey = {};
          function addArr(arr) {
            (arr || []).forEach(function (c) {
              if (!c || !c.key) return;
              if (!byKey[c.key]) byKey[c.key] = c;
            });
          }
          ['AGG', 'EXE', 'SUM', 'RST'].forEach(function (sub) {
            if (cfg.columnsBySub && cfg.columnsBySub[sub]) addArr(cfg.columnsBySub[sub]);
            if (cfg.columnsRegionalPayout && cfg.columnsRegionalPayout[sub]) addArr(cfg.columnsRegionalPayout[sub]);
          });
          return Object.keys(byKey).map(function (k) { return byKey[k]; });
        }
        return [];
      }
      function escHqOrgCell(s) {
        return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
      }
      function renderHqViewCustomColTable(rows) {
        var tb = pane.querySelector('#hqViewCustomColTbody');
        if (!tb) return;
        var list = Array.isArray(rows) ? rows : [];
        if (!list.length) {
          tb.innerHTML = '<tr><td colspan="4" class="text-center text-muted small py-2">등록된 추가 항목이 없습니다.</td></tr>';
          return;
        }
        var h = '';
        list.forEach(function (row) {
          var id = row.id != null ? String(row.id) : '';
          var nm = row.displayName != null ? String(row.displayName) : '';
          var ck = row.columnKey != null ? String(row.columnKey) : '';
          h += '<tr data-hq-vcc-id="' + escHqOrgCell(id) + '"><td class="align-middle small">' + escHqOrgCell(nm) + '</td>' +
            '<td class="align-middle"><code class="small">' + escHqOrgCell(ck) + '</code></td>' +
            '<td class="text-center align-middle"><button type="button" class="btn btn-sm btn-outline-primary hq-vcc-edit">수정</button></td>' +
            '<td class="text-center align-middle"><button type="button" class="btn btn-sm btn-outline-danger hq-vcc-del">삭제</button></td></tr>';
        });
        tb.innerHTML = h;
      }
      function loadHqViewCustomColList() {
        var p = pageUrlVal();
        if (!window.PG_API || !window.PG_API.hqOrgViewCustomColumns) return;
        window.PG_API.hqOrgViewCustomColumns(p).then(function (list) {
          renderHqViewCustomColTable(list);
        }).catch(function () {
          renderHqViewCustomColTable([]);
        });
      }
      /** 서버에 저장된 정책이 없을 때: 결제관리 화면은 조직 유형별 기본안, 그 외는 전체 체크 */
      function hqOrgApplyChecksWhenNoSavedPolicy() {
        var p = pageUrlVal();
        var set = hqOrgAllowanceDefaultKeySet(p, viewerScopeVal());
        if (set == null) {
          pane.querySelectorAll('.hq-allow-col-check').forEach(function (cb) { cb.checked = true; });
        } else {
          pane.querySelectorAll('.hq-allow-col-check').forEach(function (cb) {
            var kk = cb.getAttribute('data-key') || '';
            cb.checked = kk.length > 0 && set.indexOf(kk) !== -1;
          });
        }
      }
      function buildAllowanceChecks(pageUrl, afterBuild) {
        var mount = pane.querySelector('#hqOrgAllowColumnChecks');
        if (!mount || !window.PG_SCREENS || !window.PG_SCREENS.getMenuScreens) {
          if (typeof afterBuild === 'function') afterBuild();
          return;
        }
        var cfg = window.PG_SCREENS.getMenuScreens()[pageUrl];
        var colDefs = resolveColumnsForHqOrgAllow(cfg, pageUrl);
        function hqAllowColDefaultChecked(pageUrl0, colKey) {
          var set = hqOrgAllowanceDefaultKeySet(pageUrl0, viewerScopeVal());
          if (set == null) return true;
          return set.indexOf(colKey) !== -1;
        }
        function paintNativeCols(resolvedColDefs) {
          if (!cfg || !resolvedColDefs.length) {
            mount.innerHTML = '<span class="text-muted small">화면 정의를 찾을 수 없습니다.</span>';
            if (typeof afterBuild === 'function') afterBuild();
            return;
          }
          var fixedGk = hqOrgAllowFixedKeysForPage(pageUrl);
          var html = '';
          resolvedColDefs.forEach(function (c) {
            if (!c.key || c.type === 'checkbox' || c.type === 'payActions' || c.type === 'commissionInlineActions' || fixedGk.indexOf(c.key) !== -1) return;
            var label = c.columnGuideLabel || c.label || c.key;
            var on0 = hqAllowColDefaultChecked(pageUrl, c.key);
            html += '<label class="column-guide-item column-guide-item--on d-inline-flex align-items-center me-2 mb-1"><input type="checkbox" class="hq-allow-col-check" data-key="' + escHqOrgCell(c.key) + '"' + (on0 ? ' checked' : '') + '> <span class="column-guide-label small">' + escHqOrgCell(label) + '</span></label>';
          });
          mount.innerHTML = html || '<span class="text-muted small">선택 가능한 열이 없습니다.</span>';
          if (typeof afterBuild === 'function') afterBuild();
        }
        function paintWithCustom(resolvedColDefs) {
          if (!cfg || !resolvedColDefs.length) {
            mount.innerHTML = '<span class="text-muted small">화면 정의를 찾을 수 없습니다.</span>';
            if (typeof afterBuild === 'function') afterBuild();
            return;
          }
          if (!window.PG_API || !window.PG_API.hqOrgViewCustomColumns) {
            paintNativeCols(resolvedColDefs);
            return;
          }
          window.PG_API.hqOrgViewCustomColumns(pageUrl).then(function (customArr) {
            var fixedGk2 = hqOrgAllowFixedKeysForPage(pageUrl);
            var html = '';
            resolvedColDefs.forEach(function (c) {
              if (!c.key || c.type === 'checkbox' || c.type === 'payActions' || c.type === 'commissionInlineActions' || fixedGk2.indexOf(c.key) !== -1) return;
              var label = c.columnGuideLabel || c.label || c.key;
              var on1 = hqAllowColDefaultChecked(pageUrl, c.key);
              html += '<label class="column-guide-item column-guide-item--on d-inline-flex align-items-center me-2 mb-1"><input type="checkbox" class="hq-allow-col-check" data-key="' + escHqOrgCell(c.key) + '"' + (on1 ? ' checked' : '') + '> <span class="column-guide-label small">' + escHqOrgCell(label) + '</span></label>';
            });
            (customArr || []).forEach(function (cc) {
              var k = cc.columnKey != null ? String(cc.columnKey) : '';
              var lab = cc.displayName != null ? String(cc.displayName) : k;
              if (!k) return;
              var onC = hqAllowColDefaultChecked(pageUrl, k);
              html += '<label class="column-guide-item column-guide-item--on d-inline-flex align-items-center me-2 mb-1">' +
                '<input type="checkbox" class="hq-allow-col-check" data-key="' + escHqOrgCell(k) + '" data-hq-custom="1"' + (onC ? ' checked' : '') + '> ' +
                '<span class="column-guide-label small">' + escHqOrgCell(lab) + ' <span class="badge bg-secondary">추가</span></span></label>';
            });
            mount.innerHTML = html || '<span class="text-muted small">선택 가능한 열이 없습니다.</span>';
            if (typeof afterBuild === 'function') afterBuild();
          }).catch(function () {
            paintNativeCols(resolvedColDefs);
          });
        }
        if (PAY_LIST_NOTIFY_LAYOUT_URLS.indexOf(pageUrl) !== -1 && window.PG_API && window.PG_API.payListScreenLayout) {
          window.PG_API.payListScreenLayout(pageUrl).then(function (layout) {
            var merged = layout && !layout.error && Array.isArray(layout.columns) && layout.columns.length
              ? layout.columns.map(function (c) {
                var o = { key: c.key, label: c.label != null ? c.label : c.key };
                if (c.key === '_chk') o.type = 'checkbox';
                return o;
              })
              : null;
            paintWithCustom(merged && merged.length ? merged : colDefs);
          }).catch(function () {
            paintWithCustom(colDefs);
          });
          return;
        }
        paintWithCustom(colDefs);
      }
      function readCheckedAllowKeys() {
        var keys = [];
        pane.querySelectorAll('.hq-allow-col-check:checked').forEach(function (cb) {
          var k = cb.getAttribute('data-key');
          if (k) keys.push(k);
        });
        return keys;
      }
      function refreshHqOrgAllowPolicySummary() {
        var tb = pane.querySelector('#hqOrgAllowPolicyList');
        var hint = pane.querySelector('#hqOrgAllowPolicyListHint');
        if (!tb) return;
        var r = regionalVal();
        if (!r) {
          tb.innerHTML = '<tr><td colspan="4" class="text-muted text-center small py-3">설정 대상 본사를 선택하면 저장된 정책이 표시됩니다.</td></tr>';
          if (hint) hint.textContent = '';
          return;
        }
        if (!window.PG_API || !window.PG_API.hqOrgViewColumnAllowanceList) return;
        window.PG_API.hqOrgViewColumnAllowanceList(r).then(function (list) {
          var arr = Array.isArray(list) ? list : [];
          if (!arr.length) {
            tb.innerHTML = '<tr><td colspan="4" class="text-muted text-center small py-3">저장된 정책이 없습니다. 열을 체크한 뒤 [저장] 또는 하단 [노출 항목 저장]으로 저장하세요.</td></tr>';
            if (hint) hint.textContent = '';
            return;
          }
          var h = '';
          arr.forEach(function (row) {
            var pu = row.pageUrl != null ? String(row.pageUrl) : '';
            var vs = row.viewerScope != null ? String(row.viewerScope) : '';
            var cnt = row.allowedColumnCount != null ? row.allowedColumnCount : 0;
            var ua = row.updatedAt != null ? String(row.updatedAt) : '-';
            var plab = hqOrgAllowPageLabels[pu] || pu;
            var slab = hqOrgAllowScopeLabels[vs] || vs;
            h += '<tr class="hq-org-allow-policy-row" style="cursor:pointer" data-page-url="' + escHqOrgCell(pu) + '" data-viewer-scope="' + escHqOrgCell(vs) + '">' +
              '<td class="small">' + escHqOrgCell(plab) + '</td>' +
              '<td class="small">' + escHqOrgCell(slab) + '</td>' +
              '<td class="text-end small">' + escHqOrgCell(String(cnt)) + '</td>' +
              '<td class="small text-nowrap">' + escHqOrgCell(ua) + '</td></tr>';
          });
          tb.innerHTML = h;
          if (hint) hint.textContent = '행을 클릭하면 위에서 해당 화면·조직 유형으로 전환하고 저장된 체크를 불러옵니다.';
        }).catch(function (e) {
          tb.innerHTML = '<tr><td colspan="4" class="text-danger small text-center py-3">' + escHqOrgCell(e && e.message ? e.message : '목록 조회 실패') + '</td></tr>';
          if (hint) hint.textContent = '';
        });
      }
      function hqOrgAllowSaveCurrentPolicy() {
        var r = regionalVal();
        var p = pageUrlVal();
        if (!r) { alert('대상 본사를 선택하세요.'); return; }
        var keys = readCheckedAllowKeys();
        if (!keys.length && !window.confirm('허용 열이 하나도 없습니다. (선택 컬럼 없음) 저장할까요?')) return;
        if (dimmO) dimmO.style.display = 'flex';
        window.PG_API.hqOrgViewColumnAllowanceSave({
          regionalOrgCode: r,
          pageUrl: p,
          viewerScope: viewerScopeVal(),
          allowedKeysJson: JSON.stringify(keys)
        }).then(function () {
          alert('저장되었습니다.');
          refreshHqOrgAllowPolicySummary();
        }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimmO) dimmO.style.display = 'none'; });
      }
      function applyChecksFromJson(jsonStr) {
        var keys = [];
        try { keys = JSON.parse(jsonStr || '[]'); } catch (e) { keys = []; }
        if (!Array.isArray(keys)) keys = [];
        var set = {};
        keys.forEach(function (k) { set[k] = 1; });
        pane.querySelectorAll('.hq-allow-col-check').forEach(function (cb) {
          var k = cb.getAttribute('data-key') || '';
          cb.checked = !!set[k];
        });
      }
      if (pane.querySelector('select[data-load-regional-branches="true"]') && window.PG_API && window.PG_API.hqOrgViewColumnRegionalBranches) {
        window.PG_API.hqOrgViewColumnRegionalBranches().then(function (list) {
          var arr = Array.isArray(list) ? list : [];
          pane.querySelectorAll('select[data-load-regional-branches="true"]').forEach(function (sel) {
            var cur = sel.value || '';
            var h = '<option value="">선택</option>';
            arr.forEach(function (o) {
              var c = o.code || o.compId || '';
              var n = o.name || c;
              if (!c) return;
              h += '<option value="' + c + '">' + n + ' (' + c + ')</option>';
            });
            sel.innerHTML = h;
            if (cur) sel.value = cur;
          });
          refreshHqOrgAllowPolicySummary();
        }).catch(function () {});
      }
      loadHqViewCustomColList();
      buildAllowanceChecks(pageUrlVal());
      if (!pane._hqOrgVccDelegated) {
        pane._hqOrgVccDelegated = true;
        pane.addEventListener('click', function (ev) {
          var addB = ev.target && ev.target.closest ? ev.target.closest('#hqViewCustomColAddBtn') : null;
          if (addB && pane.contains(addB)) {
            var inp = pane.querySelector('#hqViewCustomColNameInp');
            var nm = inp && inp.value ? String(inp.value).trim() : '';
            if (!nm) { alert('표시명을 입력하세요.'); return; }
            if (dimmO) dimmO.style.display = 'flex';
            window.PG_API.hqOrgViewCustomColumnAdd({ pageUrl: pageUrlVal(), displayName: nm }).then(function () {
              if (inp) inp.value = '';
              loadHqViewCustomColList();
              buildAllowanceChecks(pageUrlVal());
              alert('추가되었습니다.');
            }).catch(function (e) { alert(e && e.message ? e.message : '추가 실패'); }).finally(function () { if (dimmO) dimmO.style.display = 'none'; });
            return;
          }
          var relB = ev.target && ev.target.closest ? ev.target.closest('#hqViewCustomColReloadBtn') : null;
          if (relB && pane.contains(relB)) {
            loadHqViewCustomColList();
            buildAllowanceChecks(pageUrlVal());
            return;
          }
          var ed = ev.target && ev.target.closest ? ev.target.closest('.hq-vcc-edit') : null;
          if (ed && pane.contains(ed)) {
            var tr = ed.closest('tr');
            var rid = tr ? tr.getAttribute('data-hq-vcc-id') : '';
            var curNm = tr && tr.cells && tr.cells[0] ? tr.cells[0].textContent.trim() : '';
            var nn = window.prompt('표시명 수정', curNm);
            if (nn == null) return;
            nn = String(nn).trim();
            if (!nn) { alert('표시명을 비울 수 없습니다.'); return; }
            if (dimmO) dimmO.style.display = 'flex';
            window.PG_API.hqOrgViewCustomColumnUpdate({ id: rid, displayName: nn }).then(function () {
              loadHqViewCustomColList();
              buildAllowanceChecks(pageUrlVal());
            }).catch(function (e) { alert(e && e.message ? e.message : '수정 실패'); }).finally(function () { if (dimmO) dimmO.style.display = 'none'; });
            return;
          }
          var del = ev.target && ev.target.closest ? ev.target.closest('.hq-vcc-del') : null;
          if (del && pane.contains(del)) {
            var tr2 = del.closest('tr');
            var rid2 = tr2 ? tr2.getAttribute('data-hq-vcc-id') : '';
            if (!rid2 || !window.confirm('이 추가 항목을 삭제할까요?')) return;
            if (dimmO) dimmO.style.display = 'flex';
            window.PG_API.hqOrgViewCustomColumnDelete({ id: rid2 }).then(function () {
              loadHqViewCustomColList();
              buildAllowanceChecks(pageUrlVal());
            }).catch(function (e) { alert(e && e.message ? e.message : '삭제 실패'); }).finally(function () { if (dimmO) dimmO.style.display = 'none'; });
          }
        });
      }
      if (!pane._hqOrgAllowBulkBound) {
        pane._hqOrgAllowBulkBound = true;
        pane.addEventListener('click', function (ev) {
          var t = ev.target;
          if (!t || !t.id) return;
          if (t.id === 'hqOrgAllowColSaveBtn') {
            ev.preventDefault();
            hqOrgAllowSaveCurrentPolicy();
          } else if (t.id === 'hqOrgAllowColSelectAllBtn') {
            ev.preventDefault();
            pane.querySelectorAll('.hq-allow-col-check').forEach(function (cb) { cb.checked = true; });
          } else if (t.id === 'hqOrgAllowColClearAllBtn') {
            ev.preventDefault();
            pane.querySelectorAll('.hq-allow-col-check').forEach(function (cb) { cb.checked = false; });
          }
        });
      }
      if (!pane._hqOrgAllowPolicyRowBound) {
        pane._hqOrgAllowPolicyRowBound = true;
        pane.addEventListener('click', function (ev) {
          var tr = ev.target && ev.target.closest ? ev.target.closest('tr.hq-org-allow-policy-row') : null;
          if (!tr || !pane.contains(tr)) return;
          var pu = tr.getAttribute('data-page-url') || '';
          var vs = tr.getAttribute('data-viewer-scope') || '';
          var ps = pane.querySelector('[name="targetPageUrl"]');
          var vsEl = pane.querySelector('[name="viewerScope"]');
          if (ps && pu) ps.value = pu;
          if (vsEl && vs) vsEl.value = vs;
          loadHqViewCustomColList();
          buildAllowanceChecks(pageUrlVal(), function () {
            var r = regionalVal();
            var p = pageUrlVal();
            if (!r) return;
            if (dimmO) dimmO.style.display = 'flex';
            window.PG_API.hqOrgViewColumnAllowanceGet(r, p, viewerScopeVal()).then(function (d) {
              if (d && d.hasPolicy && d.allowedKeysJson != null) applyChecksFromJson(String(d.allowedKeysJson));
              else hqOrgApplyChecksWhenNoSavedPolicy();
            }).catch(function () {}).finally(function () { if (dimmO) dimmO.style.display = 'none'; });
          });
        });
      }
      var pageSel = pane.querySelector('[name="targetPageUrl"]');
      if (pageSel && !pageSel._hqOrgColPageBound) {
        pageSel._hqOrgColPageBound = true;
        pageSel.addEventListener('change', function () {
          loadHqViewCustomColList();
          buildAllowanceChecks(pageUrlVal());
        });
      }
      var vsSelOrgAllow = pane.querySelector('[name="viewerScope"]');
      if (vsSelOrgAllow && !vsSelOrgAllow._hqOrgColScopeBound) {
        vsSelOrgAllow._hqOrgColScopeBound = true;
        vsSelOrgAllow.addEventListener('change', function () {
          loadHqViewCustomColList();
          buildAllowanceChecks(pageUrlVal());
        });
      }
      var loadBtn = pane.querySelector('#hqOrgAllowLoadBtn');
      if (loadBtn && !loadBtn._bound) {
        loadBtn._bound = true;
        loadBtn.addEventListener('click', function () {
          var r = regionalVal();
          var p = pageUrlVal();
          if (!r) { alert('대상 본사를 선택하세요.'); return; }
          if (dimmO) dimmO.style.display = 'flex';
          window.PG_API.hqOrgViewColumnAllowanceGet(r, p, viewerScopeVal()).then(function (d) {
            buildAllowanceChecks(p, function () {
              if (d && d.hasPolicy && d.allowedKeysJson != null) applyChecksFromJson(String(d.allowedKeysJson));
              else hqOrgApplyChecksWhenNoSavedPolicy();
            });
          }).catch(function (e) { alert(e && e.message ? e.message : '불러오기 실패'); }).finally(function () { if (dimmO) dimmO.style.display = 'none'; });
        });
      }
      var regSelOrgAllow = pane.querySelector('[name="regionalOrgCode"]');
      if (regSelOrgAllow && !regSelOrgAllow._hqOrgAllowSummaryBound) {
        regSelOrgAllow._hqOrgAllowSummaryBound = true;
        regSelOrgAllow.addEventListener('change', function () {
          refreshHqOrgAllowPolicySummary();
        });
      }
      refreshHqOrgAllowPolicySummary();
      var saveBtn = pane.querySelector('#hqOrgAllowSaveBtn');
      if (saveBtn && !saveBtn._bound) {
        saveBtn._bound = true;
        saveBtn.addEventListener('click', function () {
          hqOrgAllowSaveCurrentPolicy();
        });
      }
      var delBtn = pane.querySelector('#hqOrgAllowDeleteBtn');
      if (delBtn && !delBtn._bound) {
        delBtn._bound = true;
        delBtn.addEventListener('click', function () {
          var r = regionalVal();
          var p = pageUrlVal();
          if (!r) { alert('대상 본사를 선택하세요.'); return; }
          if (!window.confirm('선택한 본사·조직 유형·화면에 대한 컬럼 제한만 해제합니다. 계속할까요?')) return;
          if (dimmO) dimmO.style.display = 'flex';
          window.PG_API.hqOrgViewColumnAllowanceDelete({ regionalOrgCode: r, pageUrl: p, viewerScope: viewerScopeVal() }).then(function () {
            buildAllowanceChecks(p, function () {
              hqOrgApplyChecksWhenNoSavedPolicy();
            });
            refreshHqOrgAllowPolicySummary();
            alert('제한이 해제되었습니다.');
          }).catch(function (e) { alert(e && e.message ? e.message : '실패'); }).finally(function () { if (dimmO) dimmO.style.display = 'none'; });
        });
      }
    }
    if (url === '/hq/notifyMapping' && !pane._hqNotifyMappingBound) {
      pane._hqNotifyMappingBound = true;
      var dimmMap = document.getElementById('dimm');
      function fillNotifyMapping(d) {
        if (!d) return;
        var j = pane.querySelector('[name="mappingDefinitionJson"]');
        if (j && d.mappingDefinitionJson != null) j.value = d.mappingDefinitionJson;
        var u = pane.querySelector('[name="updatedAt"]');
        if (u && d.updatedAt != null) u.value = d.updatedAt;
      }
      if (dimmMap) dimmMap.style.display = 'flex';
      window.PG_API.hqNotifyMapping().then(function (d) {
        fillNotifyMapping(d);
      }).catch(function (e) {
        var msg = e && e.message ? e.message : '노티 매핑 설정을 불러오지 못했습니다.';
        alert(msg + ' 로그인·배포·(CSP일 때) connect-src 또는 <html data-pg-api-base="same-origin">(관리자와 API 동일 호스트)을 확인하세요. 편집 화면은 빈 상태로 열립니다.');
      }).finally(function () {
        initHqNotifyMappingEditor(pane);
        if (dimmMap) dimmMap.style.display = 'none';
      });
      var hqNmSave = pane.querySelector('#hqNotifyMappingSaveBtn');
      if (hqNmSave) {
        hqNmSave.addEventListener('click', function () {
          var visJson = pane.querySelector('#hqNotifyMappingJsonVisible');
          var jsonWrapEl = pane.querySelector('#hqNotifyMappingJsonEditorWrap');
          var raw = pane.querySelector('[name="mappingDefinitionJson"]');
          if (visJson && jsonWrapEl && !jsonWrapEl.classList.contains('d-none')) {
            if (raw) raw.value = visJson.value;
          } else if (pane._hqNotifyMappingReadUi && !pane._hqNotifyMappingReadUi()) {
            alert('표 입력값을 확인하세요. 카탈로그/채널 JSON 형식 오류일 수 있습니다.');
            return;
          }
          var txt = raw ? String(raw.value || '').trim() : '';
          try {
            JSON.parse(txt);
          } catch (e) {
            alert('JSON 형식이 올바르지 않습니다. 중괄호·쉼표·따옴표를 확인하세요.');
            return;
          }
          if (dimmMap) dimmMap.style.display = 'flex';
          window.PG_API.hqNotifyMappingSave({ mappingDefinitionJson: txt }).then(function (d) {
            fillNotifyMapping(d);
            initHqNotifyMappingEditor(pane);
            alert('저장되었습니다. 카탈로그에 넣은 「우리 표시명」은 결제내역 계열 그리드와 조직항목설정(VIEW)에서 보이는 열 이름과 동일하게 적용됩니다.');
          }).catch(function (e) {
            alert(e && e.message ? e.message : '저장 실패');
          }).finally(function () { if (dimmMap) dimmMap.style.display = 'none'; });
        });
      }
    }
    if (url === '/hq/notifyInbound' && !pane._hqNotifyInboundBound) {
      pane._hqNotifyInboundBound = true;
      var niDimm = document.getElementById('dimm');
      function ensureHqNiDetailModalInBody() {
        var el = document.getElementById('hqNiDetailModal');
        if (!el) return;
        var onBody = document.body.querySelector('#hqNiDetailModal');
        if (onBody && onBody !== el) {
          try { onBody.remove(); } catch (eRm) { /* ignore */ }
        }
        if (el.parentElement !== document.body) {
          document.body.appendChild(el);
        }
      }
      function hideHqNiDetailModal() {
        var el = document.getElementById('hqNiDetailModal');
        if (el && window.bootstrap && bootstrap.Modal) {
          var inst = bootstrap.Modal.getInstance(el);
          if (inst) inst.hide();
        }
      }
      if (!window._hqNiModalCloseDelegated) {
        window._hqNiModalCloseDelegated = true;
        document.body.addEventListener('click', function (ev) {
          var t = ev.target;
          if (!t || !t.id) return;
          if (t.id === 'hqNiDetailCloseX' || t.id === 'hqNiDetailCloseBtn') {
            hideHqNiDetailModal();
          }
        });
      }
      function niCollectParams() {
        var pcEl = pane.querySelector('#pageCnt');
        var rpEl = pane.querySelector('#recordsPerPage');
        var pg = pcEl ? parseInt(pcEl.value, 10) : 1;
        var sz = rpEl ? parseInt(rpEl.value, 10) : 25;
        if (isNaN(pg) || pg < 1) pg = 1;
        if (isNaN(sz) || sz < 1) sz = 25;
        return {
          page: pg,
          size: sz,
          searchKey: (pane.querySelector('[name="niSearchKey"]') || {}).value || 'MID',
          searchValue: (pane.querySelector('[name="niSearchValue"]') || {}).value || '',
          fromDate: (pane.querySelector('[name="niSearchFrom"]') || {}).value || '',
          toDate: (pane.querySelector('[name="niSearchTo"]') || {}).value || ''
        };
      }
      function escNi(s) {
        return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
      }
      /** ziobiz/NOTI 노티거래내역과 동일 표기: CALLBACK/CALL→CALL, 미기재·- → CALL(Callback URL 기본) */
      function formatNiNotifyChannelDisplay(raw) {
        var s = String(raw == null ? '' : raw).trim();
        if (!s || s === '-') return 'CALL';
        var u = s.toUpperCase();
        if (u === 'CALLBACK' || u === 'CALL') return 'CALL';
        if (u === 'RESULT') return 'RESULT';
        if (u === 'BOTH') return 'BOTH';
        return s;
      }
      function renderNiTable(data) {
        var tbody = pane.querySelector('#hqNotifyInboundTbody');
        if (!tbody) return;
        var list = (data && data.list) ? data.list : [];
        if (list.length === 0) {
          tbody.innerHTML = '<tr><td colspan="13" class="text-center text-muted py-4">조회된 노티가 없습니다.</td></tr>';
        } else {
          tbody.innerHTML = list.map(function (r) {
            var ingLab = r.ingressDeliveryKindLabel != null ? String(r.ingressDeliveryKindLabel) : '미표시';
            return '<tr class="hq-ni-row" data-id="' + escNi(r.id) + '">' +
              '<td class="small">' + escNi(r.id) + '</td>' +
              '<td class="small text-nowrap">' + escNi(r.createdAt) + '</td>' +
              '<td class="small">' + escNi(formatNiNotifyChannelDisplay(r.notifyChannelType)) + '</td>' +
              '<td class="small font-monospace">' + escNi(r.notifyTargetCode) + '</td>' +
              '<td class="small font-monospace">' + escNi(r.mid) + '</td>' +
              '<td class="small text-center">' + escNi(r.rootNo) + '</td>' +
              '<td class="small font-monospace text-truncate" style="max-width:10rem" title="' + escNi(r.transactionId) + '">' + escNi(r.transactionId) + '</td>' +
              '<td class="small text-truncate" style="max-width:8rem" title="' + escNi(r.merchantId) + '">' + escNi(r.merchantId) + '</td>' +
              '<td class="small" style="white-space:normal;line-height:1.25">' +
              '<span class="text-nowrap">결제 ' + escNi(r.paymentStatusLabel != null ? r.paymentStatusLabel : r.processStatus) + '</span>' +
              '<br><span class="text-muted small text-nowrap">처리 ' + escNi(r.parseStatusLabel != null ? r.parseStatusLabel : '—') + '</span></td>' +
              '<td class="small text-nowrap text-center" title="' + escNi(r.ingressDeliveryKind || '') + '">' + escNi(ingLab) + '</td>' +
              '<td class="small hq-ni-td-error text-truncate" style="max-width:11rem" title="' + escNi(r.errorMessage) + '">' + escNi(r.errorMessage) + '</td>' +
              '<td class="small text-truncate" style="max-width:18rem" title="' + escNi(r.rawPreview) + '">' + escNi(r.rawPreview) + '</td>' +
              '<td class="text-center small"><button type="button" class="btn btn-sm btn-outline-primary hq-ni-detail" data-id="' + escNi(r.id) + '">본문</button></td></tr>';
          }).join('');
        }
        var tp = (data && data.totalPages) ? Math.max(1, data.totalPages) : 1;
        var te = (data && data.totalElements !== undefined) ? data.totalElements : 0;
        var curPg = (data && data.page != null) ? Math.max(1, parseInt(data.page, 10) || 1) : niCollectParams().page;
        var pcEl = pane.querySelector('#pageCnt');
        if (pcEl) pcEl.value = String(curPg);
        if (typeof window.updatePaging === 'function') {
          window.updatePaging(tabId, curPg, tp, te);
        }
      }
      function niLoad() {
        if (niDimm) niDimm.style.display = 'flex';
        window.PG_API.hqNotifyInboundList(niCollectParams()).then(function (data) {
          renderNiTable(data);
        }).catch(function (e) {
          alert(e && e.message ? e.message : '조회 실패');
        }).finally(function () { if (niDimm) niDimm.style.display = 'none'; });
      }
      pane._hqNotifyInboundReload = niLoad;
      if (!pane._hqNotifyInboundPagingChange) {
        pane._hqNotifyInboundPagingChange = true;
        pane.addEventListener('paging-change', function () {
          if (typeof pane._hqNotifyInboundReload === 'function') pane._hqNotifyInboundReload();
        });
      }
      var btnS = pane.querySelector('#hqNotifyInboundSearchBtn');
      if (btnS) btnS.addEventListener('click', function () {
        var pc0 = pane.querySelector('#pageCnt');
        if (pc0) pc0.value = '1';
        niLoad();
      });
      pane.addEventListener('click', function (e) {
        var b = e.target.closest && e.target.closest('.hq-ni-detail');
        if (!b || !pane.contains(b)) return;
        var id = b.getAttribute('data-id');
        if (!id) return;
        if (niDimm) niDimm.style.display = 'flex';
        window.PG_API.hqNotifyInboundDetail(id).then(function (d) {
          ensureHqNiDetailModalInBody();
          var ta = document.getElementById('hqNiDetailBody');
          var meta = document.getElementById('hqNiDetailMeta');
          var modalEl = document.getElementById('hqNiDetailModal');
          if (ta) ta.value = d && d.rawBody != null ? String(d.rawBody) : '';
          if (meta) {
            var metaParts = ['ID=' + (d && d.id != null ? d.id : id)];
            if (d && d.createdAt) metaParts.push('수신 ' + d.createdAt);
            if (d && d.notifyChannelType) metaParts.push(formatNiNotifyChannelDisplay(d.notifyChannelType));
            var payLab = d && (d.paymentStatusLabel != null ? d.paymentStatusLabel : d.processStatus);
            if (payLab && payLab !== '-') metaParts.push('결제 ' + payLab);
            if (d && d.paymentStatusRaw) metaParts.push('PaymentStatus=' + d.paymentStatusRaw);
            if (d && d.parseStatusLabel && d.parseStatusLabel !== '—') metaParts.push('처리 ' + d.parseStatusLabel);
            if (d && d.parseStatus && d.parseStatus !== '-') metaParts.push('처리코드 ' + d.parseStatus);
            if (d && d.clientIp) metaParts.push('IP ' + d.clientIp);
            if (d && d.ingressDeliveryKindLabel) metaParts.push('수신성격 ' + d.ingressDeliveryKindLabel);
            meta.textContent = metaParts.join(' · ');
          }
          if (modalEl && window.bootstrap && bootstrap.Modal) {
            bootstrap.Modal.getOrCreateInstance(modalEl).show();
          } else {
            alert(ta ? ta.value.slice(0, 4000) : '');
          }
        }).catch(function (err) {
          alert(err && err.message ? err.message : '상세 조회 실패');
        }).finally(function () { if (niDimm) niDimm.style.display = 'none'; });
      });
      var fromEl = pane.querySelector('[name="niSearchFrom"]');
      var toEl = pane.querySelector('[name="niSearchTo"]');
      if (toEl && !toEl.value) {
        toEl.value = new Date().toISOString().slice(0, 10);
      }
      if (fromEl && !fromEl.value) {
        var t0 = new Date();
        t0.setDate(t0.getDate() - 7);
        fromEl.value = t0.toISOString().slice(0, 10);
      }
      niLoad();
    }
    if (url === '/hq/settlementAdmin' && !pane._hqSettlementAdminBound) {
      pane._hqSettlementAdminBound = true;
      function escHqSt(s) { return pgEscapeHtml(s); }
      function escHqStAttr(s) { return pgEscapeAttr(s); }
      function resolveCount(map, code) {
        if (!map || code == null) return 0;
        var k = String(code).trim();
        if (map[k] != null) return Number(map[k]) || 0;
        var up = k.toUpperCase();
        for (var ck in map) {
          if (Object.prototype.hasOwnProperty.call(map, ck) && String(ck).toUpperCase() === up) {
            return Number(map[ck]) || 0;
          }
        }
        return 0;
      }
      function renderHqSettlementGrids(rows, counts) {
        var bt = pane.querySelector('#hqStBuiltTbody');
        var et = pane.querySelector('#hqStExtraTbody');
        if (!bt || !et) return;
        bt.innerHTML = '';
        et.innerHTML = '';
        (rows || []).forEach(function (r) {
          if (!r) return;
          var tr = document.createElement('tr');
          var cnt = resolveCount(counts, r.cycleCode);
          if (r.builtIn) {
            tr.innerHTML = '<td>' + escHqSt(r.cycleCode) + '</td><td>' + escHqSt(r.displayLabel) + '</td><td class="small">' + escHqSt(r.description) + '</td>' +
              '<td class="text-end">' + escHqSt(String(r.sortOrder != null ? r.sortOrder : '')) + '</td>' +
              '<td class="text-center">' + escHqSt(r.activeYn) + '</td><td class="text-end">' + cnt + '</td>';
            bt.appendChild(tr);
          } else if (r.fromDb && r.id != null) {
            var id = String(r.id);
            tr.setAttribute('data-row-id', id);
            tr.innerHTML = '<td>' + escHqSt(id) + '</td><td>' + escHqSt(r.cycleCode) + '</td>' +
              '<td><input type="text" class="form-control form-control-sm hq-st-inp" data-k="displayLabel" value="' + escHqStAttr(r.displayLabel) + '"></td>' +
              '<td><input type="text" class="form-control form-control-sm hq-st-inp" data-k="description" value="' + escHqStAttr(r.description) + '"></td>' +
              '<td style="width:4.5rem"><input type="number" class="form-control form-control-sm hq-st-inp" data-k="sortOrder" value="' + escHqStAttr(String(r.sortOrder != null ? r.sortOrder : 0)) + '"></td>' +
              '<td style="width:4rem"><select class="form-select form-select-sm hq-st-inp" data-k="activeYn">' +
              '<option value="Y"' + (String(r.activeYn).toUpperCase() !== 'N' ? ' selected' : '') + '>Y</option>' +
              '<option value="N"' + (String(r.activeYn).toUpperCase() === 'N' ? ' selected' : '') + '>N</option></select></td>' +
              '<td class="text-end text-nowrap"><button type="button" class="btn btn-sm btn-primary hq-st-row-save">저장</button> ' +
              '<button type="button" class="btn btn-sm btn-outline-danger hq-st-row-del">삭제</button></td>';
            et.appendChild(tr);
          }
        });
      }
      function loadHqSettlementAdmin() {
        Promise.all([
          window.PG_API.hqSettlementCycleDefs(),
          window.PG_API.hqSettlementMerchantAutoCounts()
        ]).then(function (arr) {
          renderHqSettlementGrids(arr[0] || [], arr[1] || {});
        }).catch(function (e) {
          alert(e && e.message ? e.message : '목록을 불러오지 못했습니다.');
        });
      }
      function loadHqSettlementSched() {
        var f = pane.querySelector('#hqStFrom');
        var t = pane.querySelector('#hqStTo');
        window.PG_API.hqSettlementSchedulePreview({ fromDate: f && f.value, toDate: t && t.value }).then(function (list) {
          var st = pane.querySelector('#hqStSchedTbody');
          if (!st) return;
          st.innerHTML = '';
          (list || []).forEach(function (r) {
            var tr = document.createElement('tr');
            tr.innerHTML = '<td>' + escHqSt(r.settleDate) + '</td><td>' + escHqSt(r.cycleCode) + '</td><td>' + escHqSt(r.periodFrom) + '</td><td>' + escHqSt(r.periodTo) + '</td>' +
              '<td class="text-end">' + (r.autoMerchantCount != null ? escHqSt(String(r.autoMerchantCount)) : '') + '</td>';
            st.appendChild(tr);
          });
        }).catch(function (e) {
          alert(e && e.message ? e.message : '일정 조회 실패');
        });
      }
      function famToggle() {
        var fam = pane.querySelector('#hqStAddFamily');
        var ow = pane.querySelector('#hqStOffsetWrap');
        var wk = pane.querySelector('#hqStWkWrap');
        if (!fam || !ow || !wk) return;
        var v = String(fam.value || '').toUpperCase();
        if (v === 'WK') {
          ow.classList.add('d-none');
          wk.classList.remove('d-none');
        } else {
          ow.classList.remove('d-none');
          wk.classList.add('d-none');
        }
      }
      var famEl = pane.querySelector('#hqStAddFamily');
      if (famEl) {
        famEl.addEventListener('change', famToggle);
        famToggle();
      }
      var addBtn = pane.querySelector('#hqStAddBtn');
      if (addBtn) {
        addBtn.addEventListener('click', function () {
          var family = pane.querySelector('#hqStAddFamily');
          var off = pane.querySelector('#hqStAddOffset');
          var wk = pane.querySelector('#hqStWkKey');
          var body = {
            family: family ? family.value : '',
            offset: off && off.value !== '' ? parseInt(off.value, 10) : null,
            wkKey: wk ? wk.value : '',
            displayLabel: (pane.querySelector('#hqStAddLabel') || {}).value || '',
            description: (pane.querySelector('#hqStAddDesc') || {}).value || '',
            sortOrder: (pane.querySelector('#hqStAddSort') || {}).value || '100',
            activeYn: (pane.querySelector('#hqStAddActive') || {}).value || 'Y'
          };
          window.PG_API.hqSettlementCycleDefCreate(body).then(function () {
            pgInvalidateCalcCycleOptionsCache();
            loadHqSettlementAdmin();
            alert('등록되었습니다.');
          }).catch(function (e) {
            alert(e && e.message ? e.message : '등록 실패');
          });
        });
      }
      var seedMissingBtn = pane.querySelector('#hqStSeedMissingBtn');
      if (seedMissingBtn) {
        seedMissingBtn.addEventListener('click', function () {
          if (!confirm('내장 표준 정산주기 코드 중 DB(tb_hq_settlement_cycle_def)에 없는 행만 추가합니다. 계속할까요?')) return;
          window.PG_API.hqSettlementCycleDefsSeedMissing().then(function (data) {
            var n = data && data.inserted != null ? Number(data.inserted) : 0;
            pgInvalidateCalcCycleOptionsCache();
            loadHqSettlementAdmin();
            alert('추가된 행 수: ' + n + ' (이미 있던 코드는 건너뜁니다)');
          }).catch(function (e) {
            alert(e && e.message ? e.message : '복원 실패');
          });
        });
      }
      var schedBtn = pane.querySelector('#hqStSchedBtn');
      if (schedBtn) schedBtn.addEventListener('click', function () { loadHqSettlementSched(); });
      var et = pane.querySelector('#hqStExtraTbody');
      if (et) {
        et.addEventListener('click', function (ev) {
          var btn = ev.target && ev.target.closest ? ev.target.closest('button') : null;
          if (!btn) return;
          var tr = btn.closest('tr');
          var rid = tr && tr.getAttribute('data-row-id');
          if (!rid) return;
          if (btn.classList.contains('hq-st-row-del')) {
            if (!confirm('삭제할까요?')) return;
            window.PG_API.hqSettlementCycleDefDelete(rid).then(function () {
              pgInvalidateCalcCycleOptionsCache();
              loadHqSettlementAdmin();
            }).catch(function (e) {
              alert(e && e.message ? e.message : '삭제 실패');
            });
          } else if (btn.classList.contains('hq-st-row-save')) {
            var payload = { displayLabel: '', description: '', sortOrder: null, activeYn: 'Y' };
            tr.querySelectorAll('.hq-st-inp').forEach(function (inp) {
              var k = inp.getAttribute('data-k');
              if (!k) return;
              if (k === 'sortOrder') payload.sortOrder = inp.value !== '' ? parseInt(inp.value, 10) : null;
              else if (k === 'activeYn') payload.activeYn = inp.value;
              else payload[k] = inp.value;
            });
            window.PG_API.hqSettlementCycleDefUpdate(rid, payload).then(function () {
              pgInvalidateCalcCycleOptionsCache();
              loadHqSettlementAdmin();
              alert('저장되었습니다.');
            }).catch(function (e) {
              alert(e && e.message ? e.message : '저장 실패');
            });
          }
        });
      }
      var f0 = pane.querySelector('#hqStFrom');
      var t0 = pane.querySelector('#hqStTo');
      if (t0 && !t0.value) t0.value = new Date().toISOString().slice(0, 10);
      if (f0 && !f0.value) {
        var x = new Date();
        x.setDate(x.getDate() - 7);
        f0.value = x.toISOString().slice(0, 10);
      }
      loadHqSettlementAdmin();
      loadHqSettlementSched();
      (function initMdCycleUi() {
        var sel = pane.querySelector('#hqMdCycleOrgSel');
        var tbody = pane.querySelector('#hqMdCycleTbody');
        var saveBtn = pane.querySelector('#hqMdCycleSaveBtn');
        if (!sel || !tbody || !saveBtn || !window.PG_API.hqMasterDistOrgOptions || !window.PG_API.hqSettlementCycleOptionsCatalog) return;
        var fullOpts = [];
        function slotSelectHtml(selectedVal) {
          var s = selectedVal ? String(selectedVal) : '';
          var selNorm = pgNormCalcCycleCode(s);
          var h = '<option value="">(미지정)</option>';
          var matched = false;
          (fullOpts || []).forEach(function (o) {
            var val = o.v != null ? String(o.v) : '';
            if (val === '') return;
            var lab = o.t != null ? String(o.t) : val;
            var isSel = selNorm !== '' && pgNormCalcCycleCode(val) === selNorm;
            if (isSel) matched = true;
            h += '<option value="' + escHqStAttr(val) + '"' + (isSel ? ' selected' : '') + '>' + escHqSt(lab) + '</option>';
          });
          if (selNorm !== '' && !matched) {
            h += '<option value="' + escHqStAttr(selNorm) + '" selected title="본사 정산주기 병합 목록에 없거나 비활성인 코드입니다. 저장값은 유지됩니다.">' +
              escHqSt(selNorm + ' (저장값)') + '</option>';
          }
          return h;
        }
        function renderRows(slots, defaultSlot) {
          tbody.innerHTML = '';
          var ds = defaultSlot != null ? Number(defaultSlot) : 0;
          if (!isFinite(ds) || ds < 0 || ds > 9) ds = 0;
          for (var i = 0; i < 10; i++) {
            var tr = document.createElement('tr');
            var slotVal = slots && slots[i] != null ? String(slots[i]) : '';
            tr.innerHTML = '<td class="text-center">' + (i + 1) + '</td><td><select class="form-select form-select-sm hq-md-slot" data-i="' + i + '">' + slotSelectHtml(slotVal) + '</select></td>' +
              '<td class="text-center"><input type="radio" class="form-check-input hq-md-def" name="hqMdDefaultSlot" value="' + i + '"' + (ds === i ? ' checked' : '') + '></td>';
            tbody.appendChild(tr);
          }
        }
        function loadConfig(orgId) {
          if (!orgId) {
            tbody.innerHTML = '';
            return;
          }
          window.PG_API.hqMasterDistCalcCycleConfigGet(orgId).then(function (data) {
            var slots = data.slots || [];
            while (slots.length < 10) slots.push(null);
            renderRows(slots, data.defaultSlot != null ? data.defaultSlot : 0);
          }).catch(function (e) { alert(e && e.message ? e.message : '조회 실패'); });
        }
        Promise.all([
          window.PG_API.hqMasterDistOrgOptions(),
          window.PG_API.hqSettlementCycleOptionsCatalog()
        ]).then(function (arr) {
          fullOpts = arr[1] || [];
          sel.innerHTML = '<option value="">총판을 선택하세요</option>';
          (arr[0] || []).forEach(function (r) {
            var o = document.createElement('option');
            o.value = String(r.orgUnitId);
            o.textContent = (r.compId || '') + ' · ' + (r.compNm || '');
            sel.appendChild(o);
          });
        }).catch(function (e) {
          sel.innerHTML = '<option value="">목록 로드 실패</option>';
          alert(e && e.message ? e.message : '총판 목록을 불러오지 못했습니다.');
        });
        sel.addEventListener('change', function () { loadConfig(this.value); });
        saveBtn.addEventListener('click', function () {
          var orgId = sel.value;
          if (!orgId) {
            alert('총판을 선택하세요.');
            return;
          }
          var slots = [];
          tbody.querySelectorAll('.hq-md-slot').forEach(function (s) { slots.push(s.value || ''); });
          var defEl = tbody.querySelector('input[name="hqMdDefaultSlot"]:checked');
          var defaultSlot = defEl ? parseInt(defEl.value, 10) : 0;
          window.PG_API.hqMasterDistCalcCycleConfigSave({ orgUnitId: parseInt(orgId, 10), slots: slots, defaultSlot: defaultSlot }).then(function () {
            pgInvalidateCalcCycleOptionsCache();
            loadConfig(orgId);
            alert('저장되었습니다.');
          }).catch(function (e) {
            alert(e && e.message ? e.message : '저장 실패');
          });
        });
      })();
    }
    if (url === '/hq/ledgerSysSettings') {
      var dimmLs = document.getElementById('dimm');
      function formatHqLedgerZonedClock(tz) {
        try {
          return new Intl.DateTimeFormat('ko-KR', {
            timeZone: tz,
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            hour12: false
          }).format(new Date());
        } catch (e) {
          return '—';
        }
      }
      function updateHqPayFollowClocks(pane) {
        var thEl = pane.querySelector('.hq-pay-follow-clock-th');
        var jpEl = pane.querySelector('.hq-pay-follow-clock-jp');
        var selEl = pane.querySelector('.hq-pay-follow-clock-sel');
        if (thEl) thEl.textContent = formatHqLedgerZonedClock('Asia/Bangkok');
        if (jpEl) jpEl.textContent = formatHqLedgerZonedClock('Asia/Tokyo');
        var zSel = pane.querySelector('[name="payFollowRefZone"]');
        var dTz = pane.querySelector('[name="displayTimezone"]');
        var z = (zSel && zSel.value) ? String(zSel.value).trim() : '';
        if (!z && dTz && dTz.value) z = String(dTz.value).trim();
        if (selEl) {
          selEl.textContent = z
            ? (formatHqLedgerZonedClock(z) + ' (' + z + ')')
            : '(전산 표준시와 동일 — ' + (dTz && dTz.value ? String(dTz.value) : 'Asia/Bangkok') + ')';
        }
      }
      function syncPayFollowLedgerUi(pane) {
        function ynOn(name) {
          var el = pane.querySelector('[name="' + name + '"]');
          return el && String(el.value) === 'Y';
        }
        function rowDis(useName, valNames, refName) {
          var on = ynOn(useName);
          (valNames || []).forEach(function (n) {
            var e = pane.querySelector('[name="' + n + '"]');
            if (e) e.disabled = !on;
          });
          if (refName) {
            var r = pane.querySelector('[name="' + refName + '"]');
            if (r) r.disabled = !on;
          }
        }
        rowDis('autoVoidYn', ['autoVoidStartTime', 'autoVoidEndTime'], 'autoVoidReflectSettlementYn');
        rowDis('emailVoidYn', ['emailVoidStartTime', 'emailVoidEndTime'], 'emailVoidReflectSettlementYn');
        rowDis('autoRefundYn', ['autoRefundAfterDays', 'autoRefundWindowStartTime'], 'autoRefundReflectSettlementYn');
        rowDis('forceRefundYn', ['forceRefundAfterDays'], 'forceRefundReflectSettlementYn');
        (function syncEmailVoidStartOverride() {
          var autoOn = ynOn('autoVoidYn');
          var emailOn = ynOn('emailVoidYn');
          var esInp = pane.querySelector('[name="emailVoidStartTime"]');
          var autoEndInp = pane.querySelector('[name="autoVoidEndTime"]');
          if (!esInp) return;
          if (emailOn && autoOn) {
            esInp.disabled = true;
            var endV = autoEndInp && String(autoEndInp.value || '').trim();
            esInp.title = endV
              ? '자동무효·이메일무효를 함께 켠 경우: 시작은 지정할 수 없고 자동무효 마감(' + endV + ') 다음 분부터입니다. 마감은 오른쪽 시간으로 설정합니다.'
              : '자동무효·이메일무효를 함께 켠 경우: 시작 입력은 비활성입니다. 마감은 오른쪽 시간으로 설정합니다.';
          } else if (emailOn) {
            esInp.disabled = false;
            esInp.title = '비우면 당일 0:00부터. 마감은 오른쪽 시간(비우면 23:59).';
          }
        })();
        updateHqPayFollowClocks(pane);
      }
      /** 소수 자릿수가 0이면 「잘리는 자리 처리」는 의미 없음 — 선택 비활성·DOWN 고정 표시 */
      function syncLedgerFeeDecimalRoundUi() {
        var glDp = pane.querySelector('[name="feeListDecimalPlaces"]');
        var glRm = pane.querySelector('[name="feeListRoundMode"]');
        if (glDp && glRm) {
          var gdp = parseInt(String(glDp.value != null ? glDp.value : '2'), 10);
          if (isNaN(gdp)) gdp = 2;
          if (gdp === 0) {
            glRm.disabled = true;
            glRm.value = 'DOWN';
            glRm.title = '소수 자릿수가 0이면 금액은 정수이며, 잘리는 자리 처리는 적용되지 않습니다.';
          } else {
            glRm.disabled = false;
            glRm.removeAttribute('title');
          }
        }
        pane.querySelectorAll('#hqFeeCurrencyFormatTbody tr[data-currency]').forEach(function (tr) {
          var dpSel = tr.querySelector('.hq-fcf-dp');
          var rmSel = tr.querySelector('.hq-fcf-rm');
          if (!dpSel || !rmSel) return;
          var editing = tr.getAttribute('data-hq-fcf-editing') === '1';
          if (!editing) {
            dpSel.disabled = true;
            rmSel.disabled = true;
            dpSel.title = '관리 열 [수정]으로 편집 모드 전환 후 변경할 수 있습니다.';
            rmSel.title = dpSel.title;
            return;
          }
          dpSel.disabled = false;
          dpSel.removeAttribute('title');
          var d = parseInt(String(dpSel.value != null ? dpSel.value : '0'), 10);
          if (isNaN(d)) d = 2;
          if (d === 0) {
            rmSel.disabled = true;
            rmSel.value = 'DOWN';
            rmSel.title = '소수 자릿수가 0이면 잘리는 자리 처리는 적용되지 않습니다.';
          } else {
            rmSel.disabled = false;
            rmSel.removeAttribute('title');
          }
        });
      }
      function fillLedgerSys(d) {
        if (!d) return;
        var keys = ['displayTimezone', 'ntpSyncEnabledYn', 'ntpServerList', 'timeSyncIntervalMin', 'serverTimeIso', 'serverZoneId',
          'smtpHost', 'smtpPort', 'smtpTlsYn', 'smtpAuthYn', 'smtpUsername', 'mailFromAddress', 'mailFromName', 'alertRecipientEmails',
          'feeListDecimalPlaces', 'feeListRoundMode',
          'payDisplayCurrencyIsoNum', 'payDisplayCurrencyCode',
          'emailVoidTo', 'emailVoidSubject', 'emailVoidBodyTemplate', 'emailVoidCompanyName', 'emailVoidContactName',
          'emailOnSyncFailureYn', 'emailDailyDigestYn', 'emailNotifyVoidBatchYn', 'emailNotifyRefundBatchYn', 'memo', 'updatedAt',
          'autoVoidYn', 'emailVoidYn', 'autoRefundYn', 'forceRefundYn',
          'autoVoidStartTime', 'autoVoidEndTime', 'emailVoidStartTime', 'emailVoidEndTime',
          'payFollowRefZone', 'autoRefundAfterDays', 'autoRefundWindowStartTime', 'forceRefundAfterDays',
          'autoVoidReflectSettlementYn', 'emailVoidReflectSettlementYn', 'autoRefundReflectSettlementYn', 'forceRefundReflectSettlementYn',
          'helloTimelineEnabledYn', 'helloTimelineDurationMin'];
        keys.forEach(function (k) {
          var el = pane.querySelector('[name="' + k + '"]');
          if (!el) return;
          var raw = d[k];
          if (raw == null || raw === undefined || String(raw).trim() === '') {
            if (k.indexOf('ReflectSettlement') !== -1) {
              el.value = 'N';
            } else if (el.tagName === 'SELECT' && ['payFollowRefZone', 'autoRefundAfterDays', 'forceRefundAfterDays'].indexOf(k) !== -1) {
              el.value = '';
            } else if (el.type === 'time') {
              el.value = '';
            }
            return;
          }
          el.value = String(raw);
        });
        (function ensureLedgerTzOption() {
          var el = pane.querySelector('[name="displayTimezone"]');
          if (!el || el.tagName !== 'SELECT' || !d.displayTimezone) return;
          var v = String(d.displayTimezone).trim();
          if (!v) return;
          var found = false;
          for (var i = 0; i < el.options.length; i++) {
            if (el.options[i].value === v) { found = true; break; }
          }
          if (!found) {
            var o = document.createElement('option');
            o.value = v;
            o.textContent = v + ' (저장값)';
            el.appendChild(o);
          }
          el.value = v;
        })();
        ['autoRefundAfterDays', 'forceRefundAfterDays'].forEach(function (name) {
          var el = pane.querySelector('[name="' + name + '"]');
          if (!el || el.tagName !== 'SELECT') return;
          var raw = d[name];
          if (raw == null || raw === undefined || String(raw).trim() === '') {
            el.value = '';
            return;
          }
          var v = String(raw).trim();
          var found = false;
          for (var i = 0; i < el.options.length; i++) {
            if (el.options[i].value === v) { found = true; break; }
          }
          if (!found) {
            var o = document.createElement('option');
            o.value = v;
            o.textContent = v + '일 (저장값)';
            el.appendChild(o);
          }
          el.value = v;
        });
        (function ensurePayFollowRefZone() {
          var el = pane.querySelector('[name="payFollowRefZone"]');
          if (!el || el.tagName !== 'SELECT') return;
          var saved = d.payFollowRefZone != null ? String(d.payFollowRefZone).trim() : '';
          if (!saved) {
            el.value = '';
            return;
          }
          var found = false;
          for (var i = 0; i < el.options.length; i++) {
            if (el.options[i].value === saved) { found = true; break; }
          }
          if (!found) {
            var o = document.createElement('option');
            o.value = saved;
            o.textContent = saved + ' (저장값)';
            el.appendChild(o);
          }
          el.value = saved;
        })();
        var spw = pane.querySelector('[name="smtpPassword"]');
        if (spw) spw.value = '';
        var lab = pane.querySelector('[name="smtpPasswordSetLabel"]');
        if (lab) lab.value = d.smtpPasswordSet ? '저장됨' : '미설정';
        renderHqDataRetentionRows(d.dataRetentionRows);
        renderHqFeeCurrencyRows(d.feeCurrencyFormats);
        renderHqPayDisplayCurrencyCatalog(d.payDisplayCurrencyCatalog, d.payDisplayCurrencyIsoNum);
        syncPayFollowLedgerUi(pane);
        syncHelloTimelineLedgerFields();
      }
      function syncHelloTimelineLedgerFields() {
        var en = pane.querySelector('[name="helloTimelineEnabledYn"]');
        var dm = pane.querySelector('[name="helloTimelineDurationMin"]');
        if (!en || !dm) return;
        var on = String(en.value || '').toUpperCase() === 'Y';
        dm.disabled = !on;
      }
      function renderHqPayDisplayCurrencyCatalog(rows, currentIsoNum) {
        var tb = pane.querySelector('#hqPayDisplayCurrencyCatalogTbody');
        if (!tb) return;
        tb.replaceChildren();
        var cur = currentIsoNum != null ? String(currentIsoNum).trim() : '';
        if (!rows || !rows.length) {
          var trE = document.createElement('tr');
          var tdE = document.createElement('td');
          tdE.colSpan = 3;
          tdE.className = 'text-center text-muted py-3';
          tdE.textContent = '목록이 없습니다.';
          trE.appendChild(tdE);
          tb.appendChild(trE);
          return;
        }
        rows.forEach(function (r) {
          var num = String(r.isoNum != null ? r.isoNum : '').trim();
          var alpha = String(r.alpha != null ? r.alpha : '').trim().toUpperCase();
          var tr = document.createElement('tr');
          if (cur && num === cur) tr.classList.add('table-primary');
          var td1 = document.createElement('td');
          td1.className = 'font-monospace';
          td1.textContent = num;
          var td2 = document.createElement('td');
          td2.className = 'fw-semibold';
          td2.textContent = alpha;
          var td3 = document.createElement('td');
          td3.className = 'text-center small';
          td3.textContent = (cur && num === cur) ? '현재' : '—';
          tr.appendChild(td1);
          tr.appendChild(td2);
          tr.appendChild(td3);
          tb.appendChild(tr);
        });
        if (window.PG_TABLE_COL_RESIZE && typeof window.PG_TABLE_COL_RESIZE.refreshIn === 'function') {
          window.PG_TABLE_COL_RESIZE.refreshIn(pane);
        }
      }
      function renderHqFeeCurrencyRows(rows) {
        var tb = pane.querySelector('#hqFeeCurrencyFormatTbody');
        if (!tb) return;
        tb.replaceChildren();
        var rmOpts = ['CEILING', 'HALF_UP', 'DOWN'];
        var rmLabels = { CEILING: '절상', HALF_UP: '반올림', DOWN: '그대로(버림)' };
        if (!rows || !rows.length) {
          var trE = document.createElement('tr');
          var tdE = document.createElement('td');
          tdE.colSpan = 4;
          tdE.className = 'text-center text-muted py-3';
          tdE.textContent = '통화별 설정이 없습니다.';
          trE.appendChild(tdE);
          tb.appendChild(trE);
          syncLedgerFeeDecimalRoundUi();
          return;
        }
        rows.forEach(function (r) {
          var cur = String(r.currency || '').trim().toUpperCase();
          var tr = document.createElement('tr');
          tr.setAttribute('data-currency', cur);
          tr.setAttribute('data-hq-fcf-editing', '0');
          var td1 = document.createElement('td');
          td1.className = 'fw-semibold';
          td1.textContent = cur;
          var td2 = document.createElement('td');
          td2.className = 'text-center';
          var selDp = document.createElement('select');
          selDp.className = 'form-select form-select-sm hq-fcf-dp';
          var dp0 = parseInt(String(r.decimalPlaces != null ? r.decimalPlaces : '2'), 10);
          if (isNaN(dp0) || dp0 < 0) dp0 = 2;
          if (dp0 > 8) dp0 = 8;
          for (var d = 0; d <= 8; d++) {
            var o = document.createElement('option');
            o.value = String(d);
            o.textContent = String(d);
            if (d === dp0) o.selected = true;
            selDp.appendChild(o);
          }
          td2.appendChild(selDp);
          var td3 = document.createElement('td');
          var selRm = document.createElement('select');
          selRm.className = 'form-select form-select-sm hq-fcf-rm';
          var rmSaved = String(r.roundMode || 'CEILING').trim().toUpperCase();
          rmOpts.forEach(function (rm) {
            var o2 = document.createElement('option');
            o2.value = rm;
            o2.textContent = rmLabels[rm] || rm;
            if (rm === rmSaved) o2.selected = true;
            selRm.appendChild(o2);
          });
          td3.appendChild(selRm);
          var td4 = document.createElement('td');
          td4.className = 'text-center align-middle hq-fcf-act-cell';
          var wrap = document.createElement('div');
          wrap.className = 'd-flex flex-wrap gap-1 justify-content-center';
          function addActBtn(cls, label, title) {
            var b = document.createElement('button');
            b.type = 'button';
            b.className = 'btn btn-sm ' + cls;
            b.textContent = label;
            if (title) b.title = title;
            wrap.appendChild(b);
            return b;
          }
          addActBtn('btn-outline-primary hq-fcf-row-edit', '수정', '편집 모드(소수·잘리는 자리) 전환 — 연속 확인 후 활성화됩니다.');
          addActBtn('btn-primary hq-fcf-row-save', '저장', '전산설정 전체를 서버에 저장합니다. 수수료·정산 통화 형식이 즉시 반영될 수 있습니다.');
          addActBtn('btn-outline-secondary hq-fcf-row-cancel', '취소', '이 통화 행의 미저장 편집만 되돌리고 잠급니다.');
          addActBtn('btn-outline-secondary hq-fcf-copy-global', '전역값', '편집 모드에서만 사용 가능. 기본(통화 미지정) 소수·처리를 이 통화에 복사합니다.');
          td4.appendChild(wrap);
          tr.appendChild(td1);
          tr.appendChild(td2);
          tr.appendChild(td3);
          tr.appendChild(td4);
          tb.appendChild(tr);
        });
        syncLedgerFeeDecimalRoundUi();
      }
      function collectFeeCurrencyFormatRows() {
        var arr = [];
        pane.querySelectorAll('#hqFeeCurrencyFormatTbody tr[data-currency]').forEach(function (tr) {
          var cur = tr.getAttribute('data-currency');
          if (!cur) return;
          var dpSel = tr.querySelector('.hq-fcf-dp');
          var rmSel = tr.querySelector('.hq-fcf-rm');
          var dp = parseInt(String(dpSel && dpSel.value != null ? dpSel.value : '2'), 10);
          if (isNaN(dp) || dp < 0) dp = 0;
          if (dp > 8) dp = 8;
          var rm = dp === 0 ? 'DOWN' : (rmSel && rmSel.value ? String(rmSel.value).trim().toUpperCase() : 'CEILING');
          arr.push({ currency: cur, decimalPlaces: dp, roundMode: rm });
        });
        return arr;
      }
      function collectDataRetentionPolicyObj(omitId) {
        var policyObj = {};
        pane.querySelectorAll('#hqDataRetentionTbody tr[data-retention-id]').forEach(function (tr) {
          var rid = tr.getAttribute('data-retention-id');
          if (!rid || (omitId && rid === omitId)) return;
          var sched = tr.getAttribute('data-scheduler') === '1';
          var retainInp = tr.querySelector('.hq-dr-retain');
          var autoCb = tr.querySelector('.hq-dr-auto');
          var purgeInp = tr.querySelector('.hq-dr-purge');
          var rv = parseInt(String(retainInp && retainInp.value || '').trim(), 10);
          if (isNaN(rv) || rv < 1) return;
          if (sched) {
            var autoOn = autoCb && autoCb.checked;
            if (autoOn) {
              var pv = parseInt(String(purgeInp && purgeInp.value || '').trim(), 10);
              if (isNaN(pv) || pv < 1) pv = rv;
              policyObj[rid] = { retain: rv, auto: true, purge: pv };
            } else {
              policyObj[rid] = { retain: rv, auto: false };
            }
          } else {
            policyObj[rid] = rv;
          }
        });
        return policyObj;
      }
      function collectLedgerSysFd(omitId) {
        var fd = {};
        ['displayTimezone', 'ntpSyncEnabledYn', 'ntpServerList', 'timeSyncIntervalMin', 'smtpHost', 'smtpPort', 'smtpTlsYn', 'smtpAuthYn',
          'smtpUsername', 'smtpPassword', 'mailFromAddress', 'mailFromName', 'alertRecipientEmails',
          'feeListDecimalPlaces', 'feeListRoundMode',
          'emailVoidTo', 'emailVoidSubject', 'emailVoidBodyTemplate', 'emailVoidCompanyName', 'emailVoidContactName',
          'emailOnSyncFailureYn', 'emailDailyDigestYn', 'emailNotifyVoidBatchYn', 'emailNotifyRefundBatchYn', 'memo',
          'autoVoidYn', 'emailVoidYn', 'autoRefundYn', 'forceRefundYn',
          'autoVoidStartTime', 'autoVoidEndTime', 'emailVoidStartTime', 'emailVoidEndTime',
          'payFollowRefZone', 'autoRefundAfterDays', 'autoRefundWindowStartTime', 'forceRefundAfterDays',
          'autoVoidReflectSettlementYn', 'emailVoidReflectSettlementYn', 'autoRefundReflectSettlementYn', 'forceRefundReflectSettlementYn',
          'helloTimelineEnabledYn', 'helloTimelineDurationMin'].forEach(function (k) {
          var el = pane.querySelector('[name="' + k + '"]');
          if (el) fd[k] = el.value;
        });
        (function omitDisabledEmailVoidStart() {
          var esInp = pane.querySelector('[name="emailVoidStartTime"]');
          if (esInp && esInp.disabled) delete fd.emailVoidStartTime;
        })();
        (function omitDisabledRefundWindowStart() {
          var w = pane.querySelector('[name="autoRefundWindowStartTime"]');
          if (w && w.disabled) delete fd.autoRefundWindowStartTime;
        })();
        if (!fd.smtpPassword || !String(fd.smtpPassword).trim()) {
          delete fd.smtpPassword;
        }
        (function omitGlobalFeeRoundWhenZeroDp() {
          var glDp = pane.querySelector('[name="feeListDecimalPlaces"]');
          if (!glDp) return;
          var gdp = parseInt(String(glDp.value != null ? glDp.value : '2'), 10);
          if (!isNaN(gdp) && gdp === 0) delete fd.feeListRoundMode;
        })();
        fd.dataRetentionPolicyJson = JSON.stringify(collectDataRetentionPolicyObj(omitId));
        fd.feeCurrencyFormatJson = JSON.stringify(collectFeeCurrencyFormatRows());
        return fd;
      }
      function applyLedgerHelloTimelineClientAfterResponse(d) {
        if (typeof pgHelloTimelineInvalidateConfigCache === 'function') pgHelloTimelineInvalidateConfigCache();
        if (d) {
          var ynH = String(d.helloTimelineEnabledYn || 'N').toUpperCase() === 'Y' ? 'Y' : 'N';
          var dmH = parseInt(String(d.helloTimelineDurationMin != null ? d.helloTimelineDurationMin : '10'), 10);
          if (isNaN(dmH) || dmH < 1) dmH = 10;
          if (dmH > 1440) dmH = 1440;
          window._pgHelloTimelineFromLedger = { helloTimelineEnabledYn: ynH, helloTimelineDurationMin: dmH };
        }
        if (d && String(d.helloTimelineEnabledYn || 'N').toUpperCase() !== 'Y') {
          if (typeof pgHelloTimelineSetUntilMs === 'function') pgHelloTimelineSetUntilMs(0);
          document.querySelectorAll('.tab-pane').forEach(function (p) {
            if (!p.querySelector || !p.querySelector('[id^="viewSettingHelloBtn_"]')) return;
            p._viewSettingHelloHidden = false;
            if (typeof pgHelloZonesSetHidden === 'function') pgHelloZonesSetHidden(p, false);
            if (typeof pgHelloSyncButtonForPane === 'function') pgHelloSyncButtonForPane(p, false);
          });
          try {
            if (window.PG_TABLE_COL_RESIZE && typeof window.PG_TABLE_COL_RESIZE.refreshIn === 'function') {
              document.querySelectorAll('.tab-pane').forEach(function (p) { window.PG_TABLE_COL_RESIZE.refreshIn(p); });
            }
          } catch (eHl) { /* ignore */ }
        } else if (d && String(d.helloTimelineEnabledYn || 'N').toUpperCase() === 'Y') {
          var uSv = typeof pgHelloTimelineUntilMs === 'function' ? pgHelloTimelineUntilMs() : 0;
          if (typeof pgHelloApplyTimelineToAllPanes === 'function') {
            pgHelloApplyTimelineToAllPanes(!(uSv > Date.now()));
          }
        }
      }
      function postLedgerSysSave(fd) {
        if (dimmLs) dimmLs.style.display = 'flex';
        window.PG_API.hqLedgerSysSettingsSave(fd).then(function (d) {
          fillLedgerSys(d);
          applyLedgerHelloTimelineClientAfterResponse(d);
          alert('저장되었습니다.');
        }).catch(function (e) {
          alert(e && e.message ? e.message : '저장 실패');
        }).finally(function () { if (dimmLs) dimmLs.style.display = 'none'; });
      }
      /** 데이터 보관 표: 줄바꿈·다중 공백을 한 칸으로 정리. 긴 내용은 CSS로 최대 2줄 표시 */
      function hqDataRetentionSingleLine(s) {
        if (s == null || s === undefined) return '';
        return String(s).replace(/\r\n|\r|\n/g, ' ').replace(/\s+/g, ' ').trim();
      }
      function renderHqDataRetentionRows(rows) {
        var tb = pane.querySelector('#hqDataRetentionTbody');
        if (!tb) return;
        tb.replaceChildren();
        if (!rows || !rows.length) {
          var tr0 = document.createElement('tr');
          var td0 = document.createElement('td');
          td0.colSpan = 6;
          td0.className = 'text-center text-muted py-3';
          td0.textContent = '보관 설정 항목이 없습니다.';
          tr0.appendChild(td0);
          tb.appendChild(tr0);
          return;
        }
        rows.forEach(function (r) {
          var sched = !!r.schedulerApplied;
          var retain = r.retainDays != null ? r.retainDays : r.days;
          var purge = r.purgeDays != null ? r.purgeDays : retain;
          var autoOn = !!r.autoDeleteEnabled;
          var tr = document.createElement('tr');
          tr.className = 'hq-dr-data-row';
          tr.setAttribute('data-retention-id', r.id || '');
          tr.setAttribute('data-scheduler', sched ? '1' : '0');
          var labelOne = hqDataRetentionSingleLine(r.label || r.id || '');
          var descOne = hqDataRetentionSingleLine(r.description || '');
          var td1 = document.createElement('td');
          td1.className = 'fw-semibold hq-dr-type-cell';
          var wrapType = document.createElement('div');
          wrapType.className = 'hq-dr-cell-clamp-2';
          var spanLbl = document.createElement('span');
          spanLbl.className = 'hq-dr-label-text';
          spanLbl.textContent = labelOne;
          wrapType.appendChild(spanLbl);
          wrapType.appendChild(document.createTextNode(' '));
          var badge = document.createElement('span');
          badge.className = 'badge ms-1 flex-shrink-0 ' + (sched ? 'bg-info text-dark' : 'bg-secondary');
          badge.textContent = sched ? '스케줄' : '정책';
          wrapType.appendChild(badge);
          td1.appendChild(wrapType);
          td1.setAttribute('title', labelOne + ' (' + (sched ? '스케줄' : '정책') + ')');
          var tdAuto = document.createElement('td');
          tdAuto.className = 'text-center';
          var cb = document.createElement('input');
          cb.type = 'checkbox';
          cb.className = 'form-check-input hq-dr-auto';
          cb.checked = sched && autoOn;
          cb.disabled = !sched;
          cb.title = sched ? '자동삭제(스케줄)' : '스케줄 미연동';
          tdAuto.appendChild(cb);
          var tdPurge = document.createElement('td');
          var inpPurge = document.createElement('input');
          inpPurge.type = 'number';
          inpPurge.className = 'form-control form-control-sm hq-dr-purge';
          inpPurge.setAttribute('min', '1');
          inpPurge.setAttribute('max', '36500');
          inpPurge.value = purge != null ? String(purge) : '';
          inpPurge.disabled = !sched || !cb.checked;
          tdPurge.appendChild(inpPurge);
          var tdRetain = document.createElement('td');
          var inpRetain = document.createElement('input');
          inpRetain.type = 'number';
          inpRetain.className = 'form-control form-control-sm hq-dr-retain';
          inpRetain.setAttribute('min', '1');
          inpRetain.setAttribute('max', '36500');
          inpRetain.value = retain != null ? String(retain) : '';
          tdRetain.appendChild(inpRetain);
          var tdDesc = document.createElement('td');
          tdDesc.className = 'small text-muted hq-dr-desc-cell';
          var spanDesc = document.createElement('span');
          spanDesc.className = 'hq-dr-cell-clamp-2';
          spanDesc.textContent = descOne;
          tdDesc.appendChild(spanDesc);
          if (descOne) tdDesc.setAttribute('title', descOne);
          var tdAct = document.createElement('td');
          tdAct.className = 'text-center align-middle hq-dr-act-cell';
          var g = document.createElement('div');
          g.className = 'd-flex flex-wrap gap-1 justify-content-center hq-dr-act-btns';
          var bSave = document.createElement('button');
          bSave.type = 'button';
          bSave.className = 'btn btn-sm btn-outline-primary hq-dr-row-save';
          bSave.setAttribute('data-id', r.id || '');
          bSave.textContent = '저장';
          var bRev = document.createElement('button');
          bRev.type = 'button';
          bRev.className = 'btn btn-sm btn-outline-secondary hq-dr-row-revert';
          bRev.setAttribute('data-id', r.id || '');
          bRev.textContent = '수정';
          var bRst = document.createElement('button');
          bRst.type = 'button';
          bRst.className = 'btn btn-sm btn-outline-danger hq-dr-row-reset';
          bRst.setAttribute('data-id', r.id || '');
          bRst.textContent = '초기화';
          g.appendChild(bSave);
          g.appendChild(bRev);
          g.appendChild(bRst);
          tdAct.appendChild(g);
          tr.appendChild(td1);
          tr.appendChild(tdAuto);
          tr.appendChild(tdPurge);
          tr.appendChild(tdRetain);
          tr.appendChild(tdDesc);
          tr.appendChild(tdAct);
          tb.appendChild(tr);
        });
      }
      function reloadLedgerSys() {
        if (dimmLs) dimmLs.style.display = 'flex';
        window.PG_API.hqLedgerSysSettings().then(fillLedgerSys).catch(function () {}).finally(function () { if (dimmLs) dimmLs.style.display = 'none'; });
      }
      pane._pgFillLedgerSys = fillLedgerSys;
      pane._pgCollectLedgerSysFd = collectLedgerSysFd;
      pane._pgPostLedgerSysSave = postLedgerSysSave;
      if (!pane._hqLedgerPaneInit) {
        pane._hqLedgerPaneInit = true;
        pane.addEventListener('click', function (e) {
          if (!pane.contains(e.target)) return;
          var dc = window.pgDoubleConfirm;
          if (e.target.closest('#hqLedgerSysSettingsSaveBtn')) {
            e.preventDefault();
            if (typeof dc !== 'function' || !dc('전산설정을 저장하시겠습니까?', '입력한 내용이 서버에 반영됩니다. 계속하시겠습니까?')) return;
            pane._pgPostLedgerSysSave(pane._pgCollectLedgerSysFd(null));
            return;
          }
          if (e.target.closest('#hqLedgerHelloTimelineSaveBtn')) {
            e.preventDefault();
            if (typeof dc !== 'function' || !dc('헬로 타임라인 설정을 저장하시겠습니까?', '사용 여부·유지 시간(분)만 서버에 반영됩니다. 계속하시겠습니까?')) return;
            var enHt = pane.querySelector('[name="helloTimelineEnabledYn"]');
            var dmHt = pane.querySelector('[name="helloTimelineDurationMin"]');
            if (!enHt || !window.PG_API || !window.PG_API.hqLedgerSysSettingsSaveHelloTimeline) {
              alert('헬로 타임라인 저장을 사용할 수 없습니다.');
              return;
            }
            var payloadHt = { helloTimelineEnabledYn: enHt.value };
            if (dmHt) payloadHt.helloTimelineDurationMin = dmHt.value;
            if (dimmLs) dimmLs.style.display = 'flex';
            window.PG_API.hqLedgerSysSettingsSaveHelloTimeline(payloadHt).then(function (d) {
              fillLedgerSys(d);
              syncHelloTimelineLedgerFields();
              applyLedgerHelloTimelineClientAfterResponse(d);
              alert('헬로 타임라인이 저장되었습니다.');
            }).catch(function (err) {
              alert(err && err.message ? err.message : '저장 실패');
            }).finally(function () { if (dimmLs) dimmLs.style.display = 'none'; });
            return;
          }
          if (e.target.closest('#hqLedgerHelloTimelineReloadBtn')) {
            e.preventDefault();
            if (typeof dc !== 'function' || !dc('헬로 타임라인을 서버 저장값으로 되돌릴까요?', '이 항목만 서버에서 다시 읽어 옵니다. 다른 입력란은 그대로입니다. 계속하시겠습니까?')) return;
            if (dimmLs) dimmLs.style.display = 'flex';
            window.PG_API.hqLedgerSysSettings().then(function (d) {
              if (!d) return;
              ['helloTimelineEnabledYn', 'helloTimelineDurationMin'].forEach(function (k) {
                var el = pane.querySelector('[name="' + k + '"]');
                if (!el) return;
                var raw = d[k];
                if (raw == null || raw === undefined || String(raw).trim() === '') {
                  if (k === 'helloTimelineEnabledYn') el.value = 'N';
                  return;
                }
                el.value = String(raw);
              });
              syncHelloTimelineLedgerFields();
            }).catch(function () {}).finally(function () { if (dimmLs) dimmLs.style.display = 'none'; });
            return;
          }
          if (e.target.closest('.hq-fcf-row-edit')) {
            var trEd = e.target.closest('tr[data-currency]');
            if (!trEd) return;
            if (trEd.getAttribute('data-hq-fcf-editing') === '1') return;
            var curEd = trEd.getAttribute('data-currency') || '';
            if (typeof dc !== 'function' || !dc(
              '통화 ' + curEd + ' — 수수료·정산 형식을 수정하시겠습니까?',
              '소수 자릿수·잘리는 자리 처리는 수수료내역·정산 목록 API에 직접 반영됩니다. 편집 모드로 전환할까요?')) return;
            var sd0 = trEd.querySelector('.hq-fcf-dp');
            var sr0 = trEd.querySelector('.hq-fcf-rm');
            if (sd0 && sr0) trEd._hqFcfSnap = { dp: String(sd0.value), rm: String(sr0.value) };
            trEd.setAttribute('data-hq-fcf-editing', '1');
            syncLedgerFeeDecimalRoundUi();
            return;
          }
          if (e.target.closest('.hq-fcf-row-save')) {
            if (typeof dc !== 'function' || !dc(
              '전산설정을 서버에 저장하시겠습니까? (수수료·정산 통화 형식 포함)',
              '통화별 소수·라운딩을 포함해 화면의 전산설정 전체가 기록됩니다. 저장 즉시 목록 API·정산 표시에 영향을 줄 수 있습니다. 정말 저장하시겠습니까?')) return;
            pane._pgPostLedgerSysSave(pane._pgCollectLedgerSysFd(null));
            return;
          }
          if (e.target.closest('.hq-fcf-row-cancel')) {
            var trCx = e.target.closest('tr[data-currency]');
            if (!trCx) return;
            if (trCx.getAttribute('data-hq-fcf-editing') !== '1') return;
            if (typeof dc !== 'function' || !dc(
              '이 통화 행 편집을 취소하시겠습니까?',
              '이 행에서 저장하지 않은 변경만 되돌립니다. 다른 입력란은 그대로입니다. 계속하시겠습니까?')) return;
            var snapCx = trCx._hqFcfSnap;
            var sdCx = trCx.querySelector('.hq-fcf-dp');
            var srCx = trCx.querySelector('.hq-fcf-rm');
            if (snapCx && sdCx && srCx) {
              sdCx.value = snapCx.dp;
              srCx.value = snapCx.rm;
            }
            trCx.setAttribute('data-hq-fcf-editing', '0');
            delete trCx._hqFcfSnap;
            syncLedgerFeeDecimalRoundUi();
            return;
          }
          if (e.target.closest('.hq-fcf-copy-global')) {
            var trF = e.target.closest('tr[data-currency]');
            if (!trF) return;
            if (trF.getAttribute('data-hq-fcf-editing') !== '1') {
              alert('먼저 해당 통화 행의 [수정]을 눌러 편집 모드로 전환한 뒤 [전역값]을 사용할 수 있습니다.');
              return;
            }
            if (typeof dc !== 'function' || !dc(
              '전역 기본값을 이 통화에 복사하시겠습니까?',
              '위쪽 「기본(통화 미지정)」의 소수 자릿수·잘리는 자리 처리로 이 행을 덮어씁니다. 서버 반영은 [저장]이 필요합니다. 계속하시겠습니까?')) return;
            var glDp = pane.querySelector('[name="feeListDecimalPlaces"]');
            var glRm = pane.querySelector('[name="feeListRoundMode"]');
            var dpSelF = trF.querySelector('.hq-fcf-dp');
            var rmSelF = trF.querySelector('.hq-fcf-rm');
            if (glDp && dpSelF) dpSelF.value = String(glDp.value || '2');
            if (glRm && rmSelF) rmSelF.value = String(glRm.value || 'CEILING');
            syncLedgerFeeDecimalRoundUi();
            return;
          }
          if (e.target.closest('.hq-dr-row-save')) {
            if (typeof dc !== 'function' || !dc('보관 정책을 저장하시겠습니까?', '표에 입력한 전체 보관 값이 함께 저장됩니다. 계속하시겠습니까?')) return;
            pane._pgPostLedgerSysSave(pane._pgCollectLedgerSysFd(null));
            return;
          }
          if (e.target.closest('.hq-dr-row-revert')) {
            if (typeof dc !== 'function' || !dc('서버에 저장된 값으로 다시 불러오시겠습니까?', '저장하지 않은 변경이 사라집니다. 계속하시겠습니까?')) return;
            if (dimmLs) dimmLs.style.display = 'flex';
            window.PG_API.hqLedgerSysSettings().then(pane._pgFillLedgerSys).catch(function () {}).finally(function () { if (dimmLs) dimmLs.style.display = 'none'; });
            return;
          }
          var rowRst = e.target.closest('.hq-dr-row-reset');
          if (rowRst) {
            var rid = rowRst.getAttribute('data-id');
            if (!rid) return;
            if (typeof dc !== 'function' || !dc('이 데이터 유형의 저장된 보관 설정을 초기화하시겠습니까?', '해당 유형의 덮어쓰기만 제거되고 기본값이 적용됩니다. 계속하시겠습니까?')) return;
            pane._pgPostLedgerSysSave(pane._pgCollectLedgerSysFd(rid));
            return;
          }
          if (e.target.closest('#hqLedgerOperationalDataResetBtn')) {
            if (typeof dc !== 'function') return;
            if (!dc(
              '데이터 초기화가 됩니다.\n\n전체 데이터를 초기화합니다.\n단, 등록된 업체 정보는 유지됩니다.\n\n[확인]으로 다음 안내로 진행하고, [취소]로 중단합니다.',
              '마지막 확인입니다.\n\n확인을 누르면 서버에서 운영 데이터가 삭제됩니다. 복구할 수 없습니다.\n취소를 누르면 아무 작업도 하지 않습니다.\n\n정말 전체 데이터 초기화를 실행하시겠습니까?')) return;
            if (dimmLs) dimmLs.style.display = 'flex';
            window.PG_API.hqLedgerSysSettingsResetOperationalData().then(function () {
              alert('운영 데이터 초기화가 완료되었습니다.');
              reloadLedgerSys();
            }).catch(function (err) {
              alert(err && err.message ? err.message : '초기화에 실패했습니다.');
            }).finally(function () { if (dimmLs) dimmLs.style.display = 'none'; });
            return;
          }
        });
        function hqLedgerPayFollowSyncIfNeeded(target) {
          var t = target;
          if (!t || !t.name || !pane.contains(t)) return;
          var payFollowSyncNames = ['autoVoidYn', 'emailVoidYn', 'autoRefundYn', 'forceRefundYn', 'payFollowRefZone', 'displayTimezone',
            'autoVoidStartTime', 'autoVoidEndTime', 'emailVoidEndTime', 'autoRefundWindowStartTime'];
          if (payFollowSyncNames.indexOf(t.name) !== -1) syncPayFollowLedgerUi(pane);
        }
        pane.addEventListener('change', function (e) {
          var cb = e.target.closest && e.target.closest('.hq-dr-auto');
          if (cb && pane.contains(cb)) {
            var tr = cb.closest('tr');
            if (tr) {
              var pi = tr.querySelector('.hq-dr-purge');
              if (pi) pi.disabled = cb.disabled || !cb.checked;
            }
          }
          hqLedgerPayFollowSyncIfNeeded(e.target);
          var tCh = e.target;
          if (tCh && pane.contains(tCh) && tCh.name === 'helloTimelineEnabledYn') {
            syncHelloTimelineLedgerFields();
          }
          if (tCh && pane.contains(tCh) && (tCh.name === 'feeListDecimalPlaces' || tCh.name === 'feeListRoundMode'
            || (tCh.classList && tCh.classList.contains('hq-fcf-dp')))) {
            syncLedgerFeeDecimalRoundUi();
          }
        });
        pane.addEventListener('input', function (e) {
          hqLedgerPayFollowSyncIfNeeded(e.target);
        });
      }
      reloadLedgerSys();
    }
    if (url === '/hq/accountMng' && !pane._hqAccBound) {
      pane._hqAccBound = true;
      function openHqAccountAccessModal(pane, tabId, edit) {
        edit = edit || null;
        window._pgHqAccountAccessPending = { pane: pane, tabId: tabId };
        var compSel = document.getElementById('pgHqAccountAccessCompSelect');
        var modal = document.getElementById('pgHqAccountAccessAddModal');
        var idEl = document.getElementById('pgHqAccountAccessEditId');
        var titleEl = document.getElementById('pgHqAccountAccessModalTitle');
        if (idEl) idEl.value = edit && edit.id ? String(edit.id) : '';
        if (titleEl) titleEl.textContent = edit && edit.id ? '접근권한 수정' : '접근권한 추가';
        function populateHqAccCompSelect(comps) {
          if (!compSel) return;
          compSel.innerHTML = '';
          var ph = document.createElement('option');
          ph.value = '';
          ph.disabled = true;
          ph.selected = true;
          ph.textContent = '업체를 선택하세요';
          compSel.appendChild(ph);
          (comps || []).forEach(function (co) {
            var code = String(co && co.code != null ? co.code : '').trim();
            if (!code) return;
            var nm = String(co && co.name != null ? co.name : '').trim();
            var opt = document.createElement('option');
            opt.value = code;
            opt.textContent = nm ? (nm + ' · ' + code) : code;
            compSel.appendChild(opt);
          });
        }
        function stashUsersAndOpenModal(users, comps) {
          window._pgHqAccountAccessAllUsers = Array.isArray(users) ? users : [];
          if (window._pgFillHqAccountAccessUserSelect) window._pgFillHqAccountAccessUserSelect();
          populateHqAccCompSelect(comps);
          var userSel = document.getElementById('pgHqAccountAccessUsername');
          if (compSel && userSel) {
            compSel.disabled = !String(userSel.value || '').trim();
          }
          if (modal && window.PG_UI && window.PG_UI.openModal) {
            window.PG_UI.openModal(modal);
            setTimeout(function () {
              if (edit && edit.id) {
                var userSel2 = document.getElementById('pgHqAccountAccessUsername');
                var cc = String(edit.compCode != null ? edit.compCode : '').trim();
                var un = String(edit.username != null ? edit.username : '').trim();
                if (userSel2 && un) {
                  var hasU = false;
                  for (var ui = 0; ui < userSel2.options.length; ui++) {
                    if (userSel2.options[ui].value === un) { hasU = true; break; }
                  }
                  if (!hasU) {
                    var ou = document.createElement('option');
                    ou.value = un;
                    ou.textContent = un;
                    userSel2.appendChild(ou);
                  }
                  userSel2.value = un;
                }
                if (compSel && cc) {
                  var hasC = false;
                  for (var ci = 0; ci < compSel.options.length; ci++) {
                    if (compSel.options[ci].value === cc) { hasC = true; break; }
                  }
                  if (!hasC) {
                    var ox = document.createElement('option');
                    ox.value = cc;
                    ox.textContent = cc;
                    compSel.appendChild(ox);
                  }
                  compSel.value = cc;
                }
                if (compSel && userSel2 && String(userSel2.value || '').trim()) compSel.disabled = false;
              }
              try {
                var uFocus = document.getElementById('pgHqAccountAccessUsername');
                if (uFocus && !uFocus.disabled) uFocus.focus();
              } catch (e2) {}
            }, 0);
          }
        }
        if (!modal || !window.PG_UI || !window.PG_UI.openModal) {
          var uid0 = window.prompt('사용자ID(로그인 ID)를 입력하세요.');
          if (uid0 == null || !String(uid0).trim()) return;
          var cc0 = window.prompt('허용할 업체코드(본사·총판·가맹점 코드)를 입력하세요.');
          if (cc0 == null || !String(cc0).trim()) return;
          var dimm0 = document.getElementById('dimm');
          if (dimm0) dimm0.style.display = 'flex';
          window.PG_API.hqAccountAccessAdd({ username: String(uid0).trim(), compCode: String(cc0).trim() }).then(function () {
            if (typeof doSearch === 'function') doSearch(pane, tabId, 1);
          }).catch(function (err) { alert(err && err.message ? err.message : '추가 실패'); }).finally(function () { if (dimm0) dimm0.style.display = 'none'; });
          return;
        }
        function afterDataReady() {
          stashUsersAndOpenModal(pane._hqAccountAccessUsers, pane._hqAccountAccessComps);
        }
        if (pane._hqAccountAccessPickersReady) {
          afterDataReady();
          return;
        }
        if (!window.PG_API || typeof window.PG_API.hqAccountAccessList !== 'function') {
          afterDataReady();
          return;
        }
        var dimmPick = document.getElementById('dimm');
        if (dimmPick) dimmPick.style.display = 'flex';
        window.PG_API.hqAccountAccessList({}).then(function (d) {
          pane._hqAccountAccessUsers = Array.isArray(d && d.users) ? d.users : [];
          pane._hqAccountAccessComps = Array.isArray(d && d.comps) ? d.comps : [];
          pane._hqAccountAccessPickersReady = true;
          afterDataReady();
        }).catch(function () {
          pane._hqAccountAccessUsers = [];
          pane._hqAccountAccessComps = [];
          pane._hqAccountAccessPickersReady = true;
          afterDataReady();
        }).finally(function () { if (dimmPick) dimmPick.style.display = 'none'; });
      }
      pane.addEventListener('click', function (e) {
        var editBtn = e.target.closest && e.target.closest('.hq-acc-edit');
        if (editBtn && pane.contains(editBtn)) {
          openHqAccountAccessModal(pane, tabId, {
            id: editBtn.getAttribute('data-id'),
            username: editBtn.getAttribute('data-username') || '',
            compCode: editBtn.getAttribute('data-compcode') || ''
          });
          return;
        }
        var del = e.target.closest && e.target.closest('.hq-acc-del');
        if (!del || !pane.contains(del)) return;
        var rid = del.getAttribute('data-id');
        if (!rid || !window.confirm('이 접근 규칙을 삭제할까요?')) return;
        var dimmA = document.getElementById('dimm');
        if (dimmA) dimmA.style.display = 'flex';
        window.PG_API.hqAccountAccessDelete(rid).then(function () {
          if (typeof doSearch === 'function') doSearch(pane, tabId, 1);
        }).catch(function (err) { alert(err && err.message ? err.message : '삭제 실패'); }).finally(function () { if (dimmA) dimmA.style.display = 'none'; });
      });
      var hqAccAdd = pane.querySelector('#hqAccountAccessAddBtn');
      if (hqAccAdd && !hqAccAdd._bound) {
        hqAccAdd._bound = true;
        hqAccAdd.addEventListener('click', function () {
          openHqAccountAccessModal(pane, tabId, null);
        });
      }
      var hqAccSaveToolbar = pane.querySelector('#hqAccountAccessSaveBtn');
      if (hqAccSaveToolbar && !hqAccSaveToolbar._bound) {
        hqAccSaveToolbar._bound = true;
        hqAccSaveToolbar.addEventListener('click', function () {
          var m = document.getElementById('pgHqAccountAccessAddModal');
          if (m && m.classList.contains('show')) {
            var sb = document.getElementById('pgHqAccountAccessAddSubmitBtn');
            if (sb) sb.click();
          } else {
            alert('먼저 [접근권한 추가] 또는 목록의 [수정]을 눌러 창을 연 뒤 [저장]을 사용하세요.');
          }
        });
      }
      var hqAccBulkDel = pane.querySelector('#hqAccountAccessBulkDelBtn');
      if (hqAccBulkDel && !hqAccBulkDel._bound) {
        hqAccBulkDel._bound = true;
        hqAccBulkDel.addEventListener('click', function () {
          var grid = pane.querySelector('#grid_' + tabId);
          if (!grid || !window.PG_API) return;
          var ids = [];
          grid.querySelectorAll('tbody tr').forEach(function (tr) {
            var ch = tr.querySelector('.grid-row-check');
            if (ch && ch.checked) {
              var rid = tr.getAttribute('data-id');
              if (rid) ids.push(String(rid));
            }
          });
          if (!ids.length) {
            alert('삭제할 행을 체크하세요.');
            return;
          }
          if (!window.confirm(ids.length + '건의 접근 규칙을 삭제할까요?')) return;
          var dimmB = document.getElementById('dimm');
          if (dimmB) dimmB.style.display = 'flex';
          var chain = Promise.resolve();
          ids.forEach(function (id) {
            chain = chain.then(function () {
              return window.PG_API.hqAccountAccessDelete(id);
            });
          });
          chain.then(function () {
            if (typeof doSearch === 'function') doSearch(pane, tabId, 1);
          }).catch(function (err) {
            alert(err && err.message ? err.message : '삭제 실패');
          }).finally(function () { if (dimmB) dimmB.style.display = 'none'; });
        });
      }
    }
    if (url === '/user/userMng' && !pane._userMngBound) {
      pane._userMngBound = true;
      var gridTid = tabId;
      function userMngTbody() {
        return pane.querySelector('#grid_' + gridTid + ' tbody');
      }
      var addUserBtn = pane.querySelector('#addBtn');
      if (addUserBtn && !addUserBtn._bound) {
        addUserBtn._bound = true;
        addUserBtn.addEventListener('click', function () {
          var dimmA = document.getElementById('dimm');
          if (dimmA) dimmA.style.display = 'flex';
          window.PG_API.authMe().then(function (resp) {
            var d = resp && resp.data !== undefined && resp.data !== null ? resp.data : resp;
            var compId = String((d && d.compId) || '').trim();
            var compNm = String((d && d.compNm) || '').trim() || '-';
            if (!compId) {
              alert('소속 업체코드를 확인할 수 없습니다.');
              return;
            }
            var cap = {};
            if (pane._lastGridList && pane._lastGridList.length) {
              cap = pane._lastGridList[0];
            }
            pane._userMngDraftRows = pane._userMngDraftRows || [];
            pane._userMngDraftRows.unshift({
              _draft: true,
              _tempId: 'd' + Date.now(),
              compId: compId,
              compNm: compNm,
              userId: '',
              userNm: '',
              mobile: '',
              assistantRoleType: 'MANAGER',
              roleNm: 'USER',
              otpRegisteredYn: 'N',
              userStatus: 'ACTIVE',
              inactiveReason: '',
              canManageUsers: cap.canManageUsers || 'Y',
              canResetPassword: cap.canResetPassword || 'Y'
            });
            var pg = parseInt(pane.getAttribute('data-last-page') || '1', 10) || 1;
            if (typeof doSearch === 'function') doSearch(pane, tabId, pg);
          }).catch(function () {
            alert('사용자 정보를 불러오지 못했습니다.');
          }).finally(function () { if (dimmA) dimmA.style.display = 'none'; });
        });
      }
      var saveUserBtn = pane.querySelector('#saveBtn');
      if (saveUserBtn && !saveUserBtn._bound) {
        saveUserBtn._bound = true;
        saveUserBtn.addEventListener('click', function () {
          var tbody = userMngTbody();
          if (!tbody || !window.PG_API) return;
          var dimmS = document.getElementById('dimm');
          var drafts = tbody.querySelectorAll('tr[data-draft="1"]');
          var updateRows = [];
          tbody.querySelectorAll('tr').forEach(function (tr) {
            if (tr.getAttribute('data-draft') === '1') return;
            var rid = tr.getAttribute('data-id');
            if (rid) updateRows.push(tr);
          });
          var addTasks = [];
          for (var i = 0; i < drafts.length; i++) {
            (function (tr) {
              var uidEl = tr.querySelector('[data-field="userId"]');
              var nmEl = tr.querySelector('[data-field="userNm"]');
              var mobEl = tr.querySelector('[data-field="mobile"]');
              var pwdEl = tr.querySelector('.user-mng-pwd');
              var arEl = tr.querySelector('[data-field="assistantRoleType"]');
              var userId = uidEl ? String(uidEl.value || '').trim() : '';
              var userNm = nmEl ? String(nmEl.value || '').trim() : '';
              var mobile = mobEl ? String(mobEl.value || '').trim() : '';
              var password = pwdEl ? String(pwdEl.value || '').trim() : '';
              var assistantRoleType = arEl && arEl.value ? String(arEl.value).trim().toUpperCase() : 'MANAGER';
              var compId = '';
              try {
                var rowObj = pane._userMngDraftRows.filter(function (x) { return x._tempId === tr.getAttribute('data-temp-id'); })[0];
                compId = rowObj && rowObj.compId ? String(rowObj.compId).trim() : '';
              } catch (e1) { compId = ''; }
              if (!userId || !userNm || !password) {
                alert('추가 행: 사용자ID, 사용자명, 비밀번호(8자 이상)를 입력하세요.');
                addTasks = null;
                return;
              }
              if (password.length < 8) {
                alert('비밀번호는 8자 이상이어야 합니다.');
                addTasks = null;
                return;
              }
              addTasks.push(function () {
                return window.PG_API.userAdd({
                  compId: compId,
                  userId: userId,
                  userNm: userNm,
                  mobile: mobile,
                  password: password,
                  role: 'USER',
                  userType: 'ASSISTANT',
                  assistantRoleType: assistantRoleType
                });
              });
            })(drafts[i]);
            if (addTasks === null) return;
          }
          var updTasks = [];
          for (var j = 0; j < updateRows.length; j++) {
            (function (tr) {
              var rid = tr.getAttribute('data-id');
              if (!rid) return;
              var mobEl = tr.querySelector('[data-field="mobile"]');
              var stEl = tr.querySelector('[data-field="userStatus"]');
              var irEl = tr.querySelector('[data-field="inactiveReason"]');
              var arEl = tr.querySelector('[data-field="assistantRoleType"]');
              updTasks.push(function () {
                return window.PG_API.userUpdate({
                  id: rid,
                  mobile: mobEl ? String(mobEl.value || '').trim() : '',
                  userStatus: stEl && stEl.value ? String(stEl.value).trim() : 'ACTIVE',
                  inactiveReason: irEl ? String(irEl.value || '').trim() : '',
                  assistantRoleType: arEl && arEl.value ? String(arEl.value).trim() : 'MANAGER'
                });
              });
            })(updateRows[j]);
          }
          if (dimmS) dimmS.style.display = 'flex';
          var chain = Promise.resolve();
          (addTasks || []).forEach(function (fn) {
            chain = chain.then(function () { return fn(); });
          });
          chain = chain.then(function () {
            return updTasks.reduce(function (p, fn) { return p.then(function () { return fn(); }); }, Promise.resolve());
          });
          chain.then(function () {
            pane._userMngDraftRows = [];
            alert('저장되었습니다.');
            if (typeof doSearch === 'function') doSearch(pane, tabId, 1);
          }).catch(function (err) {
            alert(err && err.message ? err.message : '저장 실패');
          }).finally(function () { if (dimmS) dimmS.style.display = 'none'; });
        });
      }
      pane.addEventListener('click', function (e) {
        var draftRm = e.target.closest && e.target.closest('.user-mng-draft-remove');
        if (draftRm && pane.contains(draftRm)) {
          var tid = draftRm.getAttribute('data-temp-id') || '';
          pane._userMngDraftRows = (pane._userMngDraftRows || []).filter(function (d) { return String(d._tempId) !== tid; });
          var pg = parseInt(pane.getAttribute('data-last-page') || '1', 10) || 1;
          if (typeof doSearch === 'function') doSearch(pane, tabId, pg);
          return;
        }
        var otpB = e.target.closest && e.target.closest('.user-otp-reset-btn');
        if (otpB && pane.contains(otpB)) {
          var oid = otpB.getAttribute('data-id');
          if (!oid || !window.confirm('OTP 등록을 초기화할까요?')) return;
          var dimmO = document.getElementById('dimm');
          if (dimmO) dimmO.style.display = 'flex';
          window.PG_API.userResetOtp(oid).then(function () {
            alert('OTP가 초기화되었습니다.');
            if (typeof doSearch === 'function') doSearch(pane, tabId, parseInt(pane.getAttribute('data-last-page') || '1', 10) || 1);
          }).catch(function (err) { alert(err && err.message ? err.message : '초기화 실패'); }).finally(function () { if (dimmO) dimmO.style.display = 'none'; });
          return;
        }
        var resetBtn = e.target.closest && e.target.closest('.user-reset-pwd-btn');
        if (!resetBtn || !pane.contains(resetBtn)) return;
        var resetId = resetBtn.getAttribute('data-id');
        if (!resetId || !window.confirm('비밀번호를 아이디+1! 로 초기화할까요? 최초 로그인 시 새 창에서 비밀번호를 다시 설정합니다.')) return;
        var dimmUR = document.getElementById('dimm');
        if (dimmUR) dimmUR.style.display = 'flex';
        window.PG_API.userResetPassword(resetId).then(function (data) {
          alert('초기화 완료\n사용자ID: ' + (data.userId || '') + '\n임시 비밀번호(아이디+1!): ' + (data.tempPassword || ''));
          if (typeof doSearch === 'function') doSearch(pane, tabId, parseInt(pane.getAttribute('data-last-page') || '1', 10) || 1);
        }).catch(function (err) {
          alert(err && err.message ? err.message : '초기화 실패');
        }).finally(function () { if (dimmUR) dimmUR.style.display = 'none'; });
      });
    }
    if ((url === '/calc/payList' || url === '/calc/payNotiList') && !pane._payFollowBound) {
      pane._payFollowBound = true;
      pane.addEventListener('click', function (e) {
        var btn = e.target.closest ? e.target.closest('.pay-follow') : null;
        if (!btn || !pane.contains(btn)) return;
        if (btn.disabled) return;
        var trn = btn.getAttribute('data-trn') || '';
        var act = btn.getAttribute('data-act') || '';
        if (!trn || !act) return;
        if (!window.confirm('거래 ' + trn + '에 대해 [' + act + '] 를 실행할까요?')) return;
        var dimmP = document.getElementById('dimm');
        if (dimmP) dimmP.style.display = 'flex';
        window.PG_API.payAction(trn, act).then(function () {
          alert('처리되었습니다.');
          if (typeof doSearch === 'function') doSearch(pane, tabId, parseInt(pane.getAttribute('data-last-page') || '1', 10) || 1);
        }).catch(function (err) { alert(err && err.message ? err.message : '실패'); }).finally(function () { if (dimmP) dimmP.style.display = 'none'; });
      });
    }
    /** 본사 결제로직설정 — 결제통화 스케일 규칙 JSON 편집기 (추가·수정·삭제·목록 반영·하단 저장) */
    function initHqPayCurrencyScaleRulesEditor(pane) {
      var mount = pane.querySelector('#hqPayCurrencyScaleMount');
      var hidden = pane.querySelector('[name="payCurrencyScaleRulesJson"]');
      var tbody = pane.querySelector('#hqPayCurrencyScaleTbody');
      var draftPg = pane.querySelector('#hqPayScaleDraftPg');
      var draftCur = pane.querySelector('#hqPayScaleDraftCur');
      var draftMode = pane.querySelector('#hqPayScaleDraftMode');
      var btnAdd = pane.querySelector('#hqPayCurrencyScaleBtnAdd');
      var btnApplyEdit = pane.querySelector('#hqPayCurrencyScaleBtnApplyEdit');
      var btnCancelEdit = pane.querySelector('#hqPayCurrencyScaleBtnCancelEdit');
      var btnSyncHidden = pane.querySelector('#hqPayCurrencyScaleBtnSyncHidden');
      var editBanner = pane.querySelector('#hqPayScaleEditBanner');
      var addRowLegacy = pane.querySelector('#hqPayCurrencyScaleAddRow');
      if (!mount || !hidden || !tbody || !draftPg || !draftCur || !draftMode) return;
      var CURRENCIES = [
        { v: 'JPY', t: 'JPY' }, { v: 'KRW', t: 'KRW' }, { v: 'USD', t: 'USD' }, { v: 'EUR', t: 'EUR' },
        { v: 'THB', t: 'THB' }, { v: 'TWD', t: 'TWD' }, { v: 'HKD', t: 'HKD' }, { v: 'SGD', t: 'SGD' },
        { v: 'CNY', t: 'CNY' }, { v: 'GBP', t: 'GBP' }, { v: 'AUD', t: 'AUD' }, { v: 'VND', t: 'VND' },
        { v: 'PHP', t: 'PHP' }, { v: 'MYR', t: 'MYR' }, { v: 'IDR', t: 'IDR' }, { v: 'INR', t: 'INR' }
      ];
      var MODES = [
        { v: 'SAME', t: '= 동일' },
        { v: 'MULTIPLY_100', t: '×100' },
        { v: 'DIVIDE_100', t: '÷100' }
      ];
      function escA(s) {
        return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
      }
      var state = { rules: [], pgList: [], editingIndex: null };
      function parseHidden() {
        try {
          var j = hidden.value ? JSON.parse(hidden.value) : {};
          state.rules = Array.isArray(j.rules) ? j.rules.map(function (r) {
            return {
              pgCd: String(r.pgCd || '').trim(),
              currency: String(r.currency || '').trim().toUpperCase(),
              mode: String(r.mode || 'SAME').trim().toUpperCase()
            };
          }) : [];
        } catch (e1) {
          state.rules = [];
        }
      }
      function writeHidden() {
        hidden.value = JSON.stringify({ rules: state.rules });
      }
      function fillDraftCurModeOptions() {
        var curH = '';
        CURRENCIES.forEach(function (c) {
          curH += '<option value="' + escA(c.v) + '">' + escA(c.t) + '</option>';
        });
        draftCur.innerHTML = curH;
        var modeH = '';
        MODES.forEach(function (m) {
          modeH += '<option value="' + escA(m.v) + '">' + escA(m.t) + '</option>';
        });
        draftMode.innerHTML = modeH;
      }
      function pgIntegrationScopeText(p) {
        if (!p) return '—';
        var raw = String(p.integrationScopeLabel != null ? p.integrationScopeLabel : '').trim();
        if (raw) {
          return raw.replace(/\bURL\b/g, 'URL결제');
        }
        var parts = [];
        if (String(p.integNotiYn || '').toUpperCase() === 'Y') parts.push('노티');
        if (String(p.integUrlPayYn || '').toUpperCase() === 'Y') parts.push('URL결제');
        if (String(p.integWebChatbotYn || '').toUpperCase() === 'Y') parts.push('챗봇');
        if (String(p.integApiYn || '').toUpperCase() === 'Y') parts.push('API');
        return parts.length ? parts.join(' / ') : '—';
      }
      function fillDraftPgOptions() {
        var opts = '<option value="">' + escA('선택') + '</option>';
        state.pgList.forEach(function (p) {
          var cd = String(p.pgCd || '').trim();
          if (!cd) return;
          var nm = String(p.pgNm || cd).trim();
          var scope = pgIntegrationScopeText(p);
          opts += '<option value="' + escA(cd) + '">' + escA(nm + ' (' + cd + ') — ' + scope) + '</option>';
        });
        draftPg.innerHTML = opts;
      }
      function readDraft() {
        return {
          pgCd: String(draftPg.value || '').trim(),
          currency: String(draftCur.value || '').trim().toUpperCase(),
          mode: String(draftMode.value || 'SAME').trim().toUpperCase()
        };
      }
      function setDraft(r) {
        draftPg.value = r && r.pgCd ? r.pgCd : '';
        draftCur.value = r && r.currency ? r.currency : 'JPY';
        draftMode.value = r && r.mode ? r.mode : 'SAME';
      }
      function validateDraft(d) {
        if (!d.pgCd) {
          alert('결제대행사를 선택하세요.');
          return false;
        }
        if (!d.currency) {
          alert('통화를 선택하세요.');
          return false;
        }
        return true;
      }
      function modeLabel(m) {
        var u = String(m || 'SAME').toUpperCase();
        if (u === 'MULTIPLY_100') return '×100';
        if (u === 'DIVIDE_100') return '÷100';
        return '= 동일';
      }
      function pgLabel(cd) {
        var c = String(cd || '');
        var p = state.pgList.filter(function (x) { return String(x.pgCd || '').trim() === c; })[0];
        if (p && p.pgNm) return String(p.pgNm) + ' (' + c + ')';
        return c || '—';
      }
      function pgScopeCell(cd) {
        var c = String(cd || '');
        var p = state.pgList.filter(function (x) { return String(x.pgCd || '').trim() === c; })[0];
        return escA(pgIntegrationScopeText(p));
      }
      function updateEditBanner() {
        if (!editBanner) return;
        if (state.editingIndex != null) {
          editBanner.textContent = '행 ' + (state.editingIndex + 1) + ' 수정 중 — 값을 바꾼 뒤 「수정 적용」을 누르세요.';
          editBanner.classList.remove('d-none');
        } else {
          editBanner.textContent = '';
          editBanner.classList.add('d-none');
        }
      }
      function clearEditing() {
        state.editingIndex = null;
        updateEditBanner();
      }
      function render() {
        var html = '';
        if (!state.rules.length) {
          html = '<tr><td colspan="7" class="text-muted text-center py-3 small">등록된 규칙이 없습니다. 위에서 PG·통화·배율을 고른 뒤 「추가」하거나, 아래 링크로 빈 행을 넣을 수 있습니다. (없으면 금액은 PG에 그대로 전달됩니다.)</td></tr>';
        } else {
          state.rules.forEach(function (r, idx) {
            html += '<tr data-rule-idx="' + idx + '">' +
              '<td class="text-center small text-muted">' + (idx + 1) + '</td>' +
              '<td class="small">' + escA(pgLabel(r.pgCd)) + '</td>' +
              '<td class="small text-muted">' + pgScopeCell(r.pgCd) + '</td>' +
              '<td class="small font-monospace">' + escA(r.currency || '') + '</td>' +
              '<td class="small">' + escA(modeLabel(r.mode)) + '</td>' +
              '<td class="text-center"><button type="button" class="btn btn-sm btn-outline-primary hq-pay-scale-edit">수정</button></td>' +
              '<td class="text-center"><button type="button" class="btn btn-sm btn-outline-danger hq-pay-scale-del">삭제</button></td></tr>';
          });
        }
        tbody.innerHTML = html;
        tbody.querySelectorAll('.hq-pay-scale-edit').forEach(function (btn) {
          btn.addEventListener('click', function () {
            var tr = btn.closest('tr');
            var idx = tr ? parseInt(tr.getAttribute('data-rule-idx'), 10) : NaN;
            if (isNaN(idx) || idx < 0 || idx >= state.rules.length) return;
            state.editingIndex = idx;
            setDraft(state.rules[idx]);
            updateEditBanner();
          });
        });
        tbody.querySelectorAll('.hq-pay-scale-del').forEach(function (btn) {
          btn.addEventListener('click', function () {
            var tr = btn.closest('tr');
            var idx = tr ? parseInt(tr.getAttribute('data-rule-idx'), 10) : NaN;
            if (isNaN(idx) || idx < 0 || idx >= state.rules.length) return;
            if (!window.confirm('이 규칙 행을 삭제할까요?')) return;
            state.rules.splice(idx, 1);
            if (state.editingIndex != null) {
              if (state.editingIndex === idx) clearEditing();
              else if (state.editingIndex > idx) state.editingIndex -= 1;
            }
            writeHidden();
            render();
          });
        });
        writeHidden();
      }
      function doAddFromDraft() {
        var d = readDraft();
        if (!validateDraft(d)) return;
        if (state.editingIndex != null && !window.confirm('수정 중인 행이 있습니다. 추가하면 수정 모드가 취소됩니다. 계속할까요?')) return;
        clearEditing();
        state.rules.push({ pgCd: d.pgCd, currency: d.currency, mode: d.mode });
        writeHidden();
        render();
      }
      function doApplyEdit() {
        if (state.editingIndex == null) {
          alert('먼저 목록에서 「수정」을 눌러 편집할 행을 선택하세요.');
          return;
        }
        var d = readDraft();
        if (!validateDraft(d)) return;
        state.rules[state.editingIndex] = { pgCd: d.pgCd, currency: d.currency, mode: d.mode };
        clearEditing();
        writeHidden();
        render();
        alert('선택한 행이 반영되었습니다. 서버 저장은 화면 하단 「저장」을 누르세요.');
      }
      function doSyncHidden() {
        writeHidden();
        alert('목록이 숨김 필드에 반영되었습니다. 서버에 저장하려면 화면 하단 「저장」을 누르세요.');
      }
      function pushDefaultRow() {
        if (state.editingIndex != null && !window.confirm('수정 중인 행이 있습니다. 취소하고 빈 행을 추가할까요?')) return;
        clearEditing();
        var pg0 = state.pgList[0] ? String(state.pgList[0].pgCd || '').trim() : '';
        state.rules.push({ pgCd: pg0, currency: 'JPY', mode: 'MULTIPLY_100' });
        writeHidden();
        render();
      }
      fillDraftCurModeOptions();
      if (!mount._hqPayScaleEditorBound) {
        mount._hqPayScaleEditorBound = true;
        if (btnAdd) btnAdd.addEventListener('click', function () { doAddFromDraft(); });
        if (btnApplyEdit) btnApplyEdit.addEventListener('click', function () { doApplyEdit(); });
        if (btnCancelEdit) btnCancelEdit.addEventListener('click', function () {
          clearEditing();
          setDraft({ pgCd: '', currency: 'JPY', mode: 'MULTIPLY_100' });
        });
        if (btnSyncHidden) btnSyncHidden.addEventListener('click', function () { doSyncHidden(); });
        if (addRowLegacy) addRowLegacy.addEventListener('click', function () { pushDefaultRow(); });
      }
      function loadPgThenRender() {
        if (!window.PG_API || typeof window.PG_API.pgAgencyList !== 'function') {
          parseHidden();
          fillDraftPgOptions();
          render();
          return;
        }
        window.PG_API.pgAgencyList().then(function (list) {
          var rows = Array.isArray(list) ? list : [];
          state.pgList = rows.filter(function (r) {
            return String(r.integUrlPayYn || '').toUpperCase() === 'Y' && String(r.useYn || 'Y').toUpperCase() === 'Y';
          });
          if (!state.pgList.length) state.pgList = rows.filter(function (r) { return String(r.useYn || 'Y').toUpperCase() === 'Y'; });
          parseHidden();
          fillDraftPgOptions();
          render();
        }).catch(function () {
          parseHidden();
          fillDraftPgOptions();
          render();
        });
      }
      loadPgThenRender();
      var saveBtn = pane.querySelector('#hqPaymentOrchSaveBtn');
      if (saveBtn && !saveBtn._hqPayScaleSaveHook) {
        saveBtn._hqPayScaleSaveHook = true;
        saveBtn.addEventListener('click', function () {
          writeHidden();
        }, true);
      }
    }
    /** URL 결제 폼 설정 — 탭 제목(JSON)·파비콘(본사 tb_hq_api_config) */
    function initHqUrlPayFormChrome(pane) {
      var hidTab = pane.querySelector('#urlPayTabTitleJson');
      var koIn = pane.querySelector('#hqUrlPayFormTabTitleKo');
      var favIn = pane.querySelector('#hqUrlPayFormFaviconUrl');
      var favFile = pane.querySelector('#hqUrlPayFormFaviconFile');
      var btnTr = pane.querySelector('#hqUrlPayFormTabTitleTranslateBtn');
      if (!hidTab || !koIn) return;
      function parseTabObj() {
        try {
          var j = hidTab.value ? JSON.parse(hidTab.value) : {};
          return (j && typeof j === 'object') ? j : {};
        } catch (e0) {
          return {};
        }
      }
      function syncKoToHidden() {
        var o = parseTabObj();
        var k = String(koIn.value || '').trim();
        if (k) o.KOR = k;
        else delete o.KOR;
        hidTab.value = JSON.stringify(o);
      }
      function applyHiddenToKo() {
        var o = parseTabObj();
        koIn.value = o.KOR != null ? String(o.KOR) : '';
      }
      if (!hidTab._hqUrlPayChromeInit) {
        hidTab._hqUrlPayChromeInit = true;
        koIn.addEventListener('input', function () { syncKoToHidden(); });
        koIn.addEventListener('blur', function () { syncKoToHidden(); });
      }
      applyHiddenToKo();
      if (btnTr && !btnTr._hqUrlPayTabTrBound) {
        btnTr._hqUrlPayTabTrBound = true;
        btnTr.addEventListener('click', function () {
          syncKoToHidden();
          var ko = String(koIn.value || '').trim();
          if (!ko) {
            alert('한국어 탭 제목을 입력하세요.');
            return;
          }
          if (!window.PG_API || typeof window.PG_API.hqUrlPayTabTitleTranslateFromKo !== 'function') {
            alert('API를 사용할 수 없습니다.');
            return;
          }
          var dimmT = document.getElementById('dimm');
          if (dimmT) dimmT.style.display = 'flex';
          window.PG_API.hqUrlPayTabTitleTranslateFromKo({ tabTitleKo: ko }).then(function (data) {
            var m = data && data.tabTitle && typeof data.tabTitle === 'object' ? data.tabTitle : {};
            hidTab.value = JSON.stringify(m);
            applyHiddenToKo();
          }).catch(function (e) {
            alert((e && e.message) ? e.message : '번역 요청 실패');
          }).finally(function () { if (dimmT) dimmT.style.display = 'none'; });
        });
      }
      var favBrowse = pane.querySelector('#hqUrlPayFormFaviconBrowse');
      var favUpload = pane.querySelector('#hqUrlPayFormFaviconUpload');
      var favClear = pane.querySelector('#hqUrlPayFormFaviconClear');
      if (favBrowse && favFile && !favBrowse._hqUrlPayFavBr) {
        favBrowse._hqUrlPayFavBr = true;
        favBrowse.addEventListener('click', function () { favFile.click(); });
      }
      if (favUpload && favFile && favIn && !favUpload._hqUrlPayFavUp) {
        favUpload._hqUrlPayFavUp = true;
        favUpload.addEventListener('click', function () {
          var f = favFile.files && favFile.files[0];
          if (!f) {
            alert('파일을 선택하세요.');
            return;
          }
          if (!window.PG_API || typeof window.PG_API.hqUrlPayFaviconUpload !== 'function') {
            alert('업로드 API를 사용할 수 없습니다.');
            return;
          }
          var dimmF = document.getElementById('dimm');
          if (dimmF) dimmF.style.display = 'flex';
          window.PG_API.hqUrlPayFaviconUpload(f).then(function (data) {
            var u = data && data.url ? String(data.url) : '';
            favIn.value = u;
            if (!u) alert('업로드 응답에 URL이 없습니다.');
          }).catch(function (e) {
            alert((e && e.message) ? e.message : '업로드 실패');
          }).finally(function () { if (dimmF) dimmF.style.display = 'none'; });
        });
      }
      if (favClear && favIn && favFile && !favClear._hqUrlPayFavCl) {
        favClear._hqUrlPayFavCl = true;
        favClear.addEventListener('click', function () {
          favIn.value = '';
          favFile.value = '';
        });
      }
    }
    function syncHqUrlPayFormChromeToHidden(pane) {
      var hidTab = pane.querySelector('#urlPayTabTitleJson');
      var koIn = pane.querySelector('#hqUrlPayFormTabTitleKo');
      if (!hidTab || !koIn) return;
      try {
        var o = hidTab.value ? JSON.parse(hidTab.value) : {};
        if (!o || typeof o !== 'object') o = {};
        var k = String(koIn.value || '').trim();
        if (k) o.KOR = k;
        else delete o.KOR;
        hidTab.value = JSON.stringify(o);
      } catch (e1) {
        var k2 = String(koIn.value || '').trim();
        hidTab.value = k2 ? JSON.stringify({ KOR: k2 }) : '{}';
      }
    }
    /** 결제구문설정 — URL 결제 카드 안내 문구(PG별·다국어) */
    function initHqUrlPayCardCopyEditor(pane) {
      var mount = pane.querySelector('#hqPayCardCopyMount');
      var hidden = pane.querySelector('[name="urlPayCardCopyConfigJson"]');
      var tbody = pane.querySelector('#hqPayCardCopyTbody');
      var draftPg = pane.querySelector('#hqPayCardCopyDraftPg');
      var draftTitle = pane.querySelector('#hqPayCardCopyDraftTitle');
      var draftB1 = pane.querySelector('#hqPayCardCopyDraftBody1');
      var draftB2 = pane.querySelector('#hqPayCardCopyDraftBody2');
      var draftB3 = pane.querySelector('#hqPayCardCopyDraftBody3');
      var draftResultOk1 = pane.querySelector('#hqPayCardCopyDraftResultOk1');
      var draftResultOk2 = pane.querySelector('#hqPayCardCopyDraftResultOk2');
      var draftResultFail1 = pane.querySelector('#hqPayCardCopyDraftResultFail1');
      var draftResultFail2 = pane.querySelector('#hqPayCardCopyDraftResultFail2');
      var btnSave = pane.querySelector('#hqPayCardCopyBtnSave');
      var btnSaveI18n = pane.querySelector('#hqPayCardCopyBtnSaveI18n');
      var btnCancelEdit = pane.querySelector('#hqPayCardCopyBtnCancelEdit');
      var editBanner = pane.querySelector('#hqPayCardCopyEditBanner');
      if (!mount || !hidden || !tbody || !draftPg) return;
      if (!mount.__payCardCopyState) {
        mount.__payCardCopyState = { entries: [], urlPayPgs: [], editingId: null };
      }
      var state = mount.__payCardCopyState;
      function uuid() {
        return 'c' + Date.now().toString(36) + Math.random().toString(36).slice(2, 10);
      }
      function escA(s) {
        return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
      }
      function normLangMap(o) {
        if (o == null) return {};
        if (typeof o === 'string') return { KOR: o };
        if (typeof o === 'object') return o;
        return {};
      }
      function parseHidden() {
        try {
          var j = hidden.value ? JSON.parse(hidden.value) : {};
          var arr = Array.isArray(j.entries) ? j.entries : [];
          state.entries = arr.map(function (raw) {
            return {
              id: String(raw.id || uuid()),
              pgCd: String(raw.pgCd || '').trim(),
              activeYn: String(raw.activeYn || 'N').toUpperCase() === 'Y' ? 'Y' : 'N',
              title: normLangMap(raw.title),
              body1: normLangMap(raw.body1),
              body2: normLangMap(raw.body2),
              body3: normLangMap(raw.body3),
              resultSuccessMain: normLangMap(raw.resultSuccessMain),
              resultSuccessFoot: normLangMap(raw.resultSuccessFoot),
              resultFailMain: normLangMap(raw.resultFailMain),
              resultFailFoot: normLangMap(raw.resultFailFoot)
            };
          });
        } catch (e0) {
          state.entries = [];
        }
      }
      function mapHasAny(m) {
        return m && typeof m === 'object' && Object.keys(m).length > 0;
      }
      function writeHidden() {
        var slim = state.entries.map(function (e) {
          var o = {
            id: e.id,
            pgCd: e.pgCd,
            activeYn: e.activeYn,
            title: e.title,
            body1: e.body1,
            body2: e.body2,
            body3: e.body3
          };
          if (mapHasAny(e.resultSuccessMain)) o.resultSuccessMain = e.resultSuccessMain;
          if (mapHasAny(e.resultSuccessFoot)) o.resultSuccessFoot = e.resultSuccessFoot;
          if (mapHasAny(e.resultFailMain)) o.resultFailMain = e.resultFailMain;
          if (mapHasAny(e.resultFailFoot)) o.resultFailFoot = e.resultFailFoot;
          return o;
        });
        hidden.value = JSON.stringify({ entries: slim });
      }
      function pickKo(m) {
        if (!m || typeof m !== 'object') return '';
        return m.KOR != null ? String(m.KOR) : '';
      }
      function entryHasKoreanBody(en) {
        return pickKo(en.title).trim().length > 0 && pickKo(en.body1).trim().length > 0 && pickKo(en.body2).trim().length > 0 && pickKo(en.body3).trim().length > 0;
      }
      function deactivateSamePg(pgCd, exceptId) {
        var p = String(pgCd || '').trim();
        state.entries.forEach(function (e) {
          if (e.id === exceptId) return;
          if (String(e.pgCd || '').trim() === p) e.activeYn = 'N';
        });
      }
      function fillAllTranslations(titleKo, b1Ko, b2Ko, b3Ko, rOk1Ko, rOk2Ko, rFail1Ko, rFail2Ko) {
        var ro1 = rOk1Ko != null ? String(rOk1Ko).trim() : '';
        var ro2 = rOk2Ko != null ? String(rOk2Ko).trim() : '';
        var rf1 = rFail1Ko != null ? String(rFail1Ko).trim() : '';
        var rf2 = rFail2Ko != null ? String(rFail2Ko).trim() : '';
        if (window.PG_API && typeof window.PG_API.hqPayCopyTranslateFromKo === 'function') {
          return window.PG_API.hqPayCopyTranslateFromKo({
            titleKo: titleKo,
            body1Ko: b1Ko,
            body2Ko: b2Ko,
            body3Ko: b3Ko,
            resultOk1Ko: ro1,
            resultOk2Ko: ro2,
            resultFail1Ko: rf1,
            resultFail2Ko: rf2
          }).then(function (data) {
            return {
              title: (data && data.title) ? data.title : { KOR: titleKo },
              body1: (data && data.body1) ? data.body1 : { KOR: b1Ko },
              body2: (data && data.body2) ? data.body2 : { KOR: b2Ko },
              body3: (data && data.body3) ? data.body3 : { KOR: b3Ko },
              resultSuccessMain: (data && data.resultSuccessMain && typeof data.resultSuccessMain === 'object') ? data.resultSuccessMain : (ro1 ? { KOR: ro1 } : {}),
              resultSuccessFoot: (data && data.resultSuccessFoot && typeof data.resultSuccessFoot === 'object') ? data.resultSuccessFoot : (ro2 ? { KOR: ro2 } : {}),
              resultFailMain: (data && data.resultFailMain && typeof data.resultFailMain === 'object') ? data.resultFailMain : (rf1 ? { KOR: rf1 } : {}),
              resultFailFoot: (data && data.resultFailFoot && typeof data.resultFailFoot === 'object') ? data.resultFailFoot : (rf2 ? { KOR: rf2 } : {})
            };
          });
        }
        return Promise.resolve({
          title: { KOR: titleKo },
          body1: { KOR: b1Ko },
          body2: { KOR: b2Ko },
          body3: { KOR: b3Ko },
          resultSuccessMain: ro1 ? { KOR: ro1 } : {},
          resultSuccessFoot: ro2 ? { KOR: ro2 } : {},
          resultFailMain: rf1 ? { KOR: rf1 } : {},
          resultFailFoot: rf2 ? { KOR: rf2 } : {}
        });
      }
      function findEntry(id) {
        return state.entries.filter(function (x) { return x.id === id; })[0];
      }
      function updateEditBanner() {
        if (!editBanner) return;
        if (state.editingId) {
          editBanner.textContent = '목록에서 선택한 행을 수정 중입니다. 반영하려면 「저장」 또는 「저장(다국어)」을 누르세요.';
          editBanner.classList.remove('d-none');
        } else {
          editBanner.textContent = '';
          editBanner.classList.add('d-none');
        }
      }
      function clearEditing() {
        state.editingId = null;
        updateEditBanner();
      }
      function cellPreview(txt, maxLen) {
        var s = pickKo(typeof txt === 'object' && txt ? txt : {}).trim();
        if (!s) return '<span class="text-muted small">—</span>';
        var show = s.length > maxLen ? s.slice(0, maxLen) + '…' : s;
        return '<span class="small d-inline-block text-truncate hq-pay-card-copy-preview" style="max-width:11rem" title="' + escA(s) + '">' + escA(show) + '</span>';
      }
      function renderTable() {
        var html = '';
        if (!state.entries.length) {
          html = '<tr><td colspan="12" class="text-muted text-center py-3 small">등록된 결제구문이 없습니다. 위에서 입력 후 「저장」으로 목록에 추가하세요.</td></tr>';
        } else {
          state.entries.forEach(function (en) {
            var canAct = entryHasKoreanBody(en);
            var checked = String(en.activeYn || '').toUpperCase() === 'Y';
            html += '<tr data-card-copy-id="' + escA(en.id) + '">' +
              '<td class="small font-monospace">' + escA(en.pgCd || '') + '</td>' +
              '<td class="align-top">' + cellPreview(en.title, 36) + '</td>' +
              '<td class="align-top">' + cellPreview(en.body1, 48) + '</td>' +
              '<td class="align-top">' + cellPreview(en.body2, 48) + '</td>' +
              '<td class="align-top">' + cellPreview(en.body3, 48) + '</td>' +
              '<td class="align-top">' + cellPreview(en.resultSuccessMain, 32) + '</td>' +
              '<td class="align-top">' + cellPreview(en.resultSuccessFoot, 36) + '</td>' +
              '<td class="align-top">' + cellPreview(en.resultFailMain, 32) + '</td>' +
              '<td class="align-top">' + cellPreview(en.resultFailFoot, 36) + '</td>' +
              '<td class="text-center align-middle"><input type="checkbox" class="form-check-input hq-pay-card-copy-active"' +
              (checked ? ' checked' : '') + (!canAct ? ' disabled' : '') + ' title="활성"></td>' +
              '<td class="text-center align-middle"><button type="button" class="btn btn-sm btn-outline-primary hq-pay-card-copy-edit">수정</button></td>' +
              '<td class="text-center align-middle"><button type="button" class="btn btn-sm btn-outline-danger hq-pay-card-copy-del">삭제</button></td></tr>';
          });
        }
        tbody.innerHTML = html;
        tbody.querySelectorAll('.hq-pay-card-copy-edit').forEach(function (btn) {
          btn.addEventListener('click', function () {
            var tr = btn.closest('tr');
            var id = tr ? tr.getAttribute('data-card-copy-id') : '';
            var en = findEntry(id);
            if (!en) return;
            state.editingId = id;
            draftPg.value = String(en.pgCd || '');
            draftTitle.value = pickKo(en.title);
            draftB1.value = pickKo(en.body1);
            draftB2.value = pickKo(en.body2);
            if (draftB3) draftB3.value = pickKo(en.body3);
            if (draftResultOk1) draftResultOk1.value = pickKo(en.resultSuccessMain);
            if (draftResultOk2) draftResultOk2.value = pickKo(en.resultSuccessFoot);
            if (draftResultFail1) draftResultFail1.value = pickKo(en.resultFailMain);
            if (draftResultFail2) draftResultFail2.value = pickKo(en.resultFailFoot);
            updateEditBanner();
          });
        });
        tbody.querySelectorAll('.hq-pay-card-copy-active').forEach(function (cb) {
          cb.addEventListener('change', function () {
            var tr = cb.closest('tr');
            var id = tr ? tr.getAttribute('data-card-copy-id') : '';
            var en = state.entries.filter(function (x) { return x.id === id; })[0];
            if (!en) return;
            if (cb.checked) {
              if (!entryHasKoreanBody(en)) {
                cb.checked = false;
                alert('제목·내용1·내용2·내용3(한국어)를 모두 채운 항목만 활성화할 수 있습니다.');
                return;
              }
              deactivateSamePg(en.pgCd, en.id);
              en.activeYn = 'Y';
              renderTable();
            } else {
              en.activeYn = 'N';
            }
            writeHidden();
          });
        });
        tbody.querySelectorAll('.hq-pay-card-copy-del').forEach(function (btn) {
          btn.addEventListener('click', function () {
            var tr = btn.closest('tr');
            var id = tr ? tr.getAttribute('data-card-copy-id') : '';
            if (state.editingId === id) clearEditing();
            state.entries = state.entries.filter(function (x) { return x.id !== id; });
            renderTable();
            writeHidden();
          });
        });
        writeHidden();
      }
      function readDraft() {
        return {
          pgCd: String(draftPg.value || '').trim(),
          titleKo: String(draftTitle.value || '').trim(),
          b1: String(draftB1.value || '').trim(),
          b2: String(draftB2.value || '').trim(),
          b3: String((draftB3 && draftB3.value) || '').trim(),
          resultOk1Ko: draftResultOk1 ? String(draftResultOk1.value || '').trim() : '',
          resultOk2Ko: draftResultOk2 ? String(draftResultOk2.value || '').trim() : '',
          resultFail1Ko: draftResultFail1 ? String(draftResultFail1.value || '').trim() : '',
          resultFail2Ko: draftResultFail2 ? String(draftResultFail2.value || '').trim() : ''
        };
      }
      function validateDraft(d) {
        if (!d.pgCd) {
          alert('결제대행사를 선택하세요.');
          return false;
        }
        if (!d.titleKo || !d.b1 || !d.b2 || !d.b3) {
          alert('제목·내용1·내용2·내용3(한국어)를 모두 입력하세요.');
          return false;
        }
        return true;
      }
      function pushEntry(maps) {
        var d = readDraft();
        state.entries.push({
          id: uuid(),
          pgCd: d.pgCd,
          activeYn: 'N',
          title: maps.title,
          body1: maps.body1,
          body2: maps.body2,
          body3: maps.body3,
          resultSuccessMain: maps.resultSuccessMain && typeof maps.resultSuccessMain === 'object' ? maps.resultSuccessMain : {},
          resultSuccessFoot: maps.resultSuccessFoot && typeof maps.resultSuccessFoot === 'object' ? maps.resultSuccessFoot : {},
          resultFailMain: maps.resultFailMain && typeof maps.resultFailMain === 'object' ? maps.resultFailMain : {},
          resultFailFoot: maps.resultFailFoot && typeof maps.resultFailFoot === 'object' ? maps.resultFailFoot : {}
        });
        clearEditing();
        draftTitle.value = '';
        draftB1.value = '';
        draftB2.value = '';
        if (draftB3) draftB3.value = '';
        if (draftResultOk1) draftResultOk1.value = '';
        if (draftResultOk2) draftResultOk2.value = '';
        if (draftResultFail1) draftResultFail1.value = '';
        if (draftResultFail2) draftResultFail2.value = '';
        renderTable();
      }
      function setEntryLangKo(en, prop, koreanText) {
        var s = String(koreanText || '').trim();
        if (!s) {
          if (en[prop] && typeof en[prop] === 'object') {
            delete en[prop].KOR;
            if (Object.keys(en[prop]).length === 0) delete en[prop];
          }
          return;
        }
        if (!en[prop]) en[prop] = {};
        en[prop].KOR = s;
      }
      function applyKoreanToEntry(en, d) {
        en.pgCd = d.pgCd;
        if (!en.title) en.title = {};
        if (!en.body1) en.body1 = {};
        if (!en.body2) en.body2 = {};
        if (!en.body3) en.body3 = {};
        en.title.KOR = d.titleKo;
        en.body1.KOR = d.b1;
        en.body2.KOR = d.b2;
        en.body3.KOR = d.b3;
        setEntryLangKo(en, 'resultSuccessMain', d.resultOk1Ko);
        setEntryLangKo(en, 'resultSuccessFoot', d.resultOk2Ko);
        setEntryLangKo(en, 'resultFailMain', d.resultFail1Ko);
        setEntryLangKo(en, 'resultFailFoot', d.resultFail2Ko);
      }
      function doSaveKo() {
        var d = readDraft();
        if (!validateDraft(d)) return;
        if (state.editingId) {
          var en = findEntry(state.editingId);
          if (!en) {
            clearEditing();
            return;
          }
          applyKoreanToEntry(en, d);
          clearEditing();
          draftTitle.value = '';
          draftB1.value = '';
          draftB2.value = '';
          if (draftB3) draftB3.value = '';
          if (draftResultOk1) draftResultOk1.value = '';
          if (draftResultOk2) draftResultOk2.value = '';
          if (draftResultFail1) draftResultFail1.value = '';
          if (draftResultFail2) draftResultFail2.value = '';
          writeHidden();
          renderTable();
          alert('목록이 갱신되었습니다. 서버 반영은 화면 하단 「저장」을 누르세요.');
          return;
        }
        pushEntry({
          title: { KOR: d.titleKo },
          body1: { KOR: d.b1 },
          body2: { KOR: d.b2 },
          body3: { KOR: d.b3 },
          resultSuccessMain: d.resultOk1Ko ? { KOR: d.resultOk1Ko } : {},
          resultSuccessFoot: d.resultOk2Ko ? { KOR: d.resultOk2Ko } : {},
          resultFailMain: d.resultFail1Ko ? { KOR: d.resultFail1Ko } : {},
          resultFailFoot: d.resultFail2Ko ? { KOR: d.resultFail2Ko } : {}
        });
      }
      function doSaveI18n() {
        var d = readDraft();
        if (!validateDraft(d)) return;
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        fillAllTranslations(d.titleKo, d.b1, d.b2, d.b3, d.resultOk1Ko, d.resultOk2Ko, d.resultFail1Ko, d.resultFail2Ko).then(function (maps) {
          if (state.editingId) {
            var en = findEntry(state.editingId);
            if (!en) {
              clearEditing();
              return;
            }
            en.pgCd = d.pgCd;
            en.title = maps.title;
            en.body1 = maps.body1;
            en.body2 = maps.body2;
            en.body3 = maps.body3;
            en.resultSuccessMain = maps.resultSuccessMain && typeof maps.resultSuccessMain === 'object' ? maps.resultSuccessMain : {};
            en.resultSuccessFoot = maps.resultSuccessFoot && typeof maps.resultSuccessFoot === 'object' ? maps.resultSuccessFoot : {};
            en.resultFailMain = maps.resultFailMain && typeof maps.resultFailMain === 'object' ? maps.resultFailMain : {};
            en.resultFailFoot = maps.resultFailFoot && typeof maps.resultFailFoot === 'object' ? maps.resultFailFoot : {};
            clearEditing();
            draftTitle.value = '';
            draftB1.value = '';
            draftB2.value = '';
            if (draftB3) draftB3.value = '';
            if (draftResultOk1) draftResultOk1.value = '';
            if (draftResultOk2) draftResultOk2.value = '';
            if (draftResultFail1) draftResultFail1.value = '';
            if (draftResultFail2) draftResultFail2.value = '';
            writeHidden();
            renderTable();
            alert('목록이 갱신되었습니다(다국어). 서버 반영은 화면 하단 「저장」을 누르세요.');
          } else {
            pushEntry(maps);
          }
        }).catch(function () {
          alert('번역 요청 중 오류가 있었습니다. 한국어만 반영합니다.');
          if (state.editingId) {
            var en2 = findEntry(state.editingId);
            if (en2) applyKoreanToEntry(en2, d);
            clearEditing();
            draftTitle.value = '';
            draftB1.value = '';
            draftB2.value = '';
            if (draftB3) draftB3.value = '';
            if (draftResultOk1) draftResultOk1.value = '';
            if (draftResultOk2) draftResultOk2.value = '';
            if (draftResultFail1) draftResultFail1.value = '';
            if (draftResultFail2) draftResultFail2.value = '';
            writeHidden();
            renderTable();
          } else {
            pushEntry({
              title: { KOR: d.titleKo },
              body1: { KOR: d.b1 },
              body2: { KOR: d.b2 },
              body3: { KOR: d.b3 },
              resultSuccessMain: d.resultOk1Ko ? { KOR: d.resultOk1Ko } : {},
              resultSuccessFoot: d.resultOk2Ko ? { KOR: d.resultOk2Ko } : {},
              resultFailMain: d.resultFail1Ko ? { KOR: d.resultFail1Ko } : {},
              resultFailFoot: d.resultFail2Ko ? { KOR: d.resultFail2Ko } : {}
            });
          }
        }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      }
      function loadUrlPayPgOptions() {
        if (!window.PG_API || typeof window.PG_API.pgAgencyList !== 'function') return Promise.resolve();
        return window.PG_API.pgAgencyList().then(function (list) {
          var rows = Array.isArray(list) ? list : [];
          state.urlPayPgs = rows.filter(function (r) {
            return String(r.integUrlPayYn || '').toUpperCase() === 'Y' && String(r.useYn || 'Y').toUpperCase() === 'Y';
          });
          var opts = '<option value="">' + escA('선택') + '</option>';
          state.urlPayPgs.forEach(function (p) {
            var cd = String(p.pgCd || '').trim();
            if (!cd) return;
            var nm = String(p.pgNm || cd).trim();
            opts += '<option value="' + escA(cd) + '">' + escA(nm + ' (' + cd + ')') + '</option>';
          });
          draftPg.innerHTML = opts;
        });
      }
      if (!mount._hqPayCardCopyHandlersBound) {
        mount._hqPayCardCopyHandlersBound = true;
        if (btnSave) btnSave.addEventListener('click', function () { doSaveKo(); });
        if (btnSaveI18n) btnSaveI18n.addEventListener('click', function () { doSaveI18n(); });
        if (btnCancelEdit) btnCancelEdit.addEventListener('click', function () {
          clearEditing();
          draftTitle.value = '';
          draftB1.value = '';
          draftB2.value = '';
          if (draftB3) draftB3.value = '';
          if (draftResultOk1) draftResultOk1.value = '';
          if (draftResultOk2) draftResultOk2.value = '';
          if (draftResultFail1) draftResultFail1.value = '';
          if (draftResultFail2) draftResultFail2.value = '';
        });
      }
      loadUrlPayPgOptions().then(function () {
        parseHidden();
        renderTable();
      }).catch(function () {
        parseHidden();
        renderTable();
      });
      var saveBtn2 = pane.querySelector('#hqPaymentOrchSaveBtn');
      if (saveBtn2 && !saveBtn2._hqPayCardCopySaveHook) {
        saveBtn2._hqPayCardCopySaveHook = true;
        saveBtn2.addEventListener('click', function () {
          writeHidden();
        }, true);
      }
    }
    if (url === '/hq/apiConfig' || url === '/hq/paymentOrchestration') {
      var dimm2 = document.getElementById('dimm');
      if (dimm2) dimm2.style.display = 'flex';
      window.PG_API.hqApiConfig().then(function (data) {
        if (data && (pane.querySelector('[name="baseUrl"]') || pane.querySelector('[name="urlPayPathTemplate"]') || pane.querySelector('[name="paymentProviderRegistryJson"]') || pane.querySelector('[name="payCurrencyScaleRulesJson"]') || pane.querySelector('[name="urlPayCardCopyConfigJson"]') || pane.querySelector('[name="urlPayTabTitleJson"]') || pane.querySelector('[name="urlPayDisplayFxJson"]'))) {
          ['baseUrl', 'authType', 'timeoutSec', 'memo', 'chillpayMerchantCode', 'chillpayApiKey', 'chillpayMd5Key', 'chillpayRouteNo', 'chillpaySandbox', 'recallIncludeFeeYn', 'settlementVatApplyYn',
            'apiBrokerDefaultFlowType', 'urlPayDefaultFlowType', 'urlPayPathTemplate',
            'apiBrokerInlineEnabledYn', 'apiBrokerRedirectEnabledYn', 'urlPayInlineEnabledYn', 'urlPayRedirectEnabledYn',
            'urlPayFormMode',
            'urlPayTabTitleJson',
            'urlPayFaviconUrl',
            'paymentProviderRegistryJson',
            'payCurrencyScaleRulesJson',
            'urlPayCardCopyConfigJson',
            'urlPayDisplayFxJson'
          ].forEach(function (k) {
            var el = pane.querySelector('[name="' + k + '"]');
            if (el && data[k] != null) el.value = data[k];
          });
        }
      }).catch(function () {}).finally(function () {
        if (dimm2) dimm2.style.display = 'none';
        if (url === '/hq/paymentOrchestration') {
          initHqPayCurrencyScaleRulesEditor(pane);
          initHqUrlPayFormChrome(pane);
          initHqUrlPayCardCopyEditor(pane);
        }
      });
      var hqApiSave = pane.querySelector('#hqApiConfigSaveBtn') || pane.querySelector('#hqPaymentOrchSaveBtn');
      if (hqApiSave) hqApiSave.addEventListener('click', function () {
        syncHqUrlPayFormChromeToHidden(pane);
        var fd = {};
        pane.querySelectorAll('input, select, textarea').forEach(function (el) { if (el.name) fd[el.name] = el.value; });
        if (dimm2) dimm2.style.display = 'flex';
        window.PG_API.hqApiConfigSave(fd).then(function () { alert('저장되었습니다.'); }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimm2) dimm2.style.display = 'none'; });
      });
      var pgJump = pane.querySelector('#hqApiConfigOpenPgLink');
      if (pgJump && !pgJump._hqPgJumpBound) {
        pgJump._hqPgJumpBound = true;
        pgJump.addEventListener('click', function () {
          if (typeof window.fnTopMenuMove === 'function') {
            window.fnTopMenuMove('/hq/pgApiMng', 'M0101', 'API연동설정');
          }
        });
      }
    }
    if (url === '/hq/urlPayDeploy') {
      var dimmUp = document.getElementById('dimm');
      var hqSnapDeploy = null;
      var tbodyUp = pane.querySelector('#hqUrlPayDeployPgTbody');
      var hiddenUp = pane.querySelector('#hqUrlPayDeployFxHidden');
      function escUrlPayDep(s) {
        return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
      }
      function parseFxObjUrlPayDep(raw) {
        try {
          var o = raw ? JSON.parse(raw) : {};
          return (o && typeof o === 'object') ? o : {};
        } catch (eFx) {
          return {};
        }
      }
      function normPgUrlPayDep(cd) {
        return String(cd || '').trim().toUpperCase();
      }
      function hasAnyUrlPayDeployDisplayMode() {
        if (!tbodyUp) return false;
        var sels = tbodyUp.querySelectorAll('.hq-upd-amode');
        for (var i = 0; i < sels.length; i++) {
          if (String(sels[i].value || '').toUpperCase() === 'DISPLAY') return true;
        }
        return false;
      }
      function setUrlPayDeployRowFxControlsEnabled(tr) {
        var am = tr.querySelector('.hq-upd-amode');
        var disp = am && String(am.value || '').toUpperCase() === 'DISPLAY';
        var tip = disp ? '' : '일반형: 총판(조직) 설정 통화로 결제되며 이 항목은 적용되지 않습니다.';
        ['hq-upd-dcur', 'hq-upd-scur', 'hq-upd-fx'].forEach(function (cls) {
          var el = tr.querySelector('.' + cls);
          if (el) {
            el.disabled = !disp;
            el.title = tip;
          }
        });
        var mn = tr.querySelector('.hq-upd-manual');
        var mr = tr.querySelector('.hq-upd-mrg');
        if (mn) {
          mn.disabled = !disp;
          mn.title = tip;
        }
        if (mr) {
          mr.disabled = !disp;
          mr.title = tip;
        }
      }
      function refreshUrlPayDeployGlobalFxControls() {
        var on = hasAnyUrlPayDeployDisplayMode();
        var gTip = on ? '' : 'DISPLAY 모드 PG가 없어 비활성화됩니다. 일반형만 있으면 총판 통화로 결제됩니다.';
        ['_urlPayFxUiEnabled', '_urlPayFxUiRefresh', '_urlPayFxUiTtl', '_urlPayFxUiMarginJpy', '_urlPayFxUiMarginUsd', '_urlPayFxUiMarginKrw', '_urlPayFxUiMarginThb'].forEach(function (nm) {
          var el = pane.querySelector('[name="' + nm + '"]');
          if (el) {
            el.disabled = !on;
            el.title = gTip;
          }
        });
      }
      function bindUrlPayDeployAmountModeChange() {
        if (!tbodyUp || tbodyUp._hqUrlPayDepAmodeBound) return;
        tbodyUp._hqUrlPayDepAmodeBound = true;
        tbodyUp.addEventListener('change', function (ev) {
          var t = ev.target;
          if (t && t.classList && t.classList.contains('hq-upd-amode')) {
            var tr = t.closest('tr');
            if (tr) setUrlPayDeployRowFxControlsEnabled(tr);
            refreshUrlPayDeployGlobalFxControls();
          }
        });
      }
      function renderUrlPayDeployPgTable(urlPayPgs, fxObj) {
        if (!tbodyUp) return;
        var pgSet = fxObj.pgSettings && typeof fxObj.pgSettings === 'object' ? fxObj.pgSettings : {};
        if (!urlPayPgs.length) {
          tbodyUp.innerHTML = '<tr><td colspan="7" class="text-muted text-center py-3 small">URL결제(Y)로 등록된 PG가 없습니다. API연동설정에서 연동용도를 확인하세요.</td></tr>';
          refreshUrlPayDeployGlobalFxControls();
          bindUrlPayDeployAmountModeChange();
          return;
        }
        var html = '';
        urlPayPgs.forEach(function (p) {
          var cd = String(p.pgCd || '').trim();
          if (!cd) return;
          var cdU = normPgUrlPayDep(cd);
          var row = pgSet[cdU] || pgSet[cd] || {};
          var mode = String(row.amountMode || 'STANDARD').toUpperCase() === 'DISPLAY' ? 'DISPLAY' : 'STANDARD';
          var disp = String(row.displayCurrency || 'JPY').toUpperCase();
          if (['JPY', 'USD', 'KRW', 'THB'].indexOf(disp) < 0) disp = 'JPY';
          var setc = String(row.settlementCurrency || 'THB').toUpperCase();
          if (['THB', 'USD', 'JPY', 'KRW'].indexOf(setc) < 0) setc = 'THB';
          var fxm = String(row.fxMode || 'AUTO').toUpperCase() === 'MANUAL' ? 'MANUAL' : 'AUTO';
          var manual = '';
          if (row.manualSettlementPerUnit != null && String(row.manualSettlementPerUnit).trim() !== '') {
            manual = String(row.manualSettlementPerUnit);
          } else if (row.manualThbPerUnit != null && String(row.manualThbPerUnit).trim() !== '') {
            manual = String(row.manualThbPerUnit);
          }
          var mrg = row.marginRate != null ? String(row.marginRate) : '';
          var nm = String(p.pgNm || cd).trim();
          html += '<tr data-pg-cd="' + escUrlPayDep(cdU) + '">' +
            '<td class="small">' + escUrlPayDep(nm + ' (' + cd + ')') + '</td>' +
            '<td><select class="form-select form-select-sm hq-upd-amode" data-pg="' + escUrlPayDep(cdU) + '">' +
            '<option value="STANDARD"' + (mode === 'STANDARD' ? ' selected' : '') + '>일반형</option>' +
            '<option value="DISPLAY"' + (mode === 'DISPLAY' ? ' selected' : '') + '>DISPLAY</option></select></td>' +
            '<td><select class="form-select form-select-sm hq-upd-dcur" data-pg="' + escUrlPayDep(cdU) + '">' +
            '<option value="THB"' + (disp === 'THB' ? ' selected' : '') + '>THB</option>' +
            '<option value="JPY"' + (disp === 'JPY' ? ' selected' : '') + '>JPY</option>' +
            '<option value="USD"' + (disp === 'USD' ? ' selected' : '') + '>USD</option>' +
            '<option value="KRW"' + (disp === 'KRW' ? ' selected' : '') + '>KRW</option></select></td>' +
            '<td><select class="form-select form-select-sm hq-upd-scur" data-pg="' + escUrlPayDep(cdU) + '">' +
            '<option value="THB"' + (setc === 'THB' ? ' selected' : '') + '>THB</option>' +
            '<option value="USD"' + (setc === 'USD' ? ' selected' : '') + '>USD</option>' +
            '<option value="JPY"' + (setc === 'JPY' ? ' selected' : '') + '>JPY</option>' +
            '<option value="KRW"' + (setc === 'KRW' ? ' selected' : '') + '>KRW</option></select></td>' +
            '<td><select class="form-select form-select-sm hq-upd-fx" data-pg="' + escUrlPayDep(cdU) + '">' +
            '<option value="AUTO"' + (fxm === 'AUTO' ? ' selected' : '') + '>자동(BOT)</option>' +
            '<option value="MANUAL"' + (fxm === 'MANUAL' ? ' selected' : '') + '>수동</option></select></td>' +
            '<td><input type="text" class="form-control form-control-sm font-monospace hq-upd-manual" data-pg="' + escUrlPayDep(cdU) + '" value="' + escUrlPayDep(manual) + '" placeholder="실결제/1표시" autocomplete="off"></td>' +
            '<td><input type="text" class="form-control form-control-sm font-monospace hq-upd-mrg" data-pg="' + escUrlPayDep(cdU) + '" value="' + escUrlPayDep(mrg) + '" placeholder="PG전체 마진(선택)" autocomplete="off"></td>' +
            '</tr>';
        });
        tbodyUp.innerHTML = html;
        tbodyUp.querySelectorAll('tr[data-pg-cd]').forEach(function (tr) {
          setUrlPayDeployRowFxControlsEnabled(tr);
        });
        refreshUrlPayDeployGlobalFxControls();
        bindUrlPayDeployAmountModeChange();
      }
      function collectUrlPayDeployFxJson() {
        var o = parseFxObjUrlPayDep(hqSnapDeploy && hqSnapDeploy.urlPayDisplayFxJson);
        o.marginByCurrency = Object.assign({}, o.marginByCurrency || {});
        var anyDisplay = hasAnyUrlPayDeployDisplayMode();
        var enEl = pane.querySelector('[name="_urlPayFxUiEnabled"]');
        o.enabled = anyDisplay && enEl && enEl.value === 'Y';
        var rs = parseInt(String((pane.querySelector('[name="_urlPayFxUiRefresh"]') || {}).value || '600'), 10);
        var ttl = parseInt(String((pane.querySelector('[name="_urlPayFxUiTtl"]') || {}).value || '600'), 10);
        o.refreshSeconds = isNaN(rs) ? 600 : Math.max(60, Math.min(3600, rs));
        o.quoteTtlSeconds = isNaN(ttl) ? 600 : Math.max(120, Math.min(3600, ttl));
        function parseMargUrlPayDep(el, def) {
          var t = el ? String(el.value || '').trim() : '';
          if (!t) return def;
          var n = parseFloat(t);
          return isNaN(n) || n < 0 ? def : n;
        }
        var mj = pane.querySelector('[name="_urlPayFxUiMarginJpy"]');
        var mu = pane.querySelector('[name="_urlPayFxUiMarginUsd"]');
        var mk = pane.querySelector('[name="_urlPayFxUiMarginKrw"]');
        var mtb = pane.querySelector('[name="_urlPayFxUiMarginThb"]');
        o.marginByCurrency.JPY = parseMargUrlPayDep(mj, 0);
        o.marginByCurrency.USD = parseMargUrlPayDep(mu, 0);
        o.marginByCurrency.KRW = parseMargUrlPayDep(mk, 0);
        o.marginByCurrency.THB = parseMargUrlPayDep(mtb, 0);
        var nextPg = {};
        tbodyUp.querySelectorAll('tr[data-pg-cd]').forEach(function (tr) {
          var cdU = tr.getAttribute('data-pg-cd');
          var am = tr.querySelector('.hq-upd-amode');
          var dc = tr.querySelector('.hq-upd-dcur');
          var sc = tr.querySelector('.hq-upd-scur');
          var fx = tr.querySelector('.hq-upd-fx');
          var mn = tr.querySelector('.hq-upd-manual');
          var mr = tr.querySelector('.hq-upd-mrg');
          var setC = sc && sc.value ? String(sc.value).trim().toUpperCase() : 'THB';
          if (['THB', 'USD', 'JPY', 'KRW'].indexOf(setC) < 0) setC = 'THB';
          var manualRaw = mn ? String(mn.value || '').trim() : '';
          var dCur = dc && dc.value ? String(dc.value).trim().toUpperCase() : 'JPY';
          if (['JPY', 'USD', 'KRW', 'THB'].indexOf(dCur) < 0) dCur = 'JPY';
          var st = {
            amountMode: am && am.value ? am.value : 'STANDARD',
            displayCurrency: dCur,
            settlementCurrency: setC,
            fxMode: fx && fx.value ? fx.value : 'AUTO',
            manualRaw: manualRaw,
            marginRate: mr ? String(mr.value || '').trim() : ''
          };
          if (st.amountMode === 'STANDARD') {
            nextPg[cdU] = { amountMode: 'STANDARD' };
          } else {
            var rowObj = {
              amountMode: 'DISPLAY',
              displayCurrency: st.displayCurrency,
              settlementCurrency: st.settlementCurrency,
              fxMode: st.fxMode,
              marginRate: st.marginRate
            };
            if (st.fxMode === 'MANUAL' && manualRaw) {
              if (st.settlementCurrency === 'THB') {
                rowObj.manualThbPerUnit = manualRaw;
              } else {
                rowObj.manualSettlementPerUnit = manualRaw;
              }
            }
            nextPg[cdU] = rowObj;
          }
        });
        o.pgSettings = nextPg;
        return JSON.stringify(o);
      }
      function applyUrlPayDeployFxToForm(fxObj) {
        var en = pane.querySelector('[name="_urlPayFxUiEnabled"]');
        var rs = pane.querySelector('[name="_urlPayFxUiRefresh"]');
        var ttl = pane.querySelector('[name="_urlPayFxUiTtl"]');
        var mj = pane.querySelector('[name="_urlPayFxUiMarginJpy"]');
        var mu = pane.querySelector('[name="_urlPayFxUiMarginUsd"]');
        var mk = pane.querySelector('[name="_urlPayFxUiMarginKrw"]');
        var mtb = pane.querySelector('[name="_urlPayFxUiMarginThb"]');
        if (en) en.value = fxObj.enabled === true ? 'Y' : 'N';
        if (rs) rs.value = fxObj.refreshSeconds != null ? String(fxObj.refreshSeconds) : '600';
        if (ttl) ttl.value = fxObj.quoteTtlSeconds != null ? String(fxObj.quoteTtlSeconds) : '600';
        var mb = fxObj.marginByCurrency || {};
        if (mj) mj.value = mb.JPY != null ? String(mb.JPY) : '0';
        if (mu) mu.value = mb.USD != null ? String(mb.USD) : '0';
        if (mk) mk.value = mb.KRW != null ? String(mb.KRW) : '0';
        if (mtb) mtb.value = mb.THB != null ? String(mb.THB) : '0';
      }
      if (dimmUp) dimmUp.style.display = 'flex';
      Promise.all([
        window.PG_API && window.PG_API.hqApiConfig ? window.PG_API.hqApiConfig().catch(function () { return null; }) : Promise.resolve(null),
        window.PG_API && window.PG_API.pgAgencyList ? window.PG_API.pgAgencyList().catch(function () { return []; }) : Promise.resolve([])
      ]).then(function (pair) {
        hqSnapDeploy = pair[0] || {};
        var list = Array.isArray(pair[1]) ? pair[1] : [];
        var urlPayPgs = list.filter(function (r) {
          return String(r.integUrlPayYn || '').toUpperCase() === 'Y' && String(r.useYn || 'Y').toUpperCase() === 'Y';
        });
        var fxObj = parseFxObjUrlPayDep(hqSnapDeploy.urlPayDisplayFxJson);
        applyUrlPayDeployFxToForm(fxObj);
        renderUrlPayDeployPgTable(urlPayPgs, fxObj);
        if (hiddenUp) hiddenUp.value = hqSnapDeploy.urlPayDisplayFxJson || '';
      }).finally(function () { if (dimmUp) dimmUp.style.display = 'none'; });
      var btnOpenUp = pane.querySelector('#hqUrlPayDeployOpenApiLink');
      if (btnOpenUp && !btnOpenUp._hqUrlPayDepApiBound) {
        btnOpenUp._hqUrlPayDepApiBound = true;
        btnOpenUp.addEventListener('click', function () {
          if (typeof window.fnTopMenuMove === 'function') {
            window.fnTopMenuMove('/hq/pgApiMng', 'M0101', 'API연동설정');
          }
        });
      }
      var saveUp = pane.querySelector('#hqUrlPayDeploySaveBtn');
      if (saveUp && !saveUp._hqUrlPayDeploySaveBound) {
        saveUp._hqUrlPayDeploySaveBound = true;
        saveUp.addEventListener('click', function () {
          if (!hqSnapDeploy) {
            alert('설정을 불러온 뒤 다시 시도하세요.');
            return;
          }
          var fd = {};
          Object.keys(hqSnapDeploy).forEach(function (k) { fd[k] = hqSnapDeploy[k]; });
          fd.urlPayDisplayFxJson = collectUrlPayDeployFxJson();
          if (hiddenUp) hiddenUp.value = fd.urlPayDisplayFxJson;
          var dimmS = document.getElementById('dimm');
          if (dimmS) dimmS.style.display = 'flex';
          window.PG_API.hqApiConfigSave(fd).then(function () {
            alert('저장되었습니다.');
            return window.PG_API.hqApiConfig();
          }).then(function (d) {
            if (d) hqSnapDeploy = d;
          }).catch(function (e) {
            alert(e && e.message ? e.message : '저장 실패');
          }).finally(function () { if (dimmS) dimmS.style.display = 'none'; });
        });
      }
    }
    if (url === '/hq/domainConfig') {
      var dimmDom = document.getElementById('dimm');
      var sid = tabId;
      function escDomCfg(s) {
        return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
      }
      function escDomAttr(s) {
        return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/'/g, '&#39;');
      }
      /** 표시·입력란용: 이미 http(s)면 유지, 아니면 https:// 부착 */
      function domainCfgDisplayWithHttps(raw) {
        var s = raw == null ? '' : String(raw).trim();
        if (!s) return '';
        if (/^https?:\/\//i.test(s)) return s;
        return 'https://' + s.replace(/^\/+/, '');
      }
      /** 저장 전: 비우면 '', 있으면 스킴 없을 때 https:// 부착 */
      function domainCfgNormalizeUrlForSave(raw) {
        var s = raw == null ? '' : String(raw).trim();
        if (!s) return '';
        return domainCfgDisplayWithHttps(s);
      }
      /** http/https만 허용. 스킴 없으면 https:// 부착 */
      function domainCfgSafeUrlHref(raw) {
        var s = raw == null ? '' : String(raw).trim();
        if (!s) return null;
        var tryHref = domainCfgDisplayWithHttps(s);
        try {
          var u = new URL(tryHref);
          if (u.protocol !== 'http:' && u.protocol !== 'https:') return null;
          return u.href;
        } catch (e1) {
          return null;
        }
      }
      /** 도메인구성설정 표시용: 유효 URL이면 새 탭 링크(표시문자에도 https:// 반영), 아니면 code만 */
      function domainCfgUrlCell(raw) {
        var s = raw == null ? '' : String(raw).trim();
        if (!s) return '<span class="text-muted">—</span>';
        var display = domainCfgDisplayWithHttps(s);
        var href = domainCfgSafeUrlHref(s);
        var label = escDomCfg(display);
        if (!href) {
          return '<code class="hq-domain-url-cell">' + label + '</code>';
        }
        return '<a href="' + escDomAttr(href) + '" target="_blank" rel="noopener noreferrer" class="hq-domain-url-link text-decoration-none"><code class="hq-domain-url-cell">' + label + '</code></a>';
      }
      function domainCfgHostLink(hostname) {
        var s = hostname == null ? '' : String(hostname).trim();
        if (!s) return '<span class="text-muted">—</span>';
        var display = domainCfgDisplayWithHttps(s);
        var href = domainCfgSafeUrlHref(s);
        if (!href) return '<span class="hq-domain-host-link font-monospace">' + escDomCfg(display) + '</span>';
        return '<a href="' + escDomAttr(href) + '" target="_blank" rel="noopener noreferrer" class="hq-domain-host-link font-monospace">' + escDomCfg(display) + '</a>';
      }
      function setDomInlineMsg(el, kind, text) {
        if (!el) return;
        el.textContent = text || '';
        el.className = 'small mb-2';
        if (kind === 'success') el.className += ' text-success';
        else if (kind === 'error') el.className += ' text-danger';
        else el.className += ' text-muted';
      }
      function renderOrgDomainTableBody(rows) {
        var tb = pane.querySelector('#hqDomainOrgTableTbody_' + sid);
        if (!tb) return;
        var list = Array.isArray(rows) ? rows : [];
        if (!list.length) {
          tb.innerHTML = '<tr><td colspan="9" class="text-center text-muted py-3">등록된 본사·총판 조직이 없습니다.</td></tr>';
          return;
        }
        var html = '';
        list.forEach(function (r, i) {
          var oid = escDomCfg(r.orgUnitId);
          var nm = escDomCfg(r.name || r.code || '');
          html += '<tr data-org-id="' + oid + '">' +
            '<td class="text-center text-muted">' + (i + 1) + '</td>' +
            '<td>' + escDomCfg(r.name) + '</td>' +
            '<td class="font-monospace small">' + escDomCfg(r.code) + '</td>' +
            '<td>' + escDomCfg(r.orgLevelLabel || r.orgLevel) + '</td>' +
            '<td>' + escDomCfg(r.domainSettingName) + '</td>' +
            '<td class="text-center">' + domainCfgUrlCell(r.orgDomainAdminUrl) + '</td>' +
            '<td class="text-center">' + domainCfgUrlCell(r.orgDomainApiUrl) + '</td>' +
            '<td class="text-center">' +
            '<button type="button" class="btn btn-outline-danger btn-sm py-0 px-1" data-action="hqDomainOrgDelete" data-org-unit-id="' + oid + '" data-org-label="' + escDomAttr(nm) + '">삭제</button>' +
            '</td>' +
            '<td class="small text-muted text-nowrap">' + escDomCfg(r.domainUrlsUpdatedAt ? String(r.domainUrlsUpdatedAt).replace('T', ' ').slice(0, 19) : '') + '</td>' +
            '</tr>';
        });
        tb.innerHTML = html;
      }
      function findOrgRowById(id) {
        var rows = pane._hqDomainOrgRows || [];
        var s = String(id || '');
        for (var i = 0; i < rows.length; i++) {
          if (String(rows[i].orgUnitId) === s) return rows[i];
        }
        return null;
      }
      function setOrgEditorEnabled(on) {
        ['hqDomainSettingName_' + sid, 'hqDomainOrgAdminUrl_' + sid, 'hqDomainOrgApiUrl_' + sid].forEach(function (id) {
          var el = pane.querySelector('#' + id);
          if (el) el.disabled = !on;
        });
        var saveB = pane.querySelector('#hqDomainOrgSaveBtn_' + sid);
        if (saveB) saveB.disabled = !on;
      }
      function fillOrgEditorFromRow(row) {
        var codeEl = pane.querySelector('#hqDomainOrgCode_' + sid);
        var lvEl = pane.querySelector('#hqDomainOrgLevel_' + sid);
        var nmEl = pane.querySelector('#hqDomainSettingName_' + sid);
        var adEl = pane.querySelector('#hqDomainOrgAdminUrl_' + sid);
        var apEl = pane.querySelector('#hqDomainOrgApiUrl_' + sid);
        if (!row) {
          if (codeEl) codeEl.value = '';
          if (lvEl) lvEl.value = '';
          if (nmEl) nmEl.value = '';
          if (adEl) adEl.value = '';
          if (apEl) apEl.value = '';
          return;
        }
        if (codeEl) codeEl.value = row.code || '';
        if (lvEl) lvEl.value = row.orgLevelLabel || row.orgLevel || '';
        if (nmEl) nmEl.value = row.domainSettingName || '';
        if (adEl) adEl.value = row.orgDomainAdminUrl ? domainCfgDisplayWithHttps(row.orgDomainAdminUrl) : '';
        if (apEl) apEl.value = row.orgDomainApiUrl ? domainCfgDisplayWithHttps(row.orgDomainApiUrl) : '';
      }
      function fillOrgSelect(rows) {
        var sel = pane.querySelector('#hqDomainOrgSelect_' + sid);
        if (!sel) return;
        var list = Array.isArray(rows) ? rows : [];
        var cur = sel.value;
        sel.innerHTML = '<option value="">— 업체를 선택하세요 —</option>' +
          list.map(function (r) {
            return '<option value="' + escDomCfg(r.orgUnitId) + '">' + escDomCfg(r.name || '') + '</option>';
          }).join('');
        if (cur && list.some(function (r) { return String(r.orgUnitId) === cur; })) sel.value = cur;
      }
      function renderSslDomainLinkage(link) {
        var box = pane.querySelector('#hqDomainSslLinkage_' + sid);
        if (!box) return;
        if (!link || typeof link !== 'object') {
          box.innerHTML = '<p class="text-muted mb-0">연동 요약을 불러오지 못했습니다.</p>';
          return;
        }
        var st = link.sslStatus || '—';
        var days = link.daysRemaining != null ? String(link.daysRemaining) : '—';
        var live = link.leLiveCertName ? String(link.leLiveCertName) : '—';
        var san = Array.isArray(link.sanDnsNames) ? link.sanDnsNames : [];
        var rows = Array.isArray(link.configuredHostRows) ? link.configuredHostRows : [];
        var miss = Array.isArray(link.hostsMissingFromCert) ? link.hostsMissingFromCert : [];
        var sanOnly = Array.isArray(link.sanWithoutConfiguredUrl) ? link.sanWithoutConfiguredUrl : [];
        var hint = link.linkageHint ? String(link.linkageHint) : '';
        var missAlert = '';
        if (miss.length) {
          missAlert = '<div class="alert alert-warning py-2 small mb-2" role="alert"><strong>인증서 SAN에 없는 호스트</strong> (URL은 저장됐으나 PEM의 SAN과 불일치)<ul class="mb-0 mt-1 ps-3">' +
            miss.map(function (m) {
              return '<li>' + domainCfgHostLink(m.hostname) + ' — ' + escDomCfg(m.source) + '</li>';
            }).join('') + '</ul></div>';
        }
        var tbl = '<div class="table-responsive mb-2"><table class="table table-sm table-bordered align-middle mb-0">' +
          '<thead class="table-light"><tr><th>호스트명</th><th>출처</th><th class="text-center text-nowrap" style="width:7rem">SAN 포함</th></tr></thead><tbody>';
        if (!rows.length) {
          tbl += '<tr><td colspan="3" class="text-muted text-center py-2">비교할 URL이 없습니다. 전사 URL 또는 본사·총판 URL을 입력하세요.</td></tr>';
        } else {
          rows.forEach(function (r) {
            var ok = r.inCertificate === true;
            tbl += '<tr><td class="text-center">' + domainCfgHostLink(r.hostname) + '</td><td>' + escDomCfg(r.source) + '</td><td class="text-center">' +
              (ok ? '<span class="badge bg-success">예</span>' : '<span class="badge bg-danger">아니오</span>') + '</td></tr>';
          });
        }
        tbl += '</tbody></table></div>';
        var sanBlock = san.length
          ? '<p class="small fw-semibold mb-2">인증서 SAN (' + san.length + ')</p>' +
            '<div class="table-responsive mb-2" style="max-height:240px;overflow:auto">' +
            '<table class="table table-sm table-bordered align-middle mb-0 text-center">' +
            '<thead class="table-light"><tr><th class="text-center" style="width:3.5rem">No.</th>' +
            '<th class="text-center">브라우저 호스트명 (SAN dNSName)</th></tr></thead><tbody>' +
            san.map(function (h, idx) {
              return '<tr><td class="text-center text-muted">' + (idx + 1) + '</td>' +
                '<td class="text-center">' + domainCfgHostLink(h) + '</td></tr>';
            }).join('') +
            '</tbody></table></div>'
          : '<p class="text-muted small mb-2">SAN 목록을 읽지 못했습니다. 서버운영관리에서 LE 경로를 확인하세요.</p>';
        var sanOnlyBlock = sanOnly.length
          ? '<p class="small text-muted mb-0">SAN에만 있고 도메인구성설정 URL에 없는 호스트: ' +
            sanOnly.map(function (h) { return domainCfgHostLink(h); }).join('<span class="text-muted">, </span>') + '</p>'
          : '';
        box.innerHTML =
          '<p class="small mb-2"><strong>PEM 상태</strong> ' + escDomCfg(st) +
          ' · <strong>LE 인증서 이름</strong> <code>' + escDomCfg(live) + '</code>' +
          ' · <strong>만료까지(일)</strong> ' + escDomCfg(days) +
          (link.notAfter ? (' · <span class="text-muted">notAfter ' + escDomCfg(String(link.notAfter)) + '</span>') : '') +
          '</p>' + missAlert + tbl + sanBlock + (hint ? '<p class="text-muted small mb-2">' + escDomCfg(hint) + '</p>' : '') + sanOnlyBlock;
      }
      if (dimmDom) dimmDom.style.display = 'flex';
      window.PG_API.hqDomainConfig().then(function (data) {
        if (!data) return;
        ['publicAdminSiteUrl', 'publicApiBaseUrl'].forEach(function (k) {
          var el = pane.querySelector('[name="' + k + '"]');
          if (el && data[k] != null && String(data[k]).trim()) el.value = domainCfgDisplayWithHttps(data[k]);
          else if (el && (data[k] == null || !String(data[k]).trim())) el.value = '';
        });
        pane._hqDomainOrgRows = data.orgDomainRows || [];
        fillOrgSelect(pane._hqDomainOrgRows);
        renderOrgDomainTableBody(pane._hqDomainOrgRows);
        setOrgEditorEnabled(false);
        fillOrgEditorFromRow(null);
        var hint = pane.querySelector('#hqDomainOrgHint_' + sid);
        if (hint) hint.textContent = '업체를 선택하면 입력란이 활성화됩니다.';
        renderSslDomainLinkage(data.sslDomainLinkage);
      }).catch(function () {
        renderOrgDomainTableBody([]);
        renderSslDomainLinkage(null);
      }).finally(function () { if (dimmDom) dimmDom.style.display = 'none'; });

      var gSave = pane.querySelector('#hqDomainGlobalSaveBtn_' + sid);
      if (gSave && !gSave._hqDomBound) {
        gSave._hqDomBound = true;
        gSave.addEventListener('click', function () {
          var gMsg = pane.querySelector('#hqDomainGlobalMsg_' + sid);
          setDomInlineMsg(gMsg, '', '');
          var fd = {
            publicAdminSiteUrl: domainCfgNormalizeUrlForSave((pane.querySelector('[name="publicAdminSiteUrl"]') || {}).value || ''),
            publicApiBaseUrl: domainCfgNormalizeUrlForSave((pane.querySelector('[name="publicApiBaseUrl"]') || {}).value || '')
          };
          if (dimmDom) dimmDom.style.display = 'flex';
          window.PG_API.hqDomainConfigSave(fd).then(function (res) {
            var paIn = pane.querySelector('[name="publicAdminSiteUrl"]');
            var pbIn = pane.querySelector('[name="publicApiBaseUrl"]');
            if (paIn) paIn.value = fd.publicAdminSiteUrl;
            if (pbIn) pbIn.value = fd.publicApiBaseUrl;
            setDomInlineMsg(gMsg, 'success', (res && res.message) ? res.message : '전사 URL이 저장되었습니다.');
            if (res && res.sslDomainLinkage) renderSslDomainLinkage(res.sslDomainLinkage);
          }).catch(function (e) {
            setDomInlineMsg(gMsg, 'error', e && e.message ? e.message : '저장 실패');
          }).finally(function () { if (dimmDom) dimmDom.style.display = 'none'; });
        });
      }

      var orgSel = pane.querySelector('#hqDomainOrgSelect_' + sid);
      if (orgSel && !orgSel._hqDomBound) {
        orgSel._hqDomBound = true;
        orgSel.addEventListener('change', function () {
          var oMsg = pane.querySelector('#hqDomainOrgMsg_' + sid);
          setDomInlineMsg(oMsg, '', '');
          var id = orgSel.value;
          if (!id) {
            setOrgEditorEnabled(false);
            fillOrgEditorFromRow(null);
            return;
          }
          var row = findOrgRowById(id);
          setOrgEditorEnabled(true);
          fillOrgEditorFromRow(row);
        });
      }

      var orgSave = pane.querySelector('#hqDomainOrgSaveBtn_' + sid);
      if (orgSave && !orgSave._hqDomBound) {
        orgSave._hqDomBound = true;
        orgSave.addEventListener('click', function () {
          var oMsg = pane.querySelector('#hqDomainOrgMsg_' + sid);
          setDomInlineMsg(oMsg, '', '');
          var selEl = pane.querySelector('#hqDomainOrgSelect_' + sid);
          var oid = selEl && selEl.value ? String(selEl.value).trim() : '';
          if (!oid) {
            setDomInlineMsg(oMsg, 'error', '업체를 먼저 선택하세요.');
            return;
          }
          var body = {
            orgUnitId: oid,
            domainSettingName: (pane.querySelector('#hqDomainSettingName_' + sid) || {}).value || '',
            orgDomainAdminUrl: domainCfgNormalizeUrlForSave((pane.querySelector('#hqDomainOrgAdminUrl_' + sid) || {}).value || ''),
            orgDomainApiUrl: domainCfgNormalizeUrlForSave((pane.querySelector('#hqDomainOrgApiUrl_' + sid) || {}).value || '')
          };
          if (dimmDom) dimmDom.style.display = 'flex';
          window.PG_API.hqDomainConfigOrgSave(body).then(function (res) {
            if (res && res.orgDomainRows) {
              pane._hqDomainOrgRows = res.orgDomainRows;
              fillOrgSelect(pane._hqDomainOrgRows);
              renderOrgDomainTableBody(pane._hqDomainOrgRows);
              fillOrgEditorFromRow(findOrgRowById(oid));
            }
            if (res && res.sslDomainLinkage) renderSslDomainLinkage(res.sslDomainLinkage);
            setDomInlineMsg(oMsg, 'success', (res && res.message) ? res.message : '도메인 설정이 저장되었습니다.');
          }).catch(function (e) {
            setDomInlineMsg(oMsg, 'error', e && e.message ? e.message : '저장 실패');
          }).finally(function () { if (dimmDom) dimmDom.style.display = 'none'; });
        });
      }

      if (!pane._hqDomainOrgTableDelBound) {
        pane._hqDomainOrgTableDelBound = true;
        pane.addEventListener('click', function (e) {
          var delBtn = e.target && e.target.closest ? e.target.closest('button[data-action="hqDomainOrgDelete"]') : null;
          if (!delBtn || !pane.contains(delBtn)) return;
          var delId = delBtn.getAttribute('data-org-unit-id') || '';
          var label = delBtn.getAttribute('data-org-label') || '';
          if (!delId) return;
          var msg1 = '[' + (label || delId) + '] 조직의 도메인 설정(설정 이름·관리자 URL·API URL)을 삭제합니다. 계속하시겠습니까?';
          var msg2 = '한 번 더 확인합니다. 삭제 후 입력 내용은 서버에서 비워집니다. 정말 삭제하시겠습니까?';
          if (typeof window.pgDoubleConfirm === 'function') {
            if (!window.pgDoubleConfirm(msg1, msg2)) return;
          } else if (!window.confirm(msg1) || !window.confirm(msg2)) {
            return;
          }
          var oMsg2 = pane.querySelector('#hqDomainOrgMsg_' + sid);
          setDomInlineMsg(oMsg2, '', '');
          if (dimmDom) dimmDom.style.display = 'flex';
          window.PG_API.hqDomainConfigOrgDelete({ orgUnitId: delId }).then(function (res) {
            if (res && res.orgDomainRows) {
              pane._hqDomainOrgRows = res.orgDomainRows;
              fillOrgSelect(pane._hqDomainOrgRows);
              renderOrgDomainTableBody(pane._hqDomainOrgRows);
              var selEl2 = pane.querySelector('#hqDomainOrgSelect_' + sid);
              var cur = selEl2 && selEl2.value ? String(selEl2.value).trim() : '';
              if (cur === delId) {
                fillOrgEditorFromRow(findOrgRowById(delId));
              }
            }
            if (res && res.sslDomainLinkage) renderSslDomainLinkage(res.sslDomainLinkage);
            setDomInlineMsg(oMsg2, 'success', (res && res.message) ? res.message : '도메인 설정을 삭제했습니다.');
          }).catch(function (err) {
            setDomInlineMsg(oMsg2, 'error', err && err.message ? err.message : '삭제 실패');
          }).finally(function () { if (dimmDom) dimmDom.style.display = 'none'; });
        });
      }
    }
    if (url === '/hq/serverManage') {
      function hqSrvClearTimers() {
        if (pane._serverManageTimer) {
          clearInterval(pane._serverManageTimer);
          pane._serverManageTimer = null;
        }
        if (pane._hqSrvCountdownTimer) {
          clearInterval(pane._hqSrvCountdownTimer);
          pane._hqSrvCountdownTimer = null;
        }
      }
      hqSrvClearTimers();
      var dimmSrv = document.getElementById('dimm');

      function hqSrvEsc(s) {
        return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
      }
      function hqSrvFmtGbFromMb(mb) {
        if (mb == null || mb === '') return '—';
        var n = Number(mb);
        if (isNaN(n) || n < 0) return '—';
        return (Math.round((n / 1024) * 1000) / 1000).toFixed(3).replace(/\.?0+$/, '') + ' GB';
      }
      function hqSrvMbToGbInput(mb) {
        if (mb === undefined || mb === null || mb === '') return '';
        var n = Number(mb);
        if (isNaN(n) || n <= 0) return '';
        var g = n / 1024;
        var s = g.toFixed(4).replace(/\.?0+$/, '');
        return s;
      }
      function hqSrvGbToMbContract(gbStr) {
        if (gbStr === undefined || gbStr === null) return null;
        var t = String(gbStr).trim().replace(',', '.');
        if (t === '') return null;
        var g = parseFloat(t);
        if (isNaN(g) || g < 0) return null;
        var mb = Math.round(g * 1024);
        return mb <= 0 ? null : mb;
      }
      function hqSrvGbToMbTrafficUsed(gbStr) {
        if (gbStr === undefined || gbStr === null) return null;
        var t = String(gbStr).trim().replace(',', '.');
        if (t === '') return null;
        var g = parseFloat(t);
        if (isNaN(g) || g < 0) return null;
        return Math.max(0, Math.round(g * 1024));
      }
      /** 분 입력 → 초(60~3600, DB server_manage_ui_refresh_sec) */
      function hqSrvMinToSecRefresh(minStr) {
        if (minStr === undefined || minStr === null) return null;
        var t = String(minStr).trim().replace(',', '.');
        if (t === '') return null;
        var m = parseFloat(t);
        if (isNaN(m) || m <= 0) return null;
        var sec = Math.round(m * 60);
        return Math.max(60, Math.min(3600, sec));
      }
      function hqSrvFmtBytes(n) {
        n = Number(n) || 0;
        if (n >= 1099511627776) return (n / 1099511627776).toFixed(2) + ' TB';
        if (n >= 1073741824) return (n / 1073741824).toFixed(2) + ' GB';
        if (n >= 1048576) return (n / 1048576).toFixed(2) + ' MB';
        if (n >= 1024) return (n / 1024).toFixed(2) + ' KB';
        return n + ' B';
      }
      function hqSrvFmtUptimeMs(ms) {
        ms = Number(ms) || 0;
        var s = Math.floor(ms / 1000);
        var d = Math.floor(s / 86400);
        s -= d * 86400;
        var h = Math.floor(s / 3600);
        s -= h * 3600;
        var m = Math.floor(s / 60);
        return d + '일 ' + h + '시간 ' + m + '분';
      }
      function hqSrvBadge(status) {
        if (status === 'danger') return '<span class="badge bg-danger">위험</span>';
        if (status === 'warn') return '<span class="badge bg-warning text-dark">주의</span>';
        return '<span class="badge bg-success">양호</span>';
      }
      function hqSrvProgress(pct, level) {
        pct = Math.max(0, Math.min(100, Number(pct) || 0));
        var cls = level === 'danger' ? 'bg-danger' : level === 'warn' ? 'bg-warning' : 'bg-success';
        return '<div class="progress mt-2" style="height:10px"><div class="progress-bar ' + cls + '" role="progressbar" style="width:' + pct + '%"></div></div>';
      }
      function updateHqMonCrossOriginHint() {
        var el = pane.querySelector('#hqMonCrossOriginHint');
        if (!el) return;
        var pageH = (window.location && window.location.hostname) || '';
        var apiRoot = '';
        try {
          apiRoot = (typeof window.PG_API_BASE === 'string' ? window.PG_API_BASE : '').replace(/\/$/, '').trim();
          if (!apiRoot) apiRoot = (window.PG_PUBLIC_ICOPAY_API || 'https://api.icopay.co.kr').replace(/\/$/, '');
          var u = new URL(apiRoot);
          if (u.hostname && pageH && u.hostname !== pageH && pageH !== 'localhost' && pageH !== '127.0.0.1') {
            el.classList.remove('d-none');
            el.className = 'alert alert-info py-2 small mb-0 mt-2';
            el.innerHTML = '<strong>구조 안내 (NOTI 대비)</strong> ' +
              '<a href="https://github.com/ziobiz/NOTI" target="_blank" rel="noopener">NOTI</a> 서버관리는 Node가 <em>같은 출처</em>로 HTML을 내려 세션만으로 조회합니다. ' +
              'PG 관리자는 브라우저가 <code>' + hqSrvEsc(u.origin) + '</code> 로 API를 호출합니다. ' +
              '목록이 비면 CORS·방화벽·최신 JAR를 확인하거나, <strong>API와 동일 호스트</strong>에서 관리자를 여는 것을 권장합니다.';
            return;
          }
        } catch (e0) { /* ignore */ }
        el.classList.add('d-none');
        el.innerHTML = '';
      }
      function hqMonStat(k, v, sub, danger) {
        return '<div class="hq-mon-stat' + (danger ? ' danger' : '') + '">' +
          '<div class="hq-mon-stat-k">' + hqSrvEsc(k) + '</div>' +
          '<div class="hq-mon-stat-v">' + hqSrvEsc(v) + '</div>' +
          (sub ? '<div class="hq-mon-stat-sub">' + sub + '</div>' : '') +
          '</div>';
      }
      function hqLoadChartJsOnce() {
        if (window.Chart) return Promise.resolve();
        if (window._pgChartJsLoading) return window._pgChartJsLoading;
        window._pgChartJsLoading = new Promise(function (resolve, reject) {
          var s = document.createElement('script');
          s.src = 'https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js';
          s.async = true;
          s.onload = function () { resolve(); };
          s.onerror = function () { reject(new Error('Chart.js load failed')); };
          document.head.appendChild(s);
        });
        return window._pgChartJsLoading;
      }
      function hqUsageFormatSummary(sum, grain) {
        if (!sum) return '';
        var g = grain || 'daily';
        var lines = [];
        lines.push('<div class="fw-semibold mb-2">[' + hqSrvEsc(sum.grainLabel || '일간') + '] 현황 요약 <span class="text-muted fw-normal">(아래 그래프와 동일 데이터)</span></div>');
        if (!sum.hasData) {
          lines.push('<p class="text-muted mb-2">아직 누적 데이터가 거의 없습니다. 앱이 서버에서 수집(기본 10분 간격)을 수행하면 일별로 쌓입니다.</p>');
        }
        lines.push('<ul class="mb-0 ps-3">');
        lines.push('<li>그래프 구간 수: <strong>' + (sum.daysInChart != null ? sum.daysInChart : '—') + '</strong>' +
          (g === 'daily' && sum.maxChartDays ? ' (일간 최대 ' + sum.maxChartDays + '일)' : '') + '</li>');
        lines.push('<li>최근 7일 트래픽 합: <strong>' + hqSrvEsc(String(sum.trafficTotalLast7DaysMb != null ? sum.trafficTotalLast7DaysMb : '—')) + '</strong> MB</li>');
        lines.push('<li>그래프 기간 트래픽 합: <strong>' + hqSrvEsc(String(sum.trafficTotalPeriodMb != null ? sum.trafficTotalPeriodMb : '—')) + '</strong> MB</li>');
        var liRecent = '<li>가장 최근 일(' + hqSrvEsc(sum.latestDate || '—') + ') 트래픽 <strong>' +
          hqSrvEsc(String(sum.latestTrafficMb != null ? sum.latestTrafficMb : '—')) + '</strong> MB';
        if (sum.prevTrafficMb != null) {
          liRecent += ', 전일 <strong>' + hqSrvEsc(String(sum.prevTrafficMb)) + '</strong> MB';
        }
        if (sum.trafficDeltaMb != null) {
          liRecent += ', 증감 <strong>' + hqSrvEsc(String(sum.trafficDeltaMb)) + '</strong> MB';
          if (sum.trafficDeltaPct != null) {
            liRecent += ' (<strong>' + hqSrvEsc(String(sum.trafficDeltaPct)) + '</strong>%)';
          }
        }
        liRecent += '</li>';
        lines.push(liRecent);
        lines.push('<li>최근 31일 기준 일일 트래픽 최대: <strong>' + hqSrvEsc(String(sum.maxDayTrafficMb != null ? sum.maxDayTrafficMb : '—')) + '</strong> MB' +
          (sum.maxDayTrafficDate ? ' <span class="text-muted">(' + hqSrvEsc(sum.maxDayTrafficDate) + ')</span>' : '') + '</li>');
        lines.push('<li>일평균 트래픽(트래픽이 있었던 날만): <strong>' +
          hqSrvEsc(sum.avgDailyTrafficMb != null ? String(sum.avgDailyTrafficMb) : '—') + '</strong> MB</li>');
        lines.push('<li>메모리 일일 피크(%): 그래프 최근 값 <strong>' + hqSrvEsc(String(sum.memoryLatestPeakPct != null ? sum.memoryLatestPeakPct : '—')) +
          '</strong>%, 기간 최대 <strong>' + hqSrvEsc(String(sum.memoryPeriodMaxPeakPct != null ? sum.memoryPeriodMaxPeakPct : '—')) + '</strong>% (오른쪽 붉은 그래프)</li>');
        lines.push('</ul>');
        return lines.join('');
      }
      function refreshHqUsageCharts(pane, grain) {
        var sec = pane.querySelector('#hqSrvUsageSection');
        if (!sec) return;
        hqLoadChartJsOnce().then(function () {
          return window.PG_API.hqServerUsage(grain || 'daily');
        }).then(function (payload) {
          pane._hqUsageGrain = grain || 'daily';
          var labels = payload.labels || [];
          var tGb = payload.trafficSeriesGb || [];
          var mem = payload.memoryPeakSeriesPct || [];
          var sum = payload.summary || {};
          var c1 = pane.querySelector('#hqUsageChartMixed');
          var c2 = pane.querySelector('#hqUsageChartMem');
          if (pane._hqUsageChartMixed) {
            try { pane._hqUsageChartMixed.destroy(); } catch (e1) { /* ignore */ }
            pane._hqUsageChartMixed = null;
          }
          if (pane._hqUsageChartMem) {
            try { pane._hqUsageChartMem.destroy(); } catch (e2) { /* ignore */ }
            pane._hqUsageChartMem = null;
          }
          if (c1 && window.Chart) {
            pane._hqUsageChartMixed = new Chart(c1.getContext('2d'), {
              type: 'bar',
              data: {
                labels: labels,
                datasets: [
                  {
                    type: 'bar',
                    label: '트래픽 (송수신 합, GB)',
                    data: tGb,
                    backgroundColor: 'rgba(13, 110, 253, 0.45)',
                    borderColor: 'rgba(13, 110, 253, 0.9)',
                    borderWidth: 1,
                    yAxisID: 'y'
                  },
                  {
                    type: 'line',
                    label: '메모리 피크 (%)',
                    data: mem,
                    borderColor: 'rgb(220, 53, 69)',
                    backgroundColor: 'rgba(220, 53, 69, 0.06)',
                    borderWidth: 2,
                    tension: 0.2,
                    pointRadius: 2,
                    yAxisID: 'y1'
                  }
                ]
              },
              options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: { mode: 'index', intersect: false },
                plugins: { legend: { position: 'bottom' } },
                scales: {
                  y: {
                    type: 'linear',
                    position: 'left',
                    title: { display: true, text: 'GB' },
                    beginAtZero: true
                  },
                  y1: {
                    type: 'linear',
                    position: 'right',
                    min: 0,
                    max: 100,
                    title: { display: true, text: '%' },
                    grid: { drawOnChartArea: false }
                  }
                }
              }
            });
          }
          if (c2 && window.Chart) {
            pane._hqUsageChartMem = new Chart(c2.getContext('2d'), {
              type: 'line',
              data: {
                labels: labels,
                datasets: [{
                  label: '메모리 피크 (%)',
                  data: mem,
                  borderColor: 'rgb(220, 53, 69)',
                  backgroundColor: 'rgba(220, 53, 69, 0.12)',
                  borderWidth: 2,
                  tension: 0.2,
                  fill: true,
                  pointRadius: 2
                }]
              },
              options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom' } },
                scales: {
                  y: {
                    min: 0,
                    max: 100,
                    title: { display: true, text: '%' }
                  }
                }
              }
            });
          }
          var sumEl = pane.querySelector('#hqUsageSummary');
          if (sumEl) sumEl.innerHTML = hqUsageFormatSummary(sum, pane._hqUsageGrain);
        }).catch(function () {
          var sumEl = pane.querySelector('#hqUsageSummary');
          if (sumEl) sumEl.innerHTML = '<p class="text-danger mb-0 small">차트 데이터를 불러오지 못했습니다. ADMIN·<code>/api/hq/serverUsage</code>·최신 JAR·DB V45를 확인하세요.</p>';
        });
      }
      function renderHqServerDashboard(data) {
        var genEl = pane.querySelector('#hqSrvGeneratedAt');
        var rawEl = pane.querySelector('#hqSrvJsonRaw');
        var cardsEl = pane.querySelector('#hqSrvCards');
        var alertEl = pane.querySelector('#hqSrvAlerts');
        var intEl = pane.querySelector('#hqSrvIntervalSec');
        updateHqMonCrossOriginHint();
        if (rawEl) {
          try { rawEl.textContent = JSON.stringify(data, null, 2); } catch (e) { rawEl.textContent = String(data); }
        }
        if (genEl) genEl.textContent = '조회 시각: ' + (data && data.generatedAt ? data.generatedAt : '—');
        if (intEl) {
          var secI = (data && data.uiAutoRefreshSeconds > 0) ? data.uiAutoRefreshSeconds : 120;
          var mi = Math.floor(secI / 60);
          var sc = secI % 60;
          intEl.textContent = sc === 0 ? (mi + '분') : (mi > 0 ? (mi + '분 ' + sc + '초') : (secI + '초'));
        }
        if (alertEl) {
          var alerts = data && data.health && Array.isArray(data.health.alerts) ? data.health.alerts : [];
          if (!alerts.length) alertEl.innerHTML = '';
          else {
            alertEl.innerHTML = '<div class="alert alert-danger py-2 small mb-2" role="alert"><strong>헬스 경고</strong><ul class="mb-0 mt-1 ps-3">' +
              alerts.map(function (a) { return '<li>' + hqSrvEsc(a) + '</li>'; }).join('') + '</ul></div>';
          }
        }
        if (!cardsEl) return;
        var host = (data && data.host) || {};
        var jvm = (data && data.jvm) || {};
        if (!data || !host || Object.keys(host).length === 0) {
          var errMsg = (data && data.error) ? String(data.error) : '데이터가 없습니다.';
          cardsEl.innerHTML = '<div class="hq-mon-card border-danger">' +
            '<h3 class="text-danger">대시보드 데이터를 불러오지 못했습니다</h3>' +
            '<p class="small mb-2">' + hqSrvEsc(errMsg) + '</p>' +
            '<p class="small text-muted mb-0">' +
            '[요약 새로고침]을 누르고, F12 Network에서 <code>/api/hq/serverManage</code> 응답을 확인하세요. ' +
            'ADMIN 권한·API 기준 URL·CORS(최신 JAR)를 점검하세요.</p></div>';
          return;
        }
        var disk = (data && data.disk) || {};
        var ssl = (data && data.ssl) || {};
        var nginx = (data && data.nginxStub) || {};
        var certbot = (data && data.certbot) || {};
        var health = (data && data.health) || {};
        var tm = host.memoryTotalMb != null ? host.memoryTotalMb : 0;
        var am = host.memoryAvailableMb != null ? host.memoryAvailableMb : 0;
        var hpct = tm > 0 ? Math.round(((tm - am) / tm) * 1000) / 10 : 0;
        var heapPct = Number(jvm.heapUsedPct) || 0;
        var dp = disk.ok ? (Number(disk.usedPct) || 0) : null;
        var files = certbot.renewalConfFiles || [];
        var timer = certbot.certbotTimer || {};
        var sslDays = ssl.status === 'OK' ? Number(ssl.daysRemaining) : null;
        var sslDayCls = sslDays != null && sslDays <= 7 ? ' hq-mon-ssl-warn-danger' : sslDays != null && sslDays <= 30 ? ' hq-mon-ssl-warn' : '';
        var stats = [];
        stats.push(hqMonStat('호스트명', host.hostname || '—', host.osFamily ? (host.osFamily + ' · ' + (host.osVersion || '')) : '', !!host.error));
        stats.push(hqMonStat('시스템 메모리', host.error ? '—' : (hpct + '%'), host.error ? hqSrvEsc(host.error) : ('가용 ' + am + ' / 총 ' + tm + ' MB'), hpct >= 90));
        stats.push(hqMonStat('JVM 힙', (jvm.heapUsedMb != null ? jvm.heapUsedMb : '—') + ' / ' + (jvm.heapMaxMb != null ? jvm.heapMaxMb : '—') + ' MB', 'Java ' + (jvm.javaVersion || '—'), heapPct >= 92));
        stats.push(hqMonStat('Load(1m) · CPU', jvm.systemLoadAverage != null ? String(jvm.systemLoadAverage) : '—', '논리 ' + (jvm.cpuCount || '—') + ' 코어', false));
        stats.push(hqMonStat('업타임', hqSrvFmtUptimeMs(jvm.uptimeMs), '', false));
        stats.push(hqMonStat('디스크 사용', dp != null ? (dp + '%') : '—', disk.ok ? hqSrvFmtBytes(disk.usedBytes) + ' / ' + hqSrvFmtBytes(disk.totalBytes) : hqSrvEsc(disk.error || '조회 불가'), dp != null && dp >= 90));
        stats.push(hqMonStat('certbot.timer', timer.active || '—', 'renewal .conf ' + files.length + '개', (timer.active || '').toLowerCase() !== 'active'));
        var gridHtml = '<div class="hq-mon-grid">' + stats.join('') + '</div>';
        var resolvedPath = (data && data.sslResolvedPath) ? String(data.sslResolvedPath) : '';
        var cfgPath = (data && data.serverManageSslCertPath) ? String(data.serverManageSslCertPath) : '';
        var leDom = (data && data.serverManageSslLeDomain) ? String(data.serverManageSslLeDomain) : '';
        var sslDl = '';
        if (ssl.status === 'OK') {
          var days = Number(ssl.daysRemaining);
          var barPct = Math.min(100, Math.max(0, (days / 90) * 100));
          var lv = days < 14 ? 'danger' : days < 30 ? 'warn' : 'ok';
          sslDl =
            '<dl class="hq-mon-ssl-dl">' +
            '<dt>실제 읽은 경로</dt><dd>' + hqSrvEsc(resolvedPath || cfgPath || '—') + '</dd>' +
            '<dt>DB 저장 경로</dt><dd>' + hqSrvEsc(cfgPath || '—') + '</dd>' +
            '<dt>LE live 폴더명</dt><dd>' + hqSrvEsc(leDom || '—') + '</dd>' +
            '<dt>Subject</dt><dd class="text-break">' + hqSrvEsc(ssl.subjectDn) + '</dd>' +
            '<dt>Issuer</dt><dd class="text-break">' + hqSrvEsc(ssl.issuerDn) + '</dd>' +
            '<dt>유효 기간</dt><dd>' + hqSrvEsc(ssl.notBefore) + ' ~ ' + hqSrvEsc(ssl.notAfter) + '</dd>' +
            '<dt>잔여 일수</dt><dd class="' + sslDayCls.replace(/^\s+/, '') + '"><strong>' + days + '</strong> 일</dd>' +
            '<dt>SHA-256</dt><dd class="font-monospace small text-break">' + hqSrvEsc(ssl.fingerprintSha256 || '—') + '</dd>' +
            '</dl>' + hqSrvProgress(barPct, lv);
          var sanList = Array.isArray(ssl.sanDnsNames) ? ssl.sanDnsNames : [];
          if (sanList.length) {
            sslDl += '<p class="small fw-semibold mt-2 mb-2">SAN — 브라우저 호스트명 (dNSName)</p>' +
              '<div class="table-responsive" style="max-height:200px;overflow:auto">' +
              '<table class="table table-sm table-bordered align-middle mb-0 text-center">' +
              '<thead class="table-light"><tr><th class="text-center" style="width:3.5rem">No.</th>' +
              '<th class="text-center">호스트명</th></tr></thead><tbody>' +
              sanList.map(function (h, idx) {
                return '<tr><td class="text-center text-muted">' + (idx + 1) + '</td>' +
                  '<td class="text-center font-monospace small">' + hqSrvEsc(h) + '</td></tr>';
              }).join('') +
              '</tbody></table></div>';
          }
        } else {
          sslDl = '<p class="small text-warning mb-2">' + hqSrvEsc(ssl.detail || ssl.status || '—') + '</p>' +
            '<dl class="hq-mon-ssl-dl"><dt>실제 읽은 경로</dt><dd>' + hqSrvEsc(resolvedPath || '—') + '</dd>' +
            '<dt>환경변수</dt><dd><code>PG_SSL_CERT_PATH</code> (선택)</dd></dl>';
        }
        var sslGuide = (data && data.sslOpsGuide) || {};
        var sslGuideHtml = '';
        if (sslGuide.dnsProviderNote || sslGuide.leSanNote || sslGuide.cloudflareNote) {
          sslGuideHtml = '<details class="mt-2 small"><summary class="text-muted user-select-none">운영 안내 (DNS·SAN·프록시)</summary>' +
            '<ul class="text-muted mb-0 mt-1 ps-3">' +
            (sslGuide.dnsProviderNote ? '<li>' + hqSrvEsc(sslGuide.dnsProviderNote) + '</li>' : '') +
            (sslGuide.leSanNote ? '<li>' + hqSrvEsc(sslGuide.leSanNote) + '</li>' : '') +
            (sslGuide.cloudflareNote ? '<li>' + hqSrvEsc(sslGuide.cloudflareNote) + '</li>' : '') +
            '</ul></details>';
        }
        var sslCard = '<div class="hq-mon-card">' +
          '<h3>SSL 인증서</h3>' +
          '<p class="hq-mon-card-desc">Let’s Encrypt <code>fullchain.pem</code> 를 읽어 만료·SAN·지문을 표시합니다. 상단 폼의 LE live 폴더명(인증서 이름)을 저장하면 경로가 맞춰집니다. 도메인 URL과 SAN 대조는 <strong>도메인구성설정</strong> 화면을 사용하세요.</p>' +
          sslDl + sslGuideHtml + '</div>';
        var cbHtml = '<h3>Certbot · 갱신</h3>' +
          '<p class="hq-mon-card-desc"><code>certbot.timer</code> 가 주기적으로 <code>certbot renew</code> 를 실행합니다. 만료 30일 전부터 갱신이 시도됩니다. 서브도메인 추가 시에는 수동으로 <code>certbot --nginx -d …</code> 로 인증서를 확장한 뒤 Nginx를 리로드하세요.</p>' +
          '<p class="small mb-1"><strong>timer</strong> ' + hqSrvEsc(timer.active || '—') + '</p>' +
          '<p class="small text-muted text-break mb-2">다음 실행(원시): ' + hqSrvEsc(timer.next || '—') + '</p>' +
          '<p class="small fw-semibold mb-1">renewal/*.conf (' + files.length + ')</p>' +
          '<ul class="small mb-0 ps-3" style="max-height:120px;overflow:auto">' +
          files.slice(0, 40).map(function (f) { return '<li>' + hqSrvEsc(f) + '</li>'; }).join('') +
          (files.length > 40 ? '<li>… 외 ' + (files.length - 40) + '개</li>' : '') + '</ul>';
        var nxBody = '<p class="small mb-1">상태: <strong>' + hqSrvEsc(nginx.status) + '</strong></p>';
        if (nginx.bodyPreview) nxBody += '<pre class="small bg-light border rounded p-2 mb-0" style="max-height:140px;overflow:auto">' + hqSrvEsc(nginx.bodyPreview) + '</pre>';
        else if (nginx.detail) nxBody += '<p class="small text-muted mb-0">' + hqSrvEsc(nginx.detail) + '</p>';
        if (data && data.nginxStubStatusUrlConfigured === false && nginx.status === 'SKIPPED') {
          nxBody = '<p class="small text-muted mb-0">stub_status URL 미설정 (<code>NGINX_STUB_STATUS_URL</code> 또는 <code>app.serverManage.nginxStubStatusUrl</code>).</p>';
        }
        var nxHtml = '<h3>Nginx stub</h3><p class="hq-mon-card-desc">stub_status 연동 시 활성 접속 등을 표시합니다.</p>' + nxBody;
        var hRows = health.rows || [];
        var tableRows = hRows.map(function (r) {
          var crit = (r && r.criteria != null && String(r.criteria) !== '') ? String(r.criteria) : '—';
          return '<tr><td>' + hqSrvEsc(r.label) + '</td><td class="hq-mon-health-criteria text-muted">' + hqSrvEsc(crit) + '</td><td>' + hqSrvEsc(r.value) + '</td><td>' + hqSrvBadge(r.status) + '</td></tr>';
        }).join('');
        var ctr = (data && data.serverManageContract) || {};
        var sugMb = data && data.serverManageSuggestedTrafficUsedMb;
        var trUsedCard = ctr.trafficUsedMb != null
          ? hqSrvFmtGbFromMb(ctr.trafficUsedMb)
          : (sugMb != null && sugMb > 0 ? ('미저장 · 앱 수집 추정 ' + hqSrvFmtGbFromMb(sugMb)) : '미입력');
        var ctrCard = '<div class="hq-mon-card hq-mon-contract-card">' +
          '<h3>호스팅 약정</h3>' +
          '<p class="hq-mon-card-desc">상단 <strong>호스팅 약정</strong> 폼에서 저장한 값입니다. 표시는 GB이며 서버에는 MB로 저장됩니다.</p>' +
          '<p class="small mb-1">디스크 약정: <strong>' + hqSrvFmtGbFromMb(ctr.diskMb) + '</strong> · 트래픽 약정: <strong>' +
          (ctr.trafficMb != null ? hqSrvFmtGbFromMb(ctr.trafficMb) + ' (기간당)' : '—') + '</strong></p>' +
          '<p class="small text-muted mb-1">트래픽 누적 입력: <strong>' + trUsedCard + '</strong></p>' +
          '<p class="small text-muted mb-0">약정기간: ' + hqSrvEsc((ctr.periodStart || '—') + ' ~ ' + (ctr.periodEnd || '—')) + '</p></div>';
        var healthHtml = '<div class="hq-mon-card hq-mon-health-table">' +
          '<h3>헬스 요약</h3>' +
          '<p class="hq-mon-card-desc">일반 항목은 NOTI와 동일한 비율 임계치입니다. <strong>약정 디스크·트래픽</strong> 행은 약정(GB) 대비 사용률(주의 ≥75%, 위험 ≥90%)입니다.</p>' +
          '<div class="table-responsive"><table class="table table-sm table-bordered align-middle mb-0">' +
          '<thead class="table-light"><tr><th>항목</th><th>양호·주의·위험 기준</th><th>값</th><th style="width:88px">상태</th></tr></thead><tbody>' + tableRows + '</tbody></table></div>' +
          '<p class="small text-muted mt-2 mb-0">종합: ' + hqSrvBadge(health.worstStatus || 'ok') + '</p></div>';
        cardsEl.innerHTML = gridHtml + sslCard +
          '<div class="hq-mon-row2"><div class="hq-mon-card">' + cbHtml + '</div><div class="hq-mon-card">' + nxHtml + '</div></div>' +
          ctrCard + healthHtml;
        if (typeof requestAnimationFrame === 'function') {
          requestAnimationFrame(function () { refreshHqUsageCharts(pane, pane._hqUsageGrain || 'daily'); });
        } else {
          setTimeout(function () { refreshHqUsageCharts(pane, pane._hqUsageGrain || 'daily'); }, 0);
        }
      }
      function applyServerFormFromSummary(data) {
        if (!data) return;
        ['serverManageSslCertPath', 'serverManageSslLeDomain', 'serverManageContractStart', 'serverManageContractEnd'].forEach(function (k) {
          var inp = pane.querySelector('[name="' + k + '"]');
          if (!inp) return;
          var v = data[k];
          if (v === undefined || v === null || v === '') inp.value = '';
          else inp.value = v;
        });
        var refInp = pane.querySelector('[name="serverManageUiRefreshMin"]');
        if (refInp) {
          var rv = data.serverManageUiRefreshSec;
          if (rv !== undefined && rv !== null && rv !== '') {
            var sec0 = Number(rv);
            if (!isNaN(sec0) && sec0 >= 15) {
              var minVal = sec0 / 60;
              refInp.value = (Math.round(minVal * 100) / 100).toString().replace(/\.?0+$/, '');
            } else refInp.value = '';
          } else refInp.value = '';
        }
        var dIn = pane.querySelector('[name="serverManageContractDiskGb"]');
        if (dIn) dIn.value = hqSrvMbToGbInput(data.serverManageContractDiskMb);
        var tIn = pane.querySelector('[name="serverManageContractTrafficGb"]');
        if (tIn) tIn.value = hqSrvMbToGbInput(data.serverManageContractTrafficMb);
        var uIn = pane.querySelector('[name="serverManageTrafficUsedGb"]');
        if (uIn) {
          var tu = data.serverManageTrafficUsedMb;
          if (tu !== undefined && tu !== null && tu !== '') {
            uIn.value = hqSrvMbToGbInput(tu);
          } else if (data.serverManageSuggestedTrafficUsedMb != null && data.serverManageSuggestedTrafficUsedMb > 0) {
            uIn.value = hqSrvMbToGbInput(data.serverManageSuggestedTrafficUsedMb);
          } else {
            uIn.value = '';
          }
        }
      }
      function scheduleDataRefresh() {
        hqSrvClearTimers();
        var cb = pane.querySelector('#hqSrvAutoRefresh');
        var auto = !cb || cb.checked;
        var sec = Math.max(15, pane._hqSrvRefreshSec || 120);
        if (auto) {
          pane._serverManageTimer = setInterval(function () { loadServerSummary(false); }, sec * 1000);
        }
        pane._hqSrvNextRefreshAt = Date.now() + sec * 1000;
        pane._hqSrvCountdownTimer = setInterval(function () {
          var el = pane.querySelector('#hqSrvCountdown');
          var box = pane.querySelector('#hqSrvAutoRefresh');
          if (!el) return;
          if (!box || !box.checked) {
            el.textContent = '자동 갱신 꺼짐 · [요약 새로고침]으로 수동 조회';
            el.className = 'small fw-semibold text-secondary';
            return;
          }
          el.className = 'small fw-semibold text-primary';
          var rem = Math.max(0, Math.ceil((pane._hqSrvNextRefreshAt - Date.now()) / 1000));
          if (rem >= 60) {
            el.textContent = '다음 자동 갱신까지 약 ' + Math.floor(rem / 60) + '분 ' + (rem % 60) + '초';
          } else {
            el.textContent = '다음 자동 갱신까지 약 ' + rem + '초';
          }
        }, 1000);
      }
      function loadServerSummary(showDimm) {
        if (showDimm && dimmSrv) dimmSrv.style.display = 'flex';
        window.PG_API.hqServerManage().then(function (data) {
          applyServerFormFromSummary(data);
          renderHqServerDashboard(data);
          pane._hqSrvRefreshSec = (data && data.uiAutoRefreshSeconds > 0) ? data.uiAutoRefreshSeconds : 120;
          scheduleDataRefresh();
        }).catch(function (err) {
          hqSrvClearTimers();
          renderHqServerDashboard({
            error: err && err.message ? err.message : '조회 실패 (ADMIN 권한·네트워크 확인)',
            health: { alerts: [err && err.message ? err.message : '조회 실패'], rows: [], worstStatus: 'danger' }
          });
          var cEl = pane.querySelector('#hqSrvCountdown');
          if (cEl) {
            cEl.textContent = '조회 실패 — [요약 새로고침]을 눌러 주세요';
            cEl.className = 'small text-danger fw-semibold';
          }
        }).finally(function () {
          if (showDimm && dimmSrv) dimmSrv.style.display = 'none';
        });
      }
      loadServerSummary(true);
      if (!pane._hqSrvPaneChangeBound) {
        pane._hqSrvPaneChangeBound = true;
        pane.addEventListener('change', function (ev) {
          if (ev.target && ev.target.id === 'hqSrvAutoRefresh') {
            scheduleDataRefresh();
          }
        });
      }
      if (!pane._hqUsageGrainBound) {
        pane._hqUsageGrainBound = true;
        pane.addEventListener('click', function (ev) {
          var btn = ev.target && ev.target.closest && ev.target.closest('[data-hq-usage-grain]');
          if (!btn || !pane.contains(btn)) return;
          var g = btn.getAttribute('data-hq-usage-grain') || 'daily';
          pane._hqUsageGrain = g;
          pane.querySelectorAll('[data-hq-usage-grain]').forEach(function (b) {
            b.classList.toggle('active', b.getAttribute('data-hq-usage-grain') === g);
          });
          refreshHqUsageCharts(pane, g);
        });
      }
      var srvSave = pane.querySelector('#hqServerManageSaveBtn');
      var srvTopSave = pane.querySelector('#hqServerManageTopSaveBtn');
      var srvRef = pane.querySelector('#hqServerManageRefreshBtn');
      function setHqSrvInlineMsg(text, kind) {
        var el = pane.querySelector('#hqSrvInlineMsg');
        if (!el) return;
        el.textContent = text || '';
        el.className = 'small mt-2';
        if (kind === 'success') el.className += ' text-success';
        else if (kind === 'error') el.className += ' text-danger';
        else el.className += ' text-muted';
      }
      function runHqServerManageSave() {
        setHqSrvInlineMsg('', '');
        var fd = {};
        pane.querySelectorAll('input, select, textarea').forEach(function (el) { if (el.name) fd[el.name] = el.value; });
        var gDisk = fd.serverManageContractDiskGb;
        var gTrf = fd.serverManageContractTrafficGb;
        var gUsed = fd.serverManageTrafficUsedGb;
        var minR = fd.serverManageUiRefreshMin;
        delete fd.serverManageContractDiskGb;
        delete fd.serverManageContractTrafficGb;
        delete fd.serverManageTrafficUsedGb;
        delete fd.serverManageUiRefreshMin;
        fd.serverManageContractDiskMb = hqSrvGbToMbContract(gDisk);
        fd.serverManageContractTrafficMb = hqSrvGbToMbContract(gTrf);
        var usedMb = hqSrvGbToMbTrafficUsed(gUsed);
        fd.serverManageTrafficUsedMb = usedMb;
        if (minR !== undefined && minR !== null && String(minR).trim() !== '') {
          fd.serverManageUiRefreshSec = hqSrvMinToSecRefresh(minR);
        } else {
          fd.serverManageUiRefreshSec = '';
        }
        if (dimmSrv) dimmSrv.style.display = 'flex';
        window.PG_API.hqServerManageSave(fd).then(function () {
          setHqSrvInlineMsg('서버운영관리 설정(SSL·호스팅 약정·갱신 간격)이 저장되었습니다. 대시보드가 갱신되었습니다.', 'success');
          return window.PG_API.hqServerManage();
        }).then(function (data) {
          applyServerFormFromSummary(data);
          renderHqServerDashboard(data);
          pane._hqSrvRefreshSec = (data && data.uiAutoRefreshSeconds > 0) ? data.uiAutoRefreshSeconds : 120;
          scheduleDataRefresh();
        }).catch(function (e) {
          setHqSrvInlineMsg(e && e.message ? e.message : '저장 실패', 'error');
        }).finally(function () { if (dimmSrv) dimmSrv.style.display = 'none'; });
      }
      if (srvSave && !srvSave._bound) {
        srvSave._bound = true;
        srvSave.addEventListener('click', runHqServerManageSave);
      }
      if (srvTopSave && !srvTopSave._bound) {
        srvTopSave._bound = true;
        srvTopSave.addEventListener('click', runHqServerManageSave);
      }
      if (srvRef && !srvRef._bound) {
        srvRef._bound = true;
        srvRef.addEventListener('click', function () {
          loadServerSummary(true);
        });
      }
    }
    /** 영업일설정: 탭 pane은 유지되고 innerHTML만 갈아끼우므로, 재진입 시에도 매번 바인딩·목록조회·달력 init 필요. pane 클릭 위임은 한 번만 등록. */
    if (url === '/hq/businessDaySetting') {
      var st = pane._hqBizdayState || (pane._hqBizdayState = { manualEntries: [], currentList: [], currentEditingId: '', manualEditIdx: null });
      st.manualEntries = [];
      st.currentList = [];
      st.currentEditingId = '';
      st.manualEditIdx = null;
      var formBiz = pane.querySelector('form');
      var nameEl = formBiz ? formBiz.querySelector('[name="hqBizdayProfileName"]') : null;
      var countryEl = formBiz ? formBiz.querySelector('[name="holidayCountryCodes"]') : null;
      var extraHidden = formBiz ? (formBiz.querySelector('#hqBizdayExtraDatesHidden') || formBiz.querySelector('[name="businessHolidayExtraDates"]')) : null;
      var manualJsonHidden = formBiz ? formBiz.querySelector('#hqBizdayManualEntriesJson') : null;
      var manualTbody = pane.querySelector('#hqBizdayManualTbody');
      var tbodyBiz = pane.querySelector('#hqBizdayProfileTbody');
      var saveBtnBiz = pane.querySelector('#hqBizdayProfileSaveBtn');
      var newBtnBiz = pane.querySelector('#hqBizdayProfileNewBtn');
      var dimmBiz = document.getElementById('dimm');

      function hqExpandYmdRange(fromStr, toStr) {
        var out = [];
        if (!fromStr || String(fromStr).length < 10) return out;
        var fs = String(fromStr).substring(0, 10);
        if (!/^\d{4}-\d{2}-\d{2}$/.test(fs)) return out;
        var ts = (toStr && String(toStr).length >= 10 && /^\d{4}-\d{2}-\d{2}$/.test(String(toStr).substring(0, 10)))
          ? String(toStr).substring(0, 10) : fs;
        var a = fs.split('-').map(Number);
        var b = ts.split('-').map(Number);
        var d = new Date(a[0], a[1] - 1, a[2]);
        var end = new Date(b[0], b[1] - 1, b[2]);
        if (end < d) { var tmp = d; d = end; end = tmp; }
        var pad2 = function (n) { return n < 10 ? '0' + n : String(n); };
        while (d <= end) {
          out.push(d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate()));
          d.setDate(d.getDate() + 1);
        }
        return out;
      }

      /** hidden 비영업일 문자열 → { 'yyyy-MM-dd': true } */
      function hqBizdayParseExtraMap(text) {
        var map = {};
        String(text || '').split(/\r?\n/).forEach(function (line) {
          var m = line.trim().match(/^(\d{4}-\d{2}-\d{2})/);
          if (m) map[m[1]] = true;
        });
        return map;
      }
      function hqBizdayExtraMapToLines(map) {
        return Object.keys(map).sort().join('\n');
      }

      /** 달력·프리셋으로 쌓인 일자 + 수동 구간을 합집합으로 유지 (기존: 수동만 넣어 프리셋 토·일이 저장 시 삭제되던 버그 수정) */
      function syncHiddenFromManualEntries() {
        var set = hqBizdayParseExtraMap(extraHidden && extraHidden.value);
        st.manualEntries.forEach(function (e) {
          hqExpandYmdRange(e.fromDate, e.toDate || e.fromDate).forEach(function (day) { set[day] = true; });
        });
        if (extraHidden) extraHidden.value = hqBizdayExtraMapToLines(set);
        if (manualJsonHidden) manualJsonHidden.value = JSON.stringify(st.manualEntries);
      }

      function clearManualRangeInputs() {
        var rf = pane.querySelector('#hqBizdayRangeFrom');
        var rt = pane.querySelector('#hqBizdayRangeTo');
        var rk = pane.querySelector('#hqBizdayRangeKind');
        var rn = pane.querySelector('#hqBizdayRangeNote');
        if (rf) rf.value = '';
        if (rt) rt.value = '';
        if (rk) rk.value = '공휴일';
        if (rn) rn.value = '';
      }

      function refreshManualRangeFormUi() {
        var addBtn = pane.querySelector('#hqBizdayRangeAddBtn');
        var cancelBtn = pane.querySelector('#hqBizdayRangeCancelEditBtn');
        var editing = st.manualEditIdx != null && !isNaN(st.manualEditIdx) && st.manualEditIdx >= 0 && st.manualEditIdx < st.manualEntries.length;
        if (addBtn) addBtn.textContent = editing ? '수정 반영' : '구간 추가';
        if (cancelBtn) cancelBtn.classList.toggle('d-none', !editing);
      }

      function renderManualTable() {
        if (!manualTbody) return;
        if (!st.manualEntries.length) {
          manualTbody.innerHTML = '<tr class="hq-bizday-manual-empty"><td colspan="6" class="text-center text-muted">등록된 구간이 없습니다.</td></tr>';
          refreshManualRangeFormUi();
          return;
        }
        var html = '';
        st.manualEntries.forEach(function (e, idx) {
          var hi = st.manualEditIdx === idx ? ' table-info' : '';
          html += '<tr data-manual-idx="' + idx + '" class="' + hi.trim() + '"><td>' + (e.fromDate || '') + '</td><td>' + (e.toDate || e.fromDate || '') + '</td>' +
            '<td>' + (e.holidayKind || '') + '</td><td>' + escapeHtml(String(e.note || '')) + '</td>' +
            '<td><button type="button" class="btn btn-sm btn-outline-primary hq-bizday-manual-edit" data-idx="' + idx + '">수정</button></td>' +
            '<td><button type="button" class="btn btn-sm btn-outline-danger hq-bizday-manual-del" data-idx="' + idx + '">삭제</button></td></tr>';
        });
        manualTbody.innerHTML = html;
        refreshManualRangeFormUi();
      }

      function escapeHtml(s) {
        return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
      }

      function loadManualFromItem(item) {
        if (extraHidden) {
          extraHidden.value = (item && item.businessHolidayExtraDates != null) ? String(item.businessHolidayExtraDates) : '';
        }
        st.manualEntries = [];
        if (item && Array.isArray(item.holidayManualEntries) && item.holidayManualEntries.length) {
          st.manualEntries = item.holidayManualEntries.map(function (e) {
            return {
              fromDate: (e.fromDate != null) ? String(e.fromDate).substring(0, 10) : '',
              toDate: (e.toDate != null) ? String(e.toDate).substring(0, 10) : '',
              holidayKind: (e.holidayKind != null) ? String(e.holidayKind) : '공휴일',
              note: (e.note != null) ? String(e.note) : ''
            };
          });
        }
        syncHiddenFromManualEntries();
        st.manualEditIdx = null;
        renderManualTable();
      }

      /** yyyy-MM-dd 가 토·일이면 true (공식공휴일에 포함되는 주말). */
      function bizdayIsWeekendYmd(ymd) {
        if (!ymd || String(ymd).length < 10) return false;
        var p = String(ymd).substring(0, 10).split('-').map(Number);
        if (p.length !== 3 || isNaN(p[0]) || isNaN(p[1]) || isNaN(p[2])) return false;
        var dt = new Date(p[0], p[1] - 1, p[2]);
        var w = dt.getDay();
        return w === 0 || w === 6;
      }

      /**
       * 목록 열 집계: 서버가 내려주면 holidayCount* 사용(토·일·해당국 법정 프리셋 = 공식, 나머지 저장일 = 추가).
       * 구 API는 아래 클라이언트 추정 로직으로 폴백.
       */
      function bizdayHolidayKindCounts(it) {
        if (it != null && it.holidayCountTotal != null && it.holidayCountOfficial != null && it.holidayCountAdditional != null) {
          var t0 = Number(it.holidayCountTotal);
          var o0 = Number(it.holidayCountOfficial);
          var a0 = Number(it.holidayCountAdditional);
          if (!isNaN(t0) && t0 >= 0 && !isNaN(o0) && !isNaN(a0)) {
            return { official: o0, added: a0, total: t0 };
          }
        }
        var totalMap = {};
        String((it && it.businessHolidayExtraDates) || '').split(/\r?\n/).forEach(function (line) {
          var m = line.trim().match(/^(\d{4}-\d{2}-\d{2})/);
          if (m) totalMap[m[1]] = true;
        });
        var days = Object.keys(totalMap);
        var total = days.length;
        var cc = (it && it.countryCode != null) ? String(it.countryCode).trim().toUpperCase() : '';
        if (cc === 'GLOBAL') {
          var offG = 0;
          var addG = 0;
          days.forEach(function (d) {
            if (bizdayIsWeekendYmd(d)) offG += 1;
            else addG += 1;
          });
          return { official: offG, added: addG, total: total };
        }
        var addedMap = {};
        var arr = (it && it.holidayManualEntries) || [];
        arr.forEach(function (e) {
          hqExpandYmdRange(e.fromDate, e.toDate || e.fromDate).forEach(function (day) {
            addedMap[day] = true;
          });
        });
        var added = 0;
        Object.keys(addedMap).forEach(function (d) {
          if (totalMap[d] && !bizdayIsWeekendYmd(d)) added += 1;
        });
        var official = total - added;
        return { official: official < 0 ? 0 : official, added: added, total: total };
      }
      function renderList(list) {
        st.currentList = Array.isArray(list) ? list : [];
        if (!tbodyBiz) return;
        if (!st.currentList.length) {
          tbodyBiz.innerHTML = '<tr><td colspan="11" class="text-center text-muted">저장된 설정이 없습니다.</td></tr>';
          return;
        }
        var html = '';
        var sel = String(st.currentEditingId || '');
        st.currentList.forEach(function (it, i) {
          var hc = bizdayHolidayKindCounts(it);
          var createdDisp = (it.createdAt && String(it.createdAt).trim()) ? String(it.createdAt).trim().substring(0, 10)
            : ((it.updatedAt && String(it.updatedAt).trim()) ? String(it.updatedAt).trim().substring(0, 10) : '-');
          var updatedDisp = (it.updatedAt && String(it.updatedAt).trim()) ? String(it.updatedAt).trim().substring(0, 10) : '-';
          var idStr = String(it.id || '');
          var rowHi = (sel && sel === idStr) ? ' table-active' : '';
          html += '<tr class="hq-bizday-row' + rowHi + '" data-id="' + idStr + '" style="cursor:pointer">' +
            '<td>' + (i + 1) + '</td><td>' + (it.name || '') + '</td><td>' + (it.countryCode || '') + '</td>' +
            '<td>' + (it.createdBy || '-') + '</td>' +
            '<td class="text-center align-middle">' + hc.official + '</td><td class="text-center align-middle">' + hc.added + '</td><td class="text-center align-middle">' + hc.total + '</td>' +
            '<td>' + createdDisp + '</td><td>' + updatedDisp + '</td>' +
            '<td class="text-center p-1 align-middle"><button type="button" class="btn btn-sm btn-outline-primary hq-bizday-profile-edit" data-id="' + idStr + '">수정</button></td>' +
            '<td class="text-center p-1 align-middle"><button type="button" class="btn btn-sm btn-outline-danger hq-bizday-profile-del" data-id="' + idStr + '">삭제</button></td></tr>';
        });
        tbodyBiz.innerHTML = html;
      }
      function loadBizdayProfileIntoEditor(item) {
        if (!item) return;
        st.currentEditingId = item.id || '';
        if (nameEl) nameEl.value = item.name || '';
        if (countryEl) countryEl.value = item.countryCode || 'KR';
        loadManualFromItem(item);
        if (window.PG_HQ_HOLIDAY && typeof window.PG_HQ_HOLIDAY.init === 'function') window.PG_HQ_HOLIDAY.init(pane, { force: true });
        renderList(st.currentList);
      }
      function clearEditor() {
        st.currentEditingId = '';
        st.manualEntries = [];
        if (nameEl) nameEl.value = '';
        if (countryEl) countryEl.value = 'KR';
        if (extraHidden) extraHidden.value = '';
        syncHiddenFromManualEntries();
        st.manualEditIdx = null;
        clearManualRangeInputs();
        renderManualTable();
        if (window.PG_HQ_HOLIDAY && typeof window.PG_HQ_HOLIDAY.init === 'function') window.PG_HQ_HOLIDAY.init(pane, { force: true });
        renderList(st.currentList);
      }
      function loadList() {
        if (dimmBiz) dimmBiz.style.display = 'flex';
        window.PG_API.hqBusinessDaySettings().then(function (list) {
          renderList(list || []);
          if (window.PG_HQ_HOLIDAY && typeof window.PG_HQ_HOLIDAY.init === 'function') window.PG_HQ_HOLIDAY.init(pane, { force: true });
        }).catch(function (e) {
          if (tbodyBiz) tbodyBiz.innerHTML = '<tr><td colspan="11" class="text-center text-danger">' + (e && e.message ? e.message : '조회 실패') + '</td></tr>';
        }).finally(function () { if (dimmBiz) dimmBiz.style.display = 'none'; });
      }
      /** 신규·저장·삭제 공통: 1차 확인 → 2차 최종 확인(취소 시 중단) */
      function hqBizdayTwoStepConfirm(msgStep1, msgStep2) {
        if (!window.confirm(msgStep1)) return false;
        if (!window.confirm(msgStep2)) return false;
        return true;
      }
      if (newBtnBiz) newBtnBiz.addEventListener('click', function () {
        if (!hqBizdayTwoStepConfirm(
          '[1단계] 신규를 누르면 편집 중인 이름·기준국가·휴일 구간·달력에 반영된 데이터가 모두 초기화됩니다.\n진행하시겠습니까? (취소 시 아무 변화 없음)',
          '[2단계] 최종 확인: 모든 입력을 비우고 신규 작성 화면으로 전환합니다.\n정말 진행하시겠습니까?'
        )) return;
        clearEditor();
      });
      var addRangeBtn = pane.querySelector('#hqBizdayRangeAddBtn');
      if (addRangeBtn) {
        addRangeBtn.addEventListener('click', function () {
          var rf = pane.querySelector('#hqBizdayRangeFrom');
          var rt = pane.querySelector('#hqBizdayRangeTo');
          var rk = pane.querySelector('#hqBizdayRangeKind');
          var rn = pane.querySelector('#hqBizdayRangeNote');
          var from = rf && rf.value ? rf.value.trim() : '';
          var to = rt && rt.value ? rt.value.trim() : '';
          if (!from) { alert('시작일을 선택하세요.'); return; }
          if (!to) to = from;
          if (from > to) { var x = from; from = to; to = x; }
          var entry = {
            fromDate: from,
            toDate: to,
            holidayKind: (rk && rk.value) ? rk.value : '공휴일',
            note: (rn && rn.value) ? rn.value.trim() : ''
          };
          var ix = st.manualEditIdx;
          if (ix != null && !isNaN(ix) && ix >= 0 && ix < st.manualEntries.length) {
            st.manualEntries[ix] = entry;
            st.manualEditIdx = null;
            clearManualRangeInputs();
          } else {
            st.manualEntries.push(entry);
          }
          syncHiddenFromManualEntries();
          renderManualTable();
          if (window.PG_HQ_HOLIDAY && typeof window.PG_HQ_HOLIDAY.init === 'function') window.PG_HQ_HOLIDAY.init(pane, { force: true });
        });
      }
      var cancelRangeEditBtn = pane.querySelector('#hqBizdayRangeCancelEditBtn');
      if (cancelRangeEditBtn) {
        cancelRangeEditBtn.addEventListener('click', function () {
          st.manualEditIdx = null;
          clearManualRangeInputs();
          refreshManualRangeFormUi();
          renderManualTable();
        });
      }
      if (saveBtnBiz) saveBtnBiz.addEventListener('click', function () {
        var name = nameEl && nameEl.value ? nameEl.value.trim() : '';
        var cc = countryEl && countryEl.value ? countryEl.value : 'KR';
        syncHiddenFromManualEntries();
        var extra = extraHidden && extraHidden.value ? extraHidden.value : '';
        if (!name) { alert('이름을 입력하세요.'); return; }
        if (!hqBizdayTwoStepConfirm(
          '[1단계] 현재 화면의 영업일 설정을 서버에 저장합니다.\n진행하시겠습니까? (취소 시 저장 안 함)',
          '[2단계] 최종 확인: 저장하면 목록 및 적용 데이터가 갱신됩니다.\n저장하시겠습니까?'
        )) return;
        if (dimmBiz) dimmBiz.style.display = 'flex';
        window.PG_API.hqBusinessDaySettingsSave({
          mode: 'UPSERT',
          id: st.currentEditingId,
          name: name,
          countryCode: cc,
          businessHolidayExtraDates: extra,
          holidayManualEntries: st.manualEntries
        }).then(function (data) {
          alert((data && data.message) ? data.message : '저장되었습니다.');
          if (data && data.id) st.currentEditingId = String(data.id);
          renderList((data && data.list) ? data.list : []);
          if (st.currentEditingId && data && data.list) {
            var found = data.list.find(function (x) { return String(x.id || '') === st.currentEditingId; });
            if (found) loadManualFromItem(found);
          }
        }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimmBiz) dimmBiz.style.display = 'none'; });
      });
      function runHqBizdayDeleteById(deleteId) {
        if (!deleteId) return;
        var victim = st.currentList.filter(function (x) { return String(x.id || '') === String(deleteId); })[0];
        var vname = victim && victim.name ? String(victim.name) : String(deleteId);
        if (!hqBizdayTwoStepConfirm(
          '[1단계] 영업일 설정 [' + vname + ']을(를) 삭제합니다.\n진행하시겠습니까? (취소 시 삭제 안 함)',
          '[2단계] 최종 확인: 삭제 후에는 복구할 수 없습니다.\n삭제하시겠습니까?'
        )) return;
        if (dimmBiz) dimmBiz.style.display = 'flex';
        window.PG_API.hqBusinessDaySettingsSave({ mode: 'DELETE', id: deleteId }).then(function (data) {
          alert((data && data.message) ? data.message : '삭제되었습니다.');
          if (String(st.currentEditingId) === String(deleteId)) clearEditor();
          renderList((data && data.list) ? data.list : []);
        }).catch(function (e) { alert(e && e.message ? e.message : '삭제 실패'); }).finally(function () { if (dimmBiz) dimmBiz.style.display = 'none'; });
      }
      pane._hqBizdayRuntime = {
        st: st,
        syncHiddenFromManualEntries: syncHiddenFromManualEntries,
        hqExpandYmdRange: hqExpandYmdRange,
        hqBizdayParseExtraMap: hqBizdayParseExtraMap,
        hqBizdayExtraMapToLines: hqBizdayExtraMapToLines,
        renderManualTable: renderManualTable,
        loadBizdayProfileIntoEditor: loadBizdayProfileIntoEditor,
        runHqBizdayDeleteById: runHqBizdayDeleteById,
        loadManualFromItem: loadManualFromItem,
        clearManualRangeInputs: clearManualRangeInputs,
        refreshManualRangeFormUi: refreshManualRangeFormUi
      };
      if (!pane._hqBizdayProfileClickDelegated) {
        pane._hqBizdayProfileClickDelegated = true;
        pane.addEventListener('click', function (ev) {
          var p = ev.currentTarget;
          if (p.getAttribute('formurl') !== '/hq/businessDaySetting') return;
          var rt = p._hqBizdayRuntime;
          if (!rt || !rt.st) return;
          var st0 = rt.st;
          var editM = ev.target && ev.target.closest ? ev.target.closest('.hq-bizday-manual-edit') : null;
          if (editM && p.contains(editM)) {
            var eix = parseInt(editM.getAttribute('data-idx'), 10);
            if (!isNaN(eix) && st0.manualEntries[eix]) {
              var ent = st0.manualEntries[eix];
              var rf0 = p.querySelector('#hqBizdayRangeFrom');
              var rt0 = p.querySelector('#hqBizdayRangeTo');
              var rk0 = p.querySelector('#hqBizdayRangeKind');
              var rn0 = p.querySelector('#hqBizdayRangeNote');
              if (rf0) rf0.value = ent.fromDate || '';
              if (rt0) rt0.value = ent.toDate || ent.fromDate || '';
              if (rk0) rk0.value = ent.holidayKind || '공휴일';
              if (rn0) rn0.value = ent.note || '';
              st0.manualEditIdx = eix;
              rt.refreshManualRangeFormUi();
              rt.renderManualTable();
            }
            return;
          }
          var delM = ev.target && ev.target.closest ? ev.target.closest('.hq-bizday-manual-del') : null;
          if (delM && p.contains(delM)) {
            var ix = parseInt(delM.getAttribute('data-idx'), 10);
            if (!isNaN(ix)) {
              var editingIdx = st0.manualEditIdx;
              var exH = p.querySelector('#hqBizdayExtraDatesHidden') || p.querySelector('[name="businessHolidayExtraDates"]');
              var removedEnt = st0.manualEntries[ix];
              var removedDays = removedEnt ? rt.hqExpandYmdRange(removedEnt.fromDate, removedEnt.toDate || removedEnt.fromDate) : [];
              st0.manualEntries.splice(ix, 1);
              if (exH && removedDays.length) {
                var mset = rt.hqBizdayParseExtraMap(exH.value);
                removedDays.forEach(function (d) { delete mset[d]; });
                st0.manualEntries.forEach(function (e) {
                  rt.hqExpandYmdRange(e.fromDate, e.toDate || e.fromDate).forEach(function (day) { mset[day] = true; });
                });
                exH.value = rt.hqBizdayExtraMapToLines(mset);
              }
              if (editingIdx != null && !isNaN(editingIdx)) {
                if (editingIdx === ix) {
                  st0.manualEditIdx = null;
                  rt.clearManualRangeInputs();
                } else if (editingIdx > ix) {
                  st0.manualEditIdx = editingIdx - 1;
                }
              }
              rt.syncHiddenFromManualEntries();
              rt.renderManualTable();
              rt.refreshManualRangeFormUi();
              if (window.PG_HQ_HOLIDAY && typeof window.PG_HQ_HOLIDAY.init === 'function') window.PG_HQ_HOLIDAY.init(p, { force: true });
            }
            return;
          }
          var profileDel = ev.target && ev.target.closest ? ev.target.closest('.hq-bizday-profile-del') : null;
          if (profileDel && p.contains(profileDel)) {
            ev.stopPropagation();
            var delId = profileDel.getAttribute('data-id') || '';
            rt.runHqBizdayDeleteById(delId);
            return;
          }
          var profileEdit = ev.target && ev.target.closest ? ev.target.closest('.hq-bizday-profile-edit') : null;
          if (profileEdit && p.contains(profileEdit)) {
            ev.stopPropagation();
            var peId = profileEdit.getAttribute('data-id') || '';
            var peItem = st0.currentList.find(function (x) { return String(x.id || '') === peId; });
            if (peItem) rt.loadBizdayProfileIntoEditor(peItem);
            return;
          }
          var tr = ev.target && ev.target.closest ? ev.target.closest('.hq-bizday-row') : null;
          if (!tr || !p.contains(tr)) return;
          var id = tr.getAttribute('data-id') || '';
          var item = st0.currentList.find(function (x) { return String(x.id || '') === id; });
          if (!item) return;
          rt.loadBizdayProfileIntoEditor(item);
        });
      }
      loadList();
    }
    var hqPgApiAddBtn = pane.querySelector('#hqPgApiAddBtn');
    if (hqPgApiAddBtn && url === '/hq/pgApiMng' && !hqPgApiAddBtn._hqPgAddBound) {
      hqPgApiAddBtn._hqPgAddBound = true;
      hqPgApiAddBtn.addEventListener('click', function () {
        if (window.openPgAgencyModal) window.openPgAgencyModal({});
      });
    }
    if (url === '/hq/pgApiMng' && !pane._hqPgOperationalVisualBound) {
      pane._hqPgOperationalVisualBound = true;
      pane.addEventListener('change', function (ev) {
        var t = ev.target;
        if (!t || !t.classList || !t.classList.contains('hq-pg-operational-cb')) return;
        var tb = t.closest('tbody');
        if (!tb || !pane.contains(tb)) return;
        tb.querySelectorAll('tr').forEach(function (tr) {
          var cb = tr.querySelector('.hq-pg-operational-cb');
          var on = cb && cb.checked && !cb.disabled;
          tr.classList.toggle('hq-pg-row-operational', !!on);
          tr.classList.toggle('hq-pg-row-inactive', !on);
        });
      });
    }
    var hqPgApiOperationalSaveBtn = pane.querySelector('#hqPgApiOperationalSaveBtn');
    if (hqPgApiOperationalSaveBtn && url === '/hq/pgApiMng' && !hqPgApiOperationalSaveBtn._hqPgOpBound) {
      hqPgApiOperationalSaveBtn._hqPgOpBound = true;
      hqPgApiOperationalSaveBtn.addEventListener('click', function () {
        if (!window.pgDoubleConfirm || !window.pgDoubleConfirm('결제대행사 운영 설정을 저장하시겠습니까?', '체크한 PG만 운영(가맹점 PG 선택·연동)으로 저장됩니다. 계속하시겠습니까?')) return;
        var dimmOp = document.getElementById('dimm');
        var fullParams = collectSearchParams(pane);
        fullParams.page = 1;
        fullParams.size = 2000;
        if (dimmOp) dimmOp.style.display = 'flex';
        window.PG_API.hqPgApiMng(fullParams).then(function (data) {
          var fullList = (data && data.list) ? data.list : [];
          var override = {};
          pane.querySelectorAll('.hq-pg-operational-cb').forEach(function (cb) {
            var cd = cb.getAttribute('data-pg-cd') || '';
            if (cd) override[cd] = !!cb.checked;
          });
          var cds = [];
          fullList.forEach(function (row) {
            if (String(row.useYn || '') !== 'Y') return;
            var cdx = String(row.pgCd || '').trim();
            if (!cdx) return;
            var checked = Object.prototype.hasOwnProperty.call(override, cdx) ? override[cdx] : (String(row.operationalYn || '') === 'Y');
            if (checked) cds.push(cdx);
          });
          return window.PG_API.hqPgApiMngOperationalSave({ operationalPgCds: cds });
        }).then(function () {
          alert('운영 설정이 저장되었습니다.');
          var sb = pane.querySelector('#searchBtn');
          if (sb) sb.click();
        }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimmOp) dimmOp.style.display = 'none'; });
      });
    }
    if (url === '/hq/pgApiMng' && !pane._hqPgGridDbl) {
      pane._hqPgGridDbl = true;
      function openPgAgencyModalFromRow(row) {
        if (!row || !window.openPgAgencyModal) return;
        window.openPgAgencyModal({
          id: row.id,
          pgCd: row.pgCd,
          pgNm: row.pgNm,
          integKind: row.integKind,
          primaryEndpoint: row.primaryEndpoint,
          apiEndpoint: row.apiEndpoint || '',
          endpointNoti: row.endpointNoti || '',
          endpointUrlPay: row.endpointUrlPay || '',
          endpointApi: row.endpointApi || '',
          integNotiYn: row.integNotiYn || 'N',
          integUrlPayYn: row.integUrlPayYn || 'N',
          integWebChatbotYn: row.integWebChatbotYn || 'N',
          integApiYn: row.integApiYn || 'N',
          useYn: row.useYn || 'Y',
          merchantMid: row.merchantMid || '',
          routeNo: row.routeNo,
          sandboxYn: row.sandboxYn || 'Y',
          credentialsExtraJson: row.credentialsExtraJson || ''
        });
      }
      pane.addEventListener('click', function (e) {
        var delBtn = e.target && e.target.closest ? e.target.closest('.hq-pg-row-del') : null;
        if (delBtn && pane.contains(delBtn)) {
          e.preventDefault();
          e.stopPropagation();
          var dridx = delBtn.getAttribute('data-row-idx');
          var didx = dridx != null && dridx !== '' ? parseInt(dridx, 10) : NaN;
          var dlist = pane._lastGridList || [];
          if (isNaN(didx) || didx < 0 || didx >= dlist.length) return;
          var drow = dlist[didx];
          if (!drow || drow.id == null) return;
          var pgLab = (drow.pgNm || drow.pgCd || '') + ' (' + (drow.pgCd || '') + ')';
          var okDel = window.pgDoubleConfirm
            ? window.pgDoubleConfirm('PG 연동을 삭제하시겠습니까?\n' + pgLab, '삭제 후 복구할 수 없습니다. 가맹점 결제대행사에서 이 PG를 사용 중이면 삭제할 수 없습니다. 계속하시겠습니까?')
            : window.confirm('PG 연동을 삭제하시겠습니까?\n' + pgLab);
          if (!okDel) return;
          var dimmDel = document.getElementById('dimm');
          if (dimmDel) dimmDel.style.display = 'flex';
          window.PG_API.hqPgApiMngDelete({ id: drow.id }).then(function () {
            alert('삭제되었습니다.');
            var sb2 = pane.querySelector('#searchBtn');
            if (sb2) sb2.click();
          }).catch(function (err) {
            var msg = err && err.message ? err.message : '삭제 실패';
            if (err && err.errorCode === 'IN_USE') {
              alert('가맹점에서 이 PG를 사용 중이어서 삭제할 수 없습니다.\n\n' + msg);
            } else {
              alert(msg);
            }
          }).finally(function () { if (dimmDel) dimmDel.style.display = 'none'; });
          return;
        }
        var editBtn = e.target && e.target.closest ? e.target.closest('.hq-pg-row-edit') : null;
        if (!editBtn || !pane.contains(editBtn)) return;
        e.preventDefault();
        e.stopPropagation();
        var ridx = editBtn.getAttribute('data-row-idx');
        var idx = ridx != null && ridx !== '' ? parseInt(ridx, 10) : NaN;
        var list = pane._lastGridList || [];
        if (isNaN(idx) || idx < 0 || idx >= list.length) return;
        openPgAgencyModalFromRow(list[idx]);
      });
      pane.addEventListener('dblclick', function (e) {
        if (e.target && e.target.closest && e.target.closest('.hq-pg-row-edit, .hq-pg-row-del')) return;
        var tr = e.target.closest('tbody tr');
        if (!tr || tr.querySelector('.empty-state-cell')) return;
        var grid = pane.querySelector('#grid_' + tabId + ' tbody');
        if (!grid || !grid.contains(tr)) return;
        var attrIdx = tr.getAttribute('data-row-idx');
        var idx = attrIdx != null && attrIdx !== '' ? parseInt(attrIdx, 10) : NaN;
        if (isNaN(idx)) {
          var rows = grid.querySelectorAll('tr');
          idx = Array.prototype.indexOf.call(rows, tr);
        }
        var list = pane._lastGridList || [];
        if (idx < 0 || idx >= list.length) return;
        openPgAgencyModalFromRow(list[idx]);
      });
    }
    var hqPermissionSaveBtn = pane.querySelector('#hqPermissionSaveBtn');
    if (hqPermissionSaveBtn && url === '/hq/permissionMng' && !hqPermissionSaveBtn._bound) {
      hqPermissionSaveBtn._bound = true;
      hqPermissionSaveBtn.addEventListener('click', function () {
        var st = pane._orgPermState;
        if (!st) { alert('저장할 데이터가 없습니다.'); return; }
        if (!pgConfirmBeforeSave('저장하시겠습니까?')) return;
        var keepOrgLevel = pane._orgPermActiveLv || '';
        var dimm3 = document.getElementById('dimm');
        if (dimm3) dimm3.style.display = 'flex';
        window.PG_API.hqPermissionMngSave({ matrix: st }).then(function (res) {
          if (keepOrgLevel) pane._orgPermActiveLv = keepOrgLevel;
          var pay = res;
          if (res && res.success === true && res.data && typeof res.data === 'object'
              && res.catalog == null && res.data.catalog != null) {
            pay = res.data;
          }
          if (pay && window.initOrgPagePermissionMatrix) {
            window.initOrgPagePermissionMatrix(pane, tabId, pay);
          }
          alert('저장되었습니다.');
        }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimm3) dimm3.style.display = 'none'; });
      });
    }
    var hqPermissionReloadBtn = pane.querySelector('#hqPermissionReloadBtn');
    if (hqPermissionReloadBtn && url === '/hq/permissionMng' && !hqPermissionReloadBtn._bound) {
      hqPermissionReloadBtn._bound = true;
      hqPermissionReloadBtn.addEventListener('click', function () {
        if (!window.PG_API || !window.PG_API.hqPermissionMng) return;
        if (!pgConfirmBeforeSave(
          '서버에 저장된 단계별 기본 권한을 다시 불러옵니다. 저장하지 않은 편집은 취소됩니다. 계속할까요?',
          '불러오면 편집 중인 내용이 사라집니다. 정말 진행할까요?'
        )) return;
        var keepOrgLevel = pane._orgPermActiveLv || '';
        var dimmR = document.getElementById('dimm');
        if (dimmR) dimmR.style.display = 'flex';
        window.PG_API.hqPermissionMng({}).then(function (res) {
          if (keepOrgLevel) pane._orgPermActiveLv = keepOrgLevel;
          var pay = res;
          if (res && res.success === true && res.data && typeof res.data === 'object'
              && res.catalog == null && res.data.catalog != null) {
            pay = res.data;
          }
          if (pay && window.initOrgPagePermissionMatrix) {
            window.initOrgPagePermissionMatrix(pane, tabId, pay);
          }
        }).catch(function (e) { alert(e && e.message ? e.message : '불러오기 실패'); }).finally(function () { if (dimmR) dimmR.style.display = 'none'; });
      });
    }
    function bindSettlementExecuteRun(btn) {
      if (!btn) return;
      btn.addEventListener('click', function () {
        var fromEl = pane.querySelector('#searchFromDate');
        var toEl = pane.querySelector('#searchToDate');
        var compIdEl = pane.querySelector('input[name="searchCompId"]');
        var fromDate = fromEl && fromEl.value ? fromEl.value : '';
        var toDate = toEl && toEl.value ? toEl.value : '';
        if (!fromDate || !toDate) { alert('정산대상일(시작일~종료일)을 입력하세요.'); return; }
        var merchantId = compIdEl && compIdEl.value ? compIdEl.value.trim() : '';
        var dimm4 = document.getElementById('dimm');
        if (dimm4) dimm4.style.display = 'flex';
        var runParams = { fromDate: fromDate, toDate: toDate };
        if (merchantId) runParams.merchantId = merchantId;
        window.PG_API.settlementExecuteRun(runParams).then(function (list) {
          alert('정산 실행 완료. ' + (list && list.length ? list.length : 0) + '건');
          doSearch(pane, tabId, 1);
        }).catch(function (e) {
          alert(e && e.message ? e.message : '정산 실행 실패');
        }).finally(function () { if (dimm4) dimm4.style.display = 'none'; });
      });
    }
    var exCalcBtn = pane.querySelector('#exCalcBtn');
    if (exCalcBtn && (url === '/calc/exCalcList' || url === '/settlement/execute')) bindSettlementExecuteRun(exCalcBtn);
    var executeBtn = pane.querySelector('#executeBtn');
    if (executeBtn && (url === '/calc/exCalcList' || url === '/settlement/execute')) bindSettlementExecuteRun(executeBtn);
    pane._pgRunListSearch = doSearch;
    bindPagingEvents(pane, tabId);
  }

  function getPagingContainer(pane, tabId) {
    if (!pane || !tabId) return null;
    return pane.querySelector('#paging_' + tabId) || pane.querySelector('.pagination-pages');
  }

  function syncPaginationSizeButtons(pane, size) {
    if (!pane) return;
    pane.querySelectorAll('.pagination-size-opt').forEach(function (btn) {
      var s = btn.getAttribute('data-size');
      btn.classList.toggle('pagination-size-opt--active', s === String(size));
    });
  }

  function renderPagingNumbers(pane, tabId, currentPage, totalPages, totalElements) {
    var container = getPagingContainer(pane, tabId);
    if (!container) return;
    currentPage = Math.max(1, parseInt(currentPage, 10) || 1);
    totalPages = Math.max(1, parseInt(totalPages, 10) || 1);
    var total = totalElements != null ? parseInt(totalElements, 10) : 0;
    if (isNaN(total)) total = 0;
    var totalPageEl = pane.querySelector('#totalPageCount');
    if (totalPageEl) totalPageEl.textContent = totalPages;
    var totalEl = pane.querySelector('#totalElementsCount');
    if (totalEl) totalEl.textContent = total;
    var pageCntEl = pane.querySelector('#pageCnt');
    if (pageCntEl) { pageCntEl.value = currentPage; pageCntEl.setAttribute('max', totalPages); }
    var sizeEl = pane.querySelector('#recordsPerPage');
    if (sizeEl) syncPaginationSizeButtons(pane, sizeEl.value);
    var html = '';
    if (totalPages <= 1) {
      html = '<span class="pagination-num pagination-num--current">1</span>';
    } else {
      var cur = currentPage;
      var lastPage = totalPages;
      var show = [];
      var maxVisible = 7;
      if (lastPage <= maxVisible) {
        for (var i = 1; i <= lastPage; i++) show.push(i);
      } else {
        show.push(1);
        var from = Math.max(2, cur - 1);
        var to = Math.min(lastPage - 1, cur + 1);
        if (from > 2) show.push('...');
        for (var j = from; j <= to; j++) { if (show.indexOf(j) === -1) show.push(j); }
        if (to < lastPage - 1) show.push('...');
        if (lastPage > 1) show.push(lastPage);
      }
      for (var k = 0; k < show.length; k++) {
        var p = show[k];
        if (p === '...') {
          html += '<span class="pagination-ellipsis">…</span>';
        } else {
          var active = p === cur ? ' pagination-num--current' : '';
          html += '<button type="button" class="pagination-num' + active + '" data-page="' + p + '">' + p + '</button>';
        }
      }
    }
    container.innerHTML = html;
    container.querySelectorAll('.pagination-num[data-page]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var page = parseInt(this.getAttribute('data-page'), 10);
        if (!page) return;
        var cnt = pane.querySelector('#pageCnt');
        if (cnt) cnt.value = page;
        var totalEl = pane.querySelector('#totalElementsCount');
        var totalElements = totalEl ? parseInt(totalEl.textContent || '0', 10) : 0;
        renderPagingNumbers(pane, tabId, page, totalPages, totalElements);
        pane.dispatchEvent(new CustomEvent('paging-change', { detail: { page: page, totalPages: totalPages } }));
      });
    });
  }

  function bindPagingEvents(pane, tabId) {
    if (!pane || !tabId) return;
    renderPagingNumbers(pane, tabId, 1, 1, 0);
    /* innerHTML로 페이지네이션 버튼이 갈아끼워져도 동작하도록 pane에 위임 */
    if (!pane._pgPaginationSizeDelegated) {
      pane._pgPaginationSizeDelegated = true;
      pane.addEventListener('click', function (e) {
        var btn = e.target && e.target.closest ? e.target.closest('.pagination-size-opt') : null;
        if (!btn || !pane.contains(btn)) return;
        var size = btn.getAttribute('data-size');
        if (!size) return;
        var sizeInput = pane.querySelector('#recordsPerPage');
        if (sizeInput) sizeInput.value = size;
        syncPaginationSizeButtons(pane, size);
        var pc0 = pane.querySelector('#pageCnt');
        if (pc0) pc0.value = '1';
        var runList = pane._pgRunListSearch;
        if (typeof runList === 'function') runList(pane, tabId, 1);
      });
    }
    var pageCntEl = pane.querySelector('#pageCnt');
    if (pageCntEl) {
      pageCntEl.addEventListener('change', function () {
        var totalPages = parseInt(pane.querySelector('#totalPageCount').textContent || 1, 10);
        var page = Math.max(1, Math.min(totalPages, parseInt(this.value, 10) || 1));
        var totalEl = pane.querySelector('#totalElementsCount');
        var totalElements = totalEl ? parseInt(totalEl.textContent || '0', 10) : 0;
        renderPagingNumbers(pane, tabId, page, totalPages, totalElements);
      });
    }
  }

  window.updatePaging = function (tabId, currentPage, totalPages, totalElements) {
    var pane = document.getElementById(tabId);
    if (pane) renderPagingNumbers(pane, tabId, currentPage, totalPages, totalElements);
  };

  function ensureTabPane(tabId, url) {
    var container = document.getElementById(TAB_MAIN);
    if (!container) return null;
    var pane = document.getElementById(tabId);
    if (pane) return pane;
    pane = document.createElement('div');
    pane.id = tabId;
    pane.className = 'tab-pane tabConDiv';
    pane.setAttribute('formurl', url || '');
    container.appendChild(pane);
    return pane;
  }

  function loadContent(url, menuId, label) {
    var info = MENU_INFO[url] || {};
    var menuLabel = label || info.label || url;
    var tabId = getTabIdFromUrl(url);
    var mainPane = document.getElementById('main');

    if (url === '/main' || !url) {
      if (mainPane) {
        mainPane.classList.add('show', 'active');
        mainPane.style.display = 'block';
      }
      document.querySelectorAll('#' + TAB_MAIN + ' .tab-pane').forEach(function (p) {
        if (p.id !== 'main') {
          p.classList.remove('show', 'active');
          p.style.display = 'none';
        }
      });
    } else {
      if (mainPane) {
        mainPane.classList.remove('show', 'active');
        mainPane.style.display = 'none';
      }
      var pane = ensureTabPane(tabId, url);
      if (pane && window.PG_SCREENS && window.PG_SCREENS.getScreenHtml) {
        pane.setAttribute('formurl', url || '');
        /* innerHTML 재생성 시 pane 프로퍼티는 유지됨 — 조직항목설정 등 1회 바인딩 플래그가 남으면 재진입 시 초기화·API가 스킵됨 */
        if (url === '/hq/orgViewColumnAllowance') {
          pane._hqOrgColAllowBound = false;
        }
        if (url === '/hq/notifyMapping') {
          pane._hqNotifyMappingBound = false;
        }
        if (url === '/hq/notifyInbound') {
          pane._hqNotifyInboundBound = false;
        }
        if (url === '/hq/settlementAdmin') {
          pane._hqSettlementAdminBound = false;
        }
        pane.innerHTML = window.PG_SCREENS.getScreenHtml(url, tabId);
        pane.classList.toggle('screen-chill-pay-tr-list', url === '/calc/chillPayTrList');
        /* 탭 전환마다 innerHTML이 바뀌어도 pane은 동일 → 이전 수수료용 리스너를 제거하지 않으면 중복 또는(플래그로 스킵 시) 신규 DOM에 미바인딩 */
        if (url === '/commission/commisionList') {
          if (pane._commissionListenersAbort) {
            try { pane._commissionListenersAbort.abort(); } catch (eCommAb) {}
          }
          pane._commissionListenersAbort = new AbortController();
        }
        pane.classList.add('show', 'active');
        pane.style.display = 'block';
        bindScreenEvents(pane, tabId);
        if (window.PG_TABLE_COL_RESIZE) {
          if (typeof window.PG_TABLE_COL_RESIZE.ensureObserver === 'function') {
            window.PG_TABLE_COL_RESIZE.ensureObserver(pane);
          }
          if (typeof window.PG_TABLE_COL_RESIZE.refreshInSync === 'function') {
            window.PG_TABLE_COL_RESIZE.refreshInSync(pane);
          } else if (typeof window.PG_TABLE_COL_RESIZE.refreshIn === 'function') {
            window.PG_TABLE_COL_RESIZE.refreshIn(pane);
          }
        }
      }
      document.querySelectorAll('#' + TAB_MAIN + ' .tab-pane').forEach(function (p) {
        if (p.id !== tabId) {
          p.classList.remove('show', 'active');
          p.style.display = 'none';
        }
      });
    }

    var breadcrumb = document.querySelector('.breadcrumb-item.navi, li.navi');
    if (breadcrumb) {
      breadcrumb.textContent = info.parent ? info.parent + ' > ' + menuLabel : menuLabel;
      breadcrumb.style.fontWeight = 'bold';
    }
    var titleEl = document.getElementById('common__header__title');
    if (titleEl) {
      titleEl.innerHTML = '<i class="bi bi-chevron-right"></i> ' + menuLabel;
      titleEl.classList.toggle('empty-title', url === '/system/noticeList');
    }
  }

  window.fnTopMenuMove = function (url, menuId, label) {
    if (!url) return;
    if (!isMenuAllowedForCurrentUser(url)) {
      alert('이 메뉴에 대한 접근 권한이 없습니다. [본사권한설정]에서 해당 화면 권한을 확인하세요.');
      return;
    }
    var link = document.querySelector('.child-li[data-url="' + url + '"] a');
    var mid = menuId || (link && link.getAttribute('data-menu_id'));
    var info = MENU_INFO[url] || {};
    var text = label || (link && link.textContent.trim()) || info.label || getTabIdFromUrl(url);
    addTabAndSwitch(url, mid, text);
  };

  function syncAllNoticeWriteButtons() {
    var u = getSessionUser();
    var show = !!(u && u.canWriteNotice);
    document.querySelectorAll('[data-notice-write-btn]').forEach(function (btn) {
      btn.classList.toggle('d-none', !show);
    });
  }

  function ensureNoticeWriteModal() {
    var el = document.getElementById('pgNoticeWriteModal');
    if (el) return el;
    var wrap = document.createElement('div');
    wrap.innerHTML =
      '<div class="modal fade" id="pgNoticeWriteModal" tabindex="-1" aria-labelledby="pgNoticeWriteModalLabel" aria-hidden="true">' +
      '<div class="modal-dialog modal-lg modal-dialog-scrollable">' +
      '<div class="modal-content">' +
      '<div class="modal-header">' +
      '<h5 class="modal-title" id="pgNoticeWriteModalLabel">공지 등록</h5>' +
      '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="닫기"></button></div>' +
      '<div class="modal-body">' +
      '<div class="mb-3"><label class="form-label" for="pgNoticeWriteTitle">제목</label>' +
      '<input type="text" class="form-control" id="pgNoticeWriteTitle" maxlength="500" placeholder="제목"></div>' +
      '<div class="mb-0"><label class="form-label" for="pgNoticeWriteContent">내용</label>' +
      '<textarea class="form-control" id="pgNoticeWriteContent" rows="12" placeholder="내용"></textarea></div>' +
      '</div>' +
      '<div class="modal-footer">' +
      '<button type="button" class="btn btn-secondary" data-bs-dismiss="modal">취소</button>' +
      '<button type="button" class="btn btn-primary" id="pgNoticeWriteSaveBtn">등록</button>' +
      '</div></div></div></div>';
    document.body.appendChild(wrap.firstChild);
    el = document.getElementById('pgNoticeWriteModal');
    var saveBtn = el.querySelector('#pgNoticeWriteSaveBtn');
    if (saveBtn && !saveBtn._pgBound) {
      saveBtn._pgBound = true;
      saveBtn.addEventListener('click', function () {
        var titleEl = el.querySelector('#pgNoticeWriteTitle');
        var contentEl = el.querySelector('#pgNoticeWriteContent');
        var title = titleEl && titleEl.value ? String(titleEl.value).trim() : '';
        var content = contentEl && contentEl.value ? String(contentEl.value) : '';
        if (!title) { alert('제목을 입력하세요.'); return; }
        if (!window.PG_API || !window.PG_API.noticeCreate) { alert('API를 사용할 수 없습니다.'); return; }
        window.PG_API.noticeCreate(title, content).then(function () {
          alert('등록되었습니다.');
          try {
            var M = window.bootstrap && window.bootstrap.Modal;
            if (M) {
              var inst = M.getOrCreateInstance ? M.getOrCreateInstance(el) : M.getInstance(el);
              if (inst) inst.hide();
            }
          } catch (err) {}
          if (titleEl) titleEl.value = '';
          if (contentEl) contentEl.value = '';
          if (typeof el._onNoticeSaved === 'function') {
            try { el._onNoticeSaved(); } catch (e1) {}
          }
          el._onNoticeSaved = null;
        }).catch(function (e) {
          alert(e && e.message ? e.message : '등록 실패');
        });
      });
    }
    return el;
  }

  function openNoticeWriteModal(onSaved) {
    var el = ensureNoticeWriteModal();
    el._onNoticeSaved = typeof onSaved === 'function' ? onSaved : null;
    var titleEl = el.querySelector('#pgNoticeWriteTitle');
    var contentEl = el.querySelector('#pgNoticeWriteContent');
    if (titleEl) titleEl.value = '';
    if (contentEl) contentEl.value = '';
    try {
      var M = window.bootstrap && window.bootstrap.Modal;
      if (!M) { alert('화면 구성을 불러오지 못했습니다.'); return; }
      var modal = M.getOrCreateInstance ? M.getOrCreateInstance(el) : new M(el);
      modal.show();
    } catch (e) {
      alert('공지 등록 창을 열 수 없습니다.');
    }
  }

  // 로고 클릭 / 탭 위임 / 접기
  function syncSessionUserFromAuthMe() {
    if (!window.PG_API || !window.PG_API.authMe) return Promise.resolve();
    var timeoutMs = 15000;
    var timeoutPromise = new Promise(function (_, reject) {
      setTimeout(function () { reject(new Error('timeout')); }, timeoutMs);
    });
    return Promise.race([window.PG_API.authMe(), timeoutPromise]).then(function (r) {
      var d = r && r.data ? r.data : r;
      if (!d || d.ok === false) return;
      try {
        var prev = JSON.parse(sessionStorage.getItem('pg_admin_user') || '{}');
        if (d.pagePermissions !== undefined) prev.pagePermissions = d.pagePermissions;
        if (d.orgLevel !== undefined) prev.orgLevel = d.orgLevel;
        if (d.compId !== undefined) prev.compId = d.compId;
        if (d.orgUnitId !== undefined) prev.orgUnitId = d.orgUnitId;
        if (d.role !== undefined) prev.role = d.role;
        if (d.canWriteNotice !== undefined) prev.canWriteNotice = !!d.canWriteNotice;
        sessionStorage.setItem('pg_admin_user', JSON.stringify(prev));
        syncAllNoticeWriteButtons();
      } catch (e) {}
    }).catch(function () {});
  }

  window.applyPagePermissionToPane = function (pane, url) {
    if (!pane || !url) return;
    var perm = getPagePermissionForUrl(url);
    pane.classList.remove('pg-perm-observer', 'pg-perm-modify');
    function observerAllowTarget(t) {
      if (!t || !t.closest) return false;
      if (t.closest('.screen-search-form')) return true;
      if (t.closest('.pagination-row')) return true;
      if (t.closest('.table-column-guide')) return true;
      if (t.closest('.screen-list-sort-dir-menu')) return true;
      return false;
    }
    if (perm === 'OBSERVER') {
      pane.classList.add('pg-perm-observer');
      pane.querySelectorAll('input, select, textarea').forEach(function (el) {
        if (!el) return;
        if (el.type === 'hidden') return;
        if (el.classList && el.classList.contains('screen-list-sort-dir-select')) return;
        if (observerAllowTarget(el)) return;
        el.disabled = true;
        if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') el.readOnly = true;
      });
      pane.querySelectorAll('button').forEach(function (btn) {
        if ((btn.id || '') === 'compDetailListBtn') {
          btn.disabled = false;
          btn.removeAttribute('aria-disabled');
          btn.classList.remove('disabled');
          btn.style.pointerEvents = '';
          return;
        }
        if (observerAllowTarget(btn)) return;
        if (btn.classList.contains('screen-list-sort-dir-btn')) return;
        if ((btn.id || '') === 'compDevTreeRemoveBtn') return;
        if ((btn.id || '') === 'compAdminResetOrgBtn') return;
        if ((btn.id || '') === 'searchBtn' || btn.classList.contains('screen-search-btn')) return;
        if ((btn.id || '') === 'excelBtn' || (btn.id || '') === 'excelDownBtn' || (btn.id || '') === 'payListRefreshBtn' || (btn.id || '') === 'printBtn') return;
        if (btn.classList.contains('quick-date')) return;
        if (btn.classList.contains('pagination-size-opt')) return;
        if (btn.classList.contains('pagination-num')) return;
        if (btn.getAttribute('data-notice-write-btn')) return;
        btn.disabled = true;
        btn.style.display = '';
      });
      pane.querySelectorAll('a.btn').forEach(function (a) {
        if (!a) return;
        a.setAttribute('aria-disabled', 'true');
        a.classList.add('disabled');
        a.style.display = '';
      });
      // 핵심 저장 버튼은 항상 강제 비활성(표시는 유지)
      ['#compInfoUpdateBtn', '#compDetailSaveBtn', '#compRegSaveBtn', '#hqOrgAllowSaveBtn', '#hqBizdayProfileSaveBtn', '#hqLedgerSysSettingsSaveBtn', '#hqLedgerHelloTimelineSaveBtn'].forEach(function (sel) {
        var b = pane.querySelector(sel);
        if (!b) return;
        b.disabled = true;
        b.style.display = '';
        b.setAttribute('aria-disabled', 'true');
        b.classList.add('disabled');
        if (sel === '#compInfoUpdateBtn' || sel === '#compDetailSaveBtn') {
          b.classList.remove('btn-primary', 'btn-success', 'btn-danger', 'btn-info', 'btn-warning');
          b.classList.add('btn-secondary');
          b.style.pointerEvents = 'none';
        }
      });
      // 옵저버는 조회만 허용: 검색/페이지네이션 외 인터랙션 이벤트 자체 차단
      if (!pane._observerGuardBound) {
        pane._observerGuardBound = true;
        pane.addEventListener('click', function (e) {
          if (getPagePermissionForUrl(url) !== 'OBSERVER') return;
          var t = e.target;
          if (!t || !t.closest) return;
          if (t.closest('#compDetailListBtn')) return;
          if (observerAllowTarget(t)) return;
          if (t.closest('#searchBtn, .screen-search-btn, #excelBtn, #excelDownBtn, #payListRefreshBtn, #printBtn, .quick-date, .pagination-size-opt, .pagination-num, [data-notice-write-btn], #compDevTreeRemoveBtn, #compAdminResetOrgBtn, .screen-list-sort-dir-menu')) return;
          if (t.closest('button, a, [role="button"], .btn, .tab-close-button')) {
            e.preventDefault();
            e.stopPropagation();
            e.stopImmediatePropagation();
          }
        }, true);
        pane.addEventListener('change', function (e) {
          if (getPagePermissionForUrl(url) !== 'OBSERVER') return;
          var t = e.target;
          if (!t || !t.closest) return;
          if (observerAllowTarget(t)) return;
          e.preventDefault();
          e.stopPropagation();
          e.stopImmediatePropagation();
        }, true);
        pane.addEventListener('input', function (e) {
          if (getPagePermissionForUrl(url) !== 'OBSERVER') return;
          var t = e.target;
          if (!t || !t.closest) return;
          if (observerAllowTarget(t)) return;
          e.preventDefault();
          e.stopPropagation();
          e.stopImmediatePropagation();
        }, true);
        pane.addEventListener('submit', function (e) {
          if (getPagePermissionForUrl(url) !== 'OBSERVER') return;
          e.preventDefault();
          e.stopPropagation();
          e.stopImmediatePropagation();
        }, true);
      }
    } else if (perm === 'MODIFY') {
      pane.classList.add('pg-perm-modify');
      pane.querySelectorAll('.btn-danger, button').forEach(function (btn) {
        if ((btn.id || '') === 'compDevTreeRemoveBtn') return;
        if ((btn.id || '') === 'compAdminResetOrgBtn') return;
        var t = (btn.textContent || '').trim();
        if (t.indexOf('삭제') !== -1 || t.indexOf('일괄삭제') !== -1) btn.disabled = true;
      });
    }
  };

  window.initOrgPagePermissionMatrix = function (pane, tabId, data) {
    var capsRaw = (data && data.uiCaps) ? data.uiCaps : {};
    var caps = {};
    Object.keys(capsRaw || {}).forEach(function (k) { caps[k] = capsRaw[k]; });
    if (!Object.prototype.hasOwnProperty.call(caps, 'showLevelTabs')) {
      caps.showLevelTabs = true;
    }
    pane._orgPermUiCaps = caps;
    var matrixCard = pane.querySelector('.org-perm-matrix');
    var defActs = pane.querySelector('.org-perm-default-actions');
    /* 단계 탭만 숨기는 경우(본사·총판): 카드 전체를 숨기면 안내·읽기 전용 메시지 tbody까지 사라져 오류처럼 보입니다. */
    if (matrixCard) matrixCard.style.display = '';
    if (caps.showLevelTabs === false) {
      if (defActs) defActs.style.display = 'none';
    } else {
      if (defActs) defActs.style.display = '';
    }
    var unitSec = pane.querySelector('.org-perm-unit-section');
    if (caps.showOrgUnitPanel === false) {
      if (unitSec) unitSec.style.display = 'none';
    } else if (unitSec) unitSec.style.display = '';
    var assistSec0 = pane.querySelector('.org-perm-assist-section');
    if (caps.showAssistantPanel === false) {
      if (assistSec0) assistSec0.style.display = 'none';
    } else if (assistSec0) assistSec0.style.display = '';
    var hqPermSaveTop = pane.querySelector('#hqPermissionSaveBtn');
    if (hqPermSaveTop) hqPermSaveTop.disabled = caps.canSaveLevelMatrix === false;
    var hqPermReloadTop = pane.querySelector('#hqPermissionReloadBtn');
    if (hqPermReloadTop) hqPermReloadTop.disabled = caps.canSaveLevelMatrix === false;
    var tabs = pane.querySelector('#orgPermTabs_' + tabId);
    var tbody = pane.querySelector('#orgPermTbody_' + tabId);
    if (!tabs || !tbody) {
      var fb = pane.querySelector('[id^="orgPermTbody_"]');
      if (fb) {
        fb.innerHTML = '<tr><td colspan="4" class="text-center text-danger py-4">화면을 불러오지 못했습니다. 탭을 닫았다가 다시 열거나 새로고침 후 시도하세요.</td></tr>';
      }
      return;
    }
    var catalog = (data && Array.isArray(data.catalog)) ? data.catalog : [];
    var matrix = (data && data.matrix && typeof data.matrix === 'object' && !Array.isArray(data.matrix)) ? data.matrix : {};
    var orgLevels = (data && Array.isArray(data.orgLevels)) ? data.orgLevels : [];
    var permOpts = (data && data.permOptions) ? data.permOptions : [
      { v: 'NONE', t: '접근불가' }, { v: 'OBSERVER', t: '옵저버(조회만)' }, { v: 'MODIFY', t: '수정(삭제제한)' }, { v: 'DELETE', t: '삭제(전체)' }
    ];
    var state = {};
    if (caps.showLevelTabs !== false && orgLevels.length) {
      orgLevels.forEach(function (o) {
        var mk = o && o.code != null ? matrix[o.code] : null;
        try {
          state[o.code] = mk && typeof mk === 'object' ? JSON.parse(JSON.stringify(mk)) : {};
        } catch (eCl) {
          state[o.code] = {};
        }
      });
    }
    var activeLv = pane._orgPermActiveLv || (orgLevels.length ? orgLevels[0].code : 'HEADQUARTERS');
    if (orgLevels.length && !orgLevels.some(function (o) { return o.code === activeLv; })) {
      activeLv = orgLevels[0].code;
    }
    if (caps.showLevelTabs === false || !orgLevels.length) {
      tabs.innerHTML = '';
      tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3">조직 단계별 기본 권한은 <strong>총본사</strong>(또는 시스템 관리자)만 편집합니다.</td></tr>';
      pane._orgPermState = state;
      pane._orgPermOrgLevels = orgLevels;
      pane._orgPermActiveLv = activeLv;
      pane._orgPermCatalog = catalog;
      pane._orgPermPermOpts = permOpts;
      if (typeof window.initOrgUnitPermissionPanel === 'function') {
        window.initOrgUnitPermissionPanel(pane, tabId, data);
      }
      return;
    }
    tabs.innerHTML = orgLevels.map(function (o, i) {
      return '<li class="nav-item" role="presentation"><button type="button" class="nav-link' + (i === 0 ? ' active' : '') + '" data-org-level="' + o.code + '">' + o.name + '</button></li>';
    }).join('');
    function normalizeOrgPermCode(v) {
      var x = (v != null ? String(v) : 'DELETE').trim().toUpperCase();
      if (x === 'NONE' || x === 'OBSERVER' || x === 'MODIFY' || x === 'DELETE') return x;
      return 'DELETE';
    }
    function applyOrgPermRowStyle(tr, permCode) {
      if (!tr || !tr.classList || tr.classList.contains('org-perm-group-header')) return;
      var p = normalizeOrgPermCode(permCode);
      tr.classList.remove('org-perm-row--NONE', 'org-perm-row--OBSERVER', 'org-perm-row--MODIFY', 'org-perm-row--DELETE');
      tr.classList.add('org-perm-row', 'org-perm-row--' + p);
      tr.setAttribute('data-perm', p);
    }
    var ORG_PERM_GROUP_ORDER = ['본사설정', '업체관리', '결제관리', '정산관리', '통보관리', '사용자관리', '리스크관리', '배포설정'];
    function buildOrgPermGroups(rows) {
      var by = {};
      (rows || []).forEach(function (row) {
        var g = (row && row.parentGroup) ? String(row.parentGroup).trim() : '기타';
        if (!by[g]) by[g] = [];
        by[g].push(row);
      });
      var out = [];
      ORG_PERM_GROUP_ORDER.forEach(function (name) {
        if (by[name] && by[name].length) {
          out.push({ name: name, rows: by[name] });
          delete by[name];
        }
      });
      Object.keys(by).forEach(function (k) {
        if (by[k].length) out.push({ name: k, rows: by[k] });
      });
      return out;
    }
    function escOrgPermHtml(s) {
      return String(s == null ? '' : s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/"/g, '&quot;');
    }
    function renderRows(lv) {
      activeLv = lv;
      pane._orgPermActiveLv = activeLv;
      var m = state[lv] || {};
      var groups = buildOrgPermGroups(catalog);
      var parts = [];
      var rowSeq = 0;
      var bulkOpts = '<option value="">이 구역 일괄…</option>' + permOpts.map(function (po) {
        return '<option value="' + po.v + '">전체 · ' + escOrgPermHtml(po.t) + '</option>';
      }).join('');
      groups.forEach(function (grp) {
        parts.push(
          '<tr class="org-perm-group-header" role="presentation">' +
          '<td colspan="4" class="org-perm-group-title p-0">' +
          '<div class="d-flex flex-wrap align-items-center justify-content-between gap-2 org-perm-group-bar">' +
          '<span class="org-perm-group-name">' + escOrgPermHtml(grp.name) + '</span>' +
          '<div class="d-flex align-items-center gap-1 flex-shrink-0">' +
          '<span class="text-muted small org-perm-bulk-hint">간편</span>' +
          '<select class="form-select form-select-sm org-perm-bulk-select" title="이 대메뉴 구역의 하위 메뉴에 동일 권한을 한 번에 적용합니다">' +
          bulkOpts +
          '</select></div></div></td></tr>'
        );
        grp.rows.forEach(function (row) {
          rowSeq += 1;
          var url = row.pageUrl || '';
          var cur = normalizeOrgPermCode(m[url] != null ? m[url] : 'DELETE');
          var opts = permOpts.map(function (po) {
            return '<option value="' + po.v + '"' + (po.v === cur ? ' selected' : '') + '>' + po.t + '</option>';
          }).join('');
          parts.push(
            '<tr class="org-perm-row org-perm-row--' + cur + '" data-perm="' + cur + '" data-page-url="' + url.replace(/"/g, '&quot;') + '">' +
            '<td class="text-center text-muted small org-perm-td-no">' + rowSeq + '</td>' +
            '<td class="font-monospace small">' + escOrgPermHtml(row.menuId || '') + '</td>' +
            '<td>' + escOrgPermHtml(row.menuNm || '') + '<div class="text-muted small">' + escOrgPermHtml(url) + '</div></td>' +
            '<td><select class="form-select form-select-sm org-perm-select" data-url="' + url.replace(/"/g, '&quot;') + '">' + opts + '</select></td></tr>'
          );
        });
      });
      tbody.innerHTML = parts.join('');
      tbody.querySelectorAll('.org-perm-bulk-select').forEach(function (bulkSel) {
        bulkSel.addEventListener('change', function () {
          var v = bulkSel.value;
          if (!v) return;
          var hdr = bulkSel.closest('tr.org-perm-group-header');
          if (!hdr) return;
          var n = hdr.nextElementSibling;
          while (n && !n.classList.contains('org-perm-group-header')) {
            var rowSel = n.querySelector('.org-perm-select');
            if (rowSel) {
              rowSel.value = v;
              var u0 = rowSel.getAttribute('data-url') || '';
              if (!state[activeLv]) state[activeLv] = {};
              state[activeLv][u0] = v;
              applyOrgPermRowStyle(n, v);
            }
            n = n.nextElementSibling;
          }
          bulkSel.selectedIndex = 0;
        });
      });
      tbody.querySelectorAll('.org-perm-select').forEach(function (sel) {
        sel.addEventListener('change', function () {
          var u0 = sel.getAttribute('data-url') || '';
          if (!state[activeLv]) state[activeLv] = {};
          state[activeLv][u0] = sel.value;
          applyOrgPermRowStyle(sel.closest('tr'), sel.value);
        });
      });
    }
    renderRows(activeLv);
    tabs.querySelectorAll('button[data-org-level]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        tabs.querySelectorAll('button[data-org-level]').forEach(function (b) { b.classList.remove('active'); });
        btn.classList.add('active');
        renderRows(btn.getAttribute('data-org-level') || 'HEADQUARTERS');
      });
    });
    tabs.querySelectorAll('button[data-org-level]').forEach(function (b) {
      if ((b.getAttribute('data-org-level') || '') === activeLv) b.classList.add('active');
      else b.classList.remove('active');
    });
    if (!tabs.querySelector('button[data-org-level].active')) {
      var firstTab = tabs.querySelector('button[data-org-level]');
      if (firstTab) firstTab.classList.add('active');
    }
    pane._orgPermState = state;
    pane._orgPermOrgLevels = orgLevels;
    pane._orgPermActiveLv = activeLv;
    pane._orgPermCatalog = catalog;
    pane._orgPermPermOpts = permOpts;
    if (typeof window.initOrgUnitPermissionPanel === 'function') {
      window.initOrgUnitPermissionPanel(pane, tabId, data);
    }
  };

  window.initOrgUnitPermissionPanel = function (pane, tabId, data) {
    var sel = pane.querySelector('#orgPermUnitSelect_' + tabId);
    var codeEl = pane.querySelector('#orgPermUnitCode_' + tabId);
    var levelEl = pane.querySelector('#orgPermUnitLevel_' + tabId);
    var currentModeEl = pane.querySelector('#orgPermUnitCurrentMode_' + tabId);
    var modeEl = pane.querySelector('#orgPermUnitMode_' + tabId);
    var tbody = pane.querySelector('#orgPermUnitTbody_' + tabId);
    var hint = pane.querySelector('#orgPermUnitHint_' + tabId);
    var saveBtn = pane.querySelector('#hqOrgUnitPermissionSaveBtn_' + tabId);
    var catalog = pane._orgPermCatalog || (data && data.catalog) || [];
    var permOpts = pane._orgPermPermOpts || [
      { v: 'NONE', t: '접근불가' }, { v: 'OBSERVER', t: '옵저버(조회만)' }, { v: 'MODIFY', t: '수정(삭제제한)' }, { v: 'DELETE', t: '삭제(전체)' }
    ];
    pane._orgPermPayloadOrgLevels = (data && data.orgLevels) ? data.orgLevels : (pane._orgPermPayloadOrgLevels || []);
    if (!sel || !modeEl || !tbody) return;

    var uiCapsPanel = pane._orgPermUiCaps || {};
    if (saveBtn && uiCapsPanel.canSaveOrgUnit === false) {
      saveBtn.disabled = true;
    }
    if (modeEl && uiCapsPanel.canSaveOrgUnit === false) {
      modeEl.disabled = true;
    }

    var ORG_PERM_GROUP_ORDER = ['본사설정', '업체관리', '결제관리', '정산관리', '통보관리', '사용자관리', '리스크관리', '배포설정'];

    function escOrgPermHtml(s) {
      return String(s == null ? '' : s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/"/g, '&quot;');
    }
    function normalizeOrgPermCode(v) {
      var x = (v != null ? String(v) : 'DELETE').trim().toUpperCase();
      if (x === 'NONE' || x === 'OBSERVER' || x === 'MODIFY' || x === 'DELETE') return x;
      return 'DELETE';
    }
    function applyOrgPermRowStyle(tr, permCode) {
      if (!tr || !tr.classList || tr.classList.contains('org-perm-group-header')) return;
      var p = normalizeOrgPermCode(permCode);
      tr.classList.remove('org-perm-row--NONE', 'org-perm-row--OBSERVER', 'org-perm-row--MODIFY', 'org-perm-row--DELETE');
      tr.classList.add('org-perm-row', 'org-perm-row--' + p);
      tr.setAttribute('data-perm', p);
    }
    function buildOrgPermGroups(rows) {
      var by = {};
      (rows || []).forEach(function (row) {
        var g = (row && row.parentGroup) ? String(row.parentGroup).trim() : '기타';
        if (!by[g]) by[g] = [];
        by[g].push(row);
      });
      var out = [];
      ORG_PERM_GROUP_ORDER.forEach(function (name) {
        if (by[name] && by[name].length) {
          out.push({ name: name, rows: by[name] });
          delete by[name];
        }
      });
      Object.keys(by).forEach(function (k) {
        if (by[k].length) out.push({ name: k, rows: by[k] });
      });
      return out;
    }

    function resolveOrgLevelNameKo(u) {
      if (!u) return '';
      if (u.orgLevelName) return u.orgLevelName;
      var lv = pane._orgPermPayloadOrgLevels || [];
      var code = u.orgLevel || '';
      for (var li = 0; li < lv.length; li++) {
        if (lv[li].code === code) return lv[li].name || code;
      }
      return code;
    }

    function modeText(mode) {
      return mode === 'CUSTOM' ? '개별 설정' : '단계 기본 따름';
    }
    function fillOrgInfo(u) {
      if (codeEl) codeEl.value = u ? (u.code || '') : '';
      if (levelEl) levelEl.value = u ? resolveOrgLevelNameKo(u) : '';
      if (currentModeEl) currentModeEl.value = modeText(u && u.mode ? u.mode : 'LEVEL_DEFAULT');
    }
    function renderOrgUnitSelect(unitList, selectedId) {
      var units = unitList || [];
      pane._orgUnitList = units;
      sel.innerHTML = '<option value="">— 업체를 선택하세요 —</option>' + units.map(function (u) {
        return '<option value="' + String(u.id) + '">' + escOrgPermHtml(u.name || '') + '</option>';
      }).join('');
      if (selectedId) sel.value = String(selectedId);
    }

    function renderUnitRows(permMap, readOnly) {
      var m = permMap || {};
      var groups = buildOrgPermGroups(catalog);
      var parts = [];
      var rowSeq = 0;
      var bulkOpts = '<option value="">이 구역 일괄…</option>' + permOpts.map(function (po) {
        return '<option value="' + po.v + '">전체 · ' + escOrgPermHtml(po.t) + '</option>';
      }).join('');
      groups.forEach(function (grp) {
        parts.push(
          '<tr class="org-perm-group-header" role="presentation">' +
          '<td colspan="4" class="org-perm-group-title p-0">' +
          '<div class="d-flex flex-wrap align-items-center justify-content-between gap-2 org-perm-group-bar">' +
          '<span class="org-perm-group-name">' + escOrgPermHtml(grp.name) + '</span>' +
          '<div class="d-flex align-items-center gap-1 flex-shrink-0">' +
          '<span class="text-muted small org-perm-bulk-hint">간편</span>' +
          '<select class="form-select form-select-sm org-perm-bulk-select org-perm-unit-bulk"' + (readOnly ? ' disabled' : '') + ' title="이 대메뉴 구역의 하위 메뉴에 동일 권한을 한 번에 적용합니다">' +
          bulkOpts +
          '</select></div></div></td></tr>'
        );
        grp.rows.forEach(function (row) {
          rowSeq += 1;
          var url = row.pageUrl || '';
          var cur = normalizeOrgPermCode(m[url] != null ? m[url] : 'DELETE');
          var opts = permOpts.map(function (po) {
            return '<option value="' + po.v + '"' + (po.v === cur ? ' selected' : '') + '>' + po.t + '</option>';
          }).join('');
          parts.push(
            '<tr class="org-perm-row org-perm-row--' + cur + '" data-perm="' + cur + '" data-page-url="' + url.replace(/"/g, '&quot;') + '">' +
            '<td class="text-center text-muted small org-perm-td-no">' + rowSeq + '</td>' +
            '<td class="font-monospace small">' + escOrgPermHtml(row.menuId || '') + '</td>' +
            '<td>' + escOrgPermHtml(row.menuNm || '') + '<div class="text-muted small">' + escOrgPermHtml(url) + '</div></td>' +
            '<td><select class="form-select form-select-sm org-perm-select org-perm-unit-select"' + (readOnly ? ' disabled' : '') + ' data-url="' + url.replace(/"/g, '&quot;') + '">' + opts + '</select></td></tr>'
          );
        });
      });
      tbody.innerHTML = parts.join('');
      pane._orgUnitPermState = {};
      Object.keys(m).forEach(function (k) { pane._orgUnitPermState[k] = m[k]; });
      tbody.querySelectorAll('.org-perm-unit-bulk').forEach(function (bulkSel) {
        bulkSel.addEventListener('change', function () {
          if (readOnly) return;
          var v = bulkSel.value;
          if (!v) return;
          var hdr = bulkSel.closest('tr.org-perm-group-header');
          if (!hdr) return;
          var n = hdr.nextElementSibling;
          while (n && !n.classList.contains('org-perm-group-header')) {
            var rowSel = n.querySelector('.org-perm-unit-select');
            if (rowSel) {
              rowSel.value = v;
              var u0 = rowSel.getAttribute('data-url') || '';
              pane._orgUnitPermState[u0] = v;
              applyOrgPermRowStyle(n, v);
            }
            n = n.nextElementSibling;
          }
          bulkSel.selectedIndex = 0;
        });
      });
      tbody.querySelectorAll('.org-perm-unit-select').forEach(function (selEl) {
        selEl.addEventListener('change', function () {
          if (readOnly) return;
          var u0 = selEl.getAttribute('data-url') || '';
          pane._orgUnitPermState[u0] = selEl.value;
          applyOrgPermRowStyle(selEl.closest('tr'), selEl.value);
        });
      });
    }

    function permStrengthAssist(v) {
      var p = (v != null ? String(v) : 'DELETE').toUpperCase();
      if (p === 'DELETE') return 4;
      if (p === 'MODIFY') return 3;
      if (p === 'OBSERVER') return 2;
      if (p === 'NONE') return 1;
      return 4;
    }

    function syncOrgAssistantPanel(det) {
      var assistSec = pane.querySelector('.org-perm-assist-section');
      var roleTabs = pane.querySelector('#orgPermAssistRoleTabs_' + tabId);
      var atbody = pane.querySelector('#orgPermAssistTbody_' + tabId);
      var asave = pane.querySelector('#hqOrgAssistSaveBtn_' + tabId);
      if (!assistSec || uiCapsPanel.showAssistantPanel === false) return;
      if (!roleTabs || !atbody) return;
      if (!det || !det.effective) {
        atbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3">조직을 선택하세요.</td></tr>';
        roleTabs.innerHTML = '';
        if (asave) asave.disabled = true;
        return;
      }
      var roles = det.assistantRoles && det.assistantRoles.length ? det.assistantRoles : ['MANAGER', 'OPERATOR', 'SETTLEMENT', 'TECH'];
      var matrix = det.assistantMatrix || {};
      pane._assistDetailOrgId = det.orgUnit && det.orgUnit.id != null ? String(det.orgUnit.id) : '';
      pane._assistEffective = det.effective || {};
      pane._assistMatrixState = {};
      roles.forEach(function (r) {
        pane._assistMatrixState[r] = JSON.parse(JSON.stringify(matrix[r] || {}));
      });
      if (!pane._assistActiveRole || roles.indexOf(pane._assistActiveRole) < 0) pane._assistActiveRole = roles[0];
      pane._assistRoles = roles;

      function roleLabel(r) {
        if (r === 'MANAGER') return '관리(MANAGER)';
        if (r === 'OPERATOR') return '운영(OPERATOR)';
        if (r === 'SETTLEMENT') return '정산(SETTLEMENT)';
        if (r === 'TECH') return '기술(TECH)';
        return r;
      }

      function buildOptsForCeiling(ceiling, currentVal) {
        var c = permStrengthAssist(ceiling);
        var parts = '<option value="">조직 기본(상한)</option>';
        permOpts.forEach(function (po) {
          if (po.v === 'NONE') return;
          if (permStrengthAssist(po.v) <= c) {
            parts += '<option value="' + po.v + '"' + (currentVal === po.v ? ' selected' : '') + '>' + escOrgPermHtml(po.t) + '</option>';
          }
        });
        return parts;
      }

      function renderAssistRowsForRole(role) {
        pane._assistActiveRole = role;
        var eff = pane._assistEffective || {};
        var m = pane._assistMatrixState[role] || {};
        var groups = buildOrgPermGroups(catalog);
        var parts = [];
        var rowSeq = 0;
        groups.forEach(function (grp) {
          grp.rows.forEach(function (row) {
            var url = row.pageUrl || '';
            var ceiling = normalizeOrgPermCode(eff[url] != null ? eff[url] : 'DELETE');
            if (ceiling === 'NONE') return;
            rowSeq += 1;
            var stored = m[url] != null ? normalizeOrgPermCode(m[url]) : '';
            var selVal = stored && permStrengthAssist(stored) <= permStrengthAssist(ceiling) ? stored : '';
            var opts = buildOptsForCeiling(ceiling, selVal);
            var disp = selVal || ceiling;
            parts.push(
              '<tr class="org-perm-row org-perm-row--' + disp + '" data-page-url="' + url.replace(/"/g, '&quot;') + '">' +
              '<td class="text-center text-muted small">' + rowSeq + '</td>' +
              '<td class="font-monospace small">' + escOrgPermHtml(row.menuId || '') + '</td>' +
              '<td>' + escOrgPermHtml(row.menuNm || '') + '<div class="text-muted small">조직 상한: ' + escOrgPermHtml(ceiling) + '</div><div class="text-muted small">' + escOrgPermHtml(url) + '</div></td>' +
              '<td><select class="form-select form-select-sm org-perm-assist-select" data-url="' + url.replace(/"/g, '&quot;') + '" data-ceiling="' + ceiling + '"' + (uiCapsPanel.canSaveAssistant ? '' : ' disabled') + '>' + opts + '</select></td></tr>'
            );
          });
        });
        if (!parts.length) {
          atbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3">이 조직에서는 접근 가능한 메뉴가 없습니다.</td></tr>';
          return;
        }
        atbody.innerHTML = parts.join('');
        atbody.querySelectorAll('.org-perm-assist-select').forEach(function (selEl) {
          selEl.addEventListener('change', function () {
            var u0 = selEl.getAttribute('data-url') || '';
            var v = selEl.value;
            var st = pane._assistMatrixState[pane._assistActiveRole] || {};
            if (!v) delete st[u0];
            else st[u0] = v;
            pane._assistMatrixState[pane._assistActiveRole] = st;
            var cl = selEl.getAttribute('data-ceiling') || 'DELETE';
            applyOrgPermRowStyle(selEl.closest('tr'), v || normalizeOrgPermCode(cl));
          });
        });
      }

      roleTabs.innerHTML = roles.map(function (r) {
        return '<li class="nav-item" role="presentation"><button type="button" class="nav-link' + (r === pane._assistActiveRole ? ' active' : '') + '" data-assist-role="' + r + '">' + escOrgPermHtml(roleLabel(r)) + '</button></li>';
      }).join('');
      roleTabs.querySelectorAll('button[data-assist-role]').forEach(function (btn) {
        btn.addEventListener('click', function () {
          roleTabs.querySelectorAll('button[data-assist-role]').forEach(function (b) { b.classList.remove('active'); });
          btn.classList.add('active');
          renderAssistRowsForRole(btn.getAttribute('data-assist-role') || roles[0]);
        });
      });
      renderAssistRowsForRole(pane._assistActiveRole);
      if (asave) asave.disabled = !uiCapsPanel.canSaveAssistant || !pane._assistDetailOrgId;
    }

    var units = (data && data.orgUnits) ? data.orgUnits : [];
    var prevSelectedId = sel.value || '';
    renderOrgUnitSelect(units, prevSelectedId);
    if (units.length === 1 && !prevSelectedId) {
      sel.value = String(units[0].id);
    }

    function loadOrgUnit(id) {
      if (!id) {
        fillOrgInfo(null);
        modeEl.value = 'LEVEL_DEFAULT';
        modeEl.disabled = true;
        if (saveBtn) saveBtn.disabled = true;
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-4">조직을 선택하세요.</td></tr>';
        if (hint) hint.textContent = '조직을 선택하면 적용 방식과 권한 표가 채워집니다.';
        pane._orgUnitDetailLevelDefault = null;
        pane._orgUnitDetailEffective = null;
        syncOrgAssistantPanel(null);
        return;
      }
      var selected = (pane._orgUnitList || []).find(function (u) { return String(u.id) === String(id); }) || null;
      fillOrgInfo(selected);
      if (!window.PG_API || !window.PG_API.hqOrgUnitPermission) return;
      var dimm = document.getElementById('dimm');
      if (dimm) dimm.style.display = 'flex';
      window.PG_API.hqOrgUnitPermission({ orgUnitId: id }).then(function (res) {
        var det = res && res.data !== undefined ? res.data : res;
        if (!det) return;
        var mode = (det.mode === 'CUSTOM') ? 'CUSTOM' : 'LEVEL_DEFAULT';
        modeEl.value = mode;
        modeEl.disabled = uiCapsPanel.canSaveOrgUnit === false;
        if (saveBtn) saveBtn.disabled = uiCapsPanel.canSaveOrgUnit === false;
        if (currentModeEl) currentModeEl.value = modeText(mode);
        if (selected) selected.mode = mode;
        var levelDef = det.levelDefault || {};
        var eff = det.effective || {};
        pane._orgUnitDetailLevelDefault = levelDef;
        pane._orgUnitDetailEffective = eff;
        if (hint) {
          hint.innerHTML = mode === 'CUSTOM'
            ? '<span class="text-primary">개별 설정</span>이 저장되어 있습니다. 아래는 <strong>로그인 시 적용되는 최종 권한</strong>입니다.'
            : '<span class="text-secondary">단계 기본 따름</span> — 아래는 해당 조직 단계의 <strong>기본 매트릭스와 동일한 적용 결과</strong>입니다.';
        }
        var readOnly = (mode === 'LEVEL_DEFAULT') || uiCapsPanel.canSaveOrgUnit === false;
        renderUnitRows(readOnly ? levelDef : eff, readOnly);
        syncOrgAssistantPanel(det);
      }).catch(function (e) {
        alert(e && e.message ? e.message : '조회 실패');
      }).finally(function () { if (dimm) dimm.style.display = 'none'; });
    }

    if (!sel._orgUnitBound) {
      sel._orgUnitBound = true;
      sel.addEventListener('change', function () {
        loadOrgUnit(sel.value);
      });
    }
    if (!modeEl._orgUnitModeBound) {
      modeEl._orgUnitModeBound = true;
      modeEl.addEventListener('change', function () {
        var ld = pane._orgUnitDetailLevelDefault || {};
        var eff = pane._orgUnitDetailEffective || {};
        if (modeEl.value === 'LEVEL_DEFAULT') {
          if (hint) hint.innerHTML = '<span class="text-secondary">단계 기본 따름</span>(저장 시 개별 덮어쓰기가 제거됩니다). 미리보기는 기본 매트릭스와 동일합니다.';
          renderUnitRows(ld, true);
        } else {
          if (hint) hint.innerHTML = '<span class="text-primary">개별 설정</span> — 아래에서 수정 후 상단 [설정저장]을 누르세요.';
          var base = eff && Object.keys(eff).length ? eff : ld;
          renderUnitRows(JSON.parse(JSON.stringify(base)), uiCapsPanel.canSaveOrgUnit === false);
        }
        if (!sel.value) return;
        var previewEff = modeEl.value === 'LEVEL_DEFAULT' ? ld : (eff && Object.keys(eff).length ? eff : ld);
        var fakeDet = {
          effective: previewEff,
          assistantRoles: pane._assistRoles || ['MANAGER', 'OPERATOR', 'SETTLEMENT', 'TECH'],
          assistantMatrix: pane._assistMatrixState || {},
          orgUnit: { id: sel.value }
        };
        syncOrgAssistantPanel(fakeDet);
      });
    }
    if (saveBtn && !saveBtn._orgUnitSaveBound) {
      saveBtn._orgUnitSaveBound = true;
      saveBtn.addEventListener('click', function () {
        var id = sel.value;
        if (!id) { alert('조직을 선택하세요.'); return; }
        if (!window.PG_API || !window.PG_API.hqOrgUnitPermissionSave) return;
        if (!pgConfirmBeforeSave('저장하시겠습니까?')) return;
        var mode = modeEl.value;
        var pages = pane._orgUnitPermState || {};
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.hqOrgUnitPermissionSave({ orgUnitId: id, mode: mode, pages: pages }).then(function (res) {
          var det = res && res.data !== undefined ? res.data : res;
          alert('저장되었습니다.');
          var list = (pane._orgUnitList || []).slice();
          var newMode = det && det.mode ? det.mode : mode;
          for (var ji = 0; ji < list.length; ji++) {
            if (String(list[ji].id) === String(id)) {
              list[ji].mode = newMode;
              break;
            }
          }
          pane._orgUnitList = list;
          var selected = list.find(function (u) { return String(u.id) === String(id); }) || null;
          fillOrgInfo(selected);
          loadOrgUnit(id);
        }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      });
    }
    var asaveBtn = pane.querySelector('#hqOrgAssistSaveBtn_' + tabId);
    if (asaveBtn && !asaveBtn._assistSaveBound) {
      asaveBtn._assistSaveBound = true;
      asaveBtn.addEventListener('click', function () {
        var oid = pane._assistDetailOrgId;
        if (!oid) { alert('조직을 선택하세요.'); return; }
        if (!window.PG_API || !window.PG_API.hqOrgUnitAssistantPermissionSave) return;
        if (!pgConfirmBeforeSave('담당자 권한그룹별 메뉴를 저장하시겠습니까?')) return;
        var dimmA = document.getElementById('dimm');
        if (dimmA) dimmA.style.display = 'flex';
        window.PG_API.hqOrgUnitAssistantPermissionSave({ orgUnitId: oid, matrix: pane._assistMatrixState || {} }).then(function (res) {
          alert('저장되었습니다.');
          var det = res && res.data !== undefined ? res.data : res;
          if (det) syncOrgAssistantPanel(det);
        }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimmA) dimmA.style.display = 'none'; });
      });
    }
    if (sel.value) loadOrgUnit(sel.value);
    else if (units.length === 1) loadOrgUnit(String(units[0].id));
    else loadOrgUnit('');
  };

  document.addEventListener('DOMContentLoaded', function () {
    syncSessionUserFromAuthMe().finally(function () {
      applyAdminOnlyMenuItems();
      applyMenuVisibilityByPagePermissions();
      redirectIfActiveMenuForbidden();
    });
    setTableRowPaddingY(getTableRowPaddingY());
    var ul = document.getElementById(TAB_UL);
    if (ul) {
      ul.addEventListener('click', function (e) {
        var a = e.target.closest('.tab-a');
        if (!a) return;
        e.preventDefault();
        var li = a.closest('.copyTopTab');
        if (!li) return;
        var tabUrl = li.getAttribute('top_tab_url');
        var mid = a.getAttribute('menu_id');
        document.querySelectorAll('#' + TAB_UL + ' .tab-a').forEach(function (x) { x.classList.remove('active'); });
        a.classList.add('active');
        setActiveMenuByUrl(tabUrl);
        loadContent(tabUrl, mid, a.textContent.trim());
      });
    }
    var logo = document.getElementById('leftside-logo');
    if (logo && !logo.getAttribute('data-bound')) {
      logo.setAttribute('data-bound', '1');
      logo.addEventListener('click', function (e) {
        if (!config.contentBaseUrl) {
          e.preventDefault();
          fnTopMenuMove('/main', null, '메인');
        }
      });
    }
    // 대메뉴 플라이아웃·aria (접기 버튼보다 먼저 선언)
    var flyout = document.getElementById('flyout-submenu');
    function hideFlyout() {
      if (flyout) flyout.style.display = 'none';
    }
    function syncSideNavExpandedAria() {
      document.querySelectorAll('#side-nav-ul > .side-nav-item').forEach(function (item) {
        var link = item.querySelector('.side-nav-link');
        var sub = item.querySelector('.side-nav-second-level');
        if (!link || !sub) return;
        link.setAttribute('aria-expanded', sub.classList.contains('mm-show') ? 'true' : 'false');
      });
    }
    // 접기/펴기 버튼
    var foldBtn = document.getElementById('leftSideFoldBtn');
    var leftMenu = document.querySelector('.left-side-menu');
    if (foldBtn && leftMenu) {
      foldBtn.addEventListener('click', function () {
        var isCollapsing = !leftMenu.classList.contains('collapsed');
        leftMenu.classList.toggle('collapsed');

        // CSS 적용이 안 되더라도 확실히 접히도록 width/minWidth를 직접 제어
        if (leftMenu.classList.contains('collapsed')) {
          leftMenu.style.width = '70px';
          leftMenu.style.minWidth = '70px';
        } else {
          leftMenu.style.width = '260px';
          leftMenu.style.minWidth = '260px';
        }

        if (isCollapsing) {
          document.querySelectorAll('.side-nav-second-level').forEach(function (el) { el.classList.remove('mm-show'); });
          document.querySelectorAll('.side-nav-item').forEach(function (el) { el.classList.remove('mm-active'); });
        }

        // 접기 → 펴기 전환 시에도 항상 플라이아웃은 감춰야 함
        var f = document.getElementById('flyout-submenu');
        if (f) f.style.display = 'none';

        var span = document.getElementById('leftSideFoldSpan');
        var icon = document.getElementById('leftSideFoldIcon');
        if (span) span.textContent = leftMenu.classList.contains('collapsed') ? ' » 펴기' : ' « 접기';
        if (icon) icon.className = leftMenu.classList.contains('collapsed') ? 'bi bi-chevron-double-right' : 'bi bi-chevron-double-left';
        syncSideNavExpandedAria();
      });
    }
    // 대메뉴 클릭 → 펼침: 토글 / 접힘: 플라이아웃으로 서브 표시 (사이드바 유지)
    document.querySelectorAll('.side-nav .side-nav-link').forEach(function (a) {
      a.addEventListener('click', function (e) {
        if (this.getAttribute('href') !== 'javascript:void(0)' && this.getAttribute('href') !== '#') return;
        e.preventDefault();
        var parent = this.closest('.side-nav-item');
        var sub = parent && parent.querySelector('.side-nav-second-level');
        if (!sub) return;
        var left = document.querySelector('.left-side-menu');
        if (left && left.classList.contains('collapsed') && flyout) {
          var isSame = parent.classList.contains('mm-active') && flyout.style.display === 'block';
          if (isSame) {
            hideFlyout();
            parent.classList.remove('mm-active');
            syncSideNavExpandedAria();
            return;
          }
          var rect = this.getBoundingClientRect();
          flyout.innerHTML = '';
          var clone = sub.cloneNode(true);
          clone.classList.add('mm-show');
          flyout.appendChild(clone);
          flyout.style.top = rect.top + 'px';
          flyout.style.display = 'block';
          document.querySelectorAll('.side-nav-item').forEach(function (el) { el.classList.remove('mm-active'); });
          parent.classList.add('mm-active');
          syncSideNavExpandedAria();
        } else {
          // 펼쳐진 상태: 항상 한 개의 대메뉴만 열리도록 처리
          hideFlyout();
          var isOpen = sub.classList.contains('mm-show');
          document.querySelectorAll('.side-nav-second-level').forEach(function (el) { el.classList.remove('mm-show'); });
          document.querySelectorAll('.side-nav-item').forEach(function (el) { el.classList.remove('mm-active'); });
          if (!isOpen) {
            parent.classList.add('mm-active');
            sub.classList.add('mm-show');
          }
          syncSideNavExpandedAria();
        }
      });
    });
    document.addEventListener('click', function (e) {
      if (flyout && flyout.style.display === 'block') {
        if (!flyout.contains(e.target) && !e.target.closest('.left-side-menu')) hideFlyout();
      }
    });
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') hideFlyout();
    });
    // 서브메뉴(.child-li) 클릭 → document 위임으로 사이드바+플라이아웃 모두 처리
    document.addEventListener('click', function (e) {
      var a = e.target.closest('.child-li a');
      if (!a) return;
      e.preventDefault();
      var li = a.closest('.child-li');
      var url = li && li.getAttribute('data-url');
      if (!url) return;
      hideFlyout();
      fnTopMenuMove(url, a.getAttribute('data-menu_id'), a.textContent.trim());
    });
    var pgAgSave = document.getElementById('pgAgencyEditSaveBtn');
    if (pgAgSave && !pgAgSave._bound) {
      pgAgSave._bound = true;
      pgAgSave.addEventListener('click', function () {
        if (!window.pgDoubleConfirm || !window.pgDoubleConfirm('PG사 연동 정보를 저장하시겠습니까?', '정말 저장하시겠습니까?')) return;
        var idVal = document.getElementById('pgAgencyEditId').value.trim();
        var kindSel = document.getElementById('pgAgencyEditIntegKind');
        var integKind = kindSel && kindSel.value ? String(kindSel.value).trim().toUpperCase() : '';
        var body = {
          pgCd: document.getElementById('pgAgencyEditPgCd').value.trim(),
          pgNm: document.getElementById('pgAgencyEditPgNm').value.trim(),
          apiEndpoint: (document.getElementById('pgAgencyEditEndpoint') && document.getElementById('pgAgencyEditEndpoint').value) ? document.getElementById('pgAgencyEditEndpoint').value.trim() : '',
          integKind: integKind,
          integrationEndpoint: (document.getElementById('pgAgencyEditIntegrationEndpoint') && document.getElementById('pgAgencyEditIntegrationEndpoint').value.trim())
            ? document.getElementById('pgAgencyEditIntegrationEndpoint').value.trim() : '',
          useYn: (document.getElementById('pgAgencyEditUseYn') && document.getElementById('pgAgencyEditUseYn').value) || 'Y',
          mid: (document.getElementById('pgAgencyEditMid') && document.getElementById('pgAgencyEditMid').value) ? document.getElementById('pgAgencyEditMid').value.trim() : '',
          routeNo: (document.getElementById('pgAgencyEditRouteNo') && document.getElementById('pgAgencyEditRouteNo').value.trim()) ? document.getElementById('pgAgencyEditRouteNo').value.trim() : '',
          sandboxYn: (document.getElementById('pgAgencyEditSandboxYn') && document.getElementById('pgAgencyEditSandboxYn').value) ? document.getElementById('pgAgencyEditSandboxYn').value : 'Y',
          credentialsExtraJson: (document.getElementById('pgAgencyEditCredentialsExtra') && document.getElementById('pgAgencyEditCredentialsExtra').value.trim())
            ? document.getElementById('pgAgencyEditCredentialsExtra').value.trim() : ''
        };
        var ak = document.getElementById('pgAgencyEditApiKey');
        var mk = document.getElementById('pgAgencyEditMd5Key');
        if (ak && ak.value.trim()) body.apiKey = ak.value.trim();
        if (mk && mk.value.trim()) body.md5Key = mk.value.trim();
        if (idVal) body.id = idVal;
        if (!body.pgCd || !body.pgNm) { alert('PG코드와 결제대행사는 필수입니다.'); return; }
        if (!body.integKind) { alert('연동 용도를 선택하세요. 용도별로 PG코드를 나누어 등록합니다.'); return; }
        if (integKind === 'URL_PAY') {
          var upm = document.getElementById('pgAgencyEditUrlPayAmountMode');
          body.urlPayAmountMode = upm && upm.value ? String(upm.value).trim().toUpperCase() : 'STANDARD';
          if (body.urlPayAmountMode !== 'DISPLAY') body.urlPayAmountMode = 'STANDARD';
        }
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.hqPgApiMngSave(body).then(function () {
          alert('저장되었습니다.');
          var modalEl = document.getElementById('pgAgencyEditModal');
          if (modalEl && window.bootstrap && bootstrap.Modal) {
            var inst = bootstrap.Modal.getInstance(modalEl);
            if (inst) inst.hide();
          }
          var hqPane = document.getElementById('hq_pgApiMng');
          if (hqPane) {
            var sb = hqPane.querySelector('#searchBtn');
            if (sb) sb.click();
          }
        }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      });
    }
  });
})();
