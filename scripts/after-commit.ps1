$ErrorActionPreference = "Stop"

$root = (git rev-parse --show-toplevel) -join ""
Set-Location $root

$branch = (git symbolic-ref --quiet --short HEAD) -join ""
if ($LASTEXITCODE -ne 0) {
    Write-Warning "Automatic push skipped: detached HEAD."
    exit 0
}

$base = if ($env:CODEX_PR_BASE) { $env:CODEX_PR_BASE } else { "master" }
$dryRun = $env:CODEX_HOOK_DRY_RUN -eq "1"

if (-not $dryRun) {
    git push --set-upstream origin $branch
    if ($LASTEXITCODE -ne 0) { throw "Automatic push failed" }
}

if ($branch -eq $base) { exit 0 }

if (-not $dryRun) {
    if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
        throw "Automatic PR failed: GitHub CLI is not installed"
    }
    $url = (gh pr view $branch --json url --jq .url 2>$null) -join ""
    if ($LASTEXITCODE -eq 0) {
        Write-Host "PR already open: $url"
        exit 0
    }
}

$title = git log --reverse --format=%s "origin/$base..HEAD" | Select-Object -First 1
$commits = (git log --reverse '--format=- %s (`%h`)' "origin/$base..HEAD") -join "`n"
$files = (git diff --name-only "origin/$base...HEAD" | ForEach-Object { "- ``$_``: Changed by this branch." }) -join "`n"
$body = @(
    "## Why?",
    "",
    "Keep changes on ``$branch`` reviewable and ready to merge without a manual PR step.",
    "",
    "## What?",
    "",
    $commits,
    "",
    "## Where?",
    "",
    $files,
    "",
    "## Verification",
    "",
    "- Not run by the automatic PR hook; rely on commit-specific local checks and GitHub checks.",
    "",
    "## Skills used",
    "",
    "- None recorded."
) -join "`n"

if ($dryRun) {
    Write-Host "dry-run PR title: $title"
    Write-Host $body
    exit 0
}

$bodyPath = New-TemporaryFile
try {
    Set-Content -LiteralPath $bodyPath -Value $body -Encoding UTF8
    gh pr create --base $base --head $branch --title $title --body-file $bodyPath
    if ($LASTEXITCODE -ne 0) { throw "Automatic PR creation failed" }
}
finally {
    Remove-Item -LiteralPath $bodyPath -Force -ErrorAction SilentlyContinue
}
