# Jarvis UI

A lightweight, mobile-first Android command-center prototype. It runs as a static website, so Android Studio is not required.

## What works now

- Type `hello`, `hi`, `hlo`, `hey Jarvis` and Jarvis replies with voice and text
- Tap **Enable Hey Jarvis**, grant microphone permission once, then say `Hey Jarvis` followed by a command
- Voice commands: open supported apps, call a phone number, draft an SMS, search the web and set timers
- Local activity history, text-to-speech replies and quick actions

## Important Android limitation

A website cannot behave like Google Assistant when Chrome is closed, the screen is locked, or the browser has no microphone permission. Browser speech recognition must be started by a user gesture and can stop when Android suspends the tab. The current web prototype therefore supports **Hey Jarvis while this page is open and listening is enabled**.

For a real always-available wake word, background microphone service, contacts access and system-wide app control, Jarvis needs a native Android app with an Android foreground service, speech recognition/wake-word engine, permissions and an assistant-role integration. That is the next Android phase—not something GitHub Pages alone can provide.
