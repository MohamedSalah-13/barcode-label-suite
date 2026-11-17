@echo off
setlocal
cd /d "%~dp0"

echo [1/2] Building and testing all modules...
call mvnw.cmd install
if errorlevel 1 goto :failed

echo [2/2] Starting Barcode Label Suite...
call mvnw.cmd -pl barcode-label-app javafx:run
if errorlevel 1 goto :failed
exit /b 0

:failed
echo.
echo Build or startup failed. Review the Maven error above.
pause
exit /b 1
