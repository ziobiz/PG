(function () {
  'use strict';

  function openModal(id) {
    var modal = document.getElementById(id);
    if (modal) {
      modal.setAttribute('aria-hidden', 'false');
      document.body.style.overflow = 'hidden';
    }
  }

  function closeModal(id) {
    var modal = document.getElementById(id);
    if (modal) {
      modal.setAttribute('aria-hidden', 'true');
      document.body.style.overflow = '';
    }
  }

  document.querySelectorAll('[data-close]').forEach(function (el) {
    el.addEventListener('click', function () {
      var id = this.getAttribute('data-close');
      if (id) closeModal(id);
    });
  });

  var openChangePwModal = document.getElementById('openChangePwModal');
  if (openChangePwModal) {
    openChangePwModal.addEventListener('click', function () {
      openModal('modalChangePassword');
    });
  }

  var btnSavePassword = document.getElementById('btnSavePassword');
  if (btnSavePassword) {
    btnSavePassword.addEventListener('click', function () {
      var oldPw = document.getElementById('modalOldPassword');
      var newPw = document.getElementById('modalNewPassword');
      if (!oldPw || !newPw) return;
      var oldVal = oldPw.value.trim();
      var newVal = newPw.value;
      if (!oldVal || !newVal) {
        var msg = document.getElementById('modalAlertMessage');
        if (msg) msg.textContent = '기존 비밀번호와 변경 비밀번호를 모두 입력하세요.';
        openModal('modalAlert');
        return;
      }
      if (newVal.length < 4) {
        var msg = document.getElementById('modalAlertMessage');
        if (msg) msg.textContent = '변경 비밀번호는 4자 이상이어야 합니다.';
        openModal('modalAlert');
        return;
      }
      var username = document.getElementById('username');
      if (username && newVal === username.value.trim() + '1!') {
        var msg = document.getElementById('modalAlertMessage');
        if (msg) msg.textContent = '아이디+1! 형태의 비밀번호는 사용할 수 없습니다.';
        openModal('modalAlert');
        return;
      }
      closeModal('modalChangePassword');
      oldPw.value = '';
      newPw.value = '';
      var alertMsg = document.getElementById('modalAlertMessage');
      if (alertMsg) alertMsg.textContent = '비밀번호 변경 요청이 접수되었습니다. (백엔드 연동 후 실제 변경 처리됩니다)';
      openModal('modalAlert');
    });
  }
})();
