<#
PowerShell helper to detect installed JDK (Temurin/Eclipse Adoptium), set JAVA_HOME and user Path,
and optionally run `gradlew assembleDebug` from the project root.
Run from repository root or execute this script directly.
#>

param(
    [switch]$RunWithoutPrompt
)

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
$projectRoot = Resolve-Path (Join-Path $scriptRoot '..')
Set-Location $projectRoot

function Find-Java {
    # Try command in PATH first
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    # Common installation locations (check most likely folders)
    $candidateDirs = @(
        'C:\\Program Files\\Eclipse Adoptium',
        'C:\\Program Files\\Adoptium',
        'C:\\Program Files\\Temurin',
        'C:\\Program Files\\Java',
        'C:\\Program Files (x86)\\Java'
    )
    foreach ($d in $candidateDirs) {
        if (Test-Path $d) {
            $jdk = Get-ChildItem $d -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -match 'jdk|java|temurin|adoptium' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
            if ($jdk) {
                $candidate = Join-Path $jdk.FullName 'bin\\java.exe'
                if (Test-Path $candidate) { return $candidate }
            }
        }
    }

    # Last-resort: limited recursive search for java.exe (may take some seconds)
    try {
        $found = Get-ChildItem -Path 'C:\\Program Files','C:\\Program Files (x86)' -Filter 'java.exe' -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
        if ($found) { return $found }
    } catch {}

    return $null
}

Write-Host "Looking for installed Java (will wait up to ~5 minutes)..."
$javaPath = $null
$attempts = 0
while (-not $javaPath -and $attempts -lt 100) {
    $javaPath = Find-Java
    if (-not $javaPath) { Start-Sleep -Seconds 3; $attempts++ }
}

if (-not $javaPath) {
    Write-Error "Java executable not found. Please wait until the installer finishes, then re-run this script."
    exit 1
}

Write-Host "Found java: $javaPath"
$binDir = Split-Path $javaPath -Parent
$javaHome = Split-Path $binDir -Parent
Write-Host "Setting JAVA_HOME = $javaHome"

# Set for current session
$env:JAVA_HOME = $javaHome
$env:Path = "$($javaHome)\\bin;$env:Path"

# Persist for current user (no admin required)
try {
    [Environment]::SetEnvironmentVariable('JAVA_HOME',$javaHome,'User')
    $userPath = [Environment]::GetEnvironmentVariable('Path','User')
    if ($userPath -notlike "*$($javaHome)\\bin*") {
        [Environment]::SetEnvironmentVariable('Path',$userPath + ';' + "$($javaHome)\\bin",'User')
    }
} catch {
    Write-Warning "Failed to set user environment variables: $_"
}

Write-Host "java -version:" 
& java -version 2>&1 | ForEach-Object { Write-Host $_ }
Write-Host "javac -version:" 
& javac -version 2>&1 | ForEach-Object { Write-Host $_ }

if (-not $RunWithoutPrompt) {
    $choice = Read-Host "Run assembleDebug build now? (Y/N)"
    if ($choice -ne 'Y' -and $choice -ne 'y') { Write-Host "Declined. Script finished."; exit 0 }
}

Write-Host "Running Gradle assembleDebug..."
$gradle = Join-Path $projectRoot 'gradlew.bat'
if (-not (Test-Path $gradle)) { Write-Error "gradlew.bat не найден в $gradle"; exit 1 }

& $gradle 'assembleDebug'

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build completed successfully. APK: $projectRoot\app\build\outputs\apk\debug\app-debug.apk"
    exit 0
} else {
    Write-Error "Build finished with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}
