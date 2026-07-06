package com.dinh.gocnho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dinh.gocnho.screens.TetrisScreen
import com.dinh.gocnho.ui.theme.GócNhỏTheme

class TetrisActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GócNhỏTheme {
                TetrisScreen(onBack = { finish() })
            }
        }
    }
}
