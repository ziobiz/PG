#Requires -Version 5.1
param([switch]$WhatIf)
$ErrorActionPreference = "Stop"
$ScriptDir = $PSScriptRoot
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir "..\..")).Path
$CredPath = Join-Path $env:USERPROFILE ".pg-deploy\credentials.env"
$AskPassCmd = Join-Path $env:TEMP "pg-deploy-askpass.cmd"

function Read-Credentials([string]$path) {
    if (-not (Test-Path $path)) { throw "missing credentials.env" }
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
        throw ("credentials.env missing " + $key)
    }
    return [string]$map[$key]
}
function Write-AskPass([string]$password) {
    $safe = $password.Replace('%', '%%').Replace('^', '^^').Replace('&', '^&').Replace('|', '^|').Replace('<', '^<').Replace('>', '^>')
    $cmd = '@echo off' + [Environment]::NewLine + 'echo ' + $safe
    Set-Content -LiteralPath $AskPassCmd -Value $cmd -Encoding ASCII -Force
}
function Build-SshBaseArgs($c) {
    $port = if ($c.ContainsKey('SSH_PORT') -and $c['SSH_PORT']) { [int]$c['SSH_PORT'] } else { 22 }
    $args = @('-o', 'StrictHostKeyChecking=accept-new', '-o', 'ConnectTimeout=30')
    $key = if ($c.ContainsKey('SSH_KEY_PATH')) { [string]$c['SSH_KEY_PATH'] } else { '' }
    if ($key -and (Test-Path $key)) {
        $args += @('-i', $key, '-o', 'IdentitiesOnly=yes', '-o', 'BatchMode=yes')
    } else {
        $pass = if ($c.ContainsKey('SSH_PASSWORD')) { [string]$c['SSH_PASSWORD'] } else { '' }
        if ([string]::IsNullOrWhiteSpace($pass)) { throw 'need SSH_KEY_PATH or SSH_PASSWORD' }
        Write-AskPass $pass
        $env:SSH_ASKPASS = $AskPassCmd
        $env:SSH_ASKPASS_REQUIRE = 'force'
        $env:DISPLAY = 'localhost:0'
        $args += @('-o', 'PreferredAuthentications=password', '-o', 'PubkeyAuthentication=no', '-o', 'NumberOfPasswordPrompts=1')
    }
    return @{ Port = $port; Args = $args }
}
function Invoke-Remote($c, [string]$remoteCmd) {
    $hostName = Require-Key $c 'SSH_HOST'
    $user = Require-Key $c 'SSH_USER'
    $base = Build-SshBaseArgs $c
    $target = "${user}@${hostName}"
    $remoteCmd = ($remoteCmd -replace "`r`n", "`n") -replace "`r", "`n"
    $all = @('-p', "$($base.Port)") + $base.Args + @($target, $remoteCmd)
    Write-Host ('SSH> ' + $remoteCmd)
    if ($WhatIf) { return 0 }
    & ssh @all 2>&1 | ForEach-Object { Write-Host $_ }
    $ec = 0
    if ($null -ne $LASTEXITCODE) { $ec = [int]$LASTEXITCODE }
    return $ec
}
function Copy-ToRemote($c, [string]$localPath, [string]$remotePath) {
    $hostName = Require-Key $c 'SSH_HOST'
    $user = Require-Key $c 'SSH_USER'
    $base = Build-SshBaseArgs $c
    $dest = "${user}@${hostName}:${remotePath}"
    $all = @('-P', "$($base.Port)") + $base.Args + @($localPath, $dest)
    Write-Host ('SCP> ' + $localPath)
    if ($WhatIf) { return }
    & scp @all
    if ($LASTEXITCODE -ne 0) { throw 'SCP failed' }
}

$c = Read-Credentials $CredPath
$pyLocal = Join-Path $RepoRoot 'scripts\deploy\patch_icopay_og_nginx.py'
Copy-ToRemote $c $pyLocal '/tmp/patch_icopay_og_nginx.py'
$remote = 'python3 /tmp/patch_icopay_og_nginx.py; ec=$?; if [ $ec -eq 0 ]; then nginx -t && systemctl reload nginx; ec=$?; fi; rm -f /tmp/patch_icopay_og_nginx.py; exit $ec'
$ec = Invoke-Remote $c $remote
if ($ec -ne 0) { throw ("nginx OG patch failed exit " + $ec) }
Write-Host 'nginx OG proxy applied'
Remove-Item -LiteralPath $AskPassCmd -Force -ErrorAction SilentlyContinue
