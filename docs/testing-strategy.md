# Testing Strategy

## Goals

- Maximize confidence in product logic without building brittle IDE UI tests first.
- Keep JetBrains-specific UI integration thin and defensively coded.

## Test Layers

### 1. Pure Unit Tests

Primary coverage target:

- prompt generation
- prefix handling
- path and line formatting
- whitespace rejection
- multi-comment ordering behavior
- discard behavior
- update/remove semantics

These tests should run without the IntelliJ test framework wherever possible.

### 2. Light Platform Tests

Use JetBrains platform tests for:

- settings service default value
- settings persistence/update behavior
- service wiring where plain unit tests are insufficient

### 3. Deferred UI/Integration Testing

For MVP, do not build a full fragile automated diff-window UI suite.

Instead:

- keep diff integration logic decomposed,
- manually verify supported diff viewers in a sandbox IDE,
- document unsupported cases.

## Required Cases

### Prompt Generation

- no comments
- one comment
- multiple comments in one file
- multiple comments across files
- custom prefix

### Formatting

- relative path with nested directories
- valid line formatting
- blank or whitespace-only comments rejected

### Store Behavior

- add comment
- replace/update comment
- remove comment
- discard all comments
- stable ordering expectation for rendered output

### Settings

- default prefix value
- updated prefix persisted and read back

### Edge Conditions

- renamed/deleted/unsupported diff cases do not crash logic
- terminal unavailable path returns a controlled failure result

## Manual Verification Checklist

1. Run sandbox IDE.
2. Open a Git diff with changed text lines.
3. Hover gutter and confirm plus affordance appears.
4. Add comments in multiple files.
5. Remove one comment.
6. Submit comments with an active terminal session open.
7. Confirm prompt text is inserted into terminal.
8. Discard remaining comments.
9. Change settings prefix and repeat submit.

## Acceptance Standard

- Core logic must be covered by automated tests.
- Build must pass.
- Manual sandbox verification must be recorded in final implementation notes.
