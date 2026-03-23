@echo off
REM PG 통합관리자 - Java로 직접 실행 (gradlew 대신)
REM JAVA_HOME 문제 시 이 파일 사용

cd /d "%~dp0pg-app"

echo [로컬 개발 모드] H2 파일 DB ^(pg-app\data^) + site 정적파일
echo 접속: http://localhost:8080/login.html
echo 로그인: admin / admin1!
echo.

java -Xmx512m -cp "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain bootRun --args="--spring.profiles.active=dev"

if errorlevel 1 (
    echo.
    echo Java를 찾을 수 없습니다. PATH에 Java 17이 있는지 확인하세요.
    echo 또는 JAVA_HOME을 설정한 뒤 gradlew.bat을 사용하세요.
)
pause
