# MdPdf

A Markdown-to-PDF converter for Android with live preview, KaTeX math rendering, syntax highlighting, and multiple export themes — inspired by Obsidian.

## Features

- **Live Preview** — Editor, Split, and Preview view modes
- **KaTeX Math** — Inline (`\(...\)`) and display (`\[...\]`) math rendering
- **Syntax Highlighting** — Via Prism.js with autoloader for 200+ languages
- **PDF Export** — Paginated A4 PDF generation via WebView capture
- **Accessibility** — Localized content descriptions, TalkBack-friendly, high-contrast Material theme variants
- **Image Support** — Insert images into markdown with alt-text editing
- **Share** — Share markdown source or generated PDF via Android sharesheet
- **Copy as plain text** — Quick clipboard copy from overflow menu
- **Export Notifications** — Progress, completion, and error notifications (Android 13+ permission-aware)
- **Recent Files** — Quick-access list of recently opened documents
- **Multiple PDF Themes** — Default, Academic, Dark, Minimal, and Pure Black
- **App Themes** — Light, Dark, and Pure Black (AMOLED)
- **Spellcheck Toggle** — Enable/disable autocorrect in editor
- **SAF Folder Picker** — Set a default save folder via Storage Access Framework

## Screenshots

*Coming soon — generate via Android Studio Device Manager or physical device.*

## Build Instructions

### Prerequisites
- Android Studio Hedgehog (2024.1+) or newer
- JDK 17
- Gradle 9.x (wrapper included)

### Clone & Build
```bash
git clone https://github.com/your-org/MdPdf.git
cd MdPdf
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Run tests
```bash
# Unit tests (runs on host JVM, no emulator needed)
./gradlew test

# Instrumented tests (requires emulator or connected device)
./gradlew connectedAndroidTest

# Static analysis
./gradlew detekt
```

## Architecture

| Component | Purpose |
|-----------|---------|
| `MarkdownParser` | Parses markdown via CommonMark, extracts KaTeX math, generates preview/print HTML |
| `PdfExporter` | Renders HTML in off-screen WebView, captures pages as PDF |
| `MdPdfViewModel` | Holds UI state (text, view mode, theme) in SavedStateHandle-backed StateFlows |
| `SettingsRepository` | Thread-safe singleton for SharedPreferences persistence |
| `NotificationHelper` | Manages export notification channel and posts progress/result notifications |
| `IntentHandler` | Routes VIEW intents (PDF/markdown) from external apps |
| `MdPdfScreen` | Main Compose screen with editor, preview, overflow menu, and dialogs |
| `SettingsDialog` | Compose dialog for app/export settings |

## Testing

- **44 unit tests** across `MarkdownParser`, `SettingsRepository`, and `MdPdfViewModel` using Robolectric, Truth, and MockK
- **7 instrumented tests** for `MdPdfScreen` UI and `PdfExporter` using Compose UI testing
- **CI workflow** on GitHub Actions: unit tests → lint+detekt → instrumented tests → debug APK build

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Run `./gradlew test detekt` to ensure existing tests and lint pass
4. Add tests for your changes
5. Submit a pull request

### Adding a new Prism language
Drop the language `.min.js` file into `app/src/main/assets/prism/` and add a `<script>` tag for it in `MarkdownParser.kt`'s HTML template.

### Adding a new PDF theme
Add a new entry to the `MdTheme` enum and provide a `<style>` block in `MarkdownParser.kt`'s `toPrintHtml()` method.

## License

MIT License — see [LICENSE](LICENSE) for details.

## Third-party Notices

This project includes the following open-source libraries:

- **commonmark-java** (BSD 2-Clause) — Markdown parsing
- **KaTeX** (MIT) — Math rendering (bundled in assets)
- **Prism.js** (MIT) — Syntax highlighting (bundled in assets)

See [NOTICE](NOTICE) for full attribution details.