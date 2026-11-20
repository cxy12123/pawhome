@echo off
setlocal
title PawHome Stopper

echo Stopping PawHome by port ...
echo.

REM service ports, Eureka 8761, Config 8888, Nacos 8848/9848/9849/7848,
REM Sentinel 8858, Redis 6379
for %%P in (8080 8081 8082 8083 8084 8085 8086 8087 8761 8888 8858 8848 9848 9849 7848 6379) do (
    for /f "tokens=5" %%A in ('netstat -ano ^| findstr ":%%P " ^| findstr LISTENING') do (
        echo   port %%P  -^>  PID %%A
        taskkill /PID %%A /T /F >nul 2>&1
    )
)

echo.
echo Closing leftover service windows (if any) ...
for %%T in (gateway volunteer donation adoption notify supply animal auth eureka config sentinel nacos redis) do (
    taskkill /FI "WINDOWTITLE eq pawhome-%%T*" /F >nul 2>&1
)

echo.
echo Done. Verify with:  netstat -ano ^| findstr ":8080 :8761 :8888 :8848 :6379"
echo.
pause
exit /b 0
