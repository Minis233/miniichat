# MiniiChat

A tiny, open-source Android chat client for any OpenAI-compatible LLM API. Inspired by the look-and-feel of [Minis](https://github.com/Minis233), but stripped down to a pure chat front-end.

- Pure Jetpack Compose + Material 3 (with dynamic color on Android 12+)
- OpenAI-compatible: works with OpenAI, OpenRouter, DeepSeek, Groq, Gemini OpenAI shim, Together, Ollama, LM Studio, etc.
- Server-Sent-Events streaming, with a Stop button
- Local conversation history (DataStore)
- Adjustable system prompt, temperature, model, base URL
- Dark / light mode follows system; edge-to-edge layout
- MIT licensed, single-APK, no analytics, no backend

## Screenshots

> Build the debug APK from CI and run it on Android 8.0 (API 26) or newer.

## Build

The repo includes a GitHub Actions workflow that produces a debug APK on every push.

```bash
# Local build (needs JDK 17 + Android SDK)
./gradlew :app:assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

To get an APK without setting up Android SDK locally, push to GitHub and download the `miniichat-debug` artifact from the **Actions** tab. Tag a commit `vX.Y.Z` and the workflow will also publish a GitHub Release with the APK attached.

## Configuration

On first launch open the drawer (top-left) → Settings, then fill in:

| Field | Example |
| --- | --- |
| API Base URL | `https://api.openai.com/v1` |
| API Key | `sk-...` |
| Model | `gpt-4o-mini` |
| System prompt | `You are a helpful assistant.` |
| Temperature | `0.7` |
| Stream responses | on |

Other compatible endpoints:

- OpenRouter: `https://openrouter.ai/api/v1`
- DeepSeek: `https://api.deepseek.com/v1`, model e.g. `deepseek-chat`
- Groq: `https://api.groq.com/openai/v1`
- Together: `https://api.together.xyz/v1`
- Ollama: `http://<host>:11434/v1`, no key required (use any string)
- LM Studio: `http://<host>:1234/v1`

## Project layout

```
app/src/main/kotlin/com/miniichat
├── MainActivity.kt          # Compose entry
├── ChatViewModel.kt         # State + send/stream/persist
├── api/LlmClient.kt         # ktor + SSE
├── data/                    # DataStore (settings + conversations)
├── ui/AppRoot.kt            # Drawer, chat, settings screens
└── ui/theme/Theme.kt        # M3 color schemes
```

## License

MIT — see [LICENSE](LICENSE).

## Acknowledgments

- Inspired by [Minis](https://github.com/Minis233) — a Linux/Agent-capable AI app for Android.
- Uses Jetpack Compose, Material 3, Ktor, kotlinx.serialization.
