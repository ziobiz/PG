@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo.
echo [PG 통합관리자] 로컬 개발 서버 시작
echo 브라우저에서 http://localhost:8000 접속 후 login.html 로그인하세요.
echo API는 config.js 에서 http://localhost:8080 으로 설정됩니다.
echo 종료하려면 이 창에서 Ctrl+C 를 누르세요.
echo.
python -m http.server 8000
if errorlevel 1 (
  echo Python이 없거나 오류가 났습니다. Node.js로 시도합니다...
  npx -y serve -l 8000
)
pause
