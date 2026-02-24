@echo off
chcp 65001 >nul
echo ========================================
echo PG 프로젝트 GitHub/GitLab 업로드 스크립트
echo ========================================
echo.

cd /d "%~dp0"

REM 1. lock 파일 제거 (이전 작업이 중단된 경우)
if exist ".git\index.lock" (
    echo [1] index.lock 제거 중...
    del /f ".git\index.lock" 2>nul
    timeout /t 2 >nul
)

REM 1-2. Cursor 사용 중이면 lock이 생길 수 있음. 실패 시 Cursor를 잠시 닫고 다시 실행하세요.

REM 2. Git 사용자 설정 (커밋에 필요, 없으면 기본값 설정)
git config user.name 2>nul | findstr /r "." >nul
if errorlevel 1 (
    echo [2] Git 사용자 설정 중... (기본값)
    git config user.name "PG-Developer"
    git config user.email "pg@local.dev"
)

REM 3. Git 상태 확인
echo.
echo [3] Git 상태 확인...
git status 2>nul
if errorlevel 1 (
    echo Git이 초기화되지 않았습니다. git init 실행...
    git init
)

REM 4. 파일 추가
echo.
echo [4] 파일 스테이징 중... (시간이 걸릴 수 있습니다)
git add .

REM 5. 커밋
echo.
echo [5] 초기 커밋 생성...
git commit -m "Initial commit: PG 솔루션 프로젝트"
if errorlevel 1 (
    echo.
    echo [오류] 커밋 실패. git status로 확인하세요.
    pause
    exit /b 1
)

REM 6. 원격 저장소 연결 안내
echo.
echo [성공] 초기 커밋이 생성되었습니다.
echo.
echo ========================================
echo [6] 다음 단계: GitHub/GitLab에 저장소 생성 후
echo ========================================
echo.
echo GitHub 사용 시:
echo   1. https://github.com/new 에서 새 저장소 생성 (이름 예: PG)
echo   2. 아래 명령어 실행 (사용자명과 저장소명을 본인 것으로 변경):
echo.
echo      git remote add origin https://github.com/사용자명/PG.git
echo      git branch -M main
echo      git push -u origin main
echo.
echo GitLab 사용 시:
echo   1. GitLab에서 새 프로젝트 생성
echo   2. 아래 명령어 실행 (URL을 본인 것으로 변경):
echo.
echo      git remote add origin https://gitlab.com/사용자명/PG.git
echo      git branch -M main
echo      git push -u origin main
echo.
echo ========================================
pause
