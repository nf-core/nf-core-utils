package nfcore.plugin

import nextflow.Session
import nextflow.trace.TraceObserverV2
import nextflow.trace.TraceObserverFactory

/**
 * Factory for creating NfCoreConfigObserver instances that implement TraceObserverV2
 */
class NfCoreConfigObserverFactory implements TraceObserverFactory {
    
    @Override
    Collection<TraceObserverV2> create(Session session) {
        // Get CLI arguments from session
        def args = session.binding.variables.get('args') as List ?: []
        
        // Allow disabling the checks via config
        def checksEnabled = session.config.navigate('nfcoreutils.checks.enabled')
        if (checksEnabled == false) {
            return []
        }
        
        return [new NfCoreConfigObserver(session, args)]
    }
} 