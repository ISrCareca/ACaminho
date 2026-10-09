package pt.kaminho.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Year
import pt.kaminho.app.data.firebase.toUserMessage
import pt.kaminho.app.data.model.Vehicle
import pt.kaminho.app.data.repository.VehicleRepository

data class VehicleProfileUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val vehicle: Vehicle? = null,
    val message: String? = null,
    val error: String? = null
)

class VehicleViewModel(
    private val repository: VehicleRepository = VehicleRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(VehicleProfileUiState())
    val state: StateFlow<VehicleProfileUiState> = _state.asStateFlow()

    private var loadedDriverId: String? = null

    fun load(driverId: String) {
        if (driverId.isBlank()) {
            _state.value = _state.value.copy(
                loading = false,
                error = "Não foi possível identificar o motorista."
            )
            return
        }

        if (loadedDriverId == driverId && !_state.value.loading) {
            return
        }

        loadedDriverId = driverId
        _state.value = VehicleProfileUiState(loading = true)

        viewModelScope.launch {
            try {
                val vehicle = repository.getVehicleForDriver(driverId)
                _state.value = VehicleProfileUiState(
                    loading = false,
                    vehicle = vehicle
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = VehicleProfileUiState(
                    loading = false,
                    error = e.toUserMessage()
                )
            }
        }
    }

    fun saveVehicle(
        driverId: String,
        brand: String,
        model: String,
        year: String,
        plate: String,
        photoUrl: String
    ) {
        val cleanBrand = brand.trim()
        val cleanModel = model.trim()
        val cleanPlate = plate.trim().uppercase()
        val cleanPhotoUrl = photoUrl.trim()
        val parsedYear = year.trim().toIntOrNull()

        val validationError = when {
            driverId.isBlank() -> "Não foi possível identificar o motorista."
            cleanBrand.isBlank() -> "Indica a marca do veículo."
            cleanModel.isBlank() -> "Indica o modelo do veículo."
            parsedYear == null -> "Indica um ano válido para o veículo."
            parsedYear != null && parsedYear !in 1900..(Year.now().value + 1) ->
                "O ano deve estar entre 1900 e ${Year.now().value + 1}."
            cleanPlate.isBlank() -> "Indica a matrícula do veículo."
            cleanPhotoUrl.isNotBlank() && !isHttpUrl(cleanPhotoUrl) ->
                "A fotografia deve ter um URL válido iniciado por https:// ou http://."
            else -> null
        }

        if (validationError != null) {
            _state.value = _state.value.copy(
                saving = false,
                message = null,
                error = validationError
            )
            return
        }

        _state.value = _state.value.copy(
            saving = true,
            message = null,
            error = null
        )

        viewModelScope.launch {
            try {
                val savedVehicle = repository.saveVehicle(
                    driverId = driverId,
                    brand = cleanBrand,
                    model = cleanModel,
                    year = parsedYear!!,
                    plate = cleanPlate,
                    photoUrl = cleanPhotoUrl
                )

                _state.value = VehicleProfileUiState(
                    saving = false,
                    vehicle = savedVehicle,
                    message = "Dados do veículo guardados."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    saving = false,
                    message = null,
                    error = e.toUserMessage()
                )
            }
        }
    }

    private fun isHttpUrl(value: String): Boolean {
        return try {
            val uri = Uri.parse(value)
            (uri.scheme == "https" || uri.scheme == "http") &&
                !uri.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }
}
