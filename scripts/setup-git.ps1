$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
git -C $root config core.hooksPath .githooks
git -C $root config push.autoSetupRemote true
Write-Host "Automatic push after every commit is enabled."
