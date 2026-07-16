#Requires -Version 5.1
<#
.SYNOPSIS
  bootJar → JAR SCP → (선택) SQL → restart-pg-app.sh 동기화 → 재시작 → Started 대기

.EXAMPLE
  .\scripts\deploy\Deploy-Prod.ps1
  .\scripts\deploy\Deploy-Prod.ps1 -SqlFiles @('pg-app/src/main/resources/db/V231_ilk_card_auth_mode_and_agency.sql')

.NOTES
  실패 시 폴백: 에이전트 bootJar 까지 → 사용자가 FTP JAR·DB SQL·수동 ./restart-pg-app.sh
#>
param(
    [switch]$SkipBuild,
    [switch]$SkipUpload,
    [switch]$SkipRestart,
    [string[]]$SqlFiles = @(),
    [switch]$WhatIf
)

$ErrorActionPreference = "Stop"
$ScriptDir = $PSScriptRoot
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir "..\..")).Path
$PgAppDir = Join-Path $RepoRoot "pg-app"
$JarName = "pg-app-0.0.1-SNAPSHOT.jar"
$JarLocal = Join-Path $PgAppDir "build\libs\$JarName"
$CredPath = Join-Path $env:USERPROFILE ".pg-deploy\credentials.env"
$AskPassCmd = Join-Path $env:TEMP "pg-deploy-askpass.cmd"
$RemoteHelperLocal = Join-Path $ScriptDir "remote-restart-and-wait.sh"

function Read-Credentials([string]$path) {
    if (-not (Test-Path $path)) {
        throw "자격증명 없음: $path`n→ .\scripts\deploy\Setup-Credentials.ps1 실행 후 값을 채우세요."
    }
    $map = @{}
    Get-Content -LiteralPath $path -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { return }
        $i = $line.IndexOf("=")
        if ($i -lt 1) { return }
        $k = $line.Substring(0, $i).Trim()
        $v = $line.Substring($i + 1).Trim()
        if (($v.StartsWith('"') -and $v.EndsWith('"')) -or ($v.StartsWith("'") -and $v.EndsWith("'"))) {
            $v = $v.Substring(1, $v.Length - 2)
        }
        $map[$k] = $v
    }
    return $map
}

function Require-Key($map, [string]$key) {
    if (-not $map.ContainsKey($key) -or [string]::IsNullOrWhiteSpace([string]$map[$key])) {
        throw "credentials.env 에 $key 가 필요합니다: $CredPath"
    }
    return [string]$map[$key]
}

function Write-AskPass([string]$password) {
    $safe = $password.Replace("%", "%%").Replace("^", "^^").Replace("&", "^&").Replace("|", "^|").Replace("<", "^<").Replace(">", "^>")
    @"
@echo off
echo $safe
"@ | Set-Content -LiteralPath $AskPassCmd -Encoding ASCII -Force
}

function Clear-AskPass {
    Remove-Item -LiteralPath $AskPassCmd -Force -ErrorAction SilentlyContinue
    Remove-Item Env:SSH_ASKPASS -ErrorAction SilentlyContinue
    Remove-Item Env:SSH_ASKPASS_REQUIRE -ErrorAction SilentlyContinue
    Remove-Item Env:DISPLAY -ErrorAction SilentlyContinue
}

function Build-SshBaseArgs($c) {
    $port = if ($c.ContainsKey("SSH_PORT") -and $c["SSH_PORT"]) { [int]$c["SSH_PORT"] } else { 22 }
    $args = @(
        "-o", "StrictHostKeyChecking=accept-new",
        "-o", "ConnectTimeout=30",
        "-o", "ServerAliveInterval=30",
        "-o", "ServerAliveCountMax=10"
    )
    $key = if ($c.ContainsKey("SSH_KEY_PATH")) { [string]$c["SSH_KEY_PATH"] } else { "" }
    if ($key -and (Test-Path $key)) {
        $args += @("-i", $key, "-o", "IdentitiesOnly=yes", "-o", "BatchMode=yes")
    } else {
        $pass = if ($c.ContainsKey("SSH_PASSWORD")) { [string]$c["SSH_PASSWORD"] } else { "" }
        if ([string]::IsNullOrWhiteSpace($pass)) {
            throw "SSH_KEY_PATH 또는 SSH_PASSWORD 가 필요합니다."
        }
        Write-AskPass $pass
        $env:SSH_ASKPASS = $AskPassCmd
        $env:SSH_ASKPASS_REQUIRE = "force"
        $env:DISPLAY = "localhost:0"
        $args += @("-o", "PreferredAuthentications=password", "-o", "PubkeyAuthentication=no", "-o", "NumberOfPasswordPrompts=1")
    }
    return @{ Port = $port; Args = $args }
}

function Invoke-Remote($c, [string]$remoteCmd) {
    $hostName = Require-Key $c "SSH_HOST"
    $user = Require-Key $c "SSH_USER"
    $base = Build-SshBaseArgs $c
    $target = "${user}@${hostName}"
    # Windows here-string CRLF → Linux bash 가 $'...\r' 로 깨짐
    $remoteCmd = ($remoteCmd -replace "`r`n", "`n") -replace "`r", "`n"
    $all = @("-p", "$($base.Port)") + $base.Args + @($target, $remoteCmd)
    Write-Host "SSH> $remoteCmd"
    if ($WhatIf) { return 0 }
    # ssh stdout 이 함수 반환값에 섞이면 exit code 판정이 깨짐 → 호스트로만 출력
    & ssh @all 2>&1 | ForEach-Object { Write-Host $_ }
    $ec = 0
    if ($null -ne $LASTEXITCODE) { $ec = [int]$LASTEXITCODE }
    return $ec
}

function Invoke-RemoteOrThrow($c, [string]$remoteCmd) {
    $ec = Invoke-Remote $c $remoteCmd
    if ($ec -ne 0) { throw "SSH 실패 (exit $ec): $remoteCmd" }
}

function Copy-ToRemote($c, [string]$localPath, [string]$remotePath) {
    $hostName = Require-Key $c "SSH_HOST"
    $user = Require-Key $c "SSH_USER"
    $base = Build-SshBaseArgs $c
    $dest = "${user}@${hostName}:${remotePath}"
    $all = @("-P", "$($base.Port)") + $base.Args + @($localPath, $dest)
    Write-Host "SCP> $localPath → $dest"
    if ($WhatIf) { return }
    & scp @all
    if ($LASTEXITCODE -ne 0) { throw "SCP 실패 (exit $LASTEXITCODE)" }
}

function Write-Utf8Lf([string]$path, [string]$content) {
    $lf = ($content -replace "`r`n", "`n") -replace "`r", "`n"
    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
    [IO.File]::WriteAllText($path, $lf, $utf8NoBom)
}

function Apply-SqlFiles($c, [string[]]$files) {
    if (-not $files -or $files.Count -eq 0) { return }
    $dbHost = if ($c["DB_HOST"]) { $c["DB_HOST"] } else { "localhost" }
    $dbPort = if ($c["DB_PORT"]) { $c["DB_PORT"] } else { "5432" }
    $dbName = if ($c["DB_NAME"]) { $c["DB_NAME"] } else { "pgdev" }
    $dbUser = if ($c["DB_USER"]) { $c["DB_USER"] } else { "pgadmin" }
    $dbPass = Require-Key $c "DB_PASSWORD"
    $remoteTmp = "/tmp/pg-deploy-sql"
    Invoke-RemoteOrThrow $c "mkdir -p '$remoteTmp'"

    foreach ($rel in $files) {
        $local = Join-Path $RepoRoot ($rel -replace "/", "\")
        if (-not (Test-Path $local)) { throw "SQL 없음: $local" }
        $baseName = [IO.Path]::GetFileName($local)
        $remoteFile = "$remoteTmp/$baseName"
        Copy-ToRemote $c $local $remoteFile
        $qPass = $dbPass -replace "'", "'\''"
        $one = "export PGPASSWORD='$qPass'; psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -v ON_ERROR_STOP=1 -f '$remoteFile'; ec=`$?; unset PGPASSWORD; exit `$ec"
        Write-Host "SQL> $baseName"
        Invoke-RemoteOrThrow $c $one
    }
    try { Invoke-Remote $c "rm -rf '$remoteTmp'" | Out-Null } catch { }
}

# 저장소 server-scripts/restart-pg-app.sh → 원격. DB_* 는 서버 기존 값을 유지.
function Sync-RestartScript($c, [string]$remoteApp) {
    $local = Join-Path $RepoRoot "server-scripts\restart-pg-app.sh"
    if (-not (Test-Path $local)) {
        Write-Warning "restart-pg-app.sh 로컬 없음 — 동기화 생략: $local"
        return
    }
    $remoteTmp = "/tmp/restart-pg-app.sh.new"
    $pyRemote = "/tmp/pg-sync-restart.py"
    Copy-ToRemote $c $local $remoteTmp
    $py = @"
import re, pathlib
new_p = pathlib.Path("/tmp/restart-pg-app.sh.new")
old_p = pathlib.Path("$($remoteApp.Replace('\','/'))/restart-pg-app.sh")
text = new_p.read_text(encoding="utf-8")
if old_p.exists():
    old = old_p.read_text(encoding="utf-8")
    for key in ("DB_HOST", "DB_USER", "DB_PASSWORD"):
        m = re.search(rf'^{key}="([^"]*)"', old, re.M)
        if m:
            text = re.sub(rf'^{key}="[^"]*"', f'{key}="{m.group(1)}"', text, count=1, flags=re.M)
old_p.write_text(text, encoding="utf-8")
old_p.chmod(0o755)
print("restart-pg-app.sh synced")
"@
    $pyLocal = Join-Path $env:TEMP "pg-sync-restart.py"
    Write-Utf8Lf $pyLocal $py
    try {
        Copy-ToRemote $c $pyLocal $pyRemote
        Invoke-RemoteOrThrow $c "python3 '$pyRemote'"
    } finally {
        Remove-Item -LiteralPath $pyLocal -Force -ErrorAction SilentlyContinue
    }
}

function Show-ManualFallback([string]$jarPath, [string[]]$sqlRelPaths) {
    Write-Host ""
    Write-Host "=== 자동 배포 실패 시 수동 폴백 ===" -ForegroundColor Yellow
    Write-Host "1) JAR: $jarPath"
    Write-Host "   → 서버 /home/ftpuser/pg-app/build/libs/ 에 FTP/SCP"
    if ($sqlRelPaths -and $sqlRelPaths.Count -gt 0) {
        Write-Host "2) DB SQL:"
        foreach ($s in $sqlRelPaths) { Write-Host "   - $s" }
    } else {
        Write-Host "2) DB: 추가 SQL 없음"
    }
    Write-Host "3) SSH 후:"
    Write-Host "   cd /home/ftpuser/pg-app"
    Write-Host "   export SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20"
    Write-Host "   export APP_SETTLEMENT_AUTO_RUN=true"
    Write-Host "   ./restart-pg-app.sh"
    Write-Host "   grep 'Started PgAppApplication' pg-app.log | tail -1"
    Write-Host "확인: https://icopay.co.kr/"
}

$deployFailed = $false
try {
    Write-Host "=== PG 운영 배포 ==="
    Write-Host "Repo: $RepoRoot"
    $c = Read-Credentials $CredPath
    $remoteApp = Require-Key $c "REMOTE_PG_APP_DIR"
    $remoteJar = "$remoteApp/build/libs/$JarName"

    if (-not $SkipBuild) {
        Write-Host "`n[1/4] bootJar..."
        if (-not $WhatIf) {
            Push-Location $PgAppDir
            try {
                & .\gradlew.bat bootJar --no-daemon
                if ($LASTEXITCODE -ne 0) { throw "bootJar 실패" }
            } finally {
                Pop-Location
            }
        }
    } else {
        Write-Host "`n[1/4] bootJar 생략"
    }

    if (-not (Test-Path $JarLocal) -and -not $WhatIf -and -not $SkipUpload) {
        throw "JAR 없음: $JarLocal"
    }
    if (Test-Path $JarLocal) {
        Write-Host "JAR: $JarLocal ($((Get-Item $JarLocal).Length) bytes)"
    }

    if (-not $SkipUpload) {
        Write-Host "`n[2/4] JAR 업로드..."
        Invoke-RemoteOrThrow $c "mkdir -p '$remoteApp/build/libs'"
        Copy-ToRemote $c $JarLocal $remoteJar
        Invoke-RemoteOrThrow $c "test -s '$remoteJar' && ls -la '$remoteJar'"
    } else {
        Write-Host "`n[2/4] 업로드 생략"
    }

    if ($SqlFiles -and $SqlFiles.Count -gt 0) {
        Write-Host "`n[3/4] DB SQL ($($SqlFiles.Count) files)..."
        Apply-SqlFiles $c $SqlFiles
    } else {
        Write-Host "`n[3/4] DB 변경 없음 (SqlFiles 미지정)"
    }

    if (-not $SkipRestart) {
        Write-Host "`n[4/4] 재시작 스크립트 동기화 + 재시작..."
        Sync-RestartScript $c $remoteApp

        if (-not (Test-Path $RemoteHelperLocal)) {
            throw "원격 헬퍼 없음: $RemoteHelperLocal"
        }
        # LF 보장 후 업로드 (Windows 체크아웃 CRLF 대비)
        $helperTmp = Join-Path $env:TEMP "pg-remote-restart-and-wait.sh"
        $helperText = [IO.File]::ReadAllText($RemoteHelperLocal)
        Write-Utf8Lf $helperTmp $helperText
        $remoteHelper = "/tmp/pg-remote-restart-and-wait.sh"
        try {
            Copy-ToRemote $c $helperTmp $remoteHelper
        } finally {
            Remove-Item -LiteralPath $helperTmp -Force -ErrorAction SilentlyContinue
        }

        $grep = if ($c["VERIFY_LOG_GREP"]) { $c["VERIFY_LOG_GREP"] } else { "Started PgAppApplication" }
        # 인자 이스케이프 (공백·따옴표 최소화)
        $safeGrep = $grep -replace "'", "'\''"
        $ec = Invoke-Remote $c "chmod +x '$remoteHelper' && bash '$remoteHelper' '$remoteApp' '$safeGrep'"
        if ($ec -ne 0) {
            throw "재시작/기동 확인 실패 (exit $ec). 서버 pg-app.log 를 확인하세요."
        }
    } else {
        Write-Host "`n[4/4] 재시작 생략"
    }

    Write-Host "`n=== 배포 완료 ==="
    Write-Host "확인: https://icopay.co.kr/"
}
catch {
    $deployFailed = $true
    Write-Host "`n배포 오류: $($_.Exception.Message)" -ForegroundColor Red
    Show-ManualFallback $JarLocal $SqlFiles
    throw
}
finally {
    Clear-AskPass
    if (-not $deployFailed) {
        # no-op
    }
}
