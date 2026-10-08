# Compact hover numbers: technical design

See the [specification](spec.md) for canonical status, behavior and holds.

## Current evidence

Source inspected at `fd876649f4f38fde308e47538b7281db16b2f71f`:

- `shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/TtcText.java`:
  `statsLines` sends both throughput values through `rate`, currently
  `String.format(Locale.ROOT, "%.2f", value)`. `rate` also serves slowdown and
  accuracy text, so globally changing it would expand this feature accidentally.
- The shared `mixin/CraftConfirmTableRendererMixin.java` and
  `mixin/CraftingStatusTableRendererMixin.java` call `TtcText.statsLines` under
  the detailed-tooltip option. These are the current production callers.
- `StatsChatMessages.show` sends `StatsChatC2S`; `StatsChatServer.handle` resolves
  the current server network and publishes a summary. `details` currently emits
  only the per-second rate with two decimals. Its private `decimal` also serves
  accuracy text and must not be changed globally.
- `TtcText.compactMessages` has a separate client details renderer. No production
  caller was found by the repository search; retain its API and align its
  throughput contract so tests or later callers cannot silently use a different
  meaning of full values.
- `OptionFeature`, `FeatureOptions`, `ClientConfigFile` and
  `ClientOptionsRuntime` provide the existing generic client-option path.
- `PlanAmountLines` and renderer calls to `AEKey.formatAmount(..., SLOT)` own
  compact row quantities. They do not own the throughput line and are not changed.

## Ownership and flow

Retain server profiler -> snapshot -> client cache -> `statsLines`. Add a small
Minecraft-free `ThroughputNumbers` helper in
`shared/src/main/java/com/ctux/ae2craftingtime/core/` with `compact(double)` and
`full(double)`. Both reject nonpositive/nonfinite input as `?`.

Use `BigDecimal.valueOf(value)` for decimal display only. It represents the
canonical decimal value of the existing double; do not convert integral row
amounts to double and do not change profiling arithmetic. In `compact`, choose
the base-1000 tier, divide by its decimal power, round to two places with
`HALF_UP`, then promote on a rounded 1000 before stripping zeroes. Use explicit
suffixes from the spec. For the scientific fallback, round the original decimal
to three significant digits with `HALF_UP`; render a normalized mantissa and
base-10 exponent (`~1e21`, no redundant `+` or trailing zeroes). This path must
handle all finite positive doubles without overflow from an integer cast.

For `full`, use `stripTrailingZeros().toPlainString()`. Even the smallest positive
or largest finite double has bounded decimal length; test these boundaries.
No new dependency, cache, parser or packet is needed. Formatting is display-only.

In `TtcText.statsLines`, select compact or existing `rate` for the two throughput
arguments using the new option. Do not route samples, slowdown, percentages,
accuracy or time estimates through the helper. Keep existing order and styling.

In `StatsChatServer.details` and `TtcText.normalizedDetails`, format both rates
with `full`, regardless of the hover option. Update the two shared chat detail
translation templates (`chat.details` and `chat.details.rate`) to include
per-tick and per-second values, updating both callers and both locales together.
Retain the exact requested long amount in the summary and all existing suffix
details (used samples, confidence, accuracy). Do not add a client option to the
request, change the broadcast recipients or bypass server context/cooldown.

## Configuration and text

Add `COMPACT_HOVER_NUMBERS(Owner.CLIENT, Group.DISPLAYS, "compactHoverNumbers")`
to `OptionFeature`. The existing default-enabled feature model supplies on;
verify generic serialization, screen enumeration, reset and missing-key behavior.
Do not alias `compactStatusAmounts`, whose separate default stays unchanged.

Update English and Ukrainian option label/description, control hint and changed
chat placeholders in `shared/src/main/resources/assets/ae2craftingtime/lang/`.
The suffix policy is language-independent; unit labels remain localized.
Use the established Client options descriptions and generic screen rather than
adding a dedicated settings page.

## Failure and compatibility boundaries

No new mixin or injection target is required. All four modules already consume
the shared formatter and chat implementation. Keep optional mods optional and
do not add throughput to their UI. Item/fluid/chemical/mana conversions still
come from `AeKeyAmounts` and `ProfileAmounts` before formatting.

Invalid data renders unknown, never zero, a negative rate, NaN or infinity.
Missing stats and disabled detail/chat settings follow existing paths. Formatting
cannot repair overflow upstream; #616 is a conditional end-to-end test dependency,
not a reason to broaden this presentation patch. No save or wire migration occurs.

## Alternatives rejected

- Changing the common `rate`/`decimal` methods also changes unrelated diagnostics.
- Using AE2's integral resource SLOT formatter for floating throughput loses
  fractional rate precision and entangles resource amount units with rate units.
- Abbreviating chat or retaining two-decimal rounding fails the full-value request.
- Rewriting native quantity components adds independent scope and conflicts with
  the existing full-tooltip contract; the approved scope leaves it unchanged.

## Documentation delivery

At implementation, update the parent controls spec, profiling detail description,
and English/Ukrainian GuideME `features/learning-throughput.md`,
`features/details-and-reset.md` and `features/configuration.md`. Prepare matching
GitHub wiki updates using the actual existing wiki page names. The
[player-documentation draft](player-documentation.md) contains the new copy.
Do not publish it as shipped behavior before implementation. Record reviewed
hover/chat screenshots on each target; replace guide images only when they show
the implemented feature clearly and contain no private information.
