# https://api.icopay.co.kr/login.html 즉시 적용 (VPS)

**목표:** 브라우저에서 `https://api.icopay.co.kr/login.html` 이 pg-app(내부 **8080**)으로 넘어가게 한다.

제가 원격에서 서버를 대신 조작할 수는 없으므로, **otlpay 서버에 SSH로 접속한 뒤** 아래만 순서대로 실행하면 된다.

---

## 0. 전제

| 항목 | 확인 |
|------|------|
| DNS | `api.icopay.co.kr` **A 레코드** → **이 VPS 공인 IP** |
| pg-app | `./restart-pg-app.sh` 로 떠 있고 `ss -tlnp \| grep 8080` 에 java |
| 443 | **Nginx**가 받게 할 것. **Apache가 443을 잡고 있으면** 403/다른 응답이 난다 → Apache 끄거나, Apache에서 전부 `ProxyPass` 로 8080 넘기기 ([`server-config/apache-api-icopay-reverse-proxy.conf`](../server-config/apache-api-icopay-reverse-proxy.conf)) |

---

## 1. 자동 스크립트 (권장)

저장소 파일을 VPS에 올린 뒤:

```bash
cd /home/ftpuser/pg-app   # 또는 스크립트 둔 경로
chmod +x setup-api-nginx-ssl.sh
sed -i 's/\r$//' setup-api-nginx-ssl.sh
sudo ./setup-api-nginx-ssl.sh
```

- 원본: **`server-scripts/setup-api-nginx-ssl.sh`**
- certbot이 이메일을 요구하면, 스크립트 대신 **4단계**를 수동으로 하거나 certbot에 `--email you@domain.com` 옵션을 추가해 실행한다.

끝나면:

```bash
curl -sI https://api.icopay.co.kr/login.html | head -5
curl -sS -o /dev/null -w "%{http_code}\n" https://api.icopay.co.kr/api/auth/me
```

**200** 또는 **302가 아닌 API 응답**이면 프록시는 통과한 것이다.

---

## 2. 수동 (Nginx만)

1. **`server-config/nginx-api-icopay.conf`** 내용을  
   `/etc/nginx/conf.d/api-icopay.conf` 또는 `sites-available` 에 복사
2. 인증서 없으면 먼저:

   ```bash
   sudo apt install -y certbot python3-certbot-nginx
   sudo certbot certonly --nginx -d api.icopay.co.kr
   ```

3. 설정의 `ssl_certificate` 경로가 certbot 출력과 일치하는지 확인
4. `sudo nginx -t && sudo systemctl reload nginx`

핵심은 **`location /` 와 `location /api/` 모두** `proxy_pass http://127.0.0.1:8080;` 인 것이다. (`login.html` 은 `/` 쪽)

---

## 3. CORS (선택)

`login.html` 을 **api 호스트에서** 열고 같은 도메인으로 `/api` 를 부르면 보통 CORS 이슈는 없다.  
다른 도메인에서 `api.icopay.co.kr` 만 호출할 때를 위해, 앱의 `application.yml` **`app.cors.allowed-origins`** 에 `https://api.icopay.co.kr` 이 포함되어 있으면 안전하다. (변경 시 JAR 재빌드·배포)

---

## 4. 더 읽을 문서

- **Certbot 403 + Cloudflare:** [certbot_Cloudflare_403_해결.md](./certbot_Cloudflare_403_해결.md)
- [api_icopay_도메인_완전설정_가이드.md](./api_icopay_도메인_완전설정_가이드.md)
- [서버_재시작_방법.md](./서버_재시작_방법.md) — api.icopay 403 / Apache 점검
- [Cloudflare_Origin_SSL_설정.md](./Cloudflare_Origin_SSL_설정.md) — LE 대신 Origin 인증서
