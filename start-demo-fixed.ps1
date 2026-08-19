$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$documentsRoot = Split-Path $projectRoot -Parent
$userRoot = Split-Path $documentsRoot -Parent
$rootPom = Join-Path $projectRoot 'pom.xml'
$demoPom = Join-Path $projectRoot 'barcode-label-demo\pom.xml'
$mavenRepository = Join-Path $userRoot '.m2\repository'
$repositoryOption = "-Dmaven.repo.local=$mavenRepository"
$javafxGoal = 'org.openjfx:javafx-maven-plugin:0.0.8:run'

Write-Host "Using Maven repository: $mavenRepository"
Write-Host 'Building Barcode Label Suite...'
& mvn $repositoryOption -f $rootPom install -DskipTests
if ($LASTEXITCODE -ne 0) {
    throw "Maven build failed with exit code $LASTEXITCODE"
}

Write-Host 'Starting JavaFX demo...'
& mvn $repositoryOption -f $demoPom $javafxGoal
if ($LASTEXITCODE -ne 0) {
    throw "JavaFX demo failed with exit code $LASTEXITCODE"
}

