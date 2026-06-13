# Development Plan

## Classification

- Task type: `bugfix`
- Scope: medium selected-terminal-targeting and packaged-manual-verification slice

## Exact References

- `.project-memory/canon/current-state.md`
- `.project-memory/verify-commands.md`
- `README.md`
- `docs/task-slices.md`
- `src/main/kotlin/com/myrpasko/commentsplugin/diff/SideBySideDiffReviewController.kt`
- `src/main/kotlin/com/myrpasko/commentsplugin/terminal/TerminalPromptInserter.kt`
- `src/test/kotlin/com/myrpasko/commentsplugin/**`

## Write Scope

- Harden prompt insertion so the plugin writes only to the selected terminal tab when that tab can be resolved safely.
- Preserve clipboard fallback when the selected terminal cannot be resolved or is unsupported.
- Add focused regression coverage for selected-terminal targeting and fallback behavior where the surface is testable.
- Update repo docs and canon after verification.

## Forbidden Moves

- Do not widen this slice into persistent indicator work.
- Do not widen this slice into more diff viewer support.
- Do not add unified diff support.
- Do not silently create a terminal session when no compatible active session exists.
- Do not write into a background terminal tab when the selected terminal tab is unresolved.
- Do not claim terminal compatibility beyond the IDE/runtime paths actually verified.

## Verification Surface

- `env JAVA_HOME=/Users/myroslavpasko/Applications/WebStorm.app/Contents/jbr/Contents/Home LOCAL_IDE_PATH=/Users/myroslavpasko/Applications/WebStorm.app ./gradlew test`
- `env GRADLE_USER_HOME=.gradle-local JAVA_HOME=/Users/myroslavpasko/Applications/WebStorm.app/Contents/jbr/Contents/Home LOCAL_IDE_PATH=/Users/myroslavpasko/Applications/WebStorm.app ./gradlew buildPlugin`
- manual verification in the active PyCharm or WebStorm terminal path once the selected-terminal-targeting change lands
- manual verification of side-by-side gutter indicators, including left-side deleted-line anchors, on the packaged build

Use `/.project-memory/verify-commands.md` as the exact command source if the command surface changes.

## Success Criteria

1. Prompt insertion succeeds for the selected active terminal implementation when that tab is supported and resolvable.
2. If the selected terminal tab cannot be resolved safely, the plugin falls back cleanly to clipboard copy instead of writing into another terminal.
3. Automated verification passes and packaged manual verification is rerun for terminal insertion plus gutter indicators.
4. Repo docs and canon describe the terminal support boundary accurately.

## Slice Restrictions

### Active Slice

- inspect and widen terminal widget resolution
- harden selected-terminal targeting
- preserve clipboard fallback behavior
- rerun packaged manual verification and refresh docs

### Deferred

- persistent comment indicators
- unified diff support
- persisted review sessions
