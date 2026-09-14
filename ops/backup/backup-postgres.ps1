param(
    [string]$OutputDirectory = $env:BACKUP_ROOT_DIR,
    [string]$Database = $env:POSTGRES_DB,
    [string]$HostName = $env:DB_HOST,
    [string]$Port = $env:DB_PORT,
    [string]$UserName = $env:POSTGRES_USER
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($OutputDirectory)) { $OutputDirectory = ".\data\backups\postgres" }
if ([string]::IsNullOrWhiteSpace($Database)) { throw "POSTGRES_DB or -Database is required." }
if ([string]::IsNullOrWhiteSpace($HostName)) { $HostName = "localhost" }
if ([string]::IsNullOrWhiteSpace($Port)) { $Port = "5432" }
if ([string]::IsNullOrWhiteSpace($UserName)) { throw "POSTGRES_USER or -UserName is required." }
if ([string]::IsNullOrWhiteSpace($env:PGPASSWORD)) { throw "PGPASSWORD must be set for pg_dump without exposing the password." }

$resolvedOutput = [System.IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Force -Path $resolvedOutput | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$dumpPath = Join-Path $resolvedOutput "$Database-$timestamp.dump"
$checksumPath = "$dumpPath.sha256"

& pg_dump -h $HostName -p $Port -U $UserName -d $Database -F c -f $dumpPath
if ($LASTEXITCODE -ne 0) {
    throw "pg_dump failed with exit code $LASTEXITCODE."
}

$hash = Get-FileHash -Path $dumpPath -Algorithm SHA256
$hash.Hash.ToLowerInvariant() | Set-Content -NoNewline -Path $checksumPath

Write-Output "backup=$dumpPath"
Write-Output "sha256=$($hash.Hash.ToLowerInvariant())"
