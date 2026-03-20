/**
 * PG 솔루션 - fxhj와 동일한 메뉴 구성, 화면은 하나씩 구현
 * fnTopMenuMove(url): 메뉴 클릭 시 탭 추가/전환, 우리 페이지 또는 placeholder 표시
 */

(function () {
  'use strict';

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

  window.SITE_CONFIG = {
    contentBaseUrl: '',
    contentMode: 'placeholder',  // placeholder: 탭에 HTML 직접 삽입 → index.html 모달(parentCompSearchModal 등) 접근 가능
    paymentBaseUrl: ''  // 간편결제 URL 베이스 (예: https://api.example.com) - 비어있으면 현재 origin 사용
  };

  // 메뉴별 URL → 라벨, parent (브레드크럼/탭 제목용) - FXHJ + 본사설정 + 리스크 통합
  var MENU_INFO = {
    '/hq/pgApiMng': { label: 'PG사 API 연동', parent: '본사설정' },
    '/hq/defaultCommission': { label: '기본정책', parent: '본사설정' },
    '/hq/apiConfig': { label: 'API 구성 세팅', parent: '본사설정' },
    '/hq/permissionMng': { label: '본사별 권한 세팅', parent: '본사설정' },
    '/hq/notifyEnv': { label: '전산노티·결제환경', parent: '본사설정' },
    '/hq/accountMng': { label: '계정·업체접근', parent: '본사설정' },
    '/system/noticeList': { label: '공지사항', parent: '업체관리' },
    '/comp/myCompMng': { label: '업체정보조회', parent: '업체관리' },
    '/comp/compMngTree': { label: '업체관리', parent: '업체관리' },
    '/comp/compDetail': { label: '업체정보', parent: '업체관리' },
    '/commission/commisionList': { label: '수수료관리', parent: '업체관리' },
    '/comp/compInfoHistList': { label: '업체변경이력', parent: '업체관리' },
    '/comp/compReg': { label: '업체등록', parent: '업체관리' },
    '/calc/payList': { label: '결제내역', parent: '결제관리' },
    '/calc/payNotiList': { label: '노티내역', parent: '결제관리' },
    '/calc/paySuccessList': { label: '성공내역', parent: '결제관리' },
    '/calc/payFailList': { label: '실패내역', parent: '결제관리' },
    '/calc/payRefundList': { label: '환불내역', parent: '결제관리' },
    '/calc/payForceRefundList': { label: '강제환불', parent: '결제관리' },
    '/calc/payCancelList': { label: '취소내역', parent: '결제관리' },
    '/calc/offsetCancList': { label: '상계취소내역', parent: '결제관리' },
    '/pay/easyPay': { label: 'URL결제내역', parent: '결제관리' },
    '/calc/calcList': { label: '유통망정산내역', parent: '정산관리' },
    '/calc/calcGmList': { label: '가맹정산내역', parent: '정산관리' },
    '/calc/compPointMngList': { label: '환수금관리', parent: '정산관리' },
    '/calc/balcInfo': { label: '잔액/미수금관리', parent: '정산관리' },
    '/calc/balanceList': { label: '잔액내역', parent: '정산관리' },
    '/calc/unpaidMng': { label: '미수금관리', parent: '정산관리' },
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

  /** 가맹점 결제대행사 테이블. opts.rowActionMode=true 이면 업체정보(상세)에서 행별 저장/삭제·2중 확인 */
  function initPgBindingList(pane, initialBindings, opts) {
    opts = opts || {};
    var rowActionMode = !!opts.rowActionMode;
    var getCompId = opts.getCompId || function () { return ''; };

    var tbody = pane.querySelector('#pgBindingTbody');
    var addBtn = pane.querySelector('#pgBindingAddBtn');
    if (!tbody || !addBtn || addBtn._pgBindingInit) return;
    addBtn._pgBindingInit = true;

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
    }

    function wireRow(tr) {
      var delBtn = tr.querySelector('.pg-binding-del');
      var editBtn = tr.querySelector('.pg-binding-edit');
      var saveBtn = tr.querySelector('.pg-binding-save');
      var cancelBtn = tr.querySelector('.pg-binding-cancel');

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
          if (!pgCd) { alert('결제대행사(PG)를 선택하세요. 본사설정 > PG사 API 연동에 먼저 등록해야 목록에 나타납니다.'); return; }
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
      wireRow(tr);
    }

    window.PG_API.pgAgencyList().then(function (list) {
      var pgAgencyOptsHtml = '<option value="">선택</option>';
      (list || []).forEach(function (p) {
        pgAgencyOptsHtml += '<option value="' + (p.pgCd || '') + '">' + (p.pgNm || p.pgCd) + '</option>';
      });
      var bindings = initialBindings || [];
      if (bindings.length > 0) {
        bindings.forEach(function (b, i) {
          addRow(i, {
            id: b.id,
            pgCd: b.pgCd,
            activationYn: b.activationYn || 'Y',
            operationalYn: b.operationalYn || (i === 0 ? 'Y' : 'N'),
            payMethod: b.payMethod || 'WEB',
            mid: b.mid,
            rootNo: b.rootNo,
            apiKey: b.apiKey,
            ivKey: b.ivKey,
            installmentYn: b.installmentYn || 'N',
            maxInstallmentMonths: b.maxInstallmentMonths != null ? String(b.maxInstallmentMonths) : ''
          }, pgAgencyOptsHtml);
        });
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
    var idEl = document.getElementById('pgAgencyEditId');
    var cdEl = document.getElementById('pgAgencyEditPgCd');
    var nmEl = document.getElementById('pgAgencyEditPgNm');
    var epEl = document.getElementById('pgAgencyEditEndpoint');
    var uyEl = document.getElementById('pgAgencyEditUseYn');
    if (!idEl || !cdEl || !nmEl) return;
    idEl.value = preset.id != null ? String(preset.id) : '';
    cdEl.value = preset.pgCd || '';
    nmEl.value = preset.pgNm || '';
    if (epEl) epEl.value = preset.apiEndpoint || '';
    if (uyEl) uyEl.value = (preset.useYn === 'N') ? 'N' : 'Y';
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
      transferType: 1, calcCycle: 1, calcExcludeYn: 1, payHoldYn: 1, useYn: 1, compDivNm: 1
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

  function bindScreenEvents(pane, tabId) {
    if (!pane) return;
    function syncColumnGuideUiState() {
      pane.querySelectorAll('.column-guide-item').forEach(function (item) {
        var cb = item.querySelector('.column-guide-check');
        var on = !!(cb && cb.checked);
        item.classList.toggle('column-guide-item--on', on);
        item.classList.toggle('column-guide-item--off', !on);
      });
    }
    function getSelectedGuideKeys() {
      var keys = [];
      pane.querySelectorAll('.column-guide-check:checked').forEach(function (cb) {
        var k = cb.getAttribute('data-key');
        if (k) keys.push(k);
      });
      return keys;
    }
    function applySelectedGuideKeys(keys) {
      var set = {};
      (keys || []).forEach(function (k) { set[k] = 1; });
      var hasAny = (keys || []).length > 0;
      pane.querySelectorAll('.column-guide-check').forEach(function (cb) {
        var k = cb.getAttribute('data-key') || '';
        cb.checked = hasAny ? !!set[k] : false;
      });
      pane._selectedColumns = hasAny ? (keys || []) : null;
      syncColumnGuideUiState();
    }
    function loadViewSetting() {
      var hasGuide = !!pane.querySelector('.column-guide-check');
      if (!hasGuide || !window.PG_API || !window.PG_API.userViewSetting) return Promise.resolve();
      return window.PG_API.userViewSetting(url).then(function (data) {
        if (!data || data.hasSetting !== true) {
          pane._selectedColumns = null;
          syncColumnGuideUiState();
          return;
        }
        var json = data && data.selectedKeysJson ? String(data.selectedKeysJson) : '[]';
        var keys = [];
        try { keys = JSON.parse(json); } catch (e) { keys = []; }
        if (!Array.isArray(keys)) keys = [];
        applySelectedGuideKeys(keys);
      }).catch(function () {
        syncColumnGuideUiState();
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
      if (p && p.classList) {
        p.classList.toggle('screen-calc-gm-list', url === '/calc/calcGmList' || url === '/settlement/franchiseList');
        p.classList.toggle('screen-pay-list', url === '/calc/payList' || url === '/calc/payNotiList' || url === '/calc/paySuccessList' || url === '/calc/payFailList' || url === '/calc/payRefundList' || url === '/calc/payForceRefundList' || url === '/calc/payCancelList' || url === '/calc/offsetCancList' || url === '/pay/easyPay');
      }
      var cfg = window.PG_SCREENS && window.PG_SCREENS.getMenuScreens && window.PG_SCREENS.getMenuScreens()[url];
      if (!cfg || !cfg.columns) return;
      var params = collectSearchParams(p);
      if (pageOverride) params.page = pageOverride;
      if (cfg.payListVariant) params.payListVariant = cfg.payListVariant;
      p.setAttribute('data-last-url', url);
      p.setAttribute('data-last-page', String(params.page));
      var dimm = document.getElementById('dimm');
      if (dimm) dimm.style.display = 'flex';
      var api = window.PG_API;
      var promise = null;
      if (url === '/system/noticeList') promise = api.noticeList(params);
      else if (url === '/calc/payList' || url === '/calc/payFailList' || url === '/calc/offsetCancList' || url === '/pay/easyPay' || url === '/calc/payNotiList' || url === '/calc/paySuccessList' || url === '/calc/payRefundList' || url === '/calc/payForceRefundList' || url === '/calc/payCancelList') promise = api.payList(params);
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
      else if (url === '/calc/balanceList' || url === '/settlement/balanceList') promise = api.settlementBalanceList(params);
      else if (url === '/calc/unpaidMng' || url === '/settlement/unpaidMng') promise = api.settlementUnpaidMng(params);
      else if (url === '/pay/payHoldList' || url === '/settlement/holdList') promise = api.settlementHoldList(params);
      else if (url === '/calc/exCalcList' || url === '/settlement/execute') promise = api.settlementExecute(params);
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
        var selCols = p._selectedColumns;
        var fixedKeys = ['rowNo', 'compNm', 'compId', 'trnDate', 'trnTime', 'routeNo', 'chillTransactionId', 'compDivNm', 'merchantNm'];
        var cols = allCols.filter(function (c) {
          if (c.type === 'checkbox' || fixedKeys.indexOf(c.key) !== -1) return true;
          if (!selCols || selCols.length === 0) return true;
          return selCols.indexOf(c.key) !== -1;
        });
        p._lastGridList = list;
        p._lastGridCols = cols;
        var thead = p.querySelector('#grid_' + tid + ' thead');
        if (thead) {
          var groups = cfg.headerGroups || [];
          if (!groups.length) {
            var theadTr = thead.querySelector('tr') || document.createElement('tr');
            theadTr.innerHTML = cols.map(function (c) {
              if (c.type === 'checkbox') return '<th data-key="_chk" style="width:40px"><input type="checkbox" class="grid-check-all" title="전체선택"></th>';
              return '<th data-key="' + (c.key || '') + '">' + (c.label || c.key) + '</th>';
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
          var pageSize = data && data.size != null ? parseInt(data.size, 10) : 20;
          if (isNaN(pageSize) || pageSize < 1) pageSize = 20;
          var rowNoBase = (pageNum - 1) * pageSize;
          list.forEach(function (row, idx) {
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
            html += '<tr' + (trExtraClass ? ' class="' + trExtraClass.trim() + '"' : '') + ' data-id="' + (rowId || '') + '" data-parent-id="' + (parentId || '') + '" data-row="' + (isCompMngTree ? encodeURIComponent(JSON.stringify(row)) : '') + '">';
            cols.forEach(function (c) {
              if (c.type === 'checkbox') html += '<td><input type="checkbox" class="grid-row-check"></td>';
              else if (c.type === 'payActions') {
                var seq = row.paySeq != null ? String(row.paySeq) : '';
                var esc = function (s) {
                  return String(s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
                };
                var es = esc(seq);
                html += '<td class="small text-nowrap pay-actions-cell">' +
                  '<button type="button" class="btn btn-link btn-sm p-0 pay-follow" data-act="AUTO_VOID" data-trn="' + es + '">자동무효</button> ' +
                  '<button type="button" class="btn btn-link btn-sm p-0 pay-follow" data-act="EMAIL_VOID" data-trn="' + es + '">이메일무효</button> ' +
                  '<button type="button" class="btn btn-link btn-sm p-0 pay-follow" data-act="AUTO_REFUND" data-trn="' + es + '">자동환불</button> ' +
                  '<button type="button" class="btn btn-link btn-sm p-0 pay-follow" data-act="FORCE_REFUND" data-trn="' + es + '">강제환불</button></td>';
              } else if (c.type === 'accountAccessDelete') {
                var delId = row.id != null ? String(row.id) : '';
                html += '<td><button type="button" class="btn btn-link btn-sm p-0 text-danger hq-acc-del" data-id="' + delId + '">삭제</button></td>';
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
              } else {
                var val = row[c.key] !== undefined && row[c.key] !== null ? String(row[c.key]) : '';
                var cellClass = '';
                var isPayScr = url === '/calc/payList' || url === '/calc/payNotiList' || url === '/calc/paySuccessList' || url === '/calc/payFailList' || url === '/calc/payRefundList' || url === '/calc/payForceRefundList' || url === '/calc/payCancelList' || url === '/calc/offsetCancList' || url === '/pay/easyPay';
                if (url === '/calc/calcGmList' || url === '/settlement/franchiseList') {
                  if (['amount', 'feeCnt', 'feeRate', 'feeAmt', 'feeVat', 'holdRate', 'holdAmt', 'settleAmt'].indexOf(c.key) >= 0) cellClass = ' class="text-end"';
                  if (['calcDt', 'approveDt', 'cancelDt'].indexOf(c.key) >= 0) cellClass = ' class="text-nowrap"';
                } else if (isPayScr) {
                  var payCls = [];
                  if (['pgApproveAmt', 'payAmount', 'feeCnt', 'feeRate', 'feeAmt', 'feeVat', 'holdRate', 'holdAmt', 'settleAmt', 'chillAmount', 'icopayAmt', 'chillFeeAmt', 'totalAmt'].indexOf(c.key) >= 0) payCls.push('text-end');
                  if (['payAprv', 'holdDttm', 'calcDt', 'payDttm', 'trnDate', 'trnTime', 'payCompletedAt', 'trnId', 'chillTransactionId', 'routeNo'].indexOf(c.key) >= 0) payCls.push('text-nowrap');
                  if (['compNm', 'merchantNm', 'compDivCode9', 'chillCustomer', 'productNm'].indexOf(c.key) >= 0) payCls.push('text-start');
                  if (payCls.length) cellClass = ' class="' + payCls.join(' ') + '"';
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
                  var folderSvg = '<svg class="tree-icon tree-icon-folder" width="16" height="16" viewBox="0 0 24 24"><path fill="currentColor" d="M10 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"/></svg>';
                  var docSvg = '<svg class="tree-icon tree-icon-doc" width="16" height="16" viewBox="0 0 24 24"><path fill="currentColor" d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6z"/></svg>';
                  var icon = hasChildren ? folderSvg : docSvg;
                  var toggle = hasChildren ? '<span class="tree-toggle ' + (expanded ? 'expanded' : 'collapsed') + '" data-id="' + rowId + '" title="' + (expanded ? '접기' : '펼치기') + '">' + (expanded ? '\u25BC' : '\u25B6') + '</span>' : '<span class="tree-toggle-placeholder"></span>';
                  html += '<td class="tree-comp-cell" style="padding-left:' + px + 'px">' + icon + toggle + (val || '') + '</td>';
                } else if (isCompMngTree && c.key === 'compNm') {
                  html += '<td' + cellClass + '>' + (val || '') + '</td>';
                } else html += '<td' + cellClass + '>' + val + '</td>';
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
        if (url === '/calc/calcGmList' || url === '/settlement/franchiseList') {
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
        if (url === '/calc/payList' || url === '/calc/payNotiList' || url === '/calc/paySuccessList' || url === '/calc/payFailList' || url === '/calc/payRefundList' || url === '/calc/payForceRefundList' || url === '/calc/payCancelList' || url === '/calc/offsetCancList' || url === '/pay/easyPay') {
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
        p._lastGridList = [];
        p._lastGridCols = null;
        var tbody = p.querySelector('#grid_' + tid + ' tbody');
        if (tbody) tbody.innerHTML = '<tr><td colspan="' + (cfg.columns.length) + '" class="empty-state-cell text-center text-danger">' + (err && err.message ? err.message : '조회 실패') + '</td></tr>';
      }).finally(function () {
        if (dimm) dimm.style.display = 'none';
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
        '<div class="table-responsive"><table class="table table-sm table-bordered mb-0"><thead><tr><th style="width:90px">선택</th><th>대상명</th><th>URL</th></tr></thead><tbody id="notifyTargetPickerTbody"></tbody></table></div>' +
        '</div></div></div></div>';
      document.body.appendChild(wrap.firstChild);
      return document.getElementById('notifyTargetPickerModal');
    }
    function bindNotifyTargetPicker() {
      var btns = pane.querySelectorAll('button[data-action="노티선택"]');
      if (!btns || btns.length === 0 || !window.PG_API || !window.PG_API.hqNotifyTargets) return;
      btns.forEach(function (btn) {
        if (btn._notifyPickerBound) return;
        btn._notifyPickerBound = true;
        btn.addEventListener('click', function () {
          var field = btn.getAttribute('data-field') || '';
          var selectEl = pane.querySelector('[name="' + field + '"][data-load-notify-targets="true"]');
          if (!selectEl) return;
          var modalEl = ensureNotifyTargetPickerModal();
          var keywordEl = modalEl.querySelector('#notifyTargetKeyword');
          var searchBtn = modalEl.querySelector('#notifyTargetSearchBtn');
          var tbody = modalEl.querySelector('#notifyTargetPickerTbody');
          function render(list) {
            var kw = keywordEl && keywordEl.value ? String(keywordEl.value).trim().toLowerCase() : '';
            var rows = (list || []).filter(function (t) {
              if (!kw) return true;
              return String(t.targetName || '').toLowerCase().indexOf(kw) >= 0 || String(t.targetUrl || '').toLowerCase().indexOf(kw) >= 0;
            });
            tbody.innerHTML = '';
            if (rows.length === 0) {
              tbody.innerHTML = '<tr><td colspan="3" class="text-center text-muted">조회 결과가 없습니다.</td></tr>';
              return;
            }
            rows.forEach(function (t) {
              var tr = document.createElement('tr');
              tr.innerHTML = '<td><button type="button" class="btn btn-sm btn-outline-primary">선택</button></td><td>' + (t.targetName || t.targetCode || '') + '</td><td>' + (t.targetUrl || '') + '</td>';
              tr.querySelector('button').addEventListener('click', function () {
                selectEl.value = t.targetUrl || '';
                if (window.bootstrap && bootstrap.Modal) {
                  var mm = bootstrap.Modal.getInstance(modalEl);
                  if (mm) mm.hide();
                }
              });
              tbody.appendChild(tr);
            });
          }
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.hqNotifyTargets().then(function (list) {
            render(list || []);
            if (searchBtn && !searchBtn._bound) {
              searchBtn._bound = true;
              searchBtn.addEventListener('click', function () {
                window.PG_API.hqNotifyTargets().then(function (list2) { render(list2 || []); });
              });
            }
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
            html += '<option value="' + (t.targetUrl || '') + '">' + (t.targetName || t.targetCode || '노티') + ' - ' + (t.targetUrl || '') + '</option>';
          });
          sel.innerHTML = html;
          if (current) sel.value = current;
        });
      }).catch(function () {});
    }
    bindNotifyTargetPicker();
    var autoSearchUrls = ['/system/noticeList', '/calc/payList', '/calc/payNotiList', '/calc/paySuccessList', '/calc/payFailList', '/calc/payRefundList', '/calc/payForceRefundList', '/calc/payCancelList', '/calc/offsetCancList', '/pay/easyPay',
      '/comp/compMngTree', '/comp/compInfoHistList', '/commission/commisionList',
      '/user/userMng', '/set/gridSetMng',
      '/calc/calcList', '/calc/calcGmList', '/calc/compPointMngList', '/calc/balcInfo', '/calc/balanceList', '/calc/unpaidMng', '/calc/exCalcList', '/pay/payHoldList',
      '/noti/notiUrlMng', '/noti/notiSendMngList', '/noti/notiCashReceiptUrlMng', '/noti/notiCashReceiptSendMngList',
      '/hq/pgApiMng', '/hq/permissionMng', '/hq/accountMng', '/risk/list'];
    if (autoSearchUrls.indexOf(url) !== -1) {
      setTimeout(function () {
        if (window.PG_LAST_REGISTERED_COMP && (url === '/commission/commisionList' || url === '/comp/compMngTree')) {
          var sid = pane.querySelector('input[name="searchCompId"]');
          if (sid) sid.value = window.PG_LAST_REGISTERED_COMP;
          window.PG_LAST_REGISTERED_COMP = null;
        }
        loadViewSetting().finally(function () {
          doSearch(pane, tabId, 1);
        });
      }, 100);
    }
    if (url === '/commission/commisionList') {
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
            set('commissionHqRate', data.hqRate);
            set('commissionRegionalRate', data.regionalRate);
            set('commissionMasterRate', data.masterRate);
            set('commissionBranchRate', data.branchRate);
            set('commissionAgencyRate', data.agencyRate);
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
    if (compMngSaveColumnsBtn && !compMngSaveColumnsBtn._bound) {
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
          doSearch(pane, tabId, 1);
        }).catch(function (e) {
          alert(e && e.message ? e.message : 'VIEW SETTING 저장 실패');
        }).finally(function () {
          if (dimm) dimm.style.display = 'none';
        });
      });
    }
    var compMngClearColumnsBtn = pane.querySelector('#compMngClearColumnsBtn');
    if (compMngClearColumnsBtn && !compMngClearColumnsBtn._bound) {
      compMngClearColumnsBtn._bound = true;
      compMngClearColumnsBtn.addEventListener('click', function () {
        if (!confirm('모든 칼럼 선택을 해제하시겠습니까?')) return;
        applySelectedGuideKeys([]);
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.userViewSettingSave(url, '[]').then(function () {
          doSearch(pane, tabId, 1);
        }).catch(function (e) {
          alert(e && e.message ? e.message : 'VIEW SETTING 저장 실패');
        }).finally(function () {
          if (dimm) dimm.style.display = 'none';
        });
      });
    }
    var seedBtn = pane.querySelector('#seedBtn');
    if (seedBtn && url === '/comp/compMngTree') {
      seedBtn.addEventListener('click', function () {
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.seedDev().then(function (r) {
          var msg = (r && r.data && r.data.message) ? r.data.message : (r && r.message) ? r.message : '시드 생성 완료';
          if (r && r.success === false) alert(msg);
          else { alert(msg); doSearch(pane, tabId, 1); }
        }).catch(function (e) { alert(e && e.message ? e.message : '시드 생성 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
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
        var form = pane.querySelector('#compRegForm');
        if (!form) return;
        var fd = {};
        form.querySelectorAll('input, select, textarea').forEach(function (el) {
          if (el.name && el.type !== 'file' && el.name !== 'pgOperational') {
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
        var checkedId = form.getAttribute('data-login-id-checked') || '';
        if (checkedId !== String(fd.loginId).trim()) {
          alert('로그인ID 중복확인을 먼저 진행하세요.');
          return;
        }
        var needsParent = ['MASTER_DIST', 'BRANCH', 'AGENCY', 'SALES_OFFICE', 'MERCHANT'].indexOf((fd.compDiv || '').toUpperCase()) >= 0;
        if (needsParent && (!fd.parentId || !String(fd.parentId).trim())) { alert('상위 지점을 선택하세요. [검색] 버튼으로 상위업체를 선택해주세요.'); return; }
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
          var regionalKeys = ['remitterName', 'balanceNotifyAmt', 'suspiciousNotifyAmt', 'overseasLoginNotifyAmt', 'tempPwdNotifyAmt', 'nonTranCriterionMonth', 'sameCardLimitWebDay', 'sameCardLimitWebTimes', 'sameCardLimitWebAmt', 'sameCardLimitTerminalDay', 'sameCardLimitTerminalTimes', 'sameCardLimitTerminalAmt', 'dailyUsageFee', 'depositNameLookup', 'transferAuthNo', 'autoConvertNewMemberLimit', 'newMemberDailyLimit', 'convertRefDate', 'convertDailyLimit', 'applyStartDate', 'pgFeeGeneral', 'transferFee', 'settleDiffMonthCnt', 'settleReportBankCd', 'pgFeeSamsung', 'smsFee', 'taxInvoiceEmail', 'settleAccountNo', 'directFee', 'solutionFee', 'settleAccountHolder', 'withdrawRestrictType', 'withdrawRestrictStartTime', 'withdrawRestrictEndTime', 'terminalPayRestrict', 'webPayRestrict', 'defaultFeeHq', 'defaultFeeDist', 'defaultFeeBranch', 'defaultFeeAgency', 'defaultFeeSalesOffice', 'defaultPayLimitPerTx', 'defaultPayLimitDay', 'defaultPayLimitMonth', 'defaultPayLimitYearCorp', 'defaultPayLimitYearInd', 'copyright', 'regionalCardLimits', 'regionalTerminals'];
          var regionalSettings = {};
          regionalKeys.forEach(function (k) { if (fd[k] !== undefined && fd[k] !== null && fd[k] !== '') regionalSettings[k] = fd[k]; });
          fd.regionalSettings = JSON.stringify(regionalSettings);
        }
        if (fd.compDiv === 'MASTER_DIST') {
          var n1 = String(fd.notifyUrl1 || '').trim();
          var hasBackup = !!(String(fd.notifyUrl2 || '').trim() || String(fd.notifyUrl3 || '').trim() || String(fd.notifyUrl4 || '').trim());
          if (!n1) { alert('총판은 노티 URL 1(기본)을 입력해야 합니다.'); return; }
          if (hasBackup && !n1) { alert('노티 URL 2~4를 사용할 때는 노티 URL 1(기본)이 필수입니다.'); return; }
        }
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        var mainFile = form.querySelector('#brandingMainImageFile');
        var logoFile = form.querySelector('#brandingLogoImageFile');
        var themeEl = form.querySelector('#brandingTheme');
        var isRegOrMaster = (fd.compDiv === 'REGIONAL' || fd.compDiv === 'MASTER_DIST');
        window.PG_API.compRegister(fd).then(function (res) {
          var data = res && res.data ? res.data : res;
          var compId = data && data.compId ? data.compId : '';
          if (compId && isRegOrMaster && window.PG_API.orgBrandingUpload && window.PG_API.orgBrandingSave) {
            var chain = Promise.resolve();
            if (mainFile && mainFile.files && mainFile.files[0]) {
              chain = chain.then(function () { return window.PG_API.orgBrandingUpload(compId, 'main', mainFile.files[0]); });
            }
            if (logoFile && logoFile.files && logoFile.files[0]) {
              chain = chain.then(function () { return window.PG_API.orgBrandingUpload(compId, 'logo', logoFile.files[0]); });
            }
            if (themeEl && themeEl.value) {
              chain = chain.then(function () { return window.PG_API.orgBrandingSave(compId, themeEl.value); });
            }
            return chain.then(function () { return res; });
          }
          return res;
        }).then(function (res) {
          var data = res && res.data ? res.data : res;
          if (data && data.compId) window.PG_LAST_REGISTERED_COMP = data.compId;
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
    if (url === '/comp/compReg') {
      initBankByCountry(pane);
      initCountryBankGroup(pane);
      initCountryAddressGroup(pane);
      var form = pane.querySelector('#compRegForm');
      var loginIdElForCheck = form ? form.querySelector('[name="loginId"]') : null;
      if (loginIdElForCheck && !loginIdElForCheck._dupResetBound) {
        loginIdElForCheck._dupResetBound = true;
        loginIdElForCheck.addEventListener('input', function () {
          if (form) form.setAttribute('data-login-id-checked', '');
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
        initRegionalCardLimitTable(pane);
        initRegionalTerminalTable(pane);
        var mainBrowse = pane.querySelector('#brandingMainImageBrowse');
        var logoBrowse = pane.querySelector('#brandingLogoImageBrowse');
        if (mainBrowse) mainBrowse.addEventListener('click', function () { var f = pane.querySelector('#brandingMainImageFile'); if (f) f.click(); });
        if (logoBrowse) logoBrowse.addEventListener('click', function () { var f = pane.querySelector('#brandingLogoImageFile'); if (f) f.click(); });
        [pane.querySelector('#brandingMainImageFile'), pane.querySelector('#brandingLogoImageFile')].forEach(function (inp) {
          if (inp) inp.addEventListener('change', function () {
            var urlInp = this.id === 'brandingMainImageFile' ? pane.querySelector('#brandingMainImageUrl') : pane.querySelector('#brandingLogoImageUrl');
            if (urlInp && this.files && this.files[0]) urlInp.value = this.files[0].name + ' (업로드 예정)';
          });
        });
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
      var hqPolicySel = pane.querySelector('[name="hqPolicyScope"]');
      if (hqPolicySel && !hqPolicySel._loaded) {
        hqPolicySel._loaded = true;
        window.PG_API.hqDefaultCommission().then(function (data) {
          var list = (data && data.templates) ? data.templates : [];
          var opts = '<option value="">기본(DEFAULT)</option>';
          list.forEach(function (t) {
            var scope = t.scope || '';
            var name = t.policyName || scope;
            opts += '<option value="' + scope + '">' + name + '</option>';
          });
          hqPolicySel.innerHTML = opts;
          if (data && data.deployedTemplateScope) hqPolicySel.value = data.deployedTemplateScope;
        }).catch(function () {});
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
            var val = (compId || '') + (compNm ? ' (' + compNm + ')' : '');
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
              }
            });
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
                    var id = tr.getAttribute('data-id') || '';
                    var val = (tr.getAttribute('data-compId') || '') + (tr.getAttribute('data-compNm') ? ' (' + tr.getAttribute('data-compNm') + ')' : '');
                    ['#compRegForm', '#compDetailForm'].forEach(function (formId) {
                      var form = document.querySelector(formId);
                      if (form) {
                        var pid = form.querySelector('input[name="parentId"]');
                        if (pid) pid.value = id;
                        else { var hid = document.createElement('input'); hid.type = 'hidden'; hid.name = 'parentId'; hid.value = id; form.appendChild(hid); }
                        var pc = form.querySelector('input[name="parentComp"]');
                        if (pc) pc.value = val;
                      }
                    });
                    if (modalEl && window.bootstrap && bootstrap.Modal) { var m = bootstrap.Modal.getInstance(modalEl); if (m) m.hide(); }
                  });
                  tr.querySelector('button').addEventListener('click', function () {
                    var id = tr.getAttribute('data-id') || '';
                    var val = (tr.getAttribute('data-compId') || '') + (tr.getAttribute('data-compNm') ? ' (' + tr.getAttribute('data-compNm') + ')' : '');
                    ['#compRegForm', '#compDetailForm'].forEach(function (formId) {
                      var form = document.querySelector(formId);
                      if (form) {
                        var pid = form.querySelector('input[name="parentId"]');
                        if (pid) pid.value = id;
                        else { var hid = document.createElement('input'); hid.type = 'hidden'; hid.name = 'parentId'; hid.value = id; form.appendChild(hid); }
                        var pc = form.querySelector('input[name="parentComp"]');
                        if (pc) pc.value = val;
                      }
                    });
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
    function applyCompInfoHeadquartersVisibility(form, compDiv) {
      if (!form) return;
      var hide = compDiv === 'HEADQUARTERS';
      form.querySelectorAll('.comp-info-hide-if-hq').forEach(function (el) {
        el.style.display = hide ? 'none' : '';
      });
    }
    function applyMyCompBrandingPermission(pane, form, compDiv, brandingEditAllowedYn) {
      if (!pane || !form) return;
      var isHeadquarters = compDiv === 'HEADQUARTERS';
      var allowedComp = (isHeadquarters || compDiv === 'REGIONAL' || compDiv === 'MASTER_DIST');
      var allowed = isHeadquarters || (allowedComp && String(brandingEditAllowedYn || '').toUpperCase() === 'Y');
      var brandingCard = pane.querySelector('#brandingCard');
      if (brandingCard) brandingCard.style.display = allowedComp ? '' : 'none';
      var mainBrowse = pane.querySelector('#brandingMainImageBrowse');
      var logoBrowse = pane.querySelector('#brandingLogoImageBrowse');
      var mainFile = pane.querySelector('#brandingMainImageFile');
      var logoFile = pane.querySelector('#brandingLogoImageFile');
      var themeSel = pane.querySelector('#brandingTheme');
      [mainBrowse, logoBrowse].forEach(function (btn) {
        if (!btn) return;
        btn.style.display = allowed ? '' : 'none';
        btn.disabled = !allowed;
      });
      [mainFile, logoFile, themeSel].forEach(function (el) {
        if (!el) return;
        el.disabled = !allowed;
      });
    }
    function loadCompDetailIntoForm(pane, compId) {
      if (!compId) return;
      var dimm = document.getElementById('dimm');
      if (dimm) dimm.style.display = 'flex';
      window.PG_API.compDetail(compId).then(function (data) {
        var form = pane.querySelector('#compInfoDetailForm');
        if (!form || !data) return;
        ['compId', 'compNm', 'compDiv', 'regNo', 'bizType', 'industry', 'bizNature', 'product', 'homepage', 'settleName', 'settleTelNo', 'ceoNm', 'ceoMobile', 'compTel', 'fax', 'zipCode', 'addr', 'addrDetail', 'addrEtc', 'addrCountryCd', 'addrCountryCdOther', 'email', 'useYn', 'loginId', 'pwd', 'assistantLoginId', 'assistantPwd', 'assistantRoleType', 'brandingEditAllowedYn', 'bankCd', 'transferFee', 'cryptoTransferFee', 'accountNo', 'accountHolder', 'commissionConfigAllowed', 'webPaymentUseYn', 'baseCurrency', 'remark', 'siteUrl', 'siteSummary'].forEach(function (k) {
          var el = form.querySelector('[name="' + k + '"]');
          if (el && data[k] != null) el.value = data[k];
        });
        form.setAttribute('data-login-id-checked', data.loginId ? String(data.loginId).trim() : '');
        form.setAttribute('data-assistant-login-id-checked', data.assistantLoginId ? String(data.assistantLoginId).trim() : '');
        if ((data.compDiv === 'REGIONAL' || data.compDiv === 'MASTER_DIST') && data.baseCurrency) {
          var parts = data.compDiv === 'REGIONAL' ? String(data.baseCurrency).split(/,\s*/) : [data.baseCurrency, '', ''];
          ['baseCurrency1', 'baseCurrency2', 'baseCurrency3'].forEach(function (n, i) {
            var el = form.querySelector('[name="' + n + '"]');
            if (el) el.value = parts[i] || '';
          });
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
        var formUrl = pane.getAttribute('formurl') || '';
        var compDivSel = form.querySelector('[name="compDiv"]');
        if (formUrl === '/comp/myCompMng' && compDivSel) {
          compDivSel.disabled = true;
        }
        if (formUrl === '/comp/myCompMng') {
          applyMyCompBrandingPermission(pane, form, data.compDiv || '', data.brandingEditAllowedYn || 'N');
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
        applyCompInfoHeadquartersVisibility(form, data.compDiv || '');
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
      initCountryAddressGroup(pane);
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
            fd.baseCurrency = (fd.baseCurrency1 || '').trim();
          }
          delete fd.baseCurrency1;
          delete fd.baseCurrency2;
          delete fd.baseCurrency3;
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.compUpdate(fd).then(function () {
            alert('저장되었습니다.');
            if (url === '/comp/myCompMng' && compId) {
              loadCompDetailIntoForm(pane, compId);
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
            });
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
        var allFields = ['compId', 'parentComp', 'compNm', 'compDiv', 'regNo', 'bizType', 'industry', 'bizNature', 'product', 'homepage', 'settleName', 'settleTelNo', 'ceoNm', 'ceoMobile', 'compTel', 'fax', 'zipCode', 'addr', 'addrDetail', 'addrEtc', 'addrCountryCd', 'addrCountryCdOther', 'email', 'siteUrl', 'siteSummary', 'useYn', 'loginId', 'bankCd', 'transferFee', 'cryptoTransferFee', 'accountNo', 'accountHolder', 'commissionConfigAllowed', 'webPaymentUseYn', 'baseCurrency', 'remark', 'settleType', 'commissionRate', 'limitAmt', 'countryCd', 'countryCdOther', 'swift', 'branchName', 'branchAddr', 'contactTel', 'walletAddress', 'networkName', 'withdrawLimitDays', 'withdrawStartTime', 'withdrawEndTime', 'payLimitDefault', 'payLimitExtra', 'payLimitAlertSms', 'holdRateFollowHq', 'holdRate', 'holdDays', 'commissionFollowHq', 'hqPolicyScope', 'failFee', 'usageRate', 'payRate', 'cancelRate', 'refundRate', 'commissionMemo', 'feeSettlementPerTx', 'feeUsdt', 'feeFx', 'calcCycle', 'calcCloseTime', 'transferType', 'transferCycleDays', 'autoTransferMin', 'calcExcludeYn', 'calcExcludeTarget', 'calcStartTime', 'payHoldYn', 'defaultProductName', 'defaultProductCode', 'defaultProductAmount', 'defaultProductDesc', 'notifyUrlBackground', 'notifyUrlResult', 'notifyUrl1', 'notifyUrl2', 'notifyUrl3', 'notifyUrl4', 'remitterName', 'balanceNotifyAmt', 'suspiciousNotifyAmt', 'overseasLoginNotifyAmt', 'tempPwdNotifyAmt', 'nonTranCriterionMonth', 'sameCardLimitWebDay', 'sameCardLimitWebTimes', 'sameCardLimitWebAmt', 'sameCardLimitTerminalDay', 'sameCardLimitTerminalTimes', 'sameCardLimitTerminalAmt', 'dailyUsageFee', 'depositNameLookup', 'transferAuthNo', 'autoConvertNewMemberLimit', 'newMemberDailyLimit', 'convertRefDate', 'convertDailyLimit', 'applyStartDate', 'pgFeeGeneral', 'settleDiffMonthCnt', 'settleReportBankCd', 'pgFeeSamsung', 'smsFee', 'taxInvoiceEmail', 'settleAccountNo', 'directFee', 'solutionFee', 'settleAccountHolder', 'withdrawRestrictType', 'withdrawRestrictStartTime', 'withdrawRestrictEndTime', 'terminalPayRestrict', 'webPayRestrict', 'defaultFeeHq', 'defaultFeeDist', 'defaultFeeBranch', 'defaultFeeAgency', 'defaultFeeSalesOffice', 'defaultPayLimitPerTx', 'defaultPayLimitDay', 'defaultPayLimitMonth', 'defaultPayLimitYearCorp', 'defaultPayLimitYearInd', 'copyright'];
        allFields.forEach(function (k) {
          var el = form.querySelector('[name="' + k + '"]');
          if (el && data[k] != null) el.value = data[k];
        });
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
        initPgBindingList(pane, data.pgBindings, {
          rowActionMode: true,
          getCompId: function () {
            var el = pane.querySelector('#compDetailForm [name="compId"]');
            return el && el.value ? el.value.trim() : '';
          }
        });
        initRegionalCardLimitTable(pane, data.regionalCardLimits || []);
        initRegionalTerminalTable(pane, data.regionalTerminals || []);
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
        initCountryBankGroup(pane);
        initCountryAddressGroup(pane);
        if ((apiCompDiv === 'HEADQUARTERS' || apiCompDiv === 'REGIONAL' || apiCompDiv === 'MASTER_DIST') && window.PG_API.orgBranding) {
          window.PG_API.orgBranding(compId).then(function (b) {
            var mainUrl = pane.querySelector('#brandingMainImageUrl');
            var logoUrl = pane.querySelector('#brandingLogoImageUrl');
            var themeSel = pane.querySelector('#brandingTheme');
            if (mainUrl && b.mainImageUrl) mainUrl.value = b.mainImageUrl;
            if (logoUrl && b.logoImageUrl) logoUrl.value = b.logoImageUrl;
            if (themeSel && b.theme) themeSel.value = b.theme || 'DEFAULT';
          }).catch(function () {});
        }
        var mainBrowse = pane.querySelector('#brandingMainImageBrowse');
        var logoBrowse = pane.querySelector('#brandingLogoImageBrowse');
        if (mainBrowse) mainBrowse.addEventListener('click', function () { var f = pane.querySelector('#brandingMainImageFile'); if (f) f.click(); });
        if (logoBrowse) logoBrowse.addEventListener('click', function () { var f = pane.querySelector('#brandingLogoImageFile'); if (f) f.click(); });
        [pane.querySelector('#brandingMainImageFile'), pane.querySelector('#brandingLogoImageFile')].forEach(function (inp) {
          if (inp) inp.addEventListener('change', function () {
            var urlInp = this.id === 'brandingMainImageFile' ? pane.querySelector('#brandingMainImageUrl') : pane.querySelector('#brandingLogoImageUrl');
            if (urlInp && this.files && this.files[0]) urlInp.value = this.files[0].name + ' (업로드 예정)';
          });
        });
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
          if (fd.countryCd === 'OTHER') { fd.bankCd = fd.bankCdText || fd.bankCd; delete fd.bankCdText; }
          if (fd.addrCountryCd === 'OTHER') { fd.addrCountryCd = fd.addrCountryCdOther || ''; delete fd.addrCountryCdOther; }
          fd.compId = compId;
          if (fd.parentComp && fd.parentComp.indexOf(' (') > 0) fd.parentComp = fd.parentComp.split(' (')[0].trim();
          if (fd.regType != null) { fd.regNo = (fd.regType || 'CORP') + '|' + (fd.regNo || ''); delete fd.regType; }
          if (form.id !== 'compDetailForm') {
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
          }
          var compDivVal = form.querySelector('[name="compDiv"]') ? form.querySelector('[name="compDiv"]').value : '';
          if (compDivVal === 'MASTER_DIST') {
            var dn1 = String(fd.notifyUrl1 || '').trim();
            var dHasBackup = !!(String(fd.notifyUrl2 || '').trim() || String(fd.notifyUrl3 || '').trim() || String(fd.notifyUrl4 || '').trim());
            if (!dn1) { alert('총판은 노티 URL 1(기본)을 입력해야 합니다.'); return; }
            if (dHasBackup && !dn1) { alert('노티 URL 2~4를 사용할 때는 노티 URL 1(기본)이 필수입니다.'); return; }
          }
          if (compDivVal === 'REGIONAL') {
            var bc = [fd.baseCurrency1, fd.baseCurrency2, fd.baseCurrency3].filter(function (v) { return v && v.trim(); });
            fd.baseCurrency = bc.join(',');
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
            var regionalKeys = ['remitterName', 'balanceNotifyAmt', 'suspiciousNotifyAmt', 'overseasLoginNotifyAmt', 'tempPwdNotifyAmt', 'nonTranCriterionMonth', 'sameCardLimitWebDay', 'sameCardLimitWebTimes', 'sameCardLimitWebAmt', 'sameCardLimitTerminalDay', 'sameCardLimitTerminalTimes', 'sameCardLimitTerminalAmt', 'dailyUsageFee', 'depositNameLookup', 'transferAuthNo', 'autoConvertNewMemberLimit', 'newMemberDailyLimit', 'convertRefDate', 'convertDailyLimit', 'applyStartDate', 'pgFeeGeneral', 'transferFee', 'settleDiffMonthCnt', 'settleReportBankCd', 'pgFeeSamsung', 'smsFee', 'taxInvoiceEmail', 'settleAccountNo', 'directFee', 'solutionFee', 'settleAccountHolder', 'withdrawRestrictType', 'withdrawRestrictStartTime', 'withdrawRestrictEndTime', 'terminalPayRestrict', 'webPayRestrict', 'defaultFeeHq', 'defaultFeeDist', 'defaultFeeBranch', 'defaultFeeAgency', 'defaultFeeSalesOffice', 'defaultPayLimitPerTx', 'defaultPayLimitDay', 'defaultPayLimitMonth', 'defaultPayLimitYearCorp', 'defaultPayLimitYearInd', 'copyright', 'regionalCardLimits', 'regionalTerminals'];
            var regionalSettings = {};
            regionalKeys.forEach(function (k) { if (fd[k] !== undefined && fd[k] !== null && fd[k] !== '') regionalSettings[k] = fd[k]; });
            fd.regionalSettings = JSON.stringify(regionalSettings);
          }
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          var mainFile = form.querySelector('#brandingMainImageFile');
          var logoFile = form.querySelector('#brandingLogoImageFile');
          var themeEl = form.querySelector('#brandingTheme');
          var brandingCard = form.closest('.tab-pane') && form.closest('.tab-pane').querySelector('#brandingCard');
          var isRegOrMaster = brandingCard && !brandingCard.classList.contains('d-none');
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
            if (isRegOrMaster && window.PG_API.orgBrandingUpload && window.PG_API.orgBrandingSave) {
              var chain = Promise.resolve();
              if (mainFile && mainFile.files && mainFile.files[0]) {
                chain = chain.then(function () { return window.PG_API.orgBrandingUpload(compId, 'main', mainFile.files[0]); });
              }
              if (logoFile && logoFile.files && logoFile.files[0]) {
                chain = chain.then(function () { return window.PG_API.orgBrandingUpload(compId, 'logo', logoFile.files[0]); });
              }
              if (themeEl && themeEl.value) {
                chain = chain.then(function () { return window.PG_API.orgBrandingSave(compId, themeEl.value); });
              }
              return chain;
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
      function renderTemplateSelect(scopeEl, data) {
        if (!scopeEl) return;
        var templates = (data && data.templates) ? data.templates : [];
        var html = '';
        templates.forEach(function (t) {
          var s = t.scope || '';
          var nm = t.policyName || s.replace('HQPOL:', '');
          html += '<option value="' + s + '">' + nm + '</option>';
        });
        if (!html) html = '<option value="HQPOL:A">A</option>';
        scopeEl.innerHTML = html;
        if (data && data.deployedTemplateScope) scopeEl.value = data.deployedTemplateScope;
      }
      function fillDefaultCommissionForm(data) {
        if (!(data && pane.querySelector('[name="payRate"]'))) return;
        ['perTxFee', 'cancelRate', 'usageRate', 'failFee', 'payRate', 'refundRate', 'rollingPct', 'rollingDays', 'memo', 'policyName', 'deployYn', 'templateScope', 'deployedTemplateScope', 'feeSettlementPerTx', 'feeUsdt', 'feeFx'].forEach(function (k) {
          var el = pane.querySelector('[name="' + k + '"]');
          if (el && data[k] != null) el.value = data[k];
        });
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
      if (dimm) dimm.style.display = 'flex';
      window.PG_API.hqDefaultCommission().then(function (data) {
        fillDefaultCommissionForm(data);
        var scopeEl = pane.querySelector('[name="templateScope"]');
        if (scopeEl && !scopeEl._bound) {
          scopeEl._bound = true;
          renderTemplateSelect(scopeEl, data);
          scopeEl.value = (data && data.deployedTemplateScope) ? data.deployedTemplateScope : 'HQPOL:A';
          fillDefaultCommissionForm(currentTemplateData(data));
          scopeEl.addEventListener('change', function () {
            window.PG_API.hqDefaultCommission().then(function (d2) {
              renderTemplateSelect(scopeEl, d2);
              fillDefaultCommissionForm(currentTemplateData(d2));
            });
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
      var addTplBtn = pane.querySelector('#hqDefaultCommissionTemplateAddBtn');
      if (addTplBtn && !addTplBtn._bound) {
        addTplBtn._bound = true;
        addTplBtn.addEventListener('click', function () {
          var code = window.prompt('추가할 정책코드를 입력하세요. (예: E, VIP1)');
          if (code == null) return;
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.hqDefaultCommissionTemplateAdd({ templateCode: String(code).trim() }).then(function () {
            return window.PG_API.hqDefaultCommission();
          }).then(function (d2) {
            var scopeEl = pane.querySelector('[name="templateScope"]');
            renderTemplateSelect(scopeEl, d2);
            fillDefaultCommissionForm(currentTemplateData(d2));
            alert('정책 템플릿이 추가되었습니다.');
          }).catch(function (e) {
            alert(e && e.message ? e.message : '정책 추가 실패');
          }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
      }
      var delTplBtn = pane.querySelector('#hqDefaultCommissionTemplateDeleteBtn');
      if (delTplBtn && !delTplBtn._bound) {
        delTplBtn._bound = true;
        delTplBtn.addEventListener('click', function () {
          var scopeEl = pane.querySelector('[name="templateScope"]');
          var scope = scopeEl ? scopeEl.value : '';
          if (!scope) { alert('삭제할 정책을 먼저 선택하세요.'); return; }
          if (!window.confirm('정책 템플릿을 삭제하시겠습니까? 배포 정책이면 가맹점 생성 기본값에 영향이 있습니다.')) return;
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.hqDefaultCommissionTemplateDelete(scope).then(function () {
            return window.PG_API.hqDefaultCommission();
          }).then(function (d2) {
            var el = pane.querySelector('[name="templateScope"]');
            renderTemplateSelect(el, d2);
            fillDefaultCommissionForm(currentTemplateData(d2));
            alert('정책 템플릿이 삭제되었습니다.');
          }).catch(function (e) {
            alert(e && e.message ? e.message : '정책 삭제 실패');
          }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
      }
    }
    if (url === '/hq/notifyEnv') {
      var dimmN = document.getElementById('dimm');
      function fillNotifyTargets(list) {
        var arr = Array.isArray(list) ? list : [];
        pane.querySelectorAll('select[data-load-notify-targets="true"]').forEach(function (sel) {
          var cur = sel.value || '';
          var html = '<option value="">선택</option>';
          arr.forEach(function (t) {
            html += '<option value="' + (t.targetUrl || '') + '" data-id="' + (t.id || '') + '">' + (t.targetName || t.targetCode || '노티') + ' - ' + (t.targetUrl || '') + '</option>';
          });
          sel.innerHTML = html;
          if (cur) sel.value = cur;
        });
        var delSel = pane.querySelector('[name="deleteNotifyTargetId"]');
        if (delSel) {
          var dhtml = '<option value="">선택</option>';
          arr.forEach(function (t) { dhtml += '<option value="' + (t.id || '') + '">' + (t.targetName || t.targetCode || '노티') + '</option>'; });
          delSel.innerHTML = dhtml;
        }
      }
      function fillNotifyEnv(data) {
        if (!data) return;
        ['notifyIngressUrl', 'ingressToken', 'publicBaseUrl', 'autoVoidYn', 'emailVoidYn', 'autoRefundYn', 'forceRefundYn', 'autoVoidAfterHours', 'notifyOkResponse', 'otpRequiredYn', 'otpPolicyMode', 'passwordPolicyMode', 'forgotPasswordEnabledYn', 'managerUserControlEnabledYn', 'managerPasswordResetEnabledYn'].forEach(function (k) {
          var el = pane.querySelector('[name="' + k + '"]');
          if (el && data[k] != null && data[k] !== undefined) el.value = data[k];
        });
      }
      if (dimmN) dimmN.style.display = 'flex';
      Promise.all([
        window.PG_API.hqNotifyEnv(),
        window.PG_API.hqNotifyTargets()
      ]).then(function (res) {
        fillNotifyEnv(res[0]);
        fillNotifyTargets(res[1]);
      }).catch(function () {}).finally(function () { if (dimmN) dimmN.style.display = 'none'; });
      var hqNotifySave = pane.querySelector('#hqNotifyEnvSaveBtn');
      if (hqNotifySave && !hqNotifySave._hqNeBound) {
        hqNotifySave._hqNeBound = true;
        hqNotifySave.addEventListener('click', function () {
          var fd = {};
          ['publicBaseUrl', 'autoVoidYn', 'emailVoidYn', 'autoRefundYn', 'forceRefundYn', 'autoVoidAfterHours', 'notifyOkResponse', 'otpRequiredYn', 'otpPolicyMode', 'passwordPolicyMode', 'forgotPasswordEnabledYn', 'managerUserControlEnabledYn', 'managerPasswordResetEnabledYn'].forEach(function (k) {
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
      var createBtn = pane.querySelector('button[data-field="newNotifyTargetAction"][data-action="노티 생성"]');
      if (createBtn && !createBtn._bound) {
        createBtn._bound = true;
        createBtn.addEventListener('click', function () {
          var nameEl = pane.querySelector('[name="newNotifyTargetName"]');
          var urlEl = pane.querySelector('[name="newNotifyTargetUrl"]');
          var name = nameEl && nameEl.value ? String(nameEl.value).trim() : '';
          if (!name) { alert('노티 대상명을 입력하세요.'); return; }
          if (dimmN) dimmN.style.display = 'flex';
          window.PG_API.hqNotifyTargetCreate(name).then(function (d) {
            if (urlEl) urlEl.value = d && d.targetUrl ? d.targetUrl : '';
            return window.PG_API.hqNotifyTargets();
          }).then(function (list) {
            fillNotifyTargets(list);
            alert('노티 대상 URL이 생성되었습니다.');
          }).catch(function (e) { alert(e && e.message ? e.message : '노티 생성 실패'); }).finally(function () { if (dimmN) dimmN.style.display = 'none'; });
        });
      }
      var delBtn = pane.querySelector('button[data-field="deleteNotifyTargetAction"][data-action="선택 삭제"]');
      if (delBtn && !delBtn._bound) {
        delBtn._bound = true;
        delBtn.addEventListener('click', function () {
          var sel = pane.querySelector('[name="deleteNotifyTargetId"]');
          var id = sel && sel.value ? String(sel.value) : '';
          if (!id) { alert('삭제할 노티 대상을 선택하세요.'); return; }
          if (!window.confirm('선택한 노티 대상을 삭제하시겠습니까?')) return;
          if (dimmN) dimmN.style.display = 'flex';
          window.PG_API.hqNotifyTargetDelete(id).then(function () {
            return window.PG_API.hqNotifyTargets();
          }).then(function (list) {
            fillNotifyTargets(list);
            alert('삭제되었습니다.');
          }).catch(function (e) { alert(e && e.message ? e.message : '삭제 실패'); }).finally(function () { if (dimmN) dimmN.style.display = 'none'; });
        });
      }
    }
    if (url === '/hq/accountMng' && !pane._hqAccBound) {
      pane._hqAccBound = true;
      pane.addEventListener('click', function (e) {
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
          var uid = window.prompt('사용자ID(로그인 ID)를 입력하세요.');
          if (uid == null || !String(uid).trim()) return;
          var cc = window.prompt('허용할 업체코드(본사·총판·가맹점 코드)를 입력하세요.');
          if (cc == null || !String(cc).trim()) return;
          var dimmA = document.getElementById('dimm');
          if (dimmA) dimmA.style.display = 'flex';
          window.PG_API.hqAccountAccessAdd({ username: String(uid).trim(), compCode: String(cc).trim() }).then(function () {
            if (typeof doSearch === 'function') doSearch(pane, tabId, 1);
          }).catch(function (err) { alert(err && err.message ? err.message : '추가 실패'); }).finally(function () { if (dimmA) dimmA.style.display = 'none'; });
        });
      }
    }
    if (url === '/user/userMng' && !pane._userMngBound) {
      pane._userMngBound = true;
      var addUserBtn = pane.querySelector('#addBtn');
      if (addUserBtn && !addUserBtn._bound) {
        addUserBtn._bound = true;
        addUserBtn.addEventListener('click', function () {
          var compId = window.prompt('소속 업체코드를 입력하세요.');
          if (compId == null || !String(compId).trim()) return;
          var userId = window.prompt('사용자ID를 입력하세요.');
          if (userId == null || !String(userId).trim()) return;
          var userNm = window.prompt('사용자명을 입력하세요.');
          if (userNm == null || !String(userNm).trim()) return;
          var pwd = window.prompt('초기 비밀번호(8자 이상)를 입력하세요.');
          if (pwd == null || !String(pwd).trim()) return;
          var roleSel = window.prompt('담당구분 입력: MANAGER / OPERATOR / SETTLEMENT / TECH', 'MANAGER');
          var dimmU = document.getElementById('dimm');
          if (dimmU) dimmU.style.display = 'flex';
          window.PG_API.userAdd({
            compId: String(compId).trim(),
            userId: String(userId).trim(),
            userNm: String(userNm).trim(),
            password: String(pwd).trim(),
            role: 'USER',
            userType: 'ASSISTANT',
            assistantRoleType: roleSel ? String(roleSel).trim().toUpperCase() : 'MANAGER'
          }).then(function () {
            alert('사용자가 등록되었습니다.');
            if (typeof doSearch === 'function') doSearch(pane, tabId, 1);
          }).catch(function (err) {
            alert(err && err.message ? err.message : '사용자 등록 실패');
          }).finally(function () { if (dimmU) dimmU.style.display = 'none'; });
        });
      }
      pane.addEventListener('click', function (e) {
        var delBtn = e.target.closest && e.target.closest('.user-del-btn');
        if (delBtn && pane.contains(delBtn)) {
          var delId = delBtn.getAttribute('data-id');
          if (!delId || !window.confirm('사용자를 삭제할까요?')) return;
          var dimmUD = document.getElementById('dimm');
          if (dimmUD) dimmUD.style.display = 'flex';
          window.PG_API.userDelete(delId).then(function () {
            if (typeof doSearch === 'function') doSearch(pane, tabId, 1);
          }).catch(function (err) { alert(err && err.message ? err.message : '삭제 실패'); }).finally(function () { if (dimmUD) dimmUD.style.display = 'none'; });
          return;
        }
        var resetBtn = e.target.closest && e.target.closest('.user-reset-pwd-btn');
        if (!resetBtn || !pane.contains(resetBtn)) return;
        var resetId = resetBtn.getAttribute('data-id');
        if (!resetId || !window.confirm('비밀번호를 임시 비밀번호로 초기화할까요?')) return;
        var dimmUR = document.getElementById('dimm');
        if (dimmUR) dimmUR.style.display = 'flex';
        window.PG_API.userResetPassword(resetId).then(function (data) {
          alert('초기화 완료\n사용자ID: ' + (data.userId || '-') + '\n임시비밀번호: ' + (data.tempPassword || '-'));
          if (typeof doSearch === 'function') doSearch(pane, tabId, 1);
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
    if (hqPgApiAddBtn && url === '/hq/pgApiMng' && !hqPgApiAddBtn._hqPgAddBound) {
      hqPgApiAddBtn._hqPgAddBound = true;
      hqPgApiAddBtn.addEventListener('click', function () {
        if (window.openPgAgencyModal) window.openPgAgencyModal({});
      });
    }
    if (url === '/hq/pgApiMng' && !pane._hqPgGridDbl) {
      pane._hqPgGridDbl = true;
      pane.addEventListener('dblclick', function (e) {
        var tr = e.target.closest('tbody tr');
        if (!tr || tr.querySelector('.empty-state-cell')) return;
        var grid = pane.querySelector('#grid_' + tabId + ' tbody');
        if (!grid || !grid.contains(tr)) return;
        var rows = grid.querySelectorAll('tr');
        var idx = Array.prototype.indexOf.call(rows, tr);
        var list = pane._lastGridList || [];
        if (idx < 0 || idx >= list.length) return;
        var row = list[idx];
        if (window.openPgAgencyModal) {
          window.openPgAgencyModal({
            id: row.id,
            pgCd: row.pgCd,
            pgNm: row.pgNm,
            apiEndpoint: row.apiEndpoint || '',
            useYn: row.useYn || 'Y'
          });
        }
      });
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
    var pgAgSave = document.getElementById('pgAgencyEditSaveBtn');
    if (pgAgSave && !pgAgSave._bound) {
      pgAgSave._bound = true;
      pgAgSave.addEventListener('click', function () {
        if (!window.pgDoubleConfirm || !window.pgDoubleConfirm('PG사 연동 정보를 저장하시겠습니까?', '정말 저장하시겠습니까?')) return;
        var idVal = document.getElementById('pgAgencyEditId').value.trim();
        var body = {
          pgCd: document.getElementById('pgAgencyEditPgCd').value.trim(),
          pgNm: document.getElementById('pgAgencyEditPgNm').value.trim(),
          apiEndpoint: (document.getElementById('pgAgencyEditEndpoint') && document.getElementById('pgAgencyEditEndpoint').value) ? document.getElementById('pgAgencyEditEndpoint').value.trim() : '',
          useYn: (document.getElementById('pgAgencyEditUseYn') && document.getElementById('pgAgencyEditUseYn').value) || 'Y'
        };
        if (idVal) body.id = idVal;
        if (!body.pgCd || !body.pgNm) { alert('PG사코드와 PG사명은 필수입니다.'); return; }
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
