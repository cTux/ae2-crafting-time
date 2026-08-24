$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
git -C $root config core.hooksPath .githooks
git -C $root config push.autoSetupRemote true
Write-Host "Automatic push and pull-request creation after every commit are enabled."
