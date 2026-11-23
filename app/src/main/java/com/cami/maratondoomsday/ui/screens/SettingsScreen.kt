package com.cami.maratondoomsday.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cami.maratondoomsday.data.DEFAULT_RULES
import com.cami.maratondoomsday.domain.Formats
import com.cami.maratondoomsday.ui.MarathonUi
import com.cami.maratondoomsday.ui.theme.Gold

@Composable
fun SettingsScreen(
    ui: MarathonUi,
    author: String?,
    contentPadding: PaddingValues,
    onAuthor: (String) -> Unit,
    onCinemaDate: (java.time.LocalDate?) -> Unit,
    onRules: (List<Int>) -> Unit,
    onShareCode: () -> Unit,
    onLeave: () -> Unit,
) {
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var leaving by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Ajustes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black) }

        // ---- Quién soy ----
        item {
            SettingsCard("¿Quién usa este celular?") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("Cami", "Papá").forEach { name ->
                        val selected = author == name
                        BigButton(
                            if (selected) "✓ $name" else name,
                            onClick = { onAuthor(name) },
                            modifier = Modifier.weight(1f),
                            container = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // ---- Fecha del cine ----
        item {
            SettingsCard("🎟️ Fecha del cine") {
                Text(
                    ui.info.cinemaDate?.let { Formats.longDate(it, ui.today).replaceFirstChar { c -> c.uppercase() } }
                        ?: "Por definir",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                )
                BigOutlinedButton(
                    if (ui.info.cinemaDate == null) "Elegir fecha" else "Cambiar fecha",
                    onClick = { pickingDate = true },
                )
                if (ui.info.cinemaDate != null) {
                    TextButton(onClick = { onCinemaDate(null) }) {
                        Text("Quitar fecha (por definir)", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        // ---- Reglas ----
        item {
            SettingsCard("📅 ¿Cuántas películas por día?") {
                Text(
                    "Si un día no se ve nada (por ejemplo, el martes de D&D), déjalo en 0.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Formats.DAY_NAMES.forEachIndexed { i, day ->
                    val value = ui.info.rules.getOrElse(i) { 0 }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            day.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        FilledTonalIconButton(
                            onClick = { onRules(ui.info.rules.toMutableList().also { it[i] = (value - 1).coerceAtLeast(0) }) },
                            enabled = value > 0,
                        ) { Text("−", fontSize = 26.sp, fontWeight = FontWeight.Bold) }
                        Text(
                            "$value",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (value == 0) MaterialTheme.colorScheme.onSurfaceVariant else Gold,
                            modifier = Modifier.width(48.dp),
                        )
                        FilledTonalIconButton(
                            onClick = { onRules(ui.info.rules.toMutableList().also { it[i] = (value + 1).coerceAtMost(5) }) },
                            enabled = value < 5,
                        ) { Icon(Icons.Filled.Add, contentDescription = "Más") }
                    }
                }
                if (ui.info.rules != DEFAULT_RULES) {
                    TextButton(onClick = { onRules(DEFAULT_RULES) }) {
                        Text("Volver a las reglas originales", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        // ---- Código ----
        item {
            SettingsCard("🔑 Código del maratón") {
                Text(
                    ui.info.code,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    color = Gold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "En el otro celular, elige “Unirse con código” y escribe este código.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BigOutlinedButton("Compartir código", onClick = onShareCode)
            }
        }

        item {
            TextButton(onClick = { leaving = true }) {
                Text("Salir de este maratón", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (pickingDate) {
        DatePickDialog(initial = ui.info.cinemaDate, onPick = { onCinemaDate(it) }, onDismiss = { pickingDate = false })
    }
    if (leaving) {
        ConfirmDialog(
            title = "¿Salir del maratón?",
            text = "Solo se quita de este celular. Nada se borra: pueden volver a entrar con el código ${ui.info.code}.",
            confirmText = "Salir",
            onConfirm = onLeave,
            onDismiss = { leaving = false },
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(title)
            content()
        }
    }
}
