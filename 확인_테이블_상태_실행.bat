@echo off
chcp 65001 >nul
echo.
echo ========================================
echo   DB 상태 확인 SQL 클립보드 복사
echo ========================================
echo.

set "SQLFILE=%~dp0pg-app\src\main\resources\db\확인_테이블_상태.sql"

if not exist "%SQLFILE%" (
    echo [오류] 파일을 찾을 수 없습니다: %SQLFILE%
    pause
    exit /b 1
)

powershell -Command "Get-Content -Path '%SQLFILE%' -Raw -Encoding UTF8 | Set-Clipboard"
echo [완료] 확인용 SQL이 클립보드에 복사되었습니다!
echo.
echo 다음: pgAdmin4 - pgdev Query Tool - Ctrl+V - F5
echo 결과를 확인한 뒤 알려주세요.
echo.
pause
