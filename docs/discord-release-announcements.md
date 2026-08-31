# Discord release announcements

Use a small GitHub Actions workflow when the Discord announcement must include
direct links to every `.jar`. Discord's built-in GitHub webhook can announce
repository events, but its message format is fixed and does not guarantee that
release assets appear in the message.

The workflow below runs once when a GitHub Release is published. It reads the
release assets from GitHub, keeps only `.jar` files, and posts the release page
plus a direct download link for each JAR.

## Create the Discord webhook

You need permission to manage webhooks in the Discord server.

1. Open **Server Settings**, then **Integrations** and **Webhooks**.
2. Create a webhook named `AE2 Crafting Time Releases`.
3. Choose the announcements channel and copy the webhook URL.

Treat that URL like a password. Do not put it in this repository, a pull
request, an issue, or a chat message. If it leaks, delete the webhook and create
a new one.

Do not add `/github` to this URL. That suffix is only for Discord's built-in
GitHub message formatter; this workflow sends a normal Discord message.

## Save the URL in GitHub

In the GitHub repository, open **Settings**, **Secrets and variables**,
**Actions**, then create a repository secret:

```text
Name: DISCORD_RELEASE_WEBHOOK_URL
Value: <the copied Discord webhook URL>
```

Repository secrets are not available to pull requests from forks. That does
not affect this workflow because it only runs for a published release.

## How it runs

The tracked `.github/workflows/discord-release.yml` workflow runs on GitHub's
`release: published` event. It checks out the trusted default branch and runs
`scripts/announce-discord-release.sh` with GitHub's read-only job token. The
script uses each asset's public `browser_download_url` value for the direct
download link.

GitHub may start the workflow while `gh release create` is still uploading
assets. The script reads the expected JAR count from
`scripts/release-matrix.json`, waits for the complete set, and fails instead of
sending a partial announcement.

## Verify the next release

Do not publish a throwaway release just to test Discord. After the next approved
real release:

1. Open the **Announce release in Discord** run in the GitHub **Actions** tab and
   confirm it passed.
2. Confirm the announcements channel contains one message for the release.
3. Compare its JAR links with the assets on the GitHub Release. Every asset whose
   name ends in `.jar` should appear once.
4. Open one link and confirm it downloads the named JAR from GitHub.

If the job fails, first check that the secret name is exact and that the Discord
webhook still exists and still points to the announcements channel.

## Native webhook alternative

If direct JAR links are not required, the no-workflow option is Discord's native
GitHub webhook:

1. Copy the Discord webhook URL and append `/github`.
2. Add it under GitHub **Settings**, **Webhooks**, **Add webhook**.
3. Select `application/json`, choose individual events, and enable **Releases**.

Do not configure both options for release events, or each release will be
announced twice.

## References

- [Discord: Intro to Webhooks](https://support.discord.com/hc/en-us/articles/228383668-Intro-to-Webhooks)
- [GitHub: Events that trigger workflows](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#release)
- [GitHub: REST API endpoints for release assets](https://docs.github.com/en/rest/releases/assets)
- [GitHub: Creating webhooks](https://docs.github.com/en/webhooks/using-webhooks/creating-webhooks)
