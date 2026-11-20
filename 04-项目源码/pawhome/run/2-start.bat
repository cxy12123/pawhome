@echo off
setlocal
title PawHome Starter
call "%~dp0config.bat"
if errorlevel 1 goto :fail

REM Startup logic lives in 2-start.ps1.
REM Batch start /B with redirected output is unreliable for launching
REM several JVMs at once, so PowerShell does the real work.
pushd "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File ".\2-start.ps1"
set "RC=%errorlevel%"
popd

if not "%RC%"=="0" goto :fail
exit /b 0

:fail
echo.
pause
exit /b 1
