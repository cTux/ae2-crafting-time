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
$background = $null
$ringSideHorizontal = $null
$ringSideVertical = $null
$ringCorner = $null
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
    $background = Read-Png $ae2 'assets/ae2/textures/block/sky_stone_small_brick.png'
    $ringSideHorizontal = Read-Png $ae2 'assets/ae2/textures/block/crafting/ring_side_hor.png'
    $ringSideVertical = Read-Png $ae2 'assets/ae2/textures/block/crafting/ring_side_ver.png'
    $ringCorner = Read-Png $ae2 'assets/ae2/textures/block/crafting/ring_corner.png'
    $icon = [Drawing.Image]::FromFile((Join-Path $root 'docs/images/project-icon.png'))
    $canvas = [Drawing.Bitmap]::new(1600, 420)
    $graphics = [Drawing.Graphics]::FromImage($canvas)
    $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
    for ($y = 0; $y -lt 420; $y += 64) {
        for ($x = 0; $x -lt 1600; $x += 64) {
            $graphics.DrawImage($background, [Drawing.Rectangle]::new($x, $y, 64, 64))
        }
    }
    $shade = [Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(150, 5, 14, 18))
    try {
        $graphics.FillRectangle($shade, 0, 0, 1600, 420)
    } finally { $shade.Dispose() }
    for ($x = 32; $x -lt 1568; $x += 32) {
        $graphics.DrawImage($ringSideHorizontal, [Drawing.Rectangle]::new($x, 0, 32, 32))
        $graphics.DrawImage($ringSideHorizontal, [Drawing.Rectangle]::new($x, 388, 32, 32))
    }
    for ($y = 32; $y -lt 388; $y += 32) {
        $graphics.DrawImage($ringSideVertical, [Drawing.Rectangle]::new(0, $y, 32, 32))
        $graphics.DrawImage($ringSideVertical, [Drawing.Rectangle]::new(1568, $y, 32, 32))
    }
    foreach ($point in @(@(0,0), @(1568,0), @(0,388), @(1568,388))) {
        $graphics.DrawImage($ringCorner, [Drawing.Rectangle]::new($point[0], $point[1], 32, 32))
    }
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

    Draw-MinecraftText "It's AE2 Crafting Time!" 452 146 8 '#080e12'
    Draw-MinecraftText "It's AE2 Crafting Time!" 448 142 8 '#f0f6fc'
    Draw-MinecraftText 'Autocrafting adventure you might want to understand in details' 451 273 3 '#080e12'
    Draw-MinecraftText 'Autocrafting adventure you might want to understand in details' 448 270 3 '#48e6ee'
    $output = Join-Path $root 'docs/images/readme-banner.png'
    $canvas.Save($output, [Drawing.Imaging.ImageFormat]::Png)
    Write-Host "Rendered Minecraft bitmap lettering to $output"
} finally {
    if ($graphics) { $graphics.Dispose() }
    if ($canvas) { $canvas.Dispose() }
    if ($icon) { $icon.Dispose() }
    if ($atlas) { $atlas.Dispose() }
    if ($fontStream) { $fontStream.Dispose() }
    if ($ringCorner) { $ringCorner.Dispose() }
    if ($ringSideVertical) { $ringSideVertical.Dispose() }
    if ($ringSideHorizontal) { $ringSideHorizontal.Dispose() }
    if ($background) { $background.Dispose() }
    if ($ae2) { $ae2.Dispose() }
    if ($jar) { $jar.Dispose() }
}
