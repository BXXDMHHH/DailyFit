# DailyFit repository instructions

## Product authority
- `docs/PRD.md` is the highest-priority business specification.
- `docs/TECH_DESIGN.md` defines architecture and persistence decisions.
- `docs/DEVELOPMENT_STATUS.md` records actual implementation status.
- Do not silently change confirmed product rules.

## Engineering rules
- Kotlin, Jetpack Compose, Material 3, MVVM, ViewModel + StateFlow, Room, Coroutines, Navigation Compose.
- Reuse existing code; implement the requested stage without unrelated refactoring.
- Use stable UUID identifiers for persisted business records; never use list positions as IDs.
- Room is the source of truth. Do not present in-memory sample data as persisted functionality.
- Export Room schemas and add explicit migrations. Never enable destructive migrations.
- Keep fixed habits and temporary tasks in separate data models and statistics.
- Preserve daily plan snapshots when weekly templates change.
- Save in-progress workout data incrementally for process-death recovery.
- Completed training history is permanently read-only: no update/delete UI, Repository method, or DAO path. Protect it at database level and test it.
- Snapshot exercise names and set values into saved history.

## Stage workflow
1. Read PRD, technical design, development status, and relevant code.
2. Inspect dependencies and affected data flows.
3. Implement the complete requested stage.
4. Run Gradle compilation and relevant tests; fix failures.
5. Check regressions.
6. Update development status with changes, tests, known issues, and next stage.
7. Report completion, key modules, test results, unresolved issues, and next-stage recommendation.

## Data safety
If an operation could lose existing user data or contradict a confirmed rule, stop and explain before proceeding.
