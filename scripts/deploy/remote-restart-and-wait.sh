#!/bin/bash
# Deploy-Prod.ps1 이 SCP 후 서버에서 실행. CRLF/이스케이프 이슈를 피하기 위한 원격 헬퍼.
# 사용: remote-restart-and-wait.sh /home/ftpuser/pg-app [Started문자열]
set -u
REMOTE_APP="${1:-/home/ftpuser/pg-app}"
GREP_MARK="${2:-Started PgAppApplication}"
cd "$REMOTE_APP" || exit 1

# java 가 flock FD 를 상속한 경우 잠금이 영구 점유됨 → 재시작 전에 해제
if command -v lsof >/dev/null 2>&1; then
  for _lp in $(lsof -t .restart-pg-app.lock 2>/dev/null || true); do
    if ps -p "$_lp" -o args= 2>/dev/null | grep -q 'pg-app-0.0.1-SNAPSHOT.jar'; then
      echo "stale lock held by java PID=$_lp — stopping"
      kill "$_lp" 2>/dev/null || true
      sleep 2
      kill -9 "$_lp" 2>/dev/null || true
    fi
  done
fi

chmod +x restart-pg-app.sh 2>/dev/null || true

# 재시작 전 로그 줄 수 — 이후 새로 찍힌 Started 만 인정
before_lines=$(wc -l < pg-app.log 2>/dev/null || echo 0)
before_lines=${before_lines//[[:space:]]/}

./restart-pg-app.sh
echo "waiting_for_started (before_lines=$before_lines)..."

ok=0
for _i in $(seq 1 60); do
  after_lines=$(wc -l < pg-app.log 2>/dev/null || echo 0)
  after_lines=${after_lines//[[:space:]]/}
  new_started=0
  if [ "$after_lines" -gt "$before_lines" ] 2>/dev/null; then
    take=$((after_lines - before_lines + 20))
    if tail -n "$take" pg-app.log 2>/dev/null | grep -q "$GREP_MARK"; then
      new_started=1
    fi
  fi
  if [ "$new_started" = 1 ] && ss -tlnp 2>/dev/null | grep -q ':8080'; then
    echo READY
    grep "$GREP_MARK" pg-app.log | tail -3
    ss -tlnp 2>/dev/null | grep 8080 || true
    ok=1
    break
  fi
  echo "wait_${_i}"
  sleep 5
done

if [ "$ok" != 1 ]; then
  echo TIMEOUT_WAITING_STARTED
  grep "$GREP_MARK" pg-app.log | tail -3 || true
  ss -tlnp 2>/dev/null | grep 8080 || true
  ps -eo pid,lstart,cmd | grep 'pg-app-0.0.1-SNAPSHOT.jar' | grep -v grep || true
  exit 1
fi
exit 0
