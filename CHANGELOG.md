# Changelog

All notable changes to MdPdf will be documented in this file.

## [1.0.0] — 2025-07-12

### Added
- Live markdown preview (Editor, Split, Preview modes)
- KaTeX math rendering (inline and display)
- Prism.js syntax highlighting with autoloader
- PDF export with paginated A4 output
- Multiple PDF themes: Default, Academic, Dark, Minimal, Pure Black
- App theme options: Light, Dark, Pure Black (AMOLED)
- Image insertion with alt-text editing dialog
- Share markdown source and generated PDF
- Copy markdown as plain text to clipboard
- Export progress notifications with retry on failure
- Snackbar-based export progress (non-blocking)
- Recent files list (names only)
- Default save folder via SAF Document Tree picker
- App-specific documents folder fallback when no SAF token
- Spellcheck toggle for editor
- Localized UI strings (English + Spanish)
- Accessibility content descriptions on toolbar and dialogs
- IME action Done on editor fields
- SavedStateHandle-based state persistence across process death

### Quality
- 44 unit tests (MarkdownParser, SettingsRepository, MdPdfViewModel)
- 7 instrumented UI tests (MdPdfScreen, PdfExporter)
- Detekt static analysis (0 warnings) with project config
- GitHub Actions CI: unit tests → lint+detekt → instrumented tests → APK build
- KDoc on all public classes
- Centralized `Constants` object for preference keys and configuration
- `DialogState` sealed class replacing scattered Boolean flags
- Intent handling extracted to `IntentHandler` object
- StateFlow simplification (map instead of combine with dummy flow)
- Minification + resource shrinking enabled for debug builds
- 12 unused KaTeX contrib scripts removed
- WebView deprecated API calls removed
- Cache clearing before PDF export
- Jetifier disabled (all AndroidX)