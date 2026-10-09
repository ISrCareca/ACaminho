package pt.kaminho.app.ui.passenger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import pt.kaminho.app.data.model.TripStatus
import pt.kaminho.app.data.model.UserRole
import pt.kaminho.app.data.model.User
import pt.kaminho.app.ui.components.HomeScaffold
import pt.kaminho.app.ui.components.TripRatingCard
import pt.kaminho.app.viewmodel.PassengerViewModel
import pt.kaminho.app.viewmodel.TripRatingViewModel

@Composable
fun PassengerHomeScreen(
    user: User,
    onLogout: () -> Unit,
    viewModel: PassengerViewModel = viewModel()
) {

    var pickup by remember {
        mutableStateOf("")
    }

    var destination by remember {
        mutableStateOf("")
    }

    val state by viewModel.state
        .collectAsStateWithLifecycle()

    val ratingViewModel: TripRatingViewModel = viewModel()

    LaunchedEffect(user.id) { viewModel.load(user.id) }
    val ratingState by ratingViewModel.state.collectAsStateWithLifecycle()

    val currentTrip = state.currentTrip
    LaunchedEffect(currentTrip?.id, currentTrip?.status, user.id) {
        if (currentTrip != null &&
            currentTrip.status == TripStatus.COMPLETED &&
            currentTrip.id.isNotBlank()
        ) {
            ratingViewModel.loadTripRating(
                tripId = currentTrip.id,
                raterId = user.id,
                raterRole = UserRole.PASSENGER
            )
        }
    }

    val hasActiveTrip =
        state.currentTrip != null &&
            state.currentTrip?.status != TripStatus.COMPLETED &&
            state.currentTrip?.status != TripStatus.CANCELLED

    val canRequest =
        pickup.isNotBlank() &&
            destination.isNotBlank() &&
            !state.busy &&
            !hasActiveTrip

    HomeScaffold(
        title = "ACaminho",
        onLogout = onLogout
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Column {
                Text(
                    text = "Olá, ${user.name}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Para onde vamos?",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "MAPA",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "A localização e o mapa serão integrados nesta área.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Nova viagem",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pickup,
                        onValueChange = { pickup = it },
                        label = { Text("Local de recolha") },
                        placeholder = { Text("Ex.: Rua da Constituição, Porto") },
                        singleLine = true,
                        enabled = !hasActiveTrip,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text("Destino") },
                        placeholder = { Text("Ex.: Avenida da Boavista, Porto") },
                        singleLine = true,
                        enabled = !hasActiveTrip,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.requestTrip(
                                passengerId = user.id,
                                pickup = pickup,
                                destination = destination
                            )
                        },
                        enabled = canRequest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (state.busy) {
                                "A pedir viagem..."
                            } else {
                                "Pedir viagem"
                            }
                        )
                    }
                }
            }

            state.currentTrip?.let { trip ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        val title = when (trip.status) {
                            TripStatus.REQUESTED -> "A procurar motorista"
                            TripStatus.ACCEPTED -> "Motorista encontrado"
                            TripStatus.DRIVER_ARRIVED -> "Motorista chegou"
                            TripStatus.IN_PROGRESS -> "Viagem em curso"
                            TripStatus.COMPLETED -> "Viagem concluída"
                            TripStatus.CANCELLED -> "Viagem cancelada"
                        }

                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Recolha: ${trip.origin.name}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Destino: ${trip.destination.name}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        when (trip.status) {
                            TripStatus.REQUESTED -> {
                                Text(
                                    text = "A procurar um motorista para a tua viagem...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            TripStatus.ACCEPTED,
                            TripStatus.DRIVER_ARRIVED,
                            TripStatus.IN_PROGRESS,
                            TripStatus.COMPLETED -> {
                                Text(
                                    text = when (trip.status) {
                                        TripStatus.ACCEPTED -> "Um motorista aceitou a tua viagem."
                                        TripStatus.DRIVER_ARRIVED -> "O motorista chegou ao local de recolha."
                                        TripStatus.IN_PROGRESS -> "A tua viagem está em curso."
                                        TripStatus.COMPLETED -> "A viagem foi concluída."
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Motorista",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = state.driverName ?: "A carregar informação do motorista...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (state.driverRating != null && state.driverRating!! > 0.0) {
                                        "Avaliação: %.1f ★".format(state.driverRating)
                                    } else {
                                        "Sem avaliações"
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = state.vehicleName ?: "Veículo não definido",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = state.vehiclePlate?.let { "Matrícula: $it" }
                                        ?: "Matrícula não definida",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                val vehiclePhotoUrl = state.vehiclePhotoUrl?.trim()
                                if (!vehiclePhotoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = vehiclePhotoUrl,
                                        contentDescription = "Fotografia do veículo do motorista",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = "Fotografia do veículo não definida",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = when (trip.status) {
                                        TripStatus.ACCEPTED -> "Estado: A caminho do local de recolha"
                                        TripStatus.DRIVER_ARRIVED -> "Estado: Motorista chegou"
                                        TripStatus.IN_PROGRESS -> "Estado: Viagem em curso"
                                        TripStatus.COMPLETED -> "Estado: Viagem concluída"
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            TripStatus.CANCELLED -> {
                                Text(
                                    text = "A viagem foi cancelada.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        if (trip.status == TripStatus.REQUESTED) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.cancelCurrentTrip(user.id) },
                                enabled = !state.busy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (state.busy) "A cancelar..." else "Cancelar viagem")
                            }
                        }
                    }
                }
            }

            if (state.currentTrip?.status == TripStatus.COMPLETED) {
                TripRatingCard(
                    title = "Avalia o motorista",
                    loading = ratingState.loading,
                    submitting = ratingState.submitting,
                    existingScore = ratingState.existingScore,
                    selectedScore = ratingState.selectedScore,
                    message = ratingState.message,
                    error = ratingState.error,
                    onSelectScore = ratingViewModel::selectScore,
                    onSubmit = ratingViewModel::submitSelectedRating
                )
            }

            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
