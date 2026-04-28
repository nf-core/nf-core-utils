# ARCHITECTURE.md — System Architecture

> Living document. Update whenever the layer diagram or module boundaries change.

---

## 1. High-Level Layer Diagram

```
┌───────────────────────────────────────────────────────────────┐
│                   Nextflow Pipeline (user)                     │
│   include { fn } from 'plugin/nf-core-utils'                  │
└──────────────────────────┬────────────────────────────────────┘
                           │  @Function annotations
┌──────────────────────────▼────────────────────────────────────┐
│               Extension Layer  (PF4J entry points)            │
│  NfUtilsPlugin.groovy  ·  NfUtilsExtension.groovy             │
│  NfcorePipelineObserverFactory.groovy                         │
└──────┬───────────┬──────────────┬────────────────┬────────────┘
       │           │              │                │
┌──────▼───┐ ┌────▼──────┐ ┌────▼──────┐ ┌───────▼───────────┐
│ Nextflow │ │  nf-core  │ │References │ │    Reporting /     │
│ Pipeline │ │ Utilities │ │ Extension │ │    Orchestrator    │
│ Utils    │ │           │ │           │ │                    │
└──────────┘ └───────────┘ └───────────┘ └────────────────────┘
```

---

## 2. Package Map

All source lives under `src/main/groovy/nfcore/plugin/`:

```
nfcore/plugin/
├── NfUtilsPlugin.groovy              # BasePlugin subclass; PF4J entry point
├── NfUtilsExtension.groovy           # @Function declarations; thin delegates
├── NfcorePipelineObserver.groovy     # Trace observer for lifecycle events
├── NfcorePipelineObserverFactory.groovy
└── nfcore/
    ├── NfcoreConfigValidator.groovy      # Profile/config validation
    ├── NfcoreNotificationUtils.groovy    # Email, Slack, Teams, terminal output
    ├── NfcoreReportingUtils.groovy       # MultiQC, pipeline summaries
    ├── NfcoreVersionUtils.groovy         # Version aggregation, YAML, channels
    ├── NfcoreCitationUtils.groovy        # Citation extraction from meta.yml
    └── NfcoreReportingOrchestrator.groovy # Composes version+citation+bib+methods
```

And the sibling packages:

```
nfcore/plugin/
├── nextflow/
│   └── NextflowPipelineUtils.groovy   # getWorkflowVersion, dumpParametersToJSON, checkCondaChannels
└── references/
    └── ReferencesUtils.groovy         # getReferencesFile, getReferencesValue
```

---

## 3. Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Stateless utility classes | Simplifies testing; no shared mutable state across pipeline runs |
| Extension methods delegate to utility classes | Thin extension surface; business logic is independently testable |
| Single external dependency (SnakeYAML) | Keeps the plugin lightweight; YAML is required for meta.yml parsing |
| PF4J plugin model | Standard Nextflow plugin contract; enables versioning via the plugin registry |
| Groovy (not Java) | Aligns with Nextflow's own language; richer DSL for expression of pipeline idioms |

See [`docs/design-docs/`](docs/design-docs/) for per-decision ADRs and [`docs/design-docs/core-beliefs.md`](docs/design-docs/core-beliefs.md) for principles.

---

## 4. Data & Control Flow

### 4.1 Version String Generation

```
workflow.manifest.version + workflow.commitId
        │
        ▼
NextflowPipelineUtils.getWorkflowVersion()
        │  applies: 'v' prefix, 7-char git hash, 'g' prefix
        ▼
"v1.2.0-gabc1234"  →  log / published to channels
```

### 4.2 Reporting Pipeline

```
NfcoreVersionUtils (collect versions from YAML channels)
        +
NfcoreCitationUtils (parse meta.yml citations)
        │
        ▼
NfcoreReportingOrchestrator.generateCompleteReport()
        │
        ├── MultiQC versions YAML
        ├── Citations / bibliography
        └── Methods description (Markdown)
```

### 4.3 Observer Lifecycle

```
Nextflow session start
        │
NfcorePipelineObserverFactory.create()
        │
NfcorePipelineObserver (registered with Nextflow trace subsystem)
        │  onFlowCreate / onFlowComplete / onProcessComplete
        └── delegates to NfcoreNotificationUtils / NfcoreReportingUtils
```

---

## 5. Testing Architecture

```
src/test/groovy/nfcore/plugin/
├── nextflow/      NextflowPipelineTest.groovy
├── nfcore/        NfcoreVersionUtilsTest.groovy
│                  NfcoreReportingOrchestratorTest.groovy
│                  … (one test per utility class)
└── references/    ReferencesTest.groovy
```

- Framework: **Spock** (Groovy BDD).
- Test resources: `src/test/resources/` (YAML fixtures, mock configs).
- Run: `make test` or `./gradlew test`.

---

## 6. Build & Release

| Step | Command |
|------|---------|
| Compile | `make assemble` |
| Test | `make test` |
| Local install | `make install` |
| Release | `make release` (requires `~/.gradle/gradle.properties` with GitHub credentials) |

Plugin version lives in `build.gradle` → `version`. Nextflow compatibility floor: `25.10.0`.

---

## 7. Future Architecture Considerations

Tracked in [`docs/exec-plans/active/`](docs/exec-plans/active/) and [`docs/exec-plans/tech-debt-tracker.md`](docs/exec-plans/tech-debt-tracker.md).
