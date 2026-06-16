/**
 * 본사(REGIONAL) 영업일·휴일: 연도별 12개월 미니달력 + 공휴일 API 병합
 * 영업일설정(/hq/businessDaySetting): 기준국가(KR/US/JP/TH/CN/GLOBAL)별 프리셋, data-hq-calendar-readonly 시 클릭 토글 비활성
 */
(function () {
  'use strict';

  function uiT(s) {
    if (s == null || s === '') return '';
    if (window.PG_UI_I18N && typeof window.PG_UI_I18N.t === 'function') return String(window.PG_UI_I18N.t(String(s)));
    return String(s);
  }
  var DAY_HDR_KEYS = ['달력요일_일', '달력요일_월', '달력요일_화', '달력요일_수', '달력요일_목', '달력요일_금', '달력요일_토'];
  /** API/저장 구분값(한국어 키) — 표시만 uiT() */
  var HQ_BIZDAY_KIND_KEYS = ['공휴일', '국경일', '기념일', '종교휴일', '임시공휴일', '대체공휴일'];

  function translateHolidayKind(kind) {
    var k = (kind != null) ? String(kind).trim() : '';
    if (!k) k = '공휴일';
    return uiT(k);
  }

  function refreshKindSelect(sel) {
    if (!sel) return;
    var cur = sel.value;
    sel.innerHTML = HQ_BIZDAY_KIND_KEYS.map(function (k) {
      return '<option value="' + k + '" data-pg-ui-t="' + k + '">' + uiT(k) + '</option>';
    }).join('');
    if (cur && HQ_BIZDAY_KIND_KEYS.indexOf(cur) >= 0) sel.value = cur;
  }

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

  function expandYmdRange(fromStr, toStr) {
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
    while (d <= end) {
      out.push(d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()));
      d.setDate(d.getDate() + 1);
    }
    return out;
  }

  /** 날짜 -> { kind, note } (구간 JSON 기준) */
  function buildKindMetaFromEntriesJson(text) {
    var byDate = {};
    try {
      var arr = JSON.parse(text || '[]');
      if (!Array.isArray(arr)) return byDate;
      arr.forEach(function (e) {
        var from = (e.fromDate != null) ? String(e.fromDate).trim() : '';
        var to = (e.toDate != null) ? String(e.toDate).trim() : '';
        var kind = (e.holidayKind != null) ? String(e.holidayKind).trim() : '공휴일'; /* 저장/API 값(한국어 키) 유지 */
        var note = (e.note != null) ? String(e.note) : '';
        expandYmdRange(from, to || from).forEach(function (day) {
          byDate[day] = { kind: kind, note: note };
        });
      });
    } catch (err) { /* ignore */ }
    return byDate;
  }

  function daysInMonth(y, m) {
    return new Date(y, m, 0).getDate();
  }

  function renderMonth(y, m, selectedMap, kindMeta) {
    var dim = daysInMonth(y, m);
    var firstDow = new Date(y, m - 1, 1).getDay();
    var box = document.createElement('div');
    box.className = 'hq-holiday-month border rounded p-2 bg-white h-100';
    var title = document.createElement('div');
    title.className = 'fw-semibold small mb-1';
    var monthTpl = uiT('{Y}년 {M}월');
    title.textContent = monthTpl.replace(/\{Y\}/g, String(y)).replace(/\{M\}/g, String(m)).replace(/\{Mm\}/g, pad(m));
    box.appendChild(title);
    var tbl = document.createElement('table');
    tbl.className = 'table table-sm table-bordered mb-0 hq-holiday-mini-table text-center';
    var thead = document.createElement('thead');
    var trh = document.createElement('tr');
    trh.className = 'small';
    DAY_HDR_KEYS.forEach(function (k) {
      var th = document.createElement('th');
      th.textContent = uiT(k);
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
        var meta = kindMeta && kindMeta[ds];
        if (isWeekend || selectedMap[ds]) {
          btn.classList.add('hq-holiday-day--off');
        }
        if (meta && meta.kind) {
          btn.setAttribute('data-holiday-kind', meta.kind);
          btn.classList.add('hq-holiday-day--kind');
          btn.setAttribute('title', translateHolidayKind(meta.kind) + (meta.note ? ' — ' + meta.note : ''));
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

  function renderYear(wrap, year, selectedMap, kindMeta) {
    var grid = wrap.querySelector('.hq-holiday-calendar-grid');
    if (!grid) return;
    grid.innerHTML = '';
    var row = document.createElement('div');
    row.className = 'row g-2';
    for (var mo = 1; mo <= 12; mo++) {
      var col = document.createElement('div');
      col.className = 'col-lg-6 col-xl-4';
      col.appendChild(renderMonth(year, mo, selectedMap, kindMeta || {}));
      row.appendChild(col);
    }
    grid.appendChild(row);
  }

  function initWrap(wrap, pane) {
    var cardBody = wrap.closest('.card-body');
    if (!cardBody) return false;
    var ta = cardBody.querySelector('[name="businessHolidayExtraDates"]');
    var metaInput = cardBody.querySelector('[name="holidayManualEntriesJson"]');
    var countryEl = cardBody.querySelector('[name="holidayCountryCodes"]');
    var yearSel = wrap.querySelector('.hq-holiday-year');
    var grid = wrap.querySelector('.hq-holiday-calendar-grid');
    var readOnly = wrap.getAttribute('data-hq-calendar-readonly') === 'true';
    if (!ta || !yearSel || !grid) return false;

    var y0 = new Date().getFullYear();
    yearSel.innerHTML = '';
    for (var i = y0 - 1; i <= y0 + 2; i++) {
      var o = document.createElement('option');
      o.value = String(i);
      var yTpl = uiT('{Y}년');
      o.textContent = yTpl.replace(/\{Y\}/g, String(i));
      if (i === y0) o.selected = true;
      yearSel.appendChild(o);
    }

    function refresh() {
      var sel = parseDates(ta.value);
      var kindMeta = metaInput ? buildKindMetaFromEntriesJson(metaInput.value) : {};
      var y = parseInt(yearSel.value, 10) || y0;
      renderYear(wrap, y, sel, kindMeta);
    }

    yearSel.addEventListener('change', refresh);
    var btnRef = wrap.querySelector('.hq-holiday-refresh');
    if (btnRef) btnRef.addEventListener('click', function () {
      refresh();
    });

    var btnPreset = wrap.querySelector('.hq-holiday-load-presets');
    if (btnPreset) btnPreset.addEventListener('click', function () {
      if (!window.PG_API || !window.PG_API.holidayPresets) {
        alert(uiT('API가 준비되지 않았습니다.'));
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
        alert(e && e.message ? uiT(e.message) : uiT('공휴일 불러오기 실패'));
      });
    });

    if (!readOnly) {
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
    }

    ta.addEventListener('input', refresh);
    if (metaInput) metaInput.addEventListener('input', refresh);
    refresh();
    return true;
  }

  function init(pane, opts) {
    if (!pane || !pane.querySelector) return;
    var force = opts && opts.force;
    pane.querySelectorAll('.hq-holiday-ui-wrap').forEach(function (wrap) {
      if (force) delete wrap._hqHolidayInit;
      if (wrap._hqHolidayInit) return;
      var ok = initWrap(wrap, pane);
      if (ok) wrap._hqHolidayInit = true;
    });
  }

  window.PG_HQ_HOLIDAY = {
    init: init,
    renderMonth: renderMonth,
    uiT: uiT,
    translateHolidayKind: translateHolidayKind,
    refreshKindSelect: refreshKindSelect,
    kindKeys: HQ_BIZDAY_KIND_KEYS
  };
})();
