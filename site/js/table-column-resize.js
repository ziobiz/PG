/**
 * 데이터 테이블 공통: 헤더(마지막 헤더 행 기준) 오른쪽 가장자리에서 드래그해 열 너비 조절.
 * - 대상: class 에 table 이 있는 표. 제외: .table-no-col-resize, 상위 .table-no-col-resize-wrap
 * - 다단 thead 는 격자를 채운 뒤 마지막 행 기준으로 열별 th 에 핸들 부착
 * - id 가 grid_* 인 표는 localStorage(pg_col_w_<id>)에 열 너비 저장
 */
(function (global) {
  'use strict';

  var MIN_COL = 40;
  var STORAGE_PREFIX = 'pg_col_w_';
  var applying = false;
  /** 동일 root 에 refreshIn 이 짧은 간격으로 여러 번 오면(페이지네이션·MutationObserver·finally) 한 번으로 합쳐 떨림 방지 */
  var refreshDebounceTimers = new WeakMap();
  var REFRESH_DEBOUNCE_MS = 50;

  /** MutationObserver 가 colgroup·핸들만 보고 refreshIn 을 반복 호출하면 화면이 떨림 → 해당 뮤테이션은 무시 */
  function mutationDomNodeIsColResizeUi(node) {
    if (!node) {
      return true;
    }
    if (node.nodeType !== 1) {
      return true;
    }
    var el = node;
    if (el.classList && el.classList.contains('pg-col-resize-handle')) {
      return true;
    }
    if (el.tagName === 'COLGROUP' && el.classList && el.classList.contains('pg-col-resize-group')) {
      return true;
    }
    if (el.tagName === 'COL' && el.parentElement && el.parentElement.classList &&
        el.parentElement.classList.contains('pg-col-resize-group')) {
      return true;
    }
    return false;
  }

  function mutationRecordIsOnlyColResizeChildList(m) {
    if (!m || m.type !== 'childList') {
      return false;
    }
    var i;
    for (i = 0; i < m.addedNodes.length; i++) {
      if (!mutationDomNodeIsColResizeUi(m.addedNodes[i])) {
        return false;
      }
    }
    for (i = 0; i < m.removedNodes.length; i++) {
      if (!mutationDomNodeIsColResizeUi(m.removedNodes[i])) {
        return false;
      }
    }
    return m.addedNodes.length + m.removedNodes.length > 0;
  }

  function mutationRecordsAllColResizeOnly(records) {
    if (!records || !records.length) {
      return false;
    }
    var r;
    for (r = 0; r < records.length; r++) {
      if (!mutationRecordIsOnlyColResizeChildList(records[r])) {
        return false;
      }
    }
    return true;
  }

  function eligibleTable(t) {
    if (!t || t.tagName !== 'TABLE') return false;
    if (!t.classList || !t.classList.contains('table')) return false;
    if (t.classList.contains('table-no-col-resize')) return false;
    if (t.classList.contains('org-perm-table')) return false;
    if (t.closest && t.closest('.table-no-col-resize-wrap')) return false;
    return true;
  }

  /** 결제관리 그리드: 내용 너비 우선 + 드래그는 min-width 로 유지 */
  function isPayMngDataGrid(table) {
    return !!(table && table.classList && table.classList.contains('pay-mng-data-grid'));
  }

  function countDataColumns(table) {
    var tr = table.querySelector('tbody tr');
    if (tr && tr.cells && tr.cells.length) {
      var c0 = tr.cells[0];
      if (!(c0 && c0.classList && c0.classList.contains('empty-state-cell'))) {
        return tr.cells.length;
      }
    }
    var top = table.querySelector('thead tr:first-child');
    if (!top) return 0;
    var sum = 0;
    for (var i = 0; i < top.cells.length; i++) {
      sum += parseInt(top.cells[i].colSpan, 10) || 1;
    }
    return sum;
  }

  function clearEnhancement(table) {
    table.classList.remove('pg-table-col-resize-enabled');
    table.querySelectorAll('.pg-col-resize-handle').forEach(function (h) {
      h.remove();
    });
    table.querySelectorAll('th.pg-th-has-resize').forEach(function (th) {
      th.classList.remove('pg-th-has-resize');
      th.style.zIndex = '';
    });
    var oldCg = table.querySelector('colgroup.pg-col-resize-group');
    if (oldCg) oldCg.remove();
  }

  function ensureColgroup(table, n) {
    var thead = table.querySelector('thead');
    if (!thead || n <= 0) return null;
    var cg = document.createElement('colgroup');
    cg.className = 'pg-col-resize-group';
    for (var i = 0; i < n; i++) {
      cg.appendChild(document.createElement('col'));
    }
    table.insertBefore(cg, thead);
    return cg;
  }

  function getCols(table) {
    var cg = table.querySelector('colgroup.pg-col-resize-group');
    if (!cg) return [];
    return Array.prototype.slice.call(cg.querySelectorAll('col'));
  }

  function applyStoredWidths(table, cols) {
    var id = table.id || '';
    if (!id || id.indexOf('grid_') !== 0) return;
    var payGrid = isPayMngDataGrid(table);
    try {
      var raw = global.localStorage.getItem(STORAGE_PREFIX + id);
      if (!raw) return;
      var arr = JSON.parse(raw);
      if (!Array.isArray(arr) || arr.length !== cols.length) return;
      for (var i = 0; i < arr.length; i++) {
        var w = Number(arr[i]);
        if (isNaN(w) || w < MIN_COL) continue;
        if (payGrid) {
          cols[i].style.width = '';
          cols[i].style.minWidth = w + 'px';
        } else {
          cols[i].style.minWidth = '';
          cols[i].style.width = w + 'px';
        }
      }
    } catch (e) { /* ignore */ }
  }

  /** localStorage 등으로 아직 너비가 없는 <col> 만 채움 (fixed 레이아웃에서 균등분할 → 셀 겹침 방지) */
  function colNeedsAutoSeed(col, payGrid) {
    var w = col.style && String(col.style.width || '').trim();
    var m = col.style && String(col.style.minWidth || '').trim();
    if (payGrid) {
      return !m || m === 'auto';
    }
    return !w || w === 'auto';
  }

  function seedMissingColWidthsFromDom(table, cols, n) {
    var payGrid = isPayMngDataGrid(table);
    var tr = table.querySelector('tbody tr');
    var useBody = !!(tr && tr.cells && tr.cells.length === n);
    if (useBody) {
      var c0 = tr.cells[0];
      if (c0 && c0.classList && c0.classList.contains('empty-state-cell')) {
        useBody = false;
      }
    }
    if (useBody) {
      for (var i = 0; i < n; i++) {
        if (!colNeedsAutoSeed(cols[i], payGrid)) {
          continue;
        }
        var cell = tr.cells[i];
        if (!cell) {
          continue;
        }
        var px = Math.max(MIN_COL, Math.round(cell.getBoundingClientRect().width));
        if (payGrid) {
          cols[i].style.width = '';
          cols[i].style.minWidth = px + 'px';
        } else {
          cols[i].style.minWidth = '';
          cols[i].style.width = px + 'px';
        }
      }
      return;
    }
    var thead = table.querySelector('thead');
    if (!thead || !thead.rows.length) {
      return;
    }
    var lastRow = thead.rows[thead.rows.length - 1];
    var idx = 0;
    for (var j = 0; j < lastRow.cells.length && idx < n; j++) {
      var th = lastRow.cells[j];
      var cs = parseInt(th.colSpan, 10) || 1;
      var rw = Math.max(MIN_COL * cs, Math.round(th.getBoundingClientRect().width));
      var per = Math.max(MIN_COL, Math.round(rw / cs));
      for (var k = 0; k < cs && idx < n; k++) {
        if (colNeedsAutoSeed(cols[idx], payGrid)) {
          if (payGrid) {
            cols[idx].style.width = '';
            cols[idx].style.minWidth = per + 'px';
          } else {
            cols[idx].style.minWidth = '';
            cols[idx].style.width = per + 'px';
          }
        }
        idx++;
      }
    }
    for (var u = 0; u < n; u++) {
      if (colNeedsAutoSeed(cols[u], payGrid)) {
        if (payGrid) {
          cols[u].style.minWidth = MIN_COL + 'px';
        } else {
          cols[u].style.width = MIN_COL + 'px';
        }
      }
    }
  }

  function saveWidths(table, cols) {
    var id = table.id || '';
    if (!id || id.indexOf('grid_') !== 0) return;
    try {
      var arr = cols.map(function (c) {
        return Math.round(c.getBoundingClientRect().width);
      });
      global.localStorage.setItem(STORAGE_PREFIX + id, JSON.stringify(arr));
    } catch (e) { /* ignore */ }
  }

  function bindDrag(table, cols, startIdx, span, e) {
    e.preventDefault();
    e.stopPropagation();
    var startX = e.pageX || (e.touches && e.touches[0] && e.touches[0].pageX) || 0;
    var slice = [];
    var k;
    for (k = 0; k < span; k++) {
      if (startIdx + k < cols.length) slice.push(cols[startIdx + k]);
    }
    if (!slice.length) return;
    var startWs = slice.map(function (c) {
      return c.getBoundingClientRect().width;
    });
    var totalStart = startWs.reduce(function (a, b) {
      return a + b;
    }, 0);
    /* table-layout:auto 등으로 <col> 박스가 0이면 tbody 셀 너비로 초기값 보정 */
    if (totalStart <= 0 || startWs.some(function (w) {
      return !w || w < 1;
    })) {
      var trBody = table.querySelector('tbody tr');
      if (trBody && trBody.cells && trBody.cells.length > startIdx) {
        for (var fi = 0; fi < slice.length; fi++) {
          var cell = trBody.cells[startIdx + fi];
          if (cell) {
            startWs[fi] = Math.max(MIN_COL, cell.getBoundingClientRect().width);
          }
        }
      }
      totalStart = startWs.reduce(function (a, b) {
        return a + b;
      }, 0);
    }
    if (totalStart <= 0) {
      for (var u = 0; u < slice.length; u++) {
        startWs[u] = MIN_COL;
      }
      totalStart = MIN_COL * slice.length;
    }

    function onMove(ev) {
      var x = ev.pageX || (ev.touches && ev.touches[0] && ev.touches[0].pageX) || 0;
      var dx = x - startX;
      var minTotal = MIN_COL * slice.length;
      var totalNew = Math.max(minTotal, totalStart + dx);
      if (totalStart <= 0) return;
      var payGrid = isPayMngDataGrid(table);
      for (var i = 0; i < slice.length; i++) {
        var ratio = startWs[i] / totalStart;
        var px = Math.max(MIN_COL, ratio * totalNew);
        if (payGrid) {
          slice[i].style.width = '';
          slice[i].style.minWidth = px + 'px';
        } else {
          slice[i].style.minWidth = '';
          slice[i].style.width = px + 'px';
        }
      }
    }
    function onUp() {
      document.removeEventListener('mousemove', onMove, true);
      document.removeEventListener('mouseup', onUp, true);
      document.removeEventListener('touchmove', onMove, true);
      document.removeEventListener('touchend', onUp, true);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
      saveWidths(table, cols);
    }
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
    document.addEventListener('mousemove', onMove, true);
    document.addEventListener('mouseup', onUp, true);
    document.addEventListener('touchmove', onMove, { passive: false, capture: true });
    document.addEventListener('touchend', onUp, true);
  }

  function attachHandle(table, th, cols, colStart, span) {
    if (!th || span < 1 || colStart < 0 || colStart >= cols.length) return;
    var handle = document.createElement('span');
    handle.className = 'pg-col-resize-handle';
    handle.setAttribute('role', 'separator');
    handle.setAttribute('aria-orientation', 'vertical');
    handle.title = '열 너비 조절(드래그)';
    th.classList.add('pg-th-has-resize');
    /* 오른쪽 경계 핸들이 다음 th(동일 행에서 뒤쪽 셀)에 가려져 드래그가 안 되는 경우 방지 — 왼쪽 열일수록 위로 쌓음 */
    var zi = 400 - colStart;
    if (zi < 2) zi = 2;
    th.style.zIndex = String(zi);
    th.appendChild(handle);
    handle.addEventListener('mousedown', function (e) {
      e.stopPropagation();
      e.preventDefault();
      bindDrag(table, cols, colStart, span, e);
    });
    handle.addEventListener('touchstart', function (e) {
      e.stopPropagation();
      bindDrag(table, cols, colStart, span, e);
    }, { passive: false });
  }

  /**
   * thead 격자 → 마지막 행에서 연속 같은 th 묶음마다 핸들 1개
   */
  function attachHandles(table, cols, n) {
    var thead = table.querySelector('thead');
    if (!thead || !thead.rows.length) return;
    var R = thead.rows.length;
    var lastRow = R - 1;

    var grid = [];
    for (var r = 0; r < R; r++) {
      grid[r] = [];
      for (var x = 0; x < n; x++) grid[r][x] = null;
    }

    for (var rr = 0; rr < R; rr++) {
      var cpos = 0;
      var rowCells = thead.rows[rr].cells;
      for (var ci = 0; ci < rowCells.length; ci++) {
        var th = rowCells[ci];
        while (cpos < n && grid[rr][cpos]) cpos++;
        if (cpos >= n) break;
        var rs = parseInt(th.rowSpan, 10) || 1;
        var cs = parseInt(th.colSpan, 10) || 1;
        for (var dr = 0; dr < rs; dr++) {
          for (var dc = 0; dc < cs; dc++) {
            if (rr + dr < R && cpos + dc < n) {
              grid[rr + dr][cpos + dc] = th;
            }
          }
        }
        cpos += cs;
      }
    }

    var seen = {};
    var j = 0;
    while (j < n) {
      var thb = grid[lastRow][j];
      if (!thb) {
        j++;
        continue;
      }
      var uid = thb._pgResizeUid || (thb._pgResizeUid = 'th_' + Math.random().toString(36).slice(2));
      if (seen[uid]) {
        j++;
        continue;
      }
      var span = 0;
      for (var k = j; k < n && grid[lastRow][k] === thb; k++) span++;
      attachHandle(table, thb, cols, j, span);
      seen[uid] = true;
      j += span;
    }
  }

  function refreshTable(table) {
    if (!eligibleTable(table)) return;
    var thead = table.querySelector('thead');
    if (!thead || !thead.rows.length) return;

    clearEnhancement(table);
    var n = countDataColumns(table);
    if (n <= 0) return;

    ensureColgroup(table, n);
    var cols = getCols(table);
    if (cols.length !== n) return;

    applyStoredWidths(table, cols);
    /* fixed 적용 전에 셀 너비를 읽어 <col>에 넣지 않으면 열이 과도하게 압축되어 nowrap 셀이 겹쳐 보임 */
    function applyFixedAndHandles() {
      applying = true;
      try {
        seedMissingColWidthsFromDom(table, cols, n);
        table.classList.add('pg-table-col-resize-enabled');
        attachHandles(table, cols, n);
      } finally {
        applying = false;
      }
    }
    if (typeof global.requestAnimationFrame === 'function') {
      global.requestAnimationFrame(applyFixedAndHandles);
    } else {
      applyFixedAndHandles();
    }
  }

  function refreshInCore(root) {
    if (!root || !root.querySelectorAll) return;
    applying = true;
    try {
      root.querySelectorAll('table.table').forEach(function (tbl) {
        refreshTable(tbl);
      });
    } finally {
      applying = false;
    }
  }

  function refreshInSync(root) {
    if (!root || !root.querySelectorAll) return;
    var prev = refreshDebounceTimers.get(root);
    if (prev) {
      clearTimeout(prev);
      refreshDebounceTimers.delete(root);
    }
    refreshInCore(root);
  }

  function refreshIn(root) {
    if (!root || !root.querySelectorAll) return;
    var prev = refreshDebounceTimers.get(root);
    if (prev) {
      clearTimeout(prev);
    }
    refreshDebounceTimers.set(root, global.setTimeout(function () {
      refreshDebounceTimers.delete(root);
      refreshInCore(root);
    }, REFRESH_DEBOUNCE_MS));
  }

  /**
   * 탭 pane 등: 그리드가 비동기로 채워질 때 subtree 변경 후 refreshIn(pane)을 다시 호출.
   * app.js loadContent 에서 ensureObserver(pane) + refreshIn(pane) 패턴과 맞춤.
   */
  function ensureObserver(pane) {
    if (!pane || !pane.querySelectorAll || pane._pgTcObs) {
      return;
    }
    var obs = new MutationObserver(function (records) {
      if (applying) {
        return;
      }
      if (mutationRecordsAllColResizeOnly(records)) {
        return;
      }
      refreshIn(pane);
    });
    pane._pgTcObs = obs;
    obs.observe(pane, { childList: true, subtree: true });
  }

  function debounce(fn, ms) {
    var t;
    return function () {
      clearTimeout(t);
      var args = arguments;
      t = setTimeout(function () {
        fn.apply(null, args);
      }, ms);
    };
  }

  var scheduleContentsRefresh = debounce(function () {
    if (applying) return;
    var cm = document.getElementById('contentsMain');
    if (!cm) return;
    var active = cm.querySelector('.tab-pane.tabConDiv.show.active') || cm.querySelector('.tab-pane.show.active');
    if (active && typeof active.querySelectorAll === 'function') {
      refreshIn(active);
    } else {
      refreshIn(cm);
    }
  }, 120);

  function initObservers() {
    var cm = document.getElementById('contentsMain');
    if (cm && typeof MutationObserver !== 'undefined') {
      var mo = new MutationObserver(function (records) {
        if (applying) {
          return;
        }
        if (mutationRecordsAllColResizeOnly(records)) {
          return;
        }
        scheduleContentsRefresh();
      });
      mo.observe(cm, { childList: true, subtree: true });
    }
    document.addEventListener('shown.bs.modal', function (ev) {
      var m = ev.target;
      if (m && m.querySelectorAll) refreshInSync(m);
    });
  }

  global.PG_TABLE_COL_RESIZE = {
    refresh: refreshTable,
    refreshIn: refreshIn,
    refreshInSync: refreshInSync,
    ensureObserver: ensureObserver,
    refreshAll: function () {
      var cm = document.getElementById('contentsMain');
      if (cm) refreshInSync(cm);
      document.querySelectorAll('.modal.show').forEach(function (m) {
        refreshInSync(m);
      });
    }
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () {
      initObservers();
      global.PG_TABLE_COL_RESIZE.refreshAll();
    });
  } else {
    initObservers();
    global.PG_TABLE_COL_RESIZE.refreshAll();
  }
})(typeof window !== 'undefined' ? window : this);
