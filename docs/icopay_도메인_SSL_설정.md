# icopay 도메인 + SSL 설정 (카페24 대신 자사 VPS)

[otlpay.cafe24.com](http://otlpay.cafe24.com/login.html) 처럼 **정적 호스팅만** 쓰면 API가 다른 도메인이라 CORS·403 이 자주 납니다.  
**같은 도메인**에서 HTML과 `/api` 를 쓰거나, **API만 `api.icopay.co.kr` 로 통일**하는 방식 중 하나를 쓰면 됩니다.

---

## 0. API만 `api.icopay.co.kr` 로 쓰는 경우 (한 도메인으로 통일)

**백엔드·SSL 주소를 전부 `https://api.icopay.co.kr` 로만 맞추겠다**는 뜻이면 아래가 기준입니다.

### DNS

| 타입 | 이름 | 값 |
|------|------|-----|
| A | `api` | **VPS 공인 IP** (pg-app + Nginx 있는 서버) |

### SSL + Nginx (API + 필요 시 동일 호스트로 웹)

1. `server-scripts/setup-api-nginx-ssl.sh` 를 VPS에 올린 뒤 실행 (기존 문서 [서버_재시작_방법.md](./서버_재시작_방법.md) 의 **api.icopay.co.kr SSL** 절).
2. 완료 후 **https://api.icopay.co.kr** 로 접속 가능해야 합니다.

### 관리자 화면을 어디서 열지 (둘 중 하나)

| 방식 | 주소 예 | 비고 |
|------|---------|------|
| **A. api 호스트에서 같이 서빙** | `https://api.icopay.co.kr/login.html` | Nginx가 `/` 를 pg-app(8080)으로 넘기면, `site/` 가 앱에 묶여 있을 때 동일 출처로 `/api` 호출 → **CORS 부담 적음** |
| **B. 카페24 등 다른 곳에 HTML만** | `http://otlpay.cafe24.com/login.html` | 브라우저는 **`PG_API_BASE = https://api.icopay.co.kr`** 로 호출. **반드시** pg-app `application.yml` 의 **CORS**에 카페24 출처가 포함되어야 함. icopay 측 API 게이트웨이가 OPTIONS 를 막으면 **403** 이 날 수 있음 → 그때는 **A 방식**으로 통일 권장 |

### 프론트(`site/js/config.js`)

- **A:** 브라우저 주소가 이미 `api.icopay.co.kr` 이면 기본값으로 **`PG_API_BASE` 를 비워 두면** 됩니다(같은 도메인 `/api/...`).
- **B:** 카페24 등이면 **`CAFE24_STATIC_SITE_API`** 또는 `data-pg-api-base` 를 **`https://api.icopay.co.kr`** 로 둡니다. (저장소 기본값이 이미 이 주소)

---

## 1. DNS (도메인 업체 또는 카페24 DNS)

| 타입 | 이름 | 값 |
|------|------|-----|
| A | `@` (또는 icopay.co.kr) | **VPS 공인 IP** |
| A | `www` | **동일 VPS IP** |
| A | `api` (API만 분리할 때) | **동일 또는 다른 IP** |

전파까지 수 분~몇 시간 걸릴 수 있습니다.

---

## 2. 서버 준비

- Ubuntu + Nginx
- **pg-app** 이 `127.0.0.1:8080` 에서 실행 중 (`./restart-pg-app.sh`)
- 방화벽 **80, 443** 개방

---

## 3-A. 메인 사이트(icopay.co.kr) + 같은 도메인으로 API (권장)

한 대에서 **정적 `site/`** 와 **`/api` → 8080** 을 같이 씁니다.  
브라우저는 `https://icopay.co.kr` 만 보므로 **카페24 때처럼 CORS 문제가 줄어듭니다.**

1. 저장소의 `server-scripts/setup-icopay-www-nginx-ssl.sh` 를 VPS에 올립니다.
2. SSH:

```bash
cd /home/ftpuser/pg-app   # 또는 스크립트 둔 경로
chmod +x setup-icopay-www-nginx-ssl.sh
sed -i 's/\r$//' setup-icopay-www-nginx-ssl.sh
sudo ./setup-icopay-www-nginx-ssl.sh
```

3. PC의 **`site/` 폴더 전체**를 서버 **`/var/www/icopay/`** 에 복사 (FTP/rsync, `index.html` 이 그 안에 오도록)

4. 접속 확인: **https://icopay.co.kr/login.html**

`site/js/config.js` 는 `icopay.co.kr` / `www.icopay.co.kr` 일 때 **`PG_API_BASE` 가 비어 있음** → API URL이 **`/api/...`** (같은 도메인)으로 나갑니다.

---

## 3-B. API 전용 도메인 (api.icopay.co.kr) SSL 다시 잡기

이미 쓰던 **API만** 분리해 두었다면 기존 스크립트를 다시 실행합니다.

1. `server-scripts/setup-api-nginx-ssl.sh` 업로드
2.:

```bash
chmod +x setup-api-nginx-ssl.sh
sed -i 's/\r$//' setup-api-nginx-ssl.sh
sudo ./setup-api-nginx-ssl.sh
```

3. **https://api.icopay.co.kr** 확인

프론트가 **icopay.co.kr** 이고 API가 **api.icopay.co.kr** 이면 `application.yml` 의 **CORS**에 메인 도메인이 들어가 있어야 합니다. (프로젝트 기본값에 icopay 도메인 포함)

---

## 4. 인증서 갱신

Certbot이 설치되어 있으면 보통 **cron/systemd** 로 자동 갱신됩니다.

```bash
sudo certbot renew --dry-run
```

---

## 5. 카페24와 병행할 때

- DNS를 VPS로 옮기면 **카페24 웹호스팅은 더 이상 해당 도메인으로 안 열릴 수 있음** (의도 확인).
- 테스트는 **`https://icopay.co.kr`** 기준으로 맞추는 것을 권장합니다.

---

## 관련 파일

| 파일 | 용도 |
|------|------|
| **docs/무료_SSL_NOTI와_동일_LetsEncrypt.md** | **NOTI(ziobiz)와 같은 무료 SSL(Let's Encrypt) 요약** |
| `server-scripts/setup-icopay-www-nginx-ssl.sh` | icopay.co.kr + www + 정적 + `/api` 프록시 + SSL |
| `server-scripts/setup-api-nginx-ssl.sh` | api.icopay.co.kr 전용 |
| `server-config/nginx-icopay-site-ssl.conf` | icopay.co.kr + www + `/home/ftpuser/site` + `/api` + SSL 예시 |
| `docs/서버_재시작_방법.md` | pg-app 재시작 |

---

**요약:** 도메인 **A레코드를 VPS로** 맞춘 뒤, **`setup-icopay-www-nginx-ssl.sh`** 로 SSL을 걸고 **`site/`를 `/var/www/icopay`** 에 두면, **https://icopay.co.kr/login.html** 로 통합 운영할 수 있습니다.
