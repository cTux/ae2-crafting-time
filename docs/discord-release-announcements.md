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

## Add the workflow

Create `.github/workflows/discord-release.yml` with this content:

```yaml
name: Announce release in Discord

on:
  release:
    types: [published]

permissions:
  contents: read

jobs:
  announce:
    runs-on: ubuntu-latest
    steps:
      - name: Post release and JAR links
        env:
          DISCORD_WEBHOOK_URL: ${{ secrets.DISCORD_RELEASE_WEBHOOK_URL }}
          GH_TOKEN: ${{ github.token }}
          RELEASE_ID: ${{ github.event.release.id }}
          REPOSITORY: ${{ github.repository }}
          EXPECTED_JARS: 4
        shell: bash
        run: |
          set -euo pipefail

          release_json=
          jar_count=0
          for attempt in {1..12}; do
            release_json=$(gh api "repos/$REPOSITORY/releases/$RELEASE_ID")
            jar_count=$(jq '[.assets[] | select(.name | endswith(".jar"))] | length' <<<"$release_json")
            if (( jar_count == EXPECTED_JARS )); then
              break
            fi
            sleep 5
          done

          test "$jar_count" -eq "$EXPECTED_JARS"
          release_name=$(jq -r '.name // .tag_name' <<<"$release_json")
          release_url=$(jq -r '.html_url' <<<"$release_json")
          jars=$(jq -r '
            [.assets[]
              | select(.name | endswith(".jar"))
              | "[\(.name)](\(.browser_download_url))"]
            | join("\n")
          ' <<<"$release_json")

          printf -v content \
            '**AE2 Crafting Time %s**\n%s\n\n**JAR downloads**\n%s' \
            "$release_name" "$release_url" "$jars"
          jq -n --arg content "$content" '{content: $content}' |
            curl --fail-with-body \
              -H "Content-Type: application/json" \
              --data-binary @- \
              "$DISCORD_WEBHOOK_URL"
```

The workflow does not check out or run repository code. It uses GitHub's
release event and the read-only token that GitHub creates for the job. The
`browser_download_url` value is the public, direct download URL for each release
asset.

GitHub may start the workflow while `gh release create` is still uploading
assets. The workflow waits for all four current matrix rows before posting and
fails instead of sending a partial announcement. Update `EXPECTED_JARS` whenever
the number of rows in `scripts/release-matrix.json` changes.

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
