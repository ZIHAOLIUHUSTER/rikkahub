package me.rerere.ui.markdown

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class RoleThoughtParserTest {
    private fun thoughts(source: String) = parseRoleThoughts(source).segments.filterIsInstance<RoleThoughtSegment.Thought>()

    @Test fun `normal thought preserves original Markdown body and narrative`() {
        val source = "<心声>\n她有些**紧张**。\n\n但她很*高兴*。\n</心声>\n\n*她看向窗外。*\n\n“天气不错。”"
        val document = parseRoleThoughts(source)
        assertEquals(source, document.source)
        assertEquals(source, document.segments.joinToString("") { it.source })
        val thought = document.segments.first() as RoleThoughtSegment.Thought
        assertEquals("\n她有些**紧张**。\n\n但她很*高兴*。\n", thought.content)
        assertTrue(parseMarkdown(thought.content).copyPlainText().contains("她有些紧张"))
        assertEquals(source.substringAfter("</心声>"), document.segments.last().source)
    }

    @Test fun `multiple blocks at beginning middle and end stay independent`() {
        val source = "前文<心声>第一段</心声>中间\n<心声>第二段</心声>后文<心声>第三段</心声>"
        val found = thoughts(source)
        assertEquals(listOf("第一段", "第二段", "第三段"), found.map { it.content })
        assertEquals(found.size, found.map { it.startOffset }.distinct().size)
        assertEquals(source, parseRoleThoughts(source).segments.joinToString("") { it.source })
    }

    @Test fun `Markdown code examples never become thought cards`() {
        for (source in listOf(
            "```xml\n<心声>代码</心声>\n```",
            "~~~xml\n<心声>代码</心声>\n~~~",
            "````xml\n```\n<心声>代码</心声>\n````",
            "```xml\n<心声>未闭合围栏中的例子</心声>",
            "`<心声>行内代码</心声>`",
            "`` `<心声>多反引号</心声>` ``",
            "    <心声>缩进代码</心声>",
            "> ```xml\n> <心声>引用中的代码</心声>\n> ```",
            "<pre>\n<心声>HTML 代码示例</心声>\n</pre>",
        )) {
            assertTrue(source, thoughts(source).isEmpty())
            assertEquals(source, parseRoleThoughts(source).segments.joinToString("") { it.source })
        }
    }

    @Test fun `literal closing tags in code do not close a real thought`() {
        val source = "<心声>正文 `</心声>`\n\n```xml\n</心声>\n```\n结尾</心声>后文"
        val thought = thoughts(source).single()
        assertTrue(thought.content.endsWith("结尾"))
        assertTrue(thought.content.contains("`</心声>`"))
        assertEquals("后文", parseRoleThoughts(source).segments.last().source)
    }

    @Test fun `unclosed nested and malformed tags keep source intact`() {
        for (source in listOf(
            "<心声>正在思考", "<心声>正在思考</心", "</心声>只有结束标签",
            "<心声>外层<心声>内层</心声>外层</心声>",
            "<心声>外层<心声>内层</心声>", "<心声 attr='x'>内容</心声>",
        )) {
            assertTrue(source, thoughts(source).isEmpty())
            assertEquals(source, parseRoleThoughts(source).segments.joinToString("") { it.source })
        }
    }

    @Test fun `malformed group does not suppress later independent blocks`() {
        val source = "<心声>外<心声>内</心声></心声>文本<心声>正确</心声><心声>未闭合"
        assertEquals(listOf("正确"), thoughts(source).map { it.content })
        assertEquals(source, parseRoleThoughts(source).segments.joinToString("") { it.source })
    }

    @Test fun `escaped tags and think tags remain ordinary source`() {
        val source = "\\<心声>字面值\\</心声> &lt;心声&gt;字面值&lt;/心声&gt; <think>推理</think>"
        assertTrue(thoughts(source).isEmpty())
        assertEquals(source, parseRoleThoughts(source).source)
    }

    @Test fun `streaming prefixes lose nothing and only closed blocks become cards`() {
        val source = "<心声>她有些紧张\n\n但很高兴。</心声>"
        for (length in 0..source.length) {
            val prefix = source.take(length)
            val parsed = parseRoleThoughts(prefix)
            assertEquals(prefix, parsed.segments.joinToString("") { it.source })
            assertEquals(if (length == source.length) 1 else 0, thoughts(prefix).size)
        }
        assertEquals(thoughts(source).single(), thoughts(source + "\n\n正文正在继续生成").single())
    }

    @Test fun `long content formulas and whitespace are preserved for copying`() {
        val body = "\r\n**内心** ${'$'}\\alpha + \\frac{a}{b}${'$'}\r\n".repeat(3000)
        val source = "<心声>$body</心声>"
        assertEquals(body, thoughts(source).single().content)
        assertEquals(source, thoughts(source).single().source)
    }

    @Test fun `arbitrary malformed source always round trips`() {
        val random = Random(42)
        val pieces = listOf("<心声>", "</心声>", "```", "~~~", "`", "\\", "正文", "\n", "<心", "${'$'}x${'$'}")
        repeat(200) {
            val source = (0..random.nextInt(30)).joinToString("") { pieces.random(random) }
            val document = parseRoleThoughts(source)
            assertEquals(source, document.segments.joinToString("") { it.source })
            document.segments.forEach { assertEquals(it.source, source.substring(it.startOffset, it.startOffset + it.source.length)) }
        }
    }
}
