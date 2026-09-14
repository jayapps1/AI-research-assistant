param(
    [string]$SourceDirectory = $env:DOCUMENT_STORAGE_DIR,
    [string]$OutputDirectory = $env:BACKUP_ROOT_DIR
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($SourceDirectory)) { $SourceDirectory = ".\data\documents" }
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) { $OutputDirectory = ".\data\backups\objects" }

$sourceRoot = [System.IO.Path]::GetFullPath($SourceDirectory)
$outputRoot = [System.IO.Path]::GetFullPath($OutputDirectory)
if (-not (Test-Path -LiteralPath $sourceRoot -PathType Container)) { throw "Document storage source directory does not exist." }
if ($outputRoot.StartsWith($sourceRoot, [System.StringComparison]::OrdinalIgnoreCase)) { throw "Backup destination must not be inside live document storage." }

New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$archivePath = Join-Path $outputRoot "document-storage-$timestamp.zip"
$manifestPath = Join-Path $outputRoot "document-storage-$timestamp.manifest.csv"

Get-ChildItem -LiteralPath $sourceRoot -Recurse -File | ForEach-Object {
    $relative = [System.IO.Path]::GetRelativePath($sourceRoot, $_.FullName)
    $hash = Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256
    [PSCustomObject]@{
        path = $relative
        sizeBytes = $_.Length
        sha256 = $hash.Hash.ToLowerInvariant()
    }
} | Export-Csv -NoTypeInformation -Path $manifestPath

Compress-Archive -Path (Join-Path $sourceRoot "*") -DestinationPath $archivePath -Force
$archiveHash = Get-FileHash -Path $archivePath -Algorithm SHA256
$manifestHash = Get-FileHash -Path $manifestPath -Algorithm SHA256
$archiveHash.Hash.ToLowerInvariant() | Set-Content -NoNewline -Path "$archivePath.sha256"
$manifestHash.Hash.ToLowerInvariant() | Set-Content -NoNewline -Path "$manifestPath.sha256"

Write-Output "archive=$archivePath"
Write-Output "manifest=$manifestPath"
Write-Output "archiveSha256=$($archiveHash.Hash.ToLowerInvariant())"
