param(
    [Parameter(Mandatory)][string]$Source,
    [Parameter(Mandatory)][string]$Destination,
    [Parameter(Mandatory)][ValidateRange(0, 32768)][int]$X,
    [Parameter(Mandatory)][ValidateRange(0, 32768)][int]$Y,
    [Parameter(Mandatory)][ValidateRange(1, 32768)][int]$Width,
    [Parameter(Mandatory)][ValidateRange(1, 32768)][int]$Height,
    [ValidateRange(1, 100)][int]$Quality = 90,
    [ValidateRange(0, 8192)][int]$OutputWidth = 0
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$inputPath = (Resolve-Path -LiteralPath $Source).Path
$outputPath = [IO.Path]::GetFullPath($Destination)
$extension = [IO.Path]::GetExtension($outputPath).ToLowerInvariant()
if ($inputPath -eq $outputPath -or $extension -notin @('.jpg', '.png')) {
    throw 'Destination must be a separate .jpg or .png file.'
}
$original = [Drawing.Bitmap]::FromFile($inputPath)
try {
    if ($X + $Width -gt $original.Width -or $Y + $Height -gt $original.Height) {
        throw 'Crop extends outside the source image.'
    }
    $targetWidth = if ($OutputWidth -eq 0) { $Width } else { $OutputWidth }
    $targetHeight = [Math]::Max(1, [int][Math]::Round($Height * $targetWidth / $Width))
    $bitmap = [Drawing.Bitmap]::new($targetWidth, $targetHeight, [Drawing.Imaging.PixelFormat]::Format24bppRgb)
    try {
        $graphics = [Drawing.Graphics]::FromImage($bitmap)
        try {
            $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
            $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
            $graphics.DrawImage($original, [Drawing.Rectangle]::new(0, 0, $targetWidth, $targetHeight), $X, $Y, $Width, $Height, [Drawing.GraphicsUnit]::Pixel)
        } finally { $graphics.Dispose() }
        if ($extension -eq '.png') {
            $bitmap.Save($outputPath, [Drawing.Imaging.ImageFormat]::Png)
        } else {
            $codec = [Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object MimeType -eq 'image/jpeg'
            $parameters = [Drawing.Imaging.EncoderParameters]::new(1)
            try {
                $parameters.Param[0] = [Drawing.Imaging.EncoderParameter]::new([Drawing.Imaging.Encoder]::Quality, [long]$Quality)
                $bitmap.Save($outputPath, $codec, $parameters)
            } finally { $parameters.Dispose() }
        }
    } finally { $bitmap.Dispose() }
} finally { $original.Dispose() }
