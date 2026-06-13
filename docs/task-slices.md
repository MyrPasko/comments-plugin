# Task Slices

## Current Baseline

- `main` contains the original MVP merged from PR `#1`.
- The current `main` working tree supports `SimpleDiffViewer` plus compatible local side-by-side change-list diff viewers.
- Installable plugin packaging is verified through `buildPlugin` against the local WebStorm runtime.

## Completed Slices

- current-IDE portability and installation
- local side-by-side diff checkpoint landing
- lightweight line-anchored comment composer and submit cleanup

## Slice 1. Terminal Compatibility Expansion

- inspect the active terminal widget types exposed by current PyCharm/WebStorm builds
- extend widget resolution beyond classic shell-backed terminals if a safe API path exists
- keep clipboard fallback as the default safe path for unsupported terminals
- add regression coverage for terminal detection and fallback behavior

## Slice 2. Persistent Comment Indicators

- verify and harden the non-hover indicator for lines that already have review comments
- keep add/edit/remove flows reachable from the indicator path
- refresh indicators after add, update, remove, and discard actions
- add focused tests or seam points for indicator refresh logic

## Slice 3. Post-Stabilization Options

- evaluate unified diff support as a separate slice
- decide whether review comments should persist across IDE restart
- reassess the manual diff UI verification strategy after the first stabilization slices land
