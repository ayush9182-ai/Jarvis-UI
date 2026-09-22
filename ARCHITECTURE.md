# Production brain architecture

`JarvisAgent` routes deterministic phone actions locally and sends conversation, reasoning and code prompts to the same Gemini `gemini-3.5-flash` model using one API key. The app includes an in-app Google AI Studio setup guide.

The backend proxy keeps a server-side key out of the APK for public deployments. For personal installs, users can paste their own key in the app.
