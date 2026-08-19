$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$demoPom = Join-Path $projectRoot 'barcode-label-demo\pom.xml'

Write-Host 'Building and installing Barcode Label Suite...'
& mvn -f (Join-Path $projectRoot 'pom.xml') install -DskipTests
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host 'Starting the JavaFX demo...'
& mvn -f $demoPom javafx:run
exit $LASTEXITCODE

