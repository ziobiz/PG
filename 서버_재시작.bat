@echo off
REM PG 서버 재시작 - 8080 포트 사용 중인 프로세스 종료 후 서버 실행

echo [8080 포트 정리 중...]
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
  echo PID %%a 종료 중...
  taskkill /PID %%a /F 2>nul
  timeout /t 2 /nobreak >nul
  goto :killed
)
:killed

cd /d "%~dp0pg-app"

echo.
echo [서버 시작]
echo 접속: http://localhost:8080/login.html
echo 로그인: admin / admin1!
echo.

java -Xmx512m -cp "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain bootRun --args="--spring.profiles.active=dev"

if errorlevel 1 (
  echo.
  echo Java를 찾을 수 없습니다. PATH에 Java 17이 있는지 확인하세요.
)
pause
