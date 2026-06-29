/**
 * 검수관리 결제개요(/calc/payOverview) — 통합 결제내역 + 단말기·위치·IP·원인 열.
 */
(function (w) {
  'use strict';
  var base = w.PG_PAY_LIST_INTEGRATED;
  if (!base || !base.columns) return;

  function cloneArr(arr) {
    return JSON.parse(JSON.stringify(arr || []));
  }

  function insertAfterKey(cols, afterKey, insertCols) {
    var idx = -1;
    for (var i = 0; i < cols.length; i++) {
      if (cols[i] && cols[i].key === afterKey) { idx = i; break; }
    }
    if (idx < 0) idx = 7;
    var toInsert = Array.isArray(insertCols) ? insertCols : [insertCols];
    cols.splice.apply(cols, [idx + 1, 0].concat(toInsert));
  }

  function addKeysToList(list, keys) {
    var out = (list || []).slice();
    keys.forEach(function (k) {
      if (out.indexOf(k) === -1) out.push(k);
    });
    return out;
  }

  var extraCols = [
    { key: 'payerDeviceLabel', label: '단말기' },
    { key: 'payerRegion', label: '위치' },
    { key: 'payerClientIp', label: 'IP' },
    { key: 'outcomeCause', label: '원인' }
  ];
  var extraKeys = ['payerDeviceLabel', 'payerRegion', 'payerClientIp', 'outcomeCause'];
  var helloPriorityBase = (base.viewSettingHelloPriorityKeys || base.viewSettingDefaultSelectedKeys || []).slice();
  var helloPriority = helloPriorityBase.slice();
  (function insertHelloKeysAfter(afterKey, keys) {
    var idx = helloPriority.indexOf(afterKey);
    if (idx < 0) idx = helloPriority.length - 1;
    keys.forEach(function (k, i) {
      if (helloPriority.indexOf(k) === -1) {
        helloPriority.splice(idx + 1 + i, 0, k);
      }
    });
  })('paymentChannel', extraKeys);

  var cols = cloneArr(base.columns);
  insertAfterKey(cols, 'paymentChannel', extraCols);

  var scopes = base.orgAllowanceDefaultKeysByScope || {};
  w.PG_PAY_LIST_OVERVIEW = {
    columnGuideFixedKeys: (base.columnGuideFixedKeys || []).slice(),
    columnGuideHiddenKeys: (base.columnGuideHiddenKeys || []).slice(),
    viewSettingDefaultSelectedKeys: addKeysToList(base.viewSettingDefaultSelectedKeys, extraKeys),
    viewSettingHelloPriorityKeys: helloPriority,
    orgAllowanceDefaultKeysByScope: {
      REGIONAL: null,
      MASTER_DIST: addKeysToList(scopes.MASTER_DIST, extraKeys),
      BRANCH_GROUP: addKeysToList(scopes.BRANCH_GROUP, extraKeys),
      MERCHANT: addKeysToList(scopes.MERCHANT, extraKeys)
    },
    headerGroups: cloneArr(base.headerGroups),
    columns: cols
  };
})(typeof window !== 'undefined' ? window : this);
