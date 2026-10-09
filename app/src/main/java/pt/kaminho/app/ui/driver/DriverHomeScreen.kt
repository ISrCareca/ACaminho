package pt.kaminho.app.ui.driver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import pt.kaminho.app.data.model.DriverStatus
import pt.kaminho.app.data.model.TripStatus
import pt.kaminho.app.data.model.UserRole
import pt.kaminho.app.data.model.User
import pt.kaminho.app.ui.components.HomeScaffold
import pt.kaminho.app.ui.components.TripRatingCard
import pt.kaminho.app.viewmodel.DriverViewModel
import pt.kaminho.app.viewmodel.VehicleViewModel
import pt.kaminho.app.viewmodel.TripRatingViewModel

@Composable
fun DriverHomeScreen(
    user: User,
    onLogout: () -> Unit,
    viewModel: DriverViewModel = viewModel()
) {

    val state by viewModel.state
        .collectAsStateWithLifecycle()

    val vehicleViewModel: VehicleViewModel = viewModel()
    val vehicleState by vehicleViewModel.state.collectAsStateWithLifecycle()

    val ratingViewModel: TripRatingViewModel = viewModel()
    val ratingState by ratingViewModel.state.collectAsStateWithLifecycle()
    var completedTripId by rememberSaveable(user.id) { mutableStateOf("") }
    var completedPassengerId by rememberSaveable(user.id) { mutableStateOf("") }

    var brand by remember(user.id) { mutableStateOf("") }
    var model by remember(user.id) { mutableStateOf("") }
    var year by remember(user.id) { mutableStateOf("") }
    var plate by remember(user.id) { mutableStateOf("") }
    var photoUrl by remember(user.id) { mutableStateOf("") }

    LaunchedEffect(user.id) {
        viewModel.load(user.id)
        vehicleViewModel.load(user.id)
    }

    LaunchedEffect(vehicleState.vehicle) {
        vehicleState.vehicle?.let { vehicle ->
            brand = vehicle.brand
            model = vehicle.model
            year = if (vehicle.year > 0) vehicle.year.toString() else ""
            plate = vehicle.plate
            photoUrl = vehicle.photoUrl
        }
    }

    LaunchedEffect(state.trips.map { it.passengerId }.distinct()) {
        state.trips.map { it.passengerId }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach(ratingViewModel::loadTargetSummary)
    }

    LaunchedEffect(completedTripId, state.status, user.id) {
        if (completedTripId.isNotBlank() &&
            completedPassengerId.isNotBlank() &&
            state.status != DriverStatus.ON_TRIP
        ) {
            ratingViewModel.loadTripRating(
                tripId = completedTripId,
                raterId = user.id,
                raterRole = UserRole.DRIVER
            )
        }
    }

    HomeScaffold(
        title = "ACaminho",
        onLogout = onLogout
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    ),

            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {

            Column {

                Text(
                    text =
                        "Olá, ${user.name}",

                    style =
                        MaterialTheme.typography.headlineSmall,

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        when (state.status) {

                            DriverStatus.OFFLINE ->
                                "Estás indisponível para receber viagens."

                            DriverStatus.ONLINE ->
                                "Estás disponível para receber viagens."

                            DriverStatus.ON_TRIP ->
                                "Estás numa viagem."
                        },

                    style =
                        MaterialTheme.typography.bodyMedium
                )
            }

            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(18.dp)
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                when (state.status) {

                                    DriverStatus.OFFLINE ->
                                        "Offline"

                                    DriverStatus.ONLINE ->
                                        "Online"

                                    DriverStatus.ON_TRIP ->
                                        "Em viagem"
                                },

                            style =
                                MaterialTheme.typography.titleMedium,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                when (state.status) {

                                    DriverStatus.OFFLINE ->
                                        "Fica online para receber pedidos."

                                    DriverStatus.ONLINE ->
                                        "A procurar pedidos de viagem."

                                    DriverStatus.ON_TRIP ->
                                        "Não estás disponível para novos pedidos."
                                },

                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }

                    Switch(
                        checked =
                            state.status !=
                                DriverStatus.OFFLINE,

                        onCheckedChange =
                            viewModel::setOnline,

                        enabled =
                            !state.busy &&
                                state.status !=
                                DriverStatus.ON_TRIP
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "O teu veículo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Preenche os dados que serão apresentados ao passageiro quando aceitar uma viagem.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Marca") },
                        placeholder = { Text("Ex.: BMW") },
                        singleLine = true,
                        enabled = !vehicleState.saving,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Modelo") },
                        placeholder = { Text("Ex.: 320d") },
                        singleLine = true,
                        enabled = !vehicleState.saving,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = year,
                        onValueChange = { value -> year = value.filter(Char::isDigit).take(4) },
                        label = { Text("Ano") },
                        placeholder = { Text("Ex.: 2020") },
                        singleLine = true,
                        enabled = !vehicleState.saving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = plate,
                        onValueChange = { plate = it.uppercase() },
                        label = { Text("Matrícula") },
                        placeholder = { Text("Ex.: 12-AA-34") },
                        singleLine = true,
                        enabled = !vehicleState.saving,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = photoUrl,
                        onValueChange = { photoUrl = it.trimStart() },
                        label = { Text("URL da fotografia (opcional)") },
                        placeholder = { Text("https://.../veiculo.jpg") },
                        singleLine = true,
                        enabled = !vehicleState.saving,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Usa um URL direto para uma imagem acessível sem iniciar sessão. O endereço de uma página web não é necessariamente um URL de imagem.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Pré-visualização do veículo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Button(
                        onClick = {
                            vehicleViewModel.saveVehicle(
                                driverId = user.id,
                                brand = brand,
                                model = model,
                                year = year,
                                plate = plate,
                                photoUrl = photoUrl
                            )
                        },
                        enabled = !vehicleState.saving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (vehicleState.saving) "A guardar veículo..." else "Guardar veículo"
                        )
                    }

                    if (vehicleState.loading) {
                        Text(
                            text = "A carregar dados do veículo...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    vehicleState.message?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    vehicleState.error?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(230.dp),

                shape =
                    RoundedCornerShape(18.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {

                Box(
                    modifier =
                        Modifier.fillMaxSize(),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Text(
                            text =
                                "MAPA",

                            style =
                                MaterialTheme.typography.titleLarge,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )

                        Text(
                            text =
                                when (state.status) {

                                    DriverStatus.OFFLINE ->
                                        "Fica online para ativar a localização."

                                    DriverStatus.ONLINE ->
                                        "A localizar a tua posição..."

                                    DriverStatus.ON_TRIP ->
                                        "O mapa da viagem será apresentado aqui."
                                },

                            style =
                                MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Card(
                    modifier =
                        Modifier.weight(1f),

                    shape =
                        RoundedCornerShape(16.dp)
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp)
                    ) {

                        Text(
                            text =
                                "Ganhos hoje",

                            style =
                                MaterialTheme.typography.bodySmall
                        )

                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )

                        Text(
                            text =
                                "€0,00",

                            style =
                                MaterialTheme.typography.titleLarge,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }

                Card(
                    modifier =
                        Modifier.weight(1f),

                    shape =
                        RoundedCornerShape(16.dp)
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp)
                    ) {

                        Text(
                            text =
                                "Viagens",

                            style =
                                MaterialTheme.typography.bodySmall
                        )

                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )

                        Text(
                            text =
                                "0",

                            style =
                                MaterialTheme.typography.titleLarge,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }

            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(18.dp)
            ) {

                Column(
                    modifier =
                        Modifier.padding(16.dp)
                ) {

                    Text(
                        text =
                            when (state.status) {

                                DriverStatus.ON_TRIP ->
                                    "Viagem atual"

                                else ->
                                    "Pedidos de viagem"
                            },

                        style =
                            MaterialTheme.typography.titleMedium,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    when (state.status) {

                        DriverStatus.OFFLINE -> {

                            Text(
                                text =
                                    "Fica online para começar a receber viagens.",

                                style =
                                    MaterialTheme.typography.bodyMedium
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            Button(
                                onClick = {
                                    viewModel.setOnline(true)
                                },

                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Text(
                                    text =
                                        "Ficar online"
                                )
                            }
                        }

                        DriverStatus.ONLINE -> {

                            if (
                                state.trips.isEmpty()
                            ) {

                                Text(
                                    text =
                                        "À espera de novos pedidos...",

                                    style =
                                        MaterialTheme.typography.bodyMedium
                                )

                            } else {

                                state.trips.forEach { trip ->

                                    Card(
                                        modifier =
                                            Modifier.fillMaxWidth(),

                                        shape =
                                            RoundedCornerShape(14.dp)
                                    ) {

                                        Column(
                                            modifier =
                                                Modifier.padding(
                                                    14.dp
                                                )
                                        ) {

                                            Text(
                                                text =
                                                    "Nova viagem",

                                                style =
                                                    MaterialTheme.typography.titleMedium,

                                                fontWeight =
                                                    FontWeight.Bold
                                            )

                                            Spacer(
                                                modifier =
                                                    Modifier.height(8.dp)
                                            )

                                            Text(
                                                text =
                                                    "Recolha: ${trip.origin.name}",

                                                style =
                                                    MaterialTheme.typography.bodyMedium
                                            )

                                            Spacer(
                                                modifier =
                                                    Modifier.height(4.dp)
                                            )

                                            Text(
                                                text =
                                                    "Destino: ${trip.destination.name}",

                                                style =
                                                    MaterialTheme.typography.bodyMedium
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            val passengerSummary = ratingState.targetSummaries[trip.passengerId]
                                            Text(
                                                text = when {
                                                    passengerSummary?.average != null ->
                                                        "Avaliação do passageiro: %.1f ★ (%d)".format(
                                                            passengerSummary.average,
                                                            passengerSummary.count
                                                        )
                                                    passengerSummary != null ->
                                                        "Passageiro sem avaliações"
                                                    else ->
                                                        "A carregar avaliação do passageiro..."
                                                },
                                                style = MaterialTheme.typography.bodySmall
                                            )

                                            Spacer(
                                                modifier =
                                                    Modifier.height(12.dp)
                                            )

                                            Button(
                                                onClick = {
                                                    viewModel.acceptTrip(
                                                        trip
                                                    )
                                                },

                                                enabled =
                                                    !state.busy,

                                                modifier =
                                                    Modifier.fillMaxWidth()
                                            ) {

                                                Text(
                                                    text =
                                                        if (state.busy) {
                                                            "A aceitar..."
                                                        } else {
                                                            "Aceitar viagem"
                                                        }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(
                                        modifier =
                                            Modifier.height(10.dp)
                                    )
                                }
                            }
                        }

                        DriverStatus.ON_TRIP -> {

                            state.acceptedTrip?.let { trip ->

                                Text(
                                    text =
                                        when (trip.status) {

                                            TripStatus.ACCEPTED ->
                                                "A caminho do passageiro"

                                            TripStatus.DRIVER_ARRIVED ->
                                                "Chegou ao local de recolha"

                                            TripStatus.IN_PROGRESS ->
                                                "Viagem em curso"

                                            else ->
                                                "Viagem"
                                        },

                                    style =
                                        MaterialTheme.typography.titleMedium,

                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(10.dp)
                                )

                                Text(
                                    text =
                                        "Recolha: ${trip.origin.name}",

                                    style =
                                        MaterialTheme.typography.bodyMedium
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(4.dp)
                                )

                                Text(
                                    text =
                                        "Destino: ${trip.destination.name}",

                                    style =
                                        MaterialTheme.typography.bodyMedium
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(8.dp)
                                )

                                Text(
                                    text =
                                        when (trip.status) {

                                            TripStatus.ACCEPTED ->
                                                "O passageiro está à tua espera no local de recolha."

                                            TripStatus.DRIVER_ARRIVED ->
                                                "Confirmaste a chegada ao local."

                                            TripStatus.IN_PROGRESS ->
                                                "A viagem está a decorrer."

                                            else ->
                                                ""
                                        },

                                    style =
                                        MaterialTheme.typography.bodyMedium
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(12.dp)
                                )

                                when (trip.status) {

                                    TripStatus.ACCEPTED -> {

                                        Button(
                                            onClick = {
                                                viewModel.driverArrived()
                                            },

                                            enabled =
                                                !state.busy,

                                            modifier =
                                                Modifier.fillMaxWidth()
                                        ) {

                                            Text(
                                                text =
                                                    if (state.busy) {
                                                        "A atualizar..."
                                                    } else {
                                                        "Cheguei ao local"
                                                    }
                                            )
                                        }
                                    }

                                    TripStatus.DRIVER_ARRIVED -> {

                                        Button(
                                            onClick = {
                                                viewModel.startTrip()
                                            },

                                            enabled =
                                                !state.busy,

                                            modifier =
                                                Modifier.fillMaxWidth()
                                        ) {

                                            Text(
                                                text =
                                                    if (state.busy) {
                                                        "A iniciar..."
                                                    } else {
                                                        "Iniciar viagem"
                                                    }
                                            )
                                        }
                                    }

                                    TripStatus.IN_PROGRESS -> {

                                        Button(
                                            onClick = {
                                                completedTripId = trip.id
                                                completedPassengerId = trip.passengerId
                                                viewModel.completeTrip()
                                            },

                                            enabled =
                                                !state.busy,

                                            modifier =
                                                Modifier.fillMaxWidth()
                                        ) {

                                            Text(
                                                text =
                                                    if (state.busy) {
                                                        "A concluir..."
                                                    } else {
                                                        "Concluir viagem"
                                                    }
                                            )
                                        }
                                    }

                                    else -> Unit
                                }

                                if (trip.status == TripStatus.ACCEPTED ||
                                    trip.status == TripStatus.DRIVER_ARRIVED
                                ) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { viewModel.cancelAcceptedTrip() },
                                        enabled = !state.busy,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (state.busy) "A cancelar..." else "Cancelar viagem")
                                    }
                                }
                            }
                        }
                    }

                    state.error?.let { error ->

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                error,

                            color =
                                MaterialTheme.colorScheme.error,

                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }

                    if (completedTripId.isNotBlank() &&
                        completedPassengerId.isNotBlank() &&
                        state.status != DriverStatus.ON_TRIP
                    ) {
                        TripRatingCard(
                            title = "Avalia o passageiro",
                            loading = ratingState.loading,
                            submitting = ratingState.submitting,
                            existingScore = ratingState.existingScore,
                            selectedScore = ratingState.selectedScore,
                            message = ratingState.message,
                            error = ratingState.error,
                            onSelectScore = ratingViewModel::selectScore,
                            onSubmit = ratingViewModel::submitSelectedRating,
                            onDismiss = {
                                completedTripId = ""
                                completedPassengerId = ""
                            }
                        )
                    }
                }
            }
        }
    }
}