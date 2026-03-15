# Cloudflare Origin Certificate로 SSL 적용 (Let's Encrypt 대안)

api.icopay.co.kr 에서 Let's Encrypt(certbot)가 403으로 실패할 때 사용합니다.
Cafe24 등 호스팅에서 .well-known 경로를 차단하는 경우 이 방법을 사용하세요.

---

## 1단계: Cloudflare에서 Origin 인증서 발급

1. https://dash.cloudflare.com 접속 → **icopay.co.kr** 선택
2. 왼쪽 메뉴 **SSL/TLS** → **Origin Server**
3. **Create Certificate** 클릭
4. 설정:
   - Private key type: **RSA (2048)**
   - Hostnames: **api.icopay.co.kr** (또는 `*.icopay.co.kr, icopay.co.kr` 로 전체)
   - Certificate Validity: **15 years**
5. **Create** 클릭
6. **Origin Certificate** 와 **Private Key** 내용을 각각 복사 (다른 곳에 백업)

---

## 2단계: 서버에 인증서 파일 생성

SSH 접속 후:

```bash
# 디렉터리 생성
sudo mkdir -p /etc/nginx/ssl

# Origin Certificate 저장 (-----BEGIN CERTIFICATE----- 부터 -----END CERTIFICATE----- 까지 전체 복사)
sudo nano /etc/nginx/ssl/api.icopay.co.kr.origin.pem

# Private Key 저장 (-----BEGIN PRIVATE KEY----- 부터 -----END PRIVATE KEY----- 까지 전체 복사)
sudo nano /etc/nginx/ssl/api.icopay.co.kr.origin.key

# 권한 설정
sudo chmod 600 /etc/nginx/ssl/api.icopay.co.kr.origin.key
```

---

## 3단계: Nginx 설정

```bash
sudo nano /etc/nginx/conf.d/api-icopay.conf
```

아래 내용으로 **전체 교체**:

```nginx
server {
    listen 80;
    server_name api.icopay.co.kr;
    return 301 https://api.icopay.co.kr$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.icopay.co.kr;

    ssl_certificate /etc/nginx/ssl/api.icopay.co.kr.origin.pem;
    ssl_certificate_key /etc/nginx/ssl/api.icopay.co.kr.origin.key;
    ssl_protocols TLSv1.2 TLSv1.3;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Accept "application/json";
    }

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

저장: Ctrl+O, Enter, Ctrl+X

---

## 4단계: Nginx 재시작

```bash
sudo nginx -t
sudo systemctl reload nginx
```

---

## 5단계: Cloudflare SSL 모드

Cloudflare 대시보드 → SSL/TLS → **Overview**:
- **Full (strict)** 선택 (Origin Certificate 사용 시 필수)

---

## 6단계: api.icopay.co.kr 프록시 상태

- **프록싱됨(주황색)** 으로 두면 Cloudflare가 HTTPS 종료
- **DNS 전용(회색)** 으로 두면 서버에서 직접 HTTPS 제공

둘 다 가능합니다. 프록싱 사용 시 트래픽: 사용자 ↔ Cloudflare(HTTPS) ↔ 서버(HTTPS).

---

완료 후 https://api.icopay.co.kr 접속 확인.
