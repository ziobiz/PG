# Windows: pg-app 을 dev 프로파일로 재기동 (8080 LISTEN 종료 후 bootRun)
# 저장소 루트에서: powershell -ExecutionPolicy Bypass -File scripts\restart-pg-dev.ps1
$ErrorActionPreference = 'Continue'
$repoRoot = Split-Path -Parent $PSScriptRoot
$pgApp = Join-Path $repoRoot 'pg-app'
$gradlew = Join-Path $pgApp 'gradlew.bat'
if (-not (Test-Path $gradlew)) {
    Write-Error "gradlew.bat 없음: $gradlew"
    exit 1
}

Write-Host '[restart-pg-dev] 8080 LISTEN 프로세스 종료...'
try {
    $conns = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
    foreach ($c in $conns) {
        $procId = $c.OwningProcess
        Write-Host "  PID $procId 종료"
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
    }
    if (-not $conns) { Write-Host '  (8080 LISTEN 없음)' }
} catch {
    Write-Host '  Get-NetTCPConnection 실패, netstat 폴백...'
    foreach ($line in (netstat -ano)) {
        if ($line -match ':8080\s+.*LISTENING\s+(\d+)\s*$') {
            $p = [int]$Matches[1]
            Write-Host "  PID $p 종료"
            Stop-Process -Id $p -Force -ErrorAction SilentlyContinue
        }
    }
}

Start-Sleep -Seconds 2

$log = Join-Path $pgApp 'pg-app-dev-restart.log'
Write-Host "[restart-pg-dev] gradlew bootRun (dev) 백그라운드 기동 — 로그: $log"

$pinfo = New-Object System.Diagnostics.ProcessStartInfo
$pinfo.FileName = 'cmd.exe'
$pinfo.WorkingDirectory = $pgApp
# /c 뒤 한 덩어리: cd 후 gradlew, 로그 리다이렉트
$pinfo.Arguments = '/c cd /d "' + $pgApp + '" && gradlew.bat bootRun --args=--spring.profiles.active=dev >> "' + $log + '" 2>&1'
$pinfo.UseShellExecute = $false
$pinfo.CreateNoWindow = $true
[void][System.Diagnostics.Process]::Start($pinfo)
Write-Host '[restart-pg-dev] 기동 요청 완료 (잠시 후 로그 확인)'
