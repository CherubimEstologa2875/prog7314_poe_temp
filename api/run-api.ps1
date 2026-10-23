<#
.SYNOPSIS
    Finds a working JDK, points JAVA_HOME at it, and starts the Runway API.

.DESCRIPTION
    The Gradle wrapper launches its JVM from %JAVA_HOME%\bin\java.exe before it
    reads any project file, so a JAVA_HOME pointing at a broken runtime fails
    with "could not open ...\lib\jvm.cfg" and nothing in the build can correct
    it (Gradle, 2026).

    That message means java.exe exists but the runtime beside it does not -
    typically an Android Studio update that did not finish writing its bundled
    JetBrains Runtime. This script tests candidates for BOTH files before
    trusting one, so a half-installed JDK is skipped rather than used.

    Run it from the api folder:
        .\run-api.ps1

.NOTES
    PROG7314 POE Part 2, Group 11 - SCRUM-78.
#>

#Requires -Version 5.1

$ErrorActionPreference = 'Stop'

# A JDK is only usable if the launcher AND the runtime configuration are both
# present. Checking java.exe alone is what lets a corrupt install through.
function Test-Jdk {
    param([string]$JdkPath)

    if ([string]::IsNullOrWhiteSpace($JdkPath)) { return $false }
    if (-not (Test-Path (Join-Path $JdkPath 'bin\java.exe'))) { return $false }
    if (-not (Test-Path (Join-Path $JdkPath 'lib\jvm.cfg'))) { return $false }
    return $true
}

Write-Host 'Looking for a usable JDK...' -ForegroundColor Cyan

$candidates = New-Object System.Collections.Generic.List[string]

# 1. Whatever JAVA_HOME already says - it may well be fine.
if ($env:JAVA_HOME) { $candidates.Add($env:JAVA_HOME) }

# 2. The java on PATH, walked back two folders to its JDK home.
$javaCommand = Get-Command java -ErrorAction SilentlyContinue
if ($javaCommand) {
    $candidates.Add((Split-Path (Split-Path $javaCommand.Source -Parent) -Parent))
}

# 3. Standard install locations, newest name first.
$roots = @(
    (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
    (Join-Path $env:ProgramFiles 'Java'),
    (Join-Path $env:LOCALAPPDATA 'Programs\Eclipse Adoptium')
)
foreach ($root in $roots) {
    if (Test-Path $root) {
        Get-ChildItem $root -Directory |
            Sort-Object Name -Descending |
            ForEach-Object { $candidates.Add($_.FullName) }
    }
}

# 4. Android Studio's bundled runtime, last - it is the one that tends to break.
$candidates.Add((Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'))

$jdk = $candidates | Where-Object { Test-Jdk $_ } | Select-Object -First 1

if (-not $jdk) {
    Write-Host ''
    Write-Host 'No usable JDK found.' -ForegroundColor Red
    Write-Host 'Install Temurin 21, then run this script again:'
    Write-Host '    winget install EclipseAdoptium.Temurin.21.JDK --source winget'
    Write-Host '    https://adoptium.net/temurin/releases/?version=21'
    exit 1
}

Write-Host "Using JDK: $jdk" -ForegroundColor Green

# For this process, so the wrapper below picks it up immediately.
$env:JAVA_HOME = $jdk

# And permanently for this Windows user, so new terminals and Android Studio
# inherit it. Only rewritten when it is actually wrong, to avoid churn.
$persisted = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
if ($persisted -ne $jdk) {
    [Environment]::SetEnvironmentVariable('JAVA_HOME', $jdk, 'User')
    Write-Host 'JAVA_HOME saved for this user. New terminals will pick it up.' -ForegroundColor Green
}

# Default to `run`, but allow `.\run-api.ps1 build` or `test` as well.
$gradleArgs = if ($args.Count -gt 0) { $args } else { @('run') }

# Gradle takes the project directory from the working directory, so make sure
# it is this folder however the script was invoked.
Push-Location $PSScriptRoot
try {
    & (Join-Path $PSScriptRoot 'gradlew.bat') $gradleArgs
    exit $LASTEXITCODE
} finally {
    Pop-Location
}

<# Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Gradle, 2026. The Gradle wrapper. [online] Available at: <https://docs.gradle.org/current/userguide/gradle_wrapper.html> [Accessed 11 September 2026].
#>
