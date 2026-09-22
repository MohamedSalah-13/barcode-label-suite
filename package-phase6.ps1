[CmdletBinding()]
param(
    [ValidateSet('app-image', 'exe', 'msi')]
    [string]$Type = 'app-image',
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath($PSScriptRoot).TrimEnd('\')
$mavenWrapper = Join-Path $projectRoot 'mvnw.cmd'
$demoDirectory = Join-Path $projectRoot 'phase6-build\demo-final'
$targetDirectory = Join-Path $demoDirectory 'target'
$inputDirectory = Join-Path $targetDirectory 'package-input'
$outputDirectory = Join-Path $projectRoot 'dist'
$appName = 'BarcodeLabelSuite'
$mainJar = 'barcode-label-demo-v6-final-0.6.1-SNAPSHOT.jar'
$mainClass = 'com.mohamed.barcodelabel.demo.v6.Phase6Launcher'

function Assert-ProjectPath {
    param([Parameter(Mandatory)][string]$Path)

    $resolved = [IO.Path]::GetFullPath($Path)
    if (-not $resolved.StartsWith("$projectRoot\", [StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to modify a path outside the project: $resolved"
    }
    return $resolved
}

function Invoke-Maven {
    param([Parameter(Mandatory)][string[]]$Arguments)

    & $mavenWrapper @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Maven failed with exit code ${LASTEXITCODE}: $($Arguments -join ' ')"
    }
}

if (-not $SkipBuild) {
    & (Join-Path $projectRoot 'verify-phase6.ps1')
}

$jpackage = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\jpackage.exe' } else { $null }
if (-not $jpackage -or -not (Test-Path -LiteralPath $jpackage -PathType Leaf)) {
    $jpackageCommand = Get-Command jpackage.exe -ErrorAction SilentlyContinue
    if (-not $jpackageCommand) {
        throw 'jpackage.exe was not found. Set JAVA_HOME to a JDK 21 installation.'
    }
    $jpackage = $jpackageCommand.Source
}

$jpackageVersion = & $jpackage --version
if ($LASTEXITCODE -ne 0 -or -not ($jpackageVersion -match '^21(?:\.|$)')) {
    throw "JDK 21 jpackage is required; found version '$jpackageVersion'."
}

$inputDirectory = Assert-ProjectPath $inputDirectory
$outputDirectory = Assert-ProjectPath $outputDirectory
$packagedApplication = Assert-ProjectPath (Join-Path $outputDirectory $appName)

if (Test-Path -LiteralPath $inputDirectory) {
    Remove-Item -LiteralPath $inputDirectory -Recurse -Force
}
New-Item -ItemType Directory -Path $inputDirectory | Out-Null

Invoke-Maven @(
    '-f', (Join-Path $demoDirectory 'pom.xml'),
    'dependency:copy-dependencies',
    "-DoutputDirectory=$inputDirectory",
    '-DincludeScope=runtime'
)

$builtJar = Join-Path $targetDirectory $mainJar
if (-not (Test-Path -LiteralPath $builtJar -PathType Leaf)) {
    throw "Application JAR was not found: $builtJar"
}
Copy-Item -LiteralPath $builtJar -Destination $inputDirectory

if ($Type -eq 'app-image' -and (Test-Path -LiteralPath $packagedApplication)) {
    Remove-Item -LiteralPath $packagedApplication -Recurse -Force
}
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

$arguments = @(
    '--type', $Type,
    '--dest', $outputDirectory,
    '--name', $appName,
    '--app-version', '0.6.1',
    '--vendor', 'Mohamed',
    '--input', $inputDirectory,
    '--main-jar', $mainJar,
    '--main-class', $mainClass,
    '--java-options', '-Dfile.encoding=UTF-8'
)

if ($Type -in @('exe', 'msi')) {
    $arguments += @('--win-menu', '--win-shortcut')
}

& $jpackage @arguments
if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed with exit code $LASTEXITCODE. MSI/EXE packaging also requires WiX Toolset."
}

Write-Host "Package created in $outputDirectory" -ForegroundColor Green
