package com.example.mdpdf

import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tasklist.TaskListItemsExtension
import org.commonmark.ext.gfm.tasklist.TaskListExtension
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.parser.Parser

/** Theme presets for the PDF output styling. */
enum class MdTheme(val label: String) {
    DEFAULT("Default"),
    ACADEMIC("Academic"),
    DARK("Dark"),
    MINIMAL("Minimal"),
    PURE_BLACK("Pure Black")
}

/**
 * Parses markdown text into HTML for preview and print/PDF export.
 *
 * Uses [org.commonmark] for standard markdown parsing with GFM table
 * and strikethrough extensions. Math expressions (KaTeX-compatible)
 * are extracted via regex and wrapped in KaTeX render hooks.
 *
 * Two HTML outputs are generated:
 * - [toHtml] produces a scrollable preview with Prism syntax highlighting.
 * - [toPrintHtml] produces a paginated PDF-ready document with page breaks.
 */
class MarkdownParser {

    private val mathRegex = Regex(
        """(\$\$[\s\S]*?\$\$|\\\[[\s\S]*?\\\]|\\\(.*?\\\)|\$(?:[^$\\]|\\.)*?\$)"""
    )

    private val extensions = listOf(
        TablesExtension.create(),
        StrikethroughExtension.create(),
        TaskListItemsExtension.create(),
    )

    private val parser = Parser.builder()
        .extensions(extensions)
        .build()

    private val htmlRenderer = HtmlRenderer.builder()
        .extensions(extensions)
        .escapeHtml(true)
        .sanitizeUrls(true)
        .build()

    private fun extractAndProtectMath(markdown: String): Pair<String, List<String>> {
        val blocks = mutableListOf<String>()
        val result = mathRegex.replace(markdown) { match ->
            blocks.add(match.value)
            "\u00A0MATH${blocks.lastIndex}\u00A0"
        }
        return result to blocks
    }

    private fun restoreMath(html: String, blocks: List<String>): String {
        var result = html
        blocks.forEachIndexed { index, math ->
            result = result.replace("\u00A0MATH${index}\u00A0", math)
        }
        return result
    }

    fun toHtml(markdown: String, theme: MdTheme = MdTheme.DEFAULT, showErrors: Boolean = true): String {
        val (clean, blocks) = extractAndProtectMath(markdown)
        val document = parser.parse(clean)
        val bodyHtml = htmlRenderer.render(document)
        val restored = restoreMath(bodyHtml, blocks)
        return wrapWithTemplate(restored, theme, isPrint = false, showErrors = showErrors)
    }

    fun toPrintHtml(markdown: String, theme: MdTheme = MdTheme.DEFAULT, showErrors: Boolean = true): String {
        val (clean, blocks) = extractAndProtectMath(markdown)
        val document = parser.parse(clean)
        val bodyHtml = htmlRenderer.render(document)
        val restored = restoreMath(bodyHtml, blocks)
        return wrapWithTemplate(restored, theme, isPrint = true, showErrors = showErrors)
    }

    private fun wrapWithTemplate(bodyHtml: String, theme: MdTheme, isPrint: Boolean, showErrors: Boolean): String {
        val themeCss = when (theme) {
            MdTheme.ACADEMIC -> ACADEMIC_CSS
            MdTheme.DARK -> DARK_CSS
            MdTheme.MINIMAL -> MINIMAL_CSS
            MdTheme.PURE_BLACK -> PURE_BLACK_CSS
            MdTheme.DEFAULT -> DEFAULT_CSS
        }
        val prismTheme = when (theme) {
            MdTheme.DARK -> "prism-okaidia.min.css"
            MdTheme.PURE_BLACK -> "prism-okaidia.min.css"
            else -> "prism.min.css"
        }

        val prismThemeUrl = "file:///android_asset/prism/themes/$prismTheme"
        val errorHideCss = if (showErrors) "" else ".katex-error { display: none !important; }"

        if (isPrint) {
            return """<!DOCTYPE html>
<html><head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=595,initial-scale=1.0"/>
<link rel="stylesheet" href="$prismThemeUrl"/>
<link rel="stylesheet" href="$KATEX_CSS"/>
<style>
$PRINT_CSS
$themeCss
$errorHideCss
</style>
</head><body>
$bodyHtml
<script src="$PRISM_JS"></script>
<script src="$PRISM_AUTOLOADER"></script>
<script src="$KATEX_JS"></script>
<script src="$KATEX_AUTO_RENDER"></script>
<script>
if(typeof Prism!=='undefined'&&Prism.plugins&&Prism.plugins.autoloader){
  Prism.plugins.autoloader.languages_path = 'file:///android_asset/prism/components/';
    }
    function tryRender(attempt){
if(typeof renderMathInElement!=='undefined'&&typeof katex!=='undefined'){
try{
renderMathInElement(document.body,{delimiters:[
{left:'$$',right:'$$',display:true},
{left:'$',right:'$',display:false},
{left:'\\(',right:'\\)',display:false},
{left:'\\[',right:'\\]',display:true}
]})
}catch(e){console.error('MdPdf: render error',e)}
}else if(attempt<20){
window.setTimeout(function(){tryRender(attempt+1)},500)
}else{
console.error('MdPdf: KaTeX failed to load')
}
}
function addCodeHeaders(){
var pres=document.querySelectorAll('pre[class*="language-"]');
if(pres.length===0) pres=document.querySelectorAll('pre code[class*="language-"]');
if(pres.length===0) pres=document.querySelectorAll('pre');
pres.forEach(function(pre){
if(pre.querySelector('.code-header'))return;
var header=document.createElement('div');header.className='code-header';
var label=document.createElement('span');label.className='lang-label';
var code=pre.querySelector('code');
var lang='';
if(code&&code.className){
var m=code.className.match(/language-(\w+)/);
if(m) lang=m[1];
}
label.textContent=lang||'CODE';
header.appendChild(label);
var btn=document.createElement('button');btn.className='copy-btn';
btn.textContent='Copy';
btn.onclick=function(){
var txt=code?code.textContent:pre.textContent;
if(navigator.clipboard&&navigator.clipboard.writeText){
navigator.clipboard.writeText(txt).then(function(){
btn.textContent='Copied!';
setTimeout(function(){btn.textContent='Copy';},2000);
});
}
};
header.appendChild(btn);
pre.parentNode.insertBefore(header,pre);
});
}

document.addEventListener('DOMContentLoaded',function(){
if(typeof Prism!=='undefined'){Prism.highlightAll()}
addCodeHeaders()
tryRender(1)
})
</script>
</body></html>"""
        }

        return """<!DOCTYPE html>
<html><head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1.0"/>
<link rel="stylesheet" href="$prismThemeUrl"/>
<link rel="stylesheet" href="$KATEX_CSS"/>
<style>
$COMMON_CSS
$themeCss
$errorHideCss
</style>
</head><body>
$bodyHtml
<script src="$PRISM_JS"></script>
<script src="$PRISM_AUTOLOADER"></script>
<script src="$KATEX_JS"></script>
<script src="$KATEX_AUTO_RENDER"></script>
<script>
if(typeof Prism!=='undefined'&&Prism.plugins&&Prism.plugins.autoloader){
  Prism.plugins.autoloader.languages_path = 'file:///android_asset/prism/components/';
  }
  console.log('MdPdf: scripts starting, katex='+typeof katex+', renderMathInElement='+typeof renderMathInElement)
function addCodeHeaders(){
var pres=document.querySelectorAll('pre[class*="language-"]');
if(pres.length===0) pres=document.querySelectorAll('pre code[class*="language-"]');
if(pres.length===0) pres=document.querySelectorAll('pre');
pres.forEach(function(pre){
if(pre.querySelector('.code-header'))return;
var header=document.createElement('div');header.className='code-header';
var label=document.createElement('span');label.className='lang-label';
var code=pre.querySelector('code');
var lang='';
if(code&&code.className){
var m=code.className.match(/language-(\w+)/);
if(m) lang=m[1];
}
label.textContent=lang||'CODE';
header.appendChild(label);
var btn=document.createElement('button');btn.className='copy-btn';
btn.textContent='Copy';
btn.onclick=function(){
var txt=code?code.textContent:pre.textContent;
if(navigator.clipboard&&navigator.clipboard.writeText){
navigator.clipboard.writeText(txt).then(function(){
btn.textContent='Copied!';
setTimeout(function(){btn.textContent='Copy';},2000);
});
}
};
header.appendChild(btn);
pre.parentNode.insertBefore(header,pre);
});
}
function tryRender(attempt){
if(typeof renderMathInElement!=='undefined'&&typeof katex!=='undefined'){
console.log('MdPdf: rendering math (attempt '+attempt+')')
try{
renderMathInElement(document.body,{delimiters:[
{left:'$$',right:'$$',display:true},
{left:'$',right:'$',display:false},
{left:'\\(',right:'\\)',display:false},
{left:'\\[',right:'\\]',display:true}
]})
console.log('MdPdf: math rendered successfully')
}catch(e){console.error('MdPdf: render error',e)}
}else if(attempt<10){
console.warn('MdPdf: KaTeX not ready (attempt '+attempt+'), retrying...')
window.setTimeout(function(){tryRender(attempt+1)},1000)
}else{
console.error('MdPdf: KaTeX failed to load after 10 attempts')
}
}
document.addEventListener('DOMContentLoaded',function(){
if(typeof Prism!=='undefined'){Prism.highlightAll()}
addCodeHeaders()
window.setTimeout(function(){tryRender(1)},500)
})
</script>
</body></html>"""
    }
}

private const val PRISM_JS = "file:///android_asset/prism/prism.min.js"
private const val PRISM_AUTOLOADER = "file:///android_asset/prism/prism-autoloader.min.js"
private const val KATEX_JS = "file:///android_asset/katex/katex.min.js"
private const val KATEX_CSS = "file:///android_asset/katex/katex.min.css"
private const val KATEX_AUTO_RENDER = "file:///android_asset/katex/contrib/auto-render.min.js"

private val COMMON_CSS = """
body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    font-size: 15px; line-height: 1.6; color: #1a1a1a;
    padding: 16px; max-width: 100%; word-wrap: break-word;
    -webkit-print-color-adjust: exact; print-color-adjust: exact;
}
h1 { font-size: 1.8em; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; margin: 0.67em 0; }
h2 { font-size: 1.5em; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; margin: 0.83em 0; }
h3 { font-size: 1.25em; margin: 1em 0; }
h4 { font-size: 1em; margin: 1.33em 0; }
p { margin: 0 0 16px; }
code { background: #f0f0f0; padding: 2px 6px; border-radius: 3px; font-family: 'Fira Code','Consolas',monospace; font-size: 0.88em; }
pre { background: #f6f8fa; padding: 16px; border-radius: 6px; overflow-x: auto; border: 1px solid #eaecef; }
pre code { background: none; padding: 0; border-radius: 0; }
blockquote { border-left: 4px solid #d0d7de; margin: 0 0 16px; padding: 0 16px; color: #57606a; }
blockquote p { margin: 0 0 6px; }
blockquote p:last-child { margin: 0; }
.code-header { display: flex; justify-content: space-between; align-items: center; padding: 4px 12px; background: #e8e8e8; font-size: 12px; font-family: sans-serif; border-radius: 6px 6px 0 0; border: 1px solid #eaecef; border-bottom: none; }
.code-header + pre { margin-top: 0; border-radius: 0 0 6px 6px; border-top: none; }
.code-header .lang-label { color: #666; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; font-size: 11px; }
.code-header .copy-btn { background: none; border: 1px solid #d0d7de; border-radius: 4px; padding: 2px 8px; font-size: 11px; cursor: pointer; color: #444; }
.code-header .copy-btn:active { background: #d0d7de; }
table { border-collapse: collapse; width: 100%; margin: 16px 0; display: block; overflow-x: auto; }
th, td { border: 1px solid #d0d7de; padding: 8px 12px; text-align: left; }
th { background: #f6f8fa; font-weight: 600; }
tr:nth-child(even) { background: #fafbfc; }
img { max-width: 100%; height: auto; border-radius: 4px; }
a { color: #0969da; text-decoration: none; }
ul, ol { padding-left: 24px; margin: 0 0 16px; }
li { margin: 4px 0; }

/* Task list (GFM) */
.task-list-item { list-style: none; margin-left: -24px; padding-left: 24px; position: relative; }
.task-list-item input[type=checkbox] { position: absolute; left: 0; top: 4px; width: 16px; height: 16px; cursor: default; accent-color: #0969da; }
.task-list-item::before { display: none; }

hr { border: none; border-top: 1px solid #eaecef; margin: 24px 0; }
.katex { font-size: 1.1em; }
""".trimIndent()

private const val DEFAULT_CSS = ""
private val ACADEMIC_CSS = """
body { font-family: 'Georgia', 'Times New Roman', serif; font-size: 16px; line-height: 1.8; }
h1, h2 { font-family: 'Georgia', serif; font-weight: 700; }
code { font-family: 'Courier New', monospace; }
""".trimIndent()
private val DARK_CSS = """
body { background: #1e1e1e; color: #d4d4d4; }
h1, h2 { border-bottom-color: #404040; }
a { color: #569cd6; }
code { background: #2d2d2d; }
pre { background: #2d2d2d; border-color: #404040; }
blockquote { border-left-color: #569cd6; color: #9e9e9e; }
th, td { border-color: #404040; }
th { background: #2d2d2d; }
tr:nth-child(even) { background: #252526; }
hr { border-top-color: #404040; }
""".trimIndent()
private val MINIMAL_CSS = """
body { font-family: 'Helvetica Neue', Arial, sans-serif; font-size: 14px; line-height: 1.5; color: #333; }
h1 { font-size: 1.5em; border: none; font-weight: 600; }
h2 { font-size: 1.3em; border: none; font-weight: 600; }
h3 { font-size: 1.15em; font-weight: 600; }
pre { background: #f9f9f9; border: none; border-radius: 0; }
blockquote { border-left-width: 2px; }
table { font-size: 0.95em; }
th, td { padding: 6px 10px; }
""".trimIndent()
private val PURE_BLACK_CSS = """
body { background: #000; color: #e0e0e0; }
h1, h2 { border-bottom-color: #333; }
a { color: #66b3ff; }
code { background: #111; }
pre { background: #0a0a0a; border-color: #222; }
blockquote { border-left-color: #66b3ff; color: #999; }
th, td { border-color: #333; }
th { background: #111; }
tr:nth-child(even) { background: #0d0d0d; }
hr { border-top-color: #333; }
.code-header { background: #1a1a1a; border-color: #222; }
.code-header .lang-label { color: #999; }
.code-header .copy-btn { border-color: #444; color: #ccc; background: #1a1a1a; }
""".trimIndent()
private val PRINT_CSS = """
body {
    font-family: 'Georgia', 'Times New Roman', serif;
    font-size: 11pt; line-height: 1.6; color: #000;
    padding: 20mm 15mm; max-width: 100%;
    orphans: 3; widows: 3;
}
h1 { font-size: 18pt; border-bottom: 1px solid #ccc; padding-bottom: 0.3em; margin: 0.8em 0 0.4em; page-break-before: always; }
h1:first-of-type { page-break-before: avoid; }
h2 { font-size: 14pt; border-bottom: 1px solid #eaecef; padding-bottom: 0.2em; margin: 1em 0 0.4em; }
h3 { font-size: 12pt; margin: 0.8em 0 0.3em; }
h4 { font-size: 11pt; margin: 0.6em 0 0.2em; }
p { margin: 0 0 0.5em; text-align: justify; }
code { background: #f0f0f0; padding: 1px 4px; font-family: 'Consolas', 'Monaco', monospace; font-size: 9pt; }
pre { background: #f8f8f8; padding: 8pt 10pt; border: 0.5pt solid #ddd; overflow-x: hidden; }
pre code { background: none; padding: 0; font-size: 9pt; }
blockquote { border-left: 2pt solid #ccc; margin: 0.5em 0; padding: 0 10pt; color: #555; }
blockquote p { margin: 0 0 4pt; }
blockquote p:last-child { margin: 0; }
.code-header { display: flex; justify-content: space-between; align-items: center; padding: 3pt 8pt; background: #eee; font-size: 8pt; font-family: 'Georgia', serif; border: 0.5pt solid #ddd; border-bottom: none; }
.code-header + pre { margin-top: 0; border-top: none; }
.code-header .lang-label { color: #555; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5pt; font-size: 7pt; }
.code-header .copy-btn { background: none; border: 0.5pt solid #ccc; border-radius: 2pt; padding: 1pt 5pt; font-size: 7pt; color: #444; }
table { border-collapse: collapse; width: 100%; margin: 0.5em 0; }
th, td { border: 0.5pt solid #aaa; padding: 4pt 6pt; text-align: left; font-size: 10pt; }
th { background: #eee; font-weight: 600; }
img { max-width: 100%; height: auto; }
a { color: #000; text-decoration: underline; }
ul, ol { padding-left: 20pt; margin: 0.3em 0; }
li { margin: 1pt 0; }
hr { border: none; border-top: 0.5pt solid #ccc; margin: 0.8em 0; }
.katex { font-size: 1em; }
""".trimIndent()
