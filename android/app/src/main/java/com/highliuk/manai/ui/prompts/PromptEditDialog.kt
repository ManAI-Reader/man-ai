package com.highliuk.manai.ui.prompts

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.PromptTemplate
import com.highliuk.manai.domain.model.ReasoningLevel

@StringRes
internal fun reasoningLevelLabelRes(level: ReasoningLevel): Int = when (level) {
    ReasoningLevel.DEFAULT -> R.string.reasoning_default
    ReasoningLevel.OFF -> R.string.reasoning_off
    ReasoningLevel.LOW -> R.string.reasoning_low
    ReasoningLevel.MEDIUM -> R.string.reasoning_medium
    ReasoningLevel.HIGH -> R.string.reasoning_high
}

@Composable
fun PromptEditDialog(
    template: PromptTemplate,
    @StringRes errorRes: Int?,
    onConfirm: (name: String, template: String, reasoningLevel: ReasoningLevel) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(template) { mutableStateOf(template.name) }
    var text by remember(template) { mutableStateOf(template.template) }
    var reasoning by remember(template) { mutableStateOf(template.reasoningLevel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (template.id == 0L) R.string.add_prompt else R.string.edit_prompt
                )
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                    minLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
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
                    modifier = Modifier.padding(top = 12.dp),
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
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, text, reasoning) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun ReasoningLevelSelector(
    selected: ReasoningLevel,
    onSelect: (ReasoningLevel) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("reasoning_selector"),
    ) {
        ReasoningLevel.entries.forEach { level ->
            FilterChip(
                selected = level == selected,
                onClick = { onSelect(level) },
                label = { Text(stringResource(reasoningLevelLabelRes(level))) },
                modifier = Modifier.testTag("reasoning_chip_${level.name}"),
            )
        }
    }
}
