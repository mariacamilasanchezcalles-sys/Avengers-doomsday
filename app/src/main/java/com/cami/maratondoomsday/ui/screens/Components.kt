package com.cami.maratondoomsday.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cami.maratondoomsday.data.DEFAULT_WHERE
import com.cami.maratondoomsday.data.Movie
import com.cami.maratondoomsday.data.Note
import com.cami.maratondoomsday.domain.Formats
import com.cami.maratondoomsday.ui.hud.HudButton
import com.cami.maratondoomsday.ui.hud.HudLabel
import com.cami.maratondoomsday.ui.hud.HudPanel
import com.cami.maratondoomsday.ui.hud.HudSquareButton
import com.cami.maratondoomsday.ui.hud.HudTone
import com.cami.maratondoomsday.ui.hud.chamfer
import com.cami.maratondoomsday.ui.hud.hudFieldColors
import com.cami.maratondoomsday.ui.hud.hudFieldShape
import com.cami.maratondoomsday.ui.hud.hudFieldTextStyle
import com.cami.maratondoomsday.ui.theme.Gold
import com.cami.maratondoomsday.ui.theme.Holo
import com.cami.maratondoomsday.ui.theme.HoloDim
import com.cami.maratondoomsday.ui.theme.Mono
import com.cami.maratondoomsday.ui.theme.PanelHigh
import com.cami.maratondoomsday.ui.theme.Rajdhani
import com.cami.maratondoomsday.ui.theme.StarkRed
import com.cami.maratondoomsday.ui.theme.TextHi
import com.cami.maratondoomsday.ui.theme.TextLo
import com.cami.maratondoomsday.ui.theme.Void
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Color de cada persona en la bitácora. */
/** Nombres antiguos (Cami/Papá) se muestran con los nuevos. */
fun displayName(author: String): String = when (author) {
    "Cami" -> "María"
    "Papá" -> "Mario"
    else -> author
}

fun authorColor(author: String): Color = when (displayName(author)) {
    "Mario" -> Gold
    "Mayda" -> Color(0xFFFF6B9A)
    else -> Holo
}

/** Ventana emergente con marco HUD. */
@Composable
fun HudDialog(
    title: String,
    onDismiss: () -> Unit,
    accent: Color = Holo,
    buttons: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        HudPanel(label = title, accent = accent, fill = PanelHigh, padding = 20.dp) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content()
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
                    buttons()
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    HudDialog(
        title = "Confirmar",
        onDismiss = onDismiss,
        accent = StarkRed,
        buttons = {
            HudButton(confirmText, onClick = { onConfirm(); onDismiss() }, tone = HudTone.Red, minHeight = 56.dp)
            HudButton("Cancelar", onClick = onDismiss, tone = HudTone.Ghost, minHeight = 56.dp)
        },
    ) {
        Text(title, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, color = TextHi)
        Text(text, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge, color = TextLo)
    }
}

/** Selector de fecha. Material usa milisegundos en UTC. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickDialog(initial: LocalDate?, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    val colors = DatePickerDefaults.colors(
        containerColor = PanelHigh,
        titleContentColor = Holo,
        headlineContentColor = Gold,
        weekdayContentColor = Holo,
        subheadContentColor = TextLo,
        navigationContentColor = Holo,
        yearContentColor = TextHi,
        currentYearContentColor = Gold,
        selectedYearContentColor = Color.White,
        selectedYearContainerColor = StarkRed,
        dayContentColor = TextHi,
        selectedDayContentColor = Color.White,
        selectedDayContainerColor = StarkRed,
        todayContentColor = Gold,
        todayDateBorderColor = Gold,
        dividerColor = HoloDim,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = chamfer(18.dp),
        colors = colors,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { HudLabel("Aceptar", color = Gold, size = 17.sp) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { HudLabel("Cancelar", color = TextLo, size = 17.sp) }
        },
    ) {
        DatePicker(state = state, colors = colors)
    }
}

@Composable
fun HudField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold) },
        singleLine = singleLine,
        minLines = minLines,
        textStyle = hudFieldTextStyle(),
        keyboardOptions = keyboardOptions,
        shape = hudFieldShape,
        colors = hudFieldColors(),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Formulario para agregar o editar una película. */
@Composable
fun MovieEditDialog(
    initial: Movie?,
    onSave: (title: String, year: Int?, durationMin: Int?, where: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(initial?.title ?: "") }
    var year by rememberSaveable { mutableStateOf(initial?.year?.toString() ?: "") }
    var duration by rememberSaveable { mutableStateOf(initial?.durationMin?.toString() ?: "") }
    var where by rememberSaveable { mutableStateOf(initial?.where ?: DEFAULT_WHERE) }
    val number = KeyboardOptions(keyboardType = KeyboardType.Number)

    HudDialog(
        title = if (initial == null) "Nueva película" else "Editar película",
        onDismiss = onDismiss,
        accent = Gold,
        buttons = {
            HudButton(
                "Guardar",
                onClick = {
                    onSave(title.trim(), year.toIntOrNull(), duration.toIntOrNull(), where.trim())
                    onDismiss()
                },
                enabled = title.isNotBlank(),
                minHeight = 56.dp,
            )
            HudButton("Cancelar", onClick = onDismiss, tone = HudTone.Ghost, minHeight = 56.dp)
        },
    ) {
        HudField(title, { title = it }, "Título", singleLine = false)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudField(year, { year = it.filter(Char::isDigit).take(4) }, "Año", Modifier.weight(1f), keyboardOptions = number)
            HudField(duration, { duration = it.filter(Char::isDigit).take(3) }, "Minutos", Modifier.weight(1f), keyboardOptions = number)
        }
        HudField(where, { where = it }, "Dónde verla")
    }
}

/** Notas de una película, con aspecto de bitácora + campo para agregar. */
@Composable
fun NotesSection(
    notes: List<Note>,
    onAdd: (String) -> Unit,
    onEdit: (Note, String) -> Unit,
    onDelete: (Note) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<Note?>(null) }
    var deleting by remember { mutableStateOf<Note?>(null) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (notes.isEmpty()) {
            Text(
                "Sin registros todavía. Anoten personajes, pistas, teorías o escenas post-créditos.",
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                color = TextLo,
            )
        }
        notes.forEachIndexed { i, note ->
            LogEntry(i + 1, note, onEdit = { editing = note }, onDelete = { deleting = note })
        }
        HudField(draft, { draft = it }, "Escribe una nota nueva", singleLine = false, minLines = 2)
        HudButton(
            "Agregar nota",
            onClick = { onAdd(draft); draft = "" },
            enabled = draft.isNotBlank(),
            tone = HudTone.Gold,
            icon = Icons.Filled.Add,
        )
    }

    editing?.let { note ->
        NoteEditDialog(note, onSave = { onEdit(note, it) }, onDismiss = { editing = null })
    }
    deleting?.let { note ->
        ConfirmDialog(
            title = "¿Borrar nota?",
            text = "“${note.text.take(80)}”",
            confirmText = "Borrar",
            onConfirm = { onDelete(note) },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun LogEntry(number: Int, note: Note, onEdit: () -> Unit, onDelete: () -> Unit) {
    val color = authorColor(note.author)
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(Void.copy(alpha = 0.45f), chamfer(8.dp)),
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(color))
        Column(Modifier.weight(1f).padding(start = 12.dp, top = 10.dp, bottom = 6.dp)) {
            val edited = if (note.editedAt != null) " · EDITADA" else ""
            Text(
                "#%02d  [%s]  %s%s".format(number, displayName(note.author).uppercase(), Formats.noteTime(note.createdAt).uppercase(), edited),
                fontFamily = Mono,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                color = color,
            )
            Text(
                note.text,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                color = TextHi,
                modifier = Modifier.padding(top = 4.dp, end = 4.dp),
            )
        }
        Column(
            Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HudSquareButton(onClick = onEdit, size = 44.dp) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar nota", tint = Holo, modifier = Modifier.size(20.dp))
            }
            HudSquareButton(onClick = onDelete, size = 44.dp, color = StarkRed) {
                Icon(Icons.Filled.Delete, contentDescription = "Borrar nota", tint = StarkRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun NoteEditDialog(note: Note, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(note.text) }
    HudDialog(
        title = "Editar nota",
        onDismiss = onDismiss,
        buttons = {
            HudButton("Guardar", onClick = { onSave(text); onDismiss() }, enabled = text.isNotBlank(), minHeight = 56.dp)
            HudButton("Cancelar", onClick = onDismiss, tone = HudTone.Ghost, minHeight = 56.dp)
        },
    ) {
        HudField(text, { text = it }, "Nota", singleLine = false, minLines = 3)
    }
}

/** Avisos inferiores ("¡Listo!… Deshacer") con estilo HUD. */
@Composable
fun HudSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = chamfer(12.dp),
            containerColor = PanelHigh,
            contentColor = TextHi,
            actionColor = Gold,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}
