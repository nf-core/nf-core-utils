# Software Versions Validation Test

This validation test demonstrates the enhanced `softwareVersionsToYAML()` function from the nf-core-utils plugin.

## Features Tested

1. **YAML String Inputs**: Legacy inline YAML strings
2. **Topic Tuples**: Modern `[process, tool, version]` format
3. **File Inputs**: Reading from `versions.yml` files
4. **Map Objects**: Direct tool->version mappings
5. **Mixed Inputs**: Combining all input types
6. **Custom Nextflow Version**: Overriding the detected Nextflow version
7. **Named Parameters**: Using named parameter syntax
8. **Process Name Extraction**: Extracting process names from full paths
9. **Alphabetical Sorting**: Processes and tools sorted alphabetically

## Running the Test

```bash
cd validation/software-versions
nextflow run main.nf
```

## Expected Behavior

The function should:

- Accept all input types seamlessly
- Merge versions from multiple sources
- Sort output alphabetically
- Include workflow and Nextflow version information
- Handle errors gracefully

## Example Output

```yaml
ALPHA_PROCESS:
  alpha: 2.0.0
  zulu: 3.0.0
Software:
  bcftools: 1.16
  fastqc: 0.12.1
  multiqc: 1.15
Workflow:
  nf-core/test-softwareversions: v1.0.0
  Nextflow: 24.10.0
```
