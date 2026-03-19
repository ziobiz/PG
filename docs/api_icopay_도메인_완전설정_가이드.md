# https://api.icopay.co.kr 도메인 완전 설정 가이드

**목표:** 브라우저·카페24·`curl` 모두에서 `https://api.icopay.co.kr` 로 접속했을 때 **403(Apache Forbidden) 없이** pg-app(Spring, **8080**)이 응답하게 만듭니다.  
특히 **`POST /api/auth/login`** 이 JSON으로 동작해야 [카페24 로그인](http://otlpay.cafe24.com/login.html) 이 성공합니다.

---

## 한눈에 체크리스트

| 순서 | 할 일 | 정상 신호 |
|------|--------|-----------|
| 1 | DNS `api.icopay.co.kr` **A레코드** → **VPS 공인 IP** | `ping api.icopay.co.kr` 이 서버 IP |
| 2 | VPS에서 **pg-app** 이 `127.0.0.1:8080` 에서 기동 | `curl -sS http://127.0.0.1:8080/api/auth/me` → 200 JSON |
| 3 | **443 포트는 Nginx 또는 Apache 중 하나만** | `ss -tlnp \| grep ':443'` |
| 4 | **전체 경로** `/`·`/api/` → **8080 프록시** | 공인 `curl` 이 403 HTML 아님 |
| 5 | **SSL** (Let’s Encrypt 등) | 브라우저 자물쇠, `https://` |
| 6 | 최신 **JAR** + `SecurityConfig` 에 `/api/auth/me` 등 **permitAll** | 로컬 8080에서 로그인·/me 확인 |
| 7 | 카페24에 **`site/js/config.js`** 등 최신 업로드 | `PG_API_BASE` → `https://api.icopay.co.kr` |

---

## 1. DNS (도메인 관리 쪽)

1. `api.icopay.co.kr` **A 레코드**를 **pg-app 올린 VPS 공인 IP**로 설정합니다.  
2. **Cloudflare**를 쓰면:
   - **프록시(주황 구름)** 켜도 됩니다. 다만 **Origin**이 443에서 제대로 응답해야 합니다.
   - Origin에서 Let’s Encrypt가 막히면 [Cloudflare Origin SSL](./Cloudflare_Origin_SSL_설정.md) 문서 참고.

---

## 2. 애플리케이션 (pg-app)

1. PC에서 `pg-app` 빌드: `gradlew.bat bootJar`  
2. JAR을 서버 `/home/ftpuser/pg-app/build/libs/` 등에 두고 **재시작**  
   - 스크립트: `server-scripts/restart-pg-app.sh` (DB·프로파일 확인)  
3. 서버에서 **반드시** 확인:

```bash
curl -sS --max-time 5 http://127.0.0.1:8080/api/auth/me
```

JSON이 나오면 앱은 정상입니다. (여기서 302면 **JAR이 예전 보안 설정**일 수 있음 → 최신 빌드 재배포)

---

## 3. 443 을 누가 쓰는지 (충돌 방지)

```bash
sudo ss -tlnp | grep ':443'
```

- **Apache** 와 **Nginx** 가 **둘 다 443** 이면 하나를 끄거나, **한쪽만** `api.icopay.co.kr` 을 맡깁니다.  
- 지금처럼 **403 + Apache 문구**가 나오면 대개 **Apache가 443을 잡고 있고 8080으로 안 넘기는 상태**입니다.

---

## 4-A. Nginx로 통일하는 방법 (권장)

### 4-A-1. 자동 스크립트 (Ubuntu 계열)

저장소의 파일을 서버에 올린 뒤:

```bash
chmod +x setup-api-nginx-ssl.sh
sed -i 's/\r$//' setup-api-nginx-ssl.sh
sudo ./setup-api-nginx-ssl.sh
```

- 설정 파일: `/etc/nginx/conf.d/api-icopay.conf`  
- 인증서: Certbot **webroot** 방식

### 4-A-2. 수동 적용

예시 전문: **`server-config/nginx-api-icopay.conf`**

핵심은 **`location /api/`** 와 **`location /`** 모두:

```nginx
proxy_pass http://127.0.0.1:8080;
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
```

적용 후:

```bash
sudo nginx -t && sudo systemctl reload nginx
```

### 4-A-3. Apache가 443을 잡고 있으면

**택 1:** Apache 중지 후 Nginx만 사용  

```bash
sudo systemctl stop apache2   # 또는 httpd
sudo systemctl enable nginx
```

**택 2:** Apache 유지 시 → 아래 **4-B** 로만 `api` 처리 (Nginx와 443 중복 금지)

---

## 4-B. Apache만 쓰는 경우

예시: **`server-config/apache-api-icopay-reverse-proxy.conf`**

- `ProxyPass / http://127.0.0.1:8080/`  
- `ProxyPassReverse / http://127.0.0.1:8080/`  
- 모듈: `proxy`, `proxy_http`, `headers`, `ssl`  
- SELinux: `setsebool -P httpd_can_network_connect 1`

SSL 경로는 Let’s Encrypt 실제 경로에 맞게 수정합니다.

---

## 5. SSL 인증서

- **Let’s Encrypt:** `certbot` 으로 `api.icopay.co.kr` 전용 인증서  
- 스크립트 `setup-api-nginx-ssl.sh` 가 HTTP-01용 `/.well-known` 까지 포함합니다.  
- 자세한 설명: [무료_SSL_NOTI와_동일_LetsEncrypt.md](./무료_SSL_NOTI와_동일_LetsEncrypt.md)

---

## 6. 방화벽

- **80, 443** 허용 (Certbot·HTTPS)  
- **8080** 은 대개 **외부에 열지 않고** 127.0.0.1 만 써도 됩니다 (프록시만 공개).

---

## 7. 공인 도메인 검증 (반드시)

로컬 8080이 정상일 때, **같은 서버 밖(PC)** 에서:

```bash
curl -sS -o /dev/null -w "GET /api/me HTTP %{http_code}\n" --max-time 10 https://api.icopay.co.kr/api/auth/me

curl -sS -o /tmp/login.out -w "POST login HTTP %{http_code}\n" -X POST https://api.icopay.co.kr/api/auth/login \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"username":"admin","password":"admin1!"}' --max-time 15
```

- **403** + HTML `Forbidden` → **아직 웹서버(Apache 등)가 8080으로 안 넘김**  
- **200** + JSON (`success` true/false) → **프록시·앱까지 도달** (이때부터 비번만 맞으면 로그인 가능)

---

## 8. 프론트(카페24) 쪽

- **`site/js/config.js`** : `localhost` 가 아닌 호스트는 **`https://api.icopay.co.kr`**  
- **`login.html` / `index.html`** 최신본 FTP 업로드  
- 브라우저 **localStorage `pg_api_base`** 에 `http://localhost:8080` 남아 있으면 삭제 후 새로고침  

CORS 허용: `pg-app` 의 `application.yml` → `app.cors.allowed-origins` 에 `https://otlpay.cafe24.com` 등 포함 (프로젝트 기본값에 이미 있을 수 있음).

---

## 9. 관련 파일 (저장소)

| 파일 | 용도 |
|------|------|
| `server-scripts/setup-api-nginx-ssl.sh` | Nginx + Certbot 자동 |
| `server-config/nginx-api-icopay.conf` | Nginx 수동 예시 |
| `server-config/apache-api-icopay-reverse-proxy.conf` | Apache 프록시 예시 |
| `server-scripts/restart-pg-app.sh` | JAR 재시작 |
| [api_403_진단_및_Apache설정.md](./api_403_진단_및_Apache설정.md) | 403 원인 상세 |
| [배포_전_테스트_체크리스트.md](./배포_전_테스트_체크리스트.md) | 빌드·테스트 |

---

## 요약 한 문장

**`https://api.icopay.co.kr` 이 “도메인으로 나오게” 하려면 DNS를 VPS로 맞춘 뒤, 443 웹서버에서 `/`·`/api` 전부를 `127.0.0.1:8080` 으로 넘기고 SSL을 걸면 됩니다.**  
지금 로그인 실패의 직접 원인은 대부분 **이 프록시가 안 되어 `/api/auth/login` 이 403으로 막히는 것**입니다.
