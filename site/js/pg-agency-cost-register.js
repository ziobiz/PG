/**
 * 연동·배포 — 결제대행사 등록 (근원원가)
 * 위: 기본정보·수수료·APM·기타 카드 · 아래: VIEW SETTING·더블클릭·히스토리
 */
(function (global) {
  'use strict';

  var LS_KEY = 'pg_hq_cost_policy_cols_v3';

  var APM_KEYS = [
    'feeApmCrypto', 'feeApmPaypay', 'feeApmRakuten', 'feeApmPromptpay',
    'feeApmLine', 'feeApmKakao', 'feeApmWechat'
  ];

  var CARD_FEE_KEYS = [
    'payRate', 'feeCardVisa', 'feeCardMaster', 'feeCardJcb',
    'feeCardAmex', 'feeCardUnion', 'feeCardDiners'
  ];

  var FEE_KEYS = [
    'perTxFee', 'usageRate', 'failFee', 'cancelRate', 'voidFeePerTx', 'handVoidFeePerTx', 'manualVoidFeePerTx', 'refundRate', 'handRefundFeePerTx',
    'feeSettlementPerTx', 'remittanceTransferFee', 'usdtTransferFeeUsd', 'feeUsdt', 'feeFx',
    'rollingPct', 'rollingDays', 'fee3dsRate', 'chargebackFeePerTx'
  ].concat(CARD_FEE_KEYS).concat(APM_KEYS);

  var COLS = [
    { key: 'useYn', label: '운영유무', edit: true },
    { key: 'costNm', label: '대행사명', edit: true },
    { key: 'vendorNm', label: '계약대행사', edit: true },
    { key: 'pgCd', label: '결제코드', edit: true },
    { key: 'multiYn', label: '멀티유무', edit: true },
    { key: 'displayCurrency', label: '표시통화', edit: true },
    { key: 'currencyCode', label: '청구통화', edit: true },
    { key: 'vatRatePct', label: '부가세율', edit: true },
    { key: 'cardFeeMode', label: '카드수수료방식', edit: true },
    { key: 'payRate', label: '카드수수료%', edit: true },
    { key: 'feeCardVisa', label: 'VISA%', edit: true },
    { key: 'feeCardMaster', label: 'MASTER%', edit: true },
    { key: 'feeCardJcb', label: 'JCB%', edit: true },
    { key: 'feeCardAmex', label: 'AMEX%', edit: true },
    { key: 'feeCardUnion', label: 'UNION%', edit: true },
    { key: 'feeCardDiners', label: 'DINERS%', edit: true },
    { key: 'cardBrandScopeHint', label: '카드브랜드', edit: false },
    { key: 'perTxFee', label: '건당수수료', edit: true },
    { key: 'failFee', label: '실패수수료', edit: true },
    { key: 'cancelRate', label: '취소수수료', edit: true },
    { key: 'voidFeePerTx', label: '무효수수료', edit: true },
    { key: 'handVoidFeePerTx', label: '수무효수수료', edit: true },
    { key: 'manualVoidFeePerTx', label: '이무효수수료', edit: true },
    { key: 'refundRate', label: '환불수수료', edit: true },
    { key: 'handRefundFeePerTx', label: '수환불수수료', edit: true },
    { key: 'feeSettlementPerTx', label: '정산수수료', edit: true },
    { key: 'usageRate', label: '월이용료', edit: true },
    { key: 'fee3dsRate', label: '3DS(건)', edit: true },
    { key: 'chargebackFeePerTx', label: '차지백(건)', edit: true },
    { key: 'remittanceTransferFee', label: '송금이체', edit: true },
    { key: 'usdtTransferFeeUsd', label: 'USDT송금(USD)', edit: true },
    { key: 'feeUsdt', label: 'USDT(%)', edit: true },
    { key: 'feeFx', label: 'FX(%)', edit: true },
    { key: 'feeApmCrypto', label: '크립토수수료(%)', edit: true },
    { key: 'feeApmPaypay', label: 'PayPay 수수료(%)', edit: true },
    { key: 'feeApmRakuten', label: '라쿠텐페이 수수료(%)', edit: true },
    { key: 'feeApmPromptpay', label: 'PromptPay 수수료(%)', edit: true },
    { key: 'feeApmLine', label: 'LINE 수수료(%)', edit: true },
    { key: 'feeApmKakao', label: '카카오 수수료(%)', edit: true },
    { key: 'feeApmWechat', label: '위챗 수수료(%)', edit: true },
    { key: 'rollingPct', label: '롤링(%)', edit: true },
    { key: 'rollingDays', label: '보류일수', edit: true },
    { key: 'settleScheduleLabel', label: '정산주기', edit: false },
    { key: 'settleScheduleType', label: '정산유형', edit: true },
    { key: 'settleLagN', label: 'N', edit: true },
    { key: 'settleBatchTime', label: 'D일괄시각', edit: true },
    { key: 'vatExcluded', label: 'VAT제외', edit: false },
    { key: 'vatIncluded', label: 'VAT포함', edit: false },
    { key: 'linkedAgencyCnt', label: '연결설정수', edit: false },
    { key: 'policyRemark', label: '정책비고', edit: true },
    { key: 'updatedAt', label: '수정시각', edit: false },
    { key: '_act', label: '비고', edit: false, fixed: true }
  ];

  function t(k) {
    if (global.PG_UI_I18N && typeof global.PG_UI_I18N.t === 'function') {
      try { return global.PG_UI_I18N.t(k) || k; } catch (e) { /* ignore */ }
    }
    return k;
  }

  function esc(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
  }

  function loadVisible() {
    try {
      var raw = localStorage.getItem(LS_KEY);
      if (raw) {
        var o = JSON.parse(raw);
        if (o && typeof o === 'object') return o;
      }
    } catch (e) { /* ignore */ }
    var all = {};
    COLS.forEach(function (c) { all[c.key] = true; });
    return all;
  }

  function saveVisible(map) {
    try { localStorage.setItem(LS_KEY, JSON.stringify(map)); } catch (e) { /* ignore */ }
  }

  function visibleCols(vis) {
    return COLS.filter(function (c) {
      if (c.fixed) return true;
      return vis[c.key] !== false;
    });
  }

  function bind(pane) {
    if (!pane || !global.PG_API) return;
    var dimm = document.getElementById('dimm');
    var dirty = {};
    var selectedId = null;
    var vis = loadVisible();

    function flash(variant, msg) {
      if (global.PG_UI && global.PG_UI.showBanner) {
        global.PG_UI.showBanner(pane, 'hqPgAgencyCostFlash', variant, msg, 6500);
      }
    }

    function fillChargeback(opts) {
      var sel = pane.querySelector('#hqPgCostChargebackPolicyId');
      if (!sel) return;
      var prev = sel.value || '';
      var h = '<option value="">' + esc(t('(미사용) 건당 차지백만')) + '</option>';
      (opts || []).forEach(function (o) {
        h += '<option value="' + esc(String(o.id)) + '">' + esc(o.name || '') + '</option>';
      });
      sel.innerHTML = h;
      if (prev) sel.value = prev;
    }

    function syncMultiUi() {
      var multiEl = pane.querySelector('#hqPgCostMultiYn');
      var wrap = pane.querySelector('#hqPgCostDisplayCurrencyWrap');
      var displayEl = pane.querySelector('#hqPgCostDisplayCurrency');
      var chargeEl = pane.querySelector('#hqPgCostCurrencyCode');
      var isMulti = multiEl && String(multiEl.value || 'N').toUpperCase() === 'Y';
      if (wrap) wrap.style.display = isMulti ? '' : 'none';
      if (!isMulti && displayEl && chargeEl) {
        displayEl.value = chargeEl.value;
      }
    }

    function applyForm(d) {
      d = d || {};
      var map = {
        costNm: '#hqPgCostCostNm',
        vendorNm: '#hqPgCostVendorNm',
        pgCd: '#hqPgCostPgCd',
        multiYn: '#hqPgCostMultiYn',
        cardFeeMode: '#hqPgCostCardFeeMode',
        currencyCode: '#hqPgCostCurrencyCode',
        displayCurrency: '#hqPgCostDisplayCurrency',
        vatRatePct: '#hqPgCostVatRatePct',
        useYn: '#hqPgCostUseYn',
        settleScheduleType: '#hqPgCostSettleType',
        settleLagN: '#hqPgCostSettleLagN',
        settleBatchTime: '#hqPgCostSettleBatchTime',
        policyRemark: '#hqPgCostPolicyRemark',
        chargebackPolicyId: '#hqPgCostChargebackPolicyId'
      };
      Object.keys(map).forEach(function (k) {
        var el = pane.querySelector(map[k]);
        if (!el) return;
        if (d[k] != null) el.value = String(d[k]);
      });
      var idEl = pane.querySelector('#hqPgCostId');
      if (idEl) idEl.value = d.id != null ? String(d.id) : '';
      FEE_KEYS.forEach(function (k) {
        var el = pane.querySelector('[name="' + k + '"]');
        if (el && d[k] != null) el.value = String(d[k]);
      });
      var ei;
      for (ei = 1; ei <= 4; ei++) {
        ['Name', 'Mode', 'Value'].forEach(function (suf) {
          var el2 = pane.querySelector('[name="extraFee' + ei + suf + '"]');
          if (el2) el2.value = d['extraFee' + ei + suf] != null ? String(d['extraFee' + ei + suf]) : '';
        });
      }
      var typ = pane.querySelector('#hqPgCostSettleType');
      var wrap = pane.querySelector('#hqPgCostSettleBatchWrap');
      if (typ && wrap) wrap.style.display = typ.value === 'D' ? '' : 'none';
      syncMultiUi();
      syncCardFeeUi();
    }

    function syncCardFeeUi() {
      var modeEl = pane.querySelector('#hqPgCostCardFeeMode');
      var uni = pane.querySelector('#hqPgCostCardFeeUnifiedWrap');
      var brand = pane.querySelector('#hqPgCostCardFeeBrandWrap');
      var per = modeEl && String(modeEl.value || '').toUpperCase() === 'PER_BRAND';
      if (uni) uni.classList.toggle('d-none', !!per);
      if (brand) brand.classList.toggle('d-none', !per);
    }

    function collectForm() {
      var body = {};
      var idEl = pane.querySelector('#hqPgCostId');
      if (idEl && idEl.value) body.id = idEl.value;
      ['costNm', 'vendorNm', 'pgCd', 'multiYn', 'cardFeeMode', 'currencyCode', 'displayCurrency', 'vatRatePct', 'useYn',
        'settleBasis', 'settleScheduleType', 'settleLagN', 'settleBatchTime', 'policyRemark', 'chargebackPolicyId']
        .concat(FEE_KEYS).forEach(function (k) {
          var el = pane.querySelector('[name="' + k + '"]');
          if (k === 'costNm') el = pane.querySelector('#hqPgCostCostNm');
          if (k === 'vendorNm') el = pane.querySelector('#hqPgCostVendorNm');
          if (k === 'pgCd') el = pane.querySelector('#hqPgCostPgCd');
          if (k === 'multiYn') el = pane.querySelector('#hqPgCostMultiYn');
          if (k === 'cardFeeMode') el = pane.querySelector('#hqPgCostCardFeeMode');
          if (k === 'currencyCode') el = pane.querySelector('#hqPgCostCurrencyCode');
          if (k === 'displayCurrency') el = pane.querySelector('#hqPgCostDisplayCurrency');
          if (k === 'vatRatePct') el = pane.querySelector('#hqPgCostVatRatePct');
          if (k === 'useYn') el = pane.querySelector('#hqPgCostUseYn');
          if (k === 'settleScheduleType') el = pane.querySelector('#hqPgCostSettleType');
          if (k === 'settleLagN') el = pane.querySelector('#hqPgCostSettleLagN');
          if (k === 'settleBatchTime') el = pane.querySelector('#hqPgCostSettleBatchTime');
          if (k === 'policyRemark') el = pane.querySelector('#hqPgCostPolicyRemark');
          if (k === 'chargebackPolicyId') el = pane.querySelector('#hqPgCostChargebackPolicyId');
          if (el) body[k] = el.value;
        });
      var ei;
      for (ei = 1; ei <= 4; ei++) {
        ['Name', 'Mode', 'Value'].forEach(function (suf) {
          var el2 = pane.querySelector('[name="extraFee' + ei + suf + '"]');
          if (el2) body['extraFee' + ei + suf] = el2.value;
        });
      }
      if (!body.settleBasis) body.settleBasis = 'TRANSACTION';
      if (String(body.multiYn || 'N').toUpperCase() !== 'Y') {
        body.displayCurrency = body.currencyCode;
        body.multiYn = 'N';
      } else {
        body.multiYn = 'Y';
      }
      return body;
    }

    function validateRequired(body) {
      if (body.useYn == null || String(body.useYn).trim() === '') {
        return t('운영유무를 선택하세요.');
      }
      if (!body.vendorNm || !String(body.vendorNm).trim()) {
        return t('계약대행사를 입력하세요.');
      }
      if (!body.pgCd || !String(body.pgCd).trim()) {
        return t('결제코드를 입력하세요.');
      }
      if (!body.currencyCode || !String(body.currencyCode).trim()) {
        return t('청구통화를 선택하세요.');
      }
      if (String(body.multiYn || 'N').toUpperCase() === 'Y') {
        if (!body.displayCurrency || !String(body.displayCurrency).trim()) {
          return t('표시통화를 선택하세요.');
        }
      }
      if (body.vatRatePct == null || String(body.vatRatePct).trim() === '') {
        return t('부가세율을 입력하세요.');
      }
      if (body.multiYn == null || String(body.multiYn).trim() === '') {
        return t('멀티유무를 선택하세요.');
      }
      if (body.rollingPct == null || String(body.rollingPct).trim() === '') {
        return t('롤링(%)을 입력하세요.');
      }
      if (body.rollingDays == null || String(body.rollingDays).trim() === '') {
        return t('보류일수를 입력하세요.');
      }
      if (!body.settleScheduleType || !String(body.settleScheduleType).trim()) {
        return t('정산주기 유형을 선택하세요.');
      }
      if (body.settleLagN == null || String(body.settleLagN).trim() === '') {
        return t('정산주기 N을 입력하세요.');
      }
      if (String(body.settleScheduleType).toUpperCase() === 'D' && (!body.settleBatchTime || !String(body.settleBatchTime).trim())) {
        return t('D 일괄시각을 입력하세요.');
      }
      return null;
    }

    function openHello() {
      var box = pane.querySelector('#hqPgCostHelloPanel');
      if (!box) return;
      var h = '<div class="small fw-semibold mb-2">' + esc(t('VIEW SETTING')) + ' / ' + esc(t('헬로')) + '</div>';
      h += '<div class="row g-1 small">';
      COLS.forEach(function (c) {
        if (c.fixed) return;
        var on = vis[c.key] !== false;
        h += '<div class="col-6 col-md-4 col-lg-3"><label class="form-check">'
          + '<input type="checkbox" class="form-check-input hq-cost-hello-cb" data-key="' + esc(c.key) + '"' + (on ? ' checked' : '') + '>'
          + '<span class="form-check-label">' + esc(t(c.label)) + '</span></label></div>';
      });
      h += '</div><div class="mt-2 d-flex gap-2">'
        + '<button type="button" class="btn btn-sm btn-primary" id="hqPgCostHelloApply">' + esc(t('적용')) + '</button>'
        + '<button type="button" class="btn btn-sm btn-outline-secondary" id="hqPgCostHelloAll">' + esc(t('전체')) + '</button>'
        + '<button type="button" class="btn btn-sm btn-outline-secondary" id="hqPgCostHelloClose">' + esc(t('닫기')) + '</button>'
        + '</div>';
      box.innerHTML = h;
      box.classList.remove('d-none');
      box.querySelector('#hqPgCostHelloApply').onclick = function () {
        box.querySelectorAll('.hq-cost-hello-cb').forEach(function (cb) {
          vis[cb.getAttribute('data-key')] = !!cb.checked;
        });
        saveVisible(vis);
        renderList(pane._hqPgCostBootstrap && pane._hqPgCostBootstrap.policies);
        box.classList.add('d-none');
      };
      box.querySelector('#hqPgCostHelloAll').onclick = function () {
        box.querySelectorAll('.hq-cost-hello-cb').forEach(function (cb) { cb.checked = true; });
      };
      box.querySelector('#hqPgCostHelloClose').onclick = function () { box.classList.add('d-none'); };
    }

    function renderList(rows) {
      var tb = pane.querySelector('#hqPgAgencyCostListTbody');
      var thead = pane.querySelector('#hqPgAgencyCostListThead');
      if (!tb || !thead) return;
      var cols = visibleCols(vis);
      var th = '<tr>';
      cols.forEach(function (c) {
        th += '<th class="text-nowrap" data-pg-ui-t="' + esc(c.label) + '">' + esc(t(c.label)) + '</th>';
      });
      th += '</tr>';
      thead.innerHTML = th;
      if (!rows || !rows.length) {
        tb.innerHTML = '<tr><td colspan="' + cols.length + '" class="text-muted text-center small">'
          + esc(t('등록된 원가 정책이 없습니다.')) + '</td></tr>';
        return;
      }
      var h = '';
      rows.forEach(function (r) {
        var id = r.id != null ? String(r.id) : '';
        var rowDirty = dirty[id] || {};
        h += '<tr class="hq-pg-cost-list-row' + (selectedId === id ? ' table-success' : '') + '" data-id="' + esc(id) + '">';
        cols.forEach(function (c) {
          if (c.key === '_act') {
            h += '<td class="text-nowrap">'
              + '<button type="button" class="btn btn-outline-primary btn-sm py-0 me-1 hq-cost-row-save" data-id="' + esc(id) + '">' + esc(t('저장')) + '</button>'
              + '<button type="button" class="btn btn-outline-danger btn-sm py-0 hq-cost-row-del" data-id="' + esc(id) + '"'
              + ' data-linked="' + esc(r.linkedAgencyCnt != null ? r.linkedAgencyCnt : 0) + '">' + esc(t('삭제')) + '</button>'
              + '</td>';
            return;
          }
          var val = rowDirty[c.key] != null ? rowDirty[c.key] : (r[c.key] != null ? r[c.key] : '');
          var cls = c.edit ? ' hq-cost-cell-edit' : '';
          h += '<td class="text-nowrap' + cls + '" data-key="' + esc(c.key) + '" data-id="' + esc(id) + '"'
            + (c.edit ? ' title="' + esc(t('더블클릭하여 수정')) + '"' : '') + '>'
            + esc(val) + '</td>';
        });
        h += '</tr>';
      });
      tb.innerHTML = h;

      tb.querySelectorAll('.hq-pg-cost-list-row').forEach(function (tr) {
        tr.addEventListener('click', function () {
          var id = tr.getAttribute('data-id');
          selectedId = id;
          var row = (rows || []).find(function (x) { return String(x.id) === String(id); });
          if (row) {
            applyForm(row);
            loadHistory(id);
          }
          renderList(rows);
        });
      });

      function doubleOk(msg1, msg2) {
        if (typeof global.pgDoubleConfirm === 'function') {
          return global.pgDoubleConfirm(msg1, msg2);
        }
        return window.confirm(t(msg1)) && window.confirm(t(msg2));
      }

      tb.querySelectorAll('.hq-cost-cell-edit').forEach(function (td) {
        td.addEventListener('dblclick', function (ev) {
          ev.stopPropagation();
          var id = td.getAttribute('data-id');
          var key = td.getAttribute('data-key');
          if (!id || !key) return;
          if (!doubleOk(
            '이 칸을 수정할까요?\n취소하면 수정하지 않습니다.',
            '수정을 진행합니다. 정말 계속할까요?'
          )) return;
          var cur = td.textContent;
          var inp = document.createElement('input');
          inp.type = 'text';
          inp.className = 'form-control form-control-sm';
          inp.value = cur;
          td.textContent = '';
          td.appendChild(inp);
          inp.focus();
          inp.select();
          function commit() {
            var v = inp.value;
            dirty[id] = dirty[id] || {};
            dirty[id][key] = v;
            td.textContent = v;
            td.classList.add('table-warning');
          }
          inp.addEventListener('blur', commit);
          inp.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') { e.preventDefault(); commit(); }
            if (e.key === 'Escape') { td.textContent = cur; }
          });
        });
      });

      tb.querySelectorAll('.hq-cost-row-save').forEach(function (btn) {
        btn.addEventListener('click', function (ev) {
          ev.stopPropagation();
          var id = btn.getAttribute('data-id');
          var row = (rows || []).find(function (x) { return String(x.id) === String(id); });
          if (!row) return;
          var body = Object.assign({}, row, dirty[id] || {}, { id: id });
          var err = validateRequired(body);
          if (err) {
            flash('warning', err);
            return;
          }
          if (!doubleOk(
            '이 원가 정책을 저장할까요?\n취소하면 저장되지 않습니다.',
            '서버에 반영합니다. 정말 진행할까요?'
          )) return;
          if (dimm) dimm.style.display = 'flex';
          global.PG_API.hqPgAgencyCostPolicySave(body).then(function (res) {
            flash('success', (res && res.message) ? t(res.message) : t('저장되었습니다.'));
            delete dirty[id];
            return reload().then(function () {
              if (res && res.policy) {
                applyForm(res.policy);
                loadHistory(res.policy.id);
              }
            });
          }).catch(function (e) {
            flash('danger', (e && e.message) ? t(e.message) : t('저장 실패'));
          }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
      });

      tb.querySelectorAll('.hq-cost-row-del').forEach(function (btn) {
        btn.addEventListener('click', function (ev) {
          ev.stopPropagation();
          var id = btn.getAttribute('data-id');
          var linked = parseInt(btn.getAttribute('data-linked') || '0', 10) || 0;
          if (linked > 0) {
            flash('warning', t('연결된 결제대행사 설정이 있어 삭제할 수 없습니다. (미사용·비활성 포함)'));
            return;
          }
          if (!doubleOk(
            '이 원가 정책을 삭제할까요?\n취소하면 삭제되지 않습니다.',
            '삭제는 되돌릴 수 없습니다. 정말 삭제할까요?'
          )) return;
          if (!global.PG_API || typeof global.PG_API.hqPgAgencyCostPolicyDelete !== 'function') {
            flash('danger', t('삭제 API가 없습니다.'));
            return;
          }
          if (dimm) dimm.style.display = 'flex';
          global.PG_API.hqPgAgencyCostPolicyDelete(id).then(function (res) {
            flash('success', (res && res.message) ? t(res.message) : t('삭제되었습니다.'));
            delete dirty[id];
            if (selectedId === String(id)) {
              selectedId = null;
              var defs = pane._hqPgCostBootstrap && pane._hqPgCostBootstrap.defaults ? pane._hqPgCostBootstrap.defaults : {};
              applyForm(defs);
              loadHistory(null);
            }
            return reload();
          }).catch(function (e) {
            flash('danger', (e && e.message) ? t(e.message) : t('삭제 실패'));
          }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
      });
    }

    function loadHistory(policyId) {
      var tb = pane.querySelector('#hqPgCostHistTbody');
      if (!tb || !policyId) {
        if (tb) tb.innerHTML = '<tr><td colspan="5" class="text-muted text-center small">' + esc(t('목록에서 행을 선택하세요.')) + '</td></tr>';
        return;
      }
      global.PG_API.hqPgAgencyCostPolicyHistory(policyId).then(function (list) {
        if (!list || !list.length) {
          tb.innerHTML = '<tr><td colspan="5" class="text-muted text-center small">' + esc(t('변경 이력이 없습니다.')) + '</td></tr>';
          return;
        }
        var h = '';
        list.forEach(function (r, i) {
          h += '<tr><td>' + (i + 1) + '</td><td class="text-nowrap">' + esc(r.createdAt) + '</td>'
            + '<td>' + esc(r.costNm) + '</td><td>' + esc(r.pgCd) + '</td><td>' + esc(r.changedBy) + '</td></tr>';
        });
        tb.innerHTML = h;
      }).catch(function () {
        tb.innerHTML = '<tr><td colspan="5" class="text-muted text-center small">' + esc(t('이력 조회 실패')) + '</td></tr>';
      });
    }

    function reload() {
      if (dimm) dimm.style.display = 'flex';
      return global.PG_API.hqPgAgencyCostPolicy().then(function (data) {
        pane._hqPgCostBootstrap = data;
        fillChargeback(data.chargebackOptions);
        renderList(data.policies);
      }).catch(function (e) {
        flash('danger', (e && e.message) ? e.message : t('불러오기 실패'));
      }).finally(function () { if (dimm) dimm.style.display = 'none'; });
    }

    var typ = pane.querySelector('#hqPgCostSettleType');
    if (typ && !typ._hqCostBound) {
      typ._hqCostBound = true;
      typ.addEventListener('change', function () {
        var wrap = pane.querySelector('#hqPgCostSettleBatchWrap');
        if (wrap) wrap.style.display = typ.value === 'D' ? '' : 'none';
      });
    }

    var multiEl = pane.querySelector('#hqPgCostMultiYn');
    if (multiEl && !multiEl._hqCostMultiBound) {
      multiEl._hqCostMultiBound = true;
      multiEl.addEventListener('change', syncMultiUi);
    }
    var chargeEl = pane.querySelector('#hqPgCostCurrencyCode');
    if (chargeEl && !chargeEl._hqCostChargeBound) {
      chargeEl._hqCostChargeBound = true;
      chargeEl.addEventListener('change', syncMultiUi);
    }
    var cardModeEl = pane.querySelector('#hqPgCostCardFeeMode');
    if (cardModeEl && !cardModeEl._hqCostCardFeeBound) {
      cardModeEl._hqCostCardFeeBound = true;
      cardModeEl.addEventListener('change', syncCardFeeUi);
    }
    syncCardFeeUi();

    function once(el, key, fn) {
      if (!el) return;
      var k = key + '_' + (pane.getAttribute('data-tab-id') || '0');
      if (el._pgBindKey === k) return;
      el._pgBindKey = k;
      el.addEventListener('click', fn);
    }

    once(pane.querySelector('#hqPgAgencyCostNewBtn'), '_hqCostNew', function () {
      dirty = {};
      selectedId = null;
      var defs = pane._hqPgCostBootstrap && pane._hqPgCostBootstrap.defaults ? pane._hqPgCostBootstrap.defaults : {};
      applyForm(defs);
      loadHistory(null);
      flash('info', t('신규 근원원가를 입력한 뒤 저장하세요.'));
      renderList(pane._hqPgCostBootstrap && pane._hqPgCostBootstrap.policies);
    });

    once(pane.querySelector('#hqPgAgencyCostSaveBtn'), '_hqCostSave', function () {
      var body = collectForm();
      var err = validateRequired(body);
      if (err) {
        flash('warning', err);
        return;
      }
      var okSave = (typeof global.pgDoubleConfirm === 'function')
        ? global.pgDoubleConfirm(
          '이 원가 정책을 저장할까요?\n취소하면 저장되지 않습니다.',
          '서버에 반영합니다. 정말 진행할까요?'
        )
        : (window.confirm(t('이 원가 정책을 저장할까요?\n취소하면 저장되지 않습니다.'))
          && window.confirm(t('서버에 반영합니다. 정말 진행할까요?')));
      if (!okSave) return;
      if (dimm) dimm.style.display = 'flex';
      global.PG_API.hqPgAgencyCostPolicySave(body).then(function (res) {
        flash('success', (res && res.message) ? t(res.message) : t('저장되었습니다.'));
        return reload().then(function () {
          if (res && res.policy) {
            applyForm(res.policy);
            selectedId = res.policy.id != null ? String(res.policy.id) : null;
            loadHistory(selectedId);
          }
        });
      }).catch(function (e) {
        flash('danger', (e && e.message) ? t(e.message) : t('저장 실패'));
      }).finally(function () { if (dimm) dimm.style.display = 'none'; });
    });

    once(pane.querySelector('#hqPgAgencyCostReloadBtn'), '_hqCostRel', function () { reload(); });
    once(pane.querySelector('#hqPgCostHelloBtn'), '_hqCostHello', openHello);

    reload().then(function () {
      var defs = pane._hqPgCostBootstrap && pane._hqPgCostBootstrap.defaults ? pane._hqPgCostBootstrap.defaults : {};
      applyForm(defs);
      loadHistory(null);
    });
  }

  global.PG_AGENCY_COST_REGISTER = { bind: bind, COLS: COLS };
})(typeof window !== 'undefined' ? window : this);
