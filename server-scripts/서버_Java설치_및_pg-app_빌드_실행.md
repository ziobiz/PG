# 서버에서 Java 설치 + pg-app 빌드·실행 (PC 환경 없이)

PC에 Java/Gradle 없이, **FTP로 소스 올린 뒤 서버에서 빌드·실행**하는 순서입니다.

---

## 1. 서버에 Java 17 설치 (한 번만)

서버(otlpay)에 **SSH(PuTTY)** 접속한 뒤 아래를 **한 번에** 붙여 넣고 Enter:

```bash
sudo apt-get update && sudo apt-get install -y openjdk-17-jdk && java -version
```

`openjdk version "17.x.x"` 가 나오면 성공입니다.

---

## 2. pg-app 소스를 서버로 올리기 (FTP)

**FileZilla**로 서버에 접속한 뒤:

1. **로컬(왼쪽)**: `D:\Delopment\PG\pg-app` 폴더 전체 선택  
   (또는 Cursor 프로젝트의 `pg-app` 폴더)
2. **서버(오른쪽)**: `/root/pg-app` 또는 `/home/계정/pg-app` 같은 폴더로 이동
3. **pg-app 폴더 전체**를 드래그해서 서버로 업로드

반드시 **gradle/wrapper/gradle-wrapper.jar** 파일이 포함되도록 업로드하세요.  
(PC에서 한 번 `Invoke-WebRequest`로 받아 둔 JAR가 `pg-app/gradle/wrapper/` 안에 있어야 합니다.)

---

## 3. 서버에서 JAR 빌드

SSH 접속한 터미널에서:

```bash
cd /root/pg-app
chmod +x gradlew
./gradlew bootJar
```

끝나면 `build/libs/pg-app-0.0.1-SNAPSHOT.jar` 가 생성됩니다.

- **오류**: `gradle-wrapper.jar` 없음 → PC의 `pg-app/gradle/wrapper/gradle-wrapper.jar` 를 FTP로 서버 같은 경로에 업로드 후 다시 `./gradlew bootJar`

---

## 4. 서버에서 pg-app 실행

**DB 비밀번호**를 본인 환경에 맞게 바꾼 뒤 실행:

```bash
cd /root/pg-app
export DB_HOST=localhost
export DB_USER=pgadmin
export DB_PASSWORD=여기에_pgadmin_비밀번호
nohup java -jar build/libs/pg-app-0.0.1-SNAPSHOT.jar --server.port=8080 &
```

- `nohup ... &` → 터미널을 닫아도 앱이 계속 돌아갑니다.
- 포트를 바꾸려면 `--server.port=8081` 처럼 수정하세요.

---

## 5. 브라우저에서 확인

- 주소: **http://서버IP:8080**
- 로그인: **admin** / **admin1!**

---

## 6. 앱 종료·재시작

**프로세스 확인:**

```bash
ps aux | grep pg-app
```

**종료 (PID는 위에서 확인한 숫자):**

```bash
kill PID
```

**다시 실행:** 4번 명령 다시 실행.

---

## 요약

| 순서 | 할 일 |
|------|--------|
| 1 | 서버에 Java 17 설치 (`apt install openjdk-17-jdk`) |
| 2 | FTP로 **pg-app 폴더 전체** 서버에 업로드 (gradle-wrapper.jar 포함) |
| 3 | 서버에서 `cd pg-app` → `chmod +x gradlew` → `./gradlew bootJar` |
| 4 | `export DB_*` 후 `nohup java -jar build/libs/pg-app-0.0.1-SNAPSHOT.jar --server.port=8080 &` |
| 5 | 브라우저에서 http://서버IP:8080 접속 후 admin / admin1! 로그인 |
