package me.rerere.rikkahub.ui.components.richtext

/** Thinking content and unfinished answers remain source-only, including manual previews. */
internal fun canPreviewCodeBlock(
    language: String,
    completeCodeBlock: Boolean,
    isStreaming: Boolean,
    isReasoning: Boolean,
): Boolean = completeCodeBlock && !isStreaming && !isReasoning &&
    language.trim().lowercase() in setOf("html", "svg")

/** Only explicit, closed HTML fences in a completed answer automatically preview. */
internal fun shouldAutoPreviewHtml(
    language: String,
    completeCodeBlock: Boolean,
    isStreaming: Boolean,
    enabled: Boolean,
    isReasoning: Boolean = false,
): Boolean = enabled && language.trim().equals("html", ignoreCase = true) &&
    canPreviewCodeBlock(language, completeCodeBlock, isStreaming, isReasoning)
