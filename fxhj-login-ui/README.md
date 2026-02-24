# FXHJ 로그인 UI 복제본

**참고 사이트**: https://fxhj.soonpay.co.kr/login (ONTHELINE FXHJ, 뷰어 전용)

## 구성

- **index.html** – 로그인 페이지 마크업 (아이디, 비밀번호, 변경 비밀번호, 인증번호, OTP번호, 로그인 버튼, 사칭 피해 주의 안내, 초기비밀번호 변경·확인·알림 모달)
- **styles.css** – 레이아웃·폼·모달 스타일
- **script.js** – 모달 열기/닫기, 로그인 폼 제출(UI만), 초기비밀번호 변경 모달 트리거

## 실행

브라우저에서 `index.html` 을 열거나, 로컬 웹 서버로 해당 폴더를 서빙하면 됩니다.

```bash
# 예: Python
cd fxhj-login-ui
python -m http.server 8080
# http://localhost:8080
```

## 참고

- 실제 로그인·OTP·비밀번호 변경은 백엔드 API 연동 후 구현해야 합니다.
- 원본 사이트와 100% 동일한 픽셀/색상은 캡처 기준으로 추가 조정이 필요할 수 있습니다.
