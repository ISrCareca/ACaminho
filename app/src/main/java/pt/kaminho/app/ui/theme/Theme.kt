package pt.kaminho.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cores inspiradas no mockup: azul-escuro nos botões e cabeçalhos, verde para a margem.
val Navy = Color(0xFF0F2A44)
val Green = Color(0xFF1E9E63)
val GreenContainer = Color(0xFFE6F6EE)

private val ACaminhoColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6F2),
    onPrimaryContainer = Navy,
    secondary = Green,
    onSecondary = Color.White,
    secondaryContainer = GreenContainer,
    onSecondaryContainer = Color(0xFF0B5A38),
    background = Color(0xFFF4F7FA),
    onBackground = Color(0xFF121C27),
    surface = Color.White,
    onSurface = Color(0xFF121C27),
    error = Color(0xFFC62828),
)

@Composable
fun ACaminhoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ACaminhoColors, content = content)
}
