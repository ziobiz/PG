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

  function eligibleTable(t) {
    if (!t || t.tagName !== 'TABLE') return false;
    if (!t.classList || !t.classList.contains('table')) return false;
    if (t.classList.contains('table-no-col-resize')) return false;
    if (t.closest && t.closest('.table-no-col-resize-wrap')) return false;
    return true;
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
    try {
      var raw = global.localStorage.getItem(STORAGE_PREFIX + id);
      if (!raw) return;
      var arr = JSON.parse(raw);
      if (!Array.isArray(arr) || arr.length !== cols.length) return;
      for (var i = 0; i < arr.length; i++) {
        var w = Number(arr[i]);
        if (!isNaN(w) && w >= MIN_COL) cols[i].style.width = w + 'px';
      }
    } catch (e) { /* ignore */ }
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

    function onMove(ev) {
      var x = ev.pageX || (ev.touches && ev.touches[0] && ev.touches[0].pageX) || 0;
      var dx = x - startX;
      var minTotal = MIN_COL * slice.length;
      var totalNew = Math.max(minTotal, totalStart + dx);
      if (totalStart <= 0) return;
      for (var i = 0; i < slice.length; i++) {
        var ratio = startWs[i] / totalStart;
        slice[i].style.width = Math.max(MIN_COL, ratio * totalNew) + 'px';
      }
    }
    function onUp() {
      document.removeEventListener('mousemove', onMove);
      document.removeEventListener('mouseup', onUp);
      document.removeEventListener('touchmove', onMove);
      document.removeEventListener('touchend', onUp);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
      saveWidths(table, cols);
    }
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
    document.addEventListener('mousemove', onMove);
    document.addEventListener('mouseup', onUp);
    document.addEventListener('touchmove', onMove, { passive: false });
    document.addEventListener('touchend', onUp);
  }

  function attachHandle(table, th, cols, colStart, span) {
    if (!th || span < 1 || colStart < 0 || colStart >= cols.length) return;
    var handle = document.createElement('span');
    handle.className = 'pg-col-resize-handle';
    handle.setAttribute('role', 'separator');
    handle.setAttribute('aria-orientation', 'vertical');
    handle.title = '열 너비 조절(드래그)';
    th.classList.add('pg-th-has-resize');
    th.appendChild(handle);
    handle.addEventListener('mousedown', function (e) {
      bindDrag(table, cols, colStart, span, e);
    });
    handle.addEventListener('touchstart', function (e) {
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

    table.classList.add('pg-table-col-resize-enabled');
    applyStoredWidths(table, cols);
    attachHandles(table, cols, n);
  }

  function refreshIn(root) {
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
    if (cm) refreshIn(cm);
  }, 120);

  function initObservers() {
    var cm = document.getElementById('contentsMain');
    if (cm && typeof MutationObserver !== 'undefined') {
      var mo = new MutationObserver(function () {
        if (applying) return;
        scheduleContentsRefresh();
      });
      mo.observe(cm, { childList: true, subtree: true });
    }
    document.addEventListener('shown.bs.modal', function (ev) {
      var m = ev.target;
      if (m && m.querySelectorAll) refreshIn(m);
    });
  }

  global.PG_TABLE_COL_RESIZE = {
    refresh: refreshTable,
    refreshIn: refreshIn,
    refreshAll: function () {
      var cm = document.getElementById('contentsMain');
      if (cm) refreshIn(cm);
      document.querySelectorAll('.modal.show').forEach(function (m) {
        refreshIn(m);
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
