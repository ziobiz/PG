#Requires -Version 5.1
<#
.SYNOPSIS
  PG 워크스페이스에서 NOTI 운영 배포를 호출합니다.
  실제 스크립트: d:\Delopment\Notificaiton\scripts\deploy\Deploy-Noti.ps1
  자격증명: %USERPROFILE%\.noti-deploy\credentials.env (Git 금지)
#>
param(
    [string[]]$Files = @('server.js'),
    [switch]$SkipRestart,
    [switch]$WhatIf
)
$ErrorActionPreference = "Stop"
# scripts/deploy → PG → Delopment → Notificaiton
$delopment = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$notiRoot = Join-Path $delopment "Notificaiton"
if (-not (Test-Path $notiRoot)) {
    $notiRoot = "d:\Delopment\Notificaiton"
}
$script = Join-Path $notiRoot "scripts\deploy\Deploy-Noti.ps1"
if (-not (Test-Path $script)) {
    throw "NOTI 배포 스크립트 없음: $script"
}
& $script -Files $Files -SkipRestart:$SkipRestart -WhatIf:$WhatIf
