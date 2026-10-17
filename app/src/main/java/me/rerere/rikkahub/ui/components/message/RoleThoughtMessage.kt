package me.rerere.rikkahub.ui.components.message

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Copy01
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.components.richtext.MarkdownBlock
import me.rerere.rikkahub.ui.components.ui.ChainOfThought
import me.rerere.ui.markdown.RoleThoughtDocument
import me.rerere.ui.markdown.RoleThoughtSegment
import me.rerere.ui.markdown.parseRoleThoughts

/** Like the existing Markdown parser, conflate streaming updates and parse them off the UI thread. */
@Composable
internal fun rememberRoleThoughtDocument(source: String, messageId: String): RoleThoughtDocument {
    val state = remember(messageId) { mutableStateOf(parseRoleThoughts(source)) }
    val currentSource by rememberUpdatedState(source)
    LaunchedEffect(state) {
        snapshotFlow { currentSource }.conflate()
            .filter { it != state.value.source }
            .map(::parseRoleThoughts).flowOn(Dispatchers.Default)
            .collect { state.value = it }
    }
    return state.value
}

@Composable
internal fun RoleThoughtMessage(
    document: RoleThoughtDocument,
    expandedThoughts: MutableMap<Int, String>,
    isStreaming: Boolean,
    onClickCitation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (document.segments.none { it is RoleThoughtSegment.Thought }) {
        MarkdownBlock(document.source, modifier = modifier, isStreaming = isStreaming, onClickCitation = onClickCitation)
        return
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        document.segments.forEach { segment ->
            when (segment) {
                is RoleThoughtSegment.Text -> if (segment.source.isNotBlank()) {
                    MarkdownBlock(segment.source, isStreaming = isStreaming, onClickCitation = onClickCitation)
                }
                is RoleThoughtSegment.Thought -> RoleThoughtCard(
                    thought = segment,
                    expanded = expandedThoughts[segment.startOffset] == segment.source,
                    onExpandedChange = { expanded ->
                        if (expanded) expandedThoughts[segment.startOffset] = segment.source
                        else expandedThoughts.remove(segment.startOffset)
                    },
                    isStreaming = isStreaming,
                    onClickCitation = onClickCitation,
                )
            }
        }
    }
}

/** Independent character-content card; the generic timeline supplies only its native UI. */
@Composable
internal fun RoleThoughtCard(
    thought: RoleThoughtSegment.Thought,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    isStreaming: Boolean,
    onClickCitation: (String) -> Unit,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    ChainOfThought(steps = listOf(thought), modifier = Modifier.fillMaxWidth()) {
        ControlledChainOfThoughtStep(
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            icon = { DisableSelection { Text("◈") } },
            label = { DisableSelection { Text(stringResource(R.string.role_thought_title)) } },
            extra = {
                DisableSelection {
                    IconButton(onClick = {
                        scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Markdown", thought.content))) }
                    }) {
                        Icon(HugeIcons.Copy01, contentDescription = stringResource(R.string.role_thought_copy))
                    }
                }
            },
        ) {
            MarkdownBlock(
                content = thought.content,
                isStreaming = isStreaming,
                allowCodePreview = false,
                onClickCitation = onClickCitation,
            )
        }
    }
}
