package com.cami.maratondoomsday.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cami.maratondoomsday.data.Movie
import com.cami.maratondoomsday.domain.Formats
import com.cami.maratondoomsday.ui.MarathonUi
import com.cami.maratondoomsday.ui.ScheduledMovie
import com.cami.maratondoomsday.ui.theme.DeepRed
import com.cami.maratondoomsday.ui.theme.Gold

@Composable
fun ListScreen(
    ui: MarathonUi,
    contentPadding: PaddingValues,
    onOpenMovie: (String) -> Unit,
    onMove: (index: Int, delta: Int) -> Unit,
    onAdd: (title: String, year: Int?, durationMin: Int?, where: String) -> Unit,
) {
    var reorder by rememberSaveable { mutableStateOf(false) }
    var adding by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Lista del maratón", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(
                        "${ui.watchedCount} de ${ui.total} vistas",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(onClick = { reorder = !reorder }) {
                    Text(if (reorder) "Listo" else "Reordenar", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        itemsIndexed(ui.items, key = { _, it -> it.movie.id }) { index, item ->
            MovieRow(
                number = index + 1,
                item = item,
                today = ui.today,
                reorder = reorder,
                canUp = index > 0,
                canDown = index < ui.items.lastIndex,
                onClick = { onOpenMovie(item.movie.id) },
                onUp = { onMove(index, -1) },
                onDown = { onMove(index, +1) },
            )
        }

        item {
            BigOutlinedButton("＋  Agregar película", onClick = { adding = true }, modifier = Modifier.padding(top = 8.dp))
        }
    }

    if (adding) {
        MovieEditDialog(initial = null, onSave = onAdd, onDismiss = { adding = false })
    }
}

@Composable
private fun MovieRow(
    number: Int,
    item: ScheduledMovie,
    today: java.time.LocalDate,
    reorder: Boolean,
    canUp: Boolean,
    canDown: Boolean,
    onClick: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    val movie: Movie = item.movie
    val isToday = !movie.watched && item.date == today
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) DeepRed else MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Círculo con número o palomita
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (movie.watched) Gold else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (movie.watched) {
                    Icon(Icons.Filled.Check, contentDescription = "Vista", tint = MaterialTheme.colorScheme.onSecondary)
                } else {
                    Text("$number", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                val dateText = when {
                    movie.watched -> "✓ Vista" + (item.date?.let { " · " + Formats.relativeShort(it, today) } ?: "")
                    item.date != null -> Formats.relativeShort(item.date, today)
                    else -> "Sin fecha"
                }
                Text(
                    dateText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (movie.watched) Gold else MaterialTheme.colorScheme.primary.takeIf { !isToday }
                        ?: Gold,
                )
                Text(
                    movie.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (movie.watched) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    Formats.movieInfo(movie),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (reorder) {
                Column {
                    IconButton(onClick = onUp, enabled = canUp) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Subir")
                    }
                    IconButton(onClick = onDown, enabled = canDown) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Bajar")
                    }
                }
            }
        }
    }
}
