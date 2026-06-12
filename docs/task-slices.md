# Task Slices

## Slice 1. Research and Planning

- Inspect repo and memory bundle
- Attach repo to Memory Core V5
- Research JetBrains platform APIs and constraints
- Write research report
- Write PRD
- Write development plan
- Write testing strategy

## Slice 2. Project Scaffold

- Create Gradle Kotlin DSL plugin project
- Configure IntelliJ Platform Gradle Plugin `2.x`
- Add plugin metadata/resources
- Add bundled plugin dependencies required for Git diff and Terminal integration
- Add base package layout

## Slice 3. Core Domain

- Implement `ReviewComment` model
- Implement line/file formatting helpers
- Implement prompt prefix settings service
- Implement in-memory review comment store
- Implement prompt builder
- Implement terminal insertion abstraction and fallback contract

## Slice 4. Diff UI Integration

- Discover supported text diff viewers
- Identify changed lines on the right/new side
- Render hover-aware gutter affordance
- Open comment dialog
- Support add/update/remove behavior
- Show bottom `Discard` and `Submit` actions

## Slice 5. Terminal Submission

- Resolve active terminal tool window/session
- Insert generated prompt into active session
- Handle missing/unsupported terminal cases safely

## Slice 6. Testing

- Prompt builder tests
- Comment store tests
- Formatting tests
- Settings default/update tests
- Discard behavior tests
- Edge case tests

## Slice 7. Documentation and Closeout

- Update README
- Curate verification commands
- Update current state canon
- Create Obsidian project notes
- Run build/tests
- Commit
- Open PR
- Merge if stable
