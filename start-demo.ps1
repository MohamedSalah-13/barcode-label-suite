$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$rootPom = Join-Path $projectRoot 'pom.xml'
$demoPom = Join-Path $projectRoot 'barcode-label-demo\pom.xml'
$javafxGoal = 'org.openjfx:javafx-maven-plugin:0.0.8:run'

Write-Host 'Building Barcode Label Suite...'
& mvn -f $rootPom install -DskipTests
if ($LASTEXITCODE -ne 0) {
    throw "Maven build failed with exit code $LASTEXITCODE"
}

Write-Host 'Starting JavaFX demo...'
& mvn -f $demoPom $javafxGoal
if ($LASTEXITCODE -ne 0) {
    throw "JavaFX demo failed with exit code $LASTEXITCODE"
}

