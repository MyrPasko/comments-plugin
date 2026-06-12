# Comments Plugin

JetBrains IDE plugin prototype for pre-commit review comments on local diffs.

## What It Does

- Adds a hover-triggered plus icon to changed lines in supported diff views.
- Opens a small comment dialog with `Remove`, `Cancel`, and `Comment`.
- Collects comments across files in a project-scoped in-memory review session.
- Builds a combined prompt in this format:

```text
Fix these comments:

[src/components/Button.tsx:42] - Rename this variable.
[src/hooks/useUser.ts:18] - Handle the loading state explicitly.
```

- Inserts that prompt into the active integrated terminal session.
- Falls back to copying the prompt to the clipboard if no compatible terminal session is available.

## Current Scope

MVP support is intentionally narrow:

- supported diff viewer: side-by-side text diff (`SimpleDiffViewer`)
- prompt prefix setting: application-scoped
- comment persistence: in-memory only for the current IDE session

## Settings

The plugin adds a settings page under `Tools > Comments Plugin`.

Default prefix:

```text
Fix these comments:
```

## Development

The project uses:

- Kotlin
- Gradle Kotlin DSL
- IntelliJ Platform Gradle Plugin `2.x`

For local development on this machine, the build can target the installed WebStorm instance instead of downloading a remote IDE distribution.

### Test

```bash
env JAVA_HOME=/Users/myroslavpasko/Applications/WebStorm.app/Contents/jbr/Contents/Home \
  LOCAL_IDE_PATH=/Users/myroslavpasko/Applications/WebStorm.app \
  ./gradlew test
```

### Build

```bash
env JAVA_HOME=/Users/myroslavpasko/Applications/WebStorm.app/Contents/jbr/Contents/Home \
  LOCAL_IDE_PATH=/Users/myroslavpasko/Applications/WebStorm.app \
  ./gradlew build
```

If `LOCAL_IDE_PATH` is not set, the Gradle build falls back to a downloaded IntelliJ Platform target.

## Tests Included

- prompt generation
- stable prompt ordering
- blank comment filtering
- settings default value
- settings update behavior
- store upsert/remove/discard behavior

## Known Limitations

- Unified diff view is not implemented yet.
- Existing comments are not rendered as persistent inline badges after creation.
- Terminal insertion targets compatible classic shell-backed terminal widgets only.
- Review comments are not persisted across IDE restarts.
- Prompt submission does not auto-send an Enter keystroke; it inserts/pastes the prompt content.

## Future Improvements

- unified diff support
- persistent inline comment markers
- richer diff context for renamed/deleted lines
- project-level review session persistence
- stronger sandbox/manual integration testing for diff UI behavior
