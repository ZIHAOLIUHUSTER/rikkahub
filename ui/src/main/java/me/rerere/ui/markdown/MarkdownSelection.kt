package me.rerere.ui.markdown

import android.content.ClipData
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp

internal val LocalMathCopySourceMap = staticCompositionLocalOf<MathCopySourceMap?> { null }

/** A message-wide selection scope, including display formulas drawn on Canvas. */
@Suppress("DEPRECATION")
@Composable
fun MarkdownSelectionContainer(source: String, content: @Composable () -> Unit) {
    val sources = remember(source) { MathCopySourceMap(source) }
    val clipboard = LocalClipboard.current
    val legacyClipboard = LocalClipboardManager.current
    val mappedClipboard = remember(clipboard, sources) {
        object : Clipboard by clipboard {
            override suspend fun setClipEntry(clipEntry: ClipEntry?) {
                clipboard.setClipEntry(clipEntry?.restoreMath(sources))
            }
        }
    }
    val mappedLegacyClipboard = remember(legacyClipboard, sources) {
        object : ClipboardManager by legacyClipboard {
            override fun setText(annotatedString: AnnotatedString) {
                val restored = sources.restore(annotatedString.text)
                legacyClipboard.setText(if (restored == annotatedString.text) annotatedString else AnnotatedString(restored))
            }

            override fun setClip(clipEntry: ClipEntry?) {
                legacyClipboard.setClip(clipEntry?.restoreMath(sources))
            }
        }
    }
    CompositionLocalProvider(
        LocalMathCopySourceMap provides sources,
        LocalClipboard provides mappedClipboard,
        LocalClipboardManager provides mappedLegacyClipboard,
    ) {
        SelectionContainer(content = content)
    }
}

private fun ClipEntry.restoreMath(sources: MathCopySourceMap): ClipEntry {
    if (clipData.itemCount != 1) return this
    val text = clipData.getItemAt(0).text?.toString() ?: return this
    val restored = sources.restore(text)
    return if (restored == text) this else ClipEntry(ClipData.newPlainText("text", restored))
}

/**
 * Keep a selectable Text at the exact bounds of a rendered block. Its one-character
 * placeholder expands to the complete source on copy, including multiline LaTeX.
 * The visible renderer stays above it so formula long presses still work.
 */
@Composable
internal fun SelectableMathBlock(source: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val sources = LocalMathCopySourceMap.current
    val token = remember(sources, source) { sources?.register(source) }
    if (sources == null) {
        Box(modifier) { content() }
        return
    }
    if (token == null) {
        Text(source, modifier)
        return
    }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    Box(modifier) {
        if (size.width > 0 && size.height > 0) {
            val text = remember(token) { buildAnnotatedString { appendInlineContent("math-block", token.toString()) } }
            Text(
                text = text,
                modifier = Modifier.matchParentSize().semantics { this.text = AnnotatedString(source) },
                style = TextStyle(fontSize = 0.sp, lineHeight = 0.sp),
                inlineContent = mapOf("math-block" to InlineTextContent(
                    Placeholder(
                        with(density) { size.width.toSp() },
                        with(density) { size.height.toSp() },
                        PlaceholderVerticalAlign.Top,
                    ),
                ) {}),
            )
        }
        Box(Modifier.onSizeChanged { size = it }) { DisableSelection { content() } }
    }
}
