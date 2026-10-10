# AE2 UI prototype

Build a standalone HTML artifact with several structurally different layout
variants. Use [LOGIC.md](LOGIC.md) when the question concerns state or behavior.

## Process

1. State the design question. Read the existing Minecraft screen and its
   constraints: GUI scale, available space, text density, slot sizes, hover and
   click behavior. Use synthetic representative data and include surrounding
   screen context so the layouts can be judged realistically.
2. Default to three variants, at most five. Vary information hierarchy, layout,
   or primary interaction, rather than just colors. Keep each variant independent
   enough to explore a different structure.
3. Put HTML, CSS, JavaScript, and needed assets in one offline file outside
   production source sets. No framework, application route, build task, backend
   mutation, or authentication is needed. Match relevant AE2 styling and dimensions.
4. Add a distinct floating switcher with previous/next buttons and a readable
   variant label. Wrap around at either end. Use the URL hash to preserve the
   choice on reload, with a safe default for unknown values. Support arrow keys
   except while an input, textarea, select, or editable element has focus.
5. Open the artifact, verify switching and representative layouts, and hand over
   its file link and variant names. Label it as an approximation: HTML cannot
   verify Minecraft font rendering, GUI scaling, startup, or actual mixin behavior.
6. Record the selected variant, useful parts from other variants, and unresolved
   questions. Preserve the delivered artifact. Production implementation needs
   user authorization and the normal development and Minecraft verification workflow.

Keep the variant controls inside this artifact. Do not add shared web components
or a web route to this Java mod. Use stubs for any simulated mutation; no live
player, server, or account data belongs in the prototype.
