# Jarvis Android app

This repository now contains the native Android project under `app/`.

## Features

- Text commands and one-tap voice input
- Greetings and Hinglish replies that address the user as **Boss**
- Open supported apps, dial a number, draft SMS, open Settings and web search
- Optional Gemini API key dialog; the key is stored locally in Android SharedPreferences and is not committed to Git
- Gemini answers use a Jarvis prompt that addresses the user as Boss

## Build

Open the repository root in Android Studio, or run `./gradlew assembleDebug` after installing Gradle/Android SDK. Android Studio is only needed to compile/install the APK; the source is ready in this repository.

## Important limitation

A website cannot provide an always-on `Hey Jarvis` wake word. This native starter has a microphone button and recognizes commands beginning with `Hey Jarvis` when the app is open. A true locked-screen/background wake word requires a foreground microphone service, a wake-word engine, battery policy handling and assistant-role integration. API keys should ideally be proxied through a backend for a production release; this prototype stores the user-provided key locally.
