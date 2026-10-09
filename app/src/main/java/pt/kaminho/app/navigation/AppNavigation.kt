package pt.kaminho.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pt.kaminho.app.data.model.UserRole
import pt.kaminho.app.ui.admin.AdminHomeScreen
import pt.kaminho.app.ui.auth.AuthScreen
import pt.kaminho.app.ui.driver.DriverHomeScreen
import pt.kaminho.app.ui.passenger.PassengerHomeScreen
import pt.kaminho.app.viewmodel.AuthViewModel

object Routes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val PASSENGER = "passenger"
    const val DRIVER = "driver"
    const val ADMIN = "admin"
}

@Composable
fun AppNavigation(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val state by authViewModel.state.collectAsStateWithLifecycle()
    val user = state.user

    // Sempre que a sessão ou o perfil muda, encaminha para o ecrã certo
    // e limpa o histórico, para o botão "voltar" não regressar ao login.
    LaunchedEffect(state.initializing, user?.id, user?.role) {
        if (state.initializing) return@LaunchedEffect
        val target = when (user?.role) {
            null -> Routes.AUTH
            UserRole.PASSENGER -> Routes.PASSENGER
            UserRole.DRIVER -> Routes.DRIVER
            UserRole.ADMIN -> Routes.ADMIN
        }
        navController.navigate(target) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        composable(Routes.AUTH) {
            AuthScreen(
                state = state,
                onLogin = authViewModel::login,
                onRegister = authViewModel::register,
                onDismissError = authViewModel::clearError,
            )
        }
        composable(Routes.PASSENGER) {
            user?.let { PassengerHomeScreen(user = it, onLogout = authViewModel::logout) }
        }
        composable(Routes.DRIVER) {
            user?.let { DriverHomeScreen(user = it, onLogout = authViewModel::logout) }
        }
        composable(Routes.ADMIN) {
            AdminHomeScreen(onLogout = authViewModel::logout)
        }
    }
}
