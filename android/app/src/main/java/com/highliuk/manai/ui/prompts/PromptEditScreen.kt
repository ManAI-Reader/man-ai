package com.highliuk.manai.ui.prompts

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.PromptTemplate
import com.highliuk.manai.domain.model.ReasoningLevel

private const val TEMPLATE_MIN_LINES = 8

@StringRes
internal fun reasoningLevelLabelRes(level: ReasoningLevel): Int = when (level) {
    ReasoningLevel.DEFAULT -> R.string.reasoning_default
    ReasoningLevel.OFF -> R.string.reasoning_off
    ReasoningLevel.LOW -> R.string.reasoning_low
    ReasoningLevel.MEDIUM -> R.string.reasoning_medium
    ReasoningLevel.HIGH -> R.string.reasoning_high
}

/**
 * Full-screen prompt editor: replaces the cramped edit dialog with room for
 * the template text and a vertical reasoning-level selector.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptEditScreen(
    template: PromptTemplate?,
    @StringRes errorRes: Int?,
    onSave: (name: String, template: String, reasoningLevel: ReasoningLevel) -> Unit,
    onBack: () -> Unit,
) {
    var name by remember(template) { mutableStateOf(template?.name.orEmpty()) }
    var text by remember(template) { mutableStateOf(template?.template.orEmpty()) }
    var reasoning by remember(template) {
        mutableStateOf(template?.reasoningLevel ?: ReasoningLevel.DEFAULT)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (template == null || template.id == 0L) {
                                R.string.add_prompt
                            } else {
                                R.string.edit_prompt
                            }
                        )
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
                    TextButton(
                        onClick = { onSave(name, text, reasoning) },
                        modifier = Modifier.testTag("save_prompt"),
                    ) {
                        Text(stringResource(R.string.save))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.prompt_name)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prompt_name_field"),
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.prompt_template)) },
                minLines = TEMPLATE_MIN_LINES,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .testTag("prompt_template_field"),
            )
            Text(
                text = stringResource(R.string.prompt_placeholders_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.reasoning_level),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp),
            )
            ReasoningLevelSelector(
                selected = reasoning,
                onSelect = { reasoning = it },
            )
            if (errorRes != null) {
                Text(
                    text = stringResource(errorRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .testTag("prompt_edit_error"),
                )
            }
        }
    }
}

@Composable
private fun ReasoningLevelSelector(
    selected: ReasoningLevel,
    onSelect: (ReasoningLevel) -> Unit,
) {
    Column(modifier = Modifier.selectableGroup()) {
        ReasoningLevel.entries.forEach { level ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = level == selected,
                        role = Role.RadioButton,
                        onClick = { onSelect(level) },
                    )
                    .padding(vertical = 8.dp)
                    .testTag("reasoning_radio_${level.name}"),
            ) {
                RadioButton(
                    selected = level == selected,
                    onClick = null,
                )
                Text(
                    text = stringResource(reasoningLevelLabelRes(level)),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}
