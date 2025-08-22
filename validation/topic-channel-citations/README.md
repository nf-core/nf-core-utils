# Topic Channel Citation Management Validation Test

This validation test demonstrates the new automatic citation management system using Nextflow topic channels with **nf-core style modular organization**.

## Overview

Instead of manually configuring citations with hardcoded conditional logic, this approach allows:

1. **Runtime Citation Extraction**: Each module calls `getCitation()` with its co-located meta.yml
2. **Topic Channel Emission**: Citations are automatically emitted to a shared `citation` topic
3. **Automatic Collection**: The workflow collects all citations from executed processes
4. **Zero Maintenance**: Final citation text and bibliography generated automatically

## Test Structure (nf-core Style)

This test follows the **nf-core module organization pattern** where each tool has its own module directory:

```
topic-channel-citations/
├── main.nf (workflow only, imports modules)
├── modules/
│   └── local/
│       ├── fastqc/
│       │   ├── main.nf (FASTQC process)
│       │   └── meta.yml (FastQC metadata)
│       ├── multiqc/
│       │   ├── main.nf (MULTIQC process)
│       │   └── meta.yml (MultiQC metadata)
│       ├── samtools/
│       │   └── view/
│       │       ├── main.nf (SAMTOOLS_VIEW process)
│       │       └── meta.yml (Samtools metadata)
│       └── star/
│           └── align/
│               ├── main.nf (STAR_ALIGN process)
│               └── meta.yml (STAR metadata)
├── nextflow.config
└── README.md
```

### Modules
- **`modules/local/fastqc/`**: Quality control process with co-located metadata
- **`modules/local/multiqc/`**: Report aggregation process with co-located metadata
- **`modules/local/samtools/view/`**: SAM/BAM processing with co-located metadata
- **`modules/local/star/align/`**: Conditional alignment process (controlled by `params.run_optional`)

### Citation Flow
```
Module Execution → getCitation("${moduleDir}/meta.yml") → Topic Channel → Collect → Auto-Format → Reports
```

## Usage Pattern

### In Module Definition
```nextflow
// modules/local/fastqc/main.nf
process FASTQC {
    output:
    path "*.html", emit: html
    val citation_data, topic: citation
    
    script:
    citation_data = getCitation("${moduleDir}/meta.yml")  // <-- Co-located meta.yml
    """
    # FastQC commands here
    """
}
```

### In Main Workflow
```nextflow
// Import modules
include { FASTQC } from './modules/local/fastqc/main'
include { MULTIQC } from './modules/local/multiqc/main'
include { SAMTOOLS_VIEW } from './modules/local/samtools/view/main'
include { STAR_ALIGN } from './modules/local/star/align/main'

workflow {
    // Use modules
    fastqc_out = FASTQC(samples)
    
    // Collect citations automatically
    ch_citations = channel.topic('citation').collect()
    
    // Generate formatted output
    final_citations = ch_citations.map { citations ->
        [
            citation_text: autoToolCitationText(citations),
            bibliography: autoToolBibliographyText(citations)
        ]
    }
}
```

## Running the Test

```bash
# Build and install plugin
make install

# Run validation test 
cd validation/topic-channel-citations
nextflow run . -plugins nf-core-utils@0.2.0

# Test with optional tool enabled
nextflow run . -plugins nf-core-utils@0.2.0 --run_optional true
```

## Expected Output

The test will:
1. Execute processes that emit citations to topic channel
2. Collect all citation data automatically
3. Generate formatted citation text and bibliography
4. Write output files to `work/pipeline_info/`
5. Display final citations in the log

## Benefits Demonstrated

### Citation Management Benefits
- **Zero Maintenance**: Citations update automatically when modules change
- **Runtime Accuracy**: Only executed tools appear in citations
- **Clean Separation**: Citation logic separate from pipeline logic
- **Error Resilient**: Handles missing or malformed meta.yml files gracefully

### nf-core Module Structure Benefits
- **Realistic Organization**: Matches actual nf-core pipeline structure
- **Co-located Metadata**: Each module's meta.yml is in the same directory as its main.nf
- **Clear Module Boundaries**: Each tool is self-contained in its own directory
- **Educational Value**: Shows pipeline developers exactly how to implement citations
- **Maintainable**: Easy to understand, modify, and extend individual modules

### Development Experience
- **Clear Citation Path**: `${moduleDir}/meta.yml` references are obvious and reliable
- **Modular Testing**: Each module can be tested independently
- **Standard Patterns**: Follows established nf-core conventions that developers know

This approach transforms citation management from a manual, error-prone process into a fully automated system that perfectly integrates with nf-core's modular architecture! 🚀