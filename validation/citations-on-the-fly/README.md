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

## Run

```bash
make install
cd validation && nf-test test citations-on-the-fly/main.nf.test
```
