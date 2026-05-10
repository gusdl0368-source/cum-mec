# V26 매크로 V2 — flows/ YAML 을 LDPlayer /sdcard 로 푸시
# 사용: .\scripts\push_flows.ps1 [-File <name>]
#   -File 생략 시 flows/ 안의 모든 .yaml 푸시

param([string]$File)

$ErrorActionPreference = "Stop"

if (-not (Test-Path "config.local.json")) {
    Write-Host "config.local.json 없음. 먼저 .\scripts\check_adb.ps1 실행." -ForegroundColor Red
    exit 1
}

$config = Get-Content "config.local.json" -Raw | ConvertFrom-Json
$adb = $config.adb
$endpoint = $config.endpoint

# 앱 전용 외부 저장. APK 권한 추가 안 해도 되는 경로.
$target = "/sdcard/Android/data/com.v26macro.v2/files/v26-macro/flows/"

# 디렉터리 보장 (적어도 한 번은 앱 실행 후라야 존재)
& $adb -s $endpoint shell mkdir -p $target 2>&1 | Out-Null

if ($File) {
    $local = "flows\$File"
    if (-not (Test-Path $local)) {
        Write-Host "파일 없음: $local" -ForegroundColor Red
        exit 1
    }
    Write-Host "  push $local → $target" -ForegroundColor Cyan
    & $adb -s $endpoint push $local $target
} else {
    $files = Get-ChildItem -Path "flows" -Filter "*.yaml" -ErrorAction SilentlyContinue
    if (-not $files) {
        Write-Host "flows/ 안에 .yaml 없음." -ForegroundColor Yellow
        exit 0
    }
    foreach ($f in $files) {
        Write-Host "  push $($f.Name) → $target" -ForegroundColor Cyan
        & $adb -s $endpoint push $f.FullName $target
    }
}

Write-Host ""
Write-Host "푸시 완료. 앱에서 다음 RUN 부터 새 YAML 사용." -ForegroundColor Green
