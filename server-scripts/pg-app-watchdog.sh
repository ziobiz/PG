#!/bin/bash
# pg-app 8080 헬스 워치독 — systemd 가 있어도 수동 nohup 잔존·크래시 후 미기동을 막는다.
# cron: */5 * * * * root /home/ftpuser/pg-app/scripts/ops/pg-app-watchdog.sh
set -euo pipefail

PORT_OK=0
if command -v ss >/dev/null 2>&1; then
  if ss -tlnp 2>/dev/null | grep -q ':8080'; then PORT_OK=1; fi
elif command -v curl >/dev/null 2>&1; then
  if curl -sS -o /dev/null --connect-timeout 2 http://127.0.0.1:8080/; then PORT_OK=1; fi
fi

if [[ "$PORT_OK" -eq 1 ]]; then
  exit 0
fi

logger -t pg-app-watchdog "8080 down — attempting recovery"
if systemctl is-enabled pg-app.service >/dev/null 2>&1; then
  systemctl start pg-app.service || true
  sleep 3
  if systemctl is-active --quiet pg-app.service; then
    logger -t pg-app-watchdog "recovered via systemctl start pg-app"
    exit 0
  fi
fi

if [[ -x /home/ftpuser/pg-app/restart-pg-app.sh ]]; then
  /home/ftpuser/pg-app/restart-pg-app.sh || true
  logger -t pg-app-watchdog "invoked restart-pg-app.sh"
  exit 0
fi

logger -t pg-app-watchdog "recovery failed: no systemd unit and no restart script"
exit 1
