package com.cami.maratondoomsday.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cami.maratondoomsday.data.Movie
import com.cami.maratondoomsday.data.Note
import com.cami.maratondoomsday.domain.Formats
import com.cami.maratondoomsday.domain.Scheduler
import com.cami.maratondoomsday.ui.MarathonUi
import com.cami.maratondoomsday.ui.ScheduledMovie
import com.cami.maratondoomsday.ui.theme.DeepRed
import com.cami.maratondoomsday.ui.theme.Gold
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
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
    var editing by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var unwatching by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(movie.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                    Text(Formats.movieInfo(movie), style = MaterialTheme.typography.titleMedium, color = Gold)
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (movie.watched) MaterialTheme.colorScheme.secondaryContainer else DeepRed,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val text = when {
                        movie.watched && item.date != null -> "✓ La vimos el ${Formats.relativeLong(item.date, today)}"
                        movie.watched -> "✓ Ya la vimos"
                        item.date != null -> "📅 Toca: ${Formats.relativeLong(item.date, today)}"
                        else -> "📅 Sin fecha (revisa las reglas en Ajustes)"
                    }
                    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                }
            }

            if (!movie.watched) {
                item { BigButton("✓  Ya la vimos", onClick = { onWatched(movie) }, container = Gold, content = MaterialTheme.colorScheme.onSecondary) }
                item.date?.let { current ->
                    val next = Scheduler.nextAvailableDay(current, ui.info.rules)
                    if (next != null) {
                        item {
                            BigOutlinedButton(
                                "Mover al siguiente día disponible\n(${Formats.longDate(next, today)})",
                                onClick = { onMoveNext(movie, current) },
                            )
                        }
                    }
                }
                if (movie.notBefore != null && movie.notBefore.isAfter(today)) {
                    item {
                        TextButton(onClick = { onClearPostpone(movie) }) {
                            Text("Deshacer el cambio de día", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            } else {
                item { BigOutlinedButton("Cambiar la fecha en que la vimos", onClick = { pickingDate = true }) }
                item {
                    TextButton(onClick = { unwatching = true }) {
                        Text("Desmarcar: todavía no la vemos", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            item { BigOutlinedButton("Editar datos (título, año, dónde verla…)", onClick = { editing = true }) }

            item { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }

            item(key = "notes") {
                NotesSection(
                    notes = ui.notesFor(movie.id),
                    onAdd = { onAddNote(movie.id, it) },
                    onEdit = onEditNote,
                    onDelete = onDeleteNote,
                )
            }

            item { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }

            item {
                TextButton(onClick = { deleting = true }) {
                    Text("🗑  Quitar del maratón", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
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
