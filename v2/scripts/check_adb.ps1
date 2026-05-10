# V26 매크로 V2 — ADB 환경 검증 스크립트
# 사용: PowerShell 에서 .\scripts\check_adb.ps1
#
# 동작:
#   1. PATH 의 adb 와 LDPlayer 내장 adb (C:\LDPlayer\LDPlayer9\adb.exe) 둘 다 시도
#   2. 포트 5555 / 5554 / 5556 / 5557 자동 탐색 (LDPlayer 인스턴스 1번이 5554 일 수 있음)
#   3. 'offline' 나오면 LdBoxHeadless 충돌 워크어라운드 안내
#   4. 성공 엔드포인트를 config.local.json 으로 저장 (다른 스크립트가 사용)

$ErrorActionPreference = "Stop"

function Write-Section($text) {
    Write-Host ""
    Write-Host "=== $text ===" -ForegroundColor Cyan
}

function Try-Adb {
    param([string]$AdbPath, [int]$Port)
    $endpoint = "127.0.0.1:$Port"
    Write-Host "  [$AdbPath] connect $endpoint ... " -NoNewline
    try {
        & $AdbPath connect $endpoint 2>&1 | Out-Null
        $devices = & $AdbPath devices 2>&1 | Out-String
        if ($devices -match "$endpoint\s+device\b") {
            Write-Host "OK" -ForegroundColor Green
            return @{ adb = $AdbPath; endpoint = $endpoint; offline = $false }
        } elseif ($devices -match "$endpoint\s+offline\b") {
            Write-Host "OFFLINE" -ForegroundColor Yellow
            return @{ adb = $AdbPath; endpoint = $endpoint; offline = $true }
        } else {
            Write-Host "no device" -ForegroundColor DarkGray
            return $null
        }
    } catch {
        Write-Host "fail ($_)" -ForegroundColor DarkGray
        return $null
    }
}

Write-Section "ADB 후보 탐색"
$adbCandidates = @()
$pathAdb = (Get-Command adb -ErrorAction SilentlyContinue)
if ($pathAdb) {
    $adbCandidates += $pathAdb.Source
    Write-Host "  PATH adb: $($pathAdb.Source)"
}
@(
    "C:\LDPlayer\LDPlayer9\adb.exe",
    "C:\Program Files\LDPlayer\LDPlayer9\adb.exe",
    "D:\LDPlayer\LDPlayer9\adb.exe"
) | ForEach-Object {
    if (Test-Path $_) {
        $adbCandidates += $_
        Write-Host "  LDPlayer adb: $_"
    }
}

if ($adbCandidates.Count -eq 0) {
    Write-Host "  ADB 못 찾음. LDPlayer 9 설치 후 'C:\LDPlayer\LDPlayer9\adb.exe' 확인." -ForegroundColor Red
    exit 1
}

Write-Section "포트 탐색"
$ports = @(5555, 5554, 5556, 5557)
$result = $null
foreach ($adb in $adbCandidates) {
    foreach ($p in $ports) {
        $r = Try-Adb -AdbPath $adb -Port $p
        if ($r -and -not $r.offline) {
            $result = $r
            break
        }
        if ($r -and $r.offline -and -not $result) {
            $result = $r  # offline 도 일단 저장 - 정상 못 찾으면 안내용
        }
    }
    if ($result -and -not $result.offline) { break }
}

if (-not $result) {
    Write-Host ""
    Write-Host "어느 ADB 도 LDPlayer 와 연결 안됨." -ForegroundColor Red
    Write-Host "확인:"
    Write-Host "  - LDPlayer 가 켜져있는가?"
    Write-Host "  - LDPlayer 설정 → 기타 → 'ADB 디버깅' 켰는가?"
    exit 1
}

if ($result.offline) {
    Write-Host ""
    Write-Host "ADB 가 'offline' 상태." -ForegroundColor Yellow
    Write-Host "워크어라운드:"
    Write-Host "  1. PowerShell (관리자) 에서:"
    Write-Host "       taskkill /F /IM Ld9BoxHeadless.exe"
    Write-Host "  2. LDPlayer 재시작"
    Write-Host "  3. 이 스크립트 다시 실행"
    exit 1
}

Write-Section "정상"
Write-Host "  adb     : $($result.adb)"
Write-Host "  endpoint: $($result.endpoint)"

# device 정보
$dev = & $result.adb -s $result.endpoint shell wm size 2>&1 | Out-String
Write-Host "  size    : $($dev.Trim())"
$density = & $result.adb -s $result.endpoint shell wm density 2>&1 | Out-String
Write-Host "  density : $($density.Trim())"

# config.local.json 저장
$config = @{
    adb = $result.adb
    endpoint = $result.endpoint
    detected_at = (Get-Date).ToString("o")
} | ConvertTo-Json
Set-Content -Path "config.local.json" -Value $config -Encoding UTF8
Write-Host ""
Write-Host "config.local.json 저장 완료. 다른 스크립트가 이 파일 사용." -ForegroundColor Green
