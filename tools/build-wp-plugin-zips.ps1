# Build WordPress plugin ZIPs (icopay-core bundled).
# Usage: .\tools\build-wp-plugin-zips.ps1
# Outputs:
#   woocommerce/icopay-woocommerce-1.1.0.zip
#   wordpress/icopay-jpay-1.0.0.zip

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$CoreSrc  = Join-Path $RepoRoot 'wordpress\icopay-core'

function Copy-IcopayCore {
    param(
        [string]$DestIncludesDir
    )
    $destCore = Join-Path $DestIncludesDir 'icopay-core'
    if (Test-Path $destCore) {
        Remove-Item -Recurse -Force $destCore
    }
    Copy-Item -Path $CoreSrc -Destination $destCore -Recurse -Force
}

function Build-PluginZip {
    param(
        [string]$PluginDir,
        [string]$ZipPath,
        [string]$PluginFolderName
    )

    $staging = Join-Path $env:TEMP ("icopay-wp-build-" + [guid]::NewGuid().ToString())
    $pluginStaging = Join-Path $staging $PluginFolderName
    New-Item -ItemType Directory -Path $pluginStaging -Force | Out-Null

    Copy-Item -Path (Join-Path $PluginDir '*') -Destination $pluginStaging -Recurse -Force

    $includesDir = Join-Path $pluginStaging 'includes'
    if (-not (Test-Path $includesDir)) {
        New-Item -ItemType Directory -Path $includesDir -Force | Out-Null
    }
    Copy-IcopayCore -DestIncludesDir $includesDir

    if (Test-Path $ZipPath) {
        Remove-Item -Force $ZipPath
    }

    Compress-Archive -Path $pluginStaging -DestinationPath $ZipPath -Force
    Remove-Item -Recurse -Force $staging
    Write-Host "Created $ZipPath"
}

if (-not (Test-Path $CoreSrc)) {
    throw "icopay-core not found at $CoreSrc"
}

$wcDir = Join-Path $RepoRoot 'woocommerce\icopay-woocommerce'
$jpayDir = Join-Path $RepoRoot 'wordpress\icopay-jpay'

Build-PluginZip `
    -PluginDir $wcDir `
    -ZipPath (Join-Path $RepoRoot 'woocommerce\icopay-woocommerce-1.1.0.zip') `
    -PluginFolderName 'icopay-woocommerce'

Build-PluginZip `
    -PluginDir $jpayDir `
    -ZipPath (Join-Path $RepoRoot 'wordpress\icopay-jpay-1.0.0.zip') `
    -PluginFolderName 'icopay-jpay'

Write-Host 'Done.'
