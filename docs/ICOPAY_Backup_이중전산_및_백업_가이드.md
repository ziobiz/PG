# ICOPAY 이중 전산(Backup) 및 데이터 백업 가이드

> Primary(ICOPAY)와 Backup(ICOPAY Global)을 **물리 서버·DB 각각 독립**으로 두고, ziobiz/NOTI에서 **동일 노티 본문**을 두 URL로 fan-out 하는 운영 모델을 정리한 문서입니다.

---

## 1. 목표

| 목표 | 설명 |
|------|------|
| 솔루션 2벌 | 동일 `pg-app` JAR + `site/`, 도메인·VPS·PostgreSQL 각각 분리 |
| 평소 이중 수신 | NOTI **개발노티** → Primary, **백업노티** → Backup |
| 장애 시 전환 | 한쪽(앱·도메인)이 죽어도 다른 쪽으로 관리·API·노티 적재 지속 |
| 데이터 백업 | PostgreSQL `pg_dump` 등으로 복구용 스냅샷 보관 |

**주의:** DB를 분리하면 “자동으로 항상 100% 동일”하지 않습니다. 아래 **§4 동기화**가 같이 가야 합니다.

---

## 2. 아키텍처

```
[ziobiz/NOTI]
    |-- 개발노티 --> https://api.icopay.co.kr/.../pg-notify/{primaryToken}/...
    |-- 백업노티 --> https://api.icopayglobal.com/.../pg-notify/{backupToken}/...

[서버 A - Primary]              [서버 B - Backup]
  icopay.co.kr / api.icopay.co.kr   icopayglobal.com / api.icopayglobal.com
  PostgreSQL A                      PostgreSQL B
  pg-app :8080                      pg-app :8080
```

### 노티 수신 URL (권장 경로)

- 미들웨어: `{공개베이스}/api/middleware/notify/v1/pg-notify/{ingressToken}/…`
- 레거시: `{공개베이스}/api/open/pg-notify/{ingressToken}/…`

동일 핸들러·동일 검증(HMAC·IP·토큰). 자세한 연동: [전산노티_연동_NOTI.md](./전산노티_연동_NOTI.md), [NOTI_노티재전송_Cursor개발요청.md](./NOTI_노티재전송_Cursor개발요청.md)

---

## 3. 서버 구축 순서 (아직 서버 없을 때)

### 3-1. 지금 할 수 있는 것

- Git 저장소에 최신 코드 유지
- `pg-app/src/main/resources/db/V*.sql` 마이그레이션 순서 파악
- 도메인 확정: Primary `api.icopay.co.kr`, Backup 예시 `api.icopayglobal.com`
- NOTI에 등록할 URL·토큰 형식 정리

### 3-2. Primary 서버 첫 구축

1. VPS + PostgreSQL + Nginx(SSL) + pg-app (`restart-pg-app.sh`)
2. DB 마이그레이션 SQL 순서 적용 → [DB_서버_반영_가이드.md](./DB_서버_반영_가이드.md)
3. 본사설정 → 전산노티: **공개 URL 베이스** = `https://api.icopay.co.kr`
4. NOTI **개발노티**에 표시된 노티 URL 등록
5. **첫 DB 백업** (`pg_dump`) + 서버 밖 보관 — §5 참고

### 3-3. Backup 서버 구축

1. 별도 VPS + PostgreSQL + Nginx + 동일 JAR
2. Primary DB **전체 덤프 1회** → Backup DB `pg_restore`
3. Backup만 수정: `ingress_token`, `public_base_url` = `https://api.icopayglobal.com`
4. `site/`: `icopayglobal.com` 호스트는 `data-pg-api-base` 또는 Nginx same-origin `/api` — [site/js/config.js](../site/js/config.js) 참고
5. NOTI **백업노티** URL 등록, 테스트 노티 1건 → 양쪽 `tb_pg_notify_inbound` PARSED 확인
6. Backup DB 가맹점 **아웃바운드 URL**은 비우거나 테스트만 (이중 통보 방지)

---

## 4. “동일 데이터” 맞추기 (DB 분리 시)

| 데이터 종류 | 맞추는 방법 |
|-------------|-------------|
| 노티·거래 (`pg_trnsctn`, inbound) | NOTI fan-out (개발노티 + 백업노티) |
| 가맹점·본사설정·노티매핑·API연동·사용자 | **Primary에서만 수정** + A→B **마스터 테이블 주기 동기화** |
| 실패·불일치 | 일일 건수/diff + NOTI 구간 재전송 |

### 마스터 동기화 (서버 2대)

- **초기:** `pg_dump` 전체 → Backup `pg_restore`
- **이후:** cron으로 Primary에서 **마스터·설정 테이블만** 덤프 후 Backup 반영  
  - **덮어쓰지 말 것:** Backup 전용 `ingress_token`, `public_base_url`, `public_api_base_url`  
  - **거래 테이블**은 NOTI로 맞추므로 전체 덮어쓰기 금지
- 대안: PostgreSQL **Logical Replication**(마스터 테이블만 publication)

### 운영 규칙

- 설정·가맹점 등록: **Primary만** (또는 변경 직후 마스터 sync)
- Backup: 노티 수신 + 조회·장애 시 운영 전환용
- URL 결제·브라우저 콜백은 노티와 별도 — Backup만 쓰려면 ChillPay 콜백·`public_api_base_url`도 global 도메인 등록

---

## 5. PostgreSQL 백업 (복구용)

DB는 FTP로 올라가지 않습니다. VPS에서 `pg_dump` 사용.

```bash
mkdir -p /backup
pg_dump -h localhost -U pgadmin -d pgdev -Fc -f /backup/pgdev_$(date +%Y%m%d).dump
```

- **복원(테스트):** 복원 전 현 DB도 덤프 → `pg_restore --clean --if-exists` — [DB_서버_반영_가이드.md](./DB_서버_반영_가이드.md) §4
- **자동화:** cron 매일 새벽 + 7~30일 지난 파일 삭제
- **보관:** `/backup` 뿐 아니라 **다른 디스크·PC·오브젝트 스토리지**에 복사

앱·설정 백업: 배포 JAR 날짜별 복사, Nginx/SSL, `restart-pg-app.sh`·환경변수( Git 제외 ).

---

## 6. 장애 전환 체크리스트

1. 살아 있는 쪽 도메인으로 로그인 (`icopayglobal.com` 등)
2. NOTI 백업노티(또는 살아 있는 한 줄) 동작 확인
3. 전환 구간 누락 노티 → NOTI 재전송
4. URL결제·ChillPay ResultUrl을 global로 바꿀지 결정
5. Primary 복구 후 마스터·거래 diff 점검

---

## 7. 리스크 요약

| 리스크 | 대응 |
|--------|------|
| 한쪽만 NOTI 200 | 일일 inbound/PARSED·TransactionId diff, 재전송 |
| 마스터 미동기화 | Backup에서 MERCHANT_UNRESOLVED | A→B 마스터 sync |
| 가맹점 이중 통보 | Backup 아웃바운드 URL 비움 |
| 동시 POST 레이스(같은 DB일 때) | DB unique·멱등 키(향후); 현재는 분리 DB면 각각 1건 |
| 디스크 증가 | inbound 보관일(기본 90일)·`pg_dump` 용량·로그 로테이션 |

---

## 8. ICOPAY 내 모니터링

본사설정 → **서버운영관리** (`GET /api/hq/serverManage`): 호스트 메모리, JVM heap, 디스크(앱 경로), SSL, DB, 일별 트래픽.

임계치(코드 기준): 시스템 메모리 사용 warn 70% / danger 90%, 디스크 warn 75% / danger 90%, JVM heap warn 80% / danger 92%.

---

## 9. 관련 문서

- [서버_환경_전환_체크리스트.md](./서버_환경_전환_체크리스트.md)
- [배포_가이드_FTP_업로드_및_재시작.md](./배포_가이드_FTP_업로드_및_재시작.md)
- [노티매핑설정.md](./노티매핑설정.md)
