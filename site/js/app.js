/**
 * PG 솔루션 - fxhj와 동일한 메뉴 구성, 화면은 하나씩 구현
 * fnTopMenuMove(url): 메뉴 클릭 시 탭 추가/전환, 우리 페이지 또는 placeholder 표시
 */

(function () {
  'use strict';

  window.SITE_CONFIG = {
    contentBaseUrl: '',
    contentMode: 'iframe',
    paymentBaseUrl: ''  // 간편결제 URL 베이스 (예: https://api.example.com) - 비어있으면 현재 origin 사용
  };

  // 메뉴별 URL → 라벨, parent (브레드크럼/탭 제목용) - FXHJ + 본사설정 + 리스크 통합
  var MENU_INFO = {
    '/hq/pgApiMng': { label: 'PG사 API 연동', parent: '본사설정' },
    '/hq/defaultCommission': { label: '기본정책', parent: '본사설정' },
    '/hq/apiConfig': { label: 'API 구성 세팅', parent: '본사설정' },
    '/hq/permissionMng': { label: '본사별 권한 세팅', parent: '본사설정' },
    '/system/noticeList': { label: '공지사항', parent: '업체관리' },
    '/comp/myCompMng': { label: '업체정보조회', parent: '업체관리' },
    '/comp/compMngTree': { label: '업체관리', parent: '업체관리' },
    '/comp/compDetail': { label: '업체정보', parent: '업체관리' },
    '/commission/commisionList': { label: '수수료관리', parent: '업체관리' },
    '/comp/compInfoHistList': { label: '업체변경이력', parent: '업체관리' },
    '/comp/compReg': { label: '업체등록', parent: '업체관리' },
    '/calc/payList': { label: '결제내역', parent: '결제관리' },
    '/calc/payListNew': { label: '결제내역(신)', parent: '결제관리' },
    '/calc/payFailList': { label: '결제실패내역', parent: '결제관리' },
    '/calc/offsetCancList': { label: '상계취소내역', parent: '결제관리' },
    '/pay/easyPay': { label: 'URL간편결제 내역', parent: '결제관리' },
    '/calc/cashReceiptList': { label: '현금영수증내역', parent: '결제관리' },
    '/calc/calcList': { label: '유통망정산내역', parent: '정산관리' },
    '/calc/calcGmList': { label: '가맹정산내역', parent: '정산관리' },
    '/calc/compPointMngList': { label: '환수금관리', parent: '정산관리' },
    '/calc/balcInfo': { label: '잔액/미수금관리', parent: '정산관리' },
    '/calc/exCalcList': { label: '정산실행', parent: '정산관리' },
    '/pay/payHoldList': { label: '정산보류내역', parent: '정산관리' },
    '/noti/notiUrlMng': { label: '결제통보 URL관리', parent: '통보관리' },
    '/noti/notiSendMngList': { label: '결제통보 전송관리', parent: '통보관리' },
    '/noti/notiCashReceiptUrlMng': { label: '현금영수증통보 URL관리', parent: '통보관리' },
    '/noti/notiCashReceiptSendMngList': { label: '현금영수증통보 전송관리', parent: '통보관리' },
    '/user/userMng': { label: '사용자관리', parent: '사용자관리' },
    '/set/gridSetMng': { label: '메뉴별항목순서관리', parent: '사용자관리' },
    '/risk/list': { label: '리스크 현황', parent: '리스크관리' }
  };

  var config = window.SITE_CONFIG;
  var TAB_UL = 'copyTopTabUl';
  var TAB_MAIN = 'topTapMain';
  var CONTENT_FRAME = 'content-frame';
  var PLACEHOLDER_ID = 'content-placeholder';
  var TABLE_ROW_PADDING_KEY = 'pg_table_row_padding_y';

  function getTableRowPaddingY() {
    var v = parseInt(localStorage.getItem(TABLE_ROW_PADDING_KEY), 10);
    return (isNaN(v) || v < 4 || v > 40) ? 10 : v;
  }
  function setTableRowPaddingY(px) {
    var v = Math.max(4, Math.min(40, px));
    document.documentElement.style.setProperty('--table-row-padding-y', v + 'px');
    localStorage.setItem(TABLE_ROW_PADDING_KEY, String(v));
  }
  function injectTableRowResizeHandles(tbody, colCount) {
    if (!tbody || tbody.querySelector('.table-row-resize-handle')) return;
    var rows = Array.from(tbody.querySelectorAll('tr')).filter(function (tr) {
      return !tr.classList.contains('table-row-resize-handle') && !tr.querySelector('.empty-state-cell');
    });
    if (rows.length < 2) return;
    var count = colCount || (rows[0] && rows[0].querySelectorAll('td').length) || 10;
    for (var i = rows.length - 1; i >= 1; i--) {
      var handle = document.createElement('tr');
      handle.className = 'table-row-resize-handle';
      handle.innerHTML = '<td colspan="' + count + '"></td>';
      rows[i].parentNode.insertBefore(handle, rows[i]);
    }
  }

  function getTabIdFromUrl(url) {
    if (!url || url === '/main') return 'main';
    var path = (url || '').replace(/^\//, '').replace(/\//g, '_');
    return path || 'main';
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

  function initPgBindingList(pane, initialBindings) {
    var tbody = pane.querySelector('#pgBindingTbody');
    var addBtn = pane.querySelector('#pgBindingAddBtn');
    if (!tbody || !addBtn || addBtn._pgBindingInit) return;
    addBtn._pgBindingInit = true;
    var pgAgencyOpts = '<option value="">선택</option>';
    var payMethodOpts = '<option value="">선택</option><option value="WEB">WEB</option><option value="OFFLINE">오프라인</option><option value="APM">APM</option>';
    var activationOpts = '<option value="Y">사용</option><option value="N">미사용</option>';
    var installmentOpts = '<option value="N">미사용</option><option value="Y">사용</option>';

    function addRow(idx, data) {
      data = data || {};
      var tr = document.createElement('tr');
      tr.dataset.idx = idx;
      tr.innerHTML = '<td><input type="radio" name="pgOperational" value="' + idx + '"' + (data.operationalYn === 'Y' ? ' checked' : '') + ' title="운영대상"></td>' +
        '<td><select class="form-control form-control-sm" data-field="activationYn">' + activationOpts + '</select></td>' +
        '<td><select class="form-control form-control-sm" data-field="pgCd">' + pgAgencyOpts + '</select></td>' +
        '<td><select class="form-control form-control-sm" data-field="payMethod">' + payMethodOpts + '</select></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="mid" placeholder="MID" value="' + (data.mid || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="apiKey" placeholder="API KEY" value="' + (data.apiKey || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="ivKey" placeholder="IV KEY" value="' + (data.ivKey || '') + '"></td>' +
        '<td><select class="form-control form-control-sm" data-field="installmentYn">' + installmentOpts + '</select></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="maxInstallmentMonths" placeholder="12" value="' + (data.maxInstallmentMonths || '') + '"></td>' +
        '<td><button type="button" class="btn btn-outline-danger btn-sm pg-binding-del">삭제</button></td>';
      tr.querySelector('[data-field="activationYn"]').value = data.activationYn || 'Y';
      tr.querySelector('[data-field="pgCd"]').value = data.pgCd || '';
      tr.querySelector('[data-field="payMethod"]').value = data.payMethod || 'WEB';
      tr.querySelector('[data-field="installmentYn"]').value = data.installmentYn || 'N';
      tr.querySelector('.pg-binding-del').addEventListener('click', function () { tr.remove(); reindexRows(); });
      tbody.appendChild(tr);
    }

    function reindexRows() {
      tbody.querySelectorAll('tr').forEach(function (t, i) { t.dataset.idx = i; var r = t.querySelector('[name="pgOperational"]'); if (r) r.value = i; });
    }

    window.PG_API.pgAgencyList().then(function (list) {
      (list || []).forEach(function (p) {
        pgAgencyOpts += '<option value="' + (p.pgCd || '') + '">' + (p.pgNm || p.pgCd) + '</option>';
      });
      var bindings = initialBindings || [];
      if (bindings.length > 0) {
        bindings.forEach(function (b, i) {
          addRow(i, { pgCd: b.pgCd, activationYn: b.activationYn || 'Y', operationalYn: b.operationalYn || (i === 0 ? 'Y' : 'N'), payMethod: b.payMethod || 'WEB', mid: b.mid, apiKey: b.apiKey, ivKey: b.ivKey, installmentYn: b.installmentYn || 'N', maxInstallmentMonths: b.maxInstallmentMonths != null ? String(b.maxInstallmentMonths) : '' });
        });
        tbody.querySelectorAll('[data-field="pgCd"]').forEach(function (sel) {
          var v = sel.value;
          sel.innerHTML = pgAgencyOpts;
          sel.value = v;
        });
      }
    }).catch(function () {});

    addBtn.addEventListener('click', function () {
      var idx = tbody.querySelectorAll('tr').length;
      addRow(idx, idx === 0 ? { operationalYn: 'Y' } : {});
    });
  }

  function addTabAndSwitch(url, menuId, label) {
    var tabId = getTabIdFromUrl(url);
    var ul = document.getElementById(TAB_UL);
    if (!ul) return;

    var existing = ul.querySelector('[top_tab_url="' + url + '"]');
    if (existing) {
      existing.querySelector('.tab-a').click();
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

  function bindScreenEvents(pane, tabId) {
    if (!pane) return;
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
        var fromEl = pane.querySelector('#searchFromDate');
        var toEl = pane.querySelector('#searchToDate');
        if (fromEl) fromEl.value = fmt(from);
        if (toEl) toEl.value = fmt(to);
      });
    });
    function collectSearchParams(p) {
      var params = { page: 1, size: 20 };
      var sizeEl = p.querySelector('#recordsPerPage');
      if (sizeEl) params.size = Math.max(1, parseInt(sizeEl.value, 10) || 20);
      p.querySelectorAll('input, select').forEach(function (el) {
        var name = el.name || el.id;
        if (!name) return;
        if (el.type === 'checkbox') {
          if (!el.classList.contains('grid-check-all') && name.indexOf('search') === 0) params[name] = el.checked ? 'true' : 'false';
          return;
        }
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
      var visible = {};
      for (var i = 0; i < list.length; i++) {
        var row = list[i];
        var id = row.id != null ? String(row.id) : '';
        var pid = row.parentId != null ? String(row.parentId) : '';
        var isVisible = !pid ? true : (visible[pid] && (expanded.has ? expanded.has(pid) : pid in expanded));
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
        span.className = 'tree-toggle ' + (isExp ? 'expanded' : 'collapsed');
        span.textContent = isExp ? '\u25BC' : '\u25B6';
        span.title = isExp ? '접기' : '펼치기';
      });
    }
    function doSearch(p, tid, pageOverride) {
      var url = p.getAttribute('formurl') || '';
      if (!url || url === '/main') return;
      var cfg = window.PG_SCREENS && window.PG_SCREENS.getMenuScreens && window.PG_SCREENS.getMenuScreens()[url];
      if (!cfg || !cfg.columns) return;
      var params = collectSearchParams(p);
      if (pageOverride) params.page = pageOverride;
      p.setAttribute('data-last-url', url);
      p.setAttribute('data-last-page', String(params.page));
      var dimm = document.getElementById('dimm');
      if (dimm) dimm.style.display = 'flex';
      var api = window.PG_API;
      var promise = null;
      if (url === '/system/noticeList') promise = api.noticeList(params);
      else if (url === '/calc/payList' || url === '/calc/payListNew' || url === '/calc/payFailList' || url === '/calc/offsetCancList' || url === '/pay/easyPay' || url === '/calc/cashReceiptList') promise = api.payList(params);
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
      else if (url === '/calc/compPointMngList' || url === '/settlement/recallMng') promise = api.settlementRecallMng(params);
      else if (url === '/calc/balcInfo' || url === '/settlement/balanceMng') promise = api.settlementBalanceMng(params);
      else if (url === '/pay/payHoldList' || url === '/settlement/holdList') promise = api.settlementHoldList(params);
      else if (url === '/calc/exCalcList' || url === '/settlement/execute') promise = api.settlementExecute(params);
      else if (url === '/noti/notiUrlMng' || url === '/notify/payUrlMng') promise = api.notifyPayUrlMng(params);
      else if (url === '/noti/notiSendMngList' || url === '/notify/paySendMng') promise = api.notifyPaySendMng(params);
      else if (url === '/noti/notiCashReceiptUrlMng' || url === '/notify/cashReceiptUrlMng') promise = api.notifyCashReceiptUrlMng(params);
      else if (url === '/noti/notiCashReceiptSendMngList' || url === '/notify/cashReceiptSendMng') promise = api.notifyCashReceiptSendMng(params);
      else if (url === '/hq/pgApiMng') promise = api.hqPgApiMng(params);
      else if (url === '/hq/permissionMng') promise = api.hqPermissionMng(params);
      else if (url === '/risk/list') promise = Promise.resolve({ list: [], totalElements: 0, totalPages: 1, page: params.page || 1, size: params.size || 20 });
      if (!promise) {
        if (dimm) dimm.style.display = 'none';
        var tbody = p.querySelector('#grid_' + tid + ' tbody');
        if (tbody) tbody.innerHTML = '<tr><td colspan="' + (cfg.columns.length) + '" class="empty-state-cell text-center text-muted">조회된 데이터가 없습니다.</td></tr>';
        var cntEl = p.querySelector('#summary_건수, .summary-count, [data-summary="건수"]');
        if (cntEl) cntEl.textContent = (cntEl.id === 'summary_건수' ? '건수: ' : '') + '0';
        return;
      }
      promise.then(function (data) {
        var list = data && data.list ? data.list : [];
        var total = data && data.totalElements !== undefined ? data.totalElements : list.length;
        var totalPages = data && data.totalPages !== undefined ? data.totalPages : 1;
        var allCols = cfg.columns;
        var selCols = p._compMngSelectedColumns;
        var fixedKeys = ['rowNo', 'compId', 'compNm', 'compDivNm'];
        var cols = allCols.filter(function (c) {
          if (c.type === 'checkbox' || fixedKeys.indexOf(c.key) !== -1) return true;
          if (!selCols || selCols.length === 0) return true;
          return selCols.indexOf(c.key) !== -1;
        });
        var theadTr = p.querySelector('#grid_' + tid + ' thead tr');
        if (theadTr) {
          theadTr.innerHTML = cols.map(function (c) {
            if (c.type === 'checkbox') return '<th style="width:40px"><input type="checkbox" class="grid-check-all" title="전체선택"></th>';
            return '<th>' + (c.label || c.key) + '</th>';
          }).join('');
        }
        var thLen = cols.length;
        var tbody = p.querySelector('#grid_' + tid + ' tbody');
        if (!tbody) return;
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
          list.forEach(function (row) {
            var rowId = row.id != null ? String(row.id) : '';
            var parentId = row.parentId != null ? String(row.parentId) : '';
            var hasChildren = isCompMngTree && rowId && hasChildrenMap[rowId];
            html += '<tr data-id="' + (rowId || '') + '" data-parent-id="' + (parentId || '') + '" data-row="' + (isCompMngTree ? encodeURIComponent(JSON.stringify(row)) : '') + '">';
            cols.forEach(function (c) {
              if (c.type === 'checkbox') html += '<td><input type="checkbox" class="grid-row-check"></td>';
              else {
                var val = row[c.key] !== undefined && row[c.key] !== null ? String(row[c.key]) : '';
                if (isCompMngTree && c.key === 'rowNo') {
                  html += '<td>' + (val || '') + '</td>';
                } else if (isCompMngTree && c.key === 'compId') {
                  var depth = (row.depth != null && row.depth !== '') ? parseInt(row.depth, 10) : 0;
                  if (isNaN(depth)) depth = 0;
                  var px = 18 + depth * 18;
                  var expanded = p._treeExpanded && p._treeExpanded.has(rowId);
                  var folderSvg = '<svg class="tree-icon tree-icon-folder" width="16" height="16" viewBox="0 0 24 24"><path fill="currentColor" d="M10 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"/></svg>';
                  var docSvg = '<svg class="tree-icon tree-icon-doc" width="16" height="16" viewBox="0 0 24 24"><path fill="currentColor" d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6z"/></svg>';
                  var icon = hasChildren ? folderSvg : docSvg;
                  var toggle = hasChildren ? '<span class="tree-toggle ' + (expanded ? 'expanded' : 'collapsed') + '" data-id="' + rowId + '" title="' + (expanded ? '접기' : '펼치기') + '">' + (expanded ? '\u25BC' : '\u25B6') + '</span>' : '<span class="tree-toggle-placeholder"></span>';
                  html += '<td class="tree-comp-cell" style="padding-left:' + px + 'px">' + icon + toggle + (val || '') + '</td>';
                } else if (isCompMngTree && c.key === 'compNm') {
                  html += '<td>' + (val || '') + '</td>';
                } else html += '<td>' + val + '</td>';
              }
            });
            html += '</tr>';
          });
          tbody.innerHTML = html;
          injectTableRowResizeHandles(tbody, thLen);
          if (isCompMngTree && list.length) {
            p._treeList = list;
            applyTreeVisibility(p, tbody, list);
          }
        }
        var cntEl = p.querySelector('#summary_건수, .summary-count, [data-summary="건수"]');
        if (cntEl) cntEl.textContent = (cntEl.id === 'summary_건수' ? '건수: ' : '') + total;
        if (window.updatePaging) window.updatePaging(tid, params.page, totalPages, total);
        p.setAttribute('data-last-total-pages', String(totalPages));
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
        var tbody = p.querySelector('#grid_' + tid + ' tbody');
        if (tbody) tbody.innerHTML = '<tr><td colspan="' + (cfg.columns.length) + '" class="empty-state-cell text-center text-danger">' + (err && err.message ? err.message : '조회 실패') + '</td></tr>';
      }).finally(function () {
        if (dimm) dimm.style.display = 'none';
      });
    }
    pane.querySelectorAll('#searchBtn, .screen-search-btn').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var pageEl = pane.querySelector('#pageCnt');
        if (pageEl) pageEl.value = 1;
        doSearch(pane, tabId, 1);
      });
    });
    pane.querySelectorAll('.search-reset-btn').forEach(function (btn) {
      btn.addEventListener('click', function () {
        pane.querySelectorAll('select[name^="search"]').forEach(function (el) { el.selectedIndex = 0; });
        pane.querySelectorAll('input[name^="search"]').forEach(function (el) { if (el.type === 'text') el.value = ''; else if (el.type === 'checkbox') el.checked = false; });
        var pageEl = pane.querySelector('#pageCnt');
        if (pageEl) pageEl.value = 1;
        doSearch(pane, tabId, 1);
      });
    });
    pane.addEventListener('paging-change', function (e) {
      var detail = e.detail || {};
      doSearch(pane, tabId, detail.page);
    });
    var url = pane.getAttribute('formurl') || '';
    var autoSearchUrls = ['/system/noticeList', '/calc/payList', '/calc/payListNew', '/calc/payFailList', '/calc/offsetCancList', '/pay/easyPay', '/calc/cashReceiptList',
      '/comp/compMngTree', '/comp/myCompMng', '/comp/compInfoHistList', '/commission/commisionList',
      '/user/userMng', '/set/gridSetMng',
      '/calc/calcList', '/calc/calcGmList', '/calc/compPointMngList', '/calc/balcInfo', '/calc/exCalcList', '/pay/payHoldList',
      '/noti/notiUrlMng', '/noti/notiSendMngList', '/noti/notiCashReceiptUrlMng', '/noti/notiCashReceiptSendMngList',
      '/hq/pgApiMng', '/hq/permissionMng', '/risk/list'];
    if (autoSearchUrls.indexOf(url) !== -1) {
      setTimeout(function () { doSearch(pane, tabId, 1); }, 100);
    }
    if (url === '/commission/commisionList') {
      if (window.PG_LAST_REGISTERED_COMP) {
        var input = pane.querySelector('input[name="searchCompId"]');
        if (input) {
          input.value = window.PG_LAST_REGISTERED_COMP;
          var searchBtn = pane.querySelector('#searchBtn');
          if (searchBtn) searchBtn.click();
        }
        window.PG_LAST_REGISTERED_COMP = null;
      }
      var commissionSettingBtn = pane.querySelector('#commissionSettingBtn');
      if (commissionSettingBtn) {
        commissionSettingBtn.addEventListener('click', function () {
          var grid = pane.querySelector('#grid_' + tabId + ' tbody');
          var checked = grid ? grid.querySelector('tr .grid-row-check:checked') : null;
          if (!checked) {
            alert('그리드에서 한 건을 선택한 뒤 [수수료설정]을 눌러주세요.');
            return;
          }
          var tr = checked.closest('tr');
          var tds = tr.querySelectorAll('td');
          var cfg = window.PG_SCREENS && window.PG_SCREENS.getMenuScreens()['/commission/commisionList'];
          var cols = (cfg && cfg.columns) || [];
          var idx = cols.findIndex(function (c) { return c.key === 'compId'; });
          if (idx < 0) idx = 1;
          var compId = (tds[idx] && tds[idx].textContent) ? tds[idx].textContent.trim() : '';
          if (!compId) { alert('업체코드를 찾을 수 없습니다.'); return; }
          var modalEl = document.getElementById('commissionSettingModal');
          var compIdHidden = document.getElementById('commissionSettingCompId');
          if (compIdHidden) compIdHidden.value = compId;
          window._commissionSettingPane = pane;
          window._commissionSettingTabId = tabId;
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.commissionDetail(compId).then(function (data) {
            var set = function (id, val) { var el = document.getElementById(id); if (el && val != null) el.value = String(val); };
            set('commissionPerTxFee', data.perTxFee);
            set('commissionCancelRate', data.cancelRate);
            set('commissionPayRate', data.payRate);
            set('commissionRefundRate', data.refundRate);
            set('commissionRollingPct', data.rollingPct);
            set('commissionRollingDays', data.rollingDays);
            set('commissionFeeAnnual', data.feeAnnual);
            set('commissionFeeSettlementPerTx', data.feeSettlementPerTx);
            if (modalEl && window.bootstrap && bootstrap.Modal) {
              var modal = new bootstrap.Modal(modalEl);
              modal.show();
            }
          }).catch(function (e) { alert(e && e.message ? e.message : '수수료 조회 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
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
          modalEl.querySelectorAll('input[name]').forEach(function (el) {
            if (el.name) fd[el.name] = el.value;
          });
        }
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.commissionSave(compIdVal, fd).then(function () {
          alert('저장되었습니다.');
          if (modalEl && window.bootstrap && bootstrap.Modal) { var m = bootstrap.Modal.getInstance(modalEl); if (m) m.hide(); }
          var pane = window._commissionSettingPane;
          var tabId = window._commissionSettingTabId;
          if (pane && tabId && typeof doSearch === 'function') doSearch(pane, tabId, 1);
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
    if (compMngSaveColumnsBtn && url === '/comp/compMngTree' && !compMngSaveColumnsBtn._bound) {
      compMngSaveColumnsBtn._bound = true;
      compMngSaveColumnsBtn.addEventListener('click', function () {
        var checks = pane.querySelectorAll('.column-guide-check:checked');
        var keys = [];
        checks.forEach(function (cb) {
          var k = cb.getAttribute('data-key');
          if (k) keys.push(k);
        });
        pane._compMngSelectedColumns = keys.length > 0 ? keys : null;
        alert('저장되었습니다. 검색하면 선택된 칼럼만 표시됩니다.');
      });
    }
    var compMngClearColumnsBtn = pane.querySelector('#compMngClearColumnsBtn');
    if (compMngClearColumnsBtn && url === '/comp/compMngTree' && !compMngClearColumnsBtn._bound) {
      compMngClearColumnsBtn._bound = true;
      compMngClearColumnsBtn.addEventListener('click', function () {
        pane.querySelectorAll('.column-guide-check').forEach(function (cb) { cb.checked = false; });
      });
    }
    var compRegBtn = pane.querySelector('#compRegBtn');
    if (compRegBtn) {
      compRegBtn.addEventListener('click', function () { fnTopMenuMove('/comp/compReg'); });
    }
    var compRegSaveBtn = pane.querySelector('#compRegSaveBtn');
    if (compRegSaveBtn) {
      compRegSaveBtn.addEventListener('click', function () {
        var form = pane.querySelector('#compRegForm');
        if (!form) return;
        var fd = {};
        form.querySelectorAll('input, select, textarea').forEach(function (el) {
          if (el.name && el.type !== 'file' && el.name !== 'pgOperational') fd[el.name] = el.value;
        });
        fd.compNm = fd.compNm || fd.comp_name;
        fd.compDiv = fd.compDiv || fd.comp_div || 'MERCHANT';
        if (fd.parentId) fd.parentComp = ''; else if (fd.parentComp && fd.parentComp.indexOf(' (') > 0) fd.parentComp = fd.parentComp.split(' (')[0].trim();
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
                apiKey: sel('apiKey'),
                ivKey: sel('ivKey'),
                installmentYn: sel('installmentYn') || 'N',
                maxInstallmentMonths: sel('maxInstallmentMonths')
              });
            }
          });
          fd.pgBindings = JSON.stringify(bindings);
        }
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.compRegister(fd).then(function (res) {
          var data = res && res.data ? res.data : res;
          if (data && data.compId) {
            window.PG_LAST_REGISTERED_COMP = data.compId;
          }
          alert('저장되었습니다.');
          fnTopMenuMove('/commission/commisionList');
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
      var form = pane.querySelector('#compRegForm');
      if (form && !form.querySelector('input[name="parentId"]')) {
        var hid = document.createElement('input');
        hid.type = 'hidden';
        hid.name = 'parentId';
        hid.id = 'compRegParentId';
        form.insertBefore(hid, form.firstChild);
      }
      function toggleByCompDiv(compDiv) {
        var isMerchant = compDiv === 'MERCHANT';
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
        pane.querySelectorAll('.distributor-or-merchant-section').forEach(function (card) {
          if (showAccount) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.branch-agency-sales-hide-section').forEach(function (card) {
          if (isBranchAgencySales) card.classList.add('d-none'); else card.classList.remove('d-none');
        });
        var hint = pane.querySelector('.comp-div-hint');
        if (hint) hint.style.display = (!compDiv || compDiv === '') ? 'block' : 'none';
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
        });
        if (!compDivEl.value) compDivEl.value = 'MASTER_DIST';
        toggleByCompDiv(compDivEl.value || 'MASTER_DIST');
        initPgBindingList(pane);
      }
      var commissionFollowEl = pane.querySelector('[name="commissionFollowHq"]');
      if (commissionFollowEl && !commissionFollowEl._commissionToggleBound) {
        commissionFollowEl._commissionToggleBound = true;
        function toggleCommissionCustom(useHq) {
          pane.querySelectorAll('.commission-custom-only').forEach(function (el) {
            el.style.display = useHq === 'Y' ? 'none' : '';
          });
        }
        commissionFollowEl.addEventListener('change', function () { toggleCommissionCustom(this.value); });
        toggleCommissionCustom(commissionFollowEl.value || 'Y');
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
      var parentCompSearchBtn = pane.querySelector('button[data-field="parentComp"][data-action="검색"]');
      if (parentCompSearchBtn) {
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
            window.PG_API.compList({ searchCompId: (kw && kw.value) || '', searchCompNm: (kw && kw.value) || '', page: 1, size: 50 }).then(function (data) {
              var list = (data && data.list) ? data.list : [];
              if (!tbody) return;
              tbody.innerHTML = '';
              list.forEach(function (row) {
                var tr = document.createElement('tr');
                tr.style.cursor = 'pointer';
                tr.setAttribute('data-id', row.id != null ? row.id : '');
                tr.setAttribute('data-compId', row.compId != null ? row.compId : '');
                tr.setAttribute('data-compNm', row.compNm != null ? row.compNm : '');
                tr.innerHTML = '<td><button type="button" class="btn btn-sm btn-outline-primary">선택</button></td><td>' + (row.compId || '') + '</td><td>' + (row.compNm || '') + '</td><td>' + (row.compDivNm || row.compDiv || '') + '</td>';
                tr.addEventListener('click', function (e) {
                  if (e.target.tagName === 'BUTTON') return;
                  selectParentComp(tr);
                });
                tr.querySelector('button').addEventListener('click', function () { selectParentComp(tr); });
                tbody.appendChild(tr);
              });
              if (list.length === 0) tbody.innerHTML = '<tr><td colspan="4" class="text-muted text-center">조회된 업체가 없습니다.</td></tr>';
            }).catch(function (err) {
              if (tbody) tbody.innerHTML = '<tr><td colspan="4" class="text-danger text-center">' + (err && err.message ? err.message : '조회 실패') + '</td></tr>';
            }).finally(function () { if (dimm) dimm.style.display = 'none'; });
          }
          function selectParentComp(tr) {
            var id = tr.getAttribute('data-id');
            var compId = tr.getAttribute('data-compId');
            var compNm = tr.getAttribute('data-compNm');
            var f = pane.querySelector('#compRegForm');
            if (f) {
              var pid = f.querySelector('input[name="parentId"]');
              if (pid) pid.value = id || '';
              var pc = f.querySelector('input[name="parentComp"]');
              if (pc) pc.value = (compId || '') + (compNm ? ' (' + compNm + ')' : '');
            }
            if (modal) modal.hide();
          }
          var modalSearchBtn = document.getElementById('parentCompSearchBtn');
          if (modalSearchBtn && !modalSearchBtn._parentCompBound) {
            modalSearchBtn._parentCompBound = true;
            modalSearchBtn.addEventListener('click', function () {
              var tbody = document.getElementById('parentCompSearchTbody');
              var dimm = document.getElementById('dimm');
              var kw = document.getElementById('parentCompSearchKeyword');
              if (!tbody) return;
              if (dimm) dimm.style.display = 'flex';
              window.PG_API.compList({ searchCompId: (kw && kw.value) || '', searchCompNm: (kw && kw.value) || '', page: 1, size: 50 }).then(function (data) {
                var list = (data && data.list) ? data.list : [];
                tbody.innerHTML = '';
                list.forEach(function (row) {
                  var tr = document.createElement('tr');
                  tr.style.cursor = 'pointer';
                  tr.setAttribute('data-id', row.id != null ? row.id : '');
                  tr.setAttribute('data-compId', row.compId != null ? row.compId : '');
                  tr.setAttribute('data-compNm', row.compNm != null ? row.compNm : '');
                  tr.innerHTML = '<td><button type="button" class="btn btn-sm btn-outline-primary">선택</button></td><td>' + (row.compId || '') + '</td><td>' + (row.compNm || '') + '</td><td>' + (row.compDivNm || row.compDiv || '') + '</td>';
                  tr.addEventListener('click', function (e) {
                    if (e.target.tagName === 'BUTTON') return;
                    var f2 = document.querySelector('[formurl="/comp/compReg"]');
                    if (f2) {
                      var form = f2.querySelector('#compRegForm');
                      if (form) {
                        var pid = form.querySelector('input[name="parentId"]');
                        if (pid) pid.value = tr.getAttribute('data-id') || '';
                        var pc = form.querySelector('input[name="parentComp"]');
                        if (pc) pc.value = (tr.getAttribute('data-compId') || '') + (tr.getAttribute('data-compNm') ? ' (' + tr.getAttribute('data-compNm') + ')' : '');
                      }
                    }
                    if (modalEl && window.bootstrap && bootstrap.Modal) { var m = bootstrap.Modal.getInstance(modalEl); if (m) m.hide(); }
                  });
                  tr.querySelector('button').addEventListener('click', function () {
                    var f2 = document.querySelector('[formurl="/comp/compReg"]');
                    if (f2) {
                      var form = f2.querySelector('#compRegForm');
                      if (form) {
                        var pid = form.querySelector('input[name="parentId"]');
                        if (pid) pid.value = tr.getAttribute('data-id') || '';
                        var pc = form.querySelector('input[name="parentComp"]');
                        if (pc) pc.value = (tr.getAttribute('data-compId') || '') + (tr.getAttribute('data-compNm') ? ' (' + tr.getAttribute('data-compNm') + ')' : '');
                      }
                    }
                    if (modalEl && window.bootstrap && bootstrap.Modal) { var m = bootstrap.Modal.getInstance(modalEl); if (m) m.hide(); }
                  });
                  tbody.appendChild(tr);
                });
                if (list.length === 0) tbody.innerHTML = '<tr><td colspan="4" class="text-muted text-center">조회된 업체가 없습니다.</td></tr>';
              }).catch(function (err) {
                tbody.innerHTML = '<tr><td colspan="4" class="text-danger text-center">' + (err && err.message ? err.message : '조회 실패') + '</td></tr>';
              }).finally(function () { if (dimm) dimm.style.display = 'none'; });
            });
          }
          runSearch();
        });
      }
    }
    function loadCompDetailIntoForm(pane, compId) {
      if (!compId) return;
      var dimm = document.getElementById('dimm');
      if (dimm) dimm.style.display = 'flex';
      window.PG_API.compDetail(compId).then(function (data) {
        var form = pane.querySelector('#compInfoDetailForm');
        if (!form || !data) return;
        ['compId', 'compNm', 'compDiv', 'regNo', 'bizType', 'industry', 'bizNature', 'product', 'homepage', 'settleName', 'settleTelNo', 'ceoNm', 'ceoMobile', 'compTel', 'fax', 'zipCode', 'addr', 'addrDetail', 'email', 'useYn', 'loginId', 'bankCd', 'transferFee', 'cryptoTransferFee', 'accountNo', 'accountHolder', 'commissionConfigAllowed', 'webPaymentUseYn', 'baseCurrency', 'remark'].forEach(function (k) {
          var el = form.querySelector('[name="' + k + '"]');
          if (el && data[k] != null) el.value = data[k];
        });
        var rn = data.regNo;
        if (rn && rn.indexOf('|') >= 0) {
          var p = rn.split('|');
          var rt = form.querySelector('[name="regType"]');
          var rnEl = form.querySelector('[name="regNo"]');
          if (rt) rt.value = (p[0] === 'PERSONAL' || p[0] === 'CORP') ? p[0] : 'CORP';
          if (rnEl) rnEl.value = p.length > 1 ? p.slice(1).join('|') : '';
        }
        var pgInfoCard = pane.querySelector('#pgInfoCard');
        if (pgInfoCard) {
          pgInfoCard.style.display = (data.compDiv === 'MERCHANT') ? '' : 'none';
          var paymentUrlEl = pane.querySelector('#paymentUrlDisplay');
          if (paymentUrlEl && data.orgUnitId && data.compDiv === 'MERCHANT') {
            var base = (window.SITE_CONFIG && window.SITE_CONFIG.paymentBaseUrl) || (window.location.origin || '');
            paymentUrlEl.value = base.replace(/\/$/, '') + '/pay.html?m=' + data.orgUnitId;
          } else if (paymentUrlEl) paymentUrlEl.value = '';
        }
        var isRegionalSelf = data.compDiv === 'REGIONAL';
        if (isRegionalSelf) {
          try {
            var u = JSON.parse(sessionStorage.getItem('pg_admin_user') || '{}');
            if (u.compId === data.compId && u.orgLevel === 'REGIONAL') {
              form.querySelectorAll('input, select, textarea').forEach(function (el) { if (el.name !== 'compId') el.disabled = true; });
              var updBtn = pane.querySelector('#compInfoUpdateBtn');
              if (updBtn) updBtn.style.display = 'none';
            }
          } catch (e) {}
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
        var card = pane.querySelector('#compInfoDetailCard');
        if (card) card.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }).catch(function (e) { alert(e && e.message ? e.message : '상세 조회 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
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
      var compInfoDetailBtn = pane.querySelector('#compInfoDetailBtn');
      if (compInfoDetailBtn) {
        compInfoDetailBtn.addEventListener('click', function () {
          var grid = pane.querySelector('#grid_' + tabId + ' tbody');
          if (!grid) return;
          var checked = grid.querySelector('tr .grid-row-check:checked');
          if (!checked) { alert('그리드에서 한 건을 선택한 뒤 [상세] 버튼을 눌러주세요.'); return; }
          var tr = checked.closest('tr');
          var tds = tr.querySelectorAll('td');
          var cfg = window.PG_SCREENS && (window.PG_SCREENS.getMenuScreens()['/comp/compInfo'] || window.PG_SCREENS.getMenuScreens()['/comp/myCompMng']);
          var cols = (cfg && cfg.columns) || [];
          var compIdIdx = cols.findIndex(function (c) { return c.key === 'compId'; });
          if (compIdIdx < 0) compIdIdx = 1;
          var compId = (tds[compIdIdx] && tds[compIdIdx].textContent) ? tds[compIdIdx].textContent.trim() : '';
          if (!compId) { alert('업체코드를 찾을 수 없습니다.'); return; }
          loadCompDetailIntoForm(pane, compId);
        });
      }
      var compInfoUpdateBtn = pane.querySelector('#compInfoUpdateBtn');
      if (compInfoUpdateBtn) {
        compInfoUpdateBtn.addEventListener('click', function () {
          var form = pane.querySelector('#compInfoDetailForm');
          if (!form) return;
          var compIdEl = form.querySelector('[name="compId"]');
          var compId = compIdEl && compIdEl.value ? compIdEl.value.trim() : '';
          if (!compId) { alert('업체코드가 없습니다. 먼저 [상세]로 조회하세요.'); return; }
          var fd = {};
          form.querySelectorAll('input, select, textarea').forEach(function (el) {
            if (el.name && el.type !== 'file') fd[el.name] = el.value;
          });
          fd.compId = compId;
          if (fd.regType != null) { fd.regNo = (fd.regType || 'CORP') + '|' + (fd.regNo || ''); delete fd.regType; }
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.compUpdate(fd).then(function () {
            alert('저장되었습니다.');
            doSearch(pane, tabId, 1);
          }).catch(function (e) { alert(e && e.message ? e.message : '수정 저장 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
      }
    }
    if (url === '/comp/compDetail') {
      function toggleByCompDiv(compDiv) {
        var isMerchant = compDiv === 'MERCHANT';
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
        pane.querySelectorAll('.distributor-or-merchant-section').forEach(function (card) {
          if (showAccount) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.branch-agency-sales-hide-section').forEach(function (card) {
          if (isBranchAgencySales) card.classList.add('d-none'); else card.classList.remove('d-none');
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
        var allFields = ['compId', 'parentComp', 'compNm', 'compDiv', 'regNo', 'bizType', 'industry', 'bizNature', 'product', 'homepage', 'settleName', 'settleTelNo', 'ceoNm', 'ceoMobile', 'compTel', 'fax', 'zipCode', 'addr', 'addrDetail', 'email', 'siteUrl', 'siteSummary', 'useYn', 'loginId', 'bankCd', 'transferFee', 'cryptoTransferFee', 'accountNo', 'accountHolder', 'commissionConfigAllowed', 'webPaymentUseYn', 'baseCurrency', 'remark', 'settleType', 'commissionRate', 'limitAmt', 'countryCd', 'swift', 'branchName', 'branchAddr', 'contactTel', 'walletAddress', 'networkName', 'withdrawLimitDays', 'withdrawStartTime', 'withdrawEndTime', 'payLimitDefault', 'payLimitExtra', 'payLimitAlertSms', 'holdRateFollowHq', 'holdRate', 'holdDays', 'commissionFollowHq', 'failFee', 'usageRate', 'payRate', 'cancelRate', 'refundRate', 'commissionMemo', 'feeSettlementPerTx', 'feeUsdt', 'feeFx', 'calcCycle', 'calcCloseTime', 'transferType', 'transferCycleDays', 'autoTransferMin', 'calcExcludeYn', 'calcExcludeTarget', 'calcStartTime', 'payHoldYn', 'defaultProductName', 'defaultProductCode', 'defaultProductAmount', 'defaultProductDesc', 'notifyUrlBackground', 'notifyUrlResult'];
        allFields.forEach(function (k) {
          var el = form.querySelector('[name="' + k + '"]');
          if (el && data[k] != null) el.value = data[k];
        });
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
        initPgBindingList(pane, data.pgBindings);
        var commissionFollowEl = pane.querySelector('[name="commissionFollowHq"]');
        if (commissionFollowEl && !commissionFollowEl._commissionToggleBound) {
          commissionFollowEl._commissionToggleBound = true;
          function toggleCommissionCustom(useHq) {
            pane.querySelectorAll('.commission-custom-only').forEach(function (el) {
              el.style.display = useHq === 'Y' ? 'none' : '';
            });
          }
          commissionFollowEl.addEventListener('change', function () { toggleCommissionCustom(this.value); });
          toggleCommissionCustom(commissionFollowEl.value || 'Y');
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
        var pgInfoCard = pane.querySelector('#pgInfoCard');
        if (pgInfoCard) {
          var divForPg = apiCompDiv || storedCompDiv;
          pgInfoCard.style.display = (divForPg === 'MERCHANT') ? '' : 'none';
          var paymentUrlEl = pane.querySelector('#paymentUrlDisplay');
          if (paymentUrlEl && data.orgUnitId && divForPg === 'MERCHANT') {
            var base = (window.SITE_CONFIG && window.SITE_CONFIG.paymentBaseUrl) || (window.location.origin || '');
            paymentUrlEl.value = base.replace(/\/$/, '') + '/pay.html?m=' + data.orgUnitId;
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
      }).catch(function (e) {
        pane.innerHTML = '<div class="card"><div class="card-body"><p class="text-danger">' + (e && e.message ? e.message : '조회 실패') + '</p><button type="button" class="btn btn-secondary btn-sm" id="compDetailListBtn">목록</button></div></div>';
      }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      }
      var compDetailSaveBtn = pane.querySelector('#compDetailSaveBtn');
      if (compDetailSaveBtn) {
        compDetailSaveBtn.addEventListener('click', function () {
          var form = pane.querySelector('#compDetailForm');
          if (!form) return;
          var compIdEl = form.querySelector('[name="compId"]');
          var compId = compIdEl && compIdEl.value ? compIdEl.value.trim() : '';
          if (!compId) { alert('업체코드가 없습니다.'); return; }
          var fd = {};
          form.querySelectorAll('input, select, textarea').forEach(function (el) {
            if (el.name && el.type !== 'file' && el.name !== 'pgOperational') fd[el.name] = el.value;
          });
          fd.compId = compId;
          if (fd.parentComp && fd.parentComp.indexOf(' (') > 0) fd.parentComp = fd.parentComp.split(' (')[0].trim();
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
                  apiKey: sel('apiKey'),
                  ivKey: sel('ivKey'),
                  installmentYn: sel('installmentYn') || 'N',
                  maxInstallmentMonths: sel('maxInstallmentMonths')
                });
              }
            });
            fd.pgBindings = JSON.stringify(bindings);
          }
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.compUpdate(fd).then(function () {
            var settleFd = {};
            ['withdrawLimitDays', 'payLimitDefault', 'payLimitExtra', 'holdRate', 'holdDays', 'calcCycle', 'transferType', 'autoTransferMin', 'payHoldYn'].forEach(function (k) {
              if (fd[k] !== undefined && fd[k] !== null && fd[k] !== '') settleFd[k] = fd[k];
            });
            if (Object.keys(settleFd).length > 0) {
              return window.PG_API.settlementSettingSave(compId, settleFd);
            }
            return Promise.resolve();
          }).then(function () {
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
          var currentId = loginIdEl ? loginIdEl.value : '';
          var newId = prompt('새 로그인 ID를 입력하세요.', currentId);
          if (newId != null && newId.trim()) {
            var dimm = document.getElementById('dimm');
            if (dimm) dimm.style.display = 'flex';
            window.PG_API.compChangeLoginId(compId, newId.trim()).then(function () {
              alert('로그인 ID가 변경되었습니다.');
              if (loginIdEl) loginIdEl.value = newId.trim();
            }).catch(function (err) { alert(err && err.message ? err.message : 'ID 변경 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
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
      if (dimm) dimm.style.display = 'flex';
      window.PG_API.hqDefaultCommission().then(function (data) {
        if (data && pane.querySelector('[name="payRate"]')) {
          ['perTxFee', 'cancelRate', 'usageRate', 'failFee', 'payRate', 'refundRate', 'rollingPct', 'rollingDays', 'memo'].forEach(function (k) {
            var el = pane.querySelector('[name="' + k + '"]');
            if (el && data[k] != null) el.value = data[k];
          });
        }
      }).catch(function () {}).finally(function () { if (dimm) dimm.style.display = 'none'; });
      var hqDefSave = pane.querySelector('#hqDefaultCommissionSaveBtn');
      if (hqDefSave) hqDefSave.addEventListener('click', function () {
        var fd = {};
        pane.querySelectorAll('input, select, textarea').forEach(function (el) { if (el.name) fd[el.name] = el.value; });
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.hqDefaultCommissionSave(fd).then(function () { alert('저장되었습니다.'); }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      });
    }
    if (url === '/hq/apiConfig') {
      var dimm2 = document.getElementById('dimm');
      if (dimm2) dimm2.style.display = 'flex';
      window.PG_API.hqApiConfig().then(function (data) {
        if (data && pane.querySelector('[name="baseUrl"]')) {
          ['baseUrl', 'authType', 'timeoutSec', 'memo', 'chillpayMerchantCode', 'chillpayApiKey', 'chillpayMd5Key', 'chillpayRouteNo', 'chillpaySandbox'].forEach(function (k) {
            var el = pane.querySelector('[name="' + k + '"]');
            if (el && data[k] != null) el.value = data[k];
          });
        }
      }).catch(function () {}).finally(function () { if (dimm2) dimm2.style.display = 'none'; });
      var hqApiSave = pane.querySelector('#hqApiConfigSaveBtn');
      if (hqApiSave) hqApiSave.addEventListener('click', function () {
        var fd = {};
        pane.querySelectorAll('input, select, textarea').forEach(function (el) { if (el.name) fd[el.name] = el.value; });
        if (dimm2) dimm2.style.display = 'flex';
        window.PG_API.hqApiConfigSave(fd).then(function () { alert('저장되었습니다.'); }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimm2) dimm2.style.display = 'none'; });
      });
    }
    var hqPgApiAddBtn = pane.querySelector('#hqPgApiAddBtn');
    if (hqPgApiAddBtn) {
      hqPgApiAddBtn.addEventListener('click', function () { alert('PG사 연동 추가는 추후 상세 화면으로 구현됩니다.'); });
    }
    var hqPermissionSaveBtn = pane.querySelector('#hqPermissionSaveBtn');
    if (hqPermissionSaveBtn) {
      hqPermissionSaveBtn.addEventListener('click', function () {
        var dimm3 = document.getElementById('dimm');
        if (dimm3) dimm3.style.display = 'flex';
        window.PG_API.hqPermissionMngSave({}).then(function () { alert('저장되었습니다.'); }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimm3) dimm3.style.display = 'none'; });
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
      var total = totalPages;
      var show = [];
      var maxVisible = 7;
      if (total <= maxVisible) {
        for (var i = 1; i <= total; i++) show.push(i);
      } else {
        show.push(1);
        var from = Math.max(2, cur - 1);
        var to = Math.min(total - 1, cur + 1);
        if (from > 2) show.push('...');
        for (var j = from; j <= to; j++) { if (show.indexOf(j) === -1) show.push(j); }
        if (to < total - 1) show.push('...');
        if (total > 1) show.push(total);
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
    pane.querySelectorAll('.pagination-size-opt').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var size = this.getAttribute('data-size');
        if (!size) return;
        var sizeInput = pane.querySelector('#recordsPerPage');
        if (sizeInput) sizeInput.value = size;
        syncPaginationSizeButtons(pane, size);
        doSearch(pane, tabId, 1);
      });
    });
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
        pane.innerHTML = window.PG_SCREENS.getScreenHtml(url, tabId);
        pane.classList.add('show', 'active');
        pane.style.display = 'block';
        bindScreenEvents(pane, tabId);
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
    var link = document.querySelector('.child-li[data-url="' + url + '"] a');
    var mid = menuId || (link && link.getAttribute('data-menu_id'));
    var info = MENU_INFO[url] || {};
    var text = label || (link && link.textContent.trim()) || info.label || getTabIdFromUrl(url);
    addTabAndSwitch(url, mid, text);
  };

  // 로고 클릭 / 탭 위임 / 접기
  document.addEventListener('DOMContentLoaded', function () {
    setTableRowPaddingY(getTableRowPaddingY());
    var rowResizeState = { active: false, startY: 0, startPx: 0 };
    document.addEventListener('mousedown', function (e) {
      var handle = e.target && e.target.closest ? e.target.closest('.table-row-resize-handle') : null;
      if (!handle) return;
      e.preventDefault();
      rowResizeState.active = true;
      rowResizeState.startY = e.clientY;
      rowResizeState.startPx = getTableRowPaddingY();
    });
    document.addEventListener('mousemove', function (e) {
      if (!rowResizeState.active) return;
      var delta = e.clientY - rowResizeState.startY;
      setTableRowPaddingY(rowResizeState.startPx + delta);
    });
    document.addEventListener('mouseup', function () {
      rowResizeState.active = false;
    });
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
        if (span) span.textContent = leftMenu.classList.contains('collapsed') ? ' ≫펴기' : ' 접기';
        if (icon) icon.className = leftMenu.classList.contains('collapsed') ? 'bi bi-chevron-right' : 'bi bi-chevron-left';
      });
    }
    // 대메뉴 클릭 → 펼침: 토글 / 접힘: 플라이아웃으로 서브 표시 (사이드바 유지)
    var flyout = document.getElementById('flyout-submenu');
    function hideFlyout() {
      if (flyout) flyout.style.display = 'none';
    }
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
          this.setAttribute('aria-expanded', sub.classList.contains('mm-show'));
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
  });
})();
