#!/bin/bash
# 서버에서만 실행: systemd 설치 (비밀번호 sed 는 파이프 없이)
set -euo pipefail
DIR=/home/ftpuser/pg-app
cd "$DIR"

# 서버에 이미 있는 restart 의 DB_* 보존
cp -a "$DIR/restart-pg-app.sh" /tmp/restart-pg-app.sh.bak.db 2>/dev/null || true
cp -f "$DIR/server-scripts/restart-pg-app.sh" "$DIR/restart-pg-app.sh"
chmod +x "$DIR/restart-pg-app.sh" "$DIR/server-scripts/"*.sh

python3 <<'PY'
from pathlib import Path
import re
main = Path("/home/ftpuser/pg-app/restart-pg-app.sh")
bak = Path("/tmp/restart-pg-app.sh.bak.db")
if not bak.exists():
    raise SystemExit(0)
text = main.read_text(encoding="utf-8", errors="replace")
old = bak.read_text(encoding="utf-8", errors="replace")
for key in ("DB_HOST", "DB_USER", "DB_PASSWORD"):
    m = re.search(rf"^{key}=.*$", old, flags=re.M)
    if not m:
        continue
    text = re.sub(rf"^{key}=.*$", m.group(0), text, count=1, flags=re.M)
main.write_text(text, encoding="utf-8")
print("DB_* preserved from previous restart-pg-app.sh")
PY

bash "$DIR/server-scripts/install-pg-app-systemd.sh"
systemctl is-enabled pg-app
systemctl is-active pg-app || true
ss -lntp | grep 8080 || true
echo APPLY_SYSTEMD_DONE
