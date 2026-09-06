param(
    [Parameter(Mandatory = $true)]
    [string]$MinecraftClientJar
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem
$root = Split-Path -Parent $PSScriptRoot
$jar = [IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $MinecraftClientJar))
$atlas = $null
$icon = $null
$canvas = $null
$graphics = $null
$fontStream = $null
try {
    $entry = $jar.GetEntry('assets/minecraft/textures/font/ascii.png')
    if (-not $entry) { throw 'The client JAR does not contain the Minecraft ASCII font atlas.' }
    $fontStream = $entry.Open()
    $atlas = [Drawing.Bitmap]::new($fontStream)
    if ($atlas.Width -ne 128 -or $atlas.Height -ne 128) {
        throw 'Expected the Minecraft 1.20.1 128-by-128 ASCII atlas.'
    }
    $icon = [Drawing.Image]::FromFile((Join-Path $root 'docs/images/project-icon.png'))
    $canvas = [Drawing.Bitmap]::new(1600, 420)
    $graphics = [Drawing.Graphics]::FromImage($canvas)
    $graphics.Clear([Drawing.ColorTranslator]::FromHtml('#11191f'))
    $grid = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#151f26'))
    try {
        for ($y = 0; $y -lt 420; $y += 24) {
            for ($x = 0; $x -lt 1600; $x += 24) {
                $graphics.FillRectangle($grid, $x + 2, $y + 2, 20, 20)
            }
        }
    } finally { $grid.Dispose() }
    $accent = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#203d48'))
    try {
        foreach ($point in @(@(24,120), @(48,96), @(72,144), @(24,264), @(72,288), @(1512,72), @(1536,48), @(1560,96), @(1536,312))) {
            $graphics.FillRectangle($accent, $point[0] + 2, $point[1] + 2, 20, 20)
        }
    } finally { $accent.Dispose() }
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
    $jar.Dispose()
}
