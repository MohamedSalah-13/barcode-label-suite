@echo off
setlocal
set "PROJECT_ROOT=%~dp0"
set "MAVEN_REPOSITORY=%USERPROFILE%\.m2\repository"

call mvn "-Dmaven.repo.local=%MAVEN_REPOSITORY%" -N -f "%PROJECT_ROOT%phase3-pom.xml" install
if errorlevel 1 exit /b %errorlevel%
call mvn "-Dmaven.repo.local=%MAVEN_REPOSITORY%" -f "%PROJECT_ROOT%phase3-final-pom.xml" install -DskipTests
if errorlevel 1 exit /b %errorlevel%
call mvn "-Dmaven.repo.local=%MAVEN_REPOSITORY%" -f "%PROJECT_ROOT%phase4-final-pom.xml" install
if errorlevel 1 exit /b %errorlevel%
call mvn "-Dmaven.repo.local=%MAVEN_REPOSITORY%" -f "%PROJECT_ROOT%phase4-build\demo\pom.xml" org.openjfx:javafx-maven-plugin:0.0.8:run
exit /b %errorlevel%

