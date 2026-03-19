# api.icopay.co.kr 403 Forbidden — 진단 결과 & 조치

## 1. 저장소 밖에서 실제 접속 테스트 (재현)

다음과 같이 **모든 경로가 403** 이었습니다.

| URL | 결과 |
|-----|------|
| `https://api.icopay.co.kr/` | 403 Forbidden (Apache 문구) |
| `https://api.icopay.co.kr/login.html` | 동일 |
| `https://api.icopay.co.kr/login` | 동일 |
| `https://api.icopay.co.kr/api/auth/me` | 동일 |

응답 본문 예:

```html
<h1>Forbidden</h1>
<p>You don't have permission to access /api/auth/me on this server.</p>
```

HTTP 헤더에 `Server: cloudflare` 가 보이지만, **본문은 Apache 전형적인 403** 입니다.  
→ **Cloudflare 뒤 Origin 서버(Apache)** 가 요청을 막고 있고, **pg-app(8080)까지 요청이 가지 않는 상태**로 보면 됩니다.

같은 시점에 **`https://icopay.co.kr/login.html`**, **`/api/auth/me`** 도 403 이었습니다.  
→ **메인 도메인 Origin 도 동일한 문제**일 가능성이 큽니다. (Cloudflare에서 icopay로만 리다이렉트해도 해결되지 않을 수 있음)

---

## 2. 해결 방향 (서버에서 반드시 수행)

코드/JAR 만으로는 Origin Apache 설정을 바꿀 수 없습니다. **VPS(또는 Origin)에서 웹서버 설정을 고쳐야** 합니다.

### 선택 A — Nginx만 쓰기 (이미 저장소에 예시 있음)

- Apache **중지** 후 443 은 Nginx만 사용  
  `server-config/nginx-api-icopay.conf` + `server-scripts/setup-api-nginx-ssl.sh` 참고

### 선택 B — Apache를 쓰는 경우

- **`/` 전체**를 `http://127.0.0.1:8080/` 으로 **ReverseProxy**  
- 예시 파일: **`server-config/apache-api-icopay-reverse-proxy.conf`**

적용 전 확인:

1. 서버에서 `pg-app` 이 **8080** 에서 떠 있는지:
   - `curl -sI http://127.0.0.1:8080/api/auth/me`  
   - `curl` 없으면: `wget -S -O /dev/null http://127.0.0.1:8080/api/auth/me 2>&1 | head -20`  
   - 또는: `sudo apt update && sudo apt install -y curl`
2. **Apache와 Nginx가 동시에 443** 을 쓰지 않는지 (하나만 사용)
3. CentOS + SELinux: `sudo setsebool -P httpd_can_network_connect 1`

SSL 인증서 경로는 Let’s Encrypt 실제 경로에 맞게 수정하세요.

---

## 3. 애플리케이션 쪽 (이미 반영된 것)

- `pg-app/build.gradle`: 빌드 시 **`site/` → static`** 포함 → JAR 배포 후 **프록시만 맞으면** `/login.html` 등 서빙 가능
- Nginx 예시: `server-config/nginx-api-icopay.conf` (`location /` 도 8080 프록시)

---

## 4. Cloudflare

403 원인이 **Origin Apache** 이므로, **Redirect Rule만**으로는 API가 살아나지 않습니다.  
Origin에서 위 프록시/포트를 맞춘 뒤, 필요하면 WAF/방화벽에서 8080 직접 노출 여부를 정리하세요.

---

## 5. 확인 명령 (서버 SSH)

```bash
# 로컬에서 앱 살아 있는지 (curl 없을 때는 wget)
curl -sI http://127.0.0.1:8080/api/auth/me
# wget: wget -S -O /dev/null http://127.0.0.1:8080/api/auth/me 2>&1 | head -20

# 누가 443 듣는지
sudo ss -tlnp | grep ':443'
```

`8080` 은 200/401 등이 나오는데 공인 도메인만 403이면, **443 앞단 웹서버 설정**이 원인입니다.
