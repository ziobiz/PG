(function () {
  'use strict';

  // 모달 열기
  function openModal(id) {
    var modal = document.getElementById(id);
    if (modal) {
      modal.setAttribute('aria-hidden', 'false');
      document.body.style.overflow = 'hidden';
    }
  }

  // 모달 닫기
  function closeModal(id) {
    var modal = document.getElementById(id);
    if (modal) {
      modal.setAttribute('aria-hidden', 'true');
      document.body.style.overflow = '';
    }
  }

  // [data-close] 클릭 시 해당 모달 닫기
  document.querySelectorAll('[data-close]').forEach(function (el) {
    el.addEventListener('click', function () {
      var id = this.getAttribute('data-close');
      if (id) closeModal(id);
    });
  });

  // 로그인 폼 제출 (UI만 구현, 실제 전송은 백엔드 연동 시 구현)
  var loginForm = document.getElementById('loginForm');
  if (loginForm) {
    loginForm.addEventListener('submit', function (e) {
      e.preventDefault();
      var userId = document.getElementById('userId').value.trim();
      var password = document.getElementById('password').value;
      if (!userId) {
        alert('아이디를 입력하세요.');
        return;
      }
      if (!password) {
        alert('비밀번호를 입력하세요.');
        return;
      }
      // TODO: OTP 검증, 실제 로그인 API 호출
      console.log('Login submit (UI only)', { userId: userId });
      alert('로그인 요청 (UI만 동작, 백엔드 연동 필요)');
    });
  }

  // 초기비밀번호 변경 모달 - 저장 버튼
  var btnSavePassword = document.getElementById('btnSavePassword');
  if (btnSavePassword) {
    btnSavePassword.addEventListener('click', function () {
      var oldPw = document.getElementById('modalOldPassword').value;
      var newPw = document.getElementById('modalNewPassword').value;
      if (!oldPw || !newPw) {
        alert('기존 비밀번호와 변경 비밀번호를 모두 입력하세요.');
        return;
      }
      // TODO: 비밀번호 변경 API 호출
      console.log('Password change (UI only)');
      closeModal('modalChangePassword');
      document.getElementById('modalOldPassword').value = '';
      document.getElementById('modalNewPassword').value = '';
    });
  }

  // 초기비밀번호 변경 모달 열기
  var openChangePwModal = document.getElementById('openChangePwModal');
  if (openChangePwModal) {
    openChangePwModal.addEventListener('click', function () {
      openModal('modalChangePassword');
    });
  }
})();
