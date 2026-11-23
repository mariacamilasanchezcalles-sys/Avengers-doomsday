package com.cami.maratondoomsday.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cami.maratondoomsday.data.Movie
import com.cami.maratondoomsday.data.Note
import com.cami.maratondoomsday.domain.Formats
import com.cami.maratondoomsday.ui.MarathonUi
import com.cami.maratondoomsday.ui.ScheduledMovie
import com.cami.maratondoomsday.ui.theme.DeepGold
import com.cami.maratondoomsday.ui.theme.DeepRed
import com.cami.maratondoomsday.ui.theme.Gold

@Composable
fun TodayScreen(
    ui: MarathonUi,
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
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                Formats.longDate(today, today).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // ---- Lo que toca hoy ----
        when {
            ui.allDone -> item {
                MessageCard(
                    emoji = "🏆",
                    title = "¡Terminamos el maratón!",
                    subtitle = "Ya vimos las ${ui.total}. Solo falta ir al cine. 🍿",
                )
            }

            ui.todayPending.isNotEmpty() -> {
                item {
                    Text(
                        if (ui.todayPending.size > 1) "HOY TOCAN" else "HOY TOCA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                ui.todayPending.forEach { item ->
                    item(key = "today-" + item.movie.id) {
                        TodayMovieCard(item.movie, onWatched = { onWatched(item.movie) }, onOpen = { onOpenMovie(item.movie.id) })
                    }
                }
            }

            ui.todayWatched.isNotEmpty() -> item {
                MessageCard(
                    emoji = "🎉",
                    title = "¡Listo por hoy!",
                    subtitle = "Ya vimos " + ui.todayWatched.joinToString(" y ") { "“${it.movie.title}”" } + "." +
                        nextText(ui),
                )
            }

            else -> item {
                MessageCard(emoji = "😴", title = "Hoy descansamos", subtitle = nextText(ui).trim())
            }
        }

        // ---- Progreso ----
        item { ProgressCard(ui) }

        // ---- Cuenta regresiva ----
        item { CountdownCard(ui) }

        // ---- Aviso de atraso ----
        ui.behind?.let { b ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepGold),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "⚠️ Vamos atrasados: nos faltan ${b.pending} ${plural(b.pending, "película", "películas")} " +
                            "y quedan ${b.availableDays} ${plural(b.availableDays, "día disponible", "días disponibles")}. " +
                            "¡Ánimo! Podemos ajustar los días en Ajustes.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Gold,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        // ---- Notas de la película de hoy ----
        if (focus != null) {
            item(key = "notes-" + focus.movie.id) {
                Column {
                    Text(
                        "De “${focus.movie.title}”",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
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

private fun nextText(ui: MarathonUi): String {
    val next = ui.nextPending ?: return ""
    val date = next.date ?: return ""
    return "\n\nLa siguiente: “${next.movie.title}”, ${Formats.whenPhrase(date, ui.today)}."
}

@Composable
private fun TodayMovieCard(movie: Movie, onWatched: () -> Unit, onOpen: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DeepRed),
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen,
    ) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                movie.title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            val details = listOfNotNull(
                movie.year?.toString(),
                Formats.duration(movie.durationMin, movie.approxDuration),
            ).joinToString("  ·  ")
            if (details.isNotEmpty()) {
                Text(details, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            if (movie.where.isNotBlank()) {
                Text(
                    "📺 ${movie.where}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                )
            }
            BigButton(
                "✓  Ya la vimos",
                onClick = onWatched,
                container = Gold,
                content = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.padding(top = 6.dp).height(72.dp),
            )
        }
    }
}

@Composable
private fun MessageCard(emoji: String, title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(emoji, fontSize = 56.sp)
            Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ProgressCard(ui: MarathonUi) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "🎞️ ${ui.watchedCount} de ${ui.total} vistas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            LinearProgressIndicator(
                progress = { if (ui.total == 0) 0f else ui.watchedCount.toFloat() / ui.total },
                modifier = Modifier.fillMaxWidth().height(14.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                drawStopIndicator = {},
            )
        }
    }
}

@Composable
private fun CountdownCard(ui: MarathonUi) {
    val days = ui.daysToCinema
    val text = when {
        days == null -> "🎟️ Fecha del cine: por definir"
        days > 1 -> "🎟️ Faltan $days días para ver Doomsday en el cine"
        days == 1L -> "🎟️ ¡Mañana vamos a ver Doomsday al cine!"
        days == 0L -> "🍿 ¡Hoy vamos a ver Doomsday al cine!"
        else -> "🎬 Ya fuimos a ver Doomsday"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Gold)
            ui.info.cinemaDate?.let {
                Text(
                    Formats.longDate(it, ui.today).replaceFirstChar { c -> c.uppercase() },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

