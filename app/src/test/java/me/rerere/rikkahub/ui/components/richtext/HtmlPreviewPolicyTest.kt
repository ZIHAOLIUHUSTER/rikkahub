package me.rerere.rikkahub.ui.components.richtext

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlPreviewPolicyTest {
    @Test
    fun `closed fence still waits until message streaming ends`() {
        assertFalse(shouldAutoPreviewHtml("html", false, true, true))
        assertFalse(shouldAutoPreviewHtml("html", true, true, true))
        assertFalse(shouldAutoPreviewHtml("html", false, false, true))
        assertTrue(shouldAutoPreviewHtml(" HTML ", true, false, true))
    }

    @Test
    fun `disabled setting and other languages never auto execute`() {
        assertFalse(shouldAutoPreviewHtml("html", true, false, false))
        for (language in listOf("svg", "xml", "javascript", "plaintext", "", "htmlx")) {
            assertFalse(shouldAutoPreviewHtml(language, true, false, true))
        }
    }
}
