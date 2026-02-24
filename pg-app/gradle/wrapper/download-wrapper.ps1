# Gradle Wrapper JAR 다운로드 (한 번만 실행)
$wrapperDir = $PSScriptRoot
$jarPath = Join-Path $wrapperDir "gradle-wrapper.jar"
$url = "https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradle/wrapper/gradle-wrapper.jar"
Write-Host "Downloading gradle-wrapper.jar..."
try {
    Invoke-WebRequest -Uri $url -OutFile $jarPath -UseBasicParsing
    Write-Host "Done. Saved to: $jarPath"
} catch {
    Write-Host "Error: $_"
    Write-Host "Try: Install Gradle from https://gradle.org/install/ then run: gradle wrapper"
}
