package me.rerere.ui.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MarkdownCopyTest {
    @Test
    fun `copy preserves LaTeX backslashes braces and delimiters`() {
        val formula = """\frac{a}{b} + \alpha + \begin{matrix}1 & 2 \\ 3 & 4\end{matrix}"""
        assertEquals("$" + formula + "$", MarkdownInline.Math(formula).copySource())
        assertEquals("$$\n" + formula + "\n$$", MarkdownInline.Math(formula, true).copySource())
    }

    @Test
    fun `plain text drops formatting using the tree and keeps code and math intact`() {
        val source = """**Answer** with ${'$'}\frac{1}{2}${'$'} and `a_b \path`.

```text
**literal** \frac{x}{y}
```"""
        val document = parseMarkdown(source)
        assertEquals(source, document.source)
        assertEquals("Answer with $" + """\frac{1}{2}""" + "$ and a_b \\path.\n\n**literal** \\frac{x}{y}", document.copyPlainText())
        assertFalse(document.copyPlainText().contains("[Latex]"))
    }

    @Test
    fun `plain text retains structured lists tables and display math`() {
        val document = MarkdownDocument("untouched", listOf(
            MarkdownBlock.ListBlock(true, listOf(ListItem(listOf(
                MarkdownBlock.Paragraph(listOf(MarkdownInline.Text("item")))
            ))), start = 3),
            MarkdownBlock.Paragraph(listOf(MarkdownInline.Math("x = y", true))),
        ))
        assertEquals("3. item\n\n$$\nx = y\n$$", document.copyPlainText())
        assertEquals("untouched", document.source)
    }
}
