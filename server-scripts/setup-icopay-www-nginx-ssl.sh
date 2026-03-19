#!/bin/bash
# icopay.co.kr + www.icopay.co.kr — 정적 사이트(site/) + /api → pg-app(8080) + Let's Encrypt SSL
#
# 전제:
#   - DNS A 레코드: icopay.co.kr, www.icopay.co.kr → 이 서버 공인 IP
#   - pg-app 이 127.0.0.1:8080 에서 실행 중
#   - 80/443 방화벽 허용
#
# 사용법 (SSH root 또는 sudo):
#   chmod +x setup-icopay-www-nginx-ssl.sh
#   sed -i 's/\r$//' setup-icopay-www-nginx-ssl.sh
#   sudo ./setup-icopay-www-nginx-ssl.sh
#
# (선택) 사이트 루트를 바꾸려면 아래 SITE_ROOT 만 수정 후 실행
set -e

SITE_ROOT="/var/www/icopay"
NGINX_CONF="/etc/nginx/conf.d/icopay-www.conf"
DOMAINS="icopay.co.kr www.icopay.co.kr"
PRIMARY_DOMAIN="icopay.co.kr"

echo "0. 정적 파일 디렉터리 + ACME 경로..."
sudo mkdir -p "$SITE_ROOT" /var/www/certbot
sudo chown -R www-data:www-data "$SITE_ROOT" 2>/dev/null || true

echo "1. HTTP 전용 설정 (인증서 발급 전)..."
sudo tee "$NGINX_CONF" > /dev/null << NGINX_EOF
server {
    listen 80;
    server_name icopay.co.kr www.icopay.co.kr;

    root $SITE_ROOT;
    index index.html;

    location ^~ /.well-known/acme-challenge/ {
        root /var/www/certbot;
        allow all;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Accept "application/json";
    }

    location / {
        try_files \$uri \$uri/ /index.html;
    }
}
NGINX_EOF

echo "2. Nginx 테스트 및 재시작..."
sudo nginx -t && sudo systemctl restart nginx

echo "3. Certbot 설치..."
sudo apt-get update -qq
sudo apt-get install -y certbot

echo "4. SSL 인증서 발급 (icopay.co.kr + www)..."
sudo certbot certonly --webroot -w /var/www/certbot \
  -d icopay.co.kr -d www.icopay.co.kr \
  --non-interactive --agree-tos --register-unsafely-without-email

echo "5. HTTPS 설정 적용..."
sudo tee "$NGINX_CONF" > /dev/null << NGINX_SSL_EOF
server {
    listen 80;
    server_name icopay.co.kr www.icopay.co.kr;
    location ^~ /.well-known/acme-challenge/ {
        root /var/www/certbot;
        allow all;
    }
    location / {
        return 301 https://\$host\$request_uri;
    }
}

server {
    listen 443 ssl http2;
    server_name icopay.co.kr www.icopay.co.kr;

    ssl_certificate /etc/letsencrypt/live/${PRIMARY_DOMAIN}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${PRIMARY_DOMAIN}/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    root ${SITE_ROOT};
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Accept "application/json";
    }

    location / {
        try_files \$uri \$uri/ /index.html;
    }
}
NGINX_SSL_EOF

echo "6. Nginx 재로드..."
sudo nginx -t && sudo systemctl reload nginx

echo ""
echo "=== 완료 ==="
echo "1) PC의 site/ 폴더 내용을 서버 ${SITE_ROOT} 에 복사 (rsync 또는 FTP로 동일 구조)"
echo "2) https://${PRIMARY_DOMAIN}/login.html 접속"
echo "3) API는 같은 도메인 /api 이므로 site/js/config.js 에서 icopay 호스트는 PG_API_BASE 비움(기본)이면 됨"
echo "4) api 전용 도메인은 기존 setup-api-nginx-ssl.sh 로 별도 유지 가능"
