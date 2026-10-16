package me.rerere.ui.webview

import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

class GeneratedHtmlPolicyTest {
    @Test
    fun `policy precedes model scripts and cannot be relaxed by a later meta`() {
        val source = """<!doctype html><html><head><script>window.answer = 42</script>
            <meta http-equiv="Content-Security-Policy" content="default-src *"></head>
            <body><button onclick="this.textContent='clicked'">Go</button></body></html>"""
        val document = Jsoup.parse(isolateGeneratedHtml(source, dark = false))
        val first = document.head().children().first()!!
        assertEquals("Content-Security-Policy", first.attr("http-equiv"))
        assertEquals(GENERATED_HTML_CSP, first.attr("content"))
        assertTrue(first.attr("content").contains("connect-src 'none'"))
        assertTrue(first.attr("content").contains("form-action 'none'"))
        assertEquals("this.textContent='clicked'", document.selectFirst("button")!!.attr("onclick"))
        assertEquals("window.answer = 42", document.selectFirst("script")!!.data())
    }

    @Test
    fun `standalone forms canvas svg and inline scripts survive preparation`() {
        val source = """<input type="range" min="1" max="10" oninput="out.value=this.value">
            <output id="out">1</output><canvas id="canvas"></canvas><svg viewBox="0 0 10 10"><circle r="4"/></svg>
            <script>document.getElementById('canvas').getContext('2d').fillRect(0,0,5,5)</script>"""
        val document = Jsoup.parse(isolateGeneratedHtml(source, dark = true))
        assertEquals("range", document.selectFirst("input")!!.attr("type"))
        assertNotNull(document.selectFirst("canvas"))
        assertNotNull(document.selectFirst("svg circle"))
        assertEquals(1, document.select("meta[name=viewport]").size)
        assertTrue(document.selectFirst("style")!!.data().contains("color-scheme: dark"))
    }

    @Test
    fun `theme fallback precedes authored styles and viewport is preserved`() {
        val source = """<head><meta name="viewport" content="width=500"><style>body{background:red}</style></head>"""
        val document = Jsoup.parse(isolateGeneratedHtml(source, dark = false))
        assertEquals("width=500", document.selectFirst("meta[name=viewport]")!!.attr("content"))
        assertTrue(document.select("style").last()!!.data().contains("background:red"))
    }
}
