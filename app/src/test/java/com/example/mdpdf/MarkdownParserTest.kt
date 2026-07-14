package com.example.mdpdf

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MarkdownParserTest {

    private val parser = MarkdownParser()
    private val dollar = '$'.toString()  // literal dollar sign for math delimiters

    @Test
    fun `toHtml renders basic markdown`() {
        val html = parser.toHtml("# Hello\n\nWorld")

        assertThat(html).contains("<h1>Hello</h1>")
        assertThat(html).contains("<p>World</p>")
    }

    @Test
    fun `toHtml protects and restores inline math`() {
        val html = parser.toHtml("Inline math: $dollar x^2$dollar and $dollar y_i$dollar")

        assertThat(html).contains("x^2")
        assertThat(html).contains("y_i")
        assertThat(html).contains("$dollar x^2$dollar")
        assertThat(html).contains("$dollar y_i$dollar")
    }

    @Test
    fun `toHtml protects and restores display math`() {
        val html = parser.toHtml("Display math:\n$dollar$dollar\\sum_{i=1}^n i = n(n+1)/2$dollar$dollar")

        assertThat(html).contains("\\sum_{i=1}^n i = n(n+1)/2")
        assertThat(html).contains("$dollar$dollar")
    }

    @Test
    fun `toHtml protects parenthesis math delimiters`() {
        val html = parser.toHtml("Math: \\(a^2 + b^2 = c^2\\) and \\[E=mc^2\\]")

        assertThat(html).contains("a^2 + b^2 = c^2")
        assertThat(html).contains("E=mc^2")
    }

    @Test
    fun `toHtml renders tables`() {
        val md = """
            | A | B |
            |---|---|
            | 1 | 2 |
        """.trimIndent()

        val html = parser.toHtml(md)

        assertThat(html).contains("<table>")
        assertThat(html).contains("<th>A</th>")
        assertThat(html).contains("<th>B</th>")
        assertThat(html).contains("<td>1</td>")
        assertThat(html).contains("<td>2</td>")
    }

    @Test
    fun `toHtml renders strikethrough`() {
        val html = parser.toHtml("~~strikethrough~~")

        assertThat(html).contains("<del>strikethrough</del>")
    }

    @Test
    fun `toHtml renders code blocks with language`() {
        val md = """
            ```kotlin
            fun main() = println("Hello")
            ```
        """.trimIndent()

        val html = parser.toHtml(md)

        assertThat(html).contains("<pre")
        assertThat(html).contains("language-kotlin")
        assertThat(html).contains("fun main()")
    }

    @Test
    fun `toHtml renders blockquotes`() {
        val html = parser.toHtml("> Quote\n> Second line")

        assertThat(html).contains("<blockquote>")
        assertThat(html).contains("Quote")
        assertThat(html).contains("Second line")
    }

    @Test
    fun `toHtml renders checkboxes as list items`() {
        val md = """
                - [x] Done
                - [ ] Todo
            """.trimIndent()

        val html = parser.toHtml(md)

        // commonmark renders checkboxes as list items with [x]/[ ] preserved as text
        assertThat(html).contains("Done")
        assertThat(html).contains("Todo")
        assertThat(html).contains("<li>")
    }

    @Test
    fun `toHtml applies theme CSS`() {
        val htmlDefault = parser.toHtml("# Title", MdTheme.DEFAULT)
        val htmlDark = parser.toHtml("# Title", MdTheme.DARK)
        val htmlAcademic = parser.toHtml("# Title", MdTheme.ACADEMIC)

        assertThat(htmlDefault).contains("color: #1a1a1a")
        assertThat(htmlDark).contains("background: #1e1e1e")
        assertThat(htmlAcademic).contains("font-family: 'Georgia'")
    }

    @Test
    fun `toHtml includes Prism and KaTeX scripts`() {
        val html = parser.toHtml("test")

        assertThat(html).contains("prism.min.js")
        assertThat(html).contains("katex.min.js")
        assertThat(html).contains("auto-render.min.js")
        assertThat(html).contains("katex.min.css")
    }

    @Test
    fun `toPrintHtml uses print-specific CSS`() {
        val printHtml = parser.toPrintHtml("# Title\n\nContent")

        assertThat(printHtml).contains("font-size: 11pt")
        assertThat(printHtml).contains("padding: 20mm 15mm")
        assertThat(printHtml).contains("page-break-before: always")
    }

    @Test
    fun `toHtml handles empty string`() {
        val html = parser.toHtml("")

        assertThat(html).contains("<body>")
        assertThat(html).contains("</body>")
    }

    @Test
    fun `toHtml handles multiple math expressions`() {
        val md = "$dollar a$dollar and $dollar$dollar b$dollar$dollar and \\(c\\) and \\[d\\]"
        val html = parser.toHtml(md)

        assertThat(html).contains("$dollar a$dollar")
        assertThat(html).contains("$dollar$dollar b$dollar$dollar")
        assertThat(html).contains("\\(c\\)")
        assertThat(html).contains("\\[d\\]")
    }

    @Test
    fun `toHtml escapes HTML by default`() {
        val html = parser.toHtml("<script>alert('xss')</script>")

        // HTML raw tags should be present but escaped
        assertThat(html).contains("&lt;script&gt;")
    }

    @Test
    fun `toHtml sanitizes URLs`() {
        val html = parser.toHtml("[link](javascript:alert(1))")

        // URL should be sanitized/removed
        assertThat(html).doesNotContain("javascript:")
    }

    @Test
    fun `toHtml renders nested formatting`() {
        val html = parser.toHtml("**bold *italic* bold**")

        assertThat(html).contains("<strong>")
        assertThat(html).contains("<em>")
    }

    @Test
    fun `toHtml renders horizontal rule`() {
        val html = parser.toHtml("---")

        assertThat(html).contains("<hr")
    }

    @Test
    fun `toHtml renders ordered list`() {
        val md = """
            1. First
            2. Second
        """.trimIndent()

        val html = parser.toHtml(md)

        assertThat(html).contains("<ol>")
        assertThat(html).contains("First")
        assertThat(html).contains("Second")
    }

    @Test
    fun `toHtml renders unordered list`() {
        val md = """
            - Item 1
            - Item 2
        """.trimIndent()

        val html = parser.toHtml(md)

        assertThat(html).contains("<ul>")
        assertThat(html).contains("Item 1")
        assertThat(html).contains("Item 2")
    }
}
