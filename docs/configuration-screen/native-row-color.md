# Native row text when decoration is off

Status: draft

Scope: The row-color fallback requested in [#533](https://github.com/cTux/ae2-crafting-time/issues/533).

The requested behavior is defined below. The native color lookup and warning
precedence still need verification in each renderer before implementation.

## Required behavior

When both **Badge background** and **Fast-to-slow colors** are off, ordinary
Crafting Time row text in Crafting Plan and Crafting Status must use the same
foreground as AE2's nearby Available, Crafting, and Scheduled labels. This
includes TTC and compact amounts. Fixed white or the configured Total color
is not a substitute for the active screen/theme color.

| Badge background | Fast-to-slow colors | Ordinary row text |
| --- | --- | --- |
| Off | Off | Native AE2 amount-label foreground |
| Off | On | Existing configured TTC color behavior |
| On | Off | Existing configured fallback behavior |
| On | On | Existing configured TTC color behavior |

Preserve labels, values, interactions, layout, and distinguishable warning
meaning. Warning-specific colors need a separate precedence check; this fallback
must not make a blocked or delayed craft look ordinary. Toggling either option
must preserve saved colors and opacity. Text shadow remains independent.

## Existing seams and remaining design work

At the documentation audit baseline, `ClientOptionsRuntime.ttcColor` returns
`ClientConfig.Color.TOTAL` when TTC colors are disabled. The shared
`CraftingStatusTableRendererMixin` also explicitly styles compact amounts.
Trace the Plan and Status callers and both version-specific drawing paths before
changing a shared helper: CPU cards, chat, tooltips, and optional addon screens
must not accidentally inherit a row-specific policy.

Use the native renderer's actual foreground at the drawing boundary, after
checking whether explicit component styles override it. Do not infer the
foreground from a screenshot or hard-code a theme color. Document the resolved
API seams and warning precedence before promoting this scope to ready-to-implement.
No additional preference, server packet, or persisted format is requested.

The background switch is defined in [#532's specification](badge-background/spec.md).
The [shadow option](text-shadow/spec.md) is separate. Confirm the background
switch's implementation is available before exercising this matrix.

## Acceptance and completion

Compare all four combinations in both screens on Forge/Fabric 1.20.1,
NeoForge 1.21.1, and NeoForge 26.1.2. Include light, dark, and tinted rows,
supported GUI scales, ordinary estimates, compact amounts, and warnings.
Check with server profiling disabled where compact amounts still render.
Record native and mod text together in reviewed screenshots.

Exercise off/on, save/reopen, and custom color/opacity preservation. Add the
smallest policy and renderer checks at the existing test boundaries, then update
the configuration guidance in both GuideME locales and the GitHub wiki after
runtime verification. Link tested commits, current-head CI, and actual captures.
This draft documents the request; it does not claim the fallback is shipped.
