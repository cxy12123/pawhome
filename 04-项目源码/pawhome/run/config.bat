@echo off
REM ============================================================
REM  PawHome - ONE AND ONLY CONFIG FILE
REM  Edit the values below ONCE, then run 1-build.bat / 2-start.bat
REM  Keep all paths free of Chinese characters and spaces where possible.
REM  All paths must NOT end with a backslash.
REM ============================================================

REM 1) JDK 17 install folder (NOT the bin folder)
set "JDK17=C:\Program Files\Java\jdk-17"

REM 2) Folder where Redis was extracted (contains redis-server.exe)
set "REDIS_DIR=D:\redis"

REM 3) Folder where Nacos was extracted (contains bin\startup.cmd)
set "NACOS_DIR=D:\nacos"

REM 4) Folder containing sentinel-dashboard-1.8.7.jar
set "SENTINEL_DIR=D:\sentinel"

REM 5) Credentials. Same values are passed to every service.
set "MYSQL_USERNAME=root"
set "MYSQL_PASSWORD=root"
set "NACOS_USERNAME=nacos"
set "NACOS_PASSWORD=nacos"

REM ============================================================
REM  Do not edit below this line
REM ============================================================
set "JAVA_HOME=%JDK17%"
set "PATH=%JDK17%\bin;%PATH%"

REM Some terminals inject SERVER__PORT. Spring Boot reads it as server.port,
REM which would make Nacos and all 8 services bind to the wrong port. Clear it.
set "SERVER__PORT="

if not exist "%JDK17%\bin\java.exe" (
    echo.
    echo [ERROR] JDK 17 not found at:
    echo         %JDK17%
    echo Please install JDK 17, then edit JDK17 in config.bat
    echo.
    exit /b 1
)
if not exist "%REDIS_DIR%\redis-server.exe" (
    echo.
    echo [ERROR] Redis not found at %REDIS_DIR%
    echo Please extract Redis there, or edit REDIS_DIR in config.bat
    echo.
    exit /b 1
)
if not exist "%NACOS_DIR%\bin\startup.cmd" (
    echo.
    echo [ERROR] Nacos not found at %NACOS_DIR%
    echo Please extract Nacos there, or edit NACOS_DIR in config.bat
    echo.
    exit /b 1
)
exit /b 0
