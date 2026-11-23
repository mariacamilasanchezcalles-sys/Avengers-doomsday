package com.cami.maratondoomsday.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cami.maratondoomsday.data.DEFAULT_WHERE
import com.cami.maratondoomsday.data.Movie
import com.cami.maratondoomsday.data.Note
import com.cami.maratondoomsday.domain.Formats
import com.cami.maratondoomsday.ui.theme.Gold
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        shape = MaterialTheme.shapes.large,
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BigOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 60.dp),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.5.dp, Gold),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmText, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", style = MaterialTheme.typography.titleMedium) }
        },
    )
}

/** Selector de fecha. Material usa milisegundos en UTC. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickDialog(initial: LocalDate?, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text("Aceptar", style = MaterialTheme.typography.titleMedium) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", style = MaterialTheme.typography.titleMedium) }
        },
    ) {
        DatePicker(state = state)
    }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Agregar película" else "Editar película") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Título") }, singleLine = false,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = year, onValueChange = { year = it.filter(Char::isDigit).take(4) },
                        label = { Text("Año") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = duration, onValueChange = { duration = it.filter(Char::isDigit).take(3) },
                        label = { Text("Minutos") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = where, onValueChange = { where = it },
                    label = { Text("Dónde verla") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(title.trim(), year.toIntOrNull(), duration.toIntOrNull(), where.trim())
                    onDismiss()
                },
            ) { Text("Guardar", style = MaterialTheme.typography.titleMedium) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", style = MaterialTheme.typography.titleMedium) }
        },
    )
}

/** Lista de notas de una película + campo para agregar una nueva. */
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
        SectionTitle("📝 Notas importantes")
        if (notes.isEmpty()) {
            Text(
                "Todavía no hay notas. Anoten personajes, pistas, teorías o escenas post-créditos.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        notes.forEach { note ->
            NoteCard(note, onEdit = { editing = note }, onDelete = { deleting = note })
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text("Escribe una nota nueva") },
            minLines = 2,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        BigButton(
            "＋ Agregar nota",
            onClick = { onAdd(draft); draft = "" },
            enabled = draft.isNotBlank(),
            container = MaterialTheme.colorScheme.secondary,
            content = MaterialTheme.colorScheme.onSecondary,
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
private fun NoteCard(note: Note, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 16.dp, top = 14.dp, end = 4.dp, bottom = 4.dp)) {
            Text(note.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(end = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val who = if (note.author == "Papá") "👨 Papá" else if (note.author == "Cami") "👧 Cami" else note.author
                val edited = if (note.editedAt != null) " · editada" else ""
                Text(
                    "$who · ${Formats.noteTime(note.createdAt)}$edited",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Editar nota") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Borrar nota") }
            }
        }
    }
}

@Composable
private fun NoteEditDialog(note: Note, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(note.text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar nota") },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it }, minLines = 3,
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onSave(text); onDismiss() }) {
                Text("Guardar", style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", style = MaterialTheme.typography.titleMedium) }
        },
    )
}

@Composable
fun HSpace(w: Int) = Spacer(Modifier.width(w.dp))
