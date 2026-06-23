# ADR-0002: Emit structured citation data; let MultiQC own citation rendering

## Status

Accepted — MultiQC rendering is landing in core (MultiQC/MultiQC#3597); the `nf-core-utils` CSL-JSON output is the remaining work (this branch's follow-up).

## Context

`NfcoreCitationUtils` currently both **assembles** and **formats** citations:

- It parses module `meta.yml` tool entries — now a nested `publication` object (author, year, title, source) per nf-core/modules#12129 — and intersects them with the tools that actually ran (the `versions`/`citations` topic channels).
- It then builds inline citation strings and an HTML bibliography, and `methodsDescriptionText()` interpolates `tool_citations` / `tool_bibliography` into a MultiQC methods-description YAML template that becomes MultiQC custom content.

PR #51 pushed presentation concerns further into these strings: `<a href>` DOI links and italic `<em>et al.</em>`. Review flagged that the _same_ strings are also written to a plain-text `auto_citations.txt`, where the markup shows up as literal tags. (See the PR #51 review thread.)

This conflates two responsibilities:

- **Assembly** — turning "which tools ran" + `meta.yml` into structured citation records. This needs Nextflow runtime, topic channels, and module access, so it belongs in this plugin.
- **Presentation** — Harvard short form, italics, hyperlinks, bibliography HTML. This is a reporting concern.

Everywhere else in the nf-core stack, **MultiQC owns reporting and rendering**. Citations are the odd case where the Nextflow plugin renders HTML itself.

**Consumers today:**

1. The MultiQC methods-description section (HTML, rendered in the MultiQC report) — the production path.
2. Standalone `auto_citations.txt` / `auto_bibliography.html` — validation/demo artifacts, not a separate product report.

So there is no hidden "other report": the real target is MultiQC. The friction is that formatting lives on the wrong side of that boundary, and a couple of test artifacts reuse the HTML strings as plain text.

**MultiQC plugin model** (per the MultiQC development docs) — setuptools entry points: `multiqc.modules.v1`, `multiqc.templates.v1`, `multiqc.cli_options.v1`, `multiqc.hooks.v1` (hooks include `execution_start`, `config_loaded`, `before_modules`, `after_modules`, `execution_finish`). A custom module can discover a structured input file and render a report section.

ADR-0001 already calls for explicit report inputs and deeper reporting seams; ADR-0000 values stability and backward compatibility.

## Decision

(Proposed) Treat **structured citation data as the output contract of `nf-core-utils`**, and move citation _rendering_ to MultiQC:

1. `nf-core-utils` collects citations for the tools that actually ran and emits a **structured artifact** into the pipeline outputs: **CSL-JSON is canonical**, with an **optional BibTeX `.bib`** export. It stops at data — no HTML assembly.
2. **MultiQC core's native `citations` module** (MultiQC/MultiQC#3597) discovers that artifact (`*.csl.json`, optional `*.citations.bib`) and renders the inline citations, the bibliography, and the methods/citations section — Harvard short form, italic _et al._, DOI/homepage links. This is where the "fancy" formatting lives. No separate plugin or repo is needed — the renderer ships in MultiQC itself.
3. The **structured artifact is the only citation deliverable from `nf-core-utils`.** The plugin keeps no text/HTML renderer — we are not splitting presentation across two layers. Non-MultiQC consumers use the CSL-JSON / `.bib` directly (both are real, tool-readable formats).
4. Backward compatibility (ADR-0000 #3): keep `methodsDescriptionText()`, `toolCitationText()`, and `toolBibliographyText()` working until the MultiQC `citations` module ships in a release, then cut over cleanly and deprecate the HTML-string outputs. The cutover is preferred over a long-lived dual path.

## Rationale

- **Right altitude / single responsibility.** Assembly needs module + runtime access (here); presentation is a reporting concern (MultiQC). Splitting at the structured-data boundary removes the HTML-in-text mismatch entirely — the class of bug raised in PR #51 cannot recur, because the plugin no longer produces HTML strings.
- **Consistency.** Matches how MultiQC owns the rest of nf-core reporting, instead of hand-assembling HTML in Groovy.
- **Reuse.** CSL-JSON / `.bib` is consumable by reference managers, manuscript tooling, and any future renderer; citation style can change without touching the pipeline.
- **Builds on the `publication` schema work.** That object is exactly the structured record a `.bib` / CSL-JSON needs (author, year, title, source, doi).
- **Aligns with ADR-0001** (explicit report inputs, deep reporting seam).

## Consequences

Positive:

- Removes presentation logic — and the HTML-in-plain-text class of bugs — from the Nextflow plugin.
- Produces a reusable, standard citation artifact.
- Citation style/branding becomes a MultiQC concern, customizable per the MultiQC docs.

Tradeoffs / costs:

- Requires a MultiQC version that includes the `citations` module (MultiQC/MultiQC#3597); the optional `.bib` path needs the `multiqc[citations]` extra (`bibtexparser`).
- A temporary dual path during migration (compat HTML functions + new structured output).

## Resolved decisions

- **Canonical format:** CSL-JSON canonical, with an optional `.bib` export.
- **Renderer:** MultiQC core's `citations` module (MultiQC/MultiQC#3597) — not a separate plugin or repo. The rendering (Harvard short form, italic _et al._, methods sentence, bibliography with DOI/homepage links) lives there.
- **File contract (pinned by #3597):** `*.csl.json` — a JSON array of CSL items, each with `custom.tool` and `custom.version`; optional `*.citations.bib` (needs `multiqc[citations]`). CSL-JSON wins when a tool appears in both. Tool names act as MultiQC sample names.
- **Non-MultiQC pipelines:** the structured file is the only deliverable — `nf-core-utils` keeps no text/HTML renderer (avoid mixed-up concerns across layers).
- **Migration:** keep the existing HTML-string functions working until the MultiQC `citations` module ships in a release, then cut over cleanly.

## Sequencing

1. Land the PR #51 fix (the two-branch bibliography formatter) as an interim step so `meta.yml` `publication` parsing is correct.
2. ~~Bootstrap a MultiQC citations plugin repo~~ — superseded: the renderer is landing in MultiQC core (MultiQC/MultiQC#3597), no new repo needed.
3. Open a separate `nf-core-utils` refactor PR that emits `*.csl.json` (+ optional `*.citations.bib`) matching the #3597 contract, and deprecates the HTML-string functions once a MultiQC release with the module is available.

## Remaining design questions (for the `nf-core-utils` refactor)

- **Author-name handling:** populate CSL `author` so MultiQC's short form resolves correctly — confirm whether #3597 expects parsed `family`/`given` name objects or accepts `literal`, then parse `publication.author` free strings (e.g. `"Li H, Handsaker B, et al."`) accordingly. The Harvard short form (`Andrews (2010)`, `Danecek et al. (2021)`) needs at least the first surname.
- **DOI-only / Zenodo tools:** ensure they still emit a CSL item keyed by tool so they are never anonymous (mirror PR #52's two-branch behaviour).
- **MultiQC version floor:** pin the minimum MultiQC version that includes the `citations` module.

## Alternatives considered

### Keep formatting in `nf-core-utils` (status quo + PR #51)

Lowest immediate effort and works for the MultiQC HTML path today, but keeps presentation on the wrong side of the boundary, forces ongoing HTML/plain-text reconciliation, and diverges from "MultiQC owns reporting."

### Emit Markdown instead of HTML and rely on MultiQC markdown rendering

A smaller change, but the methods-description fragment is treated as HTML, MultiQC has no citation/style engine, and it does not fix the plain-text artifact.

### Structured output but render inside `nf-core-utils` (no MultiQC plugin)

Separates assembly from formatting internally, but still hand-rolls citation styling in Groovy and does not use MultiQC's reporting layer.
