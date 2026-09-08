$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$directory = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-image-export-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory $directory | Out-Null
try {
    $source = Join-Path $directory 'source.png'
    $bitmap = [Drawing.Bitmap]::new(32, 16)
    $graphics = [Drawing.Graphics]::FromImage($bitmap)
    $graphics.Clear([Drawing.Color]::Red)
    $graphics.FillRectangle([Drawing.Brushes]::Blue, 16, 0, 16, 16)
    $graphics.Dispose()
    $bitmap.Save($source, [Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
    $hash = (Get-FileHash $source).Hash
    foreach ($extension in @('jpg', 'png')) {
        foreach ($size in @(0, 64)) {
            $output = Join-Path $directory "crop-$size.$extension"
            & "$PSScriptRoot/export-guide-image.ps1" -Source $source -Destination $output -X 16 -Y 0 -Width 16 -Height 8 -OutputWidth $size
            $image = [Drawing.Bitmap]::FromFile($output)
            try {
                $expected = if ($size -eq 0) { 16 } else { $size }
                if ($image.Width -ne $expected -or $image.Height -ne ($expected / 2)) { throw 'Export changed crop aspect ratio.' }
                $pixel = $image.GetPixel(8, 4)
                if ($pixel.B -lt 240 -or $pixel.R -gt 15) { throw 'Export selected the wrong source region.' }
                $expectedFormat = if ($extension -eq 'png') { [Drawing.Imaging.ImageFormat]::Png } else { [Drawing.Imaging.ImageFormat]::Jpeg }
                if ($image.RawFormat.Guid -ne $expectedFormat.Guid) { throw "Export is not $extension." }
            } finally { $image.Dispose() }
        }
    }
    foreach ($invalid in @(
        @{ Destination = $source; X = 0; Y = 0; Width = 1; Height = 1 },
        @{ Destination = (Join-Path $directory 'wrong.gif'); X = 0; Y = 0; Width = 1; Height = 1 },
        @{ Destination = (Join-Path $directory 'outside.jpg'); X = 31; Y = 0; Width = 2; Height = 1 },
        @{ Destination = (Join-Path $directory 'outside.jpg'); X = 0; Y = 15; Width = 1; Height = 2 }
    )) {
        $rejected = $false
        try { & "$PSScriptRoot/export-guide-image.ps1" -Source $source @invalid } catch { $rejected = $true }
        if (-not $rejected) { throw 'Invalid export was accepted.' }
    }
    if ((Get-FileHash $source).Hash -ne $hash) { throw 'Original evidence was modified.' }
    'PASS: crop pixels, aspect ratio, JPEG/PNG encoding, native/enlarged output, bounds and source preservation.'
} finally {
    # Only remove this test's explicitly created, unique temporary directory.
    if ([IO.Path]::GetFullPath($directory).StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()), [StringComparison]::OrdinalIgnoreCase)) {
        Remove-Item -LiteralPath $directory -Recurse -Force
    }
}
