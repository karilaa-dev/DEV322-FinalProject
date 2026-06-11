param(
    [string]$ProjectRoot = $null
)

if (-not $ProjectRoot) {
    $scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
    $ProjectRoot = (Resolve-Path (Join-Path $scriptRoot '..')).Path
} else {
    $ProjectRoot = (Resolve-Path $ProjectRoot).Path
}

Set-Location $ProjectRoot

# Find adb
$cmd = Get-Command adb -ErrorAction SilentlyContinue
if ($cmd) { $adb = $cmd.Source } else { $adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe' }

if (-not (Test-Path $adb)) {
    Write-Output "adb not found: $adb"
    exit 2
}

$devs = & $adb devices 2>&1
$lines = $devs -split "`n" | Select-Object -Skip 1 | Where-Object { $_ -and $_ -notmatch '^\s*$' }
$connected = $lines | Where-Object { $_ -match '\S+\s+device$' }

if (-not $connected) {
    Write-Output 'No connected devices in device state.'
    exit 3
}

$serial = ($connected[0] -replace '\s+device$','').Trim()
Write-Output "Starting app on $serial"
& $adb -s $serial shell am start -n 'com.bananaginger.noisedetector/.MainActivity' 2>&1 | ForEach-Object { Write-Output $_ }
exit $LASTEXITCODE
