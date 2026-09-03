# Discord release announcements

Use a small GitHub Actions workflow when the Discord announcement must include
direct links to every `.jar`. Discord's built-in GitHub webhook can announce
repository events, but its message format is fixed and does not guarantee that
release assets appear in the message.

The current workflow runs when a GitHub Release is published. It reads the
release assets from GitHub, keeps only `.jar` files, and posts the release page
plus a direct download link for each JAR. It does not yet include the release
body. The following requirement is planned follow-up implementation work.

## Full release description requirement

Requirements updated 2026-09-03:

- **DR-01:** The Discord announcement must contain the same complete description
  as the published GitHub Release, as well as its GitHub link and all JAR links.
  Use that release's `body` verbatim as the source; do not generate a summary,
  reconstruct notes from commits, or repeat common notes per target/JAR.
- **DR-02:** Preserve the description's wording, order, headings, lists, and
  links. Discord rendering may differ from GitHub. Transport-only continuation
  labels or balanced formatting must not remove or rewrite release text.
- **DR-03:** If the announcement exceeds Discord's message limit, send ordered
  continuation messages containing the whole description. Never truncate it or
  replace the remaining text with a link or an attachment-only description.
- **DR-04:** Keep the existing complete-JAR-set check. Report success only after
  every part is confirmed sent. Preserve failure/partial-delivery evidence and
  do not blindly resend already posted parts. Do not turn release text into
  unintended mentions.

## Implementation and acceptance

Update `scripts/announce-discord-release.sh` and its existing
`scripts/test-discord-release-announcement.sh` regression checks. Reuse the
already-fetched release JSON's `body` field. A null/empty GitHub body has no
description to mirror; keep the title and links without invented notes.

Assemble title/link, complete body, then JAR links in that order. Prefer one
message; split longer content into sequential parts within Discord's 2,000
character content limit, preferring paragraph/line boundaries. Preserve Unicode
and split oversized individual lines without dropping text. Account for any
continuation labels in the limit. Disable allowed mentions in every payload.
Use webhook server confirmation (`wait=true`), record returned message IDs per
part, and stop on failure so partial delivery can be inspected before retrying.
These limits and confirmation behavior come from the
[Discord webhook API](https://docs.discord.com/developers/resources/webhook#execute-webhook);
the release text comes from [GitHub's release response](https://docs.github.com/en/rest/releases/releases#get-a-release).

Acceptance: existing shell tests capture payloads with no live posting and prove
short, multiline, empty, exact-limit, over-limit, Unicode, and long-line bodies
remain complete and ordered; each JAR URL appears once; mentions stay disabled;
and a failed middle part stops delivery without reporting success. Compare
reconstructed description text with the release `body`, allowing only transport
formatting. Verify actual Discord content after the next approved real release,
not by publishing a throwaway release. No runtime script changes or Discord
messages are included in this requirements update.

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
2. Confirm the announcements channel contains one complete announcement for the
   release, using ordered continuation messages only when necessary.
3. Compare its JAR links with the assets on the GitHub Release. Every asset whose
   name ends in `.jar` should appear once.
4. Open one link and confirm it downloads the named JAR from GitHub.
5. Compare the complete Discord description with the GitHub Release body;
   every section and change must appear in the same order, without truncation.

If the job fails, first check that the secret name is exact and that the Discord
webhook still exists and still points to the announcements channel.

## Native webhook limitation

Discord's native GitHub webhook is not an accepted replacement for the full-body
and direct-JAR requirements above. Its existing setup uses:

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
