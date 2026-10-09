package pt.kaminho.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pt.kaminho.app.data.model.DriverStatus

class DriverRepository(
    private val db: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    suspend fun getStatus(
        uid: String
    ): DriverStatus {

        val snapshot =
            db
                .collection("drivers")
                .document(uid)
                .get()
                .await()

        val status =
            snapshot.getString("status")
                ?: DriverStatus.OFFLINE.name

        return when (status) {

            DriverStatus.ONLINE.name ->
                DriverStatus.ONLINE

            DriverStatus.ON_TRIP.name ->
                DriverStatus.ON_TRIP

            DriverStatus.OFFLINE.name ->
                DriverStatus.OFFLINE

            else ->
                DriverStatus.OFFLINE
        }
    }

    suspend fun setStatus(
        uid: String,
        driverStatus: DriverStatus
    ) {

        db
            .collection("drivers")
            .document(uid)
            .update(
                "status",
                driverStatus.name
            )
            .await()
    }
}