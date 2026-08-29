# Known Issues

This page tracks problems we know about but can't fully fix from inside this mod.
When a bug lives in another mod, we link both our report and the upstream one so
you can follow along.

## 1.20.1 Forge: crafting won't start when the Crafting Tree mod is installed

**Status:** blocked on a third-party mod (Crafting Tree / `ae2ct`)

On Minecraft 1.20.1 with **Forge**, starting any craft fails the moment the
**Crafting Tree** mod (`ae2ct-1.20.1-1.1.1.jar`) is present. AE2 logs
`Failed to start crafting job.` and the chat shows a crash about
`FriendlyByteBuf` (`NoSuchMethodError` on `writeVarInt`, internal name
`m_130130_`).

This breaks the in-tree TTC feature on 1.20.1 Forge, because the Crafting Tree
window never opens at all.

It is **not** an AE2 Crafting Time bug. The crash comes from ae2ct's own mixin
(`ae2ct.mixins.json:AE2CraftingPlanSummary`) applied into
`appeng.menu.me.crafting.CraftingPlanSummary`. The debug log also flags that
mixin as compiled for Java 17 (class version 61), which the Mixin bundled with
that Forge build refuses to apply.

The other three targets are fine:

- 1.20.1 Fabric — works
- 1.21.1 NeoForge — works
- 26.1.2 NeoForge — works

**What you can do:** until ae2ct ships a 1.20.1 Forge build that starts crafts
again, either drop the Crafting Tree mod on that version or use one of the
working loaders above.

**Tracking:**

- Our report: https://github.com/cTux/ae2-crafting-time/issues/96
- Upstream (Crafting Tree): https://github.com/vcwdfca/AE2CraftingTree/issues/1
