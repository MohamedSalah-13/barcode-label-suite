@echo off
setlocal
cd /d "%~dp0"

echo [1/5] Installing base and render libraries...
call mvn -f pom.xml install -DskipTests
if errorlevel 1 goto :failed
call mvn -f phase2-pom.xml install -DskipTests
if errorlevel 1 goto :failed

echo [2/5] Installing phase 5 workflow...
call mvn -f phase5-pom.xml -pl barcode-label-workflow -am install
if errorlevel 1 goto :failed

echo [3/5] Installing corrected phase 5 Workbench...
call mvn -f phase5-fixed-pom.xml install -DskipTests
if errorlevel 1 goto :failed

echo [4/5] Building and testing batch production 0.6...
call mvn -f phase6-pom.xml install
if errorlevel 1 goto :failed

echo [5/5] Starting Batch Production demo...
call mvn -f phase6-build\demo\pom.xml org.openjfx:javafx-maven-plugin:0.0.8:run
if errorlevel 1 goto :failed
exit /b 0

:failed
echo.
echo Build or startup failed. Review the Maven error above.
pause
exit /b 1
