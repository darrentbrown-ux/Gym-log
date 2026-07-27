package com.gymlog.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gymlog.app.ui.GymLogApp
import com.gymlog.app.ui.GymLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Pass explicit SystemBarStyle so the activity paints behind the system bars
        // with transparent scrims on every supported API level. The default
        // enableEdgeToEdge() picks light/dark scrims based on the system theme, which
        // on some Android 12 / 16 test beds produces a translucent gray band that
        // either looks broken or — when the window is configured with a non-edge-to-
        // edge theme — triggers an IllegalStateException at first draw. Forcing
        // both styles to fully-transparent + auto light/dark icons removes the
        // dependency on theme inheritance.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        setContent {
            GymLogTheme {
                GymLogApp()
            }
        }
    }
}
