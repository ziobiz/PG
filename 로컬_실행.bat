@echo off
REM PG 통합관리자 - 로컬 개발 실행 (H2 인메모리 DB, PostgreSQL 불필요)
REM 프로젝트 루트(PG)에서 실행

cd /d "%~dp0pg-app"

echo [로컬 개발 모드] H2 DB + site 정적파일 함께 서빙
echo 접속: http://localhost:8080/login.html
echo 로그인: admin / admin1!
echo.

call gradlew.bat bootRun --args="--spring.profiles.active=dev"

pause
