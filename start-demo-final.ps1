$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$codexRoot = Split-Path $projectRoot -Parent
$documentsRoot = Split-Path $codexRoot -Parent
$userRoot = Split-Path $documentsRoot -Parent

$rootPom = Join-Path $projectRoot 'pom.xml'
$demoPom = Join-Path $projectRoot 'barcode-label-demo\pom.xml'
$mavenRepository = Join-Path $userRoot '.m2\repository'
$repositoryOption = "-Dmaven.repo.local=$mavenRepository"
$javafxGoal = 'org.openjfx:javafx-maven-plugin:0.0.8:run'

Write-Host "Project: $projectRoot"
Write-Host "Maven repository: $mavenRepository"

& mvn $repositoryOption -f $rootPom install -DskipTests
if ($LASTEXITCODE -ne 0) {
    throw "Project build failed with exit code $LASTEXITCODE"
}

& mvn $repositoryOption -f $demoPom $javafxGoal
if ($LASTEXITCODE -ne 0) {
    throw "JavaFX demo failed with exit code $LASTEXITCODE"
}

