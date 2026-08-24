package com.highliuk.manai.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.ChatMessage
import com.highliuk.manai.domain.model.ChatRole
import com.highliuk.manai.domain.model.Conversation

private const val MAX_INPUT_LINES = 5

/**
 * Messages to render in the chat: the first user message of a
 * template-launched conversation is the huge rendered prompt, which stays in
 * the history for the LLM but is hidden from the UI. A conversation is
 * template-launched exactly when it carries a source region; free follow-up
 * user messages always render.
 */
internal fun visibleMessages(
    conversation: Conversation?,
    messages: List<ChatMessage>,
): List<ChatMessage> {
    val first = messages.firstOrNull() ?: return messages
    val launchedFromTemplate = conversation?.regionIndex != null
    return if (launchedFromTemplate && first.role == ChatRole.USER) {
        messages.subList(1, messages.size)
    } else {
        messages
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    conversation: Conversation?,
    messages: List<ChatMessage>,
    streamingText: String?,
    isGenerating: Boolean,
    error: ChatUiError?,
    truncated: Boolean,
    onSend: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenSourcePage: () -> Unit,
    onDeleteConversation: () -> Unit,
    onBack: () -> Unit,
    resolveFurigana: FuriganaResolver? = null,
) {
    val listState = rememberLazyListState()
    val shownMessages = visibleMessages(conversation, messages)
    val isStreaming = streamingText != null || isGenerating
    val itemCount = shownMessages.size + if (isStreaming) 1 else 0

    LaunchedEffect(shownMessages.size, isStreaming) {
        if (itemCount > 0 && !listState.canScrollForward) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    // Keep the latest message visible when the keyboard opens and the
    // message list shrinks to make room for it.
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) {
        if (imeVisible && itemCount > 0) {
            listState.scrollToItem(itemCount - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = conversation?.title.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    ChatMenu(
                        hasSource = conversation?.mangaId != null && conversation.pageIndex != null,
                        onOpenSourcePage = onOpenSourcePage,
                        onDeleteConversation = onDeleteConversation,
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(shownMessages, key = { it.id }) { message ->
                    MessageBubble(message = message, resolveFurigana = resolveFurigana)
                }
                if (isStreaming) {
                    item {
                        StreamingBubble(
                            partialText = streamingText,
                            resolveFurigana = resolveFurigana,
                        )
                    }
                }
                if (truncated && !isStreaming) {
                    item {
                        Text(
                            text = stringResource(R.string.chat_truncated),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("chat_truncated_notice"),
                        )
                    }
                }
            }

            if (error != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = error.httpStatus
                            ?.let { status -> stringResource(error.messageRes, status) }
                            ?: stringResource(error.messageRes),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }

            ChatInputBar(
                isGenerating = isGenerating,
                onSend = onSend,
            )
        }
    }
}

@Composable
private fun ChatMenu(
    hasSource: Boolean,
    onOpenSourcePage: () -> Unit,
    onDeleteConversation: () -> Unit,
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    IconButton(
        onClick = { menuExpanded = true },
        modifier = Modifier.testTag("chat_menu"),
    ) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.more_options),
        )
    }
    DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
    ) {
        if (hasSource) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.jump_to_source)) },
                onClick = {
                    menuExpanded = false
                    onOpenSourcePage()
                },
                modifier = Modifier.testTag("menu_open_source"),
            )
        }
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            onClick = {
                menuExpanded = false
                confirmingDelete = true
            },
            modifier = Modifier.testTag("menu_delete"),
        )
    }
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            text = { Text(stringResource(R.string.delete_conversation_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        onDeleteConversation()
                    },
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, resolveFurigana: FuriganaResolver?) {
    when (message.role) {
        ChatRole.USER -> Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        ChatRole.ASSISTANT -> SelectionContainer {
            MarkdownMessageContent(
                text = message.content,
                isComplete = true,
                resolveFurigana = resolveFurigana,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StreamingBubble(partialText: String?, resolveFurigana: FuriganaResolver?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_streaming"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (partialText.isNullOrEmpty()) {
            val waitingDescription = stringResource(R.string.assistant_thinking)
            // Live region is scoped to the waiting phase only: announcing the
            // growing streamed text on every delta would spam TalkBack.
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .testTag("chat_ttfb_spinner")
                    .semantics {
                        contentDescription = waitingDescription
                        liveRegion = LiveRegionMode.Polite
                    },
            )
        } else {
            MarkdownMessageContent(
                text = partialText,
                isComplete = false,
                resolveFurigana = resolveFurigana,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    isGenerating: Boolean,
    onSend: (String) -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.weight(1f),
        ) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                maxLines = MAX_INPUT_LINES,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat_input"),
                decorationBox = { innerTextField ->
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        if (input.isEmpty()) {
                            Text(
                                text = stringResource(R.string.chat_input_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
        IconButton(
            onClick = {
                if (!isGenerating) {
                    onSend(input)
                    input = ""
                }
            },
            enabled = !isGenerating && input.isNotBlank(),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.send_message),
            )
        }
    }
}
