@echo off
setlocal
chcp 65001 >nul
title V26 템플릿 캡처 도구

cd /d "%~dp0"

REM 1) Python 찾기 (py 런처 우선, 다음 python.exe)
set "PY="
where py >nul 2>&1 && (py -3 -c "import sys; sys.exit(0 if sys.version_info >= (3,9) else 1)" >nul 2>&1 && set "PY=py -3")
if "%PY%"=="" where python >nul 2>&1 && (python -c "import sys; sys.exit(0 if sys.version_info >= (3,9) else 1)" >nul 2>&1 && set "PY=python")

if "%PY%"=="" (
    echo.
    echo [!] Python 3.9 이상이 필요합니다.
    echo.
    echo     1. https://www.python.org/downloads/ 에서 최신 Python 3 설치
    echo        ※ 설치 시 'Add python.exe to PATH' 체크 필수!
    echo     2. 설치 후 이 배치 파일을 다시 실행
    echo.
    set /p OPEN="브라우저로 다운로드 페이지를 열까요? (y/N): "
    if /I "%OPEN%"=="y" start "" "https://www.python.org/downloads/"
    pause
    exit /b 1
)

echo [+] Python: %PY%

REM 2) Pillow 설치 확인 / 설치
%PY% -c "import PIL" >nul 2>&1
if errorlevel 1 (
    echo [+] Pillow 설치 중...
    %PY% -m pip install --upgrade pip >nul
    %PY% -m pip install Pillow
    if errorlevel 1 (
        echo [!] Pillow 설치 실패. pip 설정을 확인하세요.
        pause
        exit /b 1
    )
)

REM 3) ADB 위치 안내 (LDPlayer 폴더의 adb.exe를 자동 사용)
echo [+] LDPlayer가 켜져 있는지 확인하세요.
echo.

REM 4) 도구 실행
%PY% template_capture.py
if errorlevel 1 (
    echo.
    echo [!] 종료 코드 %ERRORLEVEL%
    pause
)
endlocal
