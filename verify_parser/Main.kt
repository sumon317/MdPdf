import com.example.mdpdf.MarkdownParser
import com.example.mdpdf.MdTheme

var failures = 0
fun check(name: String, cond: Boolean) { println((if (cond) "PASS" else "FAIL") + "  " + name); if (!cond) failures++ }

fun main() {
    val p = MarkdownParser()
    val d = "$"
    val nbsp = Character.toString(160.toChar())

    var html = p.toHtml("# Hello\n\nWorld")
    check("basic h1", html.contains("<h1>Hello</h1>") && html.contains("<p>World</p>"))

    html = p.toHtml("Inline math: $d x^2$d and $d y_i$d")
    check("inline math restore", html.contains("$d x^2$d") && html.contains("$d y_i$d"))

    html = p.toHtml("Display math:\n$d$d\\sum_{i=1}^n i = n(n+1)/2$d$d")
    check("display math restore", html.contains("\\sum_{i=1}^n i = n(n+1)/2") && html.contains("$d$d"))

    html = p.toHtml("Math: \\(a^2 + b^2 = c^2\\) and \\[E=mc^2\\]")
    check("parenmath restore", html.contains("a^2 + b^2 = c^2") && html.contains("E=mc^2"))

    html = p.toHtml("$d a$d and $d$d b$d$d and \\(c\\) and \\[d\\]")
    check("multiple math", html.contains("$d a$d") && html.contains("$d$d b$d$d") && html.contains("\\(c\\)") && html.contains("\\[d\\]"))

    html = p.toHtml("A $d x^2$d B $d$d y$d$d C \\(z\\) D")
    check("no placeholder leakage", !html.contains(nbsp + "MATH") && !html.contains("MATH0") && !html.contains("MATH1") && !html.contains("MATH2") && !html.contains("MATH3"))

    html = p.toHtml("~~strike~~")
    check("strikethrough", html.contains("<del>strike</del>"))

    html = p.toHtml("| A | B |\n|---|---|\n| 1 | 2 |")
    check("tables", html.contains("<table>") && html.contains("<th>A</th>"))

    html = p.toHtml("<script>alert('xss')</script>")
    check("escape html", html.contains("&lt;script&gt;"))

    html = p.toHtml("[link](javascript:alert(1))")
    check("sanitize urls", !html.contains("javascript:"))

    html = p.toHtml("- [x] Done\n- [ ] Todo")
    check("task list", html.contains("Done") && html.contains("Todo"))

    check("theme default css", p.toHtml("# T", MdTheme.DEFAULT).contains("color: #1a1a1a"))
    check("theme dark css", p.toHtml("# T", MdTheme.DARK).contains("background: #1e1e1e"))
    check("theme academic css", p.toHtml("# T", MdTheme.ACADEMIC).contains("font-family: 'Georgia'"))

    html = p.toHtml("test")
    check("scripts included", html.contains("prism.min.js") && html.contains("katex.min.js") && html.contains("auto-render.min.js") && html.contains("katex.min.css"))

    val printHtml = p.toPrintHtml("# Title\n\nContent")
    check("print css", printHtml.contains("font-size: 11pt") && printHtml.contains("padding: 20mm 15mm") && printHtml.contains("page-break-before: always"))

    html = p.toHtml("")
    check("empty string", html.contains("<body>") && html.contains("</body>"))

    html = p.toHtml("---")
    check("hr", html.contains("<hr"))

    html = p.toHtml("**bold *italic* bold**")
    check("nested", html.contains("<strong>") && html.contains("<em>"))

    html = p.toHtml("price is $5 and $$ broken")
    check("unterminated ok", html.contains("price is"))

    html = p.toHtml("t", MdTheme.DEFAULT, showErrors = false)
    check("hide errors css", html.contains(".katex-error { display: none !important; }"))
    html = p.toHtml("t", MdTheme.DEFAULT, showErrors = true)
    check("show errors css absent", !html.contains("display: none !important"))

    // code block containing $ should not break math extraction
    html = p.toHtml("```\ncost = \"$10\" + \"$20\"\n```")
    check("code block preserved", html.contains("\"$10\"") && html.contains("\"$20\""))

    // user text that literally contains MATH0 must not be corrupted wrongly nor crash
    html = p.toHtml("word MATH0 end $d x^2$d")
    check("literal MATH token survives", html.contains("end") && html.contains("$d x^2$d"))

    val big = StringBuilder()
    for (k in 1..3000) { big.append("Text $d x_$k$d more \\(y^$k\\) end\n\n") }
    val t0 = System.currentTimeMillis()
    p.toHtml(big.toString())
    val dt = System.currentTimeMillis() - t0
    println("large doc parse ms=$dt")
    check("perf under 5s", dt < 5000)

    if (failures == 0) println("ALL CHECKS PASSED") else { println("$failures FAILURES"); kotlin.system.exitProcess(1) }
}
