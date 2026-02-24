# tb_user 테이블 생성 (PC에서 앱 안 돌리고, 서버 DB에서만)

PC에 Java/Gradle을 설치하지 않고, **서버 DB(pgdev)** 에서 SQL만 실행해서 **tb_user** 테이블과 **admin** 계정을 만드는 방법입니다.

---

## 1. pgAdmin에서 실행 (가장 쉬움)

1. **pgAdmin** 실행 후, 서버(ICOPAY) 접속.
2. 왼쪽에서 **pgdev** DB를 **더블클릭**해서 연결.
3. **Tools** → **Query Tool** (또는 pgdev 우클릭 → **Query Tool**).
4. **init-tb_user.sql** 파일 내용을 **전부 복사**해서 Query Tool에 붙여 넣기.
5. **F5** 또는 ▶ (Execute) 버튼 클릭.
6. 아래 **Messages** 탭에 "CREATE TABLE", "INSERT 0 1" 등이 나오면 성공.
7. 왼쪽에서 **pgdev** → **Schemas** → **public** → **Tables** 를 **새로고침(F5)** 하면 **tb_user** 가 보입니다.
8. **tb_user** 우클릭 → **View/Edit Data** → **All Rows** 로 **admin** 행 확인.

---

## 2. 서버(otlpay)에서 psql로 실행

FTP로 **init-tb_user.sql** 을 서버에 올린 뒤, SSH로 접속해서:

```bash
sudo -u postgres psql -d pgdev -f /경로/init-tb_user.sql
```

(경로는 파일을 둔 위치로 바꾸세요.)

---

## 3. 정리

- **PC에서는** Java/Gradle 없이, **코드만 편집**하고 **FTP로 서버에 올리는** 흐름 그대로 쓰면 됩니다.
- **tb_user**는 위 SQL 한 번 실행해서 서버 **pgdev** 에 만들어 두면 됩니다.
- 나중에 **서버에서** pg-app(JAR)을 실행할 때는, 이미 **tb_user** 와 **admin** 이 있으므로 그대로 로그인할 수 있습니다.

이후 서버에 Java 설치하고 JAR 올려서 실행하는 방법은 별도 가이드(로컬실행_및_서버배포_가이드.md 등)를 참고하면 됩니다.
