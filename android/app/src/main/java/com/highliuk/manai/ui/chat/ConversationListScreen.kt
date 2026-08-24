package com.highliuk.manai.ui.chat

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.Conversation

@Composable
fun ConversationListScreen(
    conversations: List<Conversation>,
    pendingDelete: Conversation?,
    isSearchActive: Boolean,
    searchQuery: String,
    onConversationClick: (Long) -> Unit,
    onDeleteClick: (Conversation) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(enabled = isSearchActive, onBack = onCloseSearch)
    Scaffold(
        topBar = {
            ConversationListTopBar(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchQueryChange = onSearchQueryChange,
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                val message = if (isSearchActive && searchQuery.isNotBlank()) {
                    R.string.no_search_results
                } else {
                    R.string.no_conversations
                }
                Text(stringResource(message))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("conversation_list"),
            ) {
                items(conversations, key = { it.id }) { conversation ->
                    ConversationListItem(
                        conversation = conversation,
                        onClick = { onConversationClick(conversation.id) },
                        onDeleteClick = { onDeleteClick(conversation) },
                    )
                }
            }
        }
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            text = { Text(stringResource(R.string.delete_conversation_confirm)) },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationListTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = {
            if (isSearchActive) {
                SearchField(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                )
            } else {
                Text(stringResource(R.string.conversations))
            }
        },
        navigationIcon = {
            IconButton(onClick = if (isSearchActive) onCloseSearch else onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        actions = {
            if (!isSearchActive) {
                IconButton(
                    onClick = onOpenSearch,
                    modifier = Modifier.testTag("conversation_search_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_conversations),
                    )
                }
            }
        },
    )
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .testTag("conversation_search_field"),
        placeholder = { Text(stringResource(R.string.search_conversations)) },
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cancel),
                )
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
    )
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun ConversationListItem(
    conversation: Conversation,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = conversation.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(DateUtils.getRelativeTimeSpanString(conversation.updatedAt).toString())
        },
        trailingContent = {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                )
            }
        },
    )
}
