# Citation Management Validation Test

This validation test demonstrates the citation management functionality added to nf-core-utils plugin, including both topic channel and file-based citation processing.

## Test Overview

This test validates the comprehensive citation management system that includes:

### Core Citation Functions
- `generateModuleToolCitation()` - Extract citations from module meta.yml files
- `toolCitationText()` - Format citations for display in reports
- `toolBibliographyText()` - Generate bibliography sections
- `collectCitationsFromFiles()` - Aggregate citations from multiple files
- `methodsDescriptionText()` - Generate methods descriptions with citations

### Topic Channel Architecture
- `processCitationsFromTopic()` - Process citation data in topic channel format `[module, tool, citation_data]`
- `processCitationsFromFile()` - Process legacy meta.yml file citations
- `processMixedCitationSources()` - Handle mixed topic and file-based citations during migration
- `convertMetaYamlToTopicFormat()` - Convert legacy formats to new topic channels

### Orchestrated Reporting
- `generateCitationReport()` - Comprehensive citation reporting
- `generateComprehensiveReport()` - Combined versions + citations + methods

## Test Scenario

The test creates a realistic pipeline scenario with:
1. Multiple modules with different citation formats (DOI, URL, text)
2. Mixed processing using both topic channels and file-based citations
3. Comprehensive report generation combining all citation sources
4. Error handling for missing or malformed citation data

## Expected Outputs

- `work/pipeline_info/citations_report.yml` - Structured citation data
- `work/pipeline_info/methods_description.txt` - Formatted methods description
- `work/pipeline_info/bibliography.txt` - Bibliography section
- Console output showing successful citation processing

## Migration Pattern

This test demonstrates the progressive migration strategy from file-based citations to topic channels:
1. Start with existing meta.yml files
2. Gradually convert to topic channel format
3. Use mixed processing during transition
4. Eventually migrate fully to topic channels

This mirrors the version utilities migration pattern and provides a clear path for nf-core pipelines to adopt the new citation system.