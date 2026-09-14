param(
    [Parameter(Mandatory = $true)]
    [string]$SourceDump,
    [Parameter(Mandatory = $true)]
    [string]$TargetDatabase,
    [string]$Checksum,
    [string]$HostName = $env:DB_HOST,
    [string]$Port = $env:DB_PORT,
    [string]$UserName = $env:POSTGRES_USER,
    [switch]$ConfirmDestructive
)

$ErrorActionPreference = "Stop"

if (-not $ConfirmDestructive) { throw "Pass -ConfirmDestructive after verifying the target database is safe to replace." }
if ([string]::IsNullOrWhiteSpace($TargetDatabase)) { throw "Explicit target database is required." }
if ([string]::IsNullOrWhiteSpace($HostName)) { $HostName = "localhost" }
if ([string]::IsNullOrWhiteSpace($Port)) { $Port = "5432" }
if ([string]::IsNullOrWhiteSpace($UserName)) { throw "POSTGRES_USER or -UserName is required." }
if ([string]::IsNullOrWhiteSpace($env:PGPASSWORD)) { throw "PGPASSWORD must be set for pg_restore without exposing the password." }

$dumpPath = [System.IO.Path]::GetFullPath($SourceDump)
if (-not (Test-Path -LiteralPath $dumpPath -PathType Leaf)) { throw "Source dump not found." }

if (-not [string]::IsNullOrWhiteSpace($Checksum)) {
    $actual = (Get-FileHash -Path $dumpPath -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $Checksum.ToLowerInvariant()) {
        throw "Checksum validation failed."
    }
}

& pg_restore -h $HostName -p $Port -U $UserName -d $TargetDatabase --clean --if-exists --no-owner $dumpPath
if ($LASTEXITCODE -ne 0) {
    throw "pg_restore failed with exit code $LASTEXITCODE."
}

Write-Output "restored=$TargetDatabase"
