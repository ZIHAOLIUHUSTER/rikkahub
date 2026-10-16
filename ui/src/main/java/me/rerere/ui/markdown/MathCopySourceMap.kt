package me.rerere.ui.markdown

/**
 * Maps selectable formula placeholders to source, rather than guessing from rendered text.
 * Tokens are single characters absent from the message, so selecting a formula is atomic
 * and literal user text (including `[Latex]`) is never rewritten.
 */
internal class MathCopySourceMap(source: String) {
    private val reserved = (source + org.jsoup.parser.Parser.unescapeEntities(source, false)).toSet()
    private val formulas = mutableMapOf<String, Char>()
    private val replacements = mutableMapOf<Char, String>()
    private var next = 0xE000

    @Synchronized
    fun register(source: String): Char? {
        formulas[source]?.let { return it }
        while (next <= 0xF8FF && next.toChar() in reserved) next++
        if (next > 0xF8FF) return null // Caller shows selectable source if the pool is exhausted.
        val token = (next++).toChar()
        formulas[source] = token
        replacements[token] = source
        return token
    }

    @Synchronized
    fun restore(text: String): String = buildString {
        text.forEach { char -> append(replacements[char] ?: char.toString()) }
    }
}
