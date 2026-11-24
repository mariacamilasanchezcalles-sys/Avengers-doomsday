package com.cami.maratondoomsday.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.cami.maratondoomsday.ui.hud.HudBackground
import com.cami.maratondoomsday.ui.hud.HudButton
import com.cami.maratondoomsday.ui.hud.HudLabel
import com.cami.maratondoomsday.ui.hud.HudPanel
import com.cami.maratondoomsday.ui.hud.HudTone
import com.cami.maratondoomsday.ui.hud.RadialGauge
import com.cami.maratondoomsday.ui.hud.chamfer
import com.cami.maratondoomsday.ui.hud.hudFieldColors
import com.cami.maratondoomsday.ui.hud.hudFieldShape
import com.cami.maratondoomsday.ui.theme.Gold
import com.cami.maratondoomsday.ui.theme.Holo
import com.cami.maratondoomsday.ui.theme.Mono
import com.cami.maratondoomsday.ui.theme.Orbitron
import com.cami.maratondoomsday.ui.theme.StarkRed
import com.cami.maratondoomsday.ui.theme.TextHi
import com.cami.maratondoomsday.ui.theme.TextLo

@Composable
private fun CenteredPage(content: @Composable () -> Unit) {
    HudBackground {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
    }
}

/** Anillo que se va encendiendo en ciclo, como si el sistema estuviera arrancando. */
@Composable
private fun BootEmblem(text: String) {
    val t = rememberInfiniteTransition(label = "arranque")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "carga")
    val segments = 12
    RadialGauge(done = (p * (segments + 1)).toInt(), total = segments, size = 170.dp, litColor = Holo) {
        Text(text, fontFamily = Orbitron, fontWeight = FontWeight.Black, fontSize = 26.sp, color = Gold)
    }
}

/** Texto de consola con cursor parpadeante. */
@Composable
private fun ConsoleLine(text: String) {
    val t = rememberInfiniteTransition(label = "cursor")
    val a by t.animateFloat(1f, 0f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "alfa")
    Text(
        "> ${text.uppercase()}" + if (a > 0.5f) "_" else " ",
        fontFamily = Mono,
        fontSize = 14.sp,
        letterSpacing = 1.5.sp,
        color = Holo,
    )
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
        BootEmblem("m3")
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HudLabel("Sistema de maratón · v1.0", color = Holo, size = 13.sp)
            Text(
                "PROTOCOLO\nDOOMSDAY",
                fontFamily = Orbitron,
                fontWeight = FontWeight.Black,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                letterSpacing = 3.sp,
                color = Gold,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            "Maratón rumbo al cine. Un celular crea el maratón y el otro se une con el código.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = TextLo,
        )

        if (!joining) {
            HudButton("Crear maratón", onClick = onCreate, enabled = !busy, tone = HudTone.Red, minHeight = 70.dp)
            HudButton("Unirse con código", onClick = { joining = true }, enabled = !busy, tone = HudTone.Gold, minHeight = 70.dp)
        } else {
            HudPanel(label = "Código de acceso", accent = Gold) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = MarathonRepository.normalizeCode(it) },
                    placeholder = {
                        Text("······", fontFamily = Orbitron, fontSize = 30.sp, letterSpacing = 8.sp, color = TextLo, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.displaySmall.copy(
                        textAlign = TextAlign.Center, letterSpacing = 8.sp, color = Gold,
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    shape = hudFieldShape,
                    colors = hudFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                HudButton("Unirme", onClick = { onJoin(code) }, enabled = !busy && code.length == 6, tone = HudTone.Gold)
                HudButton("Regresar", onClick = { joining = false }, tone = HudTone.Ghost, minHeight = 52.dp, textSize = 18.sp)
            }
        }

        when {
            busy -> ConsoleLine("Conectando con la base de datos")
            error != null -> HudPanel(label = "Error", accent = StarkRed, blinkLabel = true) {
                Text(error, color = TextHi, style = MaterialTheme.typography.bodyLarge)
            }
            else -> ConsoleLine("Esperando instrucciones")
        }
    }
}

@Composable
fun ChooseAuthorScreen(onChoose: (String) -> Unit) {
    CenteredPage {
        HudLabel("Identificación", color = Holo)
        Text(
            "¿Quién usa este celular?",
            style = MaterialTheme.typography.headlineLarge,
            color = TextHi,
            textAlign = TextAlign.Center,
        )
        Text(
            "Así sabremos quién escribió cada nota. Se puede cambiar después en Ajustes.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = TextLo,
        )
        HudButton("Soy María", onClick = { onChoose("María") }, tone = HudTone.Red, minHeight = 76.dp, textSize = 26.sp)
        HudButton("Soy Mario", onClick = { onChoose("Mario") }, tone = HudTone.Gold, minHeight = 76.dp, textSize = 26.sp)
    }
}

@Composable
fun LoadingScreen(error: String?, onRetry: () -> Unit, onLeave: () -> Unit) {
    CenteredPage {
        BootEmblem("···")
        ConsoleLine("Sincronizando maratón")
        if (error != null) {
            HudPanel(label = "Sin conexión", accent = StarkRed, blinkLabel = true) {
                Text(error, color = TextHi, style = MaterialTheme.typography.bodyLarge)
            }
            HudButton("Reintentar", onClick = onRetry)
            HudButton("Usar otro código", onClick = onLeave, tone = HudTone.Ghost, minHeight = 52.dp, textSize = 18.sp)
        }
    }
}

@Composable
fun NotFoundScreen(onLeave: () -> Unit) {
    CenteredPage {
        HudPanel(label = "Maratón no encontrado", accent = StarkRed, blinkLabel = true) {
            Text("Ya no encontramos este maratón.", style = MaterialTheme.typography.headlineSmall, color = TextHi)
        }
        HudButton("Crear o unirse a otro", onClick = onLeave)
    }
}

@Composable
fun CodeCreatedDialog(code: String, onShare: () -> Unit, onDismiss: () -> Unit) {
    HudDialog(
        title = "Maratón creado",
        onDismiss = onDismiss,
        accent = Gold,
        buttons = {
            HudButton("Compartir código", onClick = onShare, tone = HudTone.Gold, icon = Icons.Filled.Share, minHeight = 56.dp)
            HudButton("Entendido", onClick = onDismiss, tone = HudTone.Ghost, minHeight = 56.dp)
        },
    ) {
        Text("Este es su código de acceso:", style = MaterialTheme.typography.bodyLarge, color = TextLo)
        Box(
            Modifier
                .fillMaxWidth()
                .border(1.dp, Gold.copy(alpha = 0.5f), chamfer(10.dp))
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(code, fontFamily = Orbitron, fontWeight = FontWeight.Black, fontSize = 36.sp, letterSpacing = 8.sp, color = Gold)
        }
        Text(
            "En el otro celular, elige “Unirse con código” y escríbelo. También lo puedes ver en Ajustes.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextHi,
        )
    }
}

@Composable
fun MissingConfigScreen() {
    CenteredPage {
        HudPanel(label = "Configuración incompleta", accent = StarkRed, blinkLabel = true) {
            Text("Falta configurar Firebase", style = MaterialTheme.typography.headlineSmall, color = TextHi)
            Text(
                "Este APK se compiló sin el archivo google-services.json. Sigue la guía (GUIA.md) para agregarlo y vuelve a compilar.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextLo,
            )
        }
    }
}
