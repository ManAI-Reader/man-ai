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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.AppLanguage
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.model.ThemeMode

private val languageLabelRes: Map<AppLanguage, Int> = mapOf(
    AppLanguage.SYSTEM to R.string.language_system,
    AppLanguage.ENGLISH to R.string.language_english,
    AppLanguage.ITALIAN to R.string.language_italian,
    AppLanguage.JAPANESE to R.string.language_japanese,
    AppLanguage.SPANISH to R.string.language_spanish,
    AppLanguage.PORTUGUESE_BR to R.string.language_portuguese_br,
    AppLanguage.FRENCH to R.string.language_french,
    AppLanguage.CHINESE_SIMPLIFIED to R.string.language_chinese_simplified,
    AppLanguage.KOREAN to R.string.language_korean,
    AppLanguage.GERMAN to R.string.language_german,
    AppLanguage.RUSSIAN to R.string.language_russian,
    AppLanguage.INDONESIAN to R.string.language_indonesian,
    AppLanguage.THAI to R.string.language_thai,
    AppLanguage.POLISH to R.string.language_polish,
)

@StringRes
internal fun AppLanguage.labelRes(): Int =
    languageLabelRes[this] ?: error("Missing label resource for $this")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    gridColumns: Int,
    onGridColumnsChange: (Int) -> Unit,
    gridColumnsLandscape: Int,
    onGridColumnsLandscapeChange: (Int) -> Unit,
    readingMode: ReadingMode,
    onReadingModeChange: (ReadingMode) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    appLanguage: AppLanguage,
    onAppLanguageChange: (AppLanguage) -> Unit,
    tapToNavigate: Boolean,
    onTapToNavigateChange: (Boolean) -> Unit,
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
                        text = pluralStringResource(R.plurals.n_columns, columns, columns),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.grid_columns_landscape),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            listOf(4, 5, 6).forEach { columns ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGridColumnsLandscapeChange(columns) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = gridColumnsLandscape == columns,
                        onClick = { onGridColumnsLandscapeChange(columns) }
                    )
                    Text(
                        text = pluralStringResource(R.plurals.n_columns, columns, columns),
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
                    ReadingMode.WEBTOON -> stringResource(R.string.reading_mode_webtoon)
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTapToNavigateChange(!tapToNavigate) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.tap_to_navigate),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.tap_to_navigate_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = tapToNavigate,
                    onCheckedChange = onTapToNavigateChange
                )
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
                        text = stringResource(language.labelRes()),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
