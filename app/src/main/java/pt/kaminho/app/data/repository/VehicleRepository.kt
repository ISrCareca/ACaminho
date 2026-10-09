package pt.kaminho.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pt.kaminho.app.data.model.Vehicle

/**
 * Guarda um veículo por motorista em vehicles/{driverId}.
 * Não utiliza Firebase Storage: a fotografia é representada por um URL externo.
 */
class VehicleRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getVehicleForDriver(driverId: String): Vehicle? {
        // O documento principal usa o ID do motorista para manter uma relação 1:1.
        val directSnapshot = db
            .collection("vehicles")
            .document(driverId)
            .get()
            .await()

        if (directSnapshot.exists()) {
            return directSnapshot
                .toObject(Vehicle::class.java)
                ?.copy(id = directSnapshot.id)
        }

        // Compatibilidade com veículos que tenham sido criados anteriormente com outro ID.
        val querySnapshot = db
            .collection("vehicles")
            .whereEqualTo("driverId", driverId)
            .limit(1)
            .get()
            .await()

        val document = querySnapshot.documents.firstOrNull() ?: return null

        return document
            .toObject(Vehicle::class.java)
            ?.copy(id = document.id)
    }

    suspend fun saveVehicle(
        driverId: String,
        brand: String,
        model: String,
        year: Int,
        plate: String,
        photoUrl: String
    ): Vehicle {
        val vehicle = Vehicle(
            id = driverId,
            driverId = driverId,
            brand = brand.trim(),
            model = model.trim(),
            plate = plate.trim().uppercase(),
            year = year,
            photoUrl = photoUrl.trim()
        )

        // Primeiro guarda o veículo. O documento é estável: cada motorista tem um veículo.
        db
            .collection("vehicles")
            .document(driverId)
            .set(vehicle)
            .await()

        // Liga o veículo ao perfil do motorista, para o passageiro o encontrar diretamente.
        db
            .collection("drivers")
            .document(driverId)
            .update("vehicleId", driverId)
            .await()

        return vehicle
    }
}
