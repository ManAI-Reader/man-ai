package com.highliuk.manai

import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.highliuk.manai.domain.model.ThemeMode
import com.highliuk.manai.domain.model.isDark
import com.highliuk.manai.domain.repository.UserPreferencesRepository
import com.highliuk.manai.ui.home.HomeViewModel
import com.highliuk.manai.ui.navigation.ManAiNavHost
import com.highliuk.manai.ui.theme.ManAiTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by userPreferencesRepository.themeMode
                .collectAsState(initial = ThemeMode.SYSTEM)

            val darkTheme = themeMode.isDark() ?: isSystemInDarkTheme()

            SideEffect {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }

            ManAiTheme(themeMode = themeMode) {
                val viewModel: HomeViewModel = hiltViewModel()

                val pdfPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    uri?.let {
                        contentResolver.takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        val fileName = getFileName(it) ?: "Unknown"
                        viewModel.importManga(it.toString(), fileName)
                    }
                }

                ManAiNavHost(
                    onImportClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                    navigateToReader = viewModel.navigateToReader
                )
            }
        }
    }

    private fun getFileName(uri: android.net.Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) it.getString(nameIndex) else null
            } else null
        }
    }
}
