# V26 매크로 V2 — 즉시 스크린샷 (디버깅 용)
# 사용: .\scripts\screencap.ps1 [-Out screen.png]

param([string]$Out = "screen.png")

$ErrorActionPreference = "Stop"

if (-not (Test-Path "config.local.json")) {
    Write-Host "먼저 .\scripts\check_adb.ps1" -ForegroundColor Red
    exit 1
}
$config = Get-Content "config.local.json" -Raw | ConvertFrom-Json
$adb = $config.adb
$endpoint = $config.endpoint

& $adb -s $endpoint exec-out screencap -p > $Out
Write-Host "saved: $Out" -ForegroundColor Green
