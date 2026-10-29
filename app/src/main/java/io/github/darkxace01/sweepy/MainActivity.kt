package io.github.darkxace01.sweepy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.darkxace01.sweepy.ui.SweepyApp
import io.github.darkxace01.sweepy.ui.theme.SweepyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SweepyTheme {
                SweepyApp()
            }
        }
    }
}
