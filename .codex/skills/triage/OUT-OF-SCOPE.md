# Rejected feature decisions

GitHub issues and PRs are the durable rejection record. Search open and closed
items by domain concept and read the relevant discussion before proposing a
new disposition. Match meaning, not only keywords.

For an authorized enhancement rejection:

1. Identify the request, the concrete scope or technical reason for rejection,
   and related prior issues. A temporary lack of time is a deferral, not rejection.
2. Draft a concise decision comment with that reason and links to prior requests.
   Publish it only when the user authorized the comment; triage alone is read-only.
3. Close only when closure is authorized. Preserve existing project labels and
   record `wontfix` as prose, not a label. Read the mutation back.

When a request matches a prior rejection, show the prior issue and ask whether
that decision still applies if the user's instruction has not already settled it.
Link the original decision from an authorized comment on the new issue instead
of duplicating a local database. If the decision changes, record the new reasoning
in the current GitHub discussion and link the superseded decision. Keep history;
reopening older issues requires authorization.

Already implemented features should link the existing behavior, not be recorded
as rejected ideas. Bugs are evaluated against intended behavior rather than this
feature-rejection process. Do not create `.out-of-scope/` files as a second tracker.
