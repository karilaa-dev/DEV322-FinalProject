# Starts an AVD if no devices are connected, then installs the debug APK using install-apk.ps1
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
$projectRoot = (Resolve-Path (Join-Path $scriptRoot '..')).Path
$installScript = Join-Path $scriptRoot 'install-apk.ps1'

Write-Output "Running install script first (if a device is connected)..."
& powershell -NoProfile -ExecutionPolicy Bypass -File $installScript
$rc = $LASTEXITCODE
if ($rc -eq 0) { Write-Output 'APK installed successfully (device was present).'; exit 0 }
if ($rc -eq 2) { Write-Output 'APK not found. Please build the project first.'; exit 2 }
if ($rc -eq 3) { Write-Output 'adb not found. Please install Android platform-tools.'; exit 3 }
if ($rc -ne 4) { Write-Output "Install script exited with code $rc"; exit $rc }

Write-Output 'No devices connected — attempting to start emulator.'
$emulatorCandidates = @(
    "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe",
    "$env:ANDROID_SDK_ROOT\emulator\emulator.exe",
    'C:\\Program Files\\Android\\Android Studio\\emulator\\emulator.exe',
    'C:\\Program Files\\Android\\emulator\\emulator.exe'
)
$emulatorExe = $null
foreach ($c in $emulatorCandidates) { if ($c -and (Test-Path $c)) { $emulatorExe = $c; break } }
if (-not $emulatorExe) { Write-Output 'Emulator binary not found in common locations.'; exit 5 }

Write-Output "Using emulator binary: $emulatorExe"
$avds = & $emulatorExe -list-avds 2>&1
Write-Output "Available AVDs: $avds"
$avdLines = $avds -split "`n" | Where-Object { $_ -and $_ -notmatch '^\s*$' }
if (-not $avdLines) { Write-Output 'No AVDs found. Create one via Android Studio AVD Manager.'; exit 6 }
$avdName = $avdLines[0].Trim()
Write-Output "Starting AVD: $avdName"
Start-Process -FilePath $emulatorExe -ArgumentList ('-avd', $avdName) -WindowStyle Normal

# Find adb
$adbCmd = (Get-Command adb -ErrorAction SilentlyContinue)
$adbPath = if ($adbCmd) { $adbCmd.Source } else { "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" }

$waitSec = 0
$maxWait = 300
$connected = $null
while ($waitSec -lt $maxWait) {
    try {
        $devs = & $adbPath devices 2>&1
        $devLines = $devs -split "`n" | Select-Object -Skip 1 | Where-Object { $_ -and $_ -notmatch '^\s*$' }
        $connected = $devLines | Where-Object { $_ -match '\S+\s+device$' }
        if ($connected) { Write-Output "Device detected: $connected"; break }
    } catch {}
    Start-Sleep -Seconds 5
    $waitSec += 5
    Write-Output "Waiting for emulator to boot... $waitSec s"
}

if (-not $connected) { Write-Output 'Timeout waiting for emulator to boot.'; exit 7 }

Write-Output 'Installing APK now...'
& powershell -NoProfile -ExecutionPolicy Bypass -File $installScript
exit $LASTEXITCODE
