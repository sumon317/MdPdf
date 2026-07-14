# MdPdf — Test Report

**Device:** Xiaomi (BIMBUSV4DYZSWWBI) | **Build:** Debug APK | **Date:** 2026-07-12

---

## BUGS (What's Wrong)

### B1. Snackbar never dismisses after export completes (HIGH)
- **File:** `app/src/main/java/com/example/mdpdf/ui/MdPdfScreen.kt:70-72`
- **Issue:** After PDF export succeeds, the progress snackbar (`SnackbarDuration.Indefinite`) never dismisses. It stays on screen indefinitely, overlapping content. Even the success snackbar doesn't replace it.
- **Root cause:** The initial snackbar uses `SnackbarDuration.Indefinite` and subsequent `showSnackbar` calls queue rather than replace. The snackbar host doesn't auto-dismiss.

### B2. Initial export progress shows hardcoded "Page 0 of 1" regardless of actual page count (MEDIUM)
- **File:** `app/src/main/java/com/example/mdpdf/ui/MdPdfScreen.kt:70-72`
- **Issue:** Before calling `PdfExporter`, the code shows `Strings.exportProgress(0, 1)` — hardcoding `total=1`. The actual page count (e.g., 2) is only known after the WebView renders. Progress messages from the exporter then queue behind the indefinite initial snackbar.
- **Log evidence:** `PdfExporter: contentHeight=2001 pageCount=2` but snackbar showed "Page 0 of 1".

### B3. Checkboxes render as plain text `[x]` / `[ ]` — not as interactive checkbox UI (MEDIUM)
- **Files:** `app/src/main/java/com/example/mdpdf/MarkdownParser.kt`, `app/src/main/java/com/example/mdpdf/MdPdfViewModel.kt`
- **Issue:** GFM task list items (`- [x]`, `- [ ]`) render as literal `[x]` and `[ ]` text instead of checkbox widgets. This applies to both preview and PDF.
- **Root cause:** CommonMark's `TablesExtension` + `StrikethroughExtension` are included, but the **Task List Items extension is not**. There is also no CSS/JS post-processing to convert them to checkboxes.

### B4. Code block headers (language label + copy button) missing from live preview (MEDIUM)
- **File:** `app/src/main/java/com/example/mdpdf/MarkdownParser.kt:219-222`
- **Issue:** The `addCodeHeaders()` JavaScript function that adds language labels and copy buttons to code blocks is **only called in the print/PDF HTML template** (line 172), **not** in the preview HTML template (line 219-222).
- **Impact:** Users see code block headers only in exported PDF, not in the live preview.

### B5. Recent Files dialog is non-functional (MEDIUM)
- **File:** `app/src/main/java/com/example/mdpdf/ui/MdPdfScreen.kt:283-289`
- **Issue:** Recent files only store file **names** (not URIs). Clicking a recent file does nothing (the `onClick` handler is empty with a `TODO` comment). The dialog is effectively useless.
- **Code reference:** Line 288: `// TODO: store full URI in recentFiles for re-opening`

### B6. `PdfExporter` runs on the main thread (HIGH)
- **File:** `app/src/main/java/com/example/mdpdf/PdfExporter.kt:57-65`
- **Issue:** `runExport()` is called synchronously from `export()`, which runs on the calling coroutine's thread. Since the snackbar and `scope.launch` in `MdPdfScreen` use the main dispatcher, the export effectively blocks the main thread while the WebView renders, waits 5 seconds (`postDelayed` at line 114-118), and captures pages.
- **Impact:** UI freezes during export. This was observed as "Skipped 71 frames!" on cold start (1039ms block).

### B7. Export saves directly to app-specific storage without user choice when no default folder is set (LOW)
- **File:** `app/src/main/java/com/example/mdpdf/ui/MdPdfScreen.kt:360-363`
- **Issue:** When no default save folder is configured and no SAF picker is used, the app saves to `getExternalFilesDir(DIRECTORY_DOCUMENTS)` using `Uri.fromFile()`. This bypasses the `CreateDocument` SAF contract and the file is only accessible via the app or a file manager with special access. The user is not prompted to choose a location.
- **Log evidence:** `PDF saved to file:///storage/emulated/0/Android/data/com.example.mdpdf/files/Documents/MdPdf.pdf`

---

## PERFORMANCE ISSUES

### P1. Cold start frame drops (HIGH)
- **Log evidence:** `Skipped 71 frames! The application may be doing too much work on its main thread.` and `Choreographer$FrameHandler callback took 1039 ms`
- **Root cause:** Heavy work during `onCreate` including settings initialization, ViewModel setup, and initial Compose composition with the large default markdown document.

### P2. Debug build has minification enabled (LOW)
- **File:** `app/build.gradle.kts:31-32`
- **Issue:** Debug build has `isMinifyEnabled = true` and `isShrinkResources = true`. Gradle warns: `BuildType 'debug' is both debuggable and has 'isMinifyEnabled' set to true. All code optimizations and obfuscation are disabled for debuggable builds.` This adds build time with no benefit.

---

## UI/UX ISSUES

### U1. Toolbar is overcrowded
- **Issue:** Edit, Split, View, Open, Export PDF, and overflow menu are all in the top bar. On narrower devices or smaller screens, buttons may be clipped or hard to tap.

### U2. `isAppearanceLightStatusBars = false` in all theme branches
- **File:** `app/src/main/java/com/example/mdpdf/IntentHandler.kt:47,52,57`
- **Issue:** Light mode sets `isAppearanceLightStatusBars = false`, which gives white status bar icons on a green (#5F8B4A) background. This may cause poor contrast on some devices depending on the exact green shade.

### U3. Export notification uses system `ic_menu_save` icon
- **File:** `app/src/main/java/com/example/mdpdf/NotificationHelper.kt:43,66,79`
- **Issue:** Using `android.R.drawable.ic_menu_save` as the notification small icon looks generic and unprofessional. Should use a custom app-specific icon.

### U4. No debouncing on WebView re-renders during typing
- **File:** `app/src/main/java/com/example/mdpdf/ui/MarkdownWebView.kt`
- **Issue:** Every time `htmlContent` changes (which happens on every keystroke), the entire WebView reloads via `loadDataWithBaseURL`. There is no debouncing, so rapid typing causes many re-renders with brief blank flashes.

---

## WHAT WORKS WELL

| Feature | Status |
|---------|--------|
| View mode switching (Edit/Split/Preview) | Working |
| KaTeX math rendering (inline + display) | Excellent — matrices, integrals, Euler's identity all render correctly |
| Prism.js syntax highlighting | Working — Kotlin, Java, Python, Bash, JSON, Markdown |
| Tables with formatting | Working — borders, alternating rows, headers |
| Blockquotes | Working — left border styling |
| Bold, italic, strikethrough, inline code | Working |
| Settings dialog (theme, spellcheck, notifications, folder) | Working |
| Theme picker (5 PDF themes) | Working |
| App theme switching (Light/Dark/AMOLED) | Working |
| PDF export (generates valid PDF) | Working — 192KB, 2 pages |
| Export notification permission flow (Android 13+) | Working |
| Editor text input | Working |
| Overflow menu | Working — all 7 items present |
| Recent Files dialog | Shows "No recent files" correctly |
| Unit tests (all pass) | BUILD SUCCESSFUL |
| No crashes or ANR in logs | Clean |

---

## SUMMARY

| Category | Critical | High | Medium | Low |
|----------|----------|------|--------|-----|
| Bugs | 0 | 3 (B1, B3, B6) | 3 (B2, B4, B5) | 1 (B7) |
| Performance | 0 | 1 (P1) | 0 | 1 (P2) |
| UI/UX | 0 | 0 | 4 (U1-U4) | 0 |

### Top 3 Priorities to Fix

1. **B1+B2:** Fix snackbar lifecycle — dismiss on completion, show actual page count
2. **B6:** Move PdfExporter off the main thread
3. **B3+B4:** Add task list checkbox rendering + add `addCodeHeaders()` to preview template
