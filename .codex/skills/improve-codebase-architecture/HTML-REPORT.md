# HTML report format

Render a single self-contained HTML file in the OS temp directory. Use inline
CSS and SVG; do not load fonts, styles, scripts, or diagrams from a CDN. Mermaid
may help author a diagram, but embed the rendered SVG rather than Mermaid source
requiring a browser library. Plain boxes and arrows are enough for small graphs.

## Scaffold

```html
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Architecture review</title>
  <style>
    body { margin: 0; background: #fafaf9; color: #0f172a; font: 16px/1.5 system-ui, sans-serif; }
    main { max-width: 64rem; margin: auto; padding: 2rem 1rem; }
    article { margin: 2rem 0; padding: 1.5rem; border: 1px solid #cbd5e1; border-radius: .75rem; background: white; }
    .comparison { display: grid; grid-template-columns: repeat(auto-fit, minmax(15rem, 1fr)); gap: 1rem; }
    figure { margin: 0; } svg { width: 100%; height: auto; }
    svg text { fill: #0f172a; font: 14px system-ui, sans-serif; }
    svg rect { fill: #f1f5f9; stroke: #475569; }
    .seam { stroke-dasharray: 4 4; } .leak { stroke: #dc2626; }
  </style>
</head>
<body>
<main>
  <h1>Architecture review</h1>
  <p>Repository, date, and scope of this review.</p>
  <section aria-label="Candidates">
    <!-- Add one article per evidence-backed candidate. -->
  </section>
  <section aria-labelledby="recommendation">
    <h2 id="recommendation">Top recommendation</h2>
    <p>Which candidate to investigate first and why.</p>
  </section>
</main>
</body>
</html>
```

## Candidate

Give each candidate a short title, recommendation strength (Strong, Worth
exploring, or Speculative), affected files, and side-by-side before/after figures.
Describe the problem, proposed change, benefits, and relevant decision conflicts
in plain language. Use established domain terms when a glossary exists.

For dependency graphs, draw labeled boxes and arrows with inline SVG. A minimal
static diagram needs no script:

```html
<figure>
  <figcaption>Before: callers coordinate two modules</figcaption>
  <svg viewBox="0 0 320 90" role="img" aria-label="Caller invokes adapter">
    <rect x="10" y="20" width="110" height="45" rx="5"/>
    <text x="25" y="48">Caller</text>
    <path d="M120 42 H190 M182 35 L190 42 L182 49" fill="none" stroke="#475569"/>
    <rect x="190" y="20" width="120" height="45" rx="5"/>
    <text x="205" y="48">Adapter</text>
  </svg>
</figure>
```

Choose the diagram that explains the specific relationships; do not invent
abstractions merely to produce a more impressive picture. Keep text labels,
captions, and recommendation strength readable without relying on color alone.

## Verify

Open the saved file with network access disabled. Confirm all styling and
diagrams render, text fits at a narrow viewport, and no external request is
needed. Report the absolute artifact path. This report proposes changes; it
does not authorize implementation.
