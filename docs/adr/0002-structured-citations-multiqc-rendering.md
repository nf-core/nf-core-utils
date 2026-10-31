# ADR-0002: Emit structured citation data; let MultiQC own citation rendering

## Status

Proposed — not implemented by this ADR-only PR. As checked on 2026-10-31, [MultiQC/MultiQC#3597](https://github.com/MultiQC/MultiQC/pull/3597) is open and unmerged. Its current implementation is CSL-JSON only; neither its merge nor a released renderer is established by this proposal.

`nf-core-utils` continues to provide its existing citation and bibliography functions. This ADR does not introduce structured-file output, change public behavior, or require a new MultiQC version for the current plugin.

## Context

`NfcoreCitationUtils` currently both **assembles** and **formats** citations:

- It parses module `meta.yml` tool entries, supporting a nested `publication` object (author, year, title, source) and legacy flat fields. The versions-driven path intersects metadata with tools in the `versions` topic; callers can also supply explicit extra tools. Other paths consume citation topic data or metadata files directly.
- It then builds inline citation strings and an HTML bibliography, and `methodsDescriptionText()` interpolates `tool_citations` / `tool_bibliography` into a MultiQC methods-description YAML template that becomes MultiQC custom content.

PR #51 added Harvard short citations with `<a href>` DOI/homepage links and italic `<em>et al.</em>`. Its review flagged HTML being written to a plain-text `auto_citations.txt`. That PR is now merged, and the on-the-fly validation writes `auto_citations.html`; the topic-channel demo still writes `auto_citations.txt`. The filename correction does not itself move presentation out of the plugin.

This conflates two responsibilities:

- **Assembly** — turning "which tools ran" + `meta.yml` into structured citation records. This needs Nextflow runtime, topic channels, and module access, so it belongs in this plugin.
- **Presentation** — Harvard short form, italics, hyperlinks, bibliography HTML. This is a reporting concern.

MultiQC already renders pipeline reporting sections. Moving citation presentation there would reduce the HTML formatting owned by this plugin; this is not a claim that MultiQC owns every report in the nf-core stack.

**Consumers today:**

1. The MultiQC methods-description section (HTML, rendered through custom content).
2. Standalone `auto_citations.html` / `auto_bibliography.html` and the topic-channel demo's `auto_citations.txt`.
3. Pipeline callers of the public citation/reporting functions. Repository examples do not establish that there are no external text/HTML consumers.

Tool coverage also remains a separate concern: PR #90 records the registry MultiQC tool's missing versions-topic emission as pending behavior (issue #89). A new serialization format would not by itself discover tools absent from that topic.

ADR-0001 already calls for explicit report inputs and deeper reporting seams; ADR-0000 values stability and backward compatibility.

## Proposed decision

Introduce **structured citation data as an output contract of `nf-core-utils`**, and move citation _rendering_ to MultiQC in a separately reviewed implementation:

1. `nf-core-utils` would collect runtime tool/version data and citation metadata and emit **CSL-JSON** into pipeline outputs. BibTeX export is outside this proposal's required contract.
2. The proposed native MultiQC `citations` module would discover `*.csl.json` and render Harvard short citations, italic _et al._, DOI/homepage links, a methods sentence, and a bibliography. Targeting core avoids requiring a separate plugin repo, but depends on upstream acceptance and release.
3. Structured data is the intended long-term boundary, not the only deliverable today. Existing text/HTML functions and report-map fields remain compatible during migration, including methods-description templating.
4. Backward compatibility (ADR-0000 #3): a MultiQC release alone does not authorize removing public functions. Any future deprecation or removal needs a separate compatibility review, documented migration and version policy, and tests for existing consumers. No clean cutover is scheduled by this ADR.

## Rationale

- **Single responsibility.** Assembly needs module + runtime access (here); presentation belongs with the report renderer. A structured boundary would avoid requiring new consumers to reuse HTML strings as plain text.
- **Consistency.** Uses MultiQC's reporting layer instead of adding more citation presentation in Groovy.
- **Reuse.** CSL-JSON is consumable by reference managers, manuscript tooling, and other renderers.
- **Builds on the `publication` schema work.** Existing metadata supplies bibliographic fields, but author names and CSL field mappings still need deliberate conversion; DOI remains tool-level metadata.
- **Aligns with ADR-0001** (explicit report inputs, deep reporting seam).

## Expected consequences if implemented

Positive:

- Allows presentation logic to move out of the Nextflow plugin after a compatible migration.
- Produces a reusable, standard citation artifact.
- Makes citation rendering a MultiQC concern. The upstream proposal implements Harvard rendering, not a general configurable CSL style engine.

Tradeoffs / costs:

- Structured-file rendering requires a released MultiQC version with the module; that minimum version is not yet established.
- Existing HTML functions and new structured output would coexist during migration.
- Author conversion, tool identity/version conflicts, output wiring, and tools missing from runtime topics require implementation and integration tests.

## Upstream contract observed, not pinned

The following describes [#3597 at revision 506f802](https://github.com/MultiQC/MultiQC/pull/3597/commits/506f80235f3484d485b4429061ab7889a4abc4c8), inspected on 2026-10-31. The PR body still mentions BibTeX, but the current source and review discussion supersede that description. An open PR is not a stable released contract.

- **Discovery:** `*.csl.json` only. BibTeX input, `*.citations.bib`, and the `multiqc[citations]` extra were removed from this PR; there is no CSL-versus-BibTeX precedence rule.
- **Shape:** a JSON array of CSL items is canonical; the parser also accepts a single item or a mapping of IDs to items.
- **Tool identity/version:** `custom.tool` identifies the tool, falling back to the item's `id`; at least one is required. `custom.version` is optional. Tools act as sample names for `--ignore-samples`. The module keeps the first record found for a tool across files and sorts tool names case-insensitively for rendering.
- **Bibliographic fields:** `title`, `author`, `issued.date-parts`, `container-title`, `DOI`, and `URL`. Authors may be structured `family`/`given` or `literal` objects, or a plain string; literal/string surname extraction uses a surname-first heuristic. An author and year are needed for the Harvard short form; otherwise the renderer leads with the tool name and preserves DOI/homepage links.
- **Review still open:** upstream feedback asks to consolidate the CSL parser and clarify the renderer's provenance/license. Neither acceptance nor release can be inferred from the implementation's presence on the PR branch.

## Sequencing

1. Preserve the finished citation formatting and compatibility work from merged PR #51 and the pending registry-coverage specifications from merged PR #90. The interim bibliography fix originally stacked as PR #52 is already represented on main; this ADR does not depend on merging that stale branch.
2. Review this ADR as a proposal only. It adds no runtime feature and is not a prerequisite for publishing the existing citation behavior.
3. Reconcile the contract with MultiQC maintainers, resolve upstream review, and establish the released module and minimum supported version before promising pipeline integration.
4. Open a separate `nf-core-utils` implementation PR for CSL-JSON output, with explicit pipeline-to-MultiQC wiring, author conversion, runtime coverage, and compatibility tests. Plan any later deprecation separately.

## Remaining design questions (for the `nf-core-utils` refactor)

- **Author-name handling:** choose a loss-aware conversion of existing free strings (for example, `"Li H, Handsaker B, et al."`) into CSL authors, including organizations and compound surnames; do not assume the current upstream heuristic is universally correct.
- **DOI-only / Zenodo tools:** preserve tool identity and links without inventing publication fields, matching the existing bibliography fallback.
- **Tool/version coverage:** define handling for multiple versions of a tool and tools such as registry MultiQC that do not emit a versions topic (issue #89); the current upstream deduplication retains only one record per tool.
- **MultiQC version floor and migration:** establish the released contract and preserve existing API behavior until an explicit migration is approved.

## Alternatives considered

### Keep formatting in `nf-core-utils` (status quo + PR #51)

Lowest immediate effort and compatible with existing consumers, but keeps presentation in Groovy and requires callers to respect the HTML output contract. This remains the implemented behavior while this proposal is evaluated.

### Emit Markdown instead of HTML and rely on MultiQC markdown rendering

A smaller presentation change, but does not provide structured bibliographic data or resolve compatibility with consumers expecting HTML.

### Structured output but render inside `nf-core-utils` (no MultiQC plugin)

Separates assembly from formatting internally and avoids an upstream release dependency, but retains citation presentation in Groovy rather than using MultiQC's reporting layer.
