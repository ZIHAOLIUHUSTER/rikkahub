package me.rerere.ui.markdown

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

/** Display-only spans. Concatenating [RoleThoughtSegment.source] reconstructs the input exactly. */
sealed interface RoleThoughtSegment {
    val startOffset: Int
    val source: String

    data class Text(override val startOffset: Int, override val source: String) : RoleThoughtSegment
    data class Thought(
        override val startOffset: Int,
        override val source: String,
        val content: String,
    ) : RoleThoughtSegment
}

data class RoleThoughtDocument(val source: String, val segments: List<RoleThoughtSegment>)

private const val OPEN_THOUGHT = "<心声>"
private const val CLOSE_THOUGHT = "</心声>"

/** No message transformation: only balanced, non-nested tags outside Markdown code become cards. */
fun parseRoleThoughts(source: String): RoleThoughtDocument {
    fun unchanged() = RoleThoughtDocument(source, listOf(RoleThoughtSegment.Text(0, source)))
    if (OPEN_THOUGHT !in source || CLOSE_THOUGHT !in source) return unchanged()

    // Reuse the existing Markdown grammar for fenced/indented code and code spans,
    // including unclosed fences and fences nested in lists/quotes.
    val tree = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(source)
    val codeRanges = mutableListOf<IntRange>()
    fun collectCode(node: ASTNode) {
        if (node.type == MarkdownElementTypes.CODE_FENCE ||
            node.type == MarkdownElementTypes.CODE_BLOCK ||
            node.type == MarkdownElementTypes.HTML_BLOCK ||
            node.type == MarkdownElementTypes.CODE_SPAN
        ) {
            codeRanges += node.startOffset until node.endOffset
        } else node.children.forEach(::collectCode)
    }
    collectCode(tree)

    val segments = mutableListOf<RoleThoughtSegment>()
    var cursor = 0
    var rangeIndex = 0
    var plainStart = 0
    var openStart = -1
    var depth = 0
    var nested = false
    while (cursor < source.length) {
        while (rangeIndex < codeRanges.size && codeRanges[rangeIndex].last < cursor) rangeIndex++
        val code = codeRanges.getOrNull(rangeIndex)
        if (code != null && cursor >= code.first) {
            cursor = code.last + 1
            continue
        }
        if (source[cursor] != '<') { cursor++; continue }
        var slashes = 0
        var before = cursor - 1
        while (before >= 0 && source[before--] == '\\') slashes++
        if (slashes % 2 != 0) { cursor++; continue }
        when {
            source.startsWith(OPEN_THOUGHT, cursor) -> {
                if (depth == 0) { openStart = cursor; nested = false } else nested = true
                depth++
                cursor += OPEN_THOUGHT.length
            }
            source.startsWith(CLOSE_THOUGHT, cursor) -> {
                if (depth > 0) {
                    depth--
                    if (depth == 0 && !nested) {
                        if (plainStart < openStart) segments += RoleThoughtSegment.Text(plainStart, source.substring(plainStart, openStart))
                        val end = cursor + CLOSE_THOUGHT.length
                        segments += RoleThoughtSegment.Thought(openStart, source.substring(openStart, end), source.substring(openStart + OPEN_THOUGHT.length, cursor))
                        plainStart = end
                    }
                }
                cursor += CLOSE_THOUGHT.length
            }
            else -> cursor++
        }
    }
    if (plainStart < source.length) segments += RoleThoughtSegment.Text(plainStart, source.substring(plainStart))
    return if (segments.isEmpty()) unchanged() else RoleThoughtDocument(source, segments)
}
