# FTP 서버 설치 — 이해하기 쉬운 순서

"서버에 스크립트 복사"나 "SSH 접속"이 뭔지 모르겠다면, 아래만 따라 하시면 됩니다.

---

## 먼저 알아둘 것 (한 줄씩)

| 말 | 뜻 |
|----|-----|
| **서버** | 카페24에서 받은 그 컴퓨터(IP 주소 있는 그쪽). 우리가 파일을 올릴 "저쪽 컴퓨터". |
| **SSH로 접속** | **PuTTY**를 켜서, 서버 IP 넣고, **Open** 누르고, 로그인(계정/비밀번호) 하는 것. 즉 "서버에 붙어서 명령어 치는 상태". |
| **스크립트를 서버로 복사** | 우리 PC에 있는 `install-ftp.sh` 파일을 "서버 쪽 디스크"로 옮기는 것. FileZilla로 올리거나, 나중에 배울 `scp` 명령으로 할 수 있음. |

**쉬운 방법**: 복사 없이, **PuTTY만** 켜서 아래 "2단계"에 있는 **명령어를 그대로 복사해서 붙여 넣기**만 하면 됩니다.

---

## 1단계: 서버에 "접속"하기 (SSH = PuTTY로 접속)

우리 PC에서 **PuTTY**를 여는 것부터가 "서버에 접속하는 것"입니다.

1. **PuTTY** 실행 (putty.exe 더블클릭).
2. **Host Name** 칸에 **서버 IP** 입력 (예: `123.456.78.90`).
3. **Port** 에 **22** 입력.
4. **Open** 클릭.
5. 처음이면 "Security Alert" 나오면 **Accept** 클릭.
6. **login as:** 라고 나오면 → 서버 계정 입력 (예: `root`) 후 **Enter**.
7. **password:** 라고 나오면 → 서버 비밀번호 입력 후 **Enter** (화면에 안 보여도 그냥 입력 후 Enter).
8. 화면에 `root@서버이름:~#` 같은 글자가 보이면 **접속 완료**입니다.

→ 이제 이 검은 창에 쓰는 글자 = **서버한테 시키는 명령**입니다.

---

## 2단계: FTP 설치 명령어 넣기 (복사해서 붙여 넣기)

**스크립트 파일을 서버로 복사하지 않고**, 아래 **한 덩어리 전체**를 복사해서 PuTTY 창에 **붙여 넣기** 한 번 하세요.

1. 아래 ``` 부터 ``` 까지 **전부** 드래그해서 복사 (Ctrl+C).
2. PuTTY 검은 창 **안**을 클릭한 뒤 **우클릭** 하면 자동으로 붙여 넣기 됩니다 (또는 Ctrl+Shift+V).
3. **Enter** 한 번 누르세요.
4. 비밀번호 물어보면 **서버 로그인 비밀번호** 입력 후 Enter.
5. 끝날 때까지 기다리면 "FTP(vsftpd) 설치 완료" 같은 문구가 나옵니다.

```bash
sudo apt-get update && sudo apt-get install -y vsftpd && \
sudo cp /etc/vsftpd.conf /etc/vsftpd.conf.bak && \
sudo sed -i 's/^anonymous_enable=.*/anonymous_enable=NO/' /etc/vsftpd.conf && \
sudo sed -i 's/^#*local_enable=.*/local_enable=YES/' /etc/vsftpd.conf && \
sudo sed -i 's/^#*write_enable=.*/write_enable=YES/' /etc/vsftpd.conf && \
echo "chroot_local_user=YES" | sudo tee -a /etc/vsftpd.conf && \
echo "allow_writeable_chroot=YES" | sudo tee -a /etc/vsftpd.conf && \
echo "pasv_enable=YES" | sudo tee -a /etc/vsftpd.conf && \
echo "pasv_min_port=21100" | sudo tee -a /etc/vsftpd.conf && \
echo "pasv_max_port=21110" | sudo tee -a /etc/vsftpd.conf && \
sudo systemctl restart vsftpd && sudo systemctl enable vsftpd && \
(sudo ufw allow 21/tcp 2>/dev/null; sudo ufw allow 21100:21110/tcp 2>/dev/null; sudo ufw status 2>/dev/null || true) && \
echo "" && echo "=== FTP 설치 완료 ===" && echo "접속: 호스트=서버IP, 포트=21, 사용자=root, 비밀번호=서버비밀번호"
```

이게 끝나면 **서버에 FTP가 설치된 것**입니다. 스크립트 파일을 서버로 "복사"할 필요 없습니다.

---

## 3단계: 우리 PC(Windows)에서 FTP로 접속해 보기

1. **FileZilla** 실행.
2. 위쪽 **빠른 연결** 칸에 넣기:
   - **호스트:** 서버 IP (PuTTY에 넣었던 그 숫자)
   - **사용자명:** `root` (또는 PuTTY 로그인할 때 쓰는 계정)
   - **비밀번호:** 서버 비밀번호
   - **포트:** `21`
3. **빠른 연결** 버튼 클릭.

오른쪽에 서버 쪽 폴더가 보이면 **FTP 접속 성공**입니다.

---

## 정리 (무슨 순서인지만)

1. **PuTTY**로 서버에 접속한다 = "SSH로 접속" = 서버한테 명령어 칠 준비.
2. 그 PuTTY 창에 **위 2단계 명령어**를 복사해서 붙여 넣고 Enter = FTP 프로그램(vsftpd) 설치.
3. **FileZilla**에서 서버 IP, root, 비밀번호, 포트 21 로 접속 = 우리 PC에서 서버로 파일 올리기/내리기.

"스크립트를 서버로 복사"는 **나중에** 스크립트 파일(install-ftp.sh)을 수정해서 쓰고 싶을 때만 하면 됩니다. 지금은 **2단계 명령어만** 해도 됩니다.
