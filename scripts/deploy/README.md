# 배포 스크립트 (운영 VPS)

개발 완료 후 사용자가 **「배포해」** 라고 하면 에이전트가 이 스크립트를 실행합니다.

## 최초 1회 설정

```powershell
cd d:\Delopment\PG
.\scripts\deploy\Setup-Credentials.ps1
```

편집기에서 `%USERPROFILE%\.pg-deploy\credentials.env` 를 열어 값을 채웁니다.  
템플릿: `scripts/deploy/credentials.env.example`

**비밀번호는 Git에 넣지 마세요.** (`credentials.env` 는 무시됩니다.)

SSH는 가능하면 **키 인증** (`SSH_KEY_PATH`)을 권장합니다. 비밀번호만 있어도 Windows OpenSSH `SSH_ASKPASS` 로 동작합니다.

## 사용

```powershell
# 빌드 + JAR 업로드 + 재시작
.\scripts\deploy\Deploy-Prod.ps1

# SQL도 함께 (저장소 상대 경로)
.\scripts\deploy\Deploy-Prod.ps1 -SqlFiles @(
  'pg-app/src/main/resources/db/V231_ilk_card_auth_mode_and_agency.sql',
  'pg-app/src/main/resources/db/V232_ilk_subscription_card_token.sql'
)

# 빌드 생략(이미 bootJar 한 경우)
.\scripts\deploy\Deploy-Prod.ps1 -SkipBuild

# 업로드만, 재시작 안 함
.\scripts\deploy\Deploy-Prod.ps1 -SkipRestart
```

## 서버 전제

| 항목 | 기본값 |
|------|--------|
| 원격 앱 루트 | `/home/ftpuser/pg-app` |
| JAR | `.../build/libs/pg-app-0.0.1-SNAPSHOT.jar` |
| 재시작 | `./restart-pg-app.sh` |
| DB | `localhost` / `pgdev` / `pgadmin` (원격에서 `psql`) |

`site/` 는 JAR에 포함되므로 **일반 배포는 JAR + 재시작**이면 충분합니다.

## 다운타임 최소화

- JAR·SQL 은 **옛 프로세스 유지** 상태에서 올린 뒤, 마지막에만 재시작합니다.
- `restart-pg-app.sh` 는 JPAY/Playwright 준비를 **종료 전**에 하고, `install-deps` 는 1회만 수행합니다.
- 배포 시 저장소의 `server-scripts/restart-pg-app.sh` 를 서버에 동기화합니다(DB 비밀번호는 서버 값 유지).
- 재시작·`Started` 대기는 `remote-restart-and-wait.sh` 를 SCP 후 서버에서 실행합니다(Windows SSH CRLF 회피).

## 자동 배포 실패 시 (수동 폴백)

자동화에 문제가 있으면 **예전 방식**으로 하면 됩니다.

1. 에이전트(또는 PC)에서 `bootJar` 까지 완료 → `pg-app/build/libs/pg-app-0.0.1-SNAPSHOT.jar`
2. 사용자가 FTP/SCP 로 JAR 업로드
3. 필요 시 DB SQL 수동 적용
4. SSH:
   ```bash
   cd /home/ftpuser/pg-app
   export SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20
   export APP_SETTLEMENT_AUTO_RUN=true
   ./restart-pg-app.sh
   grep 'Started PgAppApplication' pg-app.log | tail -1
   ```

`Deploy-Prod.ps1` 이 실패하면 위 폴백 안내를 콘솔에 출력합니다.
