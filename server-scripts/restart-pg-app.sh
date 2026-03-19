#!/bin/bash
# pg-app 재시작 (Linux 서버용)
#
# 배치: 이 파일을 pg-app **프로젝트 루트**에 둡니다.
#   예) /home/ftpuser/pg-app/restart-pg-app.sh
#       → JAR 경로: /home/ftpuser/pg-app/build/libs/pg-app-0.0.1-SNAPSHOT.jar
#
# 사용법:
#   chmod +x restart-pg-app.sh
#   nano restart-pg-app.sh   # 아래 ★ 항목만 본인 DB에 맞게 수정
#   ./restart-pg-app.sh
#
# 주의: 실행 순서는 스크립트가 처리합니다. 수동 실행 시에는 반드시
#   pkill → sleep → (환경변수) → nohup 순서를 지키세요.

# 스크립트가 있는 디렉터리 = pg-app 루트 (build/libs 가 그 아래)
PG_APP_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$PG_APP_DIR/build/libs/pg-app-0.0.1-SNAPSHOT.jar"
LOG_FILE="${PG_APP_DIR}/pg-app.log"

# ─── ★ 아래만 서버 환경에 맞게 수정 (nano 로 편집) ───────────────
DB_HOST="localhost"
# PostgreSQL "역할(사용자)" 이름 — Linux root 와 다름. 보통 postgres, pgadmin 등.
DB_USER="pgadmin"
# PostgreSQL SCRAM: 비밀번호 없으면 기동 실패합니다. 반드시 실제 값으로 변경.
DB_PASSWORD="비밀번호"
# ───────────────────────────────────────────────────────────────

if [[ ! -f "$JAR" ]]; then
  echo "오류: JAR를 찾을 수 없습니다: $JAR"
  echo "  (스크립트를 pg-app 루트에 두었는지, build/libs 에 jar 가 있는지 확인)"
  exit 1
fi

if [[ -z "$DB_PASSWORD" || "$DB_PASSWORD" == "비밀번호" ]]; then
  echo "오류: restart-pg-app.sh 안의 DB_PASSWORD 를 실제 DB 비밀번호로 바꾸세요."
  exit 1
fi

cd "$PG_APP_DIR"

echo "1) 기존 pg-app 프로세스 종료..."
pkill -f "pg-app-0.0.1-SNAPSHOT.jar" 2>/dev/null || true
sleep 2
# pkill 이 실패했는데도 8080 이 java 이면(예전 실행 방식) PID 로 강제 종료
if command -v ss >/dev/null 2>&1; then
  pid=$(ss -tlnp 2>/dev/null | grep ':8080' | sed -n 's/.*pid=\([0-9]*\).*/\1/p' | head -1)
  if [[ -n "$pid" ]]; then
    echo "   8080 사용 중 PID=$pid 종료 시도..."
    kill "$pid" 2>/dev/null || true
    sleep 2
  fi
fi

echo "2) pg-app 기동 (prod)..."
export DB_HOST DB_USER DB_PASSWORD
export SPRING_PROFILES_ACTIVE=prod
nohup java -jar "$JAR" --spring.profiles.active=prod --server.port=8080 >>"$LOG_FILE" 2>&1 &
echo "   로그: tail -f $LOG_FILE"
echo "   완료 여부: grep Started $LOG_FILE | tail -1"
