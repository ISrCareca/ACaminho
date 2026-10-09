package pt.kaminho.app.data.model

enum class UserRole {
    PASSENGER,
    DRIVER,
    ADMIN
}

enum class DriverStatus {
    OFFLINE,
    ONLINE,
    ON_TRIP
}

enum class TripStatus {
    REQUESTED,
    ACCEPTED,
    DRIVER_ARRIVED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: UserRole = UserRole.PASSENGER,
)

data class Driver(
    val id: String = "",
    val userId: String = "",
    val status: DriverStatus = DriverStatus.OFFLINE,
    val vehicleId: String = "",
    val rating: Double = 0.0,
)

data class Vehicle(
    val id: String = "",
    val driverId: String = "",
    val brand: String = "",
    val model: String = "",
    val plate: String = "",
    val year: Int = 0,
    val photoUrl: String = ""
)

data class Place(
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
)

data class Trip(
    val id: String = "",
    val passengerId: String = "",
    val driverId: String = "",
    val origin: Place = Place(),
    val destination: Place = Place(),
    val estimatedPrice: Double = 0.0,
    val finalPrice: Double = 0.0,
    val status: TripStatus = TripStatus.REQUESTED,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
)