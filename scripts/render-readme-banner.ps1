param(
    [Parameter(Mandatory = $true)]
    [string]$MinecraftClientJar,

    [Parameter(Mandatory = $true)]
    [string]$Ae2Jar
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem
$root = Split-Path -Parent $PSScriptRoot
$jar = $null
$ae2 = $null
$atlas = $null
$icon = $null
$terminalBackground = $null
$terminal = $null
$canvas = $null
$graphics = $null
$fontStream = $null
try {
    $jar = [IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $MinecraftClientJar))
    $ae2 = [IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $Ae2Jar))

    function Read-Png($Archive, [string]$EntryName) {
        $entry = $Archive.GetEntry($EntryName)
        if (-not $entry) { throw "The archive does not contain $EntryName." }
        $stream = $entry.Open()
        try {
            $source = [Drawing.Bitmap]::new($stream)
            try { return [Drawing.Bitmap]::new($source) }
            finally { $source.Dispose() }
        } finally { $stream.Dispose() }
    }

    $entry = $jar.GetEntry('assets/minecraft/textures/font/ascii.png')
    if (-not $entry) { throw 'The client JAR does not contain the Minecraft ASCII font atlas.' }
    $fontStream = $entry.Open()
    $atlas = [Drawing.Bitmap]::new($fontStream)
    if ($atlas.Width -ne 128 -or $atlas.Height -ne 128) {
        throw 'Expected the Minecraft 1.20.1 128-by-128 ASCII atlas.'
    }
    $terminalBackground = Read-Png $ae2 'assets/ae2/textures/guis/background.png'
    $terminal = Read-Png $ae2 'assets/ae2/textures/guis/terminal.png'
    $icon = [Drawing.Image]::FromFile((Join-Path $root 'docs/images/project-icon.png'))
    $canvas = [Drawing.Bitmap]::new(1600, 420)
    $graphics = [Drawing.Graphics]::FromImage($canvas)
    $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
    $graphics.DrawImage($terminalBackground, [Drawing.Rectangle]::new(0, 0, 12, 12), [Drawing.Rectangle]::new(0, 0, 3, 3), [Drawing.GraphicsUnit]::Pixel)
    $graphics.DrawImage($terminalBackground, [Drawing.Rectangle]::new(12, 0, 1576, 12), [Drawing.Rectangle]::new(3, 0, 250, 3), [Drawing.GraphicsUnit]::Pixel)
    $graphics.DrawImage($terminalBackground, [Drawing.Rectangle]::new(1588, 0, 12, 12), [Drawing.Rectangle]::new(253, 0, 3, 3), [Drawing.GraphicsUnit]::Pixel)
    $graphics.DrawImage($terminalBackground, [Drawing.Rectangle]::new(0, 12, 12, 396), [Drawing.Rectangle]::new(0, 3, 3, 250), [Drawing.GraphicsUnit]::Pixel)
    $graphics.DrawImage($terminalBackground, [Drawing.Rectangle]::new(12, 12, 1576, 396), [Drawing.Rectangle]::new(3, 3, 250, 250), [Drawing.GraphicsUnit]::Pixel)
    $graphics.DrawImage($terminalBackground, [Drawing.Rectangle]::new(1588, 12, 12, 396), [Drawing.Rectangle]::new(253, 3, 3, 250), [Drawing.GraphicsUnit]::Pixel)
    $graphics.DrawImage($terminalBackground, [Drawing.Rectangle]::new(0, 408, 12, 12), [Drawing.Rectangle]::new(0, 253, 3, 3), [Drawing.GraphicsUnit]::Pixel)
    $graphics.DrawImage($terminalBackground, [Drawing.Rectangle]::new(12, 408, 1576, 12), [Drawing.Rectangle]::new(3, 253, 250, 3), [Drawing.GraphicsUnit]::Pixel)
    $graphics.DrawImage($terminalBackground, [Drawing.Rectangle]::new(1588, 408, 12, 12), [Drawing.Rectangle]::new(253, 253, 3, 3), [Drawing.GraphicsUnit]::Pixel)
    for ($x = 432; $x -lt 1548; $x += 324) {
        $width = [Math]::Min(324, 1548 - $x)
        $sourceWidth = [int]($width / 2)
        $graphics.DrawImage($terminal, [Drawing.Rectangle]::new($x, 28, $width, 36), [Drawing.Rectangle]::new(7, 18, $sourceWidth, 18), [Drawing.GraphicsUnit]::Pixel)
        $graphics.DrawImage($terminal, [Drawing.Rectangle]::new($x, 356, $width, 36), [Drawing.Rectangle]::new(7, 18, $sourceWidth, 18), [Drawing.GraphicsUnit]::Pixel)
    }
    $graphics.DrawImage($terminal, [Drawing.Rectangle]::new(1548, 76, 36, 106), [Drawing.Rectangle]::new(174, 18, 18, 53), [Drawing.GraphicsUnit]::Pixel)
    $graphics.DrawImage($terminal, [Drawing.Rectangle]::new(1548, 196, 36, 94), [Drawing.Rectangle]::new(174, 78, 18, 47), [Drawing.GraphicsUnit]::Pixel)
    $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.DrawImage($icon, [Drawing.Rectangle]::new(32, 18, 384, 384))

    function Draw-MinecraftText([string]$Text, [int]$X, [int]$Y, [int]$Scale, [string]$Color) {
        $brush = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml($Color))
        try {
            foreach ($character in $Text.ToCharArray()) {
                if ($character -eq ' ') { $X += 4 * $Scale; continue }
                $code = [int]$character
                if ($code -lt 33 -or $code -gt 126) { throw "Unsupported banner character: $character" }
                $cellX = ($code % 16) * 8
                $cellY = [int][Math]::Floor($code / 16) * 8
                $width = 0
                for ($row = 0; $row -lt 8; $row++) {
                    for ($column = 0; $column -lt 8; $column++) {
                        if ($atlas.GetPixel($cellX + $column, $cellY + $row).A -gt 0) {
                            $width = [Math]::Max($width, $column + 1)
                            $graphics.FillRectangle($brush, $X + $column * $Scale, $Y + $row * $Scale, $Scale, $Scale)
                        }
                    }
                }
                $X += ($width + 1) * $Scale
            }
            if ($X -gt 1568) { throw 'Banner text exceeds the right margin.' }
        } finally { $brush.Dispose() }
    }

    Draw-MinecraftText "It's AE2 Crafting Time!" 448 142 8 '#3f3f3f'
    Draw-MinecraftText 'Autocrafting adventure you might want to understand in details' 448 270 3 '#008c95'
    $output = Join-Path $root 'docs/images/readme-banner.png'
    $canvas.Save($output, [Drawing.Imaging.ImageFormat]::Png)
    Write-Host "Rendered Minecraft bitmap lettering to $output"
} finally {
    if ($graphics) { $graphics.Dispose() }
    if ($canvas) { $canvas.Dispose() }
    if ($icon) { $icon.Dispose() }
    if ($atlas) { $atlas.Dispose() }
    if ($fontStream) { $fontStream.Dispose() }
    if ($terminal) { $terminal.Dispose() }
    if ($terminalBackground) { $terminalBackground.Dispose() }
    if ($ae2) { $ae2.Dispose() }
    if ($jar) { $jar.Dispose() }
}
