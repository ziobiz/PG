# MCP 브라우저 설정 가이드 (Cursor IDE)

브라우저 자동화(로그인, 클릭, 폼 입력 등)를 Cursor에서 쓰려면 MCP 브라우저 서버를 켜야 합니다.

---

## 방법 1: Cursor 내장 브라우저 MCP 켜기 (cursor-ide-browser)

`cursor-ide-browser`는 Cursor에 **내장된** MCP라서 별도 설치 없이 “켜기”만 하면 됩니다.

### 1단계: 설정 열기

- **Windows**: `Ctrl + ,` (콤마)
- **Mac**: `Cmd + ,`
- 또는 메뉴 **File → Preferences → Cursor Settings**

### 2단계: MCP 메뉴로 이동

- 왼쪽에서 **Tools & MCP** (또는 **MCP**) 선택
- **MCP Servers** / 서버 목록이 보이는 화면으로 이동

### 3단계: cursor-ide-browser 켜기

- 목록에 **cursor-ide-browser** 또는 **Browser** 같은 이름이 있으면 **토글을 ON**으로
- “Add new MCP server”만 있고 목록에 없으면, Cursor가 **최신 버전**인지 확인 후 재시작

### 4단계: Cursor 완전히 재시작

- Cursor를 **완전히 종료**했다가 다시 실행
- MCP는 재시작 후에만 적용됩니다

### 참고 (알려진 이슈)

- 일부 환경에서는 `cursor-ide-browser`가 “No server found with tool: browser_navigate” 로 **동작하지 않는 버그**가 보고된 적 있습니다.
- 그럴 경우 **방법 2 (Playwright MCP)** 를 사용하는 것이 가장 확실합니다.

---

## 방법 2: Playwright MCP 사용 (권장 – 동작이 확실함)

Microsoft에서 만든 **Playwright MCP**를 쓰면 브라우저 자동화가 안정적으로 동작합니다.

### 1단계: Node.js 확인

- 터미널에서 `node -v` 실행
- **v18 이상**이어야 합니다. 없으면 [nodejs.org](https://nodejs.org)에서 설치

### 2단계: MCP 설정 파일 만들기

**프로젝트에만** 쓰려면:

- 프로젝트 루트에 `.cursor` 폴더가 없으면 만듭니다.
- `.cursor` 안에 `mcp.json` 파일을 만들고 아래 내용을 넣습니다.

**경로 예시 (이 프로젝트):**  
`d:\Delopment\PG\.cursor\mcp.json`

**내용:**

```json
{
  "mcpServers": {
    "playwright": {
      "command": "npx",
      "args": ["-y", "@playwright/mcp@latest"]
    }
  }
}
```

**전역으로** 쓰려면 (모든 프로젝트):

- **Windows**: `C:\Users\ziobi\.cursor\mcp.json`
- **Mac**: `~/.cursor/mcp.json`
- 같은 형식으로 `mcpServers`에 `playwright` 항목 추가

### 3단계: Cursor 완전히 재시작

- Cursor를 종료한 뒤 다시 실행

### 4단계: 동작 확인

- **Composer** 또는 **Agent** 모드에서 채팅 열기
- “지금 사용 가능한 MCP 도구 목록 알려줘” 또는 “playwright 브라우저로 뭐 할 수 있어?”라고 물어보기
- Playwright 관련 도구(예: 브라우저 열기, 페이지 이동, 클릭 등)가 보이면 설정 성공

---

## 방법 3: Browser MCP (브라우저 확장 프로그램 방식)

- [Browser MCP](https://browsermcp.io/) 는 **브라우저 확장 프로그램**을 설치한 뒤, 그 브라우저를 Cursor가 제어하는 방식입니다.
- 확장 프로그램 설치 + MCP 서버 설정이 필요하므로, 위 **방법 2 (Playwright)** 가 더 간단합니다.

---

## 요약

| 방법 | 난이도 | 비고 |
|------|--------|------|
| **1. cursor-ide-browser** | 쉬움 (설정에서 ON) | 내장이라 설정만 하면 되지만, 일부 환경에서 동작 안 할 수 있음 |
| **2. Playwright MCP** | 쉬움 (mcp.json 한 번 작성) | Node.js 필요, 동작이 확실함 → **권장** |
| **3. Browser MCP** | 보통 (확장 프로그램 설치) | 브라우저 확장 필요 |

**추천:**  
먼저 **설정에서 cursor-ide-browser를 켜고 재시작**해 보고,  
도구가 안 보이거나 에러가 나면 **Playwright MCP**를 `mcp.json`에 추가해 사용하세요.

---

## Playwright MCP 설정 후 로그인 자동화 예시

설정이 끝나면 채팅에서 예를 들어 이렇게 요청할 수 있습니다:

- “https://otl.soonpay.co.kr/login 에 접속해서 아이디 otlotl, 비밀번호 otlotl1! 로 로그인해 줘.”
- (OTP가 있으면) “로그인 화면까지 해 줘.” 등

OTP가 있는 사이트는 로그인 버튼까지 진행한 뒤, OTP 번호는 사용자가 직접 입력해야 할 수 있습니다.
