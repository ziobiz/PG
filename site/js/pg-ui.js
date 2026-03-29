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
})(typeof window !== 'undefined' ? window : this);
