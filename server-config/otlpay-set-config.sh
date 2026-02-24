#!/bin/bash
# 서버에서 한 번만 실행. SSH 접속 후 이 스크립트 내용을 복사해 붙여넣기 하면 됨.

cat > /etc/nginx/sites-available/otlpay.conf << 'EOF'
server {
    listen 80;
    server_name otlpay.cafe24.com;

    root /var/www/site;
    index index.html;
    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF

nginx -t && systemctl reload nginx
echo "적용 완료. 브라우저에서 http://otlpay.cafe24.com 확인하세요."
