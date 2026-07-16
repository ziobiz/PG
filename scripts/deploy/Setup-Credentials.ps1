#Requires -Version 5.1
$ErrorActionPreference = "Stop"
$dir = Join-Path $env:USERPROFILE ".pg-deploy"
$dest = Join-Path $dir "credentials.env"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$example = Join-Path $repoRoot "scripts\deploy\credentials.env.example"

New-Item -ItemType Directory -Force -Path $dir | Out-Null
if (Test-Path $dest) {
    Write-Host "이미 있습니다: $dest"
} else {
    Copy-Item $example $dest
    Write-Host "생성됨: $dest"
}
Write-Host "SSH_PASSWORD / DB_PASSWORD 를 채워 저장하세요."
try { Start-Process notepad.exe -ArgumentList $dest } catch { Write-Host "수동 편집: $dest" }
