# DESIGN.md

Design intent for `nf-core-utils`.

## Goals
- Reliable reusable utilities for nf-core pipelines.
- Predictable behavior across plugin versions.
- Maintainable internals with strong tests.

## Design Tenets
- Thin extension interface, rich utility modules.
- Fail clearly; avoid silent fallbacks.
- Keep dependencies minimal.
