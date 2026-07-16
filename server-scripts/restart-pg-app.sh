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
# 주의: 다운타임을 줄이기 위해 JPAY 스크립트 준비는 **종료 전**에 하고,
#   그 다음 pkill → 포트 해제 → nohup java 순서로 진행합니다.
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
# 태국은행 환율(URL DISPLAY·FX 자동): 포털「Average Exchange Rate」v2 구독 시 예시(주석 해제 후 값 입력)
# export BOT_THAILAND_BASE_URL='https://gateway.api.bot.or.th/Stat-ExchangeRate/v2'
# export BOT_THAILAND_DAILY_AVG_PATH='/DAILY_AVG_EXG_RATE/'
# export BOT_THAILAND_API_KEY='포털_앱_구독_Client_ID'
# export BOT_THAILAND_API_KEY_HEADER='Authorization'
# ───────────────────────────────────────────────────────────────

if [[ ! -f "$JAR" ]]; then
  echo "오류: JAR를 찾을 수 없습니다: $JAR"
  echo "  (스크립트를 pg-app 루트에 두었는지, build/libs 에 jar 가 있는지 확인)"
  exit 1
fi

# 업로드 중단·ASCII FTP 등으로 JAR가 깨지면: Invalid or corrupt jarfile / 기동·종료 시 CNF
if command -v unzip >/dev/null 2>&1; then
  if ! unzip -t "$JAR" >/dev/null 2>&1; then
    echo "오류: JAR가 손상되었거나 불완전합니다: $JAR"
    echo "  → PC에서 bootJar 산출물 크기와 서버 ls -l 용량을 비교하고, FTP는 반드시 바이너리 모드로 다시 업로드하세요."
    echo "  → 확인: unzip -t $JAR"
    exit 1
  fi
fi

if [[ -z "$DB_PASSWORD" || "$DB_PASSWORD" == "비밀번호" ]]; then
  echo "오류: restart-pg-app.sh 안의 DB_PASSWORD 를 실제 DB 비밀번호로 바꾸세요."
  exit 1
fi

cd "$PG_APP_DIR"

# 예전 버그: nohup java 가 flock FD(200)를 상속하면 잠금이 Java 수명 동안 유지되어
# 다음 ./restart-pg-app.sh 가 영원히(또는 180초) 대기한다. 점유자가 pg-app java 면 먼저 종료.
if command -v lsof >/dev/null 2>&1; then
  for _lp in $(lsof -t "$LOCK_FILE" 2>/dev/null || true); do
    if ps -p "$_lp" -o args= 2>/dev/null | grep -q 'pg-app-0.0.1-SNAPSHOT.jar'; then
      echo "   경고: 재시작 잠금을 pg-app Java(PID=$_lp)가 점유 중 → 종료 후 진행"
      kill "$_lp" 2>/dev/null || true
      sleep 2
      kill -9 "$_lp" 2>/dev/null || true
    fi
  done
fi

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

# ─── 0) JPAY 스크립트 준비 (아직 옛 Java 가 서비스 중 = 다운타임 없음) ───
echo "0) JPAY scripts 준비 (서비스 유지 중)..."
export DB_HOST DB_USER DB_PASSWORD
export SPRING_PROFILES_ACTIVE=prod
# 정산 자동 배치 tick(매 분 등). 기본 켜짐. 끄려면 실행 전: export APP_SETTLEMENT_AUTO_RUN=false
export APP_SETTLEMENT_AUTO_RUN="${APP_SETTLEMENT_AUTO_RUN:-true}"
# 운영 수동 재시작과 동일 기본값 (환경변수로 덮어쓰기 가능)
export SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE="${SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE:-20}"
export PG_SCRIPTS_DIR="${PG_SCRIPTS_DIR:-$PG_APP_DIR/scripts}"
PLAYWRIGHT_DEPS_MARKER="${PG_SCRIPTS_DIR}/.playwright-deps-ok"
mkdir -p "$PG_SCRIPTS_DIR"
if command -v unzip >/dev/null 2>&1; then
  echo "   JAR에서 jpay 스크립트 추출..."
  unzip -p "$JAR" BOOT-INF/classes/scripts/jpay-portal-export.js >"$PG_SCRIPTS_DIR/jpay-portal-export.js" 2>/dev/null || true
  unzip -p "$JAR" BOOT-INF/classes/scripts/package.json >"$PG_SCRIPTS_DIR/package.json" 2>/dev/null || true
  script_ver=$(grep -m1 'SCRIPT_VERSION' "$PG_SCRIPTS_DIR/jpay-portal-export.js" 2>/dev/null || true)
  if [[ -n "$script_ver" ]]; then
    echo "   → $script_ver"
  fi
else
  echo "   경고: unzip 없음 — JPAY 동기화 시 앱이 JAR에서 스크립트를 추출합니다."
fi
if [[ -d "$PG_SCRIPTS_DIR" ]] && command -v npm >/dev/null 2>&1 \
    && [[ ! -d "$PG_SCRIPTS_DIR/node_modules/playwright" ]]; then
  echo "   Playwright 미설치 → npm install (최초 1회, 서비스는 아직 유지)..."
  (cd "$PG_SCRIPTS_DIR" && npm install --omit=dev && npx playwright install chromium) >>"$LOG_FILE" 2>&1 || true
  rm -f "$PLAYWRIGHT_DEPS_MARKER"
fi
# install-deps 는 apt 수준이라 매 재시작마다 돌리면 다운타임만 늘림 → 마커 있으면 생략
if [[ -d "$PG_SCRIPTS_DIR/node_modules/playwright" ]] && command -v npx >/dev/null 2>&1; then
  if [[ "${FORCE_PLAYWRIGHT_DEPS:-}" == "1" || ! -f "$PLAYWRIGHT_DEPS_MARKER" ]]; then
    echo "   Chromium 시스템 의존성 확인(install-deps, 1회)..."
    if (cd "$PG_SCRIPTS_DIR" && npx playwright install-deps chromium) >>"$LOG_FILE" 2>&1; then
      date -u +%Y-%m-%dT%H:%M:%SZ >"$PLAYWRIGHT_DEPS_MARKER" 2>/dev/null || touch "$PLAYWRIGHT_DEPS_MARKER"
    fi
  else
    echo "   Chromium deps: 이미 확인됨 (강제 시 FORCE_PLAYWRIGHT_DEPS=1)"
  fi
fi

# ─── 1) 서비스 중단 → 포트 해제 (다운타임 시작) ───
echo "1) 기존 pg-app 프로세스 종료..."
pkill -f "pg-app-0.0.1-SNAPSHOT.jar" 2>/dev/null || true
sleep 1
if command -v ss >/dev/null 2>&1; then
  pid=$(ss -tlnp 2>/dev/null | grep ':8080' | sed -n 's/.*pid=\([0-9]*\).*/\1/p' | head -1)
  if [[ -n "$pid" ]]; then
    echo "   8080 사용 중 PID=$pid 종료 시도 (SIGTERM)..."
    kill "$pid" 2>/dev/null || true
    sleep 1
  fi
fi

echo "   8080 포트 해제 대기(최대 45초, 10초 후 SIGKILL)..."
for _w in $(seq 1 45); do
  if ! port_8080_busy; then
    break
  fi
  # Spring 종료가 길면 다운타임만 늘어남 → 10초 후 강제 종료
  if [[ "$_w" -eq 10 ]] && command -v ss >/dev/null 2>&1; then
    pid=$(ss -tlnp 2>/dev/null | grep ':8080' | sed -n 's/.*pid=\([0-9]*\).*/\1/p' | head -1)
    if [[ -n "$pid" ]]; then
      echo "   경고: 10초 경과 후에도 8080 점유 → PID=$pid 에 SIGKILL"
      kill -9 "$pid" 2>/dev/null || true
      sleep 1
    fi
  fi
  sleep 1
done
if port_8080_busy; then
  echo "경고: 45초 후에도 8080 이 사용 중입니다. ss -tlnp | grep 8080 로 PID 확인 후 kill 하세요."
fi

# ─── 2) 새 JAR 기동 ───
echo "2) pg-app 기동 (prod)..."
echo "   DB·JPA validate 때문에 약 1분 걸릴 수 있습니다. 완료 전 재실행 금지."
# SecureRandom 초기화 지연 완화(Linux). 추가 JVM 옵션은 JAVA_OPTS 로.
JAVA_OPTS_DEFAULT="-Djava.security.egd=file:/dev/./urandom"
# flock FD(200)를 java 에 넘기지 않음 — 상속되면 다음 재시작이 잠금에 걸린다.
nohup java $JAVA_OPTS_DEFAULT ${JAVA_OPTS:-} -jar "$JAR" --spring.profiles.active=prod --server.port=8080 >>"$LOG_FILE" 2>&1 200>&- &
echo "   로그: tail -f $LOG_FILE"
echo "   성공 한 줄: grep 'Started PgAppApplication' $LOG_FILE | tail -1"
echo "   (프롬프트가 바로 돌아오는 것은 정상입니다. java 는 백그라운드에서 기동 중입니다.)"
