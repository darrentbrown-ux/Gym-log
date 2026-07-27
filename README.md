# Gym Log

A simple Android app to track and log workouts. Built with Kotlin + Jetpack Compose + Room.

## Features

- **Exercises** — add and name workouts under four categories:
  - **Weight machines** — tricep, hip abductor, pull down, deltoid fly, …
  - **Cardio machines** — treadmill, stationary bike, stair master, elliptical
  - **Calisthenics** — pull-ups, dips, push-ups, planks, sit-ups, captain's chair
  - **Free weights** — deadlift, bicep curls, …
- **Machine settings** — per-exercise configurable fields (seat height, arm height, arm position, speed, incline, duration, etc.) that appear automatically when you log a set.
- **Dropdown selection** when adding an exercise to a workout — uses the catalog of previously entered exercises.
- **Log workouts** — start a session, prefill from a preset, and edit weights, reps, settings per set as you go. Add new exercises mid-workout.
- **Multiple preset routines** — define reusable routines (e.g. "Push day", "Legs") with default weight/reps/sets.
- **History** — every session is saved with date, name, and all sets.
- **Export workout log (CSV)** — shared via the standard Android share sheet so you can email it, save to Drive, etc.
- **Backup user settings (JSON)** — full dump of exercises, machine settings, presets and sessions for restore on a new device.

## Build

Headless build (no Android Studio required):

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug
# APK -> app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Requirements: JDK 17, Android SDK with platform 36 and build-tools 35.0.0.

## Project layout

```
app/src/main/java/com/gymlog/app/
  data/                # Room entities, DAOs, JSON/CSV codecs, repository
  ui/                  # ViewModel, navigation, theme
    components/        # shared composables
    screens/           # one Composable per screen
  MainActivity.kt      # entry point
```

## Stack

- Kotlin 2.0.21
- AGP 8.7.3, Gradle 8.9, JDK 17
- Compose BOM 2024.12.01 (Material 3 1.3.x)
- Room 2.6.1
- Navigation Compose 2.8.5
- minSdk 26, targetSdk 36, compileSdk 36
