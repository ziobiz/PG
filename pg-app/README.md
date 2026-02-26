# PG 통합관리자 (Spring Boot)

Java 17 + Spring Boot 3 + PostgreSQL + Thymeleaf + Spring Security 기반 개발 서버용 프로젝트입니다.

---

## 요구사항

- **Java 17** (JDK 17)
- **Gradle 8.x** (또는 IDE에서 Gradle 래퍼 사용)

---

## DB 설정

- **PostgreSQL** DB `pgdev`, 사용자 `pgadmin` 이 있어야 합니다.
- 비밀번호는 **환경변수** 또는 **application-local.yml** 로 설정하세요.

### 방법 1: 환경변수 (서버 배포 시)

```bash
export DB_HOST=localhost
export DB_USER=pgadmin
export DB_PASSWORD=line2025!@
java -jar pg-app.jar
```

### 방법 2: 로컬 설정 파일

1. `src/main/resources/application-local.yml.example` 를 복사해 `application-local.yml` 생성
2. `password` 에 DB 비밀번호 입력
3. 실행 시 `--spring.profiles.active=local` 추가

---

## 로컬에서 실행

**첫 실행 시** DB 비밀번호를 쓰려면 `local` 프로파일을 지정하세요 (이미 `application-local.yml` 예시가 있음).

### Gradle 래퍼가 있는 경우 (gradlew 사용)

```bash
cd pg-app
# Windows (로컬 DB 사용 시)
gradlew.bat bootRun --args="--spring.profiles.active=local"

# 또는 일반 실행 (DB_PASSWORD 환경변수 필요)
gradlew.bat bootRun
```

```bash
# Mac/Linux
./gradlew bootRun --args='--spring.profiles.active=local'
```

**gradlew 실행이 안 되면** (Gradle wrapper JAR 없음):  
Cursor에서 **pg-app 폴더를 열고** 터미널에서 `gradle wrapper` 를 실행하거나, [Gradle 설치](https://gradle.org/install/) 후 `gradle wrapper` 실행.  
또는 Cursor/IntelliJ에서 **PgAppApplication.java** 를 열고 **Run** 으로 실행해 보세요.

### 프로파일로 DB 설정 적용

```bash
gradlew.bat bootRun --args='--spring.profiles.active=local'
```

### PostgreSQL 없이 H2로 테스트 (dev 프로파일)

PostgreSQL이 없거나 빠르게 테스트할 때:

```bash
gradlew.bat bootRun --args='--spring.profiles.active=dev'
```

- H2 인메모리 DB 사용, 시드 데이터 자동 생성
- 업체관리 등 기본 메뉴에서 샘플 업체(HQ01, M001, M002, A01~E01 등) 조회 가능

---

## JAR 빌드 (서버 배포용)

```bash
cd pg-app
gradlew.bat bootJar
```

생성 파일: `build/libs/pg-app-0.0.1-SNAPSHOT.jar`

서버(otlpay)에 이 JAR를 업로드한 뒤:

```bash
export DB_HOST=localhost
export DB_USER=pgadmin
export DB_PASSWORD=비밀번호
nohup java -jar pg-app-0.0.1-SNAPSHOT.jar --server.port=8080 &
```

---

## 로그인 (DB 사용자)

- **아이디**: `admin`
- **비밀번호**: `admin1!`

최초 실행 시 DB에 `tb_user` 테이블이 생성되고, `admin` 계정이 자동 등록됩니다. 이후 로그인은 DB에서 검증합니다.

---

## 프로젝트 구조

```
pg-app/
├── build.gradle
├── src/main/java/com/pg/
│   ├── PgAppApplication.java
│   ├── config/SecurityConfig.java, DataLoader.java
│   ├── controller/LoginController.java, HomeController.java
│   ├── entity/AppUser.java
│   ├── repository/UserRepository.java
│   └── service/DbUserDetailsService.java
├── src/main/resources/
│   ├── application.yml
│   ├── templates/   (login, main, transactions, merchants)
│   └── static/css/  (login.css, dashboard.css)
└── README.md
```
