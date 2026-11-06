#!/bin/bash
# 운영 서버에서 1회(또는 재설치): pg-app systemd 자동 기동 + 5분 워치독.
# 사용: sudo bash install-pg-app-systemd.sh
set -euo pipefail

PG_APP_DIR="${PG_APP_DIR:-/home/ftpuser/pg-app}"
RESTART_SH="$PG_APP_DIR/restart-pg-app.sh"
ENV_FILE="$PG_APP_DIR/pg-app.env"
UNIT_SRC_DIR="$(cd "$(dirname "$0")" && pwd)"
UNIT_DST="/etc/systemd/system/pg-app.service"
WATCHDOG_DST="$PG_APP_DIR/scripts/ops/pg-app-watchdog.sh"
CRON_DST="/etc/cron.d/pg-app-watchdog"

if [[ "$(id -u)" -ne 0 ]]; then
  echo "root 로 실행하세요: sudo bash $0"
  exit 1
fi

if [[ ! -f "$RESTART_SH" ]]; then
  echo "없음: $RESTART_SH"
  exit 1
fi

# restart-pg-app.sh 의 DB_* 를 env 파일로 추출 (Git 에 비밀번호 넣지 않음)
extract_assign() {
  local key="$1"
  grep -E "^${key}=" "$RESTART_SH" | head -n1 | sed -E "s/^${key}=\"?([^\"]*)\"?.*/\1/"
}

DB_HOST="$(extract_assign DB_HOST)"
DB_USER="$(extract_assign DB_USER)"
DB_PASSWORD="$(extract_assign DB_PASSWORD)"
if [[ -z "$DB_PASSWORD" || "$DB_PASSWORD" == "비밀번호" ]]; then
  echo "오류: $RESTART_SH 에서 DB_PASSWORD 를 읽지 못했습니다."
  exit 1
fi

umask 077
cat >"$ENV_FILE" <<EOF
DB_HOST=${DB_HOST:-localhost}
DB_USER=${DB_USER:-pgadmin}
DB_PASSWORD=${DB_PASSWORD}
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20
PG_SCRIPTS_DIR=${PG_APP_DIR}/scripts
EOF
chown root:root "$ENV_FILE"
chmod 600 "$ENV_FILE"
echo "wrote $ENV_FILE (mode 600)"

if [[ -f "$UNIT_SRC_DIR/pg-app.service" ]]; then
  cp -f "$UNIT_SRC_DIR/pg-app.service" "$UNIT_DST"
else
  echo "오류: $UNIT_SRC_DIR/pg-app.service 없음"
  exit 1
fi
# WorkingDirectory / jar 경로를 PG_APP_DIR 에 맞춤
sed -i "s|/home/ftpuser/pg-app|${PG_APP_DIR}|g" "$UNIT_DST"
chmod 644 "$UNIT_DST"

mkdir -p "$(dirname "$WATCHDOG_DST")"
if [[ -f "$UNIT_SRC_DIR/pg-app-watchdog.sh" ]]; then
  cp -f "$UNIT_SRC_DIR/pg-app-watchdog.sh" "$WATCHDOG_DST"
else
  echo "오류: watchdog 스크립트 없음"
  exit 1
fi
chmod 755 "$WATCHDOG_DST"

cat >"$CRON_DST" <<EOF
# ICOPAY pg-app: 8080 다운 시 자동 복구 (5분)
*/5 * * * * root $WATCHDOG_DST >/dev/null 2>&1
EOF
chmod 644 "$CRON_DST"

systemctl daemon-reload
systemctl enable pg-app.service

# 이미 nohup java 가 떠 있으면 systemd 로 이관
if ss -tlnp 2>/dev/null | grep -q ':8080'; then
  echo "8080 사용 중 → 기존 프로세스 종료 후 systemd 기동으로 이관"
  pkill -f "pg-app-0.0.1-SNAPSHOT.jar" 2>/dev/null || true
  sleep 3
  pkill -9 -f "pg-app-0.0.1-SNAPSHOT.jar" 2>/dev/null || true
  for _i in $(seq 1 30); do
    ss -tlnp 2>/dev/null | grep -q ':8080' || break
    sleep 1
  done
fi

systemctl restart pg-app.service
echo "systemctl status:"
systemctl --no-pager --full status pg-app.service | head -n 25
echo "install OK. boot 시 자동 기동 + 5분 워치독 활성."
