# MdPdf — Fix Tasks from Test Report

## HIGH PRIORITY

### B1 + B2: Snackbar lifecycle fix (MdPdfScreen.kt)
- [x] Dismiss progress snackbar when export completes/fails
- [x] Show actual page count from exporter callback (not hardcoded "0 of 1")
- [x] Use `SnackbarHostState.showSnackbar` with proper replacement logic

### B6: Move PdfExporter off main thread (PdfExporter.kt, MdPdfScreen.kt)
- [x] Run `runExport()` on IO dispatcher instead of main thread
- [x] Add proper coroutine scope for background work
- [ ] Test for frame drops (no "Skipped frames" in logcat)

### B3 + B4: Task list checkboxes + code block headers in preview
- [x] Add Task List Items extension to MarkdownParser
- [x] CSS/JS to render `- [x]` / `- [ ]` as real checkboxes in preview
- [x] Call `addCodeHeaders()` in preview HTML template (not just print)

## MEDIUM PRIORITY

### B5: Recent Files dialog - store full URIs
- [ ] Update SettingsRepository to store `List<Pair<String, String>>` (name + URI)
- [ ] Implement click handler to re-open file via SAF

### U1: Toolbar overcrowding
- [ ] Consider collapsing less-used actions into overflow menu

### U2: Status bar icon contrast in Light theme
- [x] Use `isAppearanceLightStatusBars = true` for light theme

### U4: Debounce WebView re-renders
- [ ] Add debounce (~300ms) to MarkdownWebView htmlContent updates

## LOW PRIORITY

### B7: Export save location UX
- [ ] Always use SAF picker when no default folder configured

### P2: Disable minification in debug build
- [x] Set `isMinifyEnabled = false` for debug buildType

### U3: Custom notification icon
- [ ] Replace `ic_menu_save` with app-specific icon

---

## COMPLETED
- [x] Code cleanup: 0 warnings, 0 errors (language server)
- [x] R.string.* false positives eliminated via Strings.kt
- [x] Build passes: compileDebugKotlin, testDebugUnitTest, detekt