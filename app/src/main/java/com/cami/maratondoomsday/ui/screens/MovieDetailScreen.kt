package com.cami.maratondoomsday.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cami.maratondoomsday.data.Movie
import com.cami.maratondoomsday.data.Note
import com.cami.maratondoomsday.domain.Formats
import com.cami.maratondoomsday.domain.Scheduler
import com.cami.maratondoomsday.ui.MarathonUi
import com.cami.maratondoomsday.ui.ScheduledMovie
import com.cami.maratondoomsday.ui.hud.HudBackground
import com.cami.maratondoomsday.ui.hud.HudButton
import com.cami.maratondoomsday.ui.hud.HudLabel
import com.cami.maratondoomsday.ui.hud.HudPanel
import com.cami.maratondoomsday.ui.hud.HudSquareButton
import com.cami.maratondoomsday.ui.hud.HudTone
import com.cami.maratondoomsday.ui.hud.StatCell
import com.cami.maratondoomsday.ui.theme.Gold
import com.cami.maratondoomsday.ui.theme.Holo
import com.cami.maratondoomsday.ui.theme.StarkRed
import com.cami.maratondoomsday.ui.theme.TextHi
import com.cami.maratondoomsday.ui.theme.TextLo
import java.time.LocalDate

@Composable
fun MovieDetailScreen(
    item: ScheduledMovie,
    ui: MarathonUi,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onWatched: (Movie) -> Unit,
    onUnwatch: (Movie) -> Unit,
    onChangeWatchedDate: (Movie, LocalDate) -> Unit,
    onMoveNext: (Movie, LocalDate) -> Unit,
    onClearPostpone: (Movie) -> Unit,
    onEdit: (Movie, String, Int?, Int?, String) -> Unit,
    onDelete: (Movie) -> Unit,
    onAddNote: (String, String) -> Unit,
    onEditNote: (Note, String) -> Unit,
    onDeleteNote: (Note) -> Unit,
) {
    val movie = item.movie
    val today = ui.today
    val number = ui.items.indexOfFirst { it.movie.id == movie.id } + 1
    var editing by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var unwatching by rememberSaveable { mutableStateOf(false) }

    HudBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HudSquareButton(onClick = onBack, size = 56.dp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Holo)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        HudLabel("Expediente", color = Holo, size = 13.sp)
                        HudLabel("Película #%02d de %d".format(number, ui.total), color = Gold, size = 16.sp)
                    }
                }
            },
            snackbarHost = { HudSnackbarHost(snackbar) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 6.dp,
                    bottom = padding.calculateBottomPadding() + 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    val accent = if (movie.watched) Gold else StarkRed
                    HudPanel(accent = accent, padding = 18.dp) {
                        Text(movie.title, style = MaterialTheme.typography.headlineLarge, color = TextHi)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            StatCell("Año", movie.year?.toString() ?: "—", Modifier.weight(0.8f))
                            StatCell("Duración", Formats.duration(movie.durationMin, movie.approxDuration) ?: "—", Modifier.weight(1.1f))
                        }
                        StatCell("Dónde verla", movie.where.ifBlank { "—" }, valueColor = Gold, accent = Gold)
                        val (label, text) = when {
                            movie.watched && item.date != null -> "Estado · vista" to "La vimos el ${Formats.relativeLong(item.date, today)}"
                            movie.watched -> "Estado · vista" to "Ya la vimos"
                            item.date != null -> "Programada" to "Toca: ${Formats.relativeLong(item.date, today)}"
                            else -> "Sin fecha" to "Revisa las reglas en Ajustes"
                        }
                        Column(Modifier.padding(top = 4.dp)) {
                            HudLabel(label, color = accent, size = 13.sp)
                            Text(text.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleLarge, color = TextHi)
                        }
                    }
                }

                if (!movie.watched) {
                    item {
                        HudButton(
                            "Ya la vimos",
                            onClick = { onWatched(movie) },
                            tone = HudTone.Gold,
                            icon = Icons.Filled.Check,
                            minHeight = 72.dp,
                            textSize = 24.sp,
                        )
                    }
                    item.date?.let { current ->
                        Scheduler.nextAvailableDay(current, ui.info.rules)?.let { next ->
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    HudButton("Mover al siguiente día disponible", onClick = { onMoveNext(movie, current) }, tone = HudTone.Ghost)
                                    HudLabel("→ ${Formats.longDate(next, today)}", color = TextLo, size = 13.sp, modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                    }
                    if (movie.notBefore != null && movie.notBefore.isAfter(today)) {
                        item {
                            HudButton("Deshacer el cambio de día", onClick = { onClearPostpone(movie) }, tone = HudTone.Ghost, minHeight = 52.dp, textSize = 18.sp)
                        }
                    }
                } else {
                    item { HudButton("Cambiar la fecha en que la vimos", onClick = { pickingDate = true }, tone = HudTone.Ghost) }
                    item { HudButton("Desmarcar: todavía no la vemos", onClick = { unwatching = true }, tone = HudTone.Ghost, minHeight = 52.dp, textSize = 18.sp) }
                }

                item { HudButton("Editar datos", onClick = { editing = true }, tone = HudTone.Ghost, minHeight = 52.dp, textSize = 18.sp) }

                item(key = "notes") {
                    HudPanel(label = "Bitácora · notas") {
                        NotesSection(
                            notes = ui.notesFor(movie.id),
                            onAdd = { onAddNote(movie.id, it) },
                            onEdit = onEditNote,
                            onDelete = onDeleteNote,
                        )
                    }
                }

                item {
                    HudButton("Quitar del maratón", onClick = { deleting = true }, tone = HudTone.Red, minHeight = 52.dp, textSize = 18.sp)
                }
                item {
                    Text(
                        "Quitarla también borra sus notas en los dos celulares.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLo,
                    )
                }
            }
        }
    }

    if (editing) {
        MovieEditDialog(
            initial = movie,
            onSave = { t, y, d, w -> onEdit(movie, t, y, d, w) },
            onDismiss = { editing = false },
        )
    }
    if (deleting) {
        ConfirmDialog(
            title = "¿Quitar “${movie.title}”?",
            text = "Se borrará del maratón junto con sus notas, en los dos celulares.",
            confirmText = "Quitar",
            onConfirm = { onDelete(movie) },
            onDismiss = { deleting = false },
        )
    }
    if (unwatching) {
        ConfirmDialog(
            title = "¿Desmarcar?",
            text = "“${movie.title}” volverá a quedar pendiente y se acomodará en la agenda.",
            confirmText = "Desmarcar",
            onConfirm = { onUnwatch(movie) },
            onDismiss = { unwatching = false },
        )
    }
    if (pickingDate) {
        DatePickDialog(
            initial = movie.watchedDate,
            onPick = { onChangeWatchedDate(movie, it) },
            onDismiss = { pickingDate = false },
        )
    }
}
