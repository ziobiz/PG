#!/bin/bash
# api.icopay.co.kr Nginx 설정 + SSL 자동 적용
# 서버에서: FTP로 이 파일 업로드 후
#   chmod +x setup-api-nginx-ssl.sh
#   sed -i 's/\r$//' setup-api-nginx-ssl.sh
#   sudo ./setup-api-nginx-ssl.sh
#
# ★ Cloudflare(주황 구름 프록시) 사용 시:
#   Let's Encrypt HTTP-01은 http://api.icopay.co.kr/.well-known/... 로 검증하는데,
#   프록시가 켜 있으면 403·차단으로 실패합니다.
#   → Cloudflare DNS에서 api.icopay.co.kr 을 "DNS만 사용"(회색 구름)으로 바꾼 뒤
#     이 스크립트 4단계(인증서 발급)를 다시 실행하거나, docs/certbot_Cloudflare_403_해결.md 참고.

set -e

NGINX_CONF="/etc/nginx/conf.d/api-icopay.conf"

echo "0. ACME 인증용 디렉터리 생성..."
sudo mkdir -p /var/www/certbot

echo "1. HTTP 설정 적용 (ACME 경로 포함)..."
sudo tee "$NGINX_CONF" > /dev/null << 'NGINX_EOF'
server {
    listen 80;
    server_name api.icopay.co.kr;

    location ^~ /.well-known/acme-challenge/ {
        root /var/www/certbot;
        allow all;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
NGINX_EOF

echo "2. Nginx 테스트 및 재시작..."
sudo nginx -t && sudo systemctl restart nginx

echo "3. Certbot 설치..."
sudo apt update -qq
sudo apt install -y certbot

echo "4. SSL 인증서 발급 (webroot, Let's Encrypt HTTP-01)..."
echo ""
echo ">>> Cloudflare 쓰는 경우: api.icopay.co.kr 레코드가 '프록시 끔(회색)'인지 먼저 확인하세요."
echo ">>> (주황 구름이면 여기서 거의 항상 unauthorized / 403)"
echo ""
sudo certbot certonly --webroot -w /var/www/certbot -d api.icopay.co.kr \
  --non-interactive --agree-tos --register-unsafely-without-email \
  --preferred-challenges http-01

echo "5. HTTPS 설정 적용..."
sudo tee "$NGINX_CONF" > /dev/null << 'NGINX_SSL_EOF'
# Cloudflare Flexible: 방문자는 HTTPS, 원본은 80(HTTP)만 쓰면
#   return 301 https://$host... 만 있으면 매 요청마다 같은 URL로 301 → ERR_TOO_MANY_REDIRECTS
# X-Forwarded-Proto 가 https 이면 301 생략하고 바로 pg-app 으로 넘깁니다.
# (권장: Cloudflare SSL 은 Full 또는 Full (strict) + 원본 443)
map $http_x_forwarded_proto $api_80_tls_redirect {
    default 1;
    https   0;
}

server {
    listen 80;
    server_name api.icopay.co.kr;
    location ^~ /.well-known/acme-challenge/ {
        root /var/www/certbot;
        allow all;
    }
    location / {
        if ($api_80_tls_redirect) {
            return 301 https://$host$request_uri;
        }
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}

server {
    listen 443 ssl http2;
    server_name api.icopay.co.kr;

    ssl_certificate /etc/letsencrypt/live/api.icopay.co.kr/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.icopay.co.kr/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
NGINX_SSL_EOF

echo "6. Nginx 최종 재시작..."
sudo nginx -t && sudo systemctl reload nginx

echo "완료. https://api.icopay.co.kr 확인하세요."
