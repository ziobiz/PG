/**
 * PG 솔루션 - fxhj와 동일한 메뉴 구성, 화면은 하나씩 구현
 * fnTopMenuMove(url): 메뉴 클릭 시 탭 추가/전환, 우리 페이지 또는 placeholder 표시
 */

(function () {
  'use strict';

  window.SITE_CONFIG = {
    contentBaseUrl: '',
    contentMode: 'iframe'
  };

  // 메뉴별 URL → 라벨, parent (브레드크럼/탭 제목용)
  var MENU_INFO = {
    '/hq/pgApiMng': { label: 'PG사 API 연동', parent: '본사설정' },
    '/hq/defaultCommission': { label: '기본 수수료 정책', parent: '본사설정' },
    '/hq/apiConfig': { label: 'API 구성 세팅', parent: '본사설정' },
    '/hq/permissionMng': { label: '본사별 권한 세팅', parent: '본사설정' },
    '/system/noticeList': { label: '공지사항', parent: '업체관리' },
    '/comp/compInfo': { label: '업체정보조회', parent: '업체관리' },
    '/comp/compMng': { label: '업체관리', parent: '업체관리' },
    '/commission/commisionList': { label: '수수료관리', parent: '업체관리' },
    '/comp/compChangeHistory': { label: '업체변경이력', parent: '업체관리' },
    '/calc/payList': { label: '결제내역', parent: '결제관리' },
    '/calc/payListNew': { label: '결제내역(신)', parent: '결제관리' },
    '/calc/payFailList': { label: '결제실패내역', parent: '결제관리' },
    '/calc/offsetCancelList': { label: '상계취소내역', parent: '결제관리' },
    '/calc/urlPayList': { label: 'URL간편결제 내역', parent: '결제관리' },
    '/calc/cashReceiptList': { label: '현금영수증내역', parent: '결제관리' },
    '/settlement/distributionList': { label: '유통망정산내역', parent: '정산관리' },
    '/settlement/franchiseList': { label: '가맹정산내역', parent: '정산관리' },
    '/settlement/recallMng': { label: '환수금관리', parent: '정산관리' },
    '/settlement/balanceMng': { label: '잔액/미수금관리', parent: '정산관리' },
    '/settlement/execute': { label: '정산실행', parent: '정산관리' },
    '/settlement/holdList': { label: '정산보류내역', parent: '정산관리' },
    '/notify/payUrlMng': { label: '결제통보 URL관리', parent: '통보관리' },
    '/notify/paySendMng': { label: '결제통보 전송관리', parent: '통보관리' },
    '/notify/cashReceiptUrlMng': { label: '현금영수증통보 URL관리', parent: '통보관리' },
    '/notify/cashReceiptSendMng': { label: '현금영수증통보 전송관리', parent: '통보관리' },
    '/user/userMng': { label: '사용자관리', parent: '사용자관리' },
    '/user/menuOrderMng': { label: '메뉴별항목순서관리', parent: '사용자관리' },
    '/risk/list': { label: '리스크 현황', parent: '리스크관리' }
  };

  var config = window.SITE_CONFIG;
  var TAB_UL = 'copyTopTabUl';
  var TAB_MAIN = 'topTapMain';
  var CONTENT_FRAME = 'content-frame';
  var PLACEHOLDER_ID = 'content-placeholder';

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

  function addTabAndSwitch(url, menuId, label) {
    var tabId = getTabIdFromUrl(url);
    var ul = document.getElementById(TAB_UL);
    if (!ul) return;

    var existing = ul.querySelector('[top_tab_url="' + url + '"]');
    if (existing) {
      existing.querySelector('.tab-a').click();
      return;
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
      if (sizeEl) params.size = parseInt(sizeEl.value, 10) || 20;
      p.querySelectorAll('input, select').forEach(function (el) {
        var name = el.name || el.id;
        if (!name || el.type === 'checkbox' && !el.classList.contains('grid-check-all')) return;
        if (el.type === 'checkbox') return;
        var v = el.value;
        if (v === undefined || v === null) return;
        if (name.indexOf('search') === 0) params[name] = v;
      });
      var pageEl = p.querySelector('#pageCnt');
      if (pageEl) params.page = parseInt(pageEl.value, 10) || 1;
      return params;
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
      else if (url === '/calc/payList' || url === '/calc/payListNew' || url === '/calc/payFailList' || url === '/calc/offsetCancelList' || url === '/calc/urlPayList' || url === '/calc/cashReceiptList') promise = api.payList(params);
      else if (url === '/comp/compMngTree' || url === '/comp/myCompMng' || url === '/comp/compMng' || url === '/comp/compInfo') promise = api.compList(params);
      else if (url === '/comp/compChangeHistory') promise = api.compChangeHistory(params);
      else if (url === '/commission/commisionList') promise = api.commissionList(params);
      else if (url === '/user/userMng') promise = api.userList(params);
      else if (url === '/user/menuOrderMng') promise = api.menuOrderMng(params);
      else if (url === '/settlement/distributionList') promise = api.settlementDistributionList(params);
      else if (url === '/settlement/franchiseList') promise = api.settlementFranchiseList(params);
      else if (url === '/settlement/recallMng') promise = api.settlementRecallMng(params);
      else if (url === '/settlement/balanceMng') promise = api.settlementBalanceMng(params);
      else if (url === '/settlement/holdList') promise = api.settlementHoldList(params);
      else if (url === '/settlement/execute') promise = api.settlementExecute(params);
      else if (url === '/notify/payUrlMng') promise = api.notifyPayUrlMng(params);
      else if (url === '/notify/paySendMng') promise = api.notifyPaySendMng(params);
      else if (url === '/notify/cashReceiptUrlMng') promise = api.notifyCashReceiptUrlMng(params);
      else if (url === '/notify/cashReceiptSendMng') promise = api.notifyCashReceiptSendMng(params);
      else if (url === '/hq/pgApiMng') promise = api.hqPgApiMng(params);
      else if (url === '/hq/permissionMng') promise = api.hqPermissionMng(params);
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
        var cols = cfg.columns;
        var thLen = (p.querySelectorAll('#grid_' + tid + ' thead th') || []).length;
        var tbody = p.querySelector('#grid_' + tid + ' tbody');
        if (!tbody) return;
        if (list.length === 0) {
          tbody.innerHTML = '<tr><td colspan="' + (thLen || (cfg.columns && cfg.columns.length)) + '" class="empty-state-cell text-center text-muted py-4">조회된 데이터가 없습니다.</td></tr>';
        } else {
          var html = '';
          list.forEach(function (row) {
            html += '<tr>';
            cols.forEach(function (c) {
              if (c.type === 'checkbox') html += '<td><input type="checkbox" class="grid-row-check"></td>';
              else html += '<td>' + (row[c.key] !== undefined && row[c.key] !== null ? String(row[c.key]) : '') + '</td>';
            });
            html += '</tr>';
          });
          tbody.innerHTML = html;
        }
        var cntEl = p.querySelector('#summary_건수, .summary-count, [data-summary="건수"]');
        if (cntEl) cntEl.textContent = (cntEl.id === 'summary_건수' ? '건수: ' : '') + total;
        if (window.updatePaging) window.updatePaging(tid, params.page, totalPages);
        p.setAttribute('data-last-total-pages', String(totalPages));
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
    pane.addEventListener('paging-change', function (e) {
      var detail = e.detail || {};
      doSearch(pane, tabId, detail.page);
    });
    var url = pane.getAttribute('formurl') || '';
    var autoSearchUrls = ['/system/noticeList', '/calc/payList', '/calc/payListNew', '/calc/payFailList', '/calc/offsetCancelList', '/calc/urlPayList',
      '/comp/compMngTree', '/comp/myCompMng', '/comp/compMng', '/comp/compInfo', '/comp/compChangeHistory', '/commission/commisionList',
      '/user/userMng', '/user/menuOrderMng',
      '/settlement/distributionList', '/settlement/franchiseList', '/settlement/recallMng', '/settlement/balanceMng', '/settlement/holdList', '/settlement/execute',
      '/notify/payUrlMng', '/notify/paySendMng', '/notify/cashReceiptUrlMng', '/notify/cashReceiptSendMng',
      '/hq/pgApiMng', '/hq/permissionMng', '/calc/cashReceiptList'];
    if (autoSearchUrls.indexOf(url) !== -1) {
      setTimeout(function () { doSearch(pane, tabId, 1); }, 100);
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
          if (el.name && el.type !== 'file') fd[el.name] = el.value;
        });
        fd.compNm = fd.compNm || fd.comp_name;
        fd.compDiv = fd.compDiv || fd.comp_div || 'MERCHANT';
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.compRegister(fd).then(function (res) {
          alert('저장되었습니다.');
          fnTopMenuMove('/comp/compMngTree');
        }).catch(function (err) {
          alert(err && err.message ? err.message : '저장에 실패했습니다.');
        }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      });
    }
    var compRegCancelBtn = pane.querySelector('#compRegCancelBtn');
    if (compRegCancelBtn) {
      compRegCancelBtn.addEventListener('click', function () { fnTopMenuMove('/comp/compMngTree'); });
    }
    if (url === '/comp/compInfo') {
      var compInfoDetailBtn = pane.querySelector('#compInfoDetailBtn');
      if (compInfoDetailBtn) {
        compInfoDetailBtn.addEventListener('click', function () {
          var grid = pane.querySelector('#grid_' + tabId + ' tbody');
          if (!grid) return;
          var checked = grid.querySelector('tr .grid-row-check:checked');
          if (!checked) { alert('그리드에서 한 건을 선택한 뒤 [상세] 버튼을 눌러주세요.'); return; }
          var tr = checked.closest('tr');
          var tds = tr.querySelectorAll('td');
          var cols = (window.PG_SCREENS && window.PG_SCREENS.getMenuScreens()['/comp/compInfo'].columns) || [];
          var compIdIdx = cols.findIndex(function (c) { return c.key === 'compId'; });
          if (compIdIdx < 0) compIdIdx = 1;
          var compId = (tds[compIdIdx] && tds[compIdIdx].textContent) ? tds[compIdIdx].textContent.trim() : '';
          if (!compId) { alert('업체코드를 찾을 수 없습니다.'); return; }
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.compDetail(compId).then(function (data) {
            var form = pane.querySelector('#compInfoDetailForm');
            if (!form || !data) return;
            ['compId', 'compNm', 'compDiv', 'regNo', 'bizType', 'industry', 'ceoNm', 'ceoMobile', 'compTel', 'fax', 'zipCode', 'addr', 'addrDetail', 'email', 'useYn', 'loginId', 'bankCd', 'transferFee', 'accountNo', 'accountHolder', 'remark'].forEach(function (k) {
              var el = form.querySelector('[name="' + k + '"]');
              if (el && data[k] != null) el.value = data[k];
            });
          }).catch(function (e) { alert(e && e.message ? e.message : '상세 조회 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
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
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.compUpdate(fd).then(function () {
            alert('저장되었습니다.');
            doSearch(pane, tabId, 1);
          }).catch(function (e) { alert(e && e.message ? e.message : '수정 저장 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
      }
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
          ['baseUrl', 'authType', 'timeoutSec', 'memo'].forEach(function (k) {
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
    var executeBtn = pane.querySelector('#executeBtn');
    if (executeBtn && url === '/settlement/execute') {
      executeBtn.addEventListener('click', function () {
        var fromEl = pane.querySelector('#searchFromDate');
        var toEl = pane.querySelector('#searchToDate');
        var fromDate = fromEl && fromEl.value ? fromEl.value : '';
        var toDate = toEl && toEl.value ? toEl.value : '';
        if (!fromDate || !toDate) { alert('정산대상일(시작일~종료일)을 입력하세요.'); return; }
        var dimm4 = document.getElementById('dimm');
        if (dimm4) dimm4.style.display = 'flex';
        window.PG_API.settlementExecuteRun({ fromDate: fromDate, toDate: toDate }).then(function (list) {
          alert('정산 실행 완료. ' + (list && list.length ? list.length : 0) + '건');
          doSearch(pane, tabId, 1);
        }).catch(function (e) {
          alert(e && e.message ? e.message : '정산 실행 실패');
        }).finally(function () { if (dimm4) dimm4.style.display = 'none'; });
      });
    }
    bindPagingEvents(pane, tabId);
  }

  function getPagingContainer(pane, tabId) {
    if (!pane || !tabId) return null;
    return pane.querySelector('#paging_' + tabId) || pane.querySelector('.pagination-pages');
  }

  function renderPagingNumbers(pane, tabId, currentPage, totalPages) {
    var container = getPagingContainer(pane, tabId);
    if (!container) return;
    currentPage = Math.max(1, parseInt(currentPage, 10) || 1);
    totalPages = Math.max(1, parseInt(totalPages, 10) || 1);
    var totalPageEl = pane.querySelector('#totalPageCount');
    if (totalPageEl) totalPageEl.textContent = totalPages;
    var pageCntEl = pane.querySelector('#pageCnt');
    if (pageCntEl) { pageCntEl.value = currentPage; pageCntEl.setAttribute('max', totalPages); }
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
        renderPagingNumbers(pane, tabId, page, totalPages);
        pane.dispatchEvent(new CustomEvent('paging-change', { detail: { page: page, totalPages: totalPages } }));
      });
    });
  }

  function bindPagingEvents(pane, tabId) {
    if (!pane || !tabId) return;
    renderPagingNumbers(pane, tabId, 1, 1);
    var pageSearchBtn = pane.querySelector('#pageSearch');
    if (pageSearchBtn) {
      pageSearchBtn.addEventListener('click', function () {
        var pageCntEl = pane.querySelector('#pageCnt');
        var totalPageEl = pane.querySelector('#totalPageCount');
        var page = parseInt(pageCntEl && pageCntEl.value ? pageCntEl.value : 1, 10);
        var total = parseInt(totalPageEl && totalPageEl.textContent ? totalPageEl.textContent : 1, 10);
        page = Math.max(1, Math.min(total, page));
        if (pageCntEl) pageCntEl.value = page;
        renderPagingNumbers(pane, tabId, page, total);
        pane.dispatchEvent(new CustomEvent('paging-change', { detail: { page: page, totalPages: total } }));
      });
    }
    var pageCntEl = pane.querySelector('#pageCnt');
    if (pageCntEl) {
      pageCntEl.addEventListener('change', function () {
        var total = parseInt(pane.querySelector('#totalPageCount').textContent || 1, 10);
        var page = Math.max(1, Math.min(total, parseInt(this.value, 10) || 1));
        renderPagingNumbers(pane, tabId, page, total);
      });
    }
  }

  window.updatePaging = function (tabId, currentPage, totalPages) {
    var pane = document.getElementById(tabId);
    if (pane) renderPagingNumbers(pane, tabId, currentPage, totalPages);
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
    var text = label || (link && link.textContent.trim()) || getTabIdFromUrl(url);
    addTabAndSwitch(url, mid, text);
  };

  // 로고 클릭 / 탭 위임 / 접기
  document.addEventListener('DOMContentLoaded', function () {
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
    // 좌측 메뉴 접기 (layout: leftSideFoldSpan 접기/펼치기)
    var foldBtn = document.getElementById('leftSideFoldBtn');
    if (foldBtn) {
      foldBtn.addEventListener('click', function () {
        var left = document.querySelector('.left-side-menu');
        var span = document.getElementById('leftSideFoldSpan');
        if (left) {
          left.classList.toggle('collapsed');
          if (span) span.textContent = left.classList.contains('collapsed') ? ' ≫펴기' : ' 접기';
        var icon = document.getElementById('leftSideFoldIcon');
        if (icon) {
          icon.className = left.classList.contains('collapsed') ? 'bi bi-chevron-right' : 'bi bi-chevron-left';
        }
        }
      });
    }
    // 대메뉴 접기/펼치기 (metismenu 스타일)
    document.querySelectorAll('.side-nav .side-nav-link').forEach(function (a) {
      a.addEventListener('click', function (e) {
        if (this.getAttribute('href') === 'javascript:void(0)' || this.getAttribute('href') === '#') {
          e.preventDefault();
          var parent = this.closest('.side-nav-item');
          var sub = parent && parent.querySelector('.side-nav-second-level');
          if (sub) {
            parent.classList.toggle('mm-active');
            sub.classList.toggle('mm-show');
            this.setAttribute('aria-expanded', sub.classList.contains('mm-show'));
          }
        }
      });
    });
    // 좌측 서브메뉴(.child-li) 클릭 → data-url 기준으로 탭/화면 열기 (공지사항 등 확실히 로드)
    var sideNav = document.querySelector('.side-nav');
    if (sideNav) {
      sideNav.addEventListener('click', function (e) {
        var a = e.target.closest('.child-li a');
        if (!a) return;
        var li = a.closest('.child-li');
        var url = li && li.getAttribute('data-url');
        if (!url) return;
        e.preventDefault();
        fnTopMenuMove(url, a.getAttribute('data-menu_id'), a.textContent.trim());
      });
    }
  });
})();
