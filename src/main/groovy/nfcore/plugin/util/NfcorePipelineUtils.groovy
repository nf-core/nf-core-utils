/*
 * Copyright 2025, nf-core
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nfcore.plugin.util

import groovy.util.logging.Slf4j
import nextflow.Session
import java.nio.file.Path

/**
 * Utility functions for nf-core pipelines
 */
@Slf4j
class NfcorePipelineUtils {

    /**
     * Checks if a custom config or profile has been provided, logs a warning if not.
     * @param projectName The project name
     * @param config The config map (should have profile and configFiles)
     * @return true if config is valid, false otherwise
     */
    static boolean checkConfigProvided() {
        def configValidator = new NfcoreConfigValidator()
        def session = (Session) nextflow.Nextflow.session
        def meta = session.getWorkflowMetadata()
        def config = session.config
        String projectName = null
        if (meta != null && meta.metaClass?.hasProperty(meta, 'projectName')) {
            projectName = meta.projectName
        }
        return configValidator.checkConfigProvided(projectName, config)
    }

    /**
     * Checks if the profile string is valid and warns about positional arguments.
     * @param args The command line arguments
     */
    static void checkProfileProvided(args) {
        def configValidator = new NfcoreConfigValidator()
        def session = (Session) nextflow.Nextflow.session
        def profile = session.profile
        def commandLine = args ? args.join(' ') : null
        configValidator.checkProfileProvided(profile, commandLine)
    }

    /**
     * Generate workflow version string from session manifest
     * @return Version string
     */
    static String getWorkflowVersion() {
        def session = (Session) nextflow.Nextflow.session
        return NfcoreVersionUtils.getWorkflowVersion(session)
    }
    
    /**
     * Generate workflow summary for MultiQC
     * @param summaryParams Map of parameter groups and their parameters
     * @return YAML formatted string for MultiQC
     */
    static String paramsSummaryMultiqc(Map<String, Map<String, Object>> summaryParams) {
        return NfcoreReportingUtils.paramsSummaryMultiqc(summaryParams)
    }
    
    /**
     * ANSII colour codes used for terminal logging
     * @param monochrome_logs Boolean indicating whether to use monochrome logs
     * @return Map of colour codes
     */
    static Map logColours(boolean monochrome_logs=true) {
        return NfcoreNotificationUtils.logColours(monochrome_logs)
    }
    
    /**
     * Return a single report from an object that may be a Path or List
     * @param multiqc_reports The reports object
     * @return A single report Path or null
     */
    static Path getSingleReport(def multiqc_reports) {
        return NfcoreNotificationUtils.getSingleReport(multiqc_reports)
    }
    
    /**
     * Construct and send completion email
     * @param summary_params Map of summary parameters
     * @param email Email address
     * @param email_on_fail Email address for failures only
     * @param plaintext_email Whether to send plaintext email
     * @param outdir Output directory
     * @param monochrome_logs Whether to use monochrome logs
     * @param multiqc_report MultiQC report file
     */
    static void completionEmail(Map summary_params, String email, String email_on_fail, 
                               boolean plaintext_email, String outdir, 
                               boolean monochrome_logs=true, def multiqc_report=null) {
        NfcoreNotificationUtils.completionEmail(summary_params, email, email_on_fail, 
                                             plaintext_email, outdir, monochrome_logs, multiqc_report)
    }
    
    /**
     * Print pipeline summary on completion
     * @param monochrome_logs Whether to use monochrome logs
     */
    static void completionSummary(boolean monochrome_logs=true) {
        NfcoreNotificationUtils.completionSummary(monochrome_logs)
    }
    
    /**
     * Construct and send a notification to a web server as JSON e.g. Microsoft Teams and Slack
     * @param summary_params Map of summary parameters
     * @param hook_url Webhook URL
     */
    static void imNotification(Map summary_params, String hook_url) {
        NfcoreNotificationUtils.imNotification(summary_params, hook_url)
    }
    
    /**
     * Create workflow summary template for MultiQC
     * @param summary Map of parameters
     * @param nfMetadataList List of metadata fields to include
     * @param results Map of pipeline results
     * @return Map with HTML summaries for MultiQC
     */
    static Map workflowSummaryMQC(Map summary, List nfMetadataList, Map results) {
        return NfcoreReportingUtils.workflowSummaryMQC(summary, nfMetadataList, results)
    }
    
    /**
     * Generate summary logs for each section of a pipeline
     * @param sections Map of section names with their log messages
     * @param monochrome Whether to use colors in logs
     * @return Map of colored section logs
     */
    static Map sectionLogs(Map sections, boolean monochrome=false) {
        return NfcoreReportingUtils.sectionLogs(sections, monochrome)
    }

    /**
     * Exit pipeline if incorrect --genome key provided
     */
    static void genomeExistsError(Map params) {
        if (params.genomes && params.genome && !params.genomes.containsKey(params.genome)) {
            def error_string = """
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  Genome '${params.genome}' not found in any config files provided to the pipeline.
  Currently, the available genome keys are:
  ${params.genomes.keySet().join(", ")}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
""".stripIndent()
            throw new IllegalArgumentException(error_string)
        }
    }

    /**
     * Check and validate pipeline parameters
     */
    static void validateInputParameters(Map params) {
        genomeExistsError(params)
    }

    /**
     * Validate channels from input samplesheet
     * @param input List of [metas, fastqs]
     * @return [meta, fastqs] if valid, throws error otherwise
     */
    static List validateInputSamplesheet(List input) {
        def metas = input[0]
        def fastqs = input[1]
        // Check that multiple runs of the same sample are of the same datatype i.e. single-end / paired-end
        def endedness_ok = metas.collect { meta -> meta.single_end }.unique().size() == 1
        if (!endedness_ok) {
            throw new IllegalArgumentException("Please check input samplesheet -> Multiple runs of a sample must be of the same datatype i.e. single-end or paired-end: ${metas[0].id}")
        }
        return [metas[0], fastqs]
    }

    /**
     * Get attribute from genome config file e.g. fasta
     */
    static Object getGenomeAttribute(Map params, String attribute) {
        if (params.genomes && params.genome && params.genomes.containsKey(params.genome)) {
            if (params.genomes[params.genome].containsKey(attribute)) {
                return params.genomes[params.genome][attribute]
            }
        }
        return null
    }

    /**
     * Generate methods description for MultiQC
     */
    static String toolCitationText(Map params = [:]) {
        // Optionally use params to conditionally include tools
        def citation_text = "Tools used in the workflow included: " + [
            "BEDTools (Quinlan 2010)",
            "Bowtie 2 (Langmead 2012)",
            "BWA-MEM (Li 2013)",
            "BWA-MEM2 (Vasimuddin 2019)",
            "deepTools (Ramírez 2016)",
            "FastQC (Andrews 2010)",
            "FastP (Chen 2018)",
            "featureCounts (Liao 2013)",
            "GffRead (Pertea 2013)",
            "HISAT2 (Kim 2019)",
            "HOMER (Heinz 2010)",
            "MultiQC (Ewels et al. 2016)",
            "PINTS (Yao 2022)",
            "preseq (Daley 2013)",
            "RSeQC (Wang 2012)",
            "SAMTools (Li 2009)",
            "STAR (Dobin 2013)",
            "UMI-tools (Li 2009)",
            "Genomic Alignments (Lawrence 2013)",
            "groHMM (Chae 2015)",
            "."
        ].join(', ').trim()
        return citation_text
    }

    static String toolBibliographyText(Map params = [:]) {
        // Optionally use params to conditionally include references
        def reference_text = "<li>" + [
            "Quinlan AR, Hall IM. BEDTools: a flexible suite of utilities for comparing genomic features. Bioinformatics. 2010 Mar 15;26(6):841-2. doi: 10.1093/bioinformatics/btq033. Epub 2010 Jan 28. PubMed PMID: 20110278; PubMed Central PMCID: PMC2832824.",
            "Langmead, B., Salzberg, S. Fast gapped-read alignment with Bowtie 2. Nat Methods 9, 357–359 (2012). doi: 10.1038/nmeth.1923.",
            "Li H: Aligning sequence reads, clone sequences and assembly contigs with BWA-MEM. arXiv 2013. doi: 10.48550/arXiv.1303.3997",
            "M. Vasimuddin, S. Misra, H. Li and S. Aluru, Efficient Architecture-Aware Acceleration of BWA-MEM for Multicore Systems, 2019 IEEE International Parallel and Distributed Processing Symposium (IPDPS), 2019, pp. 314-324. doi: 10.1109/IPDPS.2019.00041.",
            "Ramírez, Fidel, Devon P. Ryan, Björn Grüning, Vivek Bhardwaj, Fabian Kilpert, Andreas S. Richter, Steffen Heyne, Friederike Dündar, and Thomas Manke. deepTools2: A next Generation Web Server for Deep-Sequencing Data Analysis. Nucleic Acids Research (2016). doi:10.1093/nar/gkw257.",
            "Shifu Chen, Yanqing Zhou, Yaru Chen, Jia Gu, fastp: an ultra-fast all-in-one FASTQ preprocessor, Bioinformatics, Volume 34, Issue 17, 01 September 2018, Pages i884–i890, doi: 10.1093/bioinformatics/bty560. PubMed PMID: 30423086. PubMed Central PMCID: PMC6129281",
            "Andrews, S. (2010). FastQC: A Quality Control Tool for High Throughput Sequence Data [Online].",
            "Liao Y, Smyth GK, Shi W. featureCounts: an efficient general purpose program for assigning sequence reads to genomic features. Bioinformatics. 2014 Apr 1;30(7):923-30. doi: 10.1093/bioinformatics/btt656. Epub 2013 Nov 13. PubMed PMID: 24227677.",
            "Pertea G, Pertea M. GFF Utilities: GffRead and GffCompare. F1000Res. 2020 Apr 28;9:ISCB Comm J-304. doi: 10.12688/f1000research.23297.2. eCollection 2020. PubMed PMID: 32489650; PubMed Central PMCID: PMC7222033.",
            "Kim D, Paggi JM, Park C, Bennett C, Salzberg SL. Graph-based genome alignment and genotyping with HISAT2 and HISAT-genotype Graph-based genome alignment and genotyping with HISAT2 and HISAT-genotype. Nat Biotechnol. 2019 Aug;37(8):907-915. doi: 10.1038/s41587-019-0201-4. Epub 2019 Aug 2. PubMed PMID: 31375807.",
            "Heinz S, Benner C, Spann N, Bertolino E et al. Simple Combinations of Lineage-Determining Transcription Factors Prime cis-Regulatory Elements Required for Macrophage and B Cell Identities. Mol Cell 2010 May 28;38(4):576-589. PMID: 20513432",
            "Ewels P, Magnusson M, Lundin S, Käller M. MultiQC: summarize analysis results for multiple tools and samples in a single report. Bioinformatics. 2016 Oct 1;32(19):3047-8. doi: 10.1093/bioinformatics/btw354. Epub 2016 Jun 16. PubMed PMID: 27312411; PubMed Central PMCID: PMC5039924.",
            "Yao, L., Liang, J., Ozer, A. et al. A comparison of experimental assays and analytical methods for genome-wide identification of active enhancers. Nat Biotechnol 40, 1056–1065 (2022). https://doi.org/10.1038/s41587-022-01211-7",
            "Daley T, Smith AD. Predicting the molecular complexity of sequencing libraries. Nat Methods. 2013 Apr;10(4):325-7. doi: 10.1038/nmeth.2375. Epub 2013 Feb 24. PubMed PMID: 23435259; PubMed Central PMCID: PMC3612374.",
            "Wang L, Wang S, Li W. RSeQC: quality control of RNA-seq experiments Bioinformatics. 2012 Aug 15;28(16):2184-5. doi: 10.1093/bioinformatics/bts356. Epub 2012 Jun 27. PubMed PMID: 22743226.",
            "Li H, Handsaker B, Wysoker A, Fennell T, Ruan J, Homer N, Marth G, Abecasis G, Durbin R; 1000 Genome Project Data Processing Subgroup. The Sequence Alignment/Map format and SAMtools. Bioinformatics. 2009 Aug 15;25(16):2078-9. doi: 10.1093/bioinformatics/btp352. Epub 2009 Jun 8. PubMed PMID: 19505943; PubMed Central PMCID: PMC2723002.",
            "Dobin A, Davis CA, Schlesinger F, Drenkow J, Zaleski C, Jha S, Batut P, Chaisson M, Gingeras TR. STAR: ultrafast universal RNA-seq aligner Bioinformatics. 2013 Jan 1;29(1):15-21. doi: 10.1093/bioinformatics/bts635. Epub 2012 Oct 25. PubMed PMID: 23104886; PubMed Central PMCID: PMC3530905.",
            "Smith T, Heger A, Sudbery I. UMI-tools: modeling sequencing errors in Unique Molecular Identifiers to improve quantification accuracy Genome Res. 2017 Mar;27(3):491-499. doi: 10.1101/gr.209601.116. Epub 2017 Jan 18. PubMed PMID: 28100584; PubMed Central PMCID: PMC5340976.",
            "Lawrence M, Huber W, Pagès H, Aboyoun P, Carlson M, Gentleman R, Morgan M, Carey V (2013). 'Software for Computing and Annotating Genomic Ranges.' PLoS Computational Biology, 9. doi: 10.1371/journal.pcbi.1003118, http://www.ploscompbiol.org/article/info%3Adoi%2F10.1371%2Fjournal.pcbi.1003118.",
            "Chae M, Danko CG, Kraus WL (2015). 'groHMM: a computational tool for identifying unannotated and cell type-specific transcription units from global run-on sequencing data.' BMC Bioinformatics, 16(222)."
        ].join('</li> <li>').trim()
        return reference_text
    }

    static String methodsDescriptionText(File mqc_methods_yaml, Map meta = [:]) {
        // Convert to a named map so can be used as with familiar NXF ${workflow} variable syntax in the MultiQC YML file
        if (!meta) meta = [:]
        def session = (Session) nextflow.Nextflow.session
        meta.workflow = session.getWorkflowMetadata()?.toMap() ?: [:]
        meta["manifest_map"] = session.getManifest()?.toMap() ?: [:]
        // Pipeline DOI
        if (meta.manifest_map?.doi) {
            def temp_doi_ref = ""
            def manifest_doi = meta.manifest_map.doi.tokenize(",")
            manifest_doi.each { doi_ref ->
                temp_doi_ref += "(doi: <a href='https://doi.org/${doi_ref.replace('https://doi.org/', '').replace(' ', '')}'>${doi_ref.replace('https://doi.org/', '').replace(' ', '')}</a>), "
            }
            meta["doi_text"] = temp_doi_ref[0..-3]
        } else {
            meta["doi_text"] = ""
        }
        meta["nodoi_text"] = meta.manifest_map?.doi ? "" : "<li>If available, make sure to update the text to include the Zenodo DOI of version of the pipeline used. </li>"
        meta["tool_citations"] = toolCitationText()
        meta["tool_bibliography"] = toolBibliographyText()
        def methods_text = mqc_methods_yaml.text
        def engine = new groovy.text.SimpleTemplateEngine()
        def description_html = engine.createTemplate(methods_text).make(meta)
        return description_html.toString()
    }
} 