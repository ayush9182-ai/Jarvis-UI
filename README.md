# Jarvis Android Native App

This repository is now aligned to the real Android-native version of the Jarvis assistant.

## Project structure

- `app/` contains the Android application source
- `settings.gradle.kts` bootstraps the Android project
- `app/build.gradle.kts` configures the Android app and dependencies
- `app/src/main/java/...` contains the Kotlin logic for the assistant
- `app/src/main/res/...` contains the custom JARVIS HUD branding and launcher icon

## Main build command

```bash
./gradlew assembleDebug
```

Then install the generated APK or run it in Android Studio.
