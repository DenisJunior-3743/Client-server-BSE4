param(
    [ValidateSet("Run", "Test", "Package")]
    [string]$Action = "Run"
)
$ErrorActionPreference = "Stop"
$backendDir = Split-Path $PSScriptRoot -Parent
$configFile = Join-Path $backendDir ".env"
if (-not (Test-Path -LiteralPath $configFile)) {
    throw "Create backend/.env from .env.example and fill its values first."
}
$previous = @{}
$previousJava = $env:JAVA_HOME
try {
    foreach ($line in Get-Content -LiteralPath $configFile) {
        if ($line -match '^\s*(?:#|$)') { continue }
        if ($line -notmatch '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$') {
            throw "Invalid .env entry. Expected KEY=value; entry content is hidden."
        }
        $key = $Matches[1]
        $value = $Matches[2].Trim()
        if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        if (-not $previous.ContainsKey($key)) {
            $previous[$key] = [Environment]::GetEnvironmentVariable($key, "Process")
        }
        [Environment]::SetEnvironmentVariable($key, $value, "Process")
    }
    # Use Android Studio's bundled Java 21 when JAVA_HOME has not been set.
    $bundledJdk = Join-Path $env:ProgramFiles "Android\Android Studio\jbr"
    if (-not $env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $bundledJdk "bin\java.exe"))) {
        $env:JAVA_HOME = $bundledJdk
    }
    Push-Location $backendDir
    try {
        $goal = switch ($Action) { "Run" { "spring-boot:run" } "Test" { "test" } "Package" { "package" } }
        & .\mvnw.cmd -B $goal
        if ($LASTEXITCODE -ne 0) { throw "Backend command failed with exit code $LASTEXITCODE" }
    } finally { Pop-Location }
} finally {
    foreach ($key in $previous.Keys) {
        [Environment]::SetEnvironmentVariable($key, $previous[$key], "Process")
    }
    $env:JAVA_HOME = $previousJava
}
