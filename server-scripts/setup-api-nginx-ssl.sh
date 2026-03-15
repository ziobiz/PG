#!/bin/bash
# api.icopay.co.kr Nginx 설정 + SSL 자동 적용
# 서버에서: FTP로 이 파일 업로드 후
#   chmod +x setup-api-nginx-ssl.sh
#   sed -i 's/\r$//' setup-api-nginx-ssl.sh
#   sudo ./setup-api-nginx-ssl.sh

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
NGINX_EOF

echo "2. Nginx 테스트 및 재시작..."
sudo nginx -t && sudo systemctl restart nginx

echo "3. Certbot 설치..."
sudo apt update -qq
sudo apt install -y certbot

echo "4. SSL 인증서 발급..."
sudo certbot certonly --webroot -w /var/www/certbot -d api.icopay.co.kr --non-interactive --agree-tos --register-unsafely-without-email

echo "5. HTTPS 설정 적용..."
sudo tee "$NGINX_CONF" > /dev/null << 'NGINX_SSL_EOF'
server {
    listen 80;
    server_name api.icopay.co.kr;
    location ^~ /.well-known/acme-challenge/ {
        root /var/www/certbot;
        allow all;
    }
    location / {
        return 301 https://api.icopay.co.kr$request_uri;
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
NGINX_SSL_EOF

echo "6. Nginx 최종 재시작..."
sudo nginx -t && sudo systemctl reload nginx

echo "완료. https://api.icopay.co.kr 확인하세요."
