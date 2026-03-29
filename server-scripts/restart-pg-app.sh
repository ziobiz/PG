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
#
# 동시에 스크립트를 여러 번 돌리면 Java 프로세스가 둘 이상 떠서 로그가 섞이고,
# 하나는 Started 직후 pkill 로 죽거나 8080 충돌로 ApplicationContext 가 실패할 수 있습니다.
# → flock 으로 한 번에 하나만 실행 + 8080 이 비어 있을 때까지 대기합니다.

# 스크립트가 있는 디렉터리 = pg-app 루트 (build/libs 가 그 아래)
PG_APP_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$PG_APP_DIR/build/libs/pg-app-0.0.1-SNAPSHOT.jar"
LOG_FILE="${PG_APP_DIR}/pg-app.log"
LOCK_FILE="${PG_APP_DIR}/.restart-pg-app.lock"

# ─── ★ 아래만 서버 환경에 맞게 수정 (nano 로 편집) ───────────────
DB_HOST="localhost"
# PostgreSQL "pgdev" 이름 — Linux root 와 다름. 보통 postgres, pgadmin 등.
DB_USER="pgadmin"
# PostgreSQL SCRAM: 비밀번호 없으면 기동 실패합니다. 반드시 실제 값으로 변경.
DB_PASSWORD="line2025!@"
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

# 동시 재시작 방지: 이전 실행이 포트 대기(최대 60초) 중이면 즉시 실패하면 운영이 답답함.
# → 최대 3분까지 잠금 해제를 기다린 뒤 진행. 그래도 못 잡으면 안내 후 종료.
exec 200>"$LOCK_FILE" || true
if command -v flock >/dev/null 2>&1; then
  echo "   (다른 재시작이 있으면 최대 180초 대기 후 진행합니다.)"
  if ! flock -w 180 200; then
    echo "오류: 3분 안에 재시작 잠금을 얻지 못했습니다."
    echo "  ps aux | grep restart-pg-app   로 다른 실행이 있는지 확인하거나,"
    echo "  pg-app Java 가 멈춰 잠금만 남은 경우: 해당 셸/프로세스 종료 후 다시 실행하세요."
    exit 1
  fi
fi

port_8080_busy() {
  if command -v ss >/dev/null 2>&1; then
    ss -tlnp 2>/dev/null | grep -q ':8080'
    return $?
  fi
  if command -v netstat >/dev/null 2>&1; then
    netstat -tlnp 2>/dev/null | grep -q ':8080'
    return $?
  fi
  return 1
}

echo "1) 기존 pg-app 프로세스 종료..."
pkill -f "pg-app-0.0.1-SNAPSHOT.jar" 2>/dev/null || true
sleep 2
# pkill 이 실패했는데도 8080 이 열려 있으면(예전 실행 방식) PID 로 강제 종료
if command -v ss >/dev/null 2>&1; then
  pid=$(ss -tlnp 2>/dev/null | grep ':8080' | sed -n 's/.*pid=\([0-9]*\).*/\1/p' | head -1)
  if [[ -n "$pid" ]]; then
    echo "   8080 사용 중 PID=$pid 종료 시도 (SIGTERM)..."
    kill "$pid" 2>/dev/null || true
    sleep 2
  fi
fi

echo "   8080 포트 해제 대기(최대 60초, 지연 시 SIGKILL)..."
for _w in $(seq 1 60); do
  if ! port_8080_busy; then
    break
  fi
  # Spring 종료 훅이 30초 타임아웃으로 길어질 수 있음 → 25초 넘게 점유 시 강제 종료
  if [[ "$_w" -eq 25 ]] && command -v ss >/dev/null 2>&1; then
    pid=$(ss -tlnp 2>/dev/null | grep ':8080' | sed -n 's/.*pid=\([0-9]*\).*/\1/p' | head -1)
    if [[ -n "$pid" ]]; then
      echo "   경고: 25초 경과 후에도 8080 점유 → PID=$pid 에 SIGKILL (종료 로그에 CNF 등이 찍힐 수 있음)"
      kill -9 "$pid" 2>/dev/null || true
      sleep 1
    fi
  fi
  sleep 1
done
if port_8080_busy; then
  echo "경고: 60초 후에도 8080 이 사용 중입니다. ss -tlnp | grep 8080 로 PID 확인 후 kill 하세요."
fi

echo "2) pg-app 기동 (prod)..."
echo "   첫 기동은 DB·JPA 때문에 약 1분 걸릴 수 있습니다."
echo "   완료 전에 이 스크립트를 다시 실행하지 마세요."
export DB_HOST DB_USER DB_PASSWORD
export SPRING_PROFILES_ACTIVE=prod
nohup java -jar "$JAR" --spring.profiles.active=prod --server.port=8080 >>"$LOG_FILE" 2>&1 &
echo "   로그: tail -f $LOG_FILE"
echo "   성공 한 줄: grep 'Started PgAppApplication' $LOG_FILE | tail -1"
echo "   (프롬프트가 바로 돌아오는 것은 정상입니다. java 는 백그라운드에서 기동 중입니다.)"
