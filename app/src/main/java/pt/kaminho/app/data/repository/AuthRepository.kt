package pt.kaminho.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pt.kaminho.app.data.model.Driver
import pt.kaminho.app.data.model.User
import pt.kaminho.app.data.model.UserRole

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    suspend fun register(
        name: String,
        email: String,
        phone: String,
        password: String,
        role: UserRole,
    ): User {
        // O administrador nunca se cria pela app.
        require(role == UserRole.PASSENGER || role == UserRole.DRIVER)

        val uid = auth.createUserWithEmailAndPassword(email, password).await().user?.uid
            ?: error("Não foi possível criar a conta.")
        val user = User(id = uid, name = name, email = email, phone = phone, role = role)

        try {
            val batch = db.batch()
            batch.set(db.collection("users").document(uid), user)
            if (role == UserRole.DRIVER) {
                batch.set(db.collection("drivers").document(uid), Driver(id = uid, userId = uid))
            }
            batch.commit().await()
        } catch (e: Exception) {
            // Evita contas sem perfil na base de dados.
            auth.currentUser?.delete()
            throw e
        }
        return user
    }

    suspend fun login(email: String, password: String): User {
        val uid = auth.signInWithEmailAndPassword(email, password).await().user?.uid
            ?: error("Não foi possível iniciar sessão.")
        return fetchUser(uid) ?: run {
            auth.signOut()
            error("Perfil não encontrado.")
        }
    }

    /** Devolve o utilizador com sessão iniciada, ou null se não houver. */
    suspend fun currentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return fetchUser(uid)
    }

    fun logout() = auth.signOut()

    private suspend fun fetchUser(uid: String): User? =
        db.collection("users").document(uid).get().await().toObject(User::class.java)
}
