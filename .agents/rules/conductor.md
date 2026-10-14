# Conductor Global Rules

- Automatically engage the `conductor-orchestrator` skill for all multi-step feature implementations.
- Execute tasks autonomously across Code1 and Code2 roles without asking for approval on intermediate file edits.
- Keep builds green: run `./gradlew :fluxfoxus:testDebugUnitTest` after logic additions and `./gradlew :fluxfoxus:assembleDebug` after UI additions.
