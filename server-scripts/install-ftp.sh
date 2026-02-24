#!/bin/bash
# FTP 서버(vsftpd) 설치 스크립트 — Ubuntu 20.04 / 22.04
# 서버에 SSH 접속한 뒤 이 스크립트를 실행하세요.

set -e

echo "=== FTP(vsftpd) 설치 시작 ==="

# 1. 패키지 업데이트 및 vsftpd 설치
sudo apt-get update
sudo apt-get install -y vsftpd

# 2. 기존 설정 백업
sudo cp /etc/vsftpd.conf /etc/vsftpd.conf.bak.$(date +%Y%m%d%H%M%S) 2>/dev/null || true

# 3. 설정 적용 (로컬 사용자 로그인, 쓰기 허용, 홈 디렉터리만 접근)
sudo tee /etc/vsftpd.conf > /dev/null << 'EOF'
# 기본
listen=YES
listen_ipv6=NO
anonymous_enable=NO
local_enable=YES
write_enable=YES
local_umask=022
dirmessage_enable=YES
use_localtime=YES
xferlog_enable=YES
connect_from_port_20=YES

# 보안: 사용자를 자신의 홈 디렉터리 안으로 제한
chroot_local_user=YES
allow_writeable_chroot=YES

# Passive 모드 (방화벽/공유기 뒤에서 연결 시 필요)
pasv_enable=YES
pasv_min_port=21100
pasv_max_port=21110
pasv_address=
EOF

# 4. Passive 모드 시 서버 공인 IP가 필요하면 아래 한 줄만 수정 후 사용
# echo "pasv_address=서버공인IP" | sudo tee -a /etc/vsftpd.conf

# 5. vsftpd 재시작
sudo systemctl restart vsftpd
sudo systemctl enable vsftpd

# 6. 방화벽(ufw) 사용 중이면 FTP 포트 열기
if command -v ufw >/dev/null 2>&1 && sudo ufw status 2>/dev/null | grep -q "Status: active"; then
  echo "방화벽(ufw) 활성화됨 — FTP 포트 개방 중..."
  sudo ufw allow 21/tcp comment 'FTP'
  sudo ufw allow 21100:21110/tcp comment 'FTP passive'
  sudo ufw status
else
  echo "ufw 비활성화 상태이므로 포트 추가 생략."
fi

echo ""
echo "=== FTP(vsftpd) 설치 완료 ==="
echo ""
echo "접속 방법:"
echo "  - 호스트: 서버 IP 주소"
echo "  - 포트: 21"
echo "  - 사용자: SSH로 로그인하는 계정(예: root 또는 배포용 계정)"
echo "  - 비밀번호: 해당 계정 비밀번호"
echo ""
echo "주의: 새 FTP 전용 계정을 쓰려면 서버에서 다음으로 추가하세요."
echo "  sudo adduser ftpuser"
echo "  (이후 FileZilla 등에서 호스트=서버IP, 사용자=ftpuser, 비밀번호=설정한 비밀번호)"
echo ""
