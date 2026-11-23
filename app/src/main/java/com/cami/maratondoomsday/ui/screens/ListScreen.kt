package com.cami.maratondoomsday.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cami.maratondoomsday.data.Movie
import com.cami.maratondoomsday.domain.Formats
import com.cami.maratondoomsday.ui.MarathonUi
import com.cami.maratondoomsday.ui.ScheduledMovie
import com.cami.maratondoomsday.ui.hud.HudButton
import com.cami.maratondoomsday.ui.hud.HudHeader
import com.cami.maratondoomsday.ui.hud.HudLabel
import com.cami.maratondoomsday.ui.hud.HudPanel
import com.cami.maratondoomsday.ui.hud.HudSquareButton
import com.cami.maratondoomsday.ui.hud.HudTone
import com.cami.maratondoomsday.ui.theme.Gold
import com.cami.maratondoomsday.ui.theme.Holo
import com.cami.maratondoomsday.ui.theme.Mono
import com.cami.maratondoomsday.ui.theme.Orbitron
import com.cami.maratondoomsday.ui.theme.Panel
import com.cami.maratondoomsday.ui.theme.StarkRed
import com.cami.maratondoomsday.ui.theme.TextHi
import com.cami.maratondoomsday.ui.theme.TextLo
import com.cami.maratondoomsday.ui.theme.Void
import java.time.LocalDate

@Composable
fun ListScreen(
    ui: MarathonUi,
    online: Boolean,
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
            start = 12.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item {
            Column(Modifier.padding(start = 4.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HudHeader(title = "Lista", kicker = "${ui.watchedCount} de ${ui.total} vistas", online = online)
                HudButton(
                    if (reorder) "Listo, guardar orden" else "Reordenar",
                    onClick = { reorder = !reorder },
                    tone = if (reorder) HudTone.Gold else HudTone.Ghost,
                    minHeight = 52.dp,
                    textSize = 18.sp,
                )
            }
        }

        itemsIndexed(ui.items, key = { _, it -> it.movie.id }) { index, item ->
            TimelineRow(
                number = index + 1,
                item = item,
                today = ui.today,
                isFirst = index == 0,
                isLast = index == ui.items.lastIndex,
                reorder = reorder,
                onClick = { onOpenMovie(item.movie.id) },
                onUp = { onMove(index, -1) },
                onDown = { onMove(index, +1) },
            )
        }

        item {
            HudButton(
                "Agregar película",
                onClick = { adding = true },
                tone = HudTone.Ghost,
                icon = Icons.Filled.Add,
                modifier = Modifier.padding(start = 4.dp, top = 16.dp),
            )
        }
    }

    if (adding) {
        MovieEditDialog(initial = null, onSave = onAdd, onDismiss = { adding = false })
    }
}

@Composable
private fun TimelineRow(
    number: Int,
    item: ScheduledMovie,
    today: LocalDate,
    isFirst: Boolean,
    isLast: Boolean,
    reorder: Boolean,
    onClick: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    val movie: Movie = item.movie
    val isToday = !movie.watched && item.date == today
    val accent = when {
        isToday -> StarkRed
        movie.watched -> Gold
        else -> Holo
    }

    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Línea de tiempo con marcador en rombo
        Box(
            Modifier
                .width(56.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2
                    val line = Holo.copy(alpha = 0.35f)
                    if (!isFirst) drawLine(line, Offset(x, 0f), Offset(x, size.height / 2), 2.dp.toPx())
                    if (!isLast) drawLine(line, Offset(x, size.height / 2), Offset(x, size.height), 2.dp.toPx())
                },
            contentAlignment = Alignment.Center,
        ) {
            Diamond(number, movie.watched, accent)
        }

        HudPanel(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 6.dp),
            accent = accent,
            fill = if (isToday) Color(0xFF2A0B10) else Panel,
            cut = 12.dp,
            padding = 14.dp,
            onClick = onClick,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val dateText = when {
                        movie.watched -> "✓ Vista · " + (item.date?.let { Formats.relativeShort(it, today) } ?: "")
                        isToday -> "▶ Hoy"
                        item.date != null -> Formats.relativeShort(item.date, today)
                        else -> "Sin fecha"
                    }
                    HudLabel(dateText, color = accent, size = 15.sp)
                    Text(
                        movie.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (movie.watched) TextLo else TextHi,
                    )
                    Text(
                        Formats.movieInfo(movie),
                        fontFamily = Mono,
                        fontSize = 14.sp,
                        color = TextLo,
                    )
                }
                if (reorder) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        HudSquareButton(onClick = onUp, enabled = !isFirst, size = 48.dp) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Subir", tint = Holo)
                        }
                        HudSquareButton(onClick = onDown, enabled = !isLast, size = 48.dp) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Bajar", tint = Holo)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Diamond(number: Int, watched: Boolean, accent: Color) {
    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val p = Path().apply {
                moveTo(w / 2, 0f); lineTo(w, h / 2); lineTo(w / 2, h); lineTo(0f, h / 2); close()
            }
            drawPath(p, if (watched) accent else Void)
            drawPath(p, accent, style = Stroke(2.dp.toPx()))
        }
        if (watched) {
            Icon(Icons.Filled.Check, contentDescription = "Vista", tint = Void, modifier = Modifier.size(20.dp))
        } else {
            Text(
                "%02d".format(number),
                fontFamily = Orbitron,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = accent,
            )
        }
    }
    Spacer(Modifier.size(0.dp))
}
