package com.cami.maratondoomsday.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cami.maratondoomsday.data.MarathonRepository

@Composable
private fun CenteredPage(content: @Composable () -> Unit) {
    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
    }
}

@Composable
fun SetupScreen(
    busy: Boolean,
    error: String?,
    onCreate: () -> Unit,
    onJoin: (String) -> Unit,
) {
    var joining by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }

    CenteredPage {
        Text("🍿🎬", fontSize = 56.sp)
        Text(
            "Maratón rumbo a\nDOOMSDAY",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            "Para que los dos celulares vean lo mismo, uno crea el maratón y el otro se une con el código.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        if (!joining) {
            BigButton("Crear maratón", onClick = onCreate, enabled = !busy)
            BigButton(
                "Unirse con código",
                onClick = { joining = true },
                enabled = !busy,
                container = MaterialTheme.colorScheme.secondary,
                content = MaterialTheme.colorScheme.onSecondary,
            )
        } else {
            OutlinedTextField(
                value = code,
                onValueChange = { code = MarathonRepository.normalizeCode(it) },
                label = { Text("Código de 6 caracteres") },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    textAlign = TextAlign.Center, letterSpacing = 6.sp, fontWeight = FontWeight.Bold,
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
            )
            BigButton("Unirme", onClick = { onJoin(code) }, enabled = !busy && code.length == 6)
            TextButton(onClick = { joining = false }) {
                Text("← Regresar", style = MaterialTheme.typography.titleMedium)
            }
        }

        if (busy) CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ChooseAuthorScreen(onChoose: (String) -> Unit) {
    CenteredPage {
        Text("👋", fontSize = 56.sp)
        Text(
            "¿Quién usa este celular?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            "Así sabremos quién escribió cada nota. Se puede cambiar después en Ajustes.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BigButton("👧  Soy Cami", onClick = { onChoose("Cami") })
        BigButton(
            "👨  Soy Papá",
            onClick = { onChoose("Papá") },
            container = MaterialTheme.colorScheme.secondary,
            content = MaterialTheme.colorScheme.onSecondary,
        )
    }
}

@Composable
fun LoadingScreen(error: String?, onRetry: () -> Unit, onLeave: () -> Unit) {
    CenteredPage {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
        Text("Cargando el maratón…", style = MaterialTheme.typography.titleLarge)
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            BigButton("Reintentar", onClick = onRetry)
            HorizontalDivider()
            TextButton(onClick = onLeave) { Text("Usar otro código", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
fun NotFoundScreen(onLeave: () -> Unit) {
    CenteredPage {
        Text("🤔", fontSize = 56.sp)
        Text(
            "Ya no encontramos este maratón.",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        BigButton("Crear o unirse a otro", onClick = onLeave)
    }
}

@Composable
fun CodeCreatedDialog(code: String, onShare: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¡Maratón creado! 🎉") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Este es su código:", style = MaterialTheme.typography.bodyLarge)
                Box(Modifier.padding(vertical = 12.dp)) {
                    Text(
                        code,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 6.sp,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Text(
                    "En el otro celular, elige “Unirse con código” y escríbelo. También lo puedes ver en Ajustes.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Entendido", style = MaterialTheme.typography.titleMedium) }
        },
        dismissButton = {
            TextButton(onClick = onShare) { Text("Compartir", style = MaterialTheme.typography.titleMedium) }
        },
    )
}

@Composable
fun MissingConfigScreen() {
    CenteredPage {
        Text("⚙️", fontSize = 56.sp)
        Text(
            "Falta configurar Firebase",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            "Este APK se compiló sin el archivo google-services.json. Sigue la guía (GUIA.md) para agregarlo y vuelve a compilar.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}
