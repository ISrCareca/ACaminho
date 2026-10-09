package pt.kaminho.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pt.kaminho.app.navigation.AppNavigation
import pt.kaminho.app.ui.theme.ACaminhoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        enableEdgeToEdge()

        setContent {

            ACaminhoTheme {

                AppNavigation()
            }
        }
    }
}