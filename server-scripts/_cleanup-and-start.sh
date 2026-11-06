#!/bin/bash
set -euo pipefail
systemctl stop pg-app.service 2>/dev/null || true
pkill -9 -f 'pg-app-0.0.1-SNAPSHOT.jar' 2>/dev/null || true
sleep 2
# ensure port free
for i in $(seq 1 20); do
  ss -tlnp 2>/dev/null | grep -q ':8080' || break
  pkill -9 -f 'pg-app-0.0.1-SNAPSHOT.jar' 2>/dev/null || true
  sleep 1
done
systemctl reset-failed pg-app.service 2>/dev/null || true
systemctl start pg-app.service
sleep 2
echo "=== status ==="
systemctl --no-pager --full status pg-app.service | head -n 25
echo "=== java count ==="
ps -ef | grep '[j]ava.*pg-app' | wc -l
ps -ef | grep '[j]ava.*pg-app' || true
echo CLEANUP_START_DONE
