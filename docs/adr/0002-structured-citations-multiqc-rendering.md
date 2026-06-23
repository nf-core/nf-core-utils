# ADR-0002: Emit structured citation data; let MultiQC own citation rendering

## Status

Proposed

## Context

`NfcoreCitationUtils` currently both **assembles** and **formats** citations:

- It parses module `meta.yml` tool entries — now a nested `publication` object (author, year, title, source) per nf-core/modules#12129 — and intersects them with the tools that actually ran (the `versions`/`citations` topic channels).
- It then builds inline citation strings and an HTML bibliography, and `methodsDescriptionText()` interpolates `tool_citations` / `tool_bibliography` into a MultiQC methods-description YAML template that becomes MultiQC custom content.

PR #51 pushed presentation concerns further into these strings: `<a href>` DOI links and italic `<em>et al.</em>`. Review flagged that the *same* strings are also written to a plain-text `auto_citations.txt`, where the markup shows up as literal tags. (See the PR #51 review thread.)

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

(Proposed) Treat **structured citation data as the output contract of `nf-core-utils`**, and move citation *rendering* to MultiQC:

1. `nf-core-utils` collects citations for the tools that actually ran and emits a **structured artifact** (CSL-JSON as the canonical form; optional BibTeX `.bib` export) into the pipeline outputs. It stops at data — no HTML assembly.
2. A **MultiQC citations plugin** (`multiqc.modules.v1`) discovers that artifact via a search pattern and renders the inline citations, the bibliography, and the methods/citations section — Harvard short form, italics, DOI/homepage links — using a citation style (CSL). This is where the "fancy" formatting lives.
3. Non-MultiQC consumers get the structured artifact directly (a `.bib` / CSL-JSON is a real, tool-readable deliverable). Optionally `nf-core-utils` keeps a thin plain-text renderer for a `CITATIONS.txt`, but the HTML/styling logic leaves the Nextflow plugin.
4. Backward compatibility (ADR-0000 #3): keep `methodsDescriptionText()`, `toolCitationText()`, and `toolBibliographyText()` working during migration; deprecate the HTML-string outputs once the MultiQC plugin path is available.

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

- A new MultiQC plugin is a separate Python package: repository, pip distribution, versioning, install into pipeline environments/containers, and nf-core/tools + pipeline-template integration. This is cross-project effort.
- A temporary dual path during migration (compat HTML functions + new structured output).
- Requires choosing a citation-style mechanism (a CSL processor in the MultiQC plugin).

## Open questions

- **Canonical format:** CSL-JSON (best for programmatic styling) vs BibTeX `.bib` (familiar) — or emit both? (Leaning CSL-JSON canonical + optional `.bib`.)
- **Plugin home:** a dedicated MultiQC plugin vs extending an existing nf-core MultiQC module/template, and which repo it lives in.
- **Non-MultiQC pipelines:** does `nf-core-utils` keep a minimal text/HTML renderer, or is the structured file the only non-MultiQC deliverable?
- **Migration/deprecation timeline** for `methodsDescriptionText` and friends.
- **Short term:** do we still merge PR #51 (HTML in strings) as an interim step, or hold it given this direction?

## Alternatives considered

### Keep formatting in `nf-core-utils` (status quo + PR #51)

Lowest immediate effort and works for the MultiQC HTML path today, but keeps presentation on the wrong side of the boundary, forces ongoing HTML/plain-text reconciliation, and diverges from "MultiQC owns reporting."

### Emit Markdown instead of HTML and rely on MultiQC markdown rendering

A smaller change, but the methods-description fragment is treated as HTML, MultiQC has no citation/style engine, and it does not fix the plain-text artifact.

### Structured output but render inside `nf-core-utils` (no MultiQC plugin)

Separates assembly from formatting internally, but still hand-rolls citation styling in Groovy and does not use MultiQC's reporting layer.
