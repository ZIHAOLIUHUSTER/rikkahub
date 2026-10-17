package me.rerere.rikkahub.ui.components.message

import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.Navigator
import me.rerere.ui.markdown.MarkdownSelectionContainer
import me.rerere.ui.markdown.parseRoleThoughts
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoleThoughtMessageTest {
    @get:Rule val compose = createComposeRule()

    @Test fun defaultsCollapsedThenExpandsAndCollapsesWithoutHidingNarrative() {
        show("<心声>她其实有些**紧张**。</心声>\n\n她看向窗外。")
        compose.onNodeWithText("角色心声").assertExists()
        compose.onNodeWithText("她其实有些紧张。", substring = true).assertDoesNotExist()
        compose.onNodeWithText("她看向窗外。").assertExists()
        compose.onNodeWithText("角色心声").performClick()
        awaitText("她其实有些紧张。")
        compose.onNodeWithText("她看向窗外。").assertExists()
        compose.onNodeWithText("角色心声").performClick()
        compose.onNodeWithText("她其实有些紧张。", substring = true).assertDoesNotExist()
    }

    @Test fun blocksExpandIndependentlyAndCopyExactInnerMarkdownWhileCollapsed() {
        val body = "\n**第二段** ${'$'}\\alpha + \\frac{a}{b}${'$'}\n"
        show("<心声>第一段心声。</心声>\n\n正文。\n\n<心声>$body</心声>")
        compose.onAllNodesWithText("角色心声")[0].performClick()
        awaitText("第一段心声。")
        compose.onNodeWithText("第二段", substring = true).assertDoesNotExist()
        compose.onAllNodesWithContentDescription("复制心声（Markdown）")[1].performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            assertEquals(body, clipboard.primaryClip!!.getItemAt(0).text.toString())
        }
    }

    @Test fun streamingClosureAndSelectionTransitionKeepExpandedCardStable() {
        val source = mutableStateOf("<心声>尚未闭合")
        val streaming = mutableStateOf(true)
        val expanded = mutableStateMapOf<Int, String>()
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalSettings provides Settings()) {
                    val document = rememberRoleThoughtDocument(source.value, "stream-message")
                    val content = @androidx.compose.runtime.Composable {
                        RoleThoughtMessage(document, expanded, streaming.value, {})
                    }
                    if (streaming.value) content()
                    else MarkdownSelectionContainer(source.value) { content() }
                }
            }
        }
        compose.onNodeWithText("角色心声").assertDoesNotExist()
        awaitText("尚未闭合")
        compose.runOnIdle { source.value = "<心声>尚未闭合</心声>\n\n正文。" }
        awaitText("角色心声")
        compose.onNodeWithText("角色心声").performClick()
        awaitText("尚未闭合")
        compose.runOnIdle { source.value += "继续生成。" }
        awaitText("继续生成。")
        compose.runOnIdle { streaming.value = false }
        awaitText("尚未闭合")
        assertEquals(source.value, parseRoleThoughts(source.value).source)
    }

    @Test fun codeInsideExpandedThoughtRemainsNativeSourceWithNoWebView() {
        show("<心声>\n```html\n<button>Click</button>\n```\n\n```mermaid\ngraph TD; A-->B;\n```\n</心声>")
        compose.onNodeWithText("角色心声").performClick()
        awaitText("<button>Click</button>")
        compose.runOnIdle {
            fun count(view: View): Int = (if (view is WebView) 1 else 0) +
                if (view is ViewGroup) (0 until view.childCount).sumOf { count(view.getChildAt(it)) } else 0
            val activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            assertTrue(activities.isNotEmpty())
            assertEquals(0, activities.sumOf { count(it.window.decorView) })
        }
    }

    private fun show(source: String) {
        val document = parseRoleThoughts(source)
        val expanded = mutableStateMapOf<Int, String>()
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalSettings provides Settings(),
                    LocalNavController provides Navigator(mutableListOf()),
                ) {
                    MarkdownSelectionContainer(source) { RoleThoughtMessage(document, expanded, false, {}) }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun awaitText(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
    }
}
