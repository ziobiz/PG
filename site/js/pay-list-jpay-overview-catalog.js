/**
 * 검수관리 통합개요(/calc/jpayTrList) — 결제개요와 동일 VIEW SETTING·그리드 열.
 * 단말기·위치·IP 열은 제외( JPAY 포털 Export 기준 비교용 ).
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

  var extraCols = [{ key: 'outcomeCause', label: '원인' }];
  var extraKeys = ['outcomeCause'];
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
  w.PG_JPAY_TR_OVERVIEW = {
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

  /** @deprecated PG_JPAY_TR_OVERVIEW 사용 */
  w.PG_JPAY_TR_VIEW_DEFAULTS = {
    viewSettingDefaultSelectedKeys: w.PG_JPAY_TR_OVERVIEW.viewSettingDefaultSelectedKeys.slice()
  };
})(typeof window !== 'undefined' ? window : this);
