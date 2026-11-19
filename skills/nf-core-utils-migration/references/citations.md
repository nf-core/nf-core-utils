# Tool citations from module meta.yml

Build the MultiQC methods description from the tools that actually ran, with citations read from each module's `meta.yml`, using `citationsOnTheFly` from nf-core-utils (0.7.0 or later). Do this in its own PR, after the helper migration, because it touches module files and snapshots across the whole pipeline.

Reference implementations: nf-core/seqinspector#241, nf-core/createpanelrefs#107.

## Contents

1. How the plugin resolves a citation
2. Wiring
3. Predict the warning fallout
4. Choose where citation metadata lives
5. Write verified metadata
6. Patch modules cleanly
7. Verify the rendered text
8. Docs and PR

## 1. How the plugin resolves a citation

`citationsOnTheFly(versions, metaYmlPaths[, extraTools])` takes the tool names emitted to the `versions` topic (the `val("<tool>")` in `topic: versions` tuples) and matches each against the tool keys under `tools:` in the given `meta.yml` files. For a matched tool it uses, in order:

1. the nested `publication:` block (`author`, `year`, `title`, `source`), rendered as `Author et al. (year)` in the text and a full Harvard-style entry in the bibliography
2. `doi`
3. `homepage`
4. `description`

An unmatched tool logs `WARN ... No citation metadata found for tool '<tool>' — add it to the module's meta.yml 'tools:' section`.

Known rendering quirks: the short form always says `et al.`, even for two authors; a tool with only a `description` shows the description text in the citation list.

## 2. Wiring

Replace the template's `toolCitationText()`, `toolBibliographyText()`, local `methodsDescriptionText()` and local `paramsSummaryMultiqc()` with plugin calls:

```groovy
include { citationsOnTheFly      } from 'plugin/nf-core-utils'
include { methodsDescriptionText } from 'plugin/nf-core-utils'

// MultiQC runs after this collect, so its version never reaches the topic
def methods_description = channel.topic("versions")
    .collect(flat: false)
    .map { versions ->
        methodsDescriptionText(
            multiqc_custom_methods_description.toString(),
            citationsOnTheFly(
                versions,
                files("${projectDir}/modules/**/meta.yml").collect { path -> path.toString() },
                ['multiqc'],
            ),
        )
    }
```

Pass every tool that runs after the collect (MultiQC, MultiQC plugins such as `multiqcsav`) in the third argument, otherwise it is missing from the citations. Pipeline-specific text goes in the third `methodsDescriptionText` argument as a map, for example `[subsampled_text: ...]` in seqinspector, with a matching `${subsampled_text}` in `assets/methods_description_template.yml`.

## 3. Predict the warning fallout

Pipeline snapshots usually capture `WARN` lines from stderr, so every uncited tool breaks a snapshot. List the emitted tool names before running tests:

```bash
grep -rhoE "val\([\"'][A-Za-z0-9_.-]+[\"']\), eval" modules --include=main.nf | sed -E "s/val\(.(.*).\), eval/\1/" | sort | uniq -c
```

Compare against the `tools:` keys in each module `meta.yml`. A tool emitted to the topic but missing from `tools:` (for example `gzip` in `mosdepth`, `coreutils` in `find/concatenate`) will warn. A topic name that differs from the `meta.yml` key will warn too.

For each warning, decide:

- Real tool worth citing: add a `tools:` entry for it (see 4 and 6).
- Generic shell utility with nothing to cite (`coreutils`, `find`): add the exact warning text to the snapshot ignore list in `tests/lib/UTILS.groovy`, one entry per tool. Avoid a blanket `No citation metadata found` pattern, so a newly added uncited module still fails a test and gets noticed. Ignoring only hides the warning; the tool still appears in the citation list via its fallback.

## 4. Choose where citation metadata lives

Upstream nf-core modules mostly lack `publication:` blocks. Decide with the user before editing anything, because each option has a maintenance cost:

1. Upstream first: open a PR to nf-core/modules adding the `publication:` block, then `nf-core modules update`. Slowest, no local debt.
2. Module patches: edit the module `meta.yml` and run `nf-core modules patch <module>`. Works today, but every `nf-core modules update` needs the patch regenerated. seqinspector#241 and createpanelrefs#107 did this (createpanelrefs: 25 modules).
3. Local overlay file: a pipeline-level YAML (for example `assets/citations.yml`) with a `tools:` list in `meta.yml` shape, appended to the `metaYmlPaths` list. Untested: the plugin parses any path it is given, but no pipeline has shipped this yet, so verify it renders before recommending it.

Whatever you pick, say in the PR that patches or overlays are temporary until the metadata is upstream.

## 5. Write verified metadata

Never write DOIs, authors, years, titles or journals from memory. Resolve each DOI and copy the fields from the record:

```bash
curl -s "https://api.crossref.org/works/<doi>" | python3 -c "import json,sys;m=json.load(sys.stdin)['message'];print(m['title'][0]);print(m.get('container-title'));print(m['issued']);print([a.get('family') for a in m.get('author',[])])"
curl -s "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term=<doi>%5Bdoi%5D&retmode=json"
curl -s "https://pmc.ncbi.nlm.nih.gov/tools/idconv/api/v1/articles/?ids=<pmid>&format=json"
```

The block format:

```yaml
tools:
  - mosdepth:
      doi: "10.1093/bioinformatics/btx699"
      publication:
        author: "Pedersen B.S. & Quinlan A.R."
        year: 2018
        title: "Mosdepth: quick coverage calculation for genomes and exomes"
        source: "Bioinformatics"
```

If a module's existing `doi` points to a weaker source than the paper the pipeline cites in `CITATIONS.md` (for example gatk4 modules point to an AACR 2017 conference abstract, while pipelines cite McKenna et al. 2010), ask the user before replacing it. Changing which paper a tool is credited with is a scientific attribution decision, not a formatting fix.

Bulk edits across many `meta.yml` files must handle quoted keys (`- "gatk4":`) as well as bare ones. After editing, grep for modules that still lack `publication:` and compare with the list you meant to change.

## 6. Patch modules cleanly

`nf-core modules patch` diffs every file in the module directory against the pinned upstream version, gitignored `tests/` files included. Earlier repo-wide formatting (for example `nextflow lint -format` reaching `modules/nf-core/*/tests/nextflow.config`) then leaks into the patch.

Before patching, restore every non-`meta.yml` file to the pinned SHA from `modules.json`:

```bash
SHA=$(python3 -c "import json;print(json.load(open('modules.json'))['repos']['https://github.com/nf-core/modules.git']['modules']['nf-core']['<module>']['git_sha'])")
R=~/.config/nfcore/nf-core/modules
for f in $(cd modules/nf-core/<module> && find . -type f ! -name meta.yml ! -name '*.diff' | sed 's|^\./||'); do
  git -C $R show $SHA:modules/nf-core/<module>/$f | cmp -s - modules/nf-core/<module>/$f || echo "drift: $f"
done
```

To regenerate an existing patch, use `nf-core modules patch --remove <module>` and then `nf-core modules patch <module>`, rather than hand-editing `modules.json`.

After patching, check that every diff only touches `meta.yml`:

```bash
find modules/nf-core -name "*.diff" -exec grep -h "^Changes in '" {} + | grep -v "meta.yml"   # expect no output
```

## 7. Verify the rendered text

Snapshots do not cover the citation text, so read it from a test run:

```bash
grep "Tools used" $(find ~/tests -name methods_description_mqc.yaml -newer main.nf | head -1)
```

Check that each tool shows `Author et al. (year)` where a publication exists, that tools passed as extras appear, and that the References list has full entries.

## 8. Docs and PR

- `docs/CONTRIBUTING.md`: a "Tool citations" section explaining the topic-to-`meta.yml` matching, the `publication:` format, the chosen metadata location (patch, overlay or upstream), how to regenerate patches, and the snapshot ignore list.
- `CITATIONS.md`: an entry for every tool the pipeline runs, using the verified metadata; homepage-only links for tools without a paper.
- PR description: which modules carry patches and which papers they cite, any DOI replacements and why, and the upstream follow-up.
