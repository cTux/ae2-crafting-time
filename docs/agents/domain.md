# Domain documents

Read [architecture](../architecture.md) and the relevant feature directory first.
The existing `docs/<feature>/spec.md`, `technical-design.md`, and
`implementation-plan.md` remain the canonical planning documents. Follow the
[documentation lifecycle](../documentation-status.md).

Use `GLOSSARY.md` at the repository root if one exists. Create it only when a
resolved domain term needs a shared definition; keep it a glossary, not a spec.
Check terms such as waiting, delayed, missing provider, completion interval, and
job estimate against the current feature definitions instead of inventing aliases.

Record feature decisions in their technical-design document. Use `docs/adr/`
only for a durable cross-cutting decision without an existing home. Link rather
than duplicate decisions. Loader source sets are not separate domain contexts.
