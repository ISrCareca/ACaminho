package pt.kaminho.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.kaminho.app.data.firebase.toUserMessage
import pt.kaminho.app.data.model.Place
import pt.kaminho.app.data.model.Trip
import pt.kaminho.app.data.repository.TripRepository
import pt.kaminho.app.data.repository.TripRatingRepository

data class PassengerUiState(
    val busy: Boolean = false,
    val requestSent: Boolean = false,
    val error: String? = null,
    val currentTrip: Trip? = null,
    val driverName: String? = null,
    val driverRating: Double? = null,
    val vehicleName: String? = null,
    val vehiclePlate: String? = null,
    val vehiclePhotoUrl: String? = null
)

class PassengerViewModel(
    private val repo: TripRepository = TripRepository(),
    private val ratingRepository: TripRatingRepository = TripRatingRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(PassengerUiState())
    val state: StateFlow<PassengerUiState> = _state.asStateFlow()

    private var tripListener: ListenerRegistration? = null
    private var loadedDriverId: String? = null
    private var loadedPassengerId: String? = null


    fun load(passengerId: String) {
        if (loadedPassengerId == passengerId) return
        loadedPassengerId = passengerId
        viewModelScope.launch {
            try {
                val activeTrip = repo.getActiveTripForPassenger(passengerId)
                if (activeTrip != null) {
                    _state.value = _state.value.copy(
                        requestSent = true,
                        currentTrip = activeTrip,
                        error = null
                    )
                    if (activeTrip.id.isNotBlank()) observeTrip(activeTrip.id)
                    if (activeTrip.driverId.isNotBlank()) {
                        loadedDriverId = activeTrip.driverId
                        loadDriver(activeTrip.driverId)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.toUserMessage())
            }
        }
    }

    fun cancelCurrentTrip(passengerId: String) {
        val trip = _state.value.currentTrip ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null)
            try {
                val cancelled = repo.cancelTrip(
                    tripId = trip.id,
                    actorId = passengerId,
                    actorIsDriver = false
                )
                _state.value = _state.value.copy(
                    busy = false,
                    error = if (cancelled) null else "Esta viagem já não pode ser cancelada."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(busy = false, error = e.toUserMessage())
            }
        }
    }

    fun requestTrip(
        passengerId: String,
        pickup: String,
        destination: String
    ) {
        if (pickup.isBlank() || destination.isBlank()) {
            return
        }

        viewModelScope.launch {
            loadedDriverId = null
            _state.value = PassengerUiState(busy = true)

            try {
                val trip = Trip(
                    passengerId = passengerId,
                    driverId = "",
                    origin = Place(name = pickup),
                    destination = Place(name = destination),
                    estimatedPrice = 0.0,
                    finalPrice = 0.0
                )

                val tripId = repo.createTrip(trip)
                _state.value = PassengerUiState(requestSent = true)
                observeTrip(tripId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = PassengerUiState(error = e.toUserMessage())
            }
        }
    }

    private fun observeTrip(tripId: String) {
        tripListener?.remove()

        tripListener = repo.observeTrip(
            tripId = tripId,
            onTripChanged = { trip ->
                _state.value = _state.value.copy(
                    requestSent = true,
                    currentTrip = trip,
                    error = null
                )

                if (trip.driverId.isNotBlank() && trip.driverId != loadedDriverId) {
                    loadedDriverId = trip.driverId
                    loadDriver(trip.driverId)
                }
            },
            onError = { exception ->
                _state.value = _state.value.copy(
                    error = exception.toUserMessage()
                )
            }
        )
    }

    private fun loadDriver(driverId: String) {
        viewModelScope.launch {
            try {
                val user = repo.getUser(driverId)
                val driver = repo.getDriver(driverId)
                val vehicle = repo.getVehicleForDriver(driverId)
                val liveRating = try {
                    ratingRepository.getSummary(driverId).average
                } catch (_: Exception) {
                    null
                }

                val vehicleName = vehicle
                    ?.let {
                        listOf(it.brand, it.model)
                            .filter { value -> value.isNotBlank() }
                            .joinToString(" ")
                    }
                    ?.takeIf { it.isNotBlank() }

                val vehiclePlate = vehicle
                    ?.plate
                    ?.takeIf { it.isNotBlank() }

                val vehiclePhotoUrl = vehicle
                    ?.photoUrl
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }

                _state.value = _state.value.copy(
                    driverName = user?.name?.takeIf { it.isNotBlank() },
                    driverRating = liveRating ?: driver?.rating,
                    vehicleName = vehicleName,
                    vehiclePlate = vehiclePlate,
                    vehiclePhotoUrl = vehiclePhotoUrl
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.toUserMessage()
                )
            }
        }
    }

    override fun onCleared() {
        tripListener?.remove()
        tripListener = null
        super.onCleared()
    }
}
