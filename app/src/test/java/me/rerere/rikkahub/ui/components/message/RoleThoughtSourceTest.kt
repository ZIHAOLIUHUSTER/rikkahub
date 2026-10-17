package me.rerere.rikkahub.ui.components.message

import kotlinx.serialization.json.Json
import me.rerere.ai.ui.UIMessage
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.ui.markdown.parseRoleThoughts
import org.junit.Assert.*
import org.junit.Test

class RoleThoughtSourceTest {
    @Test fun `render parsing leaves message serialization and history text unchanged`() {
        val source = "<心声>\n**角色心声** ${'$'}\\alpha${'$'}\n</心声>\n\n剧情。"
        val message = UIMessage.assistant(source)
        val serialized = Json.encodeToString(UIMessage.serializer(), message)
        parseRoleThoughts(message.toText())
        assertEquals(source, message.toText())
        assertEquals(serialized, Json.encodeToString(UIMessage.serializer(), message))
    }

    @Test fun `old settings enable feature and explicit off survives serialization`() {
        assertTrue(Json.decodeFromString(DisplaySetting.serializer(), "{}").autoCollapseRoleThoughts)
        val off = DisplaySetting(autoCollapseRoleThoughts = false)
        assertFalse(Json.decodeFromString(DisplaySetting.serializer(), Json.encodeToString(DisplaySetting.serializer(), off)).autoCollapseRoleThoughts)
        assertTrue(off.showThinkingContent)
    }
}
