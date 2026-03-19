# PostgreSQL DB 서버 반영 가이드

> 로컬 개발(`dev` 프로파일)은 **H2 인메모리** + `ddl-auto: create-drop` 이라, PC를 끄면 **데이터가 사라집니다.**  
> 서버(운영)의 **PostgreSQL**과는 별개이며, **자동으로 올라가지 않습니다.**

---

## 1. 서버에서 쓰는 DB (pg-app 기준)

| 항목 | 기본값 |
|------|--------|
| DB 이름 | `pgdev` (`jdbc:postgresql://호스트:5432/pgdev`) |
| 사용자/비번 | `restart-pg-app.sh` 의 `DB_USER` / `DB_PASSWORD` 와 동일해야 함 |

PostgreSQL에 **데이터베이스 `pgdev`** 가 있고, 앱 계정에 접속 권한이 있어야 합니다.

```sql
-- PostgreSQL(superuser)에서 예시
CREATE DATABASE pgdev;
CREATE USER pgadmin WITH PASSWORD '실제비밀번호';
GRANT ALL PRIVILEGES ON DATABASE pgdev TO pgadmin;
-- PostgreSQL 15+ 에서는 DB 소유권/스키마 권한이 더 필요할 수 있음 → 오류 나면 GRANT SCHEMA public 등 추가
```

---

## 2. 테이블(스키마)이 없어서 앱이 안 뜰 때

`application.yml` 은 **`spring.jpa.hibernate.ddl-auto: validate`** 이라, **테이블이 미리 있어야** 기동합니다.

### 방법 A: 한 번만 `update` 로 스키마 생성 (가장 단순)

1. SSH에서 환경변수로 **한 번만** `update` 지정 후 기동:

```bash
cd /home/ftpuser/pg-app
export DB_HOST=localhost
export DB_USER=pgadmin
export DB_PASSWORD='실제비밀번호'
export SPRING_JPA_HIBERNATE_DDL_AUTO=update
pkill -f pg-app-0.0.1-SNAPSHOT.jar
sleep 3
nohup java -jar build/libs/pg-app-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod --server.port=8080 >> pg-app.log 2>&1 &
```

2. 로그에 **`Started PgAppApplication`** 확인 후, **반드시** `update` 없이 다시 띄우기:

```bash
unset SPRING_JPA_HIBERNATE_DDL_AUTO
./restart-pg-app.sh
```

> 운영에서 장시간 `update` 를 두면 위험할 수 있으니, 스키마 생성 후 **`validate` 로 복귀**하는 것이 중요합니다.

### 방법 B: SQL 마이그레이션만 수동 실행

프로젝트 `pg-app/src/main/resources/db/` 아래 파일을 **번호 순서**로 실행합니다.  
(대부분 `IF NOT EXISTS` / `ADD COLUMN IF NOT EXISTS` 로 중복 실행에 강함.)

권장 순서:

1. `V2_add_comp_excel_columns.sql`
2. `V3_add_site_summary.sql`
3. `V4_add_crypto_transfer_fee.sql`
4. `V5_add_org_branding.sql`
5. `V6_add_regional_settings.sql`
6. `V7_add_addr_country.sql`
7. `V8_add_addr_etc.sql`
8. `V9_add_base_currency_multi.sql`
9. `V10_add_pg_trnsctn_origin.sql`
10. `V11_notify_env_and_inbound.sql`

pgAdmin **Query Tool** 또는:

```bash
psql -h localhost -U pgadmin -d pgdev -f V2_add_comp_excel_columns.sql
# … 이하 동일
```

> V2 만으로는 **JPA 엔티티 전체**와 100% 일치하지 않을 수 있습니다. 그 경우 **방법 A**가 더 확실합니다.

---

## 3. 초기 데이터(시드) — “DB 내용이 비어 있는 것 같다”일 때

`pg-app` 을 띄우면 **`DataLoader`** 가 실행됩니다. (프로파일 제한 없음)

- **`tb_org_unit` 이 0건**이면: 본사·총판·가맹점 트리, 프로필, 샘플 거래 등 **개발용 시드**가 들어갑니다.
- **`admin` 사용자**가 없으면 생성됩니다. 기본 비밀번호: **`admin1!`**

그래서 **빈 DB에 스키마만 맞다면**, 앱을 **정상 기동 한 번** 하는 것만으로도 **기본 데이터가 채워질 수 있습니다.**

확인용 SQL (`pg-app/src/main/resources/db/check_tables.sql` 참고):

```sql
SELECT COUNT(*) FROM tb_org_unit;
SELECT COUNT(*) FROM app_user;
```

---

## 4. 로컬 PostgreSQL에 쌓인 데이터를 서버로 “통째로” 옮기고 싶을 때

로컬을 **H2가 아니라 PostgreSQL** 로 쓰고 있고, 그 DB에 실제 데이터가 있을 때만 의미 있습니다.

**PC(또는 덤프 있는 곳):**

```bash
pg_dump -h localhost -U pgadmin -d pgdev -Fc -f pgdev_backup.dump
```

**서버:**

```bash
# 기존 DB를 덮어쓰면 위험하니, 백업 후 진행
pg_restore -h localhost -U pgadmin -d pgdev --clean --if-exists pgdev_backup.dump
```

> `admin` 비밀번호·JWT·도메인이 달라지면 로그인/토큰 이슈가 날 수 있어, **운영 전 테스트**가 필요합니다.

---

## 5. 자주 하는 오해

| 오해 | 실제 |
|------|------|
| 로컬에서 개발한 DB가 FTP로 같이 올라간다 | **아니요.** DB는 파일 업로드 대상이 아닙니다. |
| 카페24에 DB가 있다 | **아니요.** 정적 호스팅이면 HTML/JS만. DB는 **VPS PostgreSQL**. |
| JAR만 새로 올리면 DB도 갱신된다 | **아니요.** JAR는 앱 코드. 데이터는 **PostgreSQL 안**에 별도로 있습니다. |

---

## 6. 점검 순서 요약

1. 서버 PostgreSQL에 **`pgdev`** 존재 + 계정/비번이 `restart-pg-app.sh` 와 일치하는지  
2. 앱 기동 실패(스키마 오류) → 위 **방법 A** 또는 **방법 B**  
3. 앱 기동 성공인데 메뉴/데이터 없음 → `tb_org_unit` 건수 확인 후, 비어 있으면 **앱 재시작**으로 `DataLoader` 유도  
4. 여전히 비면 → `pg-app.log` 에서 `DataLoader` / SQL 예외 확인  

문제가 계속되면 `pg-app.log` 중 **ERROR** 구간과 `SELECT COUNT(*) FROM tb_org_unit;` 결과를 함께 알려 주세요.
