# Topic Channel Citation Management Validation Test

This validation test demonstrates the new automatic citation management system using Nextflow topic channels.

## Overview

Instead of manually configuring citations with hardcoded conditional logic, this approach allows:

1. **Runtime Citation Extraction**: Each process calls `getCitation()` with its meta.yml path
2. **Topic Channel Emission**: Citations are automatically emitted to a shared `citation` topic
3. **Automatic Collection**: The workflow collects all citations from executed processes
4. **Zero Maintenance**: Final citation text and bibliography generated automatically

## Test Structure

### Processes
- `FASTQC`: Quality control process
- `MULTIQC`: Report aggregation process  
- `SAMTOOLS_VIEW`: SAM/BAM processing
- `OPTIONAL_TOOL`: Conditional process (controlled by `params.run_optional`)

### Citation Flow
```
Process Execution → getCitation() → Topic Channel → Collect → Auto-Format → Reports
```

## Usage Pattern

### In Process Definition
```nextflow
process FASTQC {
    output:
    path "*.html", emit: html
    val(getCitation("${moduleDir}/meta.yml")), topic: citation  // <-- Auto citation
    
    script:
    // ... process logic
}
```

### In Workflow
```nextflow
// Collect citations automatically
ch_citations = channel.topic('citation').collect()

// Generate formatted output
final_citations = ch_citations.map { citations ->
    [
        citation_text: autoToolCitationText(citations),
        bibliography: autoToolBibliographyText(citations)
    ]
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

- **Zero Maintenance**: Citations update automatically when modules change
- **Runtime Accuracy**: Only executed tools appear in citations
- **Clean Separation**: Citation logic separate from pipeline logic
- **Backward Compatible**: Works alongside existing citation methods
- **Error Resilient**: Handles missing or malformed meta.yml files gracefully

This approach transforms citation management from a manual, error-prone process into a fully automated system! 🚀