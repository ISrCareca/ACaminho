package pt.kaminho.app.data.firebase

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/** Converte erros do Firebase em mensagens para o utilizador. */
fun Throwable.toUserMessage(): String = when (this) {
    // A ordem importa: a exceção de palavra-passe fraca é subtipo da de credenciais.
    is FirebaseAuthWeakPasswordException -> "A palavra-passe é demasiado fraca (mínimo 6 caracteres)."
    is FirebaseAuthUserCollisionException -> "Já existe uma conta com este email."
    is FirebaseAuthInvalidUserException,
    is FirebaseAuthInvalidCredentialsException -> "Email ou palavra-passe incorretos."
    is FirebaseNetworkException -> "Sem ligação à internet."
    else -> message ?: "Ocorreu um erro. Tenta novamente."
}
