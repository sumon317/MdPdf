# WebView JavaScriptInterface - needed for JS callbacks
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
