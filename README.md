# Jarvis Assistant (React + Vite)

A futuristic AI assistant application rewritten in React, TypeScript, and Vite, faithfully preserving the core features, UI identity, and business logic of the original Jarvis Android assistant.

## Features Preserved

- **Futuristic HUD Interface**: Animated Canvas HUD with rotating concentric arcs, glowing cyan rings, 12 satellite radar dots, center "J" mark, and state-reactive pulse animations.
- **Voice & Speech Integration**:
  - Voice input with Web Speech Recognition (`SpeechRecognition` / `webkitSpeechRecognition`), "Hey Jarvis" wake word handling.
  - Text-to-Speech (TTS) voice responses powered by the Web Speech Synthesis API.
  - Interactive audio mute/unmute control.
- **Intent Routing & Tools**:
  - Greetings in Hinglish & English ("hello", "hey", "namaste", "yo").
  - App & Web Launch: Direct launch for YouTube, WhatsApp Web, Spotify, Instagram, Maps, Telegram, Gmail, GitHub, etc.
  - Web Search: Fast Google search integration.
  - Phone Calls: `tel:` link integration guarded with confirmation dialogs.
  - SMS & Messaging: `sms:` link integration with message prefill and confirmation dialogs.
  - Focus Timer: Interactive visual HUD circular countdown timer with audio chime on completion.
  - General Chat & Reasoning: Conversational AI powered by Google Gemini.
- **Context & Memory Store**: Multi-turn conversation memory (last 10 turns) stored locally and passed to Gemini for multi-turn context.
- **API Key & Google AI Studio Setup**:
  - Secure in-app dialog to input and save Gemini API keys locally.
  - Interactive step-by-step AI Studio onboarding guide.
  - Server-side Gemini API proxy (`/api/assistant`) with lazy initialization and environment variable support.

## Development

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build
```
