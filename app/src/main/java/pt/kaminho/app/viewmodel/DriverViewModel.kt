package pt.kaminho.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.kaminho.app.data.firebase.toUserMessage
import pt.kaminho.app.data.model.DriverStatus
import pt.kaminho.app.data.model.Trip
import pt.kaminho.app.data.model.TripStatus
import pt.kaminho.app.data.repository.DriverRepository
import pt.kaminho.app.data.repository.TripRepository

data class DriverUiState(
    val online: Boolean = false,
    val status: DriverStatus =
        DriverStatus.OFFLINE,
    val busy: Boolean = false,
    val error: String? = null,
    val trips: List<Trip> = emptyList(),
    val acceptedTrip: Trip? = null
)

class DriverViewModel(
    private val repo: DriverRepository =
        DriverRepository(),

    private val tripRepo: TripRepository =
        TripRepository()
) : ViewModel() {

    private val _state =
        MutableStateFlow(
            DriverUiState()
        )

    val state: StateFlow<DriverUiState> =
        _state.asStateFlow()

    private var uid: String? = null

    private var requestedTripsListener:
        ListenerRegistration? = null

    fun load(
        driverId: String
    ) {

        if (uid == driverId) {
            return
        }

        uid = driverId

        viewModelScope.launch {

            try {

                val status =
                    repo.getStatus(
                        driverId
                    )

                when (status) {

                    DriverStatus.OFFLINE -> {

                        stopRequestedTripsListener()

                        _state.update {
                            it.copy(
                                online = false,
                                status =
                                    DriverStatus.OFFLINE,
                                trips = emptyList()
                            )
                        }
                    }

                    DriverStatus.ONLINE -> {

                        _state.update {
                            it.copy(
                                online = true,
                                status =
                                    DriverStatus.ONLINE
                            )
                        }

                        observeRequestedTrips()
                    }

                    DriverStatus.ON_TRIP -> {

                        stopRequestedTripsListener()

                        val activeTrip =
                            tripRepo
                                .getActiveTripForDriver(
                                    driverId
                                )

                        _state.update {
                            it.copy(
                                online = true,
                                status =
                                    DriverStatus.ON_TRIP,
                                trips = emptyList(),
                                acceptedTrip =
                                    activeTrip
                            )
                        }
                    }
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {

                _state.update {
                    it.copy(
                        error =
                            e.toUserMessage()
                    )
                }
            }
        }
    }

    fun setOnline(
        online: Boolean
    ) {

        val driverId =
            uid ?: return

        if (
            !online &&
            _state.value.status ==
                DriverStatus.ON_TRIP
        ) {
            return
        }

        viewModelScope.launch {

            _state.update {
                it.copy(
                    busy = true,
                    error = null
                )
            }

            try {

                val newStatus =
                    if (online) {
                        DriverStatus.ONLINE
                    } else {
                        DriverStatus.OFFLINE
                    }

                repo.setStatus(
                    driverId,
                    newStatus
                )

                _state.update {
                    it.copy(
                        online = online,
                        status =
                            newStatus,
                        busy = false,

                        trips =
                            if (online) {
                                it.trips
                            } else {
                                emptyList()
                            }
                    )
                }

                if (online) {
                    observeRequestedTrips()
                } else {
                    stopRequestedTripsListener()
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {

                _state.update {
                    it.copy(
                        busy = false,
                        error =
                            e.toUserMessage()
                    )
                }
            }
        }
    }

    private fun observeRequestedTrips() {

        stopRequestedTripsListener()

        requestedTripsListener =
            tripRepo.observeRequestedTrips(

                onTripsChanged = { trips ->

                    _state.update {
                        it.copy(
                            trips = trips
                        )
                    }
                },

                onError = { exception ->

                    _state.update {
                        it.copy(
                            error =
                                exception.toUserMessage()
                        )
                    }
                }
            )
    }

    fun acceptTrip(
        trip: Trip
    ) {

        val driverId =
            uid ?: return

        viewModelScope.launch {

            _state.update {
                it.copy(
                    busy = true,
                    error = null
                )
            }

            try {

                val accepted =
                    tripRepo.acceptTrip(
                        tripId =
                            trip.id,

                        driverId =
                            driverId
                    )

                if (!accepted) {

                    _state.update {
                        it.copy(
                            busy = false,
                            error =
                                "Esta viagem já foi aceite por outro motorista."
                        )
                    }

                    return@launch
                }

                stopRequestedTripsListener()

                val acceptedTrip =
                    trip.copy(
                        driverId =
                            driverId,

                        status =
                            TripStatus.ACCEPTED
                    )

                _state.update {
                    it.copy(
                        online = true,
                        status =
                            DriverStatus.ON_TRIP,
                        busy = false,
                        trips = emptyList(),
                        acceptedTrip =
                            acceptedTrip
                    )
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {

                _state.update {
                    it.copy(
                        busy = false,
                        error =
                            e.toUserMessage()
                    )
                }
            }
        }
    }

    fun driverArrived() {

        val driverId =
            uid ?: return

        val trip =
            _state.value.acceptedTrip
                ?: return

        if (
            trip.status !=
                TripStatus.ACCEPTED
        ) {
            return
        }

        viewModelScope.launch {

            _state.update {
                it.copy(
                    busy = true,
                    error = null
                )
            }

            try {

                val updated =
                    tripRepo.driverArrived(
                        tripId =
                            trip.id,

                        driverId =
                            driverId
                    )

                if (!updated) {

                    _state.update {
                        it.copy(
                            busy = false,
                            error =
                                "Não foi possível atualizar o estado da viagem."
                        )
                    }

                    return@launch
                }

                _state.update {
                    it.copy(
                        busy = false,
                        status =
                            DriverStatus.ON_TRIP,
                        acceptedTrip =
                            trip.copy(
                                status =
                                    TripStatus.DRIVER_ARRIVED
                            )
                    )
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {

                _state.update {
                    it.copy(
                        busy = false,
                        error =
                            e.toUserMessage()
                    )
                }
            }
        }
    }

    fun startTrip() {

        val driverId =
            uid ?: return

        val trip =
            _state.value.acceptedTrip
                ?: return

        if (
            trip.status !=
                TripStatus.DRIVER_ARRIVED
        ) {
            return
        }

        viewModelScope.launch {

            _state.update {
                it.copy(
                    busy = true,
                    error = null
                )
            }

            try {

                val updated =
                    tripRepo.startTrip(
                        tripId =
                            trip.id,

                        driverId =
                            driverId
                    )

                if (!updated) {

                    _state.update {
                        it.copy(
                            busy = false,
                            error =
                                "Não foi possível iniciar a viagem."
                        )
                    }

                    return@launch
                }

                _state.update {
                    it.copy(
                        busy = false,
                        status =
                            DriverStatus.ON_TRIP,
                        acceptedTrip =
                            trip.copy(
                                status =
                                    TripStatus.IN_PROGRESS,
                                startedAt =
                                    System.currentTimeMillis()
                            )
                    )
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {

                _state.update {
                    it.copy(
                        busy = false,
                        error =
                            e.toUserMessage()
                    )
                }
            }
        }
    }

    fun completeTrip() {

        val driverId =
            uid ?: return

        val trip =
            _state.value.acceptedTrip
                ?: return

        if (
            trip.status !=
                TripStatus.IN_PROGRESS
        ) {
            return
        }

        viewModelScope.launch {

            _state.update {
                it.copy(
                    busy = true,
                    error = null
                )
            }

            try {

                val completed =
                    tripRepo.completeTrip(
                        tripId =
                            trip.id,

                        driverId =
                            driverId
                    )

                if (!completed) {

                    _state.update {
                        it.copy(
                            busy = false,
                            error =
                                "Não foi possível concluir esta viagem."
                        )
                    }

                    return@launch
                }

                _state.update {
                    it.copy(
                        online = true,
                        status =
                            DriverStatus.ONLINE,
                        busy = false,
                        acceptedTrip = null,
                        trips = emptyList()
                    )
                }

                observeRequestedTrips()

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {

                _state.update {
                    it.copy(
                        busy = false,
                        error =
                            e.toUserMessage()
                    )
                }
            }
        }
    }

    fun cancelAcceptedTrip() {
        val driverId = uid ?: return
        val trip = _state.value.acceptedTrip ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                val cancelled = tripRepo.cancelTrip(
                    tripId = trip.id,
                    actorId = driverId,
                    actorIsDriver = true
                )
                if (!cancelled) {
                    _state.update {
                        it.copy(busy = false, error = "Esta viagem já não pode ser cancelada.")
                    }
                    return@launch
                }
                _state.update {
                    it.copy(
                        online = true,
                        status = DriverStatus.ONLINE,
                        busy = false,
                        acceptedTrip = null,
                        trips = emptyList(),
                        error = null
                    )
                }
                observeRequestedTrips()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = e.toUserMessage()) }
            }
        }
    }

    private fun stopRequestedTripsListener() {

        requestedTripsListener?.remove()
        requestedTripsListener = null
    }

    override fun onCleared() {

        stopRequestedTripsListener()

        super.onCleared()
    }
}