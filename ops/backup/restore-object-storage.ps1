param(
    [Parameter(Mandatory = $true)]
    [string]$SourceArchive,
    [Parameter(Mandatory = $true)]
    [string]$TargetDirectory,
    [string]$Checksum,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

$archivePath = [System.IO.Path]::GetFullPath($SourceArchive)
$targetRoot = [System.IO.Path]::GetFullPath($TargetDirectory)
if (-not (Test-Path -LiteralPath $archivePath -PathType Leaf)) { throw "Source archive not found." }

if (-not [string]::IsNullOrWhiteSpace($Checksum)) {
    $actual = (Get-FileHash -Path $archivePath -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $Checksum.ToLowerInvariant()) { throw "Checksum validation failed." }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($archivePath)
try {
    foreach ($entry in $zip.Entries) {
        $destination = [System.IO.Path]::GetFullPath((Join-Path $targetRoot $entry.FullName))
        if (-not $destination.StartsWith($targetRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Archive entry attempts path traversal."
        }
    }
}
finally {
    $zip.Dispose()
}

if ($DryRun) {
    Write-Output "dryRunValid=$archivePath"
    exit 0
}

New-Item -ItemType Directory -Force -Path $targetRoot | Out-Null
Expand-Archive -Path $archivePath -DestinationPath $targetRoot -Force
Write-Output "restored=$targetRoot"
