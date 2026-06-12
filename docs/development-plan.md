# Development Plan

## Classification

- Task type: `feature`
- Scope: medium-to-large greenfield plugin slice

## Exact References

- `docs/research/jetbrains-platform-research.md`
- `docs/PRD.md`
- `README.md`
- `.gitignore`
- Gradle/plugin scaffold files to be created at repo root
- `src/main/kotlin/**`
- `src/main/resources/**`
- `src/test/kotlin/**`
- `src/test/resources/**` if needed
- `.project-memory/canon/current-state.md`
- `.project-memory/verify-commands.md`

## Write Scope

- Scaffold the plugin project from current JetBrains best practices.
- Implement the MVP plugin and tests.
- Update repo docs and README.
- Curate memory canon files only for project reality and verification commands.
- Add Obsidian project notes in the selected project location.

## Forbidden Moves

- Do not add unrelated product features.
- Do not persist review comments across restarts in MVP.
- Do not hardcode absolute paths other than the user-provided memory bundle path already used.
- Do not claim support for all diff viewer variants if only text diff viewers are implemented.
- Do not silently create a new terminal session on submit if a user expects insertion into an existing one.
- Do not rely on undocumented platform internals without a graceful fallback path.

## Verification Surface

Planned verification commands:

- `./gradlew test`
- `./gradlew build`
- any additional focused check task if introduced by the scaffold

These will be written into `/.project-memory/verify-commands.md` once the build surface exists.

## Success Criteria

1. Project builds as a valid IntelliJ Platform plugin.
2. Settings page exists and persists prompt prefix.
3. Review comment session state supports add, update/remove, list, and discard.
4. Prompt builder formats output correctly across edge cases.
5. Supported diff viewers expose the add-comment interaction on changed lines.
6. Submit inserts text into a compatible open terminal session or shows a clear fallback.
7. Tests cover the required core behaviors.
8. README explains usage, setup, testing, limitations, and future work.

## Slice Restrictions

### Slice 1

Documentation and scaffold only:

- research report
- PRD
- plan
- task slicing
- testing strategy
- project setup

### Slice 2

Core business logic:

- settings
- models
- store
- formatter
- terminal abstraction

### Slice 3

JetBrains integration:

- diff integration
- gutter/comment dialog
- bottom actions
- settings UI

### Slice 4

Verification and closeout:

- tests
- build fixes
- README
- memory closeout
- git/PR workflow
