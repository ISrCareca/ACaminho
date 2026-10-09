package pt.kaminho.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TripRatingCard(
    title: String,
    loading: Boolean,
    submitting: Boolean,
    existingScore: Int?,
    selectedScore: Int,
    message: String?,
    error: String?,
    onSelectScore: (Int) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            when {
                loading -> Text("A carregar avaliação...")
                existingScore != null -> {
                    Text("Já avaliaste: ${"★".repeat(existingScore)}${"☆".repeat(5 - existingScore)}")
                    Text("Só é permitida uma avaliação por utilizador e por viagem.")
                    onDismiss?.let {
                        TextButton(onClick = it) { Text("Fechar") }
                    }
                }
                else -> {
                    Text("Escolhe uma classificação de 1 a 5 estrelas.")
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        (1..5).forEach { score ->
                            TextButton(
                                onClick = { onSelectScore(score) },
                                enabled = !submitting
                            ) {
                                Text(
                                    text = if (score <= selectedScore) "★" else "☆",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onSubmit,
                        enabled = !submitting && selectedScore in 1..5,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (submitting) "A guardar..." else "Enviar avaliação")
                    }
                }
            }

            if (!message.isNullOrBlank()) {
                Text(message, color = MaterialTheme.colorScheme.primary)
            }
            if (!error.isNullOrBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
