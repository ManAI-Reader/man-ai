package com.highliuk.manai.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.intl.LocaleList as ComposeLocaleList
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.AppLanguage
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    gridColumns: Int,
    onGridColumnsChange: (Int) -> Unit,
    readingMode: ReadingMode,
    onReadingModeChange: (ReadingMode) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    appLanguage: AppLanguage,
    onAppLanguageChange: (AppLanguage) -> Unit,
    comicTextScale: Float = 1.5f,
    onComicTextScaleChange: (Float) -> Unit = {},
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.grid_columns),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            listOf(2, 3).forEach { columns ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGridColumnsChange(columns) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = gridColumns == columns,
                        onClick = { onGridColumnsChange(columns) }
                    )
                    Text(
                        text = stringResource(R.string.n_columns, columns),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.reading_mode),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            ReadingMode.entries.forEach { mode ->
                val label = when (mode) {
                    ReadingMode.LTR -> stringResource(R.string.reading_mode_ltr)
                    ReadingMode.RTL -> stringResource(R.string.reading_mode_rtl)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onReadingModeChange(mode) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = readingMode == mode,
                        onClick = { onReadingModeChange(mode) }
                    )
                    Text(
                        text = label,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.theme_mode),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            ThemeMode.entries.forEach { mode ->
                val themeLabel = when (mode) {
                    ThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
                    ThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
                    ThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onThemeModeChange(mode) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = themeMode == mode,
                        onClick = { onThemeModeChange(mode) }
                    )
                    Text(
                        text = themeLabel,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            AppLanguage.entries.forEach { language ->
                val languageLabel = when (language) {
                    AppLanguage.SYSTEM -> stringResource(R.string.language_system)
                    AppLanguage.ENGLISH -> stringResource(R.string.language_english)
                    AppLanguage.ITALIAN -> stringResource(R.string.language_italian)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAppLanguageChange(language) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = appLanguage == language,
                        onClick = { onAppLanguageChange(language) }
                    )
                    Text(
                        text = languageLabel,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.comic_text_size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            Slider(
                value = comicTextScale,
                onValueChange = onComicTextScaleChange,
                valueRange = 1f..3f,
                steps = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("comic_text_scale_slider"),
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(localeList = ComposeLocaleList("ja"))) {
                        append(stringResource(R.string.comic_text_preview))
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize * comicTextScale,
                lineHeight = 1.5.em,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}
