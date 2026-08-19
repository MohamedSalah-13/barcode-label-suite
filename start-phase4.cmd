@echo off
setlocal
set "PROJECT_ROOT=%~dp0"
set "MAVEN_REPOSITORY=%USERPROFILE%\.m2\repository"

echo [1/4] Installing phase 3 parent metadata...
call mvn "-Dmaven.repo.local=%MAVEN_REPOSITORY%" -N -f "%PROJECT_ROOT%phase3-pom.xml" install
if errorlevel 1 exit /b %errorlevel%

echo [2/4] Installing printing libraries...
call mvn "-Dmaven.repo.local=%MAVEN_REPOSITORY%" -f "%PROJECT_ROOT%phase3-final-pom.xml" install -DskipTests
if errorlevel 1 exit /b %errorlevel%

echo [3/4] Building the visual designer...
call mvn "-Dmaven.repo.local=%MAVEN_REPOSITORY%" -f "%PROJECT_ROOT%phase4-pom.xml" install
if errorlevel 1 exit /b %errorlevel%

echo [4/4] Starting the phase 4 demo...
call mvn "-Dmaven.repo.local=%MAVEN_REPOSITORY%" -f "%PROJECT_ROOT%barcode-label-demo-v4\pom.xml" org.openjfx:javafx-maven-plugin:0.0.8:run
exit /b %errorlevel%

