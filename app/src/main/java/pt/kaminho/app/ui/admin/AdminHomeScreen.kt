package pt.kaminho.app.ui.admin

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import pt.kaminho.app.ui.components.HomeScaffold

@Composable
fun AdminHomeScreen(onLogout: () -> Unit) {
    HomeScaffold(title = "Administração", onLogout = onLogout) {
        // Fase 5: dashboard, utilizadores, motoristas, veículos e viagens.
        Text("O painel de administração chega na Fase 5.")
    }
}
