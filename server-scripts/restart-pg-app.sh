#!/bin/bash
# pg-app 재시작 스크립트
# 사용법: 이 파일을 pg-app 폴더에 넣고 실행
#   chmod +x restart-pg-app.sh
#   ./restart-pg-app.sh

# 스크립트가 있는 폴더 = pg-app 폴더
PG_APP_DIR="$(cd "$(dirname "$0")" && pwd)"

# ★ DB 비밀번호를 본인 환경에 맞게 수정 ★
DB_PASSWORD="비밀번호"

cd "$PG_APP_DIR" || exit 1

echo "1. 기존 pg-app 프로세스 종료 중..."
pkill -f "pg-app-0.0.1-SNAPSHOT.jar" 2>/dev/null || true
sleep 3

echo "2. pg-app 재시작 중..."
export DB_HOST=localhost
export DB_USER=pgadmin
export DB_PASSWORD="$DB_PASSWORD"
nohup java -jar build/libs/pg-app-0.0.1-SNAPSHOT.jar --server.port=8080 > pg-app.log 2>&1 &

echo "3. 완료. 잠시 후 https://api.icopay.co.kr 에서 확인하세요."
