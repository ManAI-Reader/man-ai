package com.highliuk.manai

import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.highliuk.manai.ui.home.HomeScreen
import com.highliuk.manai.ui.home.HomeViewModel
import com.highliuk.manai.ui.theme.ManAiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ManAiTheme {
                val viewModel: HomeViewModel = hiltViewModel()
                val mangaList by viewModel.mangaList.collectAsState()

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

                HomeScreen(
                    mangaList = mangaList,
                    onImportClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                    onSettingsClick = { /* TODO: navigate to settings */ }
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
