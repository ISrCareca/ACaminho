package pt.kaminho.app.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import pt.kaminho.app.data.model.Driver
import pt.kaminho.app.data.model.Place
import pt.kaminho.app.data.model.Trip
import pt.kaminho.app.data.model.TripStatus
import pt.kaminho.app.data.model.User
import pt.kaminho.app.data.model.Vehicle

class TripRepository(
    private val db: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    suspend fun createTrip(
        trip: Trip
    ): String {

        val document =
            db
                .collection("trips")
                .document()

        val tripWithId =
            trip.copy(
                id = document.id
            )

        document
            .set(tripWithId)
            .await()

        return document.id
    }

    suspend fun getRequestedTrips():
        List<Trip> {

        val snapshot =
            db
                .collection("trips")
                .whereEqualTo(
                    "status",
                    TripStatus.REQUESTED.name
                )
                .get()
                .await()

        return snapshot.documents.map {
            documentToTrip(it)
        }
    }

    fun observeRequestedTrips(
        onTripsChanged:
            (List<Trip>) -> Unit,

        onError:
            (Exception) -> Unit
    ): ListenerRegistration {

        return db
            .collection("trips")
            .whereEqualTo(
                "status",
                TripStatus.REQUESTED.name
            )
            .addSnapshotListener {
                    snapshot,
                    error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    return@addSnapshotListener
                }

                try {

                    val trips =
                        snapshot.documents.map {
                            documentToTrip(it)
                        }

                    onTripsChanged(
                        trips
                    )

                } catch (e: Exception) {

                    onError(e)
                }
            }
    }

    fun observeTrip(
        tripId: String,
        onTripChanged:
            (Trip) -> Unit,
        onError:
            (Exception) -> Unit
    ): ListenerRegistration {

        return db
            .collection("trips")
            .document(tripId)
            .addSnapshotListener {
                    snapshot,
                    error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                if (
                    snapshot == null ||
                    !snapshot.exists()
                ) {
                    return@addSnapshotListener
                }

                try {

                    val trip =
                        documentToTrip(
                            snapshot
                        )

                    onTripChanged(
                        trip
                    )

                } catch (e: Exception) {

                    onError(e)
                }
            }
    }

    suspend fun acceptTrip(
        tripId: String,
        driverId: String
    ): Boolean {
        val tripRef = db.collection("trips").document(tripId)
        val driverRef = db.collection("drivers").document(driverId)

        return db.runTransaction { transaction ->
            // All reads must happen before writes in a Firestore transaction.
            val tripSnapshot = transaction.get(tripRef)
            val driverSnapshot = transaction.get(driverRef)

            if (!tripSnapshot.exists() || !driverSnapshot.exists()) {
                return@runTransaction false
            }

            val currentStatus = tripSnapshot.getString("status")
            val driverStatus = driverSnapshot.getString("status")

            // The trip lock prevents two drivers accepting the same request.
            // The driver status lock prevents one driver accepting two trips.
            if (currentStatus != TripStatus.REQUESTED.name ||
                driverStatus != "ONLINE"
            ) {
                return@runTransaction false
            }

            transaction.update(
                tripRef,
                mapOf(
                    "driverId" to driverId,
                    "status" to TripStatus.ACCEPTED.name
                )
            )
            transaction.update(driverRef, "status", "ON_TRIP")
            true
        }.await()
    }

    suspend fun cancelTrip(
        tripId: String,
        actorId: String,
        actorIsDriver: Boolean
    ): Boolean {
        val tripRef = db.collection("trips").document(tripId)
        return db.runTransaction { transaction ->
            val tripSnapshot = transaction.get(tripRef)
            if (!tripSnapshot.exists()) return@runTransaction false

            val passengerId = tripSnapshot.getString("passengerId") ?: ""
            val driverId = tripSnapshot.getString("driverId") ?: ""
            val status = tripSnapshot.getString("status")
            val allowedStatuses = if (actorIsDriver) {
                setOf(TripStatus.ACCEPTED.name, TripStatus.DRIVER_ARRIVED.name)
            } else {
                setOf(TripStatus.REQUESTED.name, TripStatus.ACCEPTED.name)
            }

            if (status !in allowedStatuses) return@runTransaction false
            if (actorIsDriver && driverId != actorId) return@runTransaction false
            if (!actorIsDriver && passengerId != actorId) return@runTransaction false

            val driverRef = if (driverId.isNotBlank()) {
                db.collection("drivers").document(driverId)
            } else null
            val driverSnapshot = driverRef?.let { transaction.get(it) }

            transaction.update(
                tripRef,
                mapOf(
                    "status" to TripStatus.CANCELLED.name,
                    "cancelledAt" to System.currentTimeMillis(),
                    "cancelledBy" to if (actorIsDriver) "DRIVER" else "PASSENGER"
                )
            )
            if (driverRef != null && driverSnapshot?.exists() == true &&
                driverSnapshot.getString("status") == "ON_TRIP"
            ) {
                transaction.update(driverRef, "status", "ONLINE")
            }
            true
        }.await()
    }

    suspend fun driverArrived(
        tripId: String,
        driverId: String
    ): Boolean {

        return updateTripStatus(
            tripId =
                tripId,

            driverId =
                driverId,

            expectedStatus =
                TripStatus.ACCEPTED,

            newStatus =
                TripStatus.DRIVER_ARRIVED
        )
    }

    suspend fun startTrip(
        tripId: String,
        driverId: String
    ): Boolean {

        return updateTripStatus(
            tripId =
                tripId,

            driverId =
                driverId,

            expectedStatus =
                TripStatus.DRIVER_ARRIVED,

            newStatus =
                TripStatus.IN_PROGRESS,

            startedAt =
                System.currentTimeMillis()
        )
    }

    suspend fun completeTrip(
        tripId: String,
        driverId: String
    ): Boolean {

        val tripRef =
            db
                .collection("trips")
                .document(tripId)

        val driverRef =
            db
                .collection("drivers")
                .document(driverId)

        return db
            .runTransaction { transaction ->

                val tripSnapshot =
                    transaction.get(
                        tripRef
                    )

                val currentDriverId =
                    tripSnapshot
                        .getString(
                            "driverId"
                        )

                val currentStatus =
                    tripSnapshot
                        .getString(
                            "status"
                        )

                if (
                    currentDriverId !=
                    driverId
                ) {

                    return@runTransaction false
                }

                if (
                    currentStatus !=
                    TripStatus.IN_PROGRESS.name
                ) {

                    return@runTransaction false
                }

                transaction.update(
                    tripRef,
                    mapOf(
                        "status" to
                            TripStatus.COMPLETED.name,

                        "completedAt" to
                            System.currentTimeMillis()
                    )
                )

                transaction.update(
                    driverRef,
                    "status",
                    "ONLINE"
                )

                true
            }
            .await()
    }

    private suspend fun updateTripStatus(
        tripId: String,
        driverId: String,
        expectedStatus: TripStatus,
        newStatus: TripStatus,
        startedAt: Long? = null
    ): Boolean {

        val tripRef =
            db
                .collection("trips")
                .document(tripId)

        return db
            .runTransaction { transaction ->

                val tripSnapshot =
                    transaction.get(
                        tripRef
                    )

                val currentDriverId =
                    tripSnapshot
                        .getString(
                            "driverId"
                        )

                val currentStatus =
                    tripSnapshot
                        .getString(
                            "status"
                        )

                if (
                    currentDriverId !=
                    driverId
                ) {

                    return@runTransaction false
                }

                if (
                    currentStatus !=
                    expectedStatus.name
                ) {

                    return@runTransaction false
                }

                val updates =
                    mutableMapOf<String, Any>(
                        "status" to
                            newStatus.name
                    )

                if (
                    startedAt != null
                ) {

                    updates["startedAt"] =
                        startedAt
                }

                transaction.update(
                    tripRef,
                    updates
                )

                true
            }
            .await()
    }

    suspend fun getActiveTripForDriver(
        driverId: String
    ): Trip? {

        val snapshot =
            db
                .collection("trips")
                .whereEqualTo(
                    "driverId",
                    driverId
                )
                .get()
                .await()

        return snapshot.documents
            .map {
                documentToTrip(it)
            }
            .firstOrNull { trip ->

                trip.status ==
                    TripStatus.ACCEPTED ||

                trip.status ==
                    TripStatus.DRIVER_ARRIVED ||

                trip.status ==
                    TripStatus.IN_PROGRESS
            }
    }

    suspend fun getActiveTripForPassenger(
        passengerId: String
    ): Trip? {
        val snapshot = db.collection("trips")
            .whereEqualTo("passengerId", passengerId)
            .get()
            .await()

        return snapshot.documents
            .map { documentToTrip(it) }
            .filter { trip ->
                trip.status == TripStatus.REQUESTED ||
                    trip.status == TripStatus.ACCEPTED ||
                    trip.status == TripStatus.DRIVER_ARRIVED ||
                    trip.status == TripStatus.IN_PROGRESS
            }
            .maxByOrNull { trip ->
                // Most recent trip documents are not timestamped in this MVP;
                // use the start time when present, otherwise preserve query order.
                trip.startedAt ?: 0L
            }
    }

    suspend fun getUser(
        userId: String
    ): User? {

        val snapshot =
            db
                .collection("users")
                .document(userId)
                .get()
                .await()

        return snapshot
            .toObject(
                User::class.java
            )
            ?.copy(
                id = userId
            )
    }

    suspend fun getDriver(
        driverId: String
    ): Driver? {

        val snapshot =
            db
                .collection("drivers")
                .document(driverId)
                .get()
                .await()

        return snapshot
            .toObject(
                Driver::class.java
            )
            ?.copy(
                id = driverId
            )
    }

    suspend fun getVehicleForDriver(
        driverId: String
    ): Vehicle? {

        val driverSnapshot =
            db
                .collection("drivers")
                .document(driverId)
                .get()
                .await()

        val vehicleId =
            driverSnapshot
                .getString(
                    "vehicleId"
                )
                ?.trim()

        if (
            !vehicleId.isNullOrBlank()
        ) {

            val vehicleSnapshot =
                db
                    .collection("vehicles")
                    .document(
                        vehicleId
                    )
                    .get()
                    .await()

            if (
                vehicleSnapshot.exists()
            ) {

                return vehicleSnapshot
                    .toObject(
                        Vehicle::class.java
                    )
                    ?.copy(
                        id =
                            vehicleSnapshot.id
                    )
            }
        }

        val vehiclesSnapshot =
            db
                .collection("vehicles")
                .whereEqualTo(
                    "driverId",
                    driverId
                )
                .limit(1)
                .get()
                .await()

        val vehicleDocument =
            vehiclesSnapshot
                .documents
                .firstOrNull()
                ?: return null

        return vehicleDocument
            .toObject(
                Vehicle::class.java
            )
            ?.copy(
                id =
                    vehicleDocument.id
            )
    }

    private fun documentToTrip(
        document: DocumentSnapshot
    ): Trip {

        val originData =
            document.get(
                "origin"
            ) as? Map<*, *>

        val destinationData =
            document.get(
                "destination"
            ) as? Map<*, *>

        val origin =
            Place(
                name =
                    originData?.get("name")
                        as? String
                        ?: "",

                lat =
                    (
                        originData?.get("lat")
                            as? Number
                    )
                        ?.toDouble()
                        ?: 0.0,

                lng =
                    (
                        originData?.get("lng")
                            as? Number
                    )
                        ?.toDouble()
                        ?: 0.0
            )

        val destination =
            Place(
                name =
                    destinationData?.get("name")
                        as? String
                        ?: "",

                lat =
                    (
                        destinationData?.get("lat")
                            as? Number
                    )
                        ?.toDouble()
                        ?: 0.0,

                lng =
                    (
                        destinationData?.get("lng")
                            as? Number
                    )
                        ?.toDouble()
                        ?: 0.0
            )

        val statusString =
            document.getString(
                "status"
            )
                ?: TripStatus.REQUESTED.name

        val status =
            try {

                TripStatus.valueOf(
                    statusString
                )

            } catch (
                e: Exception
            ) {

                TripStatus.REQUESTED
            }

        return Trip(
            id =
                document.id,

            passengerId =
                document.getString(
                    "passengerId"
                )
                    ?: "",

            driverId =
                document.getString(
                    "driverId"
                )
                    ?: "",

            origin =
                origin,

            destination =
                destination,

            estimatedPrice =
                document.getDouble(
                    "estimatedPrice"
                )
                    ?: 0.0,

            finalPrice =
                document.getDouble(
                    "finalPrice"
                )
                    ?: 0.0,

            status =
                status,

            startedAt =
                document.getLong(
                    "startedAt"
                ),

            completedAt =
                document.getLong(
                    "completedAt"
                )
        )
    }
}