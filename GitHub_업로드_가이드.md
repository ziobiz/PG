# PG 프로젝트 GitHub/GitLab 업로드 가이드

> GitHub와 GitLab을 Cursor에 연결하셨다면, 이제 프로젝트를 업로드하면 됩니다.

---

## 1. 이미 완료된 작업

- [x] Git 저장소 초기화 (`git init`)
- [x] `.gitignore` 설정 (node_modules, 빌드 산출물, 레거시 소스 제외)
- [x] PG소스_CD, PG CD 폴더 제외 (용량 큰 참고 자료)

---

## 2. 업로드 실행 방법

### 방법 A: 배치 파일 실행 (권장)

1. **Cursor를 완전히 종료**하거나, PG 폴더를 닫은 상태에서 실행 (lock 충돌 방지)
2. `GitHub_업로드_실행.bat` 더블클릭
3. "index.lock" 오류가 나면 → Cursor 종료 후 배치 파일 다시 실행
4. 완료 후 아래 3단계로 이동

### 방법 B: 터미널에서 직접 실행

PowerShell 또는 명령 프롬프트에서:

```powershell
cd d:\Delopment\PG

# lock 파일이 있으면 제거
Remove-Item .git\index.lock -Force -ErrorAction SilentlyContinue

# 파일 추가 및 커밋
git add .
git commit -m "Initial commit: PG 솔루션 프로젝트"
```

---

## 3. GitHub/GitLab에 저장소 생성 및 Push

### GitHub

1. **저장소 생성**: https://github.com/new
   - Repository name: `PG` (또는 원하는 이름)
   - Public 선택
   - **README, .gitignore 추가하지 않음** (이미 로컬에 있음)

2. **Push 명령어** (사용자명을 본인 것으로 변경):

```powershell
cd d:\Delopment\PG
git remote add origin https://github.com/사용자명/PG.git
git branch -M main
git push -u origin main
```

### GitLab

1. **프로젝트 생성**: https://gitlab.com/projects/new
   - Project name: `PG`
   - Visibility: Public
   - **Initialize with README 체크 해제**

2. **Push 명령어**:

```powershell
cd d:\Delopment\PG
git remote add origin https://gitlab.com/사용자명/PG.git
git branch -M main
git push -u origin main
```

### GitHub와 GitLab 둘 다 사용하는 경우

```powershell
# GitHub를 origin으로
git remote add origin https://github.com/사용자명/PG.git

# GitLab을 별도 remote로
git remote add gitlab https://gitlab.com/사용자명/PG.git

git branch -M main
git push -u origin main          # GitHub에 push
git push -u gitlab main          # GitLab에도 push
```

---

## 4. 인증

- **HTTPS**: push 시 GitHub/GitLab 로그인 창이 뜹니다.
- **토큰 사용**: 비밀번호 대신 Personal Access Token 사용 권장
  - GitHub: Settings → Developer settings → Personal access tokens
  - GitLab: Preferences → Access Tokens

---

## 5. 완료 후

- Cursor에서 `d:\Delopment\PG` 폴더를 열고 평소처럼 개발
- GitHub/GitLab 연결 덕분에 Cursor가 저장소 컨텍스트를 활용합니다.
