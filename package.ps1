[CmdletBinding()]
param(
    [ValidateSet('app-image', 'exe', 'msi')]
    [string]$Type = 'app-image',
    [switch]$SkipBuild
)

# The steps every program's packaging script repeats (finding the JDK, Maven, the version, the input
# folder, WiX, the installer's options) come from desktop-updater's tools/Packaging.psm1, found in a
# clone of desktop-updater beside this repository or where DESKTOP_UPDATER_HOME points. What is
# Barcode Label Suite's own stays here.

$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath($PSScriptRoot).TrimEnd('\')
$demoDirectory = Join-Path $projectRoot 'barcode-label-app'
$targetDirectory = Join-Path $demoDirectory 'target'
$inputDirectory = Join-Path $targetDirectory 'package-input'
$outputDirectory = Join-Path $projectRoot 'dist'
$appName = 'BarcodeLabelSuite'
$mainJar = 'barcode-label-app.jar'
$mainClass = 'com.mohamed.barcodelabel.app.BarcodeLabelSuiteLauncher'
# Drawn by scripts/AppIcon.java; the window icons inside the app come from the same drawing.
$iconFile = Join-Path $demoDirectory 'src\main\package\BarcodeLabelSuite.ico'

$updaterHome = $env:DESKTOP_UPDATER_HOME
if (-not $updaterHome) {
    $repository = & git -C $projectRoot rev-parse --show-toplevel 2>$null
    if (-not $repository) { $repository = $projectRoot }
    $updaterHome = Join-Path (Split-Path -Parent $repository) 'desktop-updater'
}
$packaging = Join-Path $updaterHome 'tools/Packaging.psm1'
if (-not (Test-Path -LiteralPath $packaging)) {
    throw "desktop-updater was not found at $updaterHome. Clone https://github.com/MohamedSalah-13/desktop-updater beside this repository, or set DESKTOP_UPDATER_HOME."
}
Import-Module $packaging -Force

# The version comes from the root POM, so the installer never drifts from the build.
$appVersion = (Get-PomVersion -Pom (Join-Path $projectRoot 'pom.xml') -Platform windows).Raw
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

# The Maven wrapper reads its settings from the project, wherever the script was started from.
Push-Location $projectRoot
try {
    if (-not $SkipBuild) {
        Invoke-MavenBuild -Pom (Join-Path $projectRoot 'pom.xml') -Arguments @('clean', 'install')
    }

    # The runtime is the JDK jpackage comes from, and the update check's TLS needs JDK 21's module set.
    $jpackage = (Find-JPackage -ExactVersion 21).Path

    if (-not (Test-Path -LiteralPath $iconFile -PathType Leaf)) {
        throw "Application icon was not found: $iconFile. Run scripts/AppIcon.java; the command is in its header."
    }

    $inputDirectory = Assert-ProjectPath $inputDirectory
    $outputDirectory = Assert-ProjectPath $outputDirectory
    $packagedApplication = Assert-ProjectPath (Join-Path $outputDirectory $appName)

    $builtJar = Join-Path $targetDirectory $mainJar
    if (-not (Test-Path -LiteralPath $builtJar -PathType Leaf)) {
        throw "Application JAR was not found: $builtJar"
    }
    [void](New-JPackageInput -Path $inputDirectory -Item $builtJar)
    Invoke-MavenBuild -Pom (Join-Path $demoDirectory 'pom.xml') -ClearReadOnly @() -Arguments @(
        'dependency:copy-dependencies',
        "-DoutputDirectory=$inputDirectory",
        '-DincludeScope=runtime'
    )

    if ($Type -eq 'app-image' -and (Test-Path -LiteralPath $packagedApplication)) {
        [void](Clear-ReadOnly -Path $packagedApplication)
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
        [void](Find-WiX)
        # Installed for the current user only, so an update installs without asking for an administrator.
        # Never change the upgrade code: a new installer replaces an older one only when both carry the same one.
        $arguments += @(Get-WinInstallerArgument -Menu -Shortcut -PerUser -UpgradeUuid 'f5cf66fa-17c3-433c-87e8-c04a29dc00e6')
    }

    try {
        Invoke-Native -FilePath $jpackage -ArgumentList $arguments
    } catch {
        throw "jpackage failed: $($_.Exception.Message) MSI/EXE packaging also requires WiX Toolset."
    }
} finally {
    Pop-Location
}

Write-Host "Package created in $outputDirectory" -ForegroundColor Green
