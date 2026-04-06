/**
 * PG 통합관리자 — 모든 화면의 Bootstrap 테이블(.table) 헤더에서 컬럼 너비 조절.
 * thead 마지막 행의 각 th 오른쪽 가장자리(그립)를 드래그합니다.
 * 신규 화면·그리드도 class="table" 유지 시 자동 적용(tab-pane 내 MutationObserver).
 * 제외: 해당 테이블에 class="pg-col-resize-skip" 부여.
 */
(function () {
  'use strict';

  var MIN_W = 40;

  function leadingMultirowHeaderCols(table) {
    var thead = table.tHead;
    if (!thead || thead.rows.length < 2) {
      return 0;
    }
    var firstRow = thead.rows[0];
    var totalHeaderRows = thead.rows.length;
    var n = 0;
    for (var i = 0; i < firstRow.cells.length; i++) {
      var cell = firstRow.cells[i];
      var rs = cell.rowSpan || 1;
      if (rs >= totalHeaderRows) {
        n += (cell.colSpan || 1);
      }
    }
    return n;
  }

  function columnIndexForThInLastHeaderRow(table, th) {
    var thead = table.tHead;
    if (!thead || thead.rows.length === 0) {
      return -1;
    }
    var lastTr = thead.rows[thead.rows.length - 1];
    var lead = leadingMultirowHeaderCols(table);
    var idx = lead;
    for (var i = 0; i < lastTr.cells.length; i++) {
      var cell = lastTr.cells[i];
      if (cell === th) {
        return idx;
      }
      idx += (cell.colSpan || 1);
    }
    return -1;
  }

  function clearGrips(table) {
    table.querySelectorAll('.pg-col-resize-grip').forEach(function (g) {
      try {
        g.remove();
      } catch (e) { /* ignore */ }
    });
    table.classList.remove('pg-table-col-resize-ready');
  }

  function applyColumnWidth(table, colIndex, thRef, px) {
    var w = Math.max(MIN_W, Math.round(px)) + 'px';
    if (colIndex >= 0) {
      for (var b = 0; b < table.tBodies.length; b++) {
        var rows = table.tBodies[b].rows;
        for (var r = 0; r < rows.length; r++) {
          var cell = rows[r].cells[colIndex];
          if (cell) {
            cell.style.width = w;
            cell.style.minWidth = w;
            cell.style.maxWidth = w;
          }
        }
      }
    }
    thRef.style.width = w;
    thRef.style.minWidth = w;
    thRef.style.maxWidth = w;
  }

  function bindTable(table) {
    if (!table || table.tagName !== 'TABLE') {
      return;
    }
    if (!table.classList.contains('table')) {
      return;
    }
    if (table.classList.contains('pg-col-resize-skip')) {
      return;
    }
    if (!table.tHead || table.tHead.rows.length === 0) {
      return;
    }

    clearGrips(table);
    table.classList.add('pg-cols-resizable');

    var lastTr = table.tHead.rows[table.tHead.rows.length - 1];
    for (var i = 0; i < lastTr.cells.length; i++) {
      var th = lastTr.cells[i];
      if (th.tagName !== 'TH') {
        continue;
      }
      if ((th.colSpan || 1) > 1) {
        continue;
      }

      var colIdx = columnIndexForThInLastHeaderRow(table, th);
      if (colIdx < 0) {
        continue;
      }

      th.style.position = 'relative';
      var grip = document.createElement('span');
      grip.className = 'pg-col-resize-grip';
      grip.setAttribute('aria-hidden', 'true');
      grip.title = '컬럼 너비 조절';

      (function (thRef, cidx) {
        var startX;
        var startW;
        function onMove(ev) {
          var dx = ev.clientX - startX;
          var nw = startW + dx;
          applyColumnWidth(table, cidx, thRef, nw);
        }
        function onUp() {
          document.removeEventListener('mousemove', onMove);
          document.removeEventListener('mouseup', onUp);
          document.body.style.cursor = '';
          document.body.style.userSelect = '';
        }
        grip.addEventListener('mousedown', function (ev) {
          ev.preventDefault();
          ev.stopPropagation();
          startX = ev.clientX;
          startW = thRef.getBoundingClientRect().width;
          document.body.style.cursor = 'col-resize';
          document.body.style.userSelect = 'none';
          document.addEventListener('mousemove', onMove);
          document.addEventListener('mouseup', onUp);
        });
      })(th, colIdx);

      th.appendChild(grip);
    }
    table.classList.add('pg-table-col-resize-ready');
  }

  function refreshIn(root) {
    if (!root || !root.querySelectorAll) {
      return;
    }
    var obs = root._pgTcObs;
    if (obs) {
      try {
        obs.disconnect();
      } catch (e0) { /* ignore */ }
    }
    root.querySelectorAll('table.table').forEach(bindTable);
    if (obs) {
      try {
        obs.observe(root, { childList: true, subtree: true });
      } catch (e1) { /* ignore */ }
    }
  }

  function ensureObserver(pane) {
    if (!pane || pane._pgTcObs) {
      return;
    }
    var t = null;
    var obs = new MutationObserver(function () {
      clearTimeout(t);
      t = setTimeout(function () {
        refreshIn(pane);
      }, 220);
    });
    pane._pgTcObs = obs;
    obs.observe(pane, { childList: true, subtree: true });
  }

  window.PG_TABLE_COL_RESIZE = {
    refreshIn: refreshIn,
    ensureObserver: ensureObserver,
    bindTable: bindTable
  };

  if (typeof document !== 'undefined') {
    document.addEventListener('DOMContentLoaded', function () {
      if (window.PG_TABLE_COL_RESIZE && typeof window.PG_TABLE_COL_RESIZE.refreshIn === 'function') {
        window.PG_TABLE_COL_RESIZE.refreshIn(document.body);
      }
    });
    document.addEventListener('shown.bs.modal', function (ev) {
      var m = ev.target;
      if (!m || !window.PG_TABLE_COL_RESIZE) {
        return;
      }
      if (typeof window.PG_TABLE_COL_RESIZE.ensureObserver === 'function') {
        window.PG_TABLE_COL_RESIZE.ensureObserver(m);
      }
      if (typeof window.PG_TABLE_COL_RESIZE.refreshIn === 'function') {
        window.PG_TABLE_COL_RESIZE.refreshIn(m);
      }
    });
  }
})();
