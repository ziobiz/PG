# PG 통합관리자 — 서버 업로드 가이드 (otlpay.cafe24.com)

## 1. login.html 위치

| 구분 | 경로 |
|------|------|
| **로컬(개발)** | `d:\Delopment\PG\site\login.html` |
| **서버 접속 URL** | [http://otlpay.cafe24.com/login.html](http://otlpay.cafe24.com/login.html) |

---

## 2. 서버에 업로드할 위치

**Cafe24 호스팅**에서는 웹 문서 루트에 올립니다.

- **일반:** `public_html` 또는 `www` (호스팅 제어판에서 “웹 FTP 경로” 또는 “문서 루트”로 안내된 폴더)
- **업로드 방식:** 로컬의 **`site` 폴더 안의 내용 전체**를, 서버의 **문서 루트**에 그대로 업로드합니다.  
  즉, `site` 폴더 자체를 올리는 것이 아니라, `site` **안에 있는** 파일·폴더들을 루트에 두면 됩니다.

업로드 후 구조 예시:

```
문서 루트(public_html 등)
├── index.html
├── login.html
├── css/
│   └── site.css
├── js/
│   ├── config.js
│   ├── app.js
│   └── screens.js
└── (기타 site 안의 파일들)
```

---

## 3. 도메인 기준 URL (직관적 확인용)

아래 주소들은 모두 **http://otlpay.cafe24.com/** 기준입니다. 클릭하면 해당 페이지로 이동합니다.

| 용도 | URL |
|------|-----|
| **메인(첫 화면)** | [http://otlpay.cafe24.com/](http://otlpay.cafe24.com/) |
| **메인(index)** | [http://otlpay.cafe24.com/index.html](http://otlpay.cafe24.com/index.html) |
| **로그인** | [http://otlpay.cafe24.com/login.html](http://otlpay.cafe24.com/login.html) |

- 비로그인 시 자동 이동: [http://otlpay.cafe24.com/login.html](http://otlpay.cafe24.com/login.html)  
- 로그인 성공 시 이동: [http://otlpay.cafe24.com/index.html](http://otlpay.cafe24.com/index.html)  
- 로그아웃 시 이동: [http://otlpay.cafe24.com/login.html](http://otlpay.cafe24.com/login.html)  
- 좌측 로고 클릭 시: [http://otlpay.cafe24.com/index.html](http://otlpay.cafe24.com/index.html)

---

## 4. 업로드해야 할 파일 목록

로컬 **`site`** 폴더 내용을 문서 루트에 올리면 됩니다. 아래는 그 목록입니다.

### 4.1. 루트에 올릴 파일

| 로컬 경로 | 업로드 후 경로 |
|-----------|----------------|
| `site/index.html` | 문서 루트/`index.html` |
| `site/login.html` | 문서 루트/`login.html` |

### 4.2. css 폴더

| 로컬 경로 | 업로드 후 경로 |
|-----------|----------------|
| `site/css/site.css` | 문서 루트/`css/site.css` |

### 4.3. js 폴더

| 로컬 경로 | 업로드 후 경로 |
|-----------|----------------|
| `site/js/config.js` | 문서 루트/`js/config.js` |
| `site/js/app.js` | 문서 루트/`js/app.js` |
| `site/js/screens.js` | 문서 루트/`js/screens.js` |

### 4.4. 기타 (있는 경우)

- `site/README.md` — 업로드해도 되고, 서비스에는 불필요하면 제외해도 됩니다.

---

## 5. 한 줄 요약

- **login.html 위치:** 로컬 `d:\Delopment\PG\site\login.html` → 서버에서는 [http://otlpay.cafe24.com/login.html](http://otlpay.cafe24.com/login.html) 로 접속.
- **업로드 위치:** Cafe24 문서 루트(`public_html` 등)에 **`site` 폴더 안의 내용 전체**를 올린다.
- **업로드할 파일:**  
  `index.html`, `login.html`, `css/site.css`, `js/config.js`, `js/app.js`, `js/screens.js` (그리고 `site` 안에 있는 기타 자원이 있다면 함께 업로드).

이대로 업로드하면 [http://otlpay.cafe24.com/](http://otlpay.cafe24.com/) 로 메인, [http://otlpay.cafe24.com/login.html](http://otlpay.cafe24.com/login.html) 로 로그인 페이지에 접속할 수 있습니다.
