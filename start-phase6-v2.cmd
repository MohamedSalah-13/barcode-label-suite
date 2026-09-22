@echo off
setlocal
cd /d "%~dp0"

echo [1/6] Installing base and render libraries...
call mvnw.cmd -f pom.xml install
if errorlevel 1 goto :failed
call mvnw.cmd -f phase2-pom.xml install
if errorlevel 1 goto :failed

echo [2/6] Installing the shared Java2D print pipeline...
call mvnw.cmd -N -f phase3-pom.xml install
if errorlevel 1 goto :failed
call mvnw.cmd -N -f phase3-build-pom.xml install
if errorlevel 1 goto :failed
call mvnw.cmd -f phase3-final-pom.xml install
if errorlevel 1 goto :failed

echo [3/6] Installing phase 5 workflow...
call mvnw.cmd -f phase5-pom.xml -pl barcode-label-workflow -am install
if errorlevel 1 goto :failed

echo [4/6] Installing corrected phase 5 Workbench...
call mvnw.cmd -f phase5-fixed-pom.xml install
if errorlevel 1 goto :failed

echo [5/6] Building and testing Batch Production 0.6.1...
call mvnw.cmd -f phase6-final-pom.xml install
if errorlevel 1 goto :failed

echo [6/6] Starting Batch Production demo...
call mvnw.cmd -f phase6-build\demo-final\pom.xml org.openjfx:javafx-maven-plugin:0.0.8:run
if errorlevel 1 goto :failed
exit /b 0

:failed
echo.
echo Build or startup failed. Review the Maven error above.
pause
exit /b 1
