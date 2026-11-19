nextflow.enable.types = true

process RECORD_TOOL {
    input:
    tool: String
    version: String

    output:
    tool: String

    topic:
    record(process: "${task.process}", tool: tool, version: eval("printf '%s' '${version}'")) >> 'versions'

    script:
    """
    true
    """
}
