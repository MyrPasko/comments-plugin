# Product Requirements Document

## Product

Comments Plugin

## Problem

JetBrains IDEs provide rich diff tooling, but they do not provide a simple pre-commit review flow where a developer can leave multiple inline review-style comments on local diff lines, then send those comments directly into an already-running coding agent in the integrated terminal.

The result is friction:

- comments are scattered or temporary,
- correction prompts are manually assembled,
- the review loop starts too late, often after a push or PR.

## Goal

Provide an in-IDE pre-commit review workflow for changed lines in diff views:

1. add inline comments on changed lines,
2. collect comments across files,
3. generate a single structured prompt,
4. insert that prompt into the current integrated terminal session.

## Users

- Individual developers using Claude Code CLI, Codex CLI, or similar terminal agents inside JetBrains IDEs
- Tech leads reviewing generated code locally before commit
- Teams experimenting with AI-assisted pre-PR review loops

## Non-Goals

- Multi-user collaborative review
- GitHub/GitLab review synchronization
- Persistent comment history across IDE restarts
- Automatic fix application
- Semantic diff reconstruction beyond the current open diff context

## User Stories

1. As a developer reviewing a local diff, I want a plus icon on changed lines so I can leave review comments without leaving the diff.
2. As a reviewer, I want comments collected across multiple files so I can submit one coherent correction prompt.
3. As a user of an integrated terminal agent, I want the prompt inserted into the active terminal instead of copied manually.
4. As a user, I want the prompt prefix configurable from Settings.
5. As a user, I want `Discard` to clear the current review session quickly.

## Functional Requirements

### Diff Commenting

- Only changed lines in supported text diff viewers are commentable.
- In side-by-side diff viewers, any visible current/right-side line may be commented and deleted lines may be commented from the left side.
- A plus icon appears on hover over the target line gutter area.
- Clicking the plus icon opens a small comment input near the selected diff line.
- The inline input supports keyboard submission with `Cmd+Enter` on macOS and `Ctrl+Enter` on other platforms.
- The inline input contains:
  - text input,
  - `Remove`,
  - `Cancel`,
  - `Comment`.
- Empty or whitespace-only comments are rejected.

### Comment Model

Each comment must store:

- relative file path,
- target line number,
- display label for deleted-line anchors when the rendered prompt needs to distinguish them,
- comment text,
- enough diff/editor context to identify where the comment came from,
- a stable in-memory id for mutation/removal during the active review session.

### Review Session

- Multiple comments may exist across multiple files.
- Comments are session-scoped in memory.
- `Discard` clears all collected comments.

### Prompt Generation

- `Submit` produces one prompt.
- The prompt starts with a configurable prefix.
- Each comment is rendered as:

```text
[relative/path/to/file:line] - comment text
```

- Deleted-line comments may render with an explicit deleted-line label, for example:

```text
[relative/path/to/file:27 deleted] - comment text
```

### Terminal Insertion

- On submit, the plugin attempts to insert the generated prompt into the active integrated terminal.
- If no compatible terminal session is active, the plugin must fail gracefully and provide a fallback path.

### Settings

- Add application settings for prompt prefix.
- Default value:

```text
Fix these comments:
```

## UX Requirements

- Minimal friction
- Clear feedback on comment creation, update, submit, discard, and terminal insertion failure
- No heavy modal workflow; comment entry must stay close to the code change
- The comment input should expose the keyboard submit shortcut near the text field.

## Quality Requirements

- Modular architecture
- Isolated business logic
- Graceful handling of unsupported viewers or missing terminal integration
- Strong test coverage for formatting and state logic
- Installable portability: the plugin must be packageable as a ZIP that can be added to the developer's current JetBrains IDE and run there.

## Success Criteria

- A user can add comments on changed lines in a supported diff viewer.
- The plugin can collect and format comments into one prompt.
- The prompt prefix can be changed in Settings.
- `Discard` clears the session.
- `Submit` inserts the prompt into a compatible open terminal session or provides a safe fallback.
- A developer can build an installable plugin ZIP and add it to the current IDE with `Install Plugin from Disk...`.

## Known Constraints

- Diff viewer customization uses less-documented platform APIs.
- Terminal insertion depends on bundled Terminal plugin internals/API surface.
- Cross-IDE compatibility is conditional on bundled platform plugins and viewer behavior.
