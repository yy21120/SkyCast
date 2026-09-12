param(
    [ValidateSet("Emulator", "UsbDevice")]
    [string]$Target = "Emulator",
    [string]$DeviceSerial = "",
    [string]$ApiBaseUrl = "",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$serverRoot = Join-Path $repoRoot "server"
$androidRoot = Join-Path $repoRoot "android"
$pythonPath = Join-Path $serverRoot ".venv\Scripts\python.exe"
$gradlePath = Join-Path $androidRoot "gradlew.bat"
$apkPath = Join-Path $androidRoot "app\build\outputs\apk\debug\app-debug.apk"
$adbPath = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"

if (-not (Test-Path -LiteralPath $adbPath)) {
    throw "adb was not found at $adbPath. Install Android SDK Platform-Tools first."
}
if (-not (Test-Path -LiteralPath $pythonPath)) {
    throw "server/.venv was not found. Follow server/README.md to create it first."
}

$connectedDevices = & $adbPath devices |
    Select-String '^\S+\s+device$' |
    ForEach-Object { ($_.Line -split '\s+')[0] }

if ([string]::IsNullOrWhiteSpace($DeviceSerial)) {
    $DeviceSerial = if ($Target -eq "Emulator") {
        $connectedDevices | Where-Object { $_.StartsWith("emulator-") } | Select-Object -First 1
    } else {
        $connectedDevices | Where-Object { -not $_.StartsWith("emulator-") } | Select-Object -First 1
    }
}
if (-not $DeviceSerial) {
    throw "No matching Android target is available for $Target."
}

if ([string]::IsNullOrWhiteSpace($ApiBaseUrl)) {
    $ApiBaseUrl = if ($Target -eq "Emulator") {
        "http://10.0.2.2:8000"
    } else {
        "http://127.0.0.1:8000"
    }
}

$listener = Get-NetTCPConnection -LocalPort 8000 -State Listen -ErrorAction SilentlyContinue
if (-not $listener) {
    Start-Process `
        -FilePath $pythonPath `
        -ArgumentList '-m', 'uvicorn', 'app.main:app', '--host', '0.0.0.0', '--port', '8000' `
        -WorkingDirectory $serverRoot `
        -WindowStyle Hidden
    Start-Sleep -Seconds 2
}

$health = Invoke-RestMethod -Uri "http://127.0.0.1:8000/health"
if ($health.status -ne "ok") {
    throw "SkyCast API health check failed."
}

if (-not $SkipBuild) {
    $jdkCandidates = @(
        $env:JAVA_HOME,
        "D:\Android Studio\Android\jbr",
        "C:\Program Files\Android\Android Studio\jbr"
    ) | Where-Object { $_ -and (Test-Path -LiteralPath (Join-Path $_ "bin\java.exe")) }
    if (-not $jdkCandidates) {
        throw "Android Studio JDK was not found. Set JAVA_HOME to Android Studio's jbr directory."
    }
    $env:JAVA_HOME = $jdkCandidates[0]
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    & $gradlePath -p $androidRoot :app:assembleDebug "-PSKYCAST_API_BASE_URL=$ApiBaseUrl"
    if ($LASTEXITCODE -ne 0) { throw "APK build failed." }
}

if ($Target -eq "UsbDevice" -and $ApiBaseUrl -eq "http://127.0.0.1:8000") {
    & $adbPath -s $DeviceSerial reverse tcp:8000 tcp:8000
    if ($LASTEXITCODE -ne 0) { throw "Port forwarding failed. Allow USB debugging from this computer." }
}

& $adbPath -s $DeviceSerial install -r $apkPath
if ($LASTEXITCODE -ne 0) { throw "APK installation failed." }

& $adbPath -s $DeviceSerial shell am force-stop com.yy21120.skycast
& $adbPath -s $DeviceSerial shell am start -n com.yy21120.skycast/.MainActivity
if ($LASTEXITCODE -ne 0) { throw "SkyCast launch failed." }

Write-Host "SkyCast was built for $ApiBaseUrl, installed, and launched on $DeviceSerial."
if ($Target -eq "Emulator") {
    Write-Host "The emulator uses 10.0.2.2 to reach the API running on this computer."
}
