/**
 * 공통 UI: 인라인 배너·모달 (alert/prompt 대체용)
 */
(function (global) {
  'use strict';

  global.PG_UI = global.PG_UI || {};

  /**
   * @param {HTMLElement} root - 배너 조상(탭 pane 등)
   * @param {string} bannerId - # 제외 id
   * @param {'success'|'danger'|'warning'|'info'} variant
   * @param {string} message
   * @param {number} [autoHideMs] - 0이면 자동 숨김 없음
   */
  global.PG_UI.showBanner = function (root, bannerId, variant, message, autoHideMs) {
    if (!root || !bannerId) return;
    var box = root.querySelector('#' + bannerId);
    if (!box) return;
    var textEl = box.querySelector('[data-pg-banner-text]');
    if (textEl) textEl.textContent = message || '';
    box.classList.remove('alert-success', 'alert-danger', 'alert-warning', 'alert-info', 'alert-primary', 'd-none');
    box.classList.add('alert', 'alert-' + (variant || 'info'));
    if (box._pgHideTimer) clearTimeout(box._pgHideTimer);
    if (autoHideMs && autoHideMs > 0) {
      box._pgHideTimer = setTimeout(function () {
        box.classList.add('d-none');
      }, autoHideMs);
    }
  };

  global.PG_UI.hideBanner = function (root, bannerId) {
    if (!root || !bannerId) return;
    var box = root.querySelector('#' + bannerId);
    if (box) box.classList.add('d-none');
  };

  global.PG_UI.openModal = function (modalEl) {
    if (!modalEl || !global.bootstrap || !global.bootstrap.Modal) return;
    global.bootstrap.Modal.getOrCreateInstance(modalEl).show();
  };

  global.PG_UI.closeModal = function (modalEl) {
    if (!modalEl || !global.bootstrap || !global.bootstrap.Modal) return;
    var inst = global.bootstrap.Modal.getInstance(modalEl);
    if (inst) inst.hide();
  };

  function escHtml(s) {
    return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
  }

  /**
   * 결제내역 그리드 행·Status 뱃지 톤 — 내부 status 우선, 없으면 chillPaymentStatus·구분.
   * @returns {'success'|'cancel'|'void'|'refund'|'fail'|'pending'|'neutral'}
   */
  global.PG_UI.resolvePayRowTone = function (row) {
    if (!row || typeof row !== 'object') return 'neutral';
    var st = row.status != null ? String(row.status).trim() : '';
    if (st === '10') return 'success';
    if (st === '20') return 'cancel';
    if (st === '21' || st === '22') return 'void';
    if (st === '30' || st === '31') return 'refund';
    if (st === '99' || st === 'F0' || st.toLowerCase() === 'f0') return 'fail';
    if (st === '08') return 'pending';
    var div = String(row.payDivNm || '').trim();
    if (div === '인증대기') return 'pending';
    if (div === '환불') return 'refund';
    if (div === '실패') return 'fail';
    if (div === '취소') return 'cancel';
    if (div.indexOf('무효') >= 0) return 'void';
    var lab = row.chillPaymentStatus != null ? String(row.chillPaymentStatus).trim() : '';
    if (!lab || lab === '-') return 'neutral';
    if (lab === '성공') return 'success';
    if (lab === '취소') return 'cancel';
    if (lab === '실패' || lab === '오류') return 'fail';
    if (lab.indexOf('무효') >= 0) return 'void';
    if (lab.indexOf('환불') >= 0) return 'refund';
    if (lab === '요청' || lab.indexOf('대기') >= 0) return 'pending';
    if (/^(paid|success|complete|authorized)$/i.test(lab)) return 'success';
    if (/cancel/i.test(lab) && lab.indexOf('무효') === -1) return 'cancel';
    if (/refund/i.test(lab)) return 'refund';
    if (/fail|error|declin/i.test(lab)) return 'fail';
    if (/void/i.test(lab)) return 'void';
    if (/pending|processing|wait|request/i.test(lab)) return 'pending';
    return 'neutral';
  };

  /**
   * 통합내역(ChillPay 실시간) — Status·PaymentStatus 숫자·영문 토큰.
   * @returns {'success'|'cancel'|'void'|'refund'|'fail'|'pending'|'neutral'}
   */
  global.PG_UI.resolveChillTrRowTone = function (row) {
    if (!row || typeof row !== 'object') return 'neutral';
    var raw = row.status != null ? String(row.status).trim() : '';
    if (!raw) return 'neutral';
    var low = raw.toLowerCase();
    if (raw === '0') return 'success';
    if (raw === '2') return 'cancel';
    if (raw === '1' || raw === '3' || raw === '4') return 'fail';
    if (/voided|void|emailvoid|manual void|email void|무효|이메일무효/.test(low)) return 'void';
    if (/refund/.test(low)) return 'refund';
    if (/cancel|cancelled|canceled/.test(low)) return 'cancel';
    if (/fail|error|declin|오류/.test(low)) return 'fail';
    if (/paid|success|complete|authorized|settled|성공/.test(low)) return 'success';
    if (/pending|wait|request|processing|authorize|요청|대기/.test(low)) return 'pending';
    return 'neutral';
  };

  /** 결제내역·통합내역 Status 열 — 행 톤과 동일한 뱃지 색 */
  global.PG_UI.payGridStatusBadge = function (rawText, tone) {
    var t = tone && tone !== 'neutral' ? tone : 'neutral';
    var inner = escHtml(rawText);
    if (!inner) inner = '—';
    return '<span class="pay-grid-status-badge pay-grid-status-badge--' + t + '">' + inner + '</span>';
  };
})(typeof window !== 'undefined' ? window : this);
