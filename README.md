# Z-OS App

Android app for [Z-OS Computer](https://github.com/UIoperationl/z-os-computer-web).

## Download

👉 **[Get the APK from Releases](https://github.com/UIoperationl/z-os-app/releases)**

## Installation

1. Download `z-os.apk` from the latest release
2. On your phone: Settings → Security → enable "Install from unknown sources"
3. Open the APK file (from Files app or browser downloads)
4. Tap "Install"
5. Launch "Z-OS" from your app drawer

## Features

- **Full Z-OS desktop** in your pocket
- **Real bash shell** (when backend is alive)
- **AI chat** with tool use: bash, image gen, TTS, web search
- **BYOK**: use any OpenAI-compatible API (OpenAI, Together, local LLMs, etc.)
- **Editable system prompt** — make Z your own
- **Auto-rotates**: portrait = phone mode (tabs), landscape = desktop mode (windows)
- **Fullscreen immersive** — no status bar, no navigation bar
- **Responsive** — same smooth UI as the web version

## Architecture

```
APK (Java + WebView)
  └── WebView loads Z-OS URL
        └── Next.js web app (z-os-computer-web repo)
              └── Real bash, AI chat, file browser, etc.
```

The app is a thin native wrapper around a WebView that loads the Z-OS web app. This means:
- The UI is identical to the web version
- All features work the same
- The backend (Next.js) must be running somewhere

## Default URL

Points to the Z-OS sandbox URL: `https://preview-chat-8faf28d5-3f19-45cf-bd33-bdcf8ba3dcbc.space-z.ai/`

**This URL only works while the chat session is active.** For 24/7 access:

1. Deploy the [web app](https://github.com/UIoperationl/z-os-computer-web) to Vercel (free tier)
2. In the Z-OS app, press the **Menu** button on your phone
3. A URL bar appears at the top
4. Enter your Vercel URL (e.g. `https://z-os.vercel.app`)
5. Tap "Load"

## How to change URL

Press the **Menu** button (hardware menu button, or three-dot overflow on some devices). A URL input bar appears at the top of the screen. Enter the new URL and tap "Load". The URL is saved for next time.

## Why Capacitor-style WebView instead of native Kotlin?

The bottleneck is network round-trips to the backend server, not the wrapper. WebView is:
- **Simpler** — same codebase as web
- **Faster to ship** — no need to rebuild UI in Jetpack Compose
- **Identical UX** — what you see on web is what you get on mobile
- **Updatable** — change the web app, the APK updates automatically (no reinstall)

If performance becomes an issue, we can rebuild in Kotlin later. For v1, WebView is the right call.

## Build from source

```bash
# Requires Android SDK + JDK 17
cd android
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

## Permissions

- `INTERNET` — to connect to the Z-OS backend
- `ACCESS_NETWORK_STATE` — to detect online/offline

That's it. No camera, no contacts, no location, no storage. Just network.

## License

MIT
