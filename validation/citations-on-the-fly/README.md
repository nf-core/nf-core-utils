# citations-on-the-fly/

**Pattern: Citations on the Fly (versions-topic-driven)**

Demonstrates `citationsOnTheFly()` — citations generated from the tools that
_actually ran_, with their metadata resolved from each module's `meta.yml`.

Unlike [`topic-channel-citations/`](../topic-channel-citations/), the modules
here do **not** emit a dedicated `citation` topic. They only emit the standard
`versions` topic (`[process, tool, version]`) that every nf-core module already
produces. This means citations need **zero per-module changes** beyond the
versions reporting modules already do.

## Key validations

- `citationsOnTheFly(topicVersions, metaFilePaths)` cites exactly the tools that ran.
- `toolCitationText()` / `toolBibliographyText()` render the selected citations.
- A tool with a `meta.yml` that **did not run** (`STAR_ALIGN`, gated off by
  `params.run_optional = false`) is **not** cited — proving citations track
  execution, not the module inventory.

## Outputs

The workflow writes `auto_citations.html` and `auto_bibliography.html`. Short
citations contain DOI/homepage links and italic `et al.`, so the citation file
is HTML rather than plain text. Open it in a browser and copy the rendered
text for a manuscript; pasting HTML source without formatting does not remove
the tags. The workflow logs the output paths rather than raw HTML.

## Run

```bash
mise run validate citations-on-the-fly/main.nf.test
```
