# AndroidIDE 🛠️

> A **Visual Studio Code–inspired IDE** for Android, written entirely in Kotlin + Jetpack Compose.  
> Features Python support via Termux, a built-in AI coding assistant, and a full LSP pipeline.

[![Build Status](https://github.com/your-org/AndroidIDE/actions/workflows/build.yml/badge.svg)](https://github.com/your-org/AndroidIDE/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## ✨ Features

| Feature | Description |
|---|---|
| **Code Editor** | Sora Editor with TextMate syntax highlighting for Python, JS, TS, Kotlin, Java, C++, Rust, Go, and more |
| **Language Server (LSP)** | Connects to `pylsp` running inside Termux for completions, diagnostics, hover info, go-to-definition |
| **File Tree** | Workspace explorer with create / rename / delete operations |
| **Multi-Tab** | Open multiple files simultaneously with unsaved change indicators |
| **Search** | Global search-in-files across the open workspace |
| **Terminal** | Embedded terminal backed by Termux – run Python scripts with one tap, or open an interactive shell |
| **AI Assistant** | Chat panel + inline completions via HuggingFace (free), Ollama, any OpenAI-compatible API, or offline rule-based fallback |
| **Command Palette** | `⌘P`-style overlay for quick actions |
| **Settings** | Font size, tab width, auto-save, theme, AI provider, API keys |
| **Dark Theme** | VS Code Dark+ color scheme throughout |

---

## 🏗 Architecture

```
app/
├── di/                         # Hilt dependency injection modules
├── data/
│   ├── model/                  # Pure Kotlin data classes (FileNode, EditorTab, AiMessage, …)
│   ├── local/                  # Room database, DAOs, entities
│   └── repository/             # FileRepository, SettingsRepository (DataStore)
├── domain/
│   └── usecase/                # Business logic use-cases
├── ai/                         # AI abstraction layer
│   ├── AiCodeAssistant         # Interface
│   ├── HuggingFaceAssistant    # Free HuggingFace Inference API
│   ├── OpenAiCompatibleAssistant  # Ollama / LM Studio / OpenAI
│   ├── RuleBasedAssistant      # Offline fallback
│   └── AiRepository            # Selects active provider
├── lsp/                        # Language Server Protocol client
│   ├── LspProtocol             # JSON-RPC message builder
│   ├── LspClient               # Async bidirectional LSP connection
│   └── LspService              # Android Service managing pylsp lifecycle
├── termux/                     # Termux bridge
│   ├── TermuxBridge            # Runs scripts, opens shells
│   ├── TerminalSession         # Stateful interactive session
│   └── TerminalService         # Foreground service
├── worker/
│   └── IndexingWorker          # WorkManager background file indexer
└── ui/                         # Jetpack Compose screens
    ├── theme/                  # Colors (VS Code Dark+), Typography
    ├── navigation/             # NavHost / AppNavigation
    ├── editor/                 # MainIdeScreen, CodeEditorView, TabBar, StatusBar
    ├── filetree/               # FileTreePanel, FileTreeViewModel
    ├── terminal/               # TerminalPanel, TerminalViewModel
    ├── ai/                     # AiChatPanel, AiViewModel
    ├── settings/               # SettingsScreen, SettingsViewModel
    └── components/             # CommandPalette, shared widgets
```

**Pattern:** MVVM + Clean Architecture · Hilt DI · Coroutines/Flow · Room · DataStore

---

## 🚀 Getting Started

### Prerequisites

| Tool | Version |
|---|---|
| Android Studio | Hedgehog or later |
| JDK | 17 |
| Android SDK | API 34 |
| Kotlin | 2.0.x |

### Build

```bash
git clone https://github.com/your-org/AndroidIDE.git
cd AndroidIDE
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Release Build

1. Create a `keystore.properties` file (excluded from git):
   ```properties
   storeFile=your.jks
   storePassword=...
   keyAlias=...
   keyPassword=...
   ```
2. Run: `./gradlew assembleRelease`

---

## 📱 Setting Up Python Support

AndroidIDE uses **Termux** as the underlying Linux environment.

### 1 – Install Termux

Download from **F-Droid** (not Play Store): https://f-droid.org/packages/com.termux/

### 2 – Install Python and pylsp

Open Termux and run:

```bash
pkg update && pkg upgrade -y
pkg install python python-pip clang make -y
pip install python-lsp-server[all]
pip install pylsp-mypy pyls-flake8          # optional extras
```

### 3 – Optional: install common packages

```bash
pip install numpy pandas matplotlib requests flask
```

Once Termux is set up, **AndroidIDE automatically detects it** and:
- Runs Python scripts via `python3` in the Termux environment
- Connects the editor to `pylsp` for completions and diagnostics

---

## 🤖 AI Assistant Setup

### Option A – HuggingFace (Recommended, Free)

1. Create a free account at https://huggingface.co
2. Generate an API token at https://huggingface.co/settings/tokens
3. In AndroidIDE → **Settings → AI Assistant**, select **HuggingFace** and paste your token

The default model is `bigcode/starcoder2-3b`. The free tier allows ~1,000 requests/month.  
Without a key, the public (rate-limited) endpoint is used.

### Option B – Ollama (Local, Fully Private)

1. Install Ollama on a machine on the same Wi-Fi: https://ollama.ai
2. Pull a model: `ollama pull codellama:7b`
3. Start server: `ollama serve`
4. In AndroidIDE Settings, select **OpenAI Compatible** and set Base URL to `http://YOUR_PC_IP:11434/v1`

### Option C – LM Studio / OpenAI

Set base URL to your LM Studio local server or `https://api.openai.com/v1` with your key.

### Option D – Offline (No Config Needed)

If no provider is configured or reachable, the **Rule-Based** assistant provides snippet completions
from a built-in dictionary — works with zero network access.

---

## ⚙️ CI/CD – GitHub Actions

The workflow at `.github/workflows/build.yml` runs on every push to `main`/`develop`:

1. **Lint** – Android lint check
2. **Unit Tests** – JVM tests via JUnit + MockK + Turbine
3. **Debug APK** – Built and uploaded as artifact
4. **Release APK** – Signed with keystore from GitHub Secrets (on `main` or tags)
5. **GitHub Release** – Auto-created for `v*.*.*` tags with the signed APK attached

### Required GitHub Secrets

| Secret | Description |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded JKS keystore file |
| `KEY_ALIAS` | Key alias inside the keystore |
| `KEY_PASSWORD` | Key password |
| `STORE_PASSWORD` | Keystore password |

Generate:
```bash
keytool -genkey -v -keystore androidide.jks -alias androidide \
        -keyalg RSA -keysize 4096 -validity 10000
base64 -w 0 androidide.jks
```

---

## 🔌 Adding New Languages

The editor uses a `languageFromExtension()` mapper and TextMate grammar assets.

1. Download the `.tmLanguage.json` grammar for your language
2. Place it under `app/src/main/assets/textmate/<language>.tmLanguage.json`
3. Add the extension → language mapping in `EditorTab.languageFromExtension()`
4. Add the grammar path mapping in `CodeEditorView.applyLanguage()`

For full LSP support, install the language server in Termux and start it from `LspService`.

---

## 🧪 Testing

```bash
# Unit tests
./gradlew test

# Lint
./gradlew lint

# Instrumentation tests (requires device/emulator)
./gradlew connectedAndroidTest
```

---

## 📋 Known Limitations

| Limitation | Workaround |
|---|---|
| pylsp requires Termux | Install Termux from F-Droid; LSP gracefully disables if absent |
| Large files (>5 MB) may lag | Use smaller workspace or increase JVM heap in gradle.properties |
| AI responses are sequential (no streaming) | Streaming added in future; HF returns full completion |
| Git integration (commit/push) | Open a Termux terminal and use `git` CLI directly |
| Local LLM on-device | Use Ollama on a PC over local network; on-device models planned for v2 |

---

## 🤝 Contributing

1. Fork the repo
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Follow MVVM + Clean Architecture patterns
4. Add unit tests for new ViewModels/repositories
5. Run `./gradlew lint test` before submitting
6. Open a PR against `develop`

---

## 📜 License

```
MIT License – Copyright (c) 2024 AndroidIDE Contributors
```

See [LICENSE](LICENSE) for full text.

---

## 🙏 Acknowledgements

- [Sora Editor](https://github.com/Rosemoe/sora-editor) – The code editor engine
- [Termux](https://termux.dev) – Linux environment on Android
- [python-lsp-server](https://github.com/python-lsp/python-lsp-server) – Open-source Python LSP
- [HuggingFace](https://huggingface.co) – Free AI inference API
- [Ollama](https://ollama.ai) – Local LLM runner
