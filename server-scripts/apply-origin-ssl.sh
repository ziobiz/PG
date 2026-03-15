#!/bin/bash
# Cloudflare Origin 인증서 적용 스크립트
# 이 스크립트와 server-config/ssl/*.pem, *.key 파일을 서버에 업로드 후 실행
#   chmod +x apply-origin-ssl.sh
#   sed -i 's/\r$//' apply-origin-ssl.sh
#   sudo ./apply-origin-ssl.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SSL_SRC="$SCRIPT_DIR/../server-config/ssl"
NGINX_SSL="/etc/nginx/ssl"
NGINX_CONF="/etc/nginx/conf.d/api-icopay.conf"

if [ ! -f "$SSL_SRC/api.icopay.co.kr.origin.pem" ] || [ ! -f "$SSL_SRC/api.icopay.co.kr.origin.key" ]; then
    echo "오류: server-config/ssl/ 에 .pem, .key 파일이 없습니다."
    exit 1
fi

echo "1. SSL 디렉터리 생성 및 파일 복사..."
sudo mkdir -p "$NGINX_SSL"
sudo cp "$SSL_SRC/api.icopay.co.kr.origin.pem" "$NGINX_SSL/"
sudo cp "$SSL_SRC/api.icopay.co.kr.origin.key" "$NGINX_SSL/"
sudo chmod 600 "$NGINX_SSL/api.icopay.co.kr.origin.key"

echo "2. Nginx 설정 적용..."
sudo tee "$NGINX_CONF" > /dev/null << 'NGINX_EOF'
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

    location = /favicon.ico {
        return 204;
    }
    location = / {
        return 302 https://icopay.co.kr/login.html;
    }
    location = /login {
        return 302 https://icopay.co.kr/login.html;
    }
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Accept "application/json";
    }
}
NGINX_EOF

echo "3. Nginx 테스트 및 재시작..."
sudo nginx -t && sudo systemctl reload nginx

echo "완료. https://api.icopay.co.kr 확인하세요."
