# Cloudflare 리다이렉트 규칙 (406 해결)

api.icopay.co.kr 의 /, /login 406 오류를 Cloudflare에서 먼저 처리합니다.

---

## 설정 방법

### 1. Cloudflare 대시보드 접속

1. https://dash.cloudflare.com
2. **icopay.co.kr** 선택

### 2. Redirect Rules 메뉴

1. 왼쪽 메뉴 **Rules** (규칙) 클릭
2. **Redirect Rules** 선택
3. **Create rule** 클릭

### 3. 규칙 생성

**Rule name**: `api redirect to login`

**When incoming requests match...** (다음 조건일 때):
- Field: **URI Path**
- Operator: **equals** 또는 **starts with**
- Value: `/` (또는 `/` 와 `/login` 두 개의 규칙)

**또는** Custom filter expression 사용:
```
(http.request.uri.path eq "/" or http.request.uri.path eq "/login" or http.request.uri.path starts_with "/login/") and http.host eq "api.icopay.co.kr"
```

**Then...** (다음 작업):
- Type: **Dynamic**
- Expression: `concat("https://icopay.co.kr/login.html")`
- Status code: **302**

**또는** 간단히:
- Type: **Static**
- URL: `https://icopay.co.kr/login.html`
- Status code: **302**

### 4. 적용 대상

- **Scope**: 이 규칙이 적용될 요청
- **Hostname**: `api.icopay.co.kr` 선택
- **URI Path**: `/` 또는 `/login` 또는 `starts with /login`

### 5. 저장

**Deploy** 클릭

---

## 요약 (간단 버전)

1. **Rules** → **Redirect Rules** → **Create rule**
2. **If**: Hostname equals `api.icopay.co.kr` AND (URI Path equals `/` OR URI Path equals `/login`)
3. **Then**: Redirect to `https://icopay.co.kr/login.html`, 302
4. **Deploy**

---

## favicon.ico

favicon 406은 별도 규칙으로:
- **If**: Hostname equals `api.icopay.co.kr` AND URI Path equals `/favicon.ico`
- **Then**: Redirect to `https://icopay.co.kr/favicon.ico` (icopay에 favicon이 있다면) 또는 Skip (규칙 없이)

---

이렇게 하면 요청이 서버에 도달하기 전에 Cloudflare에서 리다이렉트됩니다.
