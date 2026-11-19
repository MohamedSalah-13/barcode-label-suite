[CmdletBinding()]
param(
    [ValidateSet('app-image', 'exe', 'msi')]
    [string]$Type = 'app-image',
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath($PSScriptRoot).TrimEnd('\')
$mavenWrapper = Join-Path $projectRoot 'mvnw.cmd'
$demoDirectory = Join-Path $projectRoot 'barcode-label-app'
$targetDirectory = Join-Path $demoDirectory 'target'
$inputDirectory = Join-Path $targetDirectory 'package-input'
$outputDirectory = Join-Path $projectRoot 'dist'
$appName = 'BarcodeLabelSuite'
$mainJar = 'barcode-label-app.jar'
$mainClass = 'com.mohamed.barcodelabel.app.BarcodeLabelSuiteLauncher'
# Drawn by scripts/AppIcon.java; the window icons inside the app come from the same drawing.
$iconFile = Join-Path $demoDirectory 'src\main\package\BarcodeLabelSuite.ico'
# The version comes from the root POM, so the installer never drifts from the build.
$appVersion = ([xml](Get-Content -LiteralPath (Join-Path $projectRoot 'pom.xml') -Raw)).project.version
if (-not ($appVersion -match '^\d+\.\d+\.\d+$')) {
    throw "jpackage needs a numeric version such as 1.2.3; pom.xml has '$appVersion'."
}

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
    Invoke-Maven @('clean', 'install')
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

if (-not (Test-Path -LiteralPath $iconFile -PathType Leaf)) {
    throw "Application icon was not found: $iconFile. Run: java scripts/AppIcon.java"
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
    '--app-version', $appVersion,
    '--vendor', 'Mohamed',
    '--input', $inputDirectory,
    '--main-jar', $mainJar,
    '--main-class', $mainClass,
    '--icon', $iconFile,
    # No --add-modules: for a classpath app jpackage puts the JDK's whole module set in the runtime, which already
    # has jdk.crypto.ec, the TLS provider the update check needs to reach GitHub over HTTPS on JDK 21. Naming
    # modules here would replace that set, not add to it.
    '--java-options', '-Dfile.encoding=UTF-8'
)

if ($Type -in @('exe', 'msi')) {
    $arguments += @(
        '--win-menu', '--win-shortcut',
        # Installed for the current user only, so an update installs without asking for an administrator.
        '--win-per-user-install',
        # Never change this: a new installer replaces an older one only when both carry the same upgrade code.
        '--win-upgrade-uuid', 'f5cf66fa-17c3-433c-87e8-c04a29dc00e6'
    )
}

& $jpackage @arguments
if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed with exit code $LASTEXITCODE. MSI/EXE packaging also requires WiX Toolset."
}

Write-Host "Package created in $outputDirectory" -ForegroundColor Green
