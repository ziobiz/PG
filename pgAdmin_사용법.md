# pgAdmin 사용법 (Windows)

PostgreSQL을 GUI로 다루는 **pgAdmin 4** 기본 사용법입니다.

---

## 1. 설치·실행

- **다운로드**: https://www.pgadmin.org/download/pgadmin-4-windows/
- **Windows 64-bit** 설치 파일 받아서 설치 후, pgAdmin 4 실행.
- 처음 실행 시 **마스터 비밀번호**를 한 번 설정하라고 나오면, pgAdmin 로그인용으로 설정해 두면 됩니다 (본인만 아는 비밀번호).

---

## 2. 서버 연결 추가

PostgreSQL이 설치된 **서버(컴퓨터)** 에 접속하려면 "서버"를 등록해야 합니다.

1. 왼쪽 **Browser** 창에서 **Servers** 위에서 **우클릭** → **Register** → **Server...**
2. **General** 탭
   - **Name**: 아무 이름 (예: `로컬 PostgreSQL`, `pgdev 서버`)
3. **Connection** 탭
   - **Host name/address**: PostgreSQL이 돌아가는 곳  
     - 본인 PC에 PostgreSQL 설치했으면 → `localhost`  
     - 원격 서버(카페24 등)면 → 서버 IP 주소
   - **Port**: `5432` (기본값)
   - **Maintenance database**: `postgres` (그대로 두면 됨)
   - **Username**: DB 사용자 (예: `pgadmin` 또는 `postgres`)
   - **Password**: 해당 사용자 비밀번호  
     - **Save password?** 체크하면 다음부터 비밀번호 안 물어봄
4. **Save** 클릭

이제 왼쪽 트리에서 **Servers** → 방금 만든 서버 이름을 **더블클릭**하면 연결됩니다. 비밀번호 물어보면 입력 후 OK.

---

## 3. 화면 구성 (연결 후)

왼쪽 **Browser** 트리 구조는 대략 이렇게 됩니다.

```
Servers
 └── [서버 이름]
      └── Databases
           └── pgdev          ← 우리 앱이 쓰는 DB
                ├── Schemas
                │    └── public
                │         ├── Tables    ← 테이블들 (tb_user 등)
                │         ├── Views
                │         └── ...
                ├── Login/Group Roles
                └── ...
```

- **Databases** → DB 목록. `pgdev` 더블클릭하면 해당 DB가 펼쳐짐.
- **Schemas** → **public** → **Tables** 에서 테이블 확인·데이터 보기.

---

## 4. 테이블 데이터 보기·수정

1. 왼쪽에서 **Databases** → **pgdev** → **Schemas** → **public** → **Tables** 펼치기.
2. 보고 싶은 테이블(예: **tb_user**) **우클릭** → **View/Edit Data** → **All Rows**.
3. 오른쪽에 **데이터 그리드**가 나옵니다.  
   - **필터**: 상단 필터 아이콘으로 조건 줄 수 있음.  
   - **새 행**: 맨 아래 빈 행에 값 넣고 저장.  
   - **수정**: 셀 더블클릭 후 값 수정 후 저장.  
   - **삭제**: 행 선택 후 삭제 버튼 또는 우클릭 메뉴.

phpMyAdmin처럼 "테이블 선택 → 데이터 보기 → 셀 클릭해서 수정" 하는 흐름과 같습니다.

---

## 5. SQL 실행 (쿼리 도구)

1. **Tools** 메뉴 → **Query Tool** 클릭 (또는 상단 **SQL 아이콘**).
2. 또는 왼쪽에서 **pgdev** 선택 후 **Tools** → **Query Tool**.
3. 위쪽 **쿼리 입력 창**에 SQL 입력 후:
   - **전체 실행**: F5 또는 ▶ (Execute/Refresh) 버튼.
   - **선택한 부분만 실행**: 실행할 문장만 드래그 후 F5.

예시:

```sql
-- tb_user 목록
SELECT * FROM public.tb_user;

-- 특정 사용자만
SELECT * FROM public.tb_user WHERE username = 'admin';
```

결과는 아래 **Data Output** 탭에 표시됩니다.

---

## 6. 요약 (자주 쓰는 동작)

| 하고 싶은 것 | 하는 방법 |
|-------------|-----------|
| 서버 접속 | Servers 우클릭 → Register → Server → Connection에 호스트/포트/사용자/비밀번호 입력 → Save |
| DB 선택 | 왼쪽 트리에서 Servers → [서버] → Databases → pgdev |
| 테이블 데이터 보기 | pgdev → Schemas → public → Tables → 테이블 우클릭 → View/Edit Data → All Rows |
| SQL 실행 | Query Tool 열기 (Tools → Query Tool) → SQL 입력 → F5 |
| 테이블 구조 보기 | 테이블 우클릭 → Properties → Columns 탭 |

---

## 7. 로컬 DB(pgdev) 접속 예시

- **Host**: `localhost`  
- **Port**: `5432`  
- **Username**: `pgadmin`  
- **Password**: (application-local.yml 또는 환경변수에 넣은 비밀번호)  
- **Database**: 연결 후 왼쪽에서 **pgdev** 선택해서 사용.

이렇게 연결해 두면 pg-app에서 쓰는 `tb_user` 등 테이블을 pgAdmin에서 바로 보고 수정할 수 있습니다.
