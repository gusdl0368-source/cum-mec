# V26 매크로 V2 — 빌드된 APK 설치
# 사용: .\scripts\install_apk.ps1 [-Build]
#   -Build : adb 설치 전에 ./gradlew assembleDebug 먼저 실행

param([switch]$Build)

$ErrorActionPreference = "Stop"

if (-not (Test-Path "config.local.json")) {
    Write-Host "config.local.json 없음. 먼저 .\scripts\check_adb.ps1 실행." -ForegroundColor Red
    exit 1
}

$config = Get-Content "config.local.json" -Raw | ConvertFrom-Json
$adb = $config.adb
$endpoint = $config.endpoint

if ($Build) {
    Write-Host "=== Gradle assembleDebug ===" -ForegroundColor Cyan
    & .\gradlew.bat assembleDebug
    if ($LASTEXITCODE -ne 0) {
        Write-Host "빌드 실패." -ForegroundColor Red
        exit 1
    }
}

$apk = "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) {
    Write-Host "APK 없음: $apk — 먼저 .\scripts\install_apk.ps1 -Build 또는 .\gradlew assembleDebug" -ForegroundColor Red
    exit 1
}

Write-Host "=== adb install -r ===" -ForegroundColor Cyan
& $adb -s $endpoint install -r $apk
if ($LASTEXITCODE -ne 0) {
    Write-Host "설치 실패." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== 앱 실행 ===" -ForegroundColor Cyan
& $adb -s $endpoint shell am start -n "com.v26macro.v2/.MainActivity"

Write-Host ""
Write-Host "설치 + 실행 완료." -ForegroundColor Green
Write-Host "로그 보려면: $adb -s $endpoint logcat -s V26Macro:V"
