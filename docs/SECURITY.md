# SECURITY.md — Security Posture

> nf-core-utils is a Nextflow plugin library used in bioinformatics pipelines.
> While it is not a user-facing web service, it processes credentials, file paths,
> and external service integrations that require a thoughtful security posture.

---

## 1. Threat Model

| Asset | Threat | Mitigation |
|-------|--------|-----------|
| SMTP credentials | Leaked into logs or param JSON | Credentials come from `params.email_on_fail`/config, never logged at info/debug; not dumped to JSON |
| Slack/Teams webhook URLs | Leaked into logs or param JSON | Treat as secrets; never log the full URL |
| File paths (outdir, references) | Path traversal | Use Nextflow `Path` resolution; do not construct paths via string concatenation with user input |
| igenomes config | Untrusted genome key | Validate key against known map; fail loudly if not found |
| External YAML (meta.yml) | Malformed YAML crashing the plugin | SnakeYAML parsing wrapped in try/catch with informative error |
| Build artifacts | Supply chain attack | Use Gradle wrapper (`./gradlew`); pin dependency versions in `build.gradle` |

---

## 2. Credential Handling Rules

1. **Never** include credentials (SMTP passwords, webhook URLs, API tokens) in:
   - Log output at any level.
   - The `params_<timestamp>.json` file produced by `dumpParametersToJSON`.
   - Exception messages.

2. Credentials must come from Nextflow secrets, environment variables, or `~/.nextflow/config` — not hardcoded in pipeline scripts.

3. If a function receives a credential parameter, document it as a secret in the function's Groovydoc.

---

## 3. Dependency Security

- Minimise runtime dependencies (currently: SnakeYAML only).
- Before adding a new dependency: check it against known CVE databases.
- Keep SnakeYAML pinned to a non-vulnerable version; review with each release.
- Run `./gradlew dependencyCheckAnalyze` (OWASP dependency check) if added to `build.gradle`.

---

## 4. Reporting a Vulnerability

Do **not** open a public GitHub issue for security vulnerabilities.

Contact: security@nf-co.re (or open a [GitHub Security Advisory](https://github.com/nf-core/nf-core-utils/security/advisories/new) if available).

Include:
- Description of the vulnerability.
- Steps to reproduce.
- Potential impact.
- Suggested fix if known.

We aim to acknowledge reports within 48 hours and publish a fix within 14 days for critical issues.

---

## 5. Security Checklist for PRs

- [ ] No credentials, tokens, or URLs hardcoded or logged.
- [ ] User-supplied file paths resolved via Nextflow's `Path` API, not string concatenation.
- [ ] New external inputs (YAML, JSON, config) wrapped in try/catch with sanitised error messages.
- [ ] New dependencies reviewed for known CVEs.
- [ ] If SMTP/webhook logic changed: verify credentials are not included in any log statement.
