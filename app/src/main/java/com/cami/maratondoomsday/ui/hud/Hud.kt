package com.cami.maratondoomsday.ui.hud

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cami.maratondoomsday.ui.theme.Gold
import com.cami.maratondoomsday.ui.theme.GoldDeep
import com.cami.maratondoomsday.ui.theme.Holo
import com.cami.maratondoomsday.ui.theme.HoloDim
import com.cami.maratondoomsday.ui.theme.Mono
import com.cami.maratondoomsday.ui.theme.Online
import com.cami.maratondoomsday.ui.theme.Orbitron
import com.cami.maratondoomsday.ui.theme.Panel
import com.cami.maratondoomsday.ui.theme.PanelHigh
import com.cami.maratondoomsday.ui.theme.Rajdhani
import com.cami.maratondoomsday.ui.theme.RedDeep
import com.cami.maratondoomsday.ui.theme.StarkRed
import com.cami.maratondoomsday.ui.theme.TextHi
import com.cami.maratondoomsday.ui.theme.TextLo
import com.cami.maratondoomsday.ui.theme.Void

/** Forma de la casa: esquinas cortadas en diagonal (arriba-izquierda y abajo-derecha). */
fun chamfer(cut: Dp) = CutCornerShape(topStart = cut, bottomEnd = cut)

private fun chamferPath(size: Size, cut: Float, grow: Float = 0f): Path {
    val l = -grow
    val t = -grow
    val r = size.width + grow
    val b = size.height + grow
    val c = cut + grow * 0.4f
    return Path().apply {
        moveTo(l + c, t)
        lineTo(r, t)
        lineTo(r, b - c)
        lineTo(r - c, b)
        lineTo(l, b)
        lineTo(l, t + c)
        close()
    }
}

/** Dibuja el marco HUD: contorno tenue, soportes brillantes en dos esquinas y los cortes resaltados. */
private fun DrawScope.hudFrame(accent: Color, cut: Float) {
    val w = size.width
    val h = size.height
    drawPath(chamferPath(size, cut), accent.copy(alpha = 0.38f), style = Stroke(1.dp.toPx()))
    val b = 22.dp.toPx()
    val sw = 3.dp.toPx()
    val half = sw / 2
    // Soporte arriba-derecha
    drawLine(accent, Offset(w - b, half), Offset(w, half), sw)
    drawLine(accent, Offset(w - half, 0f), Offset(w - half, b), sw)
    // Soporte abajo-izquierda
    drawLine(accent, Offset(0f, h - half), Offset(b, h - half), sw)
    drawLine(accent, Offset(half, h - b), Offset(half, h), sw)
    // Cortes diagonales
    drawLine(accent, Offset(0f, cut), Offset(cut, 0f), 2.dp.toPx())
    drawLine(accent, Offset(w, h - cut), Offset(w - cut, h), 2.dp.toPx())
}

// ------------------------------------------------------------------
// Fondo
// ------------------------------------------------------------------

/** Fondo de laboratorio: rejilla holográfica, resplandor superior y una línea de escaneo lenta. */
@Composable
fun HudBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val t = rememberInfiniteTransition(label = "fondo")
    val sweep by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "barrido",
    )
    Box(
        modifier
            .fillMaxSize()
            .background(Void)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(Holo.copy(alpha = 0.13f), Color.Transparent),
                        center = Offset(size.width * 0.5f, 0f),
                        radius = size.width * 1.2f,
                    )
                )
                val step = 30.dp.toPx()
                val grid = Holo.copy(alpha = 0.05f)
                var x = 0f
                while (x < size.width) {
                    drawLine(grid, Offset(x, 0f), Offset(x, size.height), 1f); x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f); y += step
                }
                val sy = sweep * (size.height + 200f) - 100f
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Holo.copy(alpha = 0.07f), Color.Transparent),
                        startY = sy - 90f, endY = sy + 90f,
                    ),
                    topLeft = Offset(0f, sy - 90f),
                    size = Size(size.width, 180f),
                )
            },
        content = content,
    )
}

// ------------------------------------------------------------------
// Textos
// ------------------------------------------------------------------

/** Etiqueta técnica en monoespaciado: "// OBJETIVO DE HOY". */
@Composable
fun HudLabel(text: String, modifier: Modifier = Modifier, color: Color = Holo, size: TextUnit = 14.sp) {
    Text(
        text.uppercase(),
        modifier = modifier,
        fontFamily = Mono,
        fontSize = size,
        letterSpacing = 2.sp,
        color = color,
    )
}

/** Punto que parpadea, para indicadores "en vivo". */
@Composable
fun BlinkDot(color: Color, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    val t = rememberInfiniteTransition(label = "parpadeo")
    val a by t.animateFloat(1f, 0.25f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alfa")
    Box(
        modifier
            .size(size)
            .drawBehind {
                drawCircle(color.copy(alpha = a * 0.35f), radius = this.size.minDimension)
                drawCircle(color.copy(alpha = a))
            }
    )
}

/** Encabezado de cada pestaña: subtítulo técnico, título grande y estado de conexión. */
@Composable
fun HudHeader(title: String, kicker: String, online: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HudLabel(kicker, Modifier.weight(1f), color = Holo, size = 13.sp)
            BlinkDot(if (online) Online else StarkRed, size = 8.dp)
            Spacer(Modifier.width(8.dp))
            HudLabel(if (online) "En línea" else "Sin red · local", color = if (online) Online else StarkRed, size = 12.sp)
        }
        Text(
            title.uppercase(),
            fontFamily = Orbitron,
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
            letterSpacing = 3.sp,
            color = Gold,
            modifier = Modifier.padding(top = 2.dp),
        )
        Box(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(2.dp)
                .drawBehind {
                    drawRect(Brush.horizontalGradient(listOf(Holo, Holo.copy(alpha = 0.25f), Color.Transparent)))
                    drawRect(Gold, size = Size(46.dp.toPx(), size.height))
                }
        )
    }
}

// ------------------------------------------------------------------
// Panel
// ------------------------------------------------------------------

@Composable
fun HudPanel(
    modifier: Modifier = Modifier,
    label: String? = null,
    accent: Color = Holo,
    fill: Color = Panel,
    cut: Dp = 18.dp,
    padding: Dp = 16.dp,
    scanning: Boolean = false,
    blinkLabel: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = chamfer(cut)
    val scan = if (scanning) {
        val t = rememberInfiniteTransition(label = "escaneo")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "y")
    } else null

    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(fill.copy(alpha = 0.96f), fill.copy(alpha = 0.78f))))
            .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.07f), Color.Transparent)))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .drawWithContent {
                drawContent()
                scan?.let { s ->
                    val y = s.value * size.height
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, accent.copy(alpha = 0.16f), Color.Transparent),
                            startY = y - 40f, endY = y + 40f,
                        ),
                        topLeft = Offset(0f, y - 40f), size = Size(size.width, 80f),
                    )
                }
                hudFrame(accent, cut.toPx())
            }
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (label != null) PanelLabel(label, accent, blinkLabel)
        content()
    }
}

@Composable
private fun PanelLabel(text: String, accent: Color, blink: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (blink) BlinkDot(accent, size = 8.dp) else Box(Modifier.size(8.dp).background(accent))
        Spacer(Modifier.width(8.dp))
        HudLabel(text, color = accent, size = 13.sp)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(accent.copy(alpha = 0.3f)))
        Spacer(Modifier.width(6.dp))
        repeat(3) { i ->
            Box(Modifier.padding(start = 3.dp).size(width = 4.dp, height = 10.dp).background(accent.copy(alpha = 0.8f - i * 0.25f)))
        }
    }
}

// ------------------------------------------------------------------
// Botones
// ------------------------------------------------------------------

enum class HudTone { Red, Gold, Ghost }

@Composable
fun HudButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: HudTone = HudTone.Red,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    minHeight: Dp = 64.dp,
    textSize: TextUnit = 22.sp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val top: Color
    val bottom: Color
    val edge: Color
    val ink: Color
    when (tone) {
        HudTone.Red -> { top = StarkRed; bottom = RedDeep; edge = Color(0xFFFF7A7A); ink = Color.White }
        HudTone.Gold -> { top = Gold; bottom = GoldDeep; edge = Color(0xFFFFE3A3); ink = Color(0xFF1A1200) }
        HudTone.Ghost -> { top = Holo.copy(alpha = 0.10f); bottom = Holo.copy(alpha = 0.03f); edge = Holo.copy(alpha = 0.75f); ink = Holo }
    }
    val glow = if (tone == HudTone.Ghost) Holo else top
    val cut = 14.dp
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .alpha(if (enabled) 1f else 0.38f)
            .drawBehind {
                if (enabled) {
                    val c = cut.toPx()
                    val boost = if (pressed) 1.8f else 1f
                    for (i in 1..4) {
                        drawPath(
                            chamferPath(size, c, grow = i * 2.5.dp.toPx()),
                            glow.copy(alpha = (0.14f / i) * boost),
                            style = Stroke(2.5.dp.toPx()),
                        )
                    }
                }
            }
            .clip(chamfer(cut))
            .background(Brush.verticalGradient(if (pressed) listOf(bottom, top) else listOf(top, bottom)))
            .border(1.5.dp, edge, chamfer(cut))
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = ink),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text.uppercase(),
                fontFamily = Rajdhani,
                fontWeight = FontWeight.Bold,
                fontSize = textSize,
                letterSpacing = 1.5.sp,
                color = ink,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Botón cuadrado pequeño (flechas, +/−, atrás). */
@Composable
fun HudSquareButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 52.dp,
    color: Color = Holo,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.3f)
            .clip(chamfer(10.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.5.dp, color.copy(alpha = 0.8f), chamfer(10.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

// ------------------------------------------------------------------
// Datos
// ------------------------------------------------------------------

/** Celda de dato: etiqueta pequeña arriba y valor grande abajo, con barra de acento a la izquierda. */
@Composable
fun StatCell(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = TextHi, accent: Color = Holo) {
    Row(modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(accent))
        Column(Modifier.padding(start = 8.dp)) {
            HudLabel(label, color = accent.copy(alpha = 0.85f), size = 12.sp)
            Text(value, fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 25.sp, color = valueColor)
        }
    }
}

/**
 * Medidor radial: un segmento por película (los vistos se encienden en dorado),
 * anillos de marcas que giran despacio y el número al centro.
 */
@Composable
fun RadialGauge(
    done: Int,
    total: Int,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    litColor: Color = Gold,
    center: @Composable () -> Unit,
) {
    val t = rememberInfiniteTransition(label = "medidor")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(30000, easing = LinearEasing)), label = "giro")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = this.center
            val r = this.size.minDimension / 2
            drawCircle(Brush.radialGradient(listOf(Holo.copy(alpha = 0.30f), Color.Transparent), c, r * 0.62f), r * 0.62f, c)
            rotate(rot) {
                drawCircle(
                    Holo.copy(alpha = 0.55f), r * 0.97f, c,
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                )
            }
            rotate(-rot * 0.6f) {
                for (i in 0 until 60) {
                    val long = i % 5 == 0
                    val outer = r * 0.90f
                    val inner = outer - (if (long) 9.dp else 4.dp).toPx()
                    val a = Math.toRadians(i * 6.0)
                    val cos = kotlin.math.cos(a).toFloat()
                    val sin = kotlin.math.sin(a).toFloat()
                    drawLine(
                        Holo.copy(alpha = if (long) 0.6f else 0.3f),
                        Offset(c.x + cos * inner, c.y + sin * inner),
                        Offset(c.x + cos * outer, c.y + sin * outer),
                        1.5f,
                    )
                }
            }
            val n = total.coerceAtLeast(1)
            val gap = if (n > 1) 4f else 0f
            val sweep = 360f / n - gap
            val ringR = r * 0.72f
            val stroke = r * 0.12f
            for (i in 0 until n) {
                val start = -90f + i * 360f / n + gap / 2
                val lit = i < done
                if (lit) {
                    drawArc(
                        litColor.copy(alpha = 0.25f), start, sweep, false,
                        topLeft = Offset(c.x - ringR, c.y - ringR), size = Size(ringR * 2, ringR * 2),
                        style = Stroke(stroke * 1.8f, cap = StrokeCap.Butt),
                    )
                }
                drawArc(
                    if (lit) litColor else Holo.copy(alpha = 0.16f), start, sweep, false,
                    topLeft = Offset(c.x - ringR, c.y - ringR), size = Size(ringR * 2, ringR * 2),
                    style = Stroke(stroke, cap = StrokeCap.Butt),
                )
            }
            drawCircle(Holo.copy(alpha = 0.65f), r * 0.56f, c, style = Stroke(1.dp.toPx()))
        }
        center()
    }
}

// ------------------------------------------------------------------
// Navegación inferior
// ------------------------------------------------------------------

data class HudTab(val label: String, val icon: ImageVector)

@Composable
fun HudNavBar(tabs: List<HudTab>, selected: Int, onSelect: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(PanelHigh.copy(alpha = 0.92f), Void)))
            .drawBehind {
                drawLine(Holo.copy(alpha = 0.55f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                val w = 70.dp.toPx()
                drawRect(Gold, topLeft = Offset((size.width - w) / 2, 0f), size = Size(w, 3.dp.toPx()))
            }
            .navigationBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEachIndexed { i, tab ->
                val sel = i == selected
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(chamfer(10.dp))
                        .background(
                            if (sel) Brush.verticalGradient(listOf(StarkRed.copy(alpha = 0.55f), RedDeep.copy(alpha = 0.35f)))
                            else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .border(if (sel) 1.5.dp else 1.dp, if (sel) Gold else HoloDim, chamfer(10.dp))
                        .clickable(role = Role.Tab) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(tab.icon, contentDescription = null, tint = if (sel) Gold else TextLo, modifier = Modifier.size(26.dp))
                        Text(
                            tab.label.uppercase(),
                            fontFamily = Rajdhani,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            letterSpacing = 1.5.sp,
                            color = if (sel) TextHi else TextLo,
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// Campos de texto
// ------------------------------------------------------------------

val hudFieldShape = chamfer(10.dp)

@Composable
fun hudFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Holo,
    unfocusedBorderColor = Holo.copy(alpha = 0.35f),
    focusedLabelColor = Gold,
    unfocusedLabelColor = TextLo,
    cursorColor = Gold,
    focusedContainerColor = Void.copy(alpha = 0.6f),
    unfocusedContainerColor = Void.copy(alpha = 0.6f),
    focusedTextColor = TextHi,
    unfocusedTextColor = TextHi,
)

@Composable
fun hudFieldTextStyle() = MaterialTheme.typography.bodyLarge.copy(color = TextHi)
