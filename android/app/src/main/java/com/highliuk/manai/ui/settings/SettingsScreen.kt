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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.intl.LocaleList as ComposeLocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.highliuk.manai.BuildConfig
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.AppLanguage
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.model.TargetLanguage
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
    comicTextScale: Float = 1.5f,
    onComicTextScaleChange: (Float) -> Unit = {},
    tapToNavigatePortrait: Boolean,
    onTapToNavigatePortraitChange: (Boolean) -> Unit,
    tapToNavigateLandscape: Boolean,
    onTapToNavigateLandscapeChange: (Boolean) -> Unit,
    showFurigana: Boolean = false,
    onShowFuriganaChange: (Boolean) -> Unit = {},
    deeplApiKey: String = "",
    onDeeplApiKeyChange: (String) -> Unit = {},
    translationTargetLang: TargetLanguage = TargetLanguage.EN,
    onTranslationTargetLangChange: (TargetLanguage) -> Unit = {},
    llmApiKey: String = "",
    onLlmApiKeyChange: (String) -> Unit = {},
    llmBaseUrl: String = "",
    onLlmBaseUrlChange: (String) -> Unit = {},
    llmModel: String = "",
    onLlmModelChange: (String) -> Unit = {},
    onManagePromptsClick: () -> Unit = {},
    versionName: String = BuildConfig.VERSION_NAME,
    versionCode: Int = BuildConfig.VERSION_CODE,
    onBack: () -> Unit
) {
    var apiKeyVisible by remember { mutableStateOf(false) }
    var llmApiKeyVisible by remember { mutableStateOf(false) }
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
                    .clickable { onTapToNavigatePortraitChange(!tapToNavigatePortrait) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.tap_to_navigate_portrait),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.tap_to_navigate_portrait_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = tapToNavigatePortrait,
                    onCheckedChange = onTapToNavigatePortraitChange
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTapToNavigateLandscapeChange(!tapToNavigateLandscape) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.tap_to_navigate_landscape),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.tap_to_navigate_landscape_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = tapToNavigateLandscape,
                    onCheckedChange = onTapToNavigateLandscapeChange
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowFuriganaChange(!showFurigana) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.show_furigana),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.show_furigana_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = showFurigana,
                    onCheckedChange = onShowFuriganaChange
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
                        .padding(vertical = 8.dp)
                        .testTag("app_lang_${language.name}"),
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

            TranslationSection(
                deeplApiKey = deeplApiKey,
                onDeeplApiKeyChange = onDeeplApiKeyChange,
                translationTargetLang = translationTargetLang,
                onTranslationTargetLangChange = onTranslationTargetLangChange,
                apiKeyVisible = apiKeyVisible,
                onApiKeyVisibleChange = { apiKeyVisible = it },
            )

            AiSection(
                llmApiKey = llmApiKey,
                onLlmApiKeyChange = onLlmApiKeyChange,
                llmBaseUrl = llmBaseUrl,
                onLlmBaseUrlChange = onLlmBaseUrlChange,
                llmModel = llmModel,
                onLlmModelChange = onLlmModelChange,
                onManagePromptsClick = onManagePromptsClick,
                apiKeyVisible = llmApiKeyVisible,
                onApiKeyVisibleChange = { llmApiKeyVisible = it },
            )

            Text(
                text = stringResource(R.string.app_version_info, versionName, versionCode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun TranslationSection(
    deeplApiKey: String,
    onDeeplApiKeyChange: (String) -> Unit,
    translationTargetLang: TargetLanguage,
    onTranslationTargetLangChange: (TargetLanguage) -> Unit,
    apiKeyVisible: Boolean,
    onApiKeyVisibleChange: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.translation_section),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )

    OutlinedTextField(
        value = deeplApiKey,
        onValueChange = onDeeplApiKeyChange,
        label = { Text(stringResource(R.string.deepl_api_key)) },
        placeholder = { Text(stringResource(R.string.deepl_api_key_hint)) },
        visualTransformation = if (apiKeyVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { onApiKeyVisibleChange(!apiKeyVisible) }) {
                Icon(
                    imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                    contentDescription = null,
                )
            }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("deepl_api_key_field"),
    )

    Text(
        text = stringResource(R.string.translation_target_language),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
    TargetLanguage.entries.forEach { lang ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTranslationTargetLangChange(lang) }
                .padding(vertical = 8.dp)
                .testTag("target_lang_${lang.code}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = translationTargetLang == lang,
                onClick = { onTranslationTargetLangChange(lang) }
            )
            Text(
                text = lang.displayName,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun AiSection(
    llmApiKey: String,
    onLlmApiKeyChange: (String) -> Unit,
    llmBaseUrl: String,
    onLlmBaseUrlChange: (String) -> Unit,
    llmModel: String,
    onLlmModelChange: (String) -> Unit,
    onManagePromptsClick: () -> Unit,
    apiKeyVisible: Boolean,
    onApiKeyVisibleChange: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.ai_section),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )

    OutlinedTextField(
        value = llmApiKey,
        onValueChange = onLlmApiKeyChange,
        label = { Text(stringResource(R.string.llm_api_key)) },
        placeholder = { Text(stringResource(R.string.llm_api_key_hint)) },
        visualTransformation = if (apiKeyVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { onApiKeyVisibleChange(!apiKeyVisible) }) {
                Icon(
                    imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                    contentDescription = null,
                )
            }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("llm_api_key_field"),
    )

    OutlinedTextField(
        value = llmBaseUrl,
        onValueChange = onLlmBaseUrlChange,
        label = { Text(stringResource(R.string.llm_base_url)) },
        placeholder = { Text(stringResource(R.string.llm_base_url_placeholder)) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("llm_base_url_field"),
    )

    OutlinedTextField(
        value = llmModel,
        onValueChange = onLlmModelChange,
        label = { Text(stringResource(R.string.llm_model)) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("llm_model_field"),
    )

    TextButton(
        onClick = onManagePromptsClick,
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Text(stringResource(R.string.manage_prompts))
    }
}
