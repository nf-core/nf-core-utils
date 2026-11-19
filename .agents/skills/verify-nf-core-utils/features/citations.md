# Citations on the fly

`citationsOnTheFly(versions, metaYmlPaths, extraTools)` cites exactly the tools that ran, using each module's `meta.yml`, and `citationOutputs()` renders them as MultiQC HTML, a bibliography, BibTeX and CSL-JSON. A module with a `meta.yml` that did not run is not cited.

## Sub-features

- `citations-ran-only` cites FastQC, SAMtools and MultiQC, which ran, and not STAR, which was gated off.
- `citations-extra` adds `multiqcsav`, a tool with no process, through the extra-tools list.
- `citations-formats` writes HTML citations, HTML bibliography, BibTeX and CSL-JSON.
- `citations-optional-on` cites STAR once `--run_optional true` runs it.

## How to get to it (user POV)

- Collect `channel.topic('versions')` and the module `meta.yml` paths in a pipeline and call `citationsOnTheFly`, then `citationOutputs`.

## Driving it with verify

Preconditions:

- Baseline preconditions from the index hold.

- **Default run.** Run `verify drive citations validation/citations-on-the-fly --outdir results`. `exit-code.txt` is `0`. `stderr.txt` has the line `=== Cited tools: fastqc, multiqc, multiqcsav, samtools ===`. `results/` contains `auto_citations.html`, `auto_bibliography.html`, `citations.bib` and `citations.json`, and none of them mention STAR.
- **Optional tool on.** Run `verify drive citations-star validation/citations-on-the-fly --outdir results --run_optional true`. The cited-tools line ends `samtools, star ===` and `results/citations.bib` mentions STAR.

## Gotchas

- The journey writes with `new File(params.outdir)`, relative to the launch dir. Without `--outdir`, `params.outdir` is null and files land in a directory literally named `null`.
- `validation/topic-channel-citations` is the older explicit-`citation`-topic pattern; it is a separate journey, not a substitute for this one.
- `validation/citations-remote-modules/check.sh` needs network access to fetch remote modules. It is not part of the offline baseline.
