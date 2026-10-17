package me.rerere.ui.webview

import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import java.io.ByteArrayInputStream

const val ISOLATED_PREVIEW_ORIGIN = "https://preview.invalid/"

/** No app assets, credentials, network, frames, workers or form submissions. */
internal const val GENERATED_HTML_CSP = "default-src 'none'; script-src 'unsafe-inline' 'unsafe-eval' blob:; style-src 'unsafe-inline'; img-src data: blob:; font-src data:; media-src data: blob:; connect-src 'none'; frame-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'; worker-src 'none'"

/** Serve the original document locally. Headers enforce isolation without rewriting HTML/SVG. */
internal fun generatedHtmlResponse(data: String) = WebResourceResponse(
    "text/html", "UTF-8", 200, "OK",
    mapOf("Content-Security-Policy" to GENERATED_HTML_CSP),
    ByteArrayInputStream(data.toByteArray(Charsets.UTF_8)),
)

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
