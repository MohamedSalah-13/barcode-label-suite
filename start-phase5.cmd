@echo off
setlocal
cd /d "%~dp0"

echo [1/5] Installing base libraries...
call mvn -f pom.xml install -DskipTests
if errorlevel 1 goto :failed

echo [2/5] Installing barcode render libraries...
call mvn -f phase2-pom.xml install -DskipTests
if errorlevel 1 goto :failed

echo [3/5] Installing visual designer parent and library...
call mvn -N -f phase4-pom.xml install
if errorlevel 1 goto :failed
call mvn -f barcode-label-fx-designer\pom.xml install -DskipTests
if errorlevel 1 goto :failed

echo [4/5] Building Barcode Label Workbench 0.5...
call mvn -f phase5-pom.xml install
if errorlevel 1 goto :failed

echo [5/5] Starting demo...
call mvn -f phase5-build\demo\pom.xml org.openjfx:javafx-maven-plugin:0.0.8:run
if errorlevel 1 goto :failed
exit /b 0

:failed
echo.
echo Build or startup failed. Review the Maven error above.
pause
exit /b 1
