package com.example.yesmaam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.yesmaam.ui.nav.YesMaamNavGraph
import com.example.yesmaam.ui.theme.YesMaamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YesMaamTheme {
                Surface(Modifier.fillMaxSize()) { YesMaamNavGraph() }
            }
        }
    }
}
