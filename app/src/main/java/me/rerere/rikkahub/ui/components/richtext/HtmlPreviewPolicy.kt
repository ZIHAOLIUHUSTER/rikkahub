package me.rerere.rikkahub.ui.components.richtext

/** Only explicit, closed HTML fences may automatically execute, after streaming ends. */
internal fun shouldAutoPreviewHtml(
    language: String,
    completeCodeBlock: Boolean,
    isStreaming: Boolean,
    enabled: Boolean,
): Boolean = enabled && completeCodeBlock && !isStreaming && language.trim().equals("html", ignoreCase = true)
