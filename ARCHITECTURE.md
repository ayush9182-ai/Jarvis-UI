# Production brain architecture

`JarvisAgent` is the orchestration layer:

1. `IntentRouter` classifies a request.
2. `AndroidTools` executes deterministic phone actions.
3. `ConfirmationPolicy` blocks silent calls/messages.
4. `MemoryStore` keeps bounded short-term context locally.
5. `LlmClient` calls the backend proxy and only uses direct Gemini as a development fallback.
6. `JarvisAgent` records the result and returns it to the UI/TTS layer.

The backend in `backend/` keeps `GEMINI_API_KEY` off the APK. Deploy it behind HTTPS, set the release `BACKEND_URL` in `app/build.gradle.kts`, and remove the direct-key fallback before publishing.

## Backend

```bash
cd backend
cp .env.example .env
# fill GEMINI_API_KEY without committing .env
npm install
node server.js
```

For a commercial release, add authentication, per-user memory in a database, audit logging with redaction, monitoring, and a proper on-device wake-word engine. The app intentionally keeps calls and SMS confirmation-based.
