package me.rerere.ui.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.*
import org.junit.Test

class MathCopySourceMapTest {
    @Test
    fun `a large selection keeps prose inline formulas and display derivations in order`() {
        val source = """推导依据
高斯光束波前曲率半径：
${'$'}${'$'}R(z)=z\left[1+\left(\frac{z_R}{z}\right)^2\right]${'$'}${'$'}
平面镜在 ${'$'}z=0${'$'}，所以光腰在 ${'$'}z_0=0${'$'}。
凹面镜在 ${'$'}z=L${'$'}，要求：
${'$'}${'$'}
R(L)=R_m
\Longrightarrow L^2-R_m L+z_R^2=0
${'$'}${'$'}"""
        val mapping = MathCopySourceMap(source)
        val radius = """${'$'}${'$'}R(z)=z\left[1+\left(\frac{z_R}{z}\right)^2\right]${'$'}${'$'}"""
        val derivation = """${'$'}${'$'}
R(L)=R_m
\Longrightarrow L^2-R_m L+z_R^2=0
${'$'}${'$'}"""
        val selected = "推导依据\n高斯光束波前曲率半径：\n" + mapping.register(radius) +
            "\n平面镜在 " + mapping.register("${'$'}z=0${'$'}") + "，所以光腰在 " + mapping.register("${'$'}z_0=0${'$'}") +
            "。\n凹面镜在 " + mapping.register("${'$'}z=L${'$'}") + "，要求：\n" + mapping.register(derivation)
        assertEquals(source, mapping.restore(selected))
        assertFalse(mapping.restore(selected).contains("[Latex]"))
    }

    @Test
    fun `literal placeholder words and private characters in prose are never changed`() {
        val literal = "[Latex]\uE000 &#xe001;"
        val mapping = MathCopySourceMap(literal)
        val token = mapping.register("${'$'}\\alpha${'$'}")!!
        assertNotEquals('\uE000', token)
        assertNotEquals('\uE001', token)
        assertEquals("[Latex]\uE000 \uE001 ${'$'}\\alpha${'$'}", mapping.restore("[Latex]\uE000 \uE001 $token"))
    }

    @Test
    fun `restoration does not recursively rewrite inserted source`() {
        val mapping = MathCopySourceMap("")
        val first = mapping.register("x")!!
        val second = mapping.register(first.toString())!!
        assertEquals(first.toString(), mapping.restore(second.toString()))
        assertEquals(first, mapping.register("x"))
    }

    @Test
    fun `visual fragments own the source once and add no zero width spaces`() {
        val mapping = MathCopySourceMap("")
        val theme = MarkdownTheme(
            headings = emptyList(), link = SpanStyle(), inlineCode = SpanStyle(),
            blockSpacing = 0.dp, markerColor = Color.Black, quoteColor = Color.Black, dividerColor = Color.Black,
        )
        val components = MarkdownComponents(inlineMath = {
            List(3) { MarkdownInlineWidget(Placeholder(10.sp, 10.sp, PlaceholderVerticalAlign.Top)) {} }
        })
        val rendered = InlineTextBuilder(
            MarkdownContext(theme, components, null),
            MarkdownInlineScope(TextStyle(fontSize = 14.sp), Density(1f), Color.Black),
            mapping,
        ).build(listOf(MarkdownInline.Text("before "), MarkdownInline.Math("\\alpha + \\beta"), MarkdownInline.Text(" after")))
        assertEquals(3, rendered.inlineContent.size)
        assertEquals("before ${'$'}\\alpha + \\beta${'$'} after", mapping.restore(rendered.text.text))
        assertFalse(rendered.text.text.contains('\u200B'))
    }

    @Test
    fun `pool exhaustion falls back instead of corrupting formula source`() {
        val mapping = MathCopySourceMap((0xE000..0xF8FF).map { it.toChar() }.joinToString(""))
        assertNull(mapping.register("x"))
    }
}
