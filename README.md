# Comments Plugin

JetBrains IDE plugin prototype for pre-commit review comments on local diffs.

## What It Does

- Adds a hover-triggered plus icon to visible side-by-side diff lines in supported diff views, including deleted lines on the left side.
- Opens a small line-anchored comment popup near the selected code change.
- Lets you submit a comment from the popup with `Cmd+Enter` on macOS or `Ctrl+Enter` on other platforms, with an inline hint below the input.
- Collects comments across files in a project-scoped in-memory review session.
- Builds a combined prompt in this format:

```text
Fix these comments:

[src/components/Button.tsx:42] - Rename this variable.
[src/hooks/useUser.ts:18] - Handle the loading state explicitly.
[src/api/client.ts:27 deleted] - Keep this branch or replace it with an equivalent guard.
```

- Inserts that prompt into the active integrated terminal session.
- Falls back to copying the prompt to the clipboard if no compatible terminal session is available.
- Clears the stored review comments after a successful terminal insertion.

## Current Scope

MVP support is still intentionally narrow, but local diff integration now covers the main side-by-side text diff entry points used for local Git review:

- supported diff viewers: `SimpleDiffViewer` and compatible local side-by-side change-list diff viewers when the IDE exposes changed ranges through the standard text diff APIs
- commentable lines: any visible line on the current/right side plus deleted lines on the left side
- popup submit shortcut: `Cmd+Enter` on macOS, `Ctrl+Enter` elsewhere
- prompt prefix setting: application-scoped
- comment persistence: in-memory only for the current IDE session

## Settings

The plugin adds a settings page under `Tools > Comments Plugin`.

Default prefix:

```text
Fix these comments:
```

Additional setting:

- `Show success confirmation after prompt insertion` is enabled by default and can be turned off either from Settings or directly from the success dialog.

## Development

The project uses:

- Kotlin
- Gradle Kotlin DSL
- IntelliJ Platform Gradle Plugin `2.x`

For local development on this machine, the build can target an installed JetBrains IDE via `LOCAL_IDE_PATH`. The commands below use the local WebStorm installation.

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

### Package For The Current IDE

Build an installable plugin ZIP against the IDE you actually want to use:

```bash
env GRADLE_USER_HOME=.gradle-local \
  JAVA_HOME=/Users/myroslavpasko/Applications/WebStorm.app/Contents/jbr/Contents/Home \
  LOCAL_IDE_PATH=/Users/myroslavpasko/Applications/WebStorm.app \
  ./gradlew buildPlugin
```

You can also pass the IDE location as a Gradle property instead of an environment variable:

```bash
env GRADLE_USER_HOME=.gradle-local \
  JAVA_HOME=/Users/myroslavpasko/Applications/WebStorm.app/Contents/jbr/Contents/Home \
  ./gradlew -PlocalIdePath=/Users/myroslavpasko/Applications/WebStorm.app buildPlugin
```

The installable artifact is written to:

```text
build/distributions/comments-plugin-0.1.0.zip
```

Install it in your IDE with `Settings/Preferences > Plugins > gear icon > Install Plugin from Disk...`.

## Tests Included

- prompt generation
- stable prompt ordering
- blank comment filtering
- settings default value
- settings update behavior
- store upsert/remove/discard behavior
- adapter classification for supported vs unsupported diff viewers
- action-surface fallback selection for bottom vs toolbar placement
- popup shortcut text and compact action-panel font sizing
- terminal resolution fallback from selected frontend tabs to generic widget TTY accessors
- prompt settings persistence for the success-confirmation preference

## Known Limitations

- Unified diff view is not implemented yet.
- Some local side-by-side diff viewers may still be unsupported if the IDE does not expose changed-line ranges through the text diff APIs used by this plugin.
- Deleted-line comments are represented in prompts with a `deleted` suffix rather than a richer diff identity.
- Existing comments now stay reachable through a dedicated gutter message marker on changed lines, but the current packaged `main` build still needs manual re-verification in the IDE.
- Terminal insertion now tries the selected frontend terminal tab first, retries through the generic `TerminalWidget` TTY-accessor path when the frontend send path is unavailable, and still falls back to clipboard copy when no compatible active session exists. Active current-IDE terminal flows still need manual re-verification after this compatibility hardening.
- Review comments are not persisted across IDE restarts.
- Prompt submission to the terminal does not auto-send an Enter keystroke; it inserts/pastes the prompt content.

## Future Improvements

- wider local side-by-side diff viewer coverage across the remaining unsupported viewer variants
- unified diff support
- stronger persistent comment markers if the current gutter indicator still proves too subtle in real diff views
- improved terminal session compatibility across current JetBrains terminal implementations
- richer diff context for renamed/deleted lines
- project-level review session persistence
- stronger sandbox/manual integration testing for diff UI behavior
