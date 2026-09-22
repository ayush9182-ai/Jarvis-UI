# Jarvis Android assistant

This repository now contains the native Android app starter for Jarvis 2.0.

## Included

- Boss-style screen and futuristic dark UI
- voice input support via Android SpeechRecognizer
- text command processing for greeting, app opening, search, call, SMS and timers
- Gemini API key entry via a dialog
- foreground service scaffold for Boss mode
- app permissions for microphone, SMS, call, notifications and foreground service access

## Important note

A real always-on `Hey Jarvis` wake-word on Android requires a foreground microphone service and device-level permissions. Browsers cannot provide locked-screen, background microphone wake-word behavior. This project is the correct native app foundation for that next step.

## Build

Open the project in Android Studio and run the app. If needed, use:

./gradlew assembleDebug

