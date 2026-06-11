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
$apk = Join-Path (Get-Location) 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $apk)) {
    Write-Output "APK not found: $apk"
    exit 2
}

$cmd = Get-Command adb -ErrorAction SilentlyContinue
if ($cmd) { $adbCmd = $cmd.Source } else { $adbCmd = $null }

if (-not $adbCmd) {
    $cands = @(
        "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe",
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
        'C:\Program Files\Android\platform-tools\adb.exe',
        'C:\Program Files (x86)\Android\platform-tools\adb.exe',
        'C:\Android\platform-tools\adb.exe',
        'C:\Program Files\Android\Android Studio\platform-tools\adb.exe',
        'C:\Users\Public\Android\Sdk\platform-tools\adb.exe'
    )
    foreach ($p in $cands) { if ($p -and (Test-Path $p)) { $adbCmd = $p; break } }
}

if (-not $adbCmd) {
    try {
        # Limit recursive search to common program files roots to avoid full-drive scans.
        $found = Get-ChildItem -Path 'C:\Program Files','C:\Program Files (x86)' -Filter 'adb.exe' -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
        if ($found) { $adbCmd = $found }
    } catch {}
}

if (-not $adbCmd) {
    Write-Output 'adb not found on PATH or common locations. Please install platform-tools or add adb to PATH.'
    exit 3
}

Write-Output "Using adb: $adbCmd"
$devout = & $adbCmd devices 2>&1
Write-Output $devout
$devlines = $devout -split "`n" | Select-Object -Skip 1 | Where-Object { $_ -and $_ -notmatch '^\s*$' }
$connected = $devlines | Where-Object { $_ -match '\S+\s+device$' }
if (-not $connected) {
    Write-Output 'No connected devices/emulators in device state.'
    exit 4
}

$first = ($connected[0] -replace '\s+device$','').Trim()
Write-Output "Installing APK to device: $first"
& $adbCmd -s $first install -r $apk 2>&1 | ForEach-Object { Write-Output $_ }
exit $LASTEXITCODE
