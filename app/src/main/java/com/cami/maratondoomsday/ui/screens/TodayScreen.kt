package com.cami.maratondoomsday.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cami.maratondoomsday.data.Movie
import com.cami.maratondoomsday.data.Note
import com.cami.maratondoomsday.domain.Formats
import com.cami.maratondoomsday.ui.MarathonUi
import com.cami.maratondoomsday.ui.ScheduledMovie
import com.cami.maratondoomsday.ui.hud.HudButton
import com.cami.maratondoomsday.ui.hud.HudHeader
import com.cami.maratondoomsday.ui.hud.HudLabel
import com.cami.maratondoomsday.ui.hud.HudPanel
import com.cami.maratondoomsday.ui.hud.HudTone
import com.cami.maratondoomsday.ui.hud.RadialGauge
import com.cami.maratondoomsday.ui.hud.StatCell
import com.cami.maratondoomsday.ui.theme.Gold
import com.cami.maratondoomsday.ui.theme.Holo
import com.cami.maratondoomsday.ui.theme.Mono
import com.cami.maratondoomsday.ui.theme.Orbitron
import com.cami.maratondoomsday.ui.theme.StarkRed
import com.cami.maratondoomsday.ui.theme.TextHi
import com.cami.maratondoomsday.ui.theme.TextLo

@Composable
fun TodayScreen(
    ui: MarathonUi,
    online: Boolean,
    contentPadding: PaddingValues,
    onWatched: (Movie) -> Unit,
    onOpenMovie: (String) -> Unit,
    onAddNote: (String, String) -> Unit,
    onEditNote: (Note, String) -> Unit,
    onDeleteNote: (Note) -> Unit,
) {
    val today = ui.today
    // La película cuyas notas se muestran abajo.
    val focus: ScheduledMovie? = ui.todayPending.firstOrNull() ?: ui.todayWatched.lastOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HudHeader(title = "Hoy", kicker = Formats.longDate(today, today), online = online) }

        // ---- Lo que toca hoy ----
        when {
            ui.allDone -> item {
                StatusPanel(
                    label = "Protocolo completo",
                    accent = Gold,
                    title = "¡Terminamos el maratón!",
                    body = "Ya vimos las ${ui.total}. Solo falta ir al cine.",
                    ui = ui,
                )
            }

            ui.todayPending.isNotEmpty() -> {
                val n = ui.todayPending.size
                ui.todayPending.forEachIndexed { i, item ->
                    item(key = "today-" + item.movie.id) {
                        TargetPanel(
                            label = if (n > 1) "Hoy toca · ${i + 1} de $n" else "Hoy toca",
                            movie = item.movie,
                            onWatched = { onWatched(item.movie) },
                            onOpen = { onOpenMovie(item.movie.id) },
                        )
                    }
                }
            }

            ui.todayWatched.isNotEmpty() -> item {
                StatusPanel(
                    label = "Misión cumplida",
                    accent = Gold,
                    title = "¡Listo por hoy!",
                    body = "Ya vimos " + ui.todayWatched.joinToString(" y ") { "“${it.movie.title}”" } + ".",
                    ui = ui,
                )
            }

            else -> item {
                StatusPanel(label = "Modo descanso", accent = Holo, title = "Hoy descansamos 😴", body = null, ui = ui)
            }
        }

        // ---- Aviso de atraso ----
        ui.behind?.let { b ->
            item {
                HudPanel(label = "Alerta de agenda", accent = StarkRed, blinkLabel = true) {
                    Text(
                        "Vamos atrasados: nos faltan ${b.pending} ${plural(b.pending, "película", "películas")} " +
                            "y quedan ${b.availableDays} ${plural(b.availableDays, "día disponible", "días disponibles")}.",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextHi,
                    )
                    Text(
                        "Tranquilos: en Ajustes podemos sumar películas a algún día.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextLo,
                    )
                }
            }
        }

        // ---- Progreso ----
        item { ProgressPanel(ui) }

        // ---- Cuenta regresiva ----
        item { CountdownPanel(ui) }

        // ---- Notas de la película de hoy ----
        if (focus != null) {
            item(key = "notes-" + focus.movie.id) {
                HudPanel(label = "Bitácora · notas") {
                    Text(
                        focus.movie.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Gold,
                    )
                    NotesSection(
                        notes = ui.notesFor(focus.movie.id),
                        onAdd = { onAddNote(focus.movie.id, it) },
                        onEdit = onEditNote,
                        onDelete = onDeleteNote,
                    )
                }
            }
        }
    }
}

private fun plural(n: Int, one: String, many: String) = if (n == 1) one else many

@Composable
private fun TargetPanel(label: String, movie: Movie, onWatched: () -> Unit, onOpen: () -> Unit) {
    HudPanel(label = label, accent = StarkRed, scanning = true, blinkLabel = true, padding = 18.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                movie.title,
                style = MaterialTheme.typography.headlineLarge,
                fontSize = 38.sp,
                lineHeight = 42.sp,
                textAlign = TextAlign.Center,
                color = TextHi,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatCell("Año", movie.year?.toString() ?: "—", Modifier.weight(0.8f))
            StatCell("Duración", Formats.duration(movie.durationMin, movie.approxDuration) ?: "—", Modifier.weight(1.1f))
        }
        StatCell("Dónde verla", movie.where.ifBlank { "—" }, valueColor = Gold, accent = Gold)
        HudButton(
            "Ya la vimos",
            onClick = onWatched,
            tone = HudTone.Gold,
            icon = Icons.Filled.Check,
            minHeight = 74.dp,
            textSize = 26.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        HudButton("Ver detalle", onClick = onOpen, tone = HudTone.Ghost, minHeight = 52.dp, textSize = 18.sp)
    }
}

@Composable
private fun StatusPanel(label: String, accent: androidx.compose.ui.graphics.Color, title: String, body: String?, ui: MarathonUi) {
    HudPanel(label = label, accent = accent, padding = 20.dp) {
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 36.sp,
            color = TextHi,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (body != null) {
            Text(body, style = MaterialTheme.typography.bodyLarge, color = TextLo, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        val next = ui.nextPending
        if (next?.date != null) {
            Column(Modifier.padding(top = 6.dp)) {
                HudLabel("La siguiente", color = Holo, size = 13.sp)
                Text(next.movie.title, style = MaterialTheme.typography.headlineSmall, color = Gold)
                Text(
                    Formats.whenPhrase(next.date, ui.today).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    color = TextHi,
                )
            }
        }
    }
}

@Composable
private fun ProgressPanel(ui: MarathonUi) {
    HudPanel(label = "Progreso del maratón") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadialGauge(done = ui.watchedCount, total = ui.total, size = 150.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "%02d".format(ui.watchedCount),
                        fontFamily = Orbitron, fontWeight = FontWeight.Black, fontSize = 34.sp, color = Gold,
                    )
                    Text("DE ${ui.total}", fontFamily = Mono, fontSize = 14.sp, letterSpacing = 2.sp, color = Holo)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "${ui.watchedCount} de ${ui.total} vistas",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextHi,
                )
                StatCell("Pendientes", "${ui.total - ui.watchedCount}")
                val pct = if (ui.total == 0) 0 else ui.watchedCount * 100 / ui.total
                StatCell("Completado", "$pct %", valueColor = Gold, accent = Gold)
            }
        }
    }
}

@Composable
private fun CountdownPanel(ui: MarathonUi) {
    val days = ui.daysToCinema
    HudPanel(label = "Cuenta regresiva · cine", accent = Gold) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                when {
                    days == null -> "--"
                    days < 0 -> "00"
                    else -> "%02d".format(days)
                },
                fontFamily = Orbitron,
                fontWeight = FontWeight.Black,
                fontSize = 60.sp,
                lineHeight = 62.sp,
                color = Gold,
            )
            Spacer(Modifier.width(12.dp))
            HudLabel(if (days == 1L) "día" else "días", color = Gold, size = 18.sp, modifier = Modifier.padding(bottom = 10.dp))
        }
        Text(
            when {
                days == null -> "Fecha del cine: por definir"
                days > 1 -> "Faltan $days días para ver Doomsday en el cine"
                days == 1L -> "¡Mañana vamos a ver Doomsday al cine!"
                days == 0L -> "¡Hoy vamos a ver Doomsday al cine!"
                else -> "Ya fuimos a ver Doomsday"
            },
            style = MaterialTheme.typography.titleLarge,
            color = TextHi,
        )
        Text(
            ui.info.cinemaDate?.let { Formats.longDate(it, ui.today).replaceFirstChar { c -> c.uppercase() } }
                ?: "Se elige en Ajustes cuando la sepamos.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextLo,
        )
    }
}
