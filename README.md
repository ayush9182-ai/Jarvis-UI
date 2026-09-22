# Jarvis UI

A lightweight, mobile-first Android command-center prototype. It runs as a static website, so Android Studio is not required.

## Run

Open `index.html` in a browser or enable GitHub Pages for this repository.

## Supported browser actions

- Voice commands through Web Speech API where supported
- Open common Android apps through app intents
- Open the phone dialer with `call 9876543210`
- Draft an SMS with `message 9876543210 hello`
- Web search with `search latest tech news`
- Local timers, speech feedback, command history and quick actions

A browser cannot silently control every Android app or send messages without user confirmation. Full wake-word listening, contacts, background actions and deeper app control belong in the native Android companion planned for Jarvis 2.0.
