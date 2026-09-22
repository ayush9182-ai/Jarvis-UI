# Native Jarvis Android app

## Added in this build

- Native futuristic mobile UI with animated Jarvis orb
- API-key screen for a user-provided Gemini key, stored locally
- Boss-style Hindi/English responses and Android text-to-speech
- Contact-name lookup with `READ_CONTACTS`
- Confirmation dialogs before opening the dialer or SMS composer
- Foreground `microphone` service with restart/backoff handling
- `Hey Jarvis` phrase detection while Boss Mode is running
- App launching, web search, dialer, SMS draft, settings and Gemini answers

## Build the APK

1. Open the repository root in Android Studio (not the `app` folder).
2. Let Gradle sync and install Android SDK 35 if prompted.
3. Connect an Android phone with USB debugging enabled, or create an emulator.
4. Select the `app` configuration and press **Run**.
5. For a debug APK: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
6. The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

CLI alternative:

```bash
./gradlew assembleDebug
```

## First-run setup

1. Allow microphone, contacts and notification permissions.
2. Tap **API KEY** and paste your Gemini key (do not commit it to Git).
3. Start Boss Mode from the app to enable the foreground listener.
4. Say **Hey Jarvis**, then give a command.

## Platform limits

The service uses Android's built-in `SpeechRecognizer` as a lightweight wake-word prototype. Android may stop recognition, restrict microphone use, or kill the service depending on battery policy and device manufacturer. A production-grade always-on detector should replace this with a dedicated on-device wake-word engine and a compliant foreground-service flow. Calls and messages intentionally open a confirmation path; the app does not silently send or place them.
