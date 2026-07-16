# PC에서 bootJar 빌드 후 서버 배포
#
# 권장 (저장소 루트):
#   .\scripts\deploy\Setup-Credentials.ps1   # 최초 1회
#   .\scripts\deploy\Deploy-Prod.ps1         # 빌드+업로드+재시작
#   .\scripts\deploy\Deploy-Prod.ps1 -SqlFiles @('pg-app/src/main/resources/db/V231_....sql')
#
# 하위 호환:
#   cd pg-app
#   .\deploy.ps1              # 빌드만
#   .\deploy.ps1 -Deploy      # 환경변수 PG_DEPLOY_* 사용 시 SSH 배포

param(
    [switch]$Deploy
)

$ErrorActionPreference = "Stop"
$PgAppDir = $PSScriptRoot
$RepoRoot = Split-Path $PgAppDir -Parent
$NewDeploy = Join-Path $RepoRoot "scripts\deploy\Deploy-Prod.ps1"

if ($Deploy -and (Test-Path $NewDeploy)) {
    Write-Host "scripts/deploy/Deploy-Prod.ps1 로 위임합니다."
    & $NewDeploy
    exit $LASTEXITCODE
}

$Jar = Join-Path $PgAppDir "build\libs\pg-app-0.0.1-SNAPSHOT.jar"

$ServerHost = $env:PG_DEPLOY_HOST
$ServerUser = $env:PG_DEPLOY_USER
if (-not $ServerUser) { $ServerUser = "root" }
$RemoteDir  = $env:PG_DEPLOY_DIR
if (-not $RemoteDir) { $RemoteDir = "/home/ftpuser/pg-app" }

Write-Host "1) bootJar 빌드..."
Set-Location $PgAppDir
& .\gradlew.bat bootJar
if ($LASTEXITCODE -ne 0) { throw "bootJar 실패" }

Write-Host "   OK: $Jar"
Write-Host "   크기: $((Get-Item $Jar).Length) bytes"

if (-not $Deploy) {
    Write-Host ""
    Write-Host "권장 자동 배포: 저장소 루트에서"
    Write-Host "  .\scripts\deploy\Deploy-Prod.ps1"
    Write-Host ""
    Write-Host "또는 환경변수 방식: .\deploy.ps1 -Deploy  (PG_DEPLOY_HOST 설정)"
    exit 0
}

if (-not $ServerHost) {
    throw "PG_DEPLOY_HOST 가 없습니다. 또는 .\scripts\deploy\Deploy-Prod.ps1 을 사용하세요."
}

$remoteJar = "${ServerUser}@${ServerHost}:${RemoteDir}/build/libs/pg-app-0.0.1-SNAPSHOT.jar"
Write-Host "2) JAR 업로드 → $remoteJar"
scp $Jar $remoteJar

Write-Host "3) 서버 재시작 (restart-pg-app.sh)..."
ssh "${ServerUser}@${ServerHost}" "cd '$RemoteDir' && chmod +x restart-pg-app.sh 2>/dev/null; ./restart-pg-app.sh"

Write-Host ""
Write-Host "배포 완료."
