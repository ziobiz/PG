# icopay.co.kr 도메인 연동 및 SSL 적용 가이드

## 구성

| 도메인 | 용도 |
|--------|------|
| **icopay.co.kr** | 사이트 (관리자, 추후 홈페이지) |
| **api.icopay.co.kr** | API 전용 (pg-app 8080) |

---

## 1. 사전 준비

- **DNS A 레코드** 설정:
  - icopay.co.kr → 서버 IP
  - www.icopay.co.kr → 서버 IP
  - api.icopay.co.kr → 서버 IP
- 서버에 **Nginx** 설치
- **SSH** 접속 가능

---

## 2. SSL 인증서 발급 (Let's Encrypt)

SSH로 서버 접속 후:

```bash
# Certbot 설치 (Ubuntu/Debian)
sudo apt update
sudo apt install certbot python3-certbot-nginx -y

# 사이트 인증서 (icopay.co.kr, www.icopay.co.kr)
sudo certbot certonly --nginx -d icopay.co.kr -d www.icopay.co.kr

# API 인증서 (api.icopay.co.kr)
sudo certbot certonly --nginx -d api.icopay.co.kr
```

---

## 3. Nginx 설정 적용

```bash
# 1) 사이트 설정 (icopay.co.kr)
sudo nano /etc/nginx/sites-available/icopay.conf
# (server-config/nginx-icopay.conf 내용 붙여넣기)

# 2) API 설정 (api.icopay.co.kr)
sudo nano /etc/nginx/sites-available/api-icopay.conf
# (server-config/nginx-api-icopay.conf 내용 붙여넣기)

# 3) 사이트 활성화
sudo ln -sf /etc/nginx/sites-available/icopay.conf /etc/nginx/sites-enabled/
sudo ln -sf /etc/nginx/sites-available/api-icopay.conf /etc/nginx/sites-enabled/

# 4) 기존 otlpay 비활성화 (필요 시)
sudo rm -f /etc/nginx/sites-enabled/otlpay.conf

# 5) 적용
sudo nginx -t
sudo systemctl reload nginx
```

---

## 4. 자동 갱신 (선택)

```bash
sudo crontab -e
# 추가: 매일 3시 갱신 시도
0 3 * * * certbot renew --quiet
```

---

## 5. 확인

| 페이지 | URL |
|--------|-----|
| 메인 | https://icopay.co.kr |
| 로그인 | https://icopay.co.kr/login.html |
| API | https://api.icopay.co.kr/api/auth/me |
