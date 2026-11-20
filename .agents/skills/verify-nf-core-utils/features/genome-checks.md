# Genome checks

A pipeline that calls `genomeExistsError()` stops at launch when the user passes a `--genome` key that is not in `params.genomes`, and lists the keys that are available. A known key, or no `--genome`, lets the pipeline continue.

## Sub-features

- `genome-unknown` aborts with the missing key and the available keys.
- `genome-known` passes for a key in `params.genomes`.
- `genome-unset` passes when `params.genome` is null.

## How to get to it (user POV)

- Run a pipeline that includes `genomeExistsError` with `--genome <key>` on the command line.
- Run the same pipeline without `--genome`.

## Driving it with verify

Preconditions:

- Baseline preconditions from the index hold.
- `validation/genome-exists-error/nextflow.config` defines `params.genomes` with `GRCh38` and `GRCm39`.

- **Unknown key.** Run `verify drive genome-unknown validation/genome-exists-error --genome hg99`. `exit-code.txt` is `1`. `stderr.txt` contains `Genome 'hg99' not found in any config files provided to the pipeline.`, `Currently, the available genome keys are:` and `GRCh38, GRCm39`. Neither output file contains `genomeExistsError OK`.
- **Known key.** Run `verify drive genome-known validation/genome-exists-error --genome GRCh38`. `exit-code.txt` is `0` and `stdout.txt` contains `genomeExistsError OK`.
- **Unset.** Run `verify drive genome-unset validation/genome-exists-error`. `exit-code.txt` is `0` and `stdout.txt` contains `genomeExistsError OK`.

## Gotchas

- The error text goes to stderr inside Nextflow's `ERROR ~` block. `genomeExistsError OK` is a `println`, so it goes to stdout.
- `getGenomeAttribute`, `getReferencePath`, `getReferenceMetadata` and `getReferenceAttribute` have no journey yet. Prove them with a scratch pipeline and a config that defines `params.genomes`; do not count this journey for them.
