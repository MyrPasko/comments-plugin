# Development Plan

## Classification

- Task type: `feature`
- Scope: medium terminal-compatibility slice

## Exact References

- `.project-memory/canon/current-state.md`
- `.project-memory/verify-commands.md`
- `README.md`
- `docs/task-slices.md`
- `src/main/kotlin/com/myrpasko/commentsplugin/terminal/TerminalPromptInserter.kt`
- `src/main/kotlin/com/myrpasko/commentsplugin/diff/DiffActionPanel.kt`
- `src/test/kotlin/com/myrpasko/commentsplugin/**`

## Write Scope

- Extend terminal widget detection beyond the currently supported classic shell-backed path when a safe API path exists.
- Preserve clipboard fallback for unsupported or missing terminal sessions.
- Add focused regression coverage for terminal widget resolution and fallback behavior where the surface is testable.
- Update repo docs and canon after verification.

## Forbidden Moves

- Do not widen this slice into persistent indicator work.
- Do not widen this slice into more diff viewer support.
- Do not add unified diff support.
- Do not silently create a terminal session when no compatible active session exists.
- Do not claim terminal compatibility beyond the IDE/runtime paths actually verified.

## Verification Surface

- `env JAVA_HOME=/Users/myroslavpasko/Applications/WebStorm.app/Contents/jbr/Contents/Home LOCAL_IDE_PATH=/Users/myroslavpasko/Applications/WebStorm.app ./gradlew test`
- `env GRADLE_USER_HOME=.gradle-local JAVA_HOME=/Users/myroslavpasko/Applications/WebStorm.app/Contents/jbr/Contents/Home LOCAL_IDE_PATH=/Users/myroslavpasko/Applications/WebStorm.app ./gradlew buildPlugin`
- manual verification in the active PyCharm or WebStorm terminal path once the widget-resolution change lands

Use `/.project-memory/verify-commands.md` as the exact command source if the command surface changes.

## Success Criteria

1. Prompt insertion succeeds for the active current-IDE terminal implementation when a safe supported widget is present.
2. Unsupported terminal implementations still fall back cleanly to clipboard copy.
3. Automated verification passes and manual terminal verification is recorded when rerun.
4. Repo docs and canon describe the terminal support boundary accurately.

## Slice Restrictions

### Active Slice

- inspect and widen terminal widget resolution
- preserve clipboard fallback behavior
- add verification and docs for the supported terminal paths

### Deferred

- persistent comment indicators
- unified diff support
- persisted review sessions
