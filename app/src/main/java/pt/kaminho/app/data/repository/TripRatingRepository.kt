package pt.kaminho.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pt.kaminho.app.data.model.UserRole

/** Resumo agregado das avaliações recebidas por um utilizador. */
data class RatingSummary(
    val average: Double? = null,
    val count: Int = 0
)

/**
 * Avaliações mútuas associadas a uma viagem concluída.
 * O ID determinístico (viagem + avaliador) impede avaliações duplicadas.
 */
class TripRatingRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun ratingDocumentId(tripId: String, raterId: String): String =
        "${tripId}_${raterId}"

    suspend fun getExistingRating(
        tripId: String,
        raterId: String
    ): Int? {
        val snapshot = db.collection("ratings")
            .document(ratingDocumentId(tripId, raterId))
            .get()
            .await()

        return snapshot.getLong("score")?.toInt()
    }

    suspend fun getTargetIdForTrip(
        tripId: String,
        raterId: String,
        raterRole: UserRole
    ): String {
        val trip = db.collection("trips")
            .document(tripId)
            .get()
            .await()

        check(trip.exists()) { "A viagem já não existe." }
        check(trip.getString("status") == "COMPLETED") {
            "Só é possível avaliar uma viagem concluída."
        }

        val driverId = trip.getString("driverId").orEmpty()
        val passengerId = trip.getString("passengerId").orEmpty()

        return when (raterRole) {
            UserRole.DRIVER -> {
                check(driverId == raterId) {
                    "Só o motorista desta viagem pode avaliar o passageiro."
                }
                passengerId
            }
            UserRole.PASSENGER -> {
                check(passengerId == raterId) {
                    "Só o passageiro desta viagem pode avaliar o motorista."
                }
                driverId
            }
            UserRole.ADMIN -> error("A administração não pode avaliar uma viagem.")
        }.also { targetId ->
            check(targetId.isNotBlank()) { "Não foi possível identificar quem será avaliado." }
        }
    }

    /**
     * Regista uma avaliação de 1 a 5. A transação verifica novamente que a viagem
     * está concluída, que o utilizador participou e que ainda não avaliou.
     * Devolve o ID do utilizador avaliado.
     */
    suspend fun submitRating(
        tripId: String,
        raterId: String,
        raterRole: UserRole,
        score: Int
    ): String {
        require(score in 1..5) { "A avaliação tem de estar entre 1 e 5 estrelas." }
        require(raterRole == UserRole.DRIVER || raterRole == UserRole.PASSENGER) {
            "Este tipo de utilizador não pode avaliar viagens."
        }

        val tripRef = db.collection("trips").document(tripId)
        val ratingRef = db.collection("ratings")
            .document(ratingDocumentId(tripId, raterId))

        return db.runTransaction { transaction ->
            val tripSnapshot = transaction.get(tripRef)
            val ratingSnapshot = transaction.get(ratingRef)

            check(tripSnapshot.exists()) { "A viagem já não existe." }
            check(tripSnapshot.getString("status") == "COMPLETED") {
                "Só é possível avaliar uma viagem concluída."
            }
            check(!ratingSnapshot.exists()) { "Já enviaste uma avaliação para esta viagem." }

            val driverId = tripSnapshot.getString("driverId").orEmpty()
            val passengerId = tripSnapshot.getString("passengerId").orEmpty()

            val targetId = when (raterRole) {
                UserRole.DRIVER -> {
                    check(driverId == raterId) {
                        "Só o motorista desta viagem pode avaliar o passageiro."
                    }
                    passengerId
                }
                UserRole.PASSENGER -> {
                    check(passengerId == raterId) {
                        "Só o passageiro desta viagem pode avaliar o motorista."
                    }
                    driverId
                }
                UserRole.ADMIN -> error("A administração não pode avaliar uma viagem.")
            }

            check(targetId.isNotBlank()) { "Não foi possível identificar quem será avaliado." }

            transaction.set(
                ratingRef,
                mapOf(
                    "tripId" to tripId,
                    "raterId" to raterId,
                    "raterRole" to raterRole.name,
                    "targetId" to targetId,
                    "targetRole" to if (raterRole == UserRole.DRIVER) {
                        UserRole.PASSENGER.name
                    } else {
                        UserRole.DRIVER.name
                    },
                    "score" to score,
                    "createdAt" to System.currentTimeMillis()
                )
            )

            targetId
        }.await()
    }

    suspend fun getSummary(targetId: String): RatingSummary {
        if (targetId.isBlank()) return RatingSummary()

        val snapshot = db.collection("ratings")
            .whereEqualTo("targetId", targetId)
            .get()
            .await()

        val scores = snapshot.documents.mapNotNull { document ->
            document.getLong("score")?.toInt()?.takeIf { it in 1..5 }
        }

        if (scores.isEmpty()) return RatingSummary()

        return RatingSummary(
            average = scores.average(),
            count = scores.size
        )
    }
}
