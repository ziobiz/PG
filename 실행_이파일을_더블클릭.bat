@echo off
chcp 65001 >nul
echo.
echo ========================================
echo   SQL 클립보드 복사 + pgAdmin4 안내
echo ========================================
echo.

set "SQLFILE=%~dp0pg-app\src\main\resources\db\V2_add_comp_excel_columns.sql"

if not exist "%SQLFILE%" (
    echo [오류] SQL 파일을 찾을 수 없습니다.
    echo 경로: %SQLFILE%
    pause
    exit /b 1
)

powershell -Command "Get-Content -Path '%SQLFILE%' -Raw -Encoding UTF8 | Set-Clipboard"
echo [완료] SQL이 클립보드에 복사되었습니다!
echo.
echo 다음 단계:
echo   1. pgAdmin4 실행
echo   2. ICOPAY - pgdev 선택 후 Query Tool 열기
echo   3. Ctrl+V 로 붙여넣기
echo   4. F5 로 실행
echo.
pause
