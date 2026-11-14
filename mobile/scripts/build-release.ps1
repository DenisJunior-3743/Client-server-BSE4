param(
    [string]$VersionName = "1.2.0-beta.1",
    [ValidateRange(1, 2100000000)][int]$VersionCode = 3,
    [ValidateSet("testing", "production")][string]$Channel = "testing",
    [string]$ApiBaseUrl = "https://client-server-bse4-backend.vercel.app/"
)
$ErrorActionPreference = "Stop"
$mobileDir = Split-Path $PSScriptRoot -Parent
$repoDir = Split-Path $mobileDir -Parent
$previousJava = $env:JAVA_HOME
try {
    if (-not $env:JAVA_HOME) {
        $bundledJdk = Join-Path $env:ProgramFiles "Android\Android Studio\jbr"
        if (Test-Path -LiteralPath (Join-Path $bundledJdk "bin\java.exe")) { $env:JAVA_HOME = $bundledJdk }
    }
    Push-Location $mobileDir
    try {
        $arguments = @(':app:assembleRelease', ':app:testReleaseUnitTest', '--no-daemon', '--max-workers=2',
            "-PciVersionCode=$VersionCode", "-PciVersionName=$VersionName", "-PrevidenChannel=$Channel", "-PapiBaseUrl=$ApiBaseUrl")
        & .\gradlew.bat @arguments
        if ($LASTEXITCODE -ne 0) { throw "Release build failed" }
    } finally { Pop-Location }
    & python (Join-Path $PSScriptRoot 'reviden\verify-apk.py') --apk (Join-Path $mobileDir 'app\build\outputs\apk\release\app-release.apk') --version-name $VersionName --version-code $VersionCode --channel $Channel --api-base-url $ApiBaseUrl
    if ($LASTEXITCODE -ne 0) { throw "Release APK verification failed" }
} finally { $env:JAVA_HOME = $previousJava }
