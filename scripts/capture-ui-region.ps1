param(
    [Parameter(Mandatory)][string]$OutputPath,
    [Parameter(Mandatory)][int]$X,
    [Parameter(Mandatory)][int]$Y,
    [Parameter(Mandatory)][ValidateRange(1, 16384)][int]$Width,
    [Parameter(Mandatory)][ValidateRange(1, 16384)][int]$Height
)

# Run inside CodexVM after inspecting its current framebuffer. Coordinates are pixels.
$ErrorActionPreference = "Stop"
Add-Type 'using System.Runtime.InteropServices; public static class CaptureDpi { [DllImport("user32.dll")] public static extern bool SetProcessDPIAware(); }'
[CaptureDpi]::SetProcessDPIAware() | Out-Null
Add-Type -AssemblyName System.Drawing, System.Windows.Forms
$region = [Drawing.Rectangle]::new($X, $Y, $Width, $Height)
if (-not [Windows.Forms.SystemInformation]::VirtualScreen.Contains($region)) {
    throw "Capture region is outside the desktop"
}
if ([IO.Path]::GetExtension($OutputPath) -ne '.png') { throw "Output must be a PNG" }
$bitmap = [Drawing.Bitmap]::new($Width, $Height)
$graphics = [Drawing.Graphics]::FromImage($bitmap)
try {
    $graphics.CopyFromScreen($region.Location, [Drawing.Point]::Empty, $region.Size)
    $bitmap.Save([IO.Path]::GetFullPath($OutputPath), [Drawing.Imaging.ImageFormat]::Png)
} finally {
    $graphics.Dispose()
    $bitmap.Dispose()
}
