# 무료 SSL — NOTI(ziobiz)와 같은 방식 (Let's Encrypt)

**NOTI** 같은 자바/스프링 + Nginx 운영에서 쓰는 **무료 SSL**은 대부분 **Let's Encrypt** 입니다.  
돈 안 내고 **3개월마다 자동 갱신**되는 공식 무료 인증서입니다.

PG 프로젝트도 **똑같이** 아래만 하면 됩니다.

---

## 준비 (공통)

1. **도메인 DNS**  
   - `api.icopay.co.kr` → **VPS 공인 IP** (A 레코드)  
   - 메인 사이트도 이 서버에서 SSL 쓰려면 `icopay.co.kr`, `www` 도 **같은 VPS IP**  
2. 서버에 **Nginx** 설치, **80·443** 포트 열림  
3. **pg-app** 은 `127.0.0.1:8080` 에서 실행 (`./restart-pg-app.sh`)

---

## 방법 1 — API만 `api.icopay.co.kr` (스크립트 한 방)

저장소 **`server-scripts/setup-api-nginx-ssl.sh`** 가  
HTTP 설정 → Certbot(webroot) → HTTPS Nginx 까지 **자동**으로 합니다.

```bash
cd /home/ftpuser/pg-app
chmod +x setup-api-nginx-ssl.sh
sed -i 's/\r$//' setup-api-nginx-ssl.sh
sudo ./setup-api-nginx-ssl.sh
```

끝나면 **https://api.icopay.co.kr** 접속 테스트.

---

## 방법 2 — NOTI에서 흔히 쓰는 `certbot --nginx` (수동, 무료 동일)

```bash
sudo apt update
sudo apt install -y certbot python3-certbot-nginx
```

**API 도메인만:**

```bash
sudo certbot certonly --nginx -d api.icopay.co.kr
```

**메인 도메인(icopay.co.kr)만:**

```bash
sudo certbot certonly --nginx -d icopay.co.kr -d www.icopay.co.kr
```

그다음 Nginx 설정 파일에 인증서 경로를 넣습니다.

- API 전용 예시: **`server-config/nginx-api-icopay.conf`** 내용을  
  `/etc/nginx/sites-available/` 에 복사 후 `sites-enabled` 에 링크  
- 메인 + 정적 `site/` + `/api` 예시: **`server-config/nginx-icopay-site-ssl.conf`**

```bash
sudo nginx -t && sudo systemctl reload nginx
```

더 자세한 단계는 **`server-config/icopay_SSL_적용_가이드.md`** 도 같이 보세요.

---

## 자동 갱신 (Let's Encrypt 필수 습관)

```bash
sudo certbot renew --dry-run
```

통과하면 보통 **systemd 타이머**로 이미 갱신 잡혀 있습니다. 없으면:

```bash
sudo crontab -e
# 추가
0 3 * * * certbot renew --quiet && systemctl reload nginx
```

---

## Let's Encrypt가 403·실패할 때 (카페24·프록시 등)

- **Cloudflare “주황 구름”** 켜져 있으면 인증 방식이 달라질 수 있음  
- 그때는 **`docs/Cloudflare_Origin_SSL_설정.md`** (Origin 인증서) 참고

---

## 정리

| NOTI와 같은 점 | PG에서 할 일 |
|----------------|--------------|
| 무료 SSL | **Let's Encrypt** (= Certbot) |
| API 도메인 | `setup-api-nginx-ssl.sh` **또는** `certbot --nginx -d api.icopay.co.kr` |
| 메인 도메인 + site 폴더 | `nginx-icopay-site-ssl.conf` + `certbot` 으로 `icopay.co.kr` 인증서 |

**카페24 도메인(`otlpay.cafe24.com`)** 은 **카페24 서버**에서 열리므로, **본인 VPS에서 Let's Encrypt로 그 이름 인증서를 딸 수 없습니다.**  
`icopay.co.kr` / `api.icopay.co.kr` 처럼 **VPS IP로 연결된 도메인**에 SSL을 걸면 NOTI와 같은 구조가 됩니다.
