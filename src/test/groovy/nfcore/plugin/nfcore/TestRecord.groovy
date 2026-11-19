package nfcore.plugin.nfcore

import nextflow.script.types.Record

/** A stand-in for nextflow.util.RecordMap, which the plugin's compile-time Nextflow (25.10.4) lacks. */
class TestRecord extends LinkedHashMap<String, Object> implements Record {
    TestRecord(Map<String, Object> fields) {
        super(fields)
    }
}
