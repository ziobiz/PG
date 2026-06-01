/**
 * 비활성카드등록 — 카드 브랜드별 접두(BIN)·자릿수 검증
 */
(function (global) {
  'use strict';

  function digitsOnly(s) {
    var out = '';
    var x = String(s == null ? '' : s);
    for (var i = 0; i < x.length; i++) {
      var c = x.charAt(i);
      if (c >= '0' && c <= '9') out += c;
    }
    return out;
  }

  /** @returns {'amex'|'diners14'|'seg16'} */
  function layoutForBrand(brand) {
    var b = String(brand || '').toUpperCase();
    if (b === 'AMEX') return 'amex';
    if (b === 'DINERS') return 'diners14';
    return 'seg16';
  }

  function isAmexPrefix(pan) {
    return pan.indexOf('34') === 0 || pan.indexOf('37') === 0;
  }

  function isVisaPrefix(pan) {
    return pan.charAt(0) === '4';
  }

  function isMastercardPrefix(pan) {
    if (pan.length < 2) return false;
    if (/^5[1-5]/.test(pan)) return true;
    if (pan.length >= 6) {
      var p6 = parseInt(pan.substring(0, 6), 10);
      if (!isNaN(p6) && p6 >= 222100 && p6 <= 272099) return true;
    }
    if (pan.length >= 4) {
      var p4 = parseInt(pan.substring(0, 4), 10);
      if (!isNaN(p4) && p4 >= 2221 && p4 <= 2720) return true;
    }
    return false;
  }

  function isJcbPrefix(pan) {
    if (pan.length < 4) return false;
    var p4 = parseInt(pan.substring(0, 4), 10);
    return !isNaN(p4) && p4 >= 3528 && p4 <= 3589;
  }

  function isDinersPrefix(pan) {
    if (pan.length < 2) return false;
    var p2 = pan.substring(0, 2);
    return p2 === '36' || p2 === '38' || p2 === '39';
  }

  function isDiscoverPrefix(pan) {
    if (pan.indexOf('6011') === 0) return true;
    if (pan.indexOf('65') === 0) return true;
    if (pan.length >= 3) {
      var p3 = pan.substring(0, 3);
      if (p3 >= '644' && p3 <= '649') return true;
    }
    if (pan.length >= 6) {
      var p6 = parseInt(pan.substring(0, 6), 10);
      if (!isNaN(p6) && p6 >= 622126 && p6 <= 622925) return true;
    }
    return false;
  }

  function isUnionPayPrefix(pan) {
    return pan.indexOf('62') === 0;
  }

  function isDomesticKrPrefix(pan) {
    return pan.charAt(0) === '9';
  }

  function prefixMatchesBrand(brand, pan) {
    var b = String(brand || '').toUpperCase();
    if (b === 'OTHER') return true;
    if (!pan || pan.length < 1) return true;
    switch (b) {
      case 'VISA': return isVisaPrefix(pan);
      case 'MASTERCARD': return isMastercardPrefix(pan);
      case 'AMEX': return isAmexPrefix(pan);
      case 'DINERS': return isDinersPrefix(pan);
      case 'JCB': return isJcbPrefix(pan);
      case 'DISCOVER': return isDiscoverPrefix(pan);
      case 'UNIONPAY': return isUnionPayPrefix(pan);
      case 'DOMESTIC_KR': return isDomesticKrPrefix(pan);
      default: return true;
    }
  }

  function requiredLength(brand) {
    var b = String(brand || '').toUpperCase();
    switch (b) {
      case 'AMEX': return 15;
      case 'DINERS': return 14;
      case 'VISA': return null;
      case 'OTHER': return null;
      default: return 16;
    }
  }

  function lengthMatchesBrand(brand, pan) {
    var b = String(brand || '').toUpperCase();
    var len = pan.length;
    if (b === 'OTHER') return len >= 13 && len <= 16;
    if (b === 'VISA') return len === 13 || len === 16;
    if (b === 'UNIONPAY') return len === 16;
    var need = requiredLength(b);
    return need != null && len === need;
  }

  function collectPanFromRow(row) {
    if (!row) return '';
    var parts = [];
    row.querySelectorAll('.ops-ic-pan-seg').forEach(function (inp) {
      parts.push(digitsOnly(inp.value));
    });
    return parts.join('');
  }

  function validateBrandPan(brand, pan) {
    var b = String(brand || '').toUpperCase();
    if (!b) return { ok: false, code: 'BRAND', msgKey: '카드 종류를 먼저 선택하세요.' };
    if (b === 'OTHER') {
      if (!pan || pan.length < 13) {
        return { ok: false, code: 'LEN', msgKey: '기타 카드번호는 13자리 이상 입력하세요.' };
      }
      if (pan.length > 16) {
        return { ok: false, code: 'LEN', msgKey: '기타 카드번호는 16자리 이하로 입력하세요.' };
      }
      return { ok: true, pan: pan, brand: b };
    }
    if (!pan || pan.length < 1) {
      return { ok: false, code: 'EMPTY', msgKey: '카드번호를 입력하세요.' };
    }
    var need = requiredLength(b);
    if (b === 'VISA') {
      if (pan.length !== 13 && pan.length !== 16) {
        return { ok: false, code: 'LEN', msgKey: 'Visa 카드번호는 13자리 또는 16자리입니다.' };
      }
    } else if (need != null && pan.length !== need) {
      return { ok: false, code: 'LEN', msgKey: brandLengthHintKey(b) };
    }
    if (!prefixMatchesBrand(b, pan)) {
      return { ok: false, code: 'MISMATCH', msgKey: '선택한 카드 종류와 카드번호 형식(접두·자릿수)이 일치하지 않습니다.' };
    }
    return { ok: true, pan: pan, brand: b };
  }

  function brandLengthHintKey(brand) {
    var b = String(brand || '').toUpperCase();
    switch (b) {
      case 'AMEX': return 'AMEX 카드번호 15자리를 모두 입력하세요.';
      case 'DINERS': return 'Diners Club 카드번호 14자리를 모두 입력하세요.';
      case 'UNIONPAY': return 'UnionPay 카드번호는 16자리입니다. (17~19자리는 기타를 선택하세요.)';
      case 'DOMESTIC_KR': return '국내 카드번호 16자리를 모두 입력하세요.';
      default: return '카드번호 16자리를 모두 입력하세요.';
    }
  }

  /** 입력 중 경고 표시 여부 (최소 자릿수 이상일 때) */
  function liveMismatch(brand, pan) {
    var b = String(brand || '').toUpperCase();
    if (!b || b === 'OTHER' || !pan) return null;
    var minCheck = 4;
    if (b === 'AMEX') minCheck = 2;
    if (pan.length < minCheck) return null;
    if (!prefixMatchesBrand(b, pan)) {
      return '선택한 카드 종류와 카드번호 형식(접두·자릿수)이 일치하지 않습니다.';
    }
    if (b === 'VISA' && pan.length >= 13 && pan.length !== 13 && pan.length !== 16) {
      return 'Visa 카드번호는 13자리 또는 16자리입니다.';
    }
    var need = requiredLength(b);
    if (need != null && pan.length >= need && pan.length !== need) {
      return brandLengthHintKey(b);
    }
    if (b === 'UNIONPAY' && pan.length > 16) {
      return 'UnionPay 17~19자리는 기타를 선택하세요.';
    }
    return null;
  }

  global.OPS_INACTIVE_CARD_BRAND = {
    digitsOnly: digitsOnly,
    layoutForBrand: layoutForBrand,
    collectPanFromRow: collectPanFromRow,
    validateBrandPan: validateBrandPan,
    liveMismatch: liveMismatch,
    brandLengthHintKey: brandLengthHintKey,
    prefixMatchesBrand: prefixMatchesBrand,
    lengthMatchesBrand: lengthMatchesBrand
  };
})(typeof window !== 'undefined' ? window : this);
