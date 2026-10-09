package pt.kaminho.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.kaminho.app.data.firebase.toUserMessage
import pt.kaminho.app.data.model.User
import pt.kaminho.app.data.model.UserRole
import pt.kaminho.app.data.repository.AuthRepository

data class AuthUiState(
    /** true enquanto se verifica se já existe sessão iniciada. */
    val initializing: Boolean = true,
    val loading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
)

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val user = try {
                repo.currentUser()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _state.update { it.copy(initializing = false, user = user) }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            return setError("Preenche o email e a palavra-passe.")
        }
        launchAuth { repo.login(email.trim(), password) }
    }

    fun register(name: String, email: String, phone: String, password: String, role: UserRole) {
        when {
            name.isBlank() -> return setError("Indica o teu nome.")
            email.isBlank() -> return setError("Indica o teu email.")
            password.length < 6 -> return setError("A palavra-passe precisa de pelo menos 6 caracteres.")
        }
        launchAuth { repo.register(name.trim(), email.trim(), phone.trim(), password, role) }
    }

    fun logout() {
        repo.logout()
        _state.update { it.copy(user = null, error = null) }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private fun setError(message: String) = _state.update { it.copy(error = message) }

    private fun launchAuth(block: suspend () -> User) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val user = block()
                _state.update { it.copy(loading = false, user = user) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.toUserMessage()) }
            }
        }
    }
}
