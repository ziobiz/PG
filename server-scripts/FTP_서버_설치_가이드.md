# FTP 서버 설치 가이드 (Ubuntu / 카페24 가상서버)

서버에 **vsftpd**를 설치해서 Windows에서 FileZilla 등으로 파일을 올리고 내리는 방법입니다.

> **용어가 헷갈리면** → [FTP_설치_쉬운_순서.md](FTP_설치_쉬운_순서.md) 를 먼저 보세요. "스크립트 복사 없이 PuTTY에서 명령어만 붙여 넣기"로 끝낼 수 있습니다.

---

## 0. 용어만 짚고 가기

- **서버** = 카페24에서 받은 그 컴퓨터(IP 있는 쪽). 파일을 올릴 "저쪽".
- **SSH로 접속** = **PuTTY**를 켜서 서버 IP 넣고 Open → 로그인(계정/비밀번호) 하는 것. 이 상태가 "서버에 붙어서 명령어 치는 것".
- **스크립트를 서버로 복사** = 우리 PC에 있는 `install-ftp.sh` 파일을 서버 쪽 디스크로 옮기는 것. FileZilla(SFTP)로 업로드하거나, `scp` 명령으로 할 수 있음. **안 해도 됨** — 아래 "방법 B"처럼 PuTTY에서 명령어만 붙여 넣어도 설치 가능.

---

## 1. 준비

- 서버에 **SSH로 접속**할 수 있어야 합니다.  
  → [PuTTY_접속방법.md](../PuTTY_접속방법.md) 참고.
- 서버 OS: **Ubuntu 20.04 / 22.04** 기준입니다.

---

## 2. 스크립트 실행 (서버에서)

### 방법 A: 스크립트를 서버로 복사한 뒤 실행

1. 이 폴더의 **`install-ftp.sh`** 파일을 서버로 복사합니다.  
   - 예: FileZilla/SFTP로 `server-scripts/install-ftp.sh` 를 서버의 `/root/` 또는 `/home/계정/` 에 업로드  
   - 또는 로컬에서 SCP 사용:  
     `scp -P 22 server-scripts/install-ftp.sh root@서버IP:/root/`

2. SSH로 서버에 접속한 뒤 실행 권한을 주고 실행합니다.

   ```bash
   chmod +x /root/install-ftp.sh
   sudo /root/install-ftp.sh
   ```

3. 끝날 때까지 기다린 뒤, 마지막에 나오는 "접속 방법" 메시지를 확인합니다.

### 방법 B: 명령어만 복사해서 한 줄씩 실행

SSH 접속 후 아래를 **순서대로** 붙여 넣어 실행합니다.

```bash
# 1) vsftpd 설치
sudo apt-get update
sudo apt-get install -y vsftpd

# 2) 설정 백업
sudo cp /etc/vsftpd.conf /etc/vsftpd.conf.bak

# 3) 설정 덮어쓰기 (로컬 사용자 로그인, 쓰기 허용, 홈만 접근)
sudo sed -i 's/^anonymous_enable=.*/anonymous_enable=NO/' /etc/vsftpd.conf
sudo sed -i 's/^#*local_enable=.*/local_enable=YES/' /etc/vsftpd.conf
sudo sed -i 's/^#*write_enable=.*/write_enable=YES/' /etc/vsftpd.conf
echo "chroot_local_user=YES" | sudo tee -a /etc/vsftpd.conf
echo "allow_writeable_chroot=YES" | sudo tee -a /etc/vsftpd.conf
echo "pasv_enable=YES" | sudo tee -a /etc/vsftpd.conf
echo "pasv_min_port=21100" | sudo tee -a /etc/vsftpd.conf
echo "pasv_max_port=21110" | sudo tee -a /etc/vsftpd.conf

# 4) 서비스 재시작 및 부팅 시 자동 시작
sudo systemctl restart vsftpd
sudo systemctl enable vsftpd

# 5) 방화벽 사용 중이면 포트 열기
sudo ufw allow 21/tcp
sudo ufw allow 21100:21110/tcp
sudo ufw status
```

---

## 3. Windows에서 FTP 접속 (FileZilla)

| 항목 | 입력 값 |
|------|----------|
| 호스트 | 서버 IP 주소 (예: 카페24에서 받은 IP) |
| 사용자명 | SSH 로그인 계정 (예: `root` 또는 배포용 계정) |
| 비밀번호 | 해당 계정 비밀번호 |
| 포트 | **21** |

- **빠른 연결**에 위 값을 넣고 연결하면 됩니다.
- Passive 모드가 기본이므로, 방화벽에서 **21**, **21100~21110** 포트가 열려 있어야 합니다. (스크립트에서 ufw로 열어 둠)

---

## 4. FTP 전용 계정 만들기 (선택)

SSH와 같은 계정 대신, FTP만 쓰는 계정을 쓰려면 서버에서:

```bash
sudo adduser ftpuser
```

- `ftpuser` 대신 원하는 아이디 입력 가능.
- 비밀번호 입력 후, 나머지는 Enter로 넘겨도 됩니다.
- FileZilla에서는 **사용자명 = ftpuser**, **비밀번호 = 방금 설정한 비밀번호**로 접속하면 됩니다.
- 해당 사용자의 홈 디렉터리(예: `/home/ftpuser`)만 FTP로 보입니다.

---

## 5. 문제 해결

- **연결 거부 / 타임아웃**  
  - 서버 방화벽(ufw)에서 21, 21100~21110 포트가 열려 있는지 확인: `sudo ufw status`  
  - 카페24 등에서는 **서버 관리 페이지**에서 추가로 방화벽/포트 개방이 필요할 수 있습니다.

- **로그인 실패**  
  - 사용자명/비밀번호가 SSH 로그인과 동일한지 확인.  
  - FTP 전용 계정을 만들었다면 그 계정/비밀번호로 시도.

- **파일 올리기/쓰기 안 됨**  
  - 해당 디렉터리 권한: `chmod 755 /home/계정` 또는 `chmod 755 /home/계정/업로드경로`  
  - vsftpd 설정에 `write_enable=YES`, `allow_writeable_chroot=YES` 가 있는지 확인.

설치 후에도 접속이 안 되면, 서버에서 `sudo systemctl status vsftpd` 로 상태를 확인하고, 에러 메시지를 알려주시면 다음 단계 안내가 가능합니다.
