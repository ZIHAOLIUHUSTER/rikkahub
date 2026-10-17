package me.rerere.ui.webview

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real WebView regressions: JVM parsing tests cannot detect a blocked main document. */
@RunWith(AndroidJUnit4::class)
class GeneratedHtmlWebViewTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun originalSvgAndInteractiveHtmlLoadWithoutAnErrorPage() {
        val state = show(FIXTURE)
        awaitPage(state, "ready")
        assertEquals("\"光腰\"", evaluate(state, "document.querySelector('text').textContent"))
        assertEquals("true", evaluate(state, "document.querySelector('svg').getBBox().width > 0"))
        assertEquals("\"url(#ar)\"", evaluate(state, "document.querySelector('line').getAttribute('marker-end')"))
        assertEquals("255", evaluate(state, "document.querySelector('canvas').getContext('2d').getImageData(2,2,1,1).data[0]"))
        assertEquals("\"73\"", evaluate(state, """
            var slider = document.querySelector('input');
            slider.value = 73; slider.dispatchEvent(new Event('input'));
            document.querySelector('output').textContent;
        """))
        assertEquals("\"Clicked\"", evaluate(state, "document.querySelector('button').click(); document.querySelector('button').textContent"))
        assertEquals("true", evaluate(state, "document.body.scrollHeight >= 900"))
        assertEquals(ISOLATED_PREVIEW_ORIGIN, state.currentUrl)
    }

    @Test
    fun networkAndAppAssetsStayBlockedWhileLocalScriptsRun() {
        val state = show(FIXTURE + """
            <script>
            window.blocked = 0;
            Promise.all([
                fetch('https://example.com/private'),
                fetch('https://rikkahub.local/assets/html/mermaid.min.js'),
                fetch('file:///data/local/tmp/private')
            ].map(p => p.then(r => { if (!r.ok) window.blocked++; }, () => window.blocked++)));
            </script>
        """)
        awaitPage(state, "ready")
        compose.waitUntil(15_000) { evaluate(state, "window.blocked") == "3" }
        assertEquals("\"undefined\"", evaluate(state, "typeof window.searchBoxJavaBridge_"))
        compose.runOnIdle {
            assertFalse(state.webView!!.settings.allowFileAccess)
            assertFalse(state.webView!!.settings.allowContentAccess)
            assertTrue(state.webView!!.settings.blockNetworkLoads)
        }
        assertEquals("42", evaluate(state, "6 * 7"))
    }

    @Test
    fun replacementReloadsTheDocumentAndLeavingCompositionReleasesIt() {
        val shown = mutableStateOf(true)
        val state = WebViewState(WebContent.Data(FIXTURE), isolatedContent = true)
        compose.setContent { if (shown.value) WebView(state, Modifier.height(240.dp)) }
        awaitPage(state, "ready")
        compose.runOnIdle { state.loadData("<title>replacement</title><body>New source</body>", mimeType = "text/html") }
        awaitPage(state, "replacement")
        assertEquals("\"New source\"", evaluate(state, "document.body.textContent"))
        compose.runOnIdle { shown.value = false }
        compose.waitForIdle()
        assertNull(state.webView)
    }

    private fun show(html: String): WebViewState {
        val state = WebViewState(WebContent.Data(html), isolatedContent = true)
        compose.setContent { WebView(state, Modifier.height(240.dp)) }
        return state
    }

    private fun awaitPage(state: WebViewState, title: String) {
        compose.waitUntil(20_000) { state.webView != null && !state.isLoading && state.pageTitle == title }
    }

    private fun evaluate(state: WebViewState, script: String): String {
        val done = CountDownLatch(1)
        var result = ""
        compose.runOnUiThread {
            state.webView!!.evaluateJavascript(script) { result = it; done.countDown() }
        }
        assertTrue("JavaScript evaluation timed out", done.await(10, TimeUnit.SECONDS))
        return result
    }

    companion object {
        private const val FIXTURE = """<!doctype html><html><head><title>ready</title>
            <meta name="viewport" content="width=device-width, initial-scale=1"></head><body>
            <svg width="600" height="240" viewBox="0 0 600 240">
            <defs><marker id="ar" markerWidth="6" markerHeight="6" refX="5" refY="3" orient="auto"><path d="M0,0 L6,3 L0,6 Z"/></marker></defs>
            <line x1="20" y1="120" x2="580" y2="120" stroke="black" marker-end="url(#ar)"/><text x="20" y="100">光腰</text></svg>
            <input type="range" min="0" max="100" oninput="document.querySelector('output').textContent=this.value"><output>0</output>
            <button onclick="this.textContent='Clicked'">Click</button><canvas width="10" height="10"></canvas>
            <div style="height:900px">Tall content</div>
            <script>var ctx = document.querySelector('canvas').getContext('2d');ctx.fillStyle='red';ctx.fillRect(0,0,10,10)</script>
            </body></html>"""
    }
}
