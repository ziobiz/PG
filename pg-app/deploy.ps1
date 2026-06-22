# PC에서 bootJar 빌드 후 서버 배포 (선택: SSH 업로드+재시작)
#
# 사용법:
#   cd pg-app
#   .\deploy.ps1                          # 빌드만
#   .\deploy.ps1 -Deploy                # 빌드 + 서버 업로드 + restart-pg-app.sh
#
# 최초 1회: 아래 $ServerHost 등을 본인 서버에 맞게 수정하거나 환경변수 PG_DEPLOY_HOST 설정

param(
    [switch]$Deploy
)

$ErrorActionPreference = "Stop"
$PgAppDir = $PSScriptRoot
$Jar = Join-Path $PgAppDir "build\libs\pg-app-0.0.1-SNAPSHOT.jar"

# ─── ★ 서버 SSH 배포 시 수정 ─────────────────────────────
$ServerHost = $env:PG_DEPLOY_HOST   # 예: "otlpay" 또는 "1.2.3.4"
$ServerUser = $env:PG_DEPLOY_USER   # 예: "root"
if (-not $ServerUser) { $ServerUser = "root" }
$RemoteDir  = $env:PG_DEPLOY_DIR    # 예: "/home/ftpuser/pg-app"
if (-not $RemoteDir) { $RemoteDir = "/home/ftpuser/pg-app" }
# ─────────────────────────────────────────────────────────

Write-Host "1) bootJar 빌드..."
Set-Location $PgAppDir
& .\gradlew.bat bootJar
if ($LASTEXITCODE -ne 0) { throw "bootJar 실패" }

Write-Host "   OK: $Jar"
Write-Host "   크기: $((Get-Item $Jar).Length) bytes"

if (-not $Deploy) {
    Write-Host ""
    Write-Host "다음 (FTP 수동 업로드 시):"
    Write-Host "  1. JAR 업로드 → $RemoteDir/build/libs/pg-app-0.0.1-SNAPSHOT.jar"
    Write-Host "  2. 서버에서: cd $RemoteDir && ./restart-pg-app.sh"
    Write-Host ""
    Write-Host "SSH 자동 배포: .\deploy.ps1 -Deploy  (PG_DEPLOY_HOST 환경변수 설정)"
    exit 0
}

if (-not $ServerHost) {
    throw "PG_DEPLOY_HOST 환경변수를 설정하세요. 예: `$env:PG_DEPLOY_HOST='your-server'"
}

$remoteJar = "${ServerUser}@${ServerHost}:${RemoteDir}/build/libs/pg-app-0.0.1-SNAPSHOT.jar"
Write-Host "2) JAR 업로드 → $remoteJar"
scp $Jar $remoteJar

Write-Host "3) 서버 재시작 (restart-pg-app.sh)..."
ssh "${ServerUser}@${ServerHost}" "cd '$RemoteDir' && chmod +x restart-pg-app.sh 2>/dev/null; ./restart-pg-app.sh"

Write-Host ""
Write-Host "배포 완료. 로그 확인:"
Write-Host "  ssh ${ServerUser}@${ServerHost} `"grep 'Started PgAppApplication' $RemoteDir/pg-app.log | tail -1`""
