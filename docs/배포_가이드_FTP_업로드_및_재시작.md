# 배포 가이드 - FTP 업로드 및 서버 재시작

> 개발 완료 후 서버에 올릴 파일과 절차를 정리합니다.

---

## 1. 요약 (한눈에 보기)

| 수정한 것 | FTP 업로드 | 서버 재시작 | 비고 |
|-----------|------------|-------------|------|
| **site/** (HTML, JS, CSS) | ✅ 필요 | ❌ 불필요 | 파일만 올리면 바로 반영 |
| **pg-app/** (Java 소스) | ✅ 필요 | ✅ 필요 | **빌드 후** JAR 올리고 재시작 |
| **pg-app/** (application.yml 등 설정만) | ✅ 필요 | ✅ 필요 | 빌드 없이 올리고 재시작 |
| **PostgreSQL 데이터/스키마** | ❌ FTP 아님 | 앱·DB 작업 | [DB_서버_반영_가이드.md](./DB_서버_반영_가이드.md) 참고 |

---

## 2. site 폴더만 수정했을 때

**HTML, JavaScript, CSS**만 바꿨다면:

1. 수정한 파일을 FTP로 `site/` 경로에 업로드
2. **끝** (재시작 없음)

브라우저 새로고침(F5) 또는 캐시 비우고 새로고침(Ctrl+Shift+R) 후 확인.

### 로그인 화면 브랜딩(로고·왼쪽 배경)

- API는 **업체 코드(`org`)** 가 있어야 브랜딩을 내려줍니다.  
  예: `https://otlpay.cafe24.com/login.html?org=본사업체코드`
- 카페24는 정적 호스팅만 하므로 `site/js/config.js` 에서 **API 베이스**가 실제 pg-app 주소를 가리켜야 합니다. (`otlpay.cafe24.com` 은 코드에서 자동 지정, 백엔드가 다르면 해당 상수 수정)
- 서버(pg-app) `application.yml` 의 **CORS**에 `https://otlpay.cafe24.com` 등이 포함되어 있어야 브라우저에서 API 호출이 됩니다.

### 업로드 예시

| 로컬 경로 | 서버 경로 (예시) |
|-----------|------------------|
| `site/js/config.js` | `/home/ftpuser/site/js/config.js` |
| `site/js/app.js` | `/home/ftpuser/site/js/app.js` |
| `site/js/screens.js` | `/home/ftpuser/site/js/screens.js` |
| `site/index.html` | `/home/ftpuser/site/index.html` |

---

## 3. pg-app(백엔드) 수정했을 때

### 3-1. Java 소스(.java)를 수정한 경우

**반드시 빌드 → JAR 업로드 → 재시작** 순서로 진행.

#### 1단계: JAR 빌드

로컬 또는 서버에서:

```bash
cd pg-app
./gradlew bootJar
```

생성 파일: `pg-app/build/libs/pg-app-0.0.1-SNAPSHOT.jar`

#### 2단계: FTP 업로드

| 업로드할 파일 | 서버 경로 (예시) |
|--------------|------------------|
| `pg-app/build/libs/pg-app-0.0.1-SNAPSHOT.jar` | 서버 pg-app `build/libs/` (기존 JAR 덮어쓰기). 예: `/home/ftpuser/pg-app/build/libs/` |

#### 3단계: 서버 재시작

SSH 접속 후 (pg-app 루트는 서버마다 다름):

```bash
cd /home/ftpuser/pg-app
./restart-pg-app.sh
```

(최초 1회: `chmod +x restart-pg-app.sh`, `DB_PASSWORD` 설정 필요)

---

### 3-2. 설정 파일만 수정한 경우 (application.yml 등)

Java 소스는 건드리지 않고 **application.yml, application-*.yml** 등만 수정했다면:

1. 수정한 설정 파일을 FTP로 `pg-app/` 경로에 업로드
2. SSH로 서버 접속 후 재시작:

```bash
cd /home/ftpuser/pg-app
./restart-pg-app.sh
```

빌드는 하지 않아도 됩니다. (단, **JAR 안에만** 들어 있는 설정은 `application.yml` 을 classpath 밖에 두는 방식이 아니면 JAR 재빌드가 필요할 수 있음.)

---

## 4. 개발 완료 시 체크리스트

개발이 끝나면 아래를 확인하세요.

### 4-1. 업로드 대상 파일 목록

수정/추가된 파일을 나열합니다. 예:

```
site/js/app.js
site/js/screens.js
site/js/api.js
pg-app/build/libs/pg-app-0.0.1-SNAPSHOT.jar   ← Java 수정 시
```

### 4-2. 각 파일의 변경 내용

- 업로드할 파일의 **변경된 부분** 또는 **전체 내용**을 제공
- FTP로 덮어쓰기 할 수 있도록 구체적으로 명시

### 4-3. DB 추가할 내용

- 실행할 SQL이 있으면 제공
- 없으면 **"없음"** 으로 표시

### 4-4. 확인 링크

- 메인: [https://icopay.co.kr/](https://icopay.co.kr/)
- API: [https://api.icopay.co.kr/](https://api.icopay.co.kr/)

---

## 5. 자주 하는 실수

| 잘못된 경우 | 올바른 방법 |
|-------------|-------------|
| Java 수정 후 JAR 빌드 없이 재시작 | `./gradlew bootJar` 후 JAR 업로드 → 재시작 |
| site만 수정했는데 pg-app 재시작 | site는 업로드만 하면 됨 |
| JAR를 잘못된 경로에 업로드 | `build/libs/` 폴더에 넣어야 함 |

---

## 6. 서버 재시작 상세

재시작 스크립트 사용법은 [서버_재시작_방법.md](./서버_재시작_방법.md) 참고.

**한 줄 요약:**

```bash
cd /root/pg-app
./restart-pg-app.sh
```
