package app.vinilogs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.vinilogs.core.designsystem.theme.VinilogsTheme
import app.vinilogs.navigation.VinilogsNavHost
import dagger.hilt.android.AndroidEntryPoint

/** The app's single activity (00-README.md: "single-activity, unidirectional data flow"). */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VinilogsTheme {
                VinilogsNavHost()
            }
        }
    }
}
