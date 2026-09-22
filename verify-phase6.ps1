[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$mavenWrapper = Join-Path $projectRoot 'mvnw.cmd'

function Invoke-Maven {
    param([Parameter(Mandatory)][string[]]$Arguments)

    & $mavenWrapper @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Maven failed with exit code ${LASTEXITCODE}: $($Arguments -join ' ')"
    }
}

Push-Location $projectRoot
try {
    Invoke-Maven @('clean', 'install')
    Invoke-Maven @('-f', 'phase2-pom.xml', 'clean', 'install')
    Invoke-Maven @('-N', '-f', 'phase3-pom.xml', 'install')
    Invoke-Maven @('-N', '-f', 'phase3-build-pom.xml', 'install')
    Invoke-Maven @('-f', 'phase3-final-pom.xml', 'clean', 'install')
    Invoke-Maven @('-f', 'phase4-final-pom.xml', 'clean', 'install')
    Invoke-Maven @('-f', 'phase5-pom.xml', '-pl', 'barcode-label-workflow', '-am', 'clean', 'install')
    Invoke-Maven @('-f', 'phase5-fixed-pom.xml', 'clean', 'install')
    Invoke-Maven @('-f', 'phase6-final-pom.xml', 'clean', 'install')
}
finally {
    Pop-Location
}

Write-Host 'Phase 6 verification completed successfully.' -ForegroundColor Green
