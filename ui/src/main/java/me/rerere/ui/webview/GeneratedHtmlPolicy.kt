package me.rerere.ui.webview

import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import java.io.ByteArrayInputStream
import org.jsoup.Jsoup

const val ISOLATED_PREVIEW_ORIGIN = "https://preview.invalid/"

/** No app assets, credentials, network, frames, workers or form submissions. */
internal const val GENERATED_HTML_CSP = "default-src 'none'; script-src 'unsafe-inline' 'unsafe-eval' blob:; style-src 'unsafe-inline'; img-src data: blob:; font-src data:; media-src data: blob:; connect-src 'none'; frame-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'; worker-src 'none'"

/** Insert policy before model-authored scripts; keep their markup and JS intact. */
fun isolateGeneratedHtml(code: String, dark: Boolean): String {
    val document = Jsoup.parse(code)
    document.outputSettings().prettyPrint(false)
    val head = document.head()
    head.prependElement("meta").attr("http-equiv", "Content-Security-Policy").attr("content", GENERATED_HTML_CSP)
    if (head.select("meta[name=viewport]").isEmpty()) {
        head.appendElement("meta").attr("name", "viewport").attr("content", "width=device-width, initial-scale=1")
    }
    head.prependElement("style").text(
        ":root { color-scheme: " + (if (dark) "dark" else "light") + "; } " +
            "body { margin: 0; background-color: " + (if (dark) "#141218" else "#fff") + "; color: " +
            (if (dark) "#e6e1e5" else "#1d1b20") + "; overflow-wrap: anywhere; }"
    )
    // CSP must precede even the theme style and any model-authored executable content.
    head.select("meta[http-equiv=Content-Security-Policy]").first()?.let {
        it.remove()
        head.prependChild(it)
    }
    return document.outerHtml()
}

@Suppress("DEPRECATION")
internal fun WebSettings.restrictGeneratedContent() {
    allowFileAccess = false
    allowContentAccess = false
    allowFileAccessFromFileURLs = false
    allowUniversalAccessFromFileURLs = false
    blockNetworkLoads = true
    blockNetworkImage = true
    domStorageEnabled = false
    databaseEnabled = false
    setGeolocationEnabled(false)
    javaScriptCanOpenWindowsAutomatically = false
    setSupportMultipleWindows(false)
    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
    mediaPlaybackRequiresUserGesture = true
    setSaveFormData(false)
}

internal fun blockedPreviewResponse() = WebResourceResponse(
    "text/plain", "UTF-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0))
)
