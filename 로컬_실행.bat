@echo off
REM PG 통합관리자 - 로컬 개발 실행 (H2 파일 DB=pg-app/data, PostgreSQL 불필요)
REM 프로젝트 루트(PG)에서 실행

cd /d "%~dp0pg-app"

REM JAVA_HOME 미설정 시 자동 감지 (java가 PATH에 있으면)
if not defined JAVA_HOME (
  for /f "tokens=*" %%i in ('where java 2^>nul') do (
    for %%j in ("%%~dpi..") do set "JAVA_HOME=%%~fj"
    goto :java_ok
  )
  echo JAVA_HOME를 설정하세요. (예: C:\Program Files\Eclipse Adoptium\jdk-17)
  pause
  exit /b 1
)
:java_ok

echo [로컬 개발 모드] H2 DB + site 정적파일 함께 서빙
echo 접속: http://localhost:8080/login.html
echo 로그인: admin / admin1!
echo.

call gradlew.bat bootRun --args="--spring.profiles.active=dev"

pause
