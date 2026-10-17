package me.rerere.rikkahub.ui.components.richtext

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import me.rerere.ui.webview.ISOLATED_PREVIEW_ORIGIN
import me.rerere.ui.webview.WebView
import me.rerere.ui.webview.rememberWebViewState
import kotlin.math.abs
import kotlin.math.ceil

/** Visible previews share a small budget; off-screen content never keeps a WebView. */
private object HtmlPreviewBudget {
    val visible = mutableStateListOf<Any>()
    const val LIMIT = 2
}

@Composable
internal fun CodeBlockPreview(code: String, language: String, modifier: Modifier = Modifier) {
    val density = LocalDensity.current.density
    val html = remember(code, language) { buildCodePreviewHtml(code, language) }
    var visible by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var contentHeight by remember(code) { mutableStateOf(200) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val token = remember { Any() }
    DisposableEffect(visible, resumed) {
        if (visible && resumed) HtmlPreviewBudget.visible.add(token)
        onDispose { HtmlPreviewBudget.visible.remove(token) }
    }
    val active = visible && resumed && token in HtmlPreviewBudget.visible.take(HtmlPreviewBudget.LIMIT)
    val maxHeight = if (expanded) 720 else 360
    val height = contentHeight.coerceIn(96, maxHeight)
    Column(modifier) {
        Box(
            modifier = Modifier.fillMaxWidth().height(height.dp).onGloballyPositioned {
                visible = !it.boundsInWindow().isEmpty
            },
            contentAlignment = Alignment.Center,
        ) {
            if (active) {
                val state = rememberWebViewState(
                    data = html,
                    baseUrl = ISOLATED_PREVIEW_ORIGIN,
                    mimeType = "text/html",
                    isolatedContent = true,
                    settings = {
                        builtInZoomControls = true
                        displayZoomControls = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        textZoom = 100
                    },
                )
                WebView(state = state, modifier = Modifier.fillMaxWidth().height(height.dp))
                // Read-only JS evaluation avoids introducing any Android JS interface.
                // Poll only visible/resumed previews; a threshold prevents layout jitter.
                LaunchedEffect(state, density) {
                    while (true) {
                        val view = state.webView
                        if (view != null && !state.isLoading) {
                            view.evaluateJavascript(HTML_HEIGHT_SCRIPT) { result ->
                                val measured = result.toDoubleOrNull()
                                if (measured != null && measured.isFinite() && measured > 0) {
                                    @Suppress("DEPRECATION")
                                    val measuredDp = measured * view.scale / density
                                    val next = (ceil(measuredDp / 4) * 4).toInt().coerceIn(96, 10000)
                                    if (abs(next - contentHeight) >= 4) contentHeight = next
                                }
                            }
                        }
                        delay(500)
                    }
                }
            } else {
                Text(if (visible && resumed) "Preview paused — use fullscreen to interact" else "HTML preview")
            }
        }
        if (contentHeight > 360) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Collapse preview" else "Expand preview")
            }
        }
    }
}

private const val HTML_HEIGHT_SCRIPT = """
(function() {
    if (!document.body) return 0;
    var b = document.body, s = getComputedStyle(b);
    return Math.ceil(Math.max(b.scrollHeight, b.getBoundingClientRect().height) +
        (parseFloat(s.marginTop) || 0) + (parseFloat(s.marginBottom) || 0));
})()
"""

internal fun buildCodePreviewHtml(code: String, language: String): String =
    if (language == "svg") {
        """<!DOCTYPE html><html><body style="margin:0;display:flex;justify-content:center;align-items:center;">$code</body></html>"""
    } else code
