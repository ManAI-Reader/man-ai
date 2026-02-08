package com.highliuk.manai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.highliuk.manai.ui.navigation.ManAiNavHost
import com.highliuk.manai.ui.theme.ManAiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ManAiTheme {
                ManAiNavHost()
            }
        }
    }
}
