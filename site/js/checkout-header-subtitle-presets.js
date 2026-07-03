/**
 * 결제창 경고·안내 문구 — 프리셋 모드·다국어 키 (관리자 UI + 공개 결제창 공통)
 * window.PG_CHECKOUT_HEADER_SUBTITLE
 */
(function (g) {
  'use strict';

  var PRESET_I18N = {
    ACTIVE_PREVENT: 'checkoutSubtitlePrevent',
    ACTIVE_CONFIRM: 'checkoutSubtitleConfirm',
    ACTIVE_APPROVAL: 'checkoutSubtitleApproval',
    ACTIVE_FAIL: 'checkoutSubtitleFail'
  };

  /** 관리자 UI 미리보기 — pg-ui-i18n L() 키(한국어 원문) */
  var PRESET_ADMIN_KO = {
    ACTIVE_PREVENT: '결제 오류를 방지하기 위해 정확한 카드 정보를 입력해 주세요.',
    ACTIVE_CONFIRM: '카드번호, 유효기간, CVC 번호가 올바른지 다시 한번 확인해 주세요.',
    ACTIVE_APPROVAL: '원활한 결제 처리를 위해 카드 정보를 오타 없이 입력해 주시기 바랍니다.',
    ACTIVE_FAIL: '카드 정보가 일치하지 않을 경우 결제 승인이 거절될 수 있습니다.'
  };

  var FALLBACK = {
    checkoutSubtitlePrevent: PRESET_ADMIN_KO.ACTIVE_PREVENT,
    checkoutSubtitleConfirm: PRESET_ADMIN_KO.ACTIVE_CONFIRM,
    checkoutSubtitleApproval: PRESET_ADMIN_KO.ACTIVE_APPROVAL,
    checkoutSubtitleFail: PRESET_ADMIN_KO.ACTIVE_FAIL
  };

  function norm(mode) {
    return String(mode || 'DEFAULT').trim().toUpperCase();
  }

  function isPresetMode(mode) {
    return !!PRESET_I18N[norm(mode)];
  }

  function isDirectActive(mode) {
    return norm(mode) === 'ACTIVE';
  }

  function presetI18nKey(mode) {
    return PRESET_I18N[norm(mode)] || null;
  }

  function adminPresetText(mode, translateFn) {
    var m = norm(mode);
    var ko = PRESET_ADMIN_KO[m];
    if (!ko) return '';
    if (typeof translateFn === 'function') {
      var t = translateFn(ko);
      if (t && t !== ko) return t;
    }
    return ko;
  }

  function resolveCheckoutText(ctx, tFn, defaultKeys) {
    ctx = ctx || {};
    defaultKeys = defaultKeys || ['brandSub3ds', 'brandSub'];
    var mode = norm(ctx.checkoutHeaderSubtitleMode);
    if (mode === 'DISABLED') return { show: false, text: '' };
    if (mode === 'ACTIVE') {
      var custom = ctx.checkoutHeaderSubtitleText ? String(ctx.checkoutHeaderSubtitleText).trim() : '';
      if (!custom) return { show: false, text: '' };
      return { show: true, text: custom };
    }
    var pKey = presetI18nKey(mode);
    if (pKey) {
      var pt = tFn ? tFn(pKey) : '';
      if (!pt || pt === pKey) pt = FALLBACK[pKey] || '';
      return { show: !!pt, text: pt };
    }
    var def = '';
    if (tFn) {
      for (var i = 0; i < defaultKeys.length; i++) {
        var dk = defaultKeys[i];
        var dv = tFn(dk);
        if (dv && dv !== dk) { def = dv; break; }
      }
    }
    if (!def) def = '3DS Secure Payment';
    return { show: true, text: def };
  }

  function headerSubtitleModeOptions(forSplitPay) {
    var defLabel = forSplitPay ? '기본(분할결제 안내)' : '기본(3DS 안전 결제)';
    return [
      { v: 'DEFAULT', t: defLabel },
      { v: 'ACTIVE', t: '활성(직접입력)' },
      { v: 'ACTIVE_PREVENT', t: '활성(오류방지형)' },
      { v: 'ACTIVE_CONFIRM', t: '활성(확인촉구형)' },
      { v: 'ACTIVE_APPROVAL', t: '활성(승인중심형)' },
      { v: 'ACTIVE_FAIL', t: '활성(실패안정형)' },
      { v: 'DISABLED', t: '비활성' }
    ];
  }

  g.PG_CHECKOUT_HEADER_SUBTITLE = {
    PRESET_I18N: PRESET_I18N,
    PRESET_ADMIN_KO: PRESET_ADMIN_KO,
    isPresetMode: isPresetMode,
    isDirectActive: isDirectActive,
    presetI18nKey: presetI18nKey,
    adminPresetText: adminPresetText,
    resolveCheckoutText: resolveCheckoutText,
    headerSubtitleModeOptions: headerSubtitleModeOptions
  };
})(typeof window !== 'undefined' ? window : this);
