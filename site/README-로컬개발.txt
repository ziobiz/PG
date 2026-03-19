■ otlpay.cafe24.com 연결 거부 시 (ERR_CONNECTION_REFUSED)

서버가 꺼져 있거나 방화벽/호스팅 설정 문제일 수 있습니다.
카페24 관리자에서 호스팅 상태·도메인 연결을 확인하세요.

당장 개발을 이어가려면 로컬에서 실행하세요.

1) "로컬 서버 실행.bat" 더블클릭
2) 브라우저에서 http://localhost:8000 열기
3) http://localhost:8000/login.html 로 로그인 화면 접속

- API 주소는 config.js에서 localhost일 때 자동으로 http://localhost:8080 으로 설정됩니다.
- 백엔드(pg-app)를 8080 포트로 실행해 두면 로그인·API 호출이 동작합니다.
- 백엔드 없이 화면만 보려면 로그인 시 "API 서버를 확인하세요" 나오면 정상이며, 같은 PC에서 백엔드를 띄우면 됩니다.

■ 나중에 서버(otlpay.cafe24.com) 복구 후

카페24는 보통 정적 파일(HTML/JS/CSS)만 제공합니다. pg-app API(/api/...)는 별도 서버에서 돌아갑니다.
→ site/js/config.js 의 CAFE24_STATIC_SITE_API 를 실제 API 주소(예: https://api.icopay.co.kr)로 두고,
  pg-app application.yml 의 app.cors.allowed-origins 에 otlpay.cafe24.com 이 포함되어 있어야
  로그인·브랜딩(로고·로그인 배경)·메뉴 API가 동작합니다.

임시로 다른 API를 쓰려면 주소창에 ?api=https://API주소 한 번 열면 localStorage 에 저장됩니다.
