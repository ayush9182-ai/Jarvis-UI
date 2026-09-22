# Gemini setup

Jarvis uses one Gemini API key for conversation, reasoning and code. The configured model is `gemini-3.5-flash` in both the app client and backend configuration.

## User setup inside the app

1. Open **Google AI Studio**: https://aistudio.google.com/apikey
2. Sign in with a Google account.
3. Tap **Create API key**.
4. Choose **Create API key in new project** if Google asks.
5. Press **Copy** beside the generated key.
6. Open Jarvis and tap **HOW TO GET API KEY** if needed.
7. Tap **API KEY**, paste the key, and press **Save**.
8. Ask Jarvis a question or give a command.

The app stores the pasted key locally on the device and never commits it to this repository. Users must not share their key. If it is exposed, revoke it in AI Studio and create a new one.

## Production recommendation

For a public release, use the backend proxy in `backend/` and keep `GEMINI_API_KEY` only in the server environment. Set `GEMINI_MODEL=gemini-3.5-flash`, deploy behind HTTPS, then set the release `BACKEND_URL`. The direct app key path is convenient for personal/testing installs but is not ideal for a public APK.
