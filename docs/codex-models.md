# Codex model routing

Use one `model/*` label on every issue to record the recommended Codex worker.
Choose the least expensive model that can complete the whole issue safely; do
not split a tightly coupled task just to justify a smaller model.

| Label | Best fit | Choose it when | Move up when |
| --- | --- | --- | --- |
| `model/gpt-6-luna` | Narrow fixes and routine implementation or documentation | The issue follows existing patterns or established architecture, from copy changes and small tests to bounded work needing normal investigation, several edits, or focused verification. This is the default for routine repository work. | The task needs repository-wide tracing, non-trivial design judgment, or coordinated changes across layers, or is compatibility-sensitive, security-sensitive, or likely to expose ambiguous behavior. |
| `model/gpt-6-sol` | Complex professional work | The issue needs substantial judgment, broad code or documentation changes, multiple compatibility targets, or a demanding review and verification pass. | Failure would be hard to detect or costly, the design spans several systems, or the task requires the strongest end-to-end reasoning. |
| `model/gpt-6-astra` | Hardest end-to-end work | The issue combines deep reasoning with complex coding, research, computer use, security, migration, or many coupled systems. Use it when correctness depends on resolving ambiguity across the full workflow. | This is the highest tier. Reduce scope or clarify the issue if it still cannot be executed safely. |

The model label is independent of `effort/*` and `priority/*`. A long mechanical
task can still fit Luna, while a small but security-sensitive change can need
Astra. Reassess the label when the issue scope or acceptance criteria change.

This repository uses GPT-6 Luna for narrow and routine work, GPT-6 Sol for
complex work, and GPT-6 Astra for the hardest tasks. Check the
[official OpenAI model guidance](https://developers.openai.com/api/docs/models)
for current model capabilities and availability.
