# JetBrains Platform Research

Date: 2026-06-12

## Executive Summary

This repository is effectively greenfield, so the safest starting point is the current JetBrains plugin template stack:

- Kotlin
- Gradle Kotlin DSL
- IntelliJ Platform Gradle Plugin `2.x`
- JUnit-based platform tests

The product requirement is feasible, but not with a clean public "review comments in diff" API. The documented public APIs cover gutter icons, settings, persistent services, actions, tool windows, testing, signing, and publishing. The critical diff-view customization and terminal insertion pieces rely on platform classes and bundled-plugin APIs that are available in practice but are less formally documented. That means the implementation should prefer:

1. business logic isolated from JetBrains UI classes,
2. a thin integration layer around diff viewers and the terminal,
3. graceful fallback behavior where platform APIs are missing or unavailable.

## Findings

### 1. Build and Project Setup

- JetBrains recommends the IntelliJ Platform Gradle Plugin `2.x` as the current build stack.
- The official template currently uses:
  - plugin id `org.jetbrains.intellij.platform`
  - Kotlin JVM
  - Gradle settings plugin `org.jetbrains.intellij.platform.settings`
  - Java runtime 17 minimum for the Gradle plugin
  - IntelliJ Platform 2023.3+ minimum for the Gradle plugin itself
- The current public template also shows a recent baseline on IntelliJ IDEA `2025.2.6.2`, which is a reasonable greenfield target.

Recommendation:

- Base the project on the official template structure, but keep it intentionally smaller than the template’s full CI/release surface.
- Target a recent IntelliJ IDEA build with bundled Git and Terminal plugins available.

### 2. Gutter UI and Hover Affordances

- Official docs confirm gutter icons are implemented through editor marker mechanisms such as line markers and related gutter renderers.
- The official `LineMarkerProvider` docs are aimed at PSI-backed editors, not diff viewers.
- For regular files, the gutter model is stable and well-supported.
- For diff viewers, the plugin should not depend on PSI line markers. Instead, it should attach editor markup directly to the diff editors and render a custom `GutterIconRenderer` for changed lines.

Implication:

- The plus icon on hover is feasible, but it should be implemented through diff-editor markup/highlighters, not via `LineMarkerProvider`.

### 3. Diff Viewer Integration

- JetBrains public SDK documentation is thin here.
- Public evidence from the open-source RefactorInsight plugin shows that diff windows can be augmented with plugin-provided UI and behavior.
- The platform supports diff viewers and viewer extension points/classes in source, but the API surface is materially less documented than settings/actions/services.

Practical conclusion:

- Use a diff-view integration layer that discovers compatible text diff viewers, inspects changed ranges, and decorates only changed lines on the right/new side.
- Treat this integration as fragile infrastructure:
  - keep it in one package,
  - minimize coupling to concrete viewer subclasses,
  - add defensive guards around unsupported viewers.

### 4. State Management and Persistence

- Official docs recommend `PersistentStateComponent` for plugin state.
- Since `SerializablePersistentStateComponent` is the recommended modern option, it is the right fit for application-level settings.
- Extension instances themselves should not own persistent state; a separate service should.

Recommendation:

- Application-level settings service for prompt prefix.
- Project-level review comment store service for current session comments.
- Keep comment persistence in memory for the active IDE session first; only settings need durable persistence in MVP.

Rationale:

- The product requirement asks for `Discard` and `Submit` on current comments, not long-term saved reviews.
- Persisting comment drafts across IDE restarts introduces staleness and mapping problems early.

### 5. Settings UI

- Official docs recommend `applicationConfigurable` or `projectConfigurable` plus a `Configurable` implementation.
- The prompt prefix is application-scoped behavior, so application settings are the right choice.

Recommendation:

- Add one application settings page under `Tools`.
- Expose a single multiline text field for the prompt prefix.
- Keep the default exactly:

```text
Fix these comments:
```

### 6. Actions and Bottom Diff Actions

- Official action-system docs emphasize that actions must be stateless and fast in `update()`.
- The requirement asks specifically for `Discard` and `Submit` at the bottom of the diff tab/window.

Practical interpretation:

- The implementation should inject a bottom action panel into compatible diff viewers when possible.
- If diff-specific bottom injection proves unstable for some viewer types, the plugin should still expose the same actions from:
  - the diff viewer toolbar/action group,
  - and/or a small bottom component attached only to supported text diff viewers.

Engineering note:

- A bottom component is more controllable than trying to emulate GitHub’s exact pending-review UI in the gutter alone.

### 7. Terminal Integration

- There is no prominent official SDK guide for "insert text into the active integrated terminal" comparable to the settings or services docs.
- Platform source for the bundled Terminal plugin exposes `TerminalToolWindowManager` as a project service and terminal widget creation APIs.
- This area appears supported in practice but less stable as a public API contract than core platform services.

Recommendation:

- Depend on the bundled Terminal plugin as an optional/bundled plugin dependency.
- Discover the active terminal widget through `TerminalToolWindowManager`.
- Prefer inserting text into the selected/open terminal tab instead of launching a new shell session.
- If no compatible terminal session is available, fail gracefully with a notification and copy the prompt to the clipboard as fallback.

This fallback is necessary because the user’s requirement assumes an agent is already running in the terminal, but the IDE may have:

- no terminal tool window,
- a terminal implementation variant not exposing the expected widget contract,
- or no active session.

### 8. Compatibility Across JetBrains IDEs

- The public docs and template target the IntelliJ Platform generally, but actual behavior varies by bundled plugins and product SKU.
- The plugin concept depends on:
  - VCS diff viewers,
  - the bundled Git integration,
  - the bundled Terminal plugin.

Implication:

- The plugin can be cross-IDE only where those capabilities exist.
- IntelliJ IDEA, WebStorm, PyCharm Professional, GoLand, PhpStorm, Rider, and similar products are plausible targets.
- The plugin should declare bundled plugin dependencies explicitly and fail closed when required capabilities are unavailable.

### 9. Testing Strategy

- JetBrains testing docs recommend model-level functional tests over Swing-heavy UI tests.
- They explicitly prefer real platform components over heavy mocking.

Recommendation:

- Put prompt generation, comment normalization, formatting, and state rules into plain Kotlin classes with straightforward unit tests.
- Use light platform tests for settings persistence/service wiring where useful.
- Do not try to build brittle full diff-window UI tests in the first slice.

### 10. Publishing and Signing

- Signing is required for Marketplace distribution.
- The first Marketplace upload must be manual.
- Use env vars for signing and publishing credentials; never commit them.

This is not required for MVP development but should be baked into the Gradle configuration layout early enough that the project does not need structural rework later.

## Recommended Architecture

### Core

- `PromptSettingsService`
- `ReviewCommentStore`
- `PromptBuilder`
- `CommentFormatter`
- `DiffLocation` / `ReviewComment` model

### JetBrains Integration

- `DiffReviewExtension`
- `DiffCommentController`
- `DiffBottomActionsPanel`
- `CommentInputDialog`
- `TerminalPromptInserter`
- `PromptSettingsConfigurable`

### Testing

- pure unit tests for formatting and state behavior
- light platform tests for settings persistence and service defaults

## Known Risks

1. Diff viewer APIs are less documented than mainstream editor/plugin APIs.
2. Hover-only gutter affordances can be visually inconsistent between IDE themes and diff implementations.
3. Terminal APIs are bundled-plugin APIs, not the cleanest public SDK layer.
4. Mapping comments to renamed/deleted lines is inherently weaker in an MVP that does not persist full diff identities.
5. Bottom-of-diff action placement may require a pragmatic compromise if a viewer subtype does not expose a stable component insertion point.

## Practical Decision

Implement the exact requested workflow for supported text diff viewers and supported terminal environments, with explicit graceful fallback where JetBrains platform limits make the behavior unavailable.

## Sources

Official JetBrains documentation:

- [Line Marker Provider](https://plugins.jetbrains.com/docs/intellij/line-marker-provider.html)
- [Persisting State of Components](https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html)
- [Settings Guide](https://plugins.jetbrains.com/docs/intellij/settings-guide.html)
- [Services](https://plugins.jetbrains.com/docs/intellij/plugin-services.html)
- [Tool Windows](https://plugins.jetbrains.com/docs/intellij/tool-windows.html)
- [Action System](https://plugins.jetbrains.com/docs/intellij/action-system.html)
- [Testing Overview](https://plugins.jetbrains.com/docs/intellij/testing-plugins.html)
- [IntelliJ Platform Gradle Plugin (2.x)](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html)
- [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html)
- [Publishing a Plugin](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html)

GitHub repositories and source/examples:

- [JetBrains IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- [Template `build.gradle.kts` raw](https://raw.githubusercontent.com/JetBrains/intellij-platform-plugin-template/main/build.gradle.kts)
- [Template `settings.gradle.kts` raw](https://raw.githubusercontent.com/JetBrains/intellij-platform-plugin-template/main/settings.gradle.kts)
- [RefactorInsight](https://github.com/JetBrains-Research/RefactorInsight)
- [TerminalToolWindowManager source](https://raw.githubusercontent.com/JetBrains/intellij-community/master/plugins/terminal/src/org/jetbrains/plugins/terminal/TerminalToolWindowManager.java)

Community signal:

- I attempted targeted search for Reddit/X/community discussions on diff-view and terminal APIs. The accessible results were low-signal and did not provide stronger implementation guidance than the official docs and public source above. For this slice, the reliable evidence is the SDK docs plus open-source platform/plugin code.
