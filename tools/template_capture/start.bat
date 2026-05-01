@echo off
title V26 Template Capture
cd /d "%~dp0"

REM ---------- Find Python ----------
set "PY="
where py >nul 2>nul
if not errorlevel 1 set "PY=py -3"

if "%PY%"=="" (
    where python >nul 2>nul
    if not errorlevel 1 set "PY=python"
)

if "%PY%"=="" goto NO_PYTHON

REM ---------- Verify Python >= 3.9 ----------
%PY% -c "import sys; sys.exit(0 if sys.hexversion >= 0x03090000 else 1)"
if errorlevel 1 goto OLD_PYTHON

echo [OK] Python: %PY%

REM ---------- Install Pillow if missing ----------
%PY% -c "import PIL" >nul 2>nul
if errorlevel 1 (
    echo [..] Installing Pillow ...
    %PY% -m pip install --upgrade pip >nul 2>nul
    %PY% -m pip install Pillow
    if errorlevel 1 goto PIP_FAIL
)

echo [OK] Make sure LDPlayer is running, then the GUI will open.
echo.

REM ---------- Run the GUI ----------
%PY% template_capture.py
if errorlevel 1 (
    echo.
    echo [!] Exit code %ERRORLEVEL%
    pause
)
exit /b 0


:NO_PYTHON
echo.
echo [!] Python 3.9+ is required but not found.
echo.
echo     1. Download: https://www.python.org/downloads/
echo     2. IMPORTANT: check "Add python.exe to PATH" during install.
echo     3. Re-run this start.bat after install.
echo.
set /p OPEN="Open download page now? (y/N): "
if /I "%OPEN%"=="y" start "" "https://www.python.org/downloads/"
pause
exit /b 1


:OLD_PYTHON
echo.
echo [!] Found Python but version is too old. Need 3.9 or newer.
echo     %PY% --version
%PY% --version
echo.
pause
exit /b 1


:PIP_FAIL
echo.
echo [!] Pillow install failed. Check your internet connection.
echo     Manual install: %PY% -m pip install Pillow
echo.
pause
exit /b 1
