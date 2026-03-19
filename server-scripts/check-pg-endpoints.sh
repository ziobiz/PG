#!/usr/bin/env bash
# pg-app 로컬(127.0.0.1:8080) 엔드포인트 빠른 점검 — curl 없으면 wget 사용
set -e
BASE="${1:-http://127.0.0.1:8080}"

fetch() {
  local url="$1"
  if command -v curl >/dev/null 2>&1; then
    curl -sS -o /tmp/pg_chk_body.txt -w "%{http_code}" "$url"
  else
    wget -qO /tmp/pg_chk_body.txt "$url" 2>/dev/null && echo 200 || echo "000"
  fi
}

echo "=== PG endpoint check: $BASE ==="
code=$(fetch "$BASE/api/auth/me")
echo "/api/auth/me HTTP $code"
head -c 200 /tmp/pg_chk_body.txt 2>/dev/null || true
echo ""
if [[ "$code" == "200" ]]; then
  echo "OK: 앱이 8080에서 응답 중입니다."
  exit 0
fi
echo "FAIL: 8080 앱 또는 경로를 확인하세요."
exit 1
