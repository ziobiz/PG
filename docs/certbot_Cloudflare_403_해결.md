# Certbot + Cloudflare 에서 `403` / `unauthorized` (api.icopay.co.kr)

증상 예:

```text
Invalid response from http://api.icopay.co.kr/.well-known/acme-challenge/...: 403
Detail: ... 2606:4700:...   ← Cloudflare IP(IPv6/IPv4)면 프록시 경유
```

Let's Encrypt는 **직접** 당신 서버의 **80번**에서 챌린지 파일을 읽어야 합니다.  
**Cloudflare 주황 구름(프록시 켬)** 이면 요청이 먼저 Cloudflare로 가고, WAF·규칙·캐시 때문에 **403**이 나와 인증이 실패합니다.

---

## 해결 1) 인증서 발급할 때만 프록시 끄기 (가장 흔함)

1. [Cloudflare 대시보드](https://dash.cloudflare.com) → **icopay.co.kr** → **DNS**
2. **`api.icopay.co.kr`** A 레코드에서 **프록시 상태를 끔** → **DNS만 사용**(회색 구름)
3. **2~5분** 기다린 뒤 (캐시·TTL):
   ```bash
   dig +short api.icopay.co.kr A
   ```
   → **VPS 공인 IP**가 나와야 합니다 (104.x / 172.x 같은 Cloudflare 대역이면 아직 프록시 쪽으로 보이는 것일 수 있음).
4. 서버에서:
   ```bash
   sudo ./setup-api-nginx-ssl.sh
   ```
   또는 4단계만:
   ```bash
   sudo certbot certonly --webroot -w /var/www/certbot -d api.icopay.co.kr \
     --force-renewal --non-interactive --agree-tos --register-unsafely-without-email
   ```
5. 인증서 받은 뒤, 원하면 다시 **프록시 켬**(주황).  
   이때 Cloudflare **SSL/TLS** 모드는 **Full** 또는 **Full (strict)** (원본에 정식 인증서 있을 때).

---

## 해결 2) 프록시 유지한 채 WAF 예외 (환경에 따라)

Cloudflare → **Security** → **WAF** / **Firewall rules**:

- URI Path **contains** `.well-known/acme-challenge` → **Allow** / Skip security

(규칙 우선순위·플랜에 따라 동작이 다를 수 있어, **해결 1이 더 확실**합니다.)

---

## 해결 3) Let's Encrypt 없이 — Cloudflare Origin 인증서

HTTP-01을 쓰지 않습니다. 절차: **`docs/Cloudflare_Origin_SSL_설정.md`**

- CF에서 Origin Certificate 발급 → Nginx `ssl_certificate` 로 장착  
- Cloudflare SSL 모드: **Full (strict)**

---

## 참고: `certbot --nginx` 로 실패한 경우

로그에 `authenticator: nginx` 면 **nginx 플러그인**을 쓴 것입니다.  
저장소 스크립트는 **`--webroot`** 를 씁니다. 수동 실행 시 플러그인을 섞지 마세요.

---

## 확인 명령 (회색 구름 후)

```bash
curl -sI "http://api.icopay.co.kr/.well-known/acme-challenge/" | head -5
```

프록시를 끈 뒤라면 **404** 정도는 나와도 되고(파일 없음), **403만 Cloudflare 페이지**가 아니면 됩니다.
