# PostgreSQL 원격 접속 설정 (Ubuntu / otlpay 서버)

pgAdmin 등에서 **서버 IP:5432** 로 접속하려면, 서버에서 아래를 확인·설정해야 합니다.

---

## 1. 실제 PostgreSQL 프로세스·포트 확인

`postgresql.service` 가 "active (exited)" 인 경우, 버전별 서비스가 따로 돌아갑니다.

**1) 어떤 버전이 돌아가는지 확인**

```bash
sudo systemctl list-units 'postgresql*'
```

또는

```bash
ps aux | grep postgres
```

**2) 5432 포트에서 대기 중인지 확인**

```bash
sudo ss -tlnp | grep 5432
```

또는

```bash
sudo netstat -tlnp | grep 5432
```

- 아무것도 안 나오면 → PostgreSQL이 5432에서 listen 하고 있지 않음. 버전별 서비스 실행 필요.
- `0.0.0.0:5432` 또는 `*:5432` → 원격 접속 가능.
- `127.0.0.1:5432` 만 있으면 → 로컬만 받고 있어서 원격 접속 불가.

---

## 2. 버전별 서비스 실행 (5432가 안 보일 때)

Ubuntu에서 보통 다음처럼 되어 있습니다.

```bash
# 예: PostgreSQL 14
sudo systemctl start postgresql@14-main
sudo systemctl enable postgresql@14-main
sudo systemctl status postgresql@14-main
```

설치된 버전 확인:

```bash
ls /etc/postgresql/
```

나온 버전 번호(예: 14, 16)로 `postgresql@14-main` 처럼 실행하면 됩니다.

---

## 3. 원격 접속 허용 (listen_addresses)

PostgreSQL이 기본값이면 **localhost(127.0.0.1)** 만 listen 해서, 외부에서 접속이 안 됩니다.

**1) 설정 파일 위치** (버전에 따라 다름)

```bash
# 예: PostgreSQL 14
sudo nano /etc/postgresql/14/main/postgresql.conf
```

**2) 다음 줄 찾아서 수정**

```
#listen_addresses = 'localhost'
```

아래처럼 바꿉니다.

```
listen_addresses = '*'
```

저장 후 PostgreSQL 재시작:

```bash
sudo systemctl restart postgresql@14-main
```

(버전 번호는 본인 환경에 맞게.)

---

## 4. 접속 허용 (pg_hba.conf)

**1) 편집**

```bash
sudo nano /etc/postgresql/14/main/pg_hba.conf
```

**2) 파일 맨 아래에 한 줄 추가**

- **특정 IP만 허용** (본인 PC IP를 알고 있을 때, 보안에 유리):

  ```
  host    all    all    본인PC_IP/32    scram-sha-256
  ```

  예: `host    all    all    123.456.78.90/32    scram-sha-256`

- **일단 모든 IP 허용** (테스트용):

  ```
  host    all    all    0.0.0.0/0    scram-sha-256
  ```

저장 후 PostgreSQL 재시작:

```bash
sudo systemctl restart postgresql@14-main
```

---

## 5. 방화벽에서 5432 열기

ufw 사용 중이면:

```bash
sudo ufw allow 5432/tcp
sudo ufw status
```

카페24 등 호스팅이면 **관리 페이지**에서도 5432 포트 개방이 필요할 수 있습니다.

---

## 6. DB·사용자 확인

`pgdev` DB와 `pgadmin` 사용자가 있어야 pg-app·pgAdmin 접속이 됩니다.

```bash
sudo -u postgres psql -c "\l"
sudo -u postgres psql -c "\du"
```

- `pgdev` DB 없으면: `sudo -u postgres createdb pgdev`
- `pgadmin` 사용자 없으면:

  ```bash
  sudo -u postgres psql -c "CREATE USER pgadmin WITH PASSWORD '원하는비밀번호';"
  sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE pgdev TO pgadmin;"
  sudo -u postgres psql -d pgdev -c "GRANT ALL ON SCHEMA public TO pgadmin;"
  ```

---

## 7. 확인 순서 요약

1. `sudo ss -tlnp | grep 5432` → 5432에서 listen 하는지
2. `listen_addresses = '*'` 인지
3. `pg_hba.conf` 에 원격 IP 또는 `0.0.0.0/0` 추가
4. `sudo systemctl restart postgresql@버전-main`
5. `sudo ufw allow 5432/tcp` (ufw 사용 시)
6. pgAdmin에서 **Host=서버IP, Port=5432, Username=pgadmin, Password=비밀번호** 로 접속 테스트
