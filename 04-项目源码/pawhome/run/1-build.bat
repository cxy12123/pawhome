@echo off
setlocal
title PawHome Build
call "%~dp0config.bat"
if errorlevel 1 goto :fail

echo.
echo === Java used for this build ===
java -version
echo.

cd /d "%~dp0.."
echo === Building PawHome, project root: %CD% ===
echo First build downloads many dependencies, please wait 5-15 minutes.
echo.

call mvn clean package -DskipTests
if errorlevel 1 (
    echo.
    echo [FAIL] Build failed. Read the ERROR lines above.
    echo Common causes:
    echo   - JAVA_HOME points to JDK 8 : fix JDK17 in config.bat
    echo   - dependency download failed : check network and run this again
    goto :fail
)

echo.
echo [OK] Build succeeded. Checking jars:
for %%A in (auth animal adoption supply donation volunteer notify) do (
    if exist "%CD%\pawhome-%%A\target\pawhome-%%A.jar" (
        echo   OK  pawhome-%%A.jar
    ) else (
        echo   MISSING  pawhome-%%A.jar
    )
)
if exist "%CD%\pawhome-gateway\target\pawhome-gateway-1.0.0.jar" (
    echo   OK  pawhome-gateway-1.0.0.jar
) else (
    echo   MISSING  pawhome-gateway-1.0.0.jar
)
echo.
echo Next step: run 2-start.bat
echo.
pause
exit /b 0

:fail
echo.
pause
exit /b 1
