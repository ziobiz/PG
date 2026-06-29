/**

 * URL 공개 결제 입력방식 — 관리자·결제창 공통 (Java UrlPayInputModeUtil 과 동일 규칙).

 */

(function (g) {

  'use strict';



  var FOLLOW_HQ = 'FOLLOW_HQ';

  var GENERAL = 'GENERAL';

  var TYPE_AA = 'TYPE_AA';

  var TYPE_BA = 'TYPE_BA';

  var TYPE_AN = 'TYPE_AN';

  var TYPE_AG = 'TYPE_AG';

  var TYPE_AF = 'TYPE_AF';

  var TYPE_AE = 'TYPE_AE';

  var TYPE_BN = 'TYPE_BN';

  var TYPE_BG = 'TYPE_BG';

  var TYPE_BF = 'TYPE_BF';

  var TYPE_BE = 'TYPE_BE';

  var TYPE_CN = 'TYPE_CN';



  var ALL_MODES = [
    FOLLOW_HQ,
    GENERAL, TYPE_AA, TYPE_BA, TYPE_AN, TYPE_AG, TYPE_AF, TYPE_AE,

    TYPE_BN, TYPE_BG, TYPE_BF, TYPE_BE, TYPE_CN

  ];



  /** @type {Record<string, string>} mode → pg-ui-i18n 키 */

  var DESC_KEYS = {

    GENERAL: '입력방식 일반 설명',
    FOLLOW_HQ: '입력방식 본사정책 따름 설명',

    TYPE_AA: '입력방식 AA 타입 설명',

    TYPE_BA: '입력방식 BA 타입 설명',

    TYPE_AN: '입력방식 AN 타입 설명',

    TYPE_AG: '입력방식 AG 타입 설명',

    TYPE_AF: '입력방식 AF 타입 설명',

    TYPE_AE: '입력방식 AE 타입 설명',

    TYPE_BN: '입력방식 BN 타입 설명',

    TYPE_BG: '입력방식 BG 타입 설명',

    TYPE_BF: '입력방식 BF 타입 설명',

    TYPE_BE: '입력방식 BE 타입 설명',

    TYPE_CN: '입력방식 CN 타입 설명'

  };



  function normalizeUrlPayInputMode(raw) {

    var mode = String(raw || GENERAL).trim().toUpperCase();

    if (mode === 'FOLLOW_HQ' || mode === 'DEFAULT' || mode === 'HQ') return FOLLOW_HQ;

    if (mode === 'AA' || mode === 'TYPEAA') return TYPE_AA;

    if (mode === 'BA' || mode === 'TYPEBA') return TYPE_BA;

    if (mode === 'AN' || mode === 'TYPEAN' || mode === 'A' || mode === 'TYPEA' || mode === 'TYPE_A') return TYPE_AN;

    if (mode === 'AG' || mode === 'TYPEAG') return TYPE_AG;

    if (mode === 'AF' || mode === 'TYPEAF') return TYPE_AF;

    if (mode === 'AE' || mode === 'TYPEAE') return TYPE_AE;

    if (mode === 'BN' || mode === 'TYPEBN' || mode === 'B' || mode === 'TYPEB' || mode === 'TYPE_B') return TYPE_BN;

    if (mode === 'BG' || mode === 'TYPEBG') return TYPE_BG;

    if (mode === 'BF' || mode === 'TYPEBF') return TYPE_BF;

    if (mode === 'BE' || mode === 'TYPEBE') return TYPE_BE;

    if (mode === 'CN' || mode === 'TYPECN' || mode === 'C' || mode === 'TYPEC' || mode === 'TYPE_C') return TYPE_CN;

    if (ALL_MODES.indexOf(mode) >= 0) return mode;

    return GENERAL;

  }



  function hidesCardBrandSelect(raw) {

    var mode = normalizeUrlPayInputMode(raw);

    return mode === TYPE_AN || mode === TYPE_AG || mode === TYPE_AF || mode === TYPE_AE

      || mode === TYPE_AA;

  }



  /** 관리자 폼·결제창과 동일한 표시 옵션 프리셋. GENERAL 은 null */

  function getUrlPayInputModePreset(raw) {

    var mode = normalizeUrlPayInputMode(raw);

    var base = {

      webPaymentUseYn: 'Y',

      defaultProductAmount: ''

    };

    if (mode === TYPE_AN || mode === TYPE_BN) {

      return Object.assign({}, base, {

        urlPayCompanyNameShowYn: 'N',

        urlPayLangMenuUseYn: 'N',

        urlPayProductNameUseYn: 'N',

        webPaymentHeaderLogoMode: 'DEFAULT',

        webPaymentHeaderSubtitleMode: 'DEFAULT'

      });

    }

    if (mode === TYPE_AG || mode === TYPE_BG) {

      return Object.assign({}, base, {

        urlPayCompanyNameShowYn: 'N',

        urlPayLangMenuUseYn: 'N',

        urlPayProductNameUseYn: 'Y',

        webPaymentHeaderLogoMode: 'DEFAULT',

        webPaymentHeaderSubtitleMode: 'DEFAULT'

      });

    }

    if (mode === TYPE_AF || mode === TYPE_BF) {

      return Object.assign({}, base, {

        urlPayCompanyNameShowYn: 'N',

        urlPayLangMenuUseYn: 'N',

        urlPayProductNameUseYn: 'Y',

        webPaymentHeaderLogoMode: 'DISABLED',

        webPaymentHeaderSubtitleMode: 'DISABLED'

      });

    }

    if (mode === TYPE_AE || mode === TYPE_BE) {

      return Object.assign({}, base, {

        urlPayCompanyNameShowYn: 'N',

        urlPayLangMenuUseYn: 'N',

        urlPayProductNameUseYn: 'N',

        webPaymentHeaderLogoMode: 'DISABLED',

        webPaymentHeaderSubtitleMode: 'DISABLED'

      });

    }

    if (mode === TYPE_AA) {

      return Object.assign({}, base, {

        urlPayCompanyNameShowYn: 'N',

        urlPayLangMenuUseYn: 'Y',

        urlPayProductNameUseYn: 'N',

        webPaymentHeaderLogoMode: 'DISABLED',

        webPaymentHeaderSubtitleMode: 'DISABLED'

      });

    }

    if (mode === TYPE_BA) {

      return Object.assign({}, base, {

        urlPayCompanyNameShowYn: 'N',

        urlPayLangMenuUseYn: 'Y',

        urlPayProductNameUseYn: 'N',

        webPaymentHeaderLogoMode: 'DISABLED',

        webPaymentHeaderSubtitleMode: 'DISABLED'

      });

    }

    if (mode === TYPE_CN) {

      return Object.assign({}, base, {

        urlPayCompanyNameShowYn: 'Y',

        urlPayLangMenuUseYn: 'Y',

        urlPayProductNameUseYn: 'Y',

        webPaymentHeaderLogoMode: 'DEFAULT',

        webPaymentHeaderSubtitleMode: 'DEFAULT'

      });

    }

    return null;

  }



  function getUrlPayInputModeDescriptionKey(raw) {

    var mode = normalizeUrlPayInputMode(raw);

    return DESC_KEYS[mode] || DESC_KEYS.GENERAL;

  }



  function isUrlPayInputModeControlled(raw) {

    return getUrlPayInputModePreset(raw) != null;

  }



  g.PG_URL_PAY_INPUT_MODE = {

    GENERAL: GENERAL,
    FOLLOW_HQ: FOLLOW_HQ,

    TYPE_AA: TYPE_AA,

    TYPE_BA: TYPE_BA,

    TYPE_AN: TYPE_AN,

    TYPE_AG: TYPE_AG,

    TYPE_AF: TYPE_AF,

    TYPE_AE: TYPE_AE,

    TYPE_BN: TYPE_BN,

    TYPE_BG: TYPE_BG,

    TYPE_BF: TYPE_BF,

    TYPE_BE: TYPE_BE,

    TYPE_CN: TYPE_CN,

    ALL_MODES: ALL_MODES,

    normalize: normalizeUrlPayInputMode,

    getPreset: getUrlPayInputModePreset,

    getDescriptionKey: getUrlPayInputModeDescriptionKey,

    isControlled: isUrlPayInputModeControlled,

    hidesCardBrandSelect: hidesCardBrandSelect

  };

})(window);

