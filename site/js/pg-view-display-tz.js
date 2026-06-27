/**
 * 목록 화면 일시적 표시 시간대 — 전산설정과 무관, 현재 조회 결과의 거래일·시간 2줄 중 표준(2줄)만 변경.
 */
(function (w) {
  'use strict';

  var STORAGE_KEY = 'pg_view_display_tz';

  var TXN_DATETIME_COL_KEYS = {
    trnDate: 1, trnTime: 1, payCompletedAt: 1, payDttm: 1, payAprv: 1,
    transactionDate: 1, paymentDate: 1, sendDt: 1, createdAt: 1, payDt: 1,
    withdrawDt: 1, cancDt: 1, holdDttm: 1, settlementCloseDate: 1,
    settlementExecDate: 1, approveDt: 1, cancelDt: 1, calcDt: 1,
    contractDate: 1, updatedAt: 1, reservationStart: 1, reservationEnd: 1
  };

  var TZ_OPTIONS = [
    { v: 'Asia/Bangkok', tag: 'TH' },
    { v: 'Asia/Seoul', tag: 'KR' },
    { v: 'Asia/Tokyo', tag: 'JP' },
    { v: 'Asia/Shanghai', tag: 'CH' },
    { v: 'Asia/Ho_Chi_Minh', tag: 'VT' },
    { v: 'Asia/Singapore', tag: 'SG' },
    { v: 'Asia/Manila', tag: 'PP' },
    { v: 'Asia/Jakarta', tag: 'IN' },
    { v: 'Asia/Dubai', tag: 'UA' },
    { v: 'UTC', tag: 'UTC' },
    { v: 'Europe/London', tag: 'EU' },
    { v: 'America/New_York', tag: 'NY' },
    { v: 'America/Los_Angeles', tag: 'LA' }
  ];

  var I18N = {
    defaultOpt: {
      KO: '본사설정', EN: 'HQ settings', JP: '本社設定', CH: '总部设置', TH: 'ส่วนตั้งค่า HQ'
    },
    title: {
      KO: '현재 목록의 거래일·거래시간 표시만 일시 변경합니다. 본사 전산설정·DB는 바뀌지 않습니다.',
      EN: 'Temporarily changes transaction date/time display for this list only. HQ settings and DB are unchanged.',
      JP: 'この一覧の取引日・時刻表示のみ一時変更します。本社設定・DBは変わりません。',
      CH: '仅临时更改本列表的交易日期/时间显示。不改变总部设置或数据库。',
      TH: 'เปลี่ยนการแสดงวันที่/เวลาธุรกรรมของรายการนี้ชั่วคราวเท่านั้น ไม่แก้การตั้งค่าสำนักงานใหญ่หรือ DB'
    }
  };

  function uiT(ko, i18nKey) {
    if (w.PG_UI && typeof w.PG_UI.t === 'function') return w.PG_UI.t(ko);
    if (i18nKey) return t(i18nKey);
    return ko;
  }

  function getLoc() {
    if (w.PG_PAY_LIST_I18N && typeof w.PG_PAY_LIST_I18N.getLocale === 'function') {
      return w.PG_PAY_LIST_I18N.getLocale();
    }
    return 'KO';
  }

  function t(key) {
    var loc = getLoc();
    var row = I18N[key];
    if (!row) return key;
    return row[loc] || row.EN || row.KO || key;
  }

  function esc(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  function screenHasTxnDatetimeColumns(cfg) {
    if (!cfg || cfg.hideListGrid) return false;
    if (cfg.viewDisplayTimezone === false) return false;
    if (cfg.viewDisplayTimezone === true) return true;
    if (cfg.payListVariant || cfg.tableColumnGuide) return true;
    if (cfg.isDailySummaryScreen) return true;
    var cols = cfg.columns;
    if (cols && cols.length) {
      for (var i = 0; i < cols.length; i++) {
        var k = cols[i] && cols[i].key;
        if (k && TXN_DATETIME_COL_KEYS[k]) return true;
      }
    }
    return false;
  }

  function readStoredTz() {
    try {
      return localStorage.getItem(STORAGE_KEY) || '';
    } catch (e) {
      return '';
    }
  }

  function writeStoredTz(v) {
    try {
      if (!v) localStorage.removeItem(STORAGE_KEY);
      else localStorage.setItem(STORAGE_KEY, v);
    } catch (e) { /* ignore */ }
  }

  function buildToolbarHtml(tabId) {
    var tid = tabId || '';
    var selId = 'pgViewDisplayTz_' + tid;
    var stored = readStoredTz();
    var h = '<div class="pg-view-display-tz-wrap d-inline-flex align-items-center gap-1 me-1" title="' + esc(uiT('목록 표시 시간대 안내', 'title')) + '">';
    h += '<select class="form-select form-select-sm pg-view-display-tz-select" id="' + esc(selId) + '" name="viewDisplayTimezone" style="width:auto;min-width:5.5rem;max-width:9rem" aria-label="' + esc(uiT('본사설정', 'defaultOpt')) + '">';
    h += '<option value="" data-pg-ui-t="본사설정">' + esc(uiT('본사설정', 'defaultOpt')) + '</option>';
    TZ_OPTIONS.forEach(function (o) {
      var sel = stored === o.v ? ' selected' : '';
      h += '<option value="' + esc(o.v) + '"' + sel + '>' + esc(o.tag) + '</option>';
    });
    h += '</select></div>';
    return h;
  }

  function collectParam(pane) {
    if (!pane) return '';
    var el = pane.querySelector('.pg-view-display-tz-select');
    return el && el.value ? String(el.value).trim() : '';
  }

  function bindPane(pane, tabId, onChange) {
    if (!pane) return;
    var sel = pane.querySelector('.pg-view-display-tz-select');
    if (!sel || sel._pgViewTzBound) return;
    sel._pgViewTzBound = true;
    var stored = readStoredTz();
    if (stored && !sel.value) sel.value = stored;
    sel.addEventListener('change', function () {
      writeStoredTz(sel.value || '');
      if (typeof onChange === 'function') onChange();
    });
  }

  function refreshI18n(pane) {
    if (!pane) return;
    var sel = pane.querySelector('.pg-view-display-tz-select');
    if (!sel) return;
    var cur = sel.value;
    var def = sel.querySelector('option[value=""]');
    if (def) def.textContent = uiT('본사설정', 'defaultOpt');
    sel.value = cur;
    sel.setAttribute('aria-label', uiT('본사설정', 'defaultOpt'));
    var wrap = pane.querySelector('.pg-view-display-tz-wrap');
    if (wrap) wrap.setAttribute('title', uiT('목록 표시 시간대 안내', 'title'));
  }

  w.PG_VIEW_DISPLAY_TZ = {
    screenHasTxnDatetimeColumns: screenHasTxnDatetimeColumns,
    buildToolbarHtml: buildToolbarHtml,
    collectParam: collectParam,
    bindPane: bindPane,
    refreshI18n: refreshI18n,
    readStoredTz: readStoredTz
  };
})(window);
