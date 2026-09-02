# Run in an interactive Windows desktop, including CodexVM's scheduled session.
$ErrorActionPreference = 'Stop'
$output = Join-Path ([IO.Path]::GetTempPath()) "ae2-capture-$([guid]::NewGuid().ToString('N')).png"
$capture = Join-Path $PSScriptRoot 'capture-ui-region.ps1'
try {
    & $capture -OutputPath $output -X 0 -Y 0 -Width 2 -Height 3
    $image = [Drawing.Image]::FromFile($output)
    try {
        if ($image.Width -ne 2 -or $image.Height -ne 3) { throw 'Wrong capture size' }
    } finally { $image.Dispose() }
    foreach ($invalid in @(
        @{ OutputPath = $output; X = 32767; Y = 32767; Width = 1; Height = 1 },
        @{ OutputPath = "$output.jpg"; X = 0; Y = 0; Width = 1; Height = 1 },
        @{ OutputPath = $output; X = 0; Y = 0; Width = 0; Height = 1 }
    )) {
        $rejected = $false
        try { & $capture @invalid } catch { $rejected = $true }
        if (-not $rejected) { throw 'Invalid capture was accepted' }
    }
    Write-Output 'PASS: dimensions, desktop bounds, PNG output, and positive size'
} finally {
    if (Test-Path -LiteralPath $output) { Remove-Item -LiteralPath $output }
}
