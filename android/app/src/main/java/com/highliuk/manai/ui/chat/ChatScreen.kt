package com.highliuk.manai.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.ChatMessage
import com.highliuk.manai.domain.model.ChatRole
import com.highliuk.manai.domain.model.Conversation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversation: Conversation?,
    messages: List<ChatMessage>,
    streamingText: String?,
    isGenerating: Boolean,
    error: String?,
    onSend: (String) -> Unit,
    onRetry: () -> Unit,
    onJumpToSource: () -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    val isStreaming = streamingText != null
    val itemCount = messages.size + if (isStreaming) 1 else 0

    LaunchedEffect(messages.size, isStreaming) {
        if (itemCount > 0 && !listState.canScrollForward) {
            listState.animateScrollToItem(itemCount - 1)
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
                    if (conversation?.mangaId != null && conversation.pageIndex != null) {
                        IconButton(
                            onClick = onJumpToSource,
                            modifier = Modifier.testTag("jump_to_source"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = stringResource(R.string.jump_to_source),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
                if (streamingText != null) {
                    item {
                        StreamingBubble(partialText = streamingText)
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
                        text = error.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.chat_error_generic),
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
private fun MessageBubble(message: ChatMessage) {
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
            Text(
                text = message.content,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StreamingBubble(partialText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_streaming"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (partialText.isEmpty()) {
            CircularProgressIndicator(Modifier.size(16.dp))
        } else {
            Text(text = partialText, modifier = Modifier.fillMaxWidth())
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
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            placeholder = { Text(stringResource(R.string.chat_input_hint)) },
            modifier = Modifier
                .weight(1f)
                .testTag("chat_input"),
        )
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
