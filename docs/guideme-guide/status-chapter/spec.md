# Status Chapter Specification

Issue: [#305](https://github.com/cTux/ae2-crafting-time/issues/305)

Status: planned.

## Goal

Add **Chapter 3: Statuses** so every state currently displayed by AE2 Crafting
Time has a detailed page explaining meaning, location, recovery, clearing, and
limits, with a real screenshot and useful cross-links.

## Pages and display priority

`statuses/index.md` links to these pages in current precedence order:

| Page | Visible state | Required explanation |
| --- | --- | --- |
| `no-space.md` | `NO SPACE` | Stored output cannot return to ME storage; free or add writable storage. |
| `no-provider.md` | `NO PROVIDER` | Scheduled patterns have no connected provider; restore provider/pattern. |
| `no-power.md` | `NO POWER` | Network cannot power next dispatch; increase generation or stored energy. |
| `locked.md` | `LOCKED` | Provider locks/redstone prevent the next batch; satisfy or disable the lock. |
| `input-blocked.md` | `INPUT BLOCKED` | Destination refuses inputs; inspect space, sides, filters, and blocking mode. |
| `no-target.md` | `NO TARGET` | Provider has no usable destination; connect/configure a compatible target. |
| `waiting.md` | `Waiting` | Output is scheduled but its first pattern has not dispatched. |
| `delayed.md` | `DELAYED` | Active work stopped producing output beyond its learned threshold. |
| `no-data-yet.md` | `No data yet` | No usable retained timing sample exists yet. |
| `estimated.md` | TTC such as `~12s` | A usable estimate exists; explain amount, total, confidence, and uncertainty. |

Stored-only NO SPACE wins first. For scheduled work, current reason precedence is
NO PROVIDER, NO POWER, LOCKED, INPUT BLOCKED, NO TARGET, then Waiting, DELAYED,
missing history, and estimate behavior. Pages explain mixed active/scheduled
rows without implying active batches share the scheduled blocking reason.

Features is Chapter 2; Statuses is Chapter 3.

## Page requirements

Every status page includes the exact English/Ukrainian label, affected rows and
precedence, observed condition, ordered recovery checklist, clearing boundary,
false-positive/retention limits, an exact screenshot with alt text/caption, and
links to the landing page, relevant feature, and adjacent/related status.

The landing page provides a compact label-to-page decision path without
inventing machine diagnoses that the mod has not verified.

## Screenshots

Reuse reviewed gallery images for NO SPACE, NO PROVIDER, NO POWER, Waiting,
DELAYED, No data yet, and TTC. Capture focused NO TARGET, INPUT BLOCKED, and
LOCKED images through the existing prepared scenario. Package images locally,
keep native pixels, show the exact label/tooltip, and exclude accounts,
addresses, chat, coordinates, unrelated worlds, and test controls.

## Languages and compatibility

English and Ukrainian use identical paths, positions, image references, and
links; Ukrainian is a complete natural translation. Both themes remain readable.
All ten statuses and the chapter cover all four supported targets. Reconcile the
approved scope first if implementation finds target drift.

This changes guide resources and existing transformations only: no detection,
precedence, thresholds, packets, persistence, config, recipes, or dependencies.

## Non-goals

- Adding/changing statuses, tooltips, recovery actions, or notifications.
- AE2-native-only statuses or machine-specific guesses.
- Video, animation, remote media, custom widgets, or another guide system.

## Acceptance criteria

| ID | Observable result |
| --- | --- |
| S1 | Chapter 3 links to exactly the ten listed states in display priority order. |
| S2 | Every state emitted by the mod maps once; no planned/native-only state is presented as shipped. |
| S3 | Every page satisfies meaning, scope, recovery, clearing, limits, screenshot, and link rules. |
| S4 | Precedence and mixed active/scheduled explanations match current renderer/data. |
| S5 | Introduction, Chapter 1, Features, Statuses, and children have no orphan/dead end. |
| S6 | English/Ukrainian paths, metadata, images, labels, and links match. |
| S7 | Images are packaged, readable, exact, and private-data-free. |
| S8 | All four distributions contain target-correct resources with no guide regressions. |
| S9 | Validation rejects missing page, translation, image, return link, stale label, or wrong position. |
| S10 | Docs/link checks pass and reviewed clients cover both renderer paths. |

See the [technical design](technical-design.md) and
[implementation plan](implementation-plan.md).
