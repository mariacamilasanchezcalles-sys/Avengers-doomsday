package com.cami.maratondoomsday.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.cami.maratondoomsday.ui.hud.HudButton
import com.cami.maratondoomsday.ui.hud.HudHeader
import com.cami.maratondoomsday.ui.hud.HudPanel
import com.cami.maratondoomsday.ui.hud.HudSquareButton
import com.cami.maratondoomsday.ui.hud.HudTone
import com.cami.maratondoomsday.ui.hud.chamfer
import com.cami.maratondoomsday.ui.theme.Gold
import com.cami.maratondoomsday.ui.theme.Holo
import com.cami.maratondoomsday.ui.theme.Orbitron
import com.cami.maratondoomsday.ui.theme.TextHi
import com.cami.maratondoomsday.ui.theme.TextLo
import java.time.LocalDate

@Composable
fun SettingsScreen(
    ui: MarathonUi,
    author: String?,
    online: Boolean,
    contentPadding: PaddingValues,
    onAuthor: (String) -> Unit,
    onCinemaDate: (LocalDate?) -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HudHeader(title = "Ajustes", kicker = "Panel de control", online = online) }

        // ---- Quién soy ----
        item {
            HudPanel(label = "¿Quién usa este celular?") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("María", "Mario").forEach { name ->
                        val selected = author == name
                        HudButton(
                            if (selected) "✓ $name" else name,
                            onClick = { onAuthor(name) },
                            tone = if (selected) HudTone.Red else HudTone.Ghost,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // ---- Fecha del cine ----
        item {
            HudPanel(label = "Fecha del cine", accent = Gold) {
                Text(
                    ui.info.cinemaDate?.let { Formats.longDate(it, ui.today).replaceFirstChar { c -> c.uppercase() } }
                        ?: "Por definir",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Gold,
                )
                HudButton(
                    if (ui.info.cinemaDate == null) "Elegir fecha" else "Cambiar fecha",
                    onClick = { pickingDate = true },
                    tone = HudTone.Gold,
                )
                if (ui.info.cinemaDate != null) {
                    HudButton("Quitar fecha (por definir)", onClick = { onCinemaDate(null) }, tone = HudTone.Ghost, minHeight = 52.dp, textSize = 18.sp)
                }
            }
        }

        // ---- Reglas ----
        item {
            HudPanel(label = "Películas por día") {
                Text(
                    "Si un día no se ve nada (como el martes de D&D), déjalo en 0.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextLo,
                )
                Formats.DAY_NAMES.forEachIndexed { i, day ->
                    val value = ui.info.rules.getOrElse(i) { 0 }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            day.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleLarge,
                            color = if (value == 0) TextLo else TextHi,
                            modifier = Modifier.weight(1f),
                        )
                        HudSquareButton(
                            onClick = { onRules(ui.info.rules.toMutableList().also { it[i] = (value - 1).coerceAtLeast(0) }) },
                            enabled = value > 0,
                        ) { Text("−", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Holo) }
                        Text(
                            "$value",
                            fontFamily = Orbitron,
                            fontWeight = FontWeight.Black,
                            fontSize = 26.sp,
                            textAlign = TextAlign.Center,
                            color = if (value == 0) TextLo else Gold,
                            modifier = Modifier.width(52.dp),
                        )
                        HudSquareButton(
                            onClick = { onRules(ui.info.rules.toMutableList().also { it[i] = (value + 1).coerceAtMost(5) }) },
                            enabled = value < 5,
                        ) { Icon(Icons.Filled.Add, contentDescription = "Más", tint = Holo) }
                    }
                }
                if (ui.info.rules != DEFAULT_RULES) {
                    HudButton("Volver a las reglas originales", onClick = { onRules(DEFAULT_RULES) }, tone = HudTone.Ghost, minHeight = 52.dp, textSize = 18.sp)
                }
            }
        }

        // ---- Código ----
        item {
            HudPanel(label = "Código de acceso", accent = Gold) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, Gold.copy(alpha = 0.5f), chamfer(10.dp))
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        ui.info.code,
                        fontFamily = Orbitron,
                        fontWeight = FontWeight.Black,
                        fontSize = 38.sp,
                        letterSpacing = 8.sp,
                        color = Gold,
                    )
                }
                Text(
                    "En el otro celular, elige “Unirse con código” y escribe este código.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextLo,
                )
                HudButton("Compartir código", onClick = onShareCode, tone = HudTone.Ghost, icon = Icons.Filled.Share)
            }
        }

        item {
            Spacer(Modifier.padding(top = 4.dp))
            HudButton("Salir de este maratón", onClick = { leaving = true }, tone = HudTone.Red, minHeight = 52.dp, textSize = 18.sp)
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
