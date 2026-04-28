# QUALITY_SCORE.md — Quality Standards & Scorecard

> Use this as a checklist before merging any PR and as a periodic health check for the codebase.

---

## 1. Quality Dimensions

| Dimension | Definition | Target |
|-----------|-----------|--------|
| **Test coverage** | % of utility class methods covered by Spock unit tests | ≥ 80% line coverage |
| **Public API docs** | All `@Function` methods have a docs entry in `docs/` | 100% |
| **No broken links** | All relative Markdown links in docs resolve | 100% |
| **CHANGELOG currency** | Every user-visible change has a CHANGELOG entry | 100% |
| **Build green** | `make test` passes on latest Nextflow compatibility target | 100% |
| **No TODO / FIXME** | No untracked `// TODO` or `// FIXME` in committed code | 0 untracked |

---

## 2. Pre-Merge Checklist

Before merging a PR, verify:

- [ ] `make test` passes locally.
- [ ] New public functions have unit tests (happy path + ≥1 failure mode).
- [ ] New public functions are documented in `docs/`.
- [ ] `CHANGELOG.md` updated under `[Unreleased]`.
- [ ] No new external runtime dependencies without a design doc.
- [ ] If a breaking API change: version bump + migration guidance in CHANGELOG.
- [ ] If a new tech-debt item was intentionally introduced: it is logged in [`docs/exec-plans/tech-debt-tracker.md`](exec-plans/tech-debt-tracker.md).

---

## 3. Code Review Standards

### Automatic disqualifiers (PR cannot merge):
- `make test` fails.
- Breaking change to a public `@Function` without a major version bump.
- New external runtime dependency without a design doc.

### Encouraged but not blocking:
- Groovydoc comments on all public methods.
- Example usage in docs for complex functions.
- Removal of pre-existing TODO comments (with proper resolution, not just deletion).

---

## 4. Periodic Health Check

Run this quarterly (or after any significant feature addition):

```bash
# Check test coverage (requires JaCoCo — add to build.gradle if not present)
./gradlew jacocoTestReport

# Find untracked TODOs
grep -r "TODO\|FIXME" src/main/ --include="*.groovy"

# Verify all docs links resolve (use markdown-link-check or similar)
# npx markdown-link-check docs/**/*.md
```

---

## 5. Current Quality Status

> Update this section after each quarterly check.

| Check | Last run | Result |
|-------|----------|--------|
| Test suite | — | — |
| Coverage report | — | — |
| TODO/FIXME audit | — | — |
| Docs link check | — | — |

---

## 6. Improving Quality Over Time

Quality items are tracked as tech-debt items in [`docs/exec-plans/tech-debt-tracker.md`](exec-plans/tech-debt-tracker.md).
When a quality gap is identified here, add a corresponding TD item there.
