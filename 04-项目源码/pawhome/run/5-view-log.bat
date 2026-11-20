@echo off
setlocal
title PawHome Logs
set "LOGDIR=%~dp0logs"

if not exist "%LOGDIR%" (
    echo.
    echo No logs yet. Run 2-start.bat first.
    echo.
    pause
    exit /b 1
)

:menu
cls
echo ============================================================
echo   PawHome logs   %LOGDIR%
echo ============================================================
echo.
dir /b "%LOGDIR%\*.log" 2>nul
echo.
echo ------------------------------------------------------------
echo   Ports:  auth 8081   animal 8082   adoption 8083   supply 8084
echo           donation 8085  volunteer 8086  notify 8087  gateway 8080
echo ------------------------------------------------------------
echo   Type a service name and press Enter:
echo     auth / animal / supply / notify / adoption /
echo     donation / volunteer / gateway / redis / sentinel
echo   Just press Enter to quit.
echo ------------------------------------------------------------
set "SVC="
set /p SVC=Service: 
if "%SVC%"=="" exit /b 0

set "FILE=%LOGDIR%\pawhome-%SVC%.log"
if not exist "%FILE%" set "FILE=%LOGDIR%\%SVC%.log"
if not exist "%FILE%" (
    echo.
    echo [ERROR] Not found: pawhome-%SVC%.log
    echo.
    pause
    goto menu
)

echo.
echo =================== last 60 lines ===================
powershell -NoProfile -Command "Get-Content -Path '%FILE%' -Tail 60"
echo ====================================================
echo.
echo Press any key to go back to the list.
pause >nul
goto menu
