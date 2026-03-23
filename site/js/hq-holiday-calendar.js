/**
 * 본사(REGIONAL) 영업일·휴일: 연도별 12개월 미니달력 + 공휴일 API 병합
 */
(function () {
  'use strict';

  var DAY_HDR = ['일', '월', '화', '수', '목', '금', '토'];

  function pad(n) {
    return n < 10 ? '0' + n : String(n);
  }

  function toYmd(y, m, d) {
    return y + '-' + pad(m) + '-' + pad(d);
  }

  function parseDates(text) {
    var map = {};
    String(text || '').split(/\r?\n/).forEach(function (line) {
      var m = line.trim().match(/^(\d{4}-\d{2}-\d{2})/);
      if (m) map[m[1]] = true;
    });
    return map;
  }

  function datesToText(map) {
    return Object.keys(map).sort().join('\n');
  }

  function daysInMonth(y, m) {
    return new Date(y, m, 0).getDate();
  }

  function renderMonth(y, m, selectedMap) {
    var dim = daysInMonth(y, m);
    var firstDow = new Date(y, m - 1, 1).getDay();
    var box = document.createElement('div');
    box.className = 'hq-holiday-month border rounded p-2 bg-white h-100';
    var title = document.createElement('div');
    title.className = 'fw-semibold small mb-1';
    title.textContent = y + '년 ' + m + '월';
    box.appendChild(title);
    var tbl = document.createElement('table');
    tbl.className = 'table table-sm table-bordered mb-0 hq-holiday-mini-table text-center';
    var thead = document.createElement('thead');
    var trh = document.createElement('tr');
    trh.className = 'small';
    DAY_HDR.forEach(function (h) {
      var th = document.createElement('th');
      th.textContent = h;
      trh.appendChild(th);
    });
    thead.appendChild(trh);
    tbl.appendChild(thead);
    var tbody = document.createElement('tbody');
    var totalCells = Math.ceil((firstDow + dim) / 7) * 7;
    var dayNum = 1;
    var tr = null;
    for (var i = 0; i < totalCells; i++) {
      if (i % 7 === 0) {
        tr = document.createElement('tr');
        tbody.appendChild(tr);
      }
      var td = document.createElement('td');
      if (i < firstDow || dayNum > dim) {
        td.innerHTML = '&nbsp;';
      } else {
        var ds = toYmd(y, m, dayNum);
        var dow = i % 7;
        var isWeekend = (dow === 0 || dow === 6);
        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'btn btn-sm hq-holiday-day p-0 w-100 border-0';
        btn.setAttribute('data-date', ds);
        if (isWeekend) btn.setAttribute('data-weekend', 'Y');
        btn.textContent = String(dayNum);
        if (isWeekend || selectedMap[ds]) {
          btn.classList.add('hq-holiday-day--off');
        }
        td.appendChild(btn);
        dayNum++;
      }
      tr.appendChild(td);
    }
    tbl.appendChild(tbody);
    box.appendChild(tbl);
    return box;
  }

  function renderYear(wrap, year, selectedMap) {
    var grid = wrap.querySelector('.hq-holiday-calendar-grid');
    if (!grid) return;
    grid.innerHTML = '';
    var row = document.createElement('div');
    row.className = 'row g-2';
    for (var mo = 1; mo <= 12; mo++) {
      var col = document.createElement('div');
      col.className = 'col-lg-6 col-xl-4';
      col.appendChild(renderMonth(year, mo, selectedMap));
      row.appendChild(col);
    }
    grid.appendChild(row);
  }

  function initWrap(wrap, pane) {
    var cardBody = wrap.closest('.card-body');
    if (!cardBody) return;
    var ta = cardBody.querySelector('[name="businessHolidayExtraDates"]');
    var countryEl = cardBody.querySelector('[name="holidayCountryCodes"]');
    var yearSel = wrap.querySelector('.hq-holiday-year');
    var grid = wrap.querySelector('.hq-holiday-calendar-grid');
    if (!ta || !yearSel || !grid) return;

    var y0 = new Date().getFullYear();
    yearSel.innerHTML = '';
    for (var i = y0 - 1; i <= y0 + 2; i++) {
      var o = document.createElement('option');
      o.value = String(i);
      o.textContent = i + '년';
      if (i === y0) o.selected = true;
      yearSel.appendChild(o);
    }

    function refresh() {
      var sel = parseDates(ta.value);
      var y = parseInt(yearSel.value, 10) || y0;
      renderYear(wrap, y, sel);
    }

    yearSel.addEventListener('change', refresh);
    wrap.querySelector('.hq-holiday-refresh').addEventListener('click', function () {
      refresh();
    });

    wrap.querySelector('.hq-holiday-load-presets').addEventListener('click', function () {
      if (!window.PG_API || !window.PG_API.holidayPresets) {
        alert('API가 준비되지 않았습니다.');
        return;
      }
      var cc = (countryEl && countryEl.value) ? String(countryEl.value).trim() : 'KR,US,JP,TH';
      var y = parseInt(yearSel.value, 10) || y0;
      window.PG_API.holidayPresets(y, cc).then(function (data) {
        var merged = parseDates(ta.value);
        var list = (data && data.dates) ? data.dates : [];
        list.forEach(function (d) {
          merged[d] = true;
        });
        ta.value = datesToText(merged);
        refresh();
      }).catch(function (e) {
        alert(e && e.message ? e.message : '공휴일 불러오기 실패');
      });
    });

    grid.addEventListener('click', function (ev) {
      var btn = ev.target && ev.target.closest ? ev.target.closest('.hq-holiday-day') : null;
      if (!btn) return;
      if (btn.getAttribute('data-weekend') === 'Y') return;
      var ds = btn.getAttribute('data-date');
      if (!ds) return;
      var set = parseDates(ta.value);
      if (set[ds]) delete set[ds];
      else set[ds] = true;
      ta.value = datesToText(set);
      refresh();
    });

    ta.addEventListener('input', refresh);
    refresh();
  }

  function init(pane, opts) {
    if (!pane || !pane.querySelector) return;
    var force = opts && opts.force;
    pane.querySelectorAll('.hq-holiday-ui-wrap').forEach(function (wrap) {
      if (force) delete wrap._hqHolidayInit;
      if (wrap._hqHolidayInit) return;
      wrap._hqHolidayInit = true;
      initWrap(wrap, pane);
    });
  }

  window.PG_HQ_HOLIDAY = { init: init };
})();
