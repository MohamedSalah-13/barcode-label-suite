@echo off
setlocal
set "PROJECT_ROOT=%~dp0"
set "MAVEN_REPOSITORY=%USERPROFILE%\.m2\repository"

echo [1/3] Installing the phase 3 parent POM...
call mvn "-Dmaven.repo.local=%MAVEN_REPOSITORY%" -N -f "%PROJECT_ROOT%phase3-pom.xml" install
if errorlevel 1 exit /b %errorlevel%

echo [2/3] Building and installing phase 3 libraries...
call mvn "-Dmaven.repo.local=%MAVEN_REPOSITORY%" -f "%PROJECT_ROOT%phase3-final-pom.xml" install
if errorlevel 1 exit /b %errorlevel%

echo [3/3] Starting the JavaFX phase 3 demo...
call mvn "-Dmaven.repo.local=%MAVEN_REPOSITORY%" -f "%PROJECT_ROOT%phase3-build\demo\pom.xml" org.openjfx:javafx-maven-plugin:0.0.8:run
exit /b %errorlevel%

