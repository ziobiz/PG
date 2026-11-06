# 사고 기록 — 2026-11-06 pg-app 502 (재부팅 후 미기동)

## 요약

| 항목 | 내용 |
|------|------|
| 증상 | `502 Bad Gateway` nginx/1.18.0 — API·관리자 전부 |
| 영향 | **2026-11-06 00:46 ~ 12:20 KST** (약 **11시간 30분**) |
| 직접 원인 | OS 재부팅 후 **Java `pg-app` 미기동** → nginx upstream `127.0.0.1:8080` connection refused |
| 복구 | 2026-11-06 12:18 `./restart-pg-app.sh` → 12:20:40 `Started PgAppApplication` (HTTP 200) |

## 타임라인 (KST)

1. **11/05 12:14** — 배포 재시작으로 `pg-app` 정상 기동 (PID 4017784, nohup)
2. **11/06 00:44:03** — `systemd-logind`: **`Power key pressed` → Powering Off**
3. **11/06 00:44:05** — Spring `ShutdownHook` (정상 종료)
4. **11/06 00:44:19** — 시스템 재부팅 완료 (`uptime` / `who -b`)
5. **11/06 00:44:25** — nginx 자동 기동 (systemd)
6. **11/06 00:46:02** — nginx error 첫 `connect() failed` → **502 시작**
7. **11/06 12:18~12:20** — 수동 `restart-pg-app.sh` 로 복구

## 원인 분석 (2단)

### A. 왜 서버가 내려갔나

저널:

```text
Nov 06 00:44:03 systemd-logind: Power key pressed.
Nov 06 00:44:03 systemd-logind: Powering Off...
Nov 06 00:44:04 systemd-logind: System is powering down.
```

- **ACPI Power key** 이벤트. Cafe24/호스팅 콘솔·하이퍼바이저 전원(재시작/전원)에 해당.
- `unattended-upgrades` 의 `Automatic-Reboot` 는 주석(비활성) — **자동 APT 재부팅 아님**.
- SSH `reboot`/`shutdown` 실행 기록 없음.
- 앱 OOM kill 아님 (메모리 여유, dmesg OOM 무관).

→ **호스팅 측 전원 이벤트로 VM이 꺼졌다가 다시 올라온 것**으로 본다. (콘솔에서 누가 눌렀는지는 호스팅 패널 감사 로그로만 추가 확인 가능)

### B. 왜 502가 11시간 지속됐나 (재발 핵심)

| 구성 | 부팅 후 |
|------|---------|
| nginx | systemd → **자동 기동** |
| PostgreSQL 등 | 기존 서비스 → 자동 |
| **pg-app (Java)** | **`nohup` + 수동 `restart-pg-app.sh` 만** → **systemd 유닛 없음** → **자동 기동 없음** |

즉 재부팅 자체보다 **“부팅 후 pg-app 자동 기동 부재”** 가 장시간 장애의 원인이다.

## 재발 방지 (조치)

1. **`pg-app.service` systemd** — `WantedBy=multi-user.target`, `Restart=on-failure`
2. **`pg-app.env`** — DB 등 환경변수 (권한 600, Git 미포함)
3. **`restart-pg-app.sh`** — 유닛 있으면 `systemctl restart pg-app` 경로 사용
4. **5분 워치독 cron** — 8080 다운 시 `systemctl start` / fallback restart
5. 설치: `server-scripts/install-pg-app-systemd.sh` (운영 1회)

## 확인 명령

```bash
systemctl is-enabled pg-app
systemctl status pg-app --no-pager
ss -lntp | grep 8080
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8080/
grep pg-app-watchdog /var/log/syslog | tail
```

## 교훈

- 운영 앱은 **반드시 systemd(또는 동등) enable** — nohup 단독 금지.
- 호스팅 전원/재부팅은 언제든 올 수 있음 → **부팅 자동 기동 + 헬스 워치독** 이중화.
- 502 시 nginx만 보지 말고 **upstream(8080) 프로세스·기동 시각**을 먼저 본다.
- **NOTI(`d:\Delopment\NOTI`, `pg-noti-relay`)도 동일 정책** — `pm2-root` + `pm2 save` + 5분 `/health` 워치독.  
  (`NOTI/docs/incident-parity-boot-autostart-with-pg-app.md`, `NOTI/scripts/ops/install-pg-noti-autostart.sh`)
