# DailyFit
DailyFit is a local-first Android app for habits, temporary tasks, workout planning, exercise management, workout logging, and read-only training history.

## Current state
This repository contains the Android project foundation: Kotlin, Compose, Material 3, Navigation Compose, ViewModel + StateFlow, Room setup, a three-tab UI shell, a pure habit-frequency rule, unit tests, and GitHub Actions. Business modules are staged; current screens are scaffolding, not completed habit/task/workout functionality.

## Requirements
- Android Studio and Android SDK Platform 37
- JDK 17
- Gradle 9.6.0 (the repository bootstrap downloads the pinned distribution)
- Android Gradle Plugin 9.4.0

## Build
Windows: `gradlew.bat testDebugUnitTest` then `gradlew.bat assembleDebug`.
macOS/Linux: `chmod +x gradlew && ./gradlew testDebugUnitTest && ./gradlew assembleDebug`.
The first build downloads Gradle and Android dependencies.

## Confirmed product rules
- App data stays local; no account, server, or cloud sync in V1.0.
- Fixed habits support daily, weekdays, and custom weekdays.
- Temporary tasks belong only to their creation date and have separate statistics.
- Single-day workout changes must not alter the weekly template.
- In-progress workouts are editable and recoverable.
- Completed training history is permanently read-only and cannot be edited or deleted.
- Saved history contains exercise-name and set-value snapshots independent of the exercise library.
