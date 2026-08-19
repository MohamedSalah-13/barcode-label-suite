@echo off
setlocal
cd /d "%~dp0"

echo [1/4] Installing base libraries...
call mvn -f pom.xml install -DskipTests
if errorlevel 1 goto :failed

echo [2/4] Installing barcode render libraries...
call mvn -f phase2-pom.xml install -DskipTests
if errorlevel 1 goto :failed

echo [3/4] Building workflow and corrected Workbench...
call mvn -f phase5-pom.xml -pl barcode-label-workflow -am install
if errorlevel 1 goto :failed
call mvn -f phase5-fixed-pom.xml clean install
if errorlevel 1 goto :failed

echo [4/4] Starting corrected demo...
call mvn -f phase5-build\demo-fixed\pom.xml org.openjfx:javafx-maven-plugin:0.0.8:run
if errorlevel 1 goto :failed
exit /b 0

:failed
echo.
echo Build or startup failed. Review the Maven error above.
pause
exit /b 1
