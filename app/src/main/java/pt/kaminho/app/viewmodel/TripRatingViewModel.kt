package pt.kaminho.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.kaminho.app.data.firebase.toUserMessage
import pt.kaminho.app.data.model.UserRole
import pt.kaminho.app.data.repository.RatingSummary
import pt.kaminho.app.data.repository.TripRatingRepository

data class TripRatingUiState(
    val tripId: String? = null,
    val raterId: String? = null,
    val raterRole: UserRole? = null,
    val loading: Boolean = false,
    val submitting: Boolean = false,
    val existingScore: Int? = null,
    val selectedScore: Int = 0,
    val summary: RatingSummary = RatingSummary(),
    val targetSummaries: Map<String, RatingSummary> = emptyMap(),
    val loadingTargetIds: Set<String> = emptySet(),
    val message: String? = null,
    val error: String? = null
)

class TripRatingViewModel(
    private val repository: TripRatingRepository = TripRatingRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(TripRatingUiState())
    val state: StateFlow<TripRatingUiState> = _state.asStateFlow()

    fun loadTripRating(
        tripId: String,
        raterId: String,
        raterRole: UserRole
    ) {
        if (tripId.isBlank() || raterId.isBlank()) return

        if (
            _state.value.tripId == tripId &&
            _state.value.raterId == raterId &&
            _state.value.raterRole == raterRole &&
            !_state.value.loading &&
            _state.value.existingScore != null
        ) return

        _state.value = _state.value.copy(
            tripId = tripId,
            raterId = raterId,
            raterRole = raterRole,
            loading = true,
            submitting = false,
            existingScore = null,
            selectedScore = 0,
            message = null,
            error = null
        )

        viewModelScope.launch {
            try {
                val existing = repository.getExistingRating(tripId, raterId)
                val targetId = repository.getTargetIdForTrip(tripId, raterId, raterRole)
                val summary = repository.getSummary(targetId)

                _state.value = _state.value.copy(
                    tripId = tripId,
                    raterId = raterId,
                    raterRole = raterRole,
                    loading = false,
                    existingScore = existing,
                    selectedScore = existing ?: 0,
                    summary = summary,
                    targetSummaries = _state.value.targetSummaries + (targetId to summary),
                    message = null,
                    error = null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.toUserMessage()
                )
            }
        }
    }

    fun selectScore(score: Int) {
        if (score !in 1..5 || _state.value.existingScore != null) return
        _state.value = _state.value.copy(selectedScore = score, error = null, message = null)
    }

    fun submitSelectedRating() {
        val current = _state.value
        val tripId = current.tripId ?: return
        val raterId = current.raterId ?: return
        val raterRole = current.raterRole ?: return
        val score = current.selectedScore

        if (score !in 1..5 || current.existingScore != null || current.submitting) return

        _state.value = current.copy(submitting = true, message = null, error = null)

        viewModelScope.launch {
            try {
                val targetId = repository.submitRating(
                    tripId = tripId,
                    raterId = raterId,
                    raterRole = raterRole,
                    score = score
                )

                // Marca a avaliação como enviada assim que a gravação termina,
                // mesmo que a consulta posterior da média falhe temporariamente.
                _state.value = _state.value.copy(
                    submitting = false,
                    existingScore = score,
                    selectedScore = score,
                    message = "Avaliação guardada.",
                    error = null
                )

                try {
                    val summary = repository.getSummary(targetId)
                    _state.value = _state.value.copy(
                        summary = summary,
                        targetSummaries = _state.value.targetSummaries + (targetId to summary)
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _state.value = _state.value.copy(error = e.toUserMessage())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    submitting = false,
                    error = e.toUserMessage()
                )
            }
        }
    }

    fun loadTargetSummary(targetId: String) {
        if (targetId.isBlank()) return
        val current = _state.value
        if (targetId in current.targetSummaries || targetId in current.loadingTargetIds) return

        _state.value = current.copy(loadingTargetIds = current.loadingTargetIds + targetId)
        viewModelScope.launch {
            try {
                val summary = repository.getSummary(targetId)
                _state.value = _state.value.copy(
                    targetSummaries = _state.value.targetSummaries + (targetId to summary),
                    loadingTargetIds = _state.value.loadingTargetIds - targetId
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loadingTargetIds = _state.value.loadingTargetIds - targetId,
                    error = e.toUserMessage()
                )
            }
        }
    }
}
