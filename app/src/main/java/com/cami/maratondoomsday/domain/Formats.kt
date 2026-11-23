package com.cami.maratondoomsday.domain

import com.cami.maratondoomsday.data.Movie
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Formatos en español de México, escritos a mano para que se vean igual en todos los celulares. */
object Formats {
    val DAY_NAMES = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
    private val DAY_SHORT = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")
    private val MONTHS = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )
    private val MONTH_SHORT = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

    fun dayName(d: LocalDate) = DAY_NAMES[d.dayOfWeek.value - 1]

    /** "lunes 23 de noviembre" (agrega el año si no es el actual). */
    fun longDate(d: LocalDate, today: LocalDate = LocalDate.now()): String {
        val base = "${dayName(d)} ${d.dayOfMonth} de ${MONTHS[d.monthValue - 1]}"
        return if (d.year != today.year) "$base de ${d.year}" else base
    }

    /** "lun 23 nov" */
    fun shortDate(d: LocalDate): String =
        "${DAY_SHORT[d.dayOfWeek.value - 1]} ${d.dayOfMonth} ${MONTH_SHORT[d.monthValue - 1]}"

    /** "Hoy", "Mañana", "Ayer" o "lun 23 nov". */
    fun relativeShort(d: LocalDate, today: LocalDate): String = when (d) {
        today -> "Hoy"
        today.plusDays(1) -> "Mañana"
        today.minusDays(1) -> "Ayer"
        else -> shortDate(d)
    }

    /** "hoy, lunes 23 de noviembre" / "mañana, martes 24..." / "miércoles 25 de noviembre" */
    fun relativeLong(d: LocalDate, today: LocalDate): String = when (d) {
        today -> "hoy, ${longDate(d, today)}"
        today.plusDays(1) -> "mañana, ${longDate(d, today)}"
        today.minusDays(1) -> "ayer, ${longDate(d, today)}"
        else -> longDate(d, today)
    }

    /** "hoy", "mañana (miércoles 25 de noviembre)" o "el miércoles 25 de noviembre" */
    fun whenPhrase(d: LocalDate, today: LocalDate): String = when (d) {
        today -> "hoy"
        today.plusDays(1) -> "mañana (${longDate(d, today)})"
        else -> "el ${longDate(d, today)}"
    }

    /** "23 nov, 8:15 p.m." */
    fun noteTime(millis: Long): String {
        val dt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        val h12 = if (dt.hour % 12 == 0) 12 else dt.hour % 12
        val ampm = if (dt.hour < 12) "a.m." else "p.m."
        return "${dt.dayOfMonth} ${MONTH_SHORT[dt.monthValue - 1]}, $h12:${"%02d".format(dt.minute)} $ampm"
    }

    /** "1 h 44 min" o "~2 h 30 min" */
    fun duration(min: Int?, approx: Boolean): String? {
        if (min == null || min <= 0) return null
        val h = min / 60
        val m = min % 60
        val text = when {
            h == 0 -> "$m min"
            m == 0 -> "$h h"
            else -> "$h h $m min"
        }
        return if (approx) "~$text" else text
    }

    /** "2000 · 1 h 44 min · Disney+" */
    fun movieInfo(movie: Movie): String = listOfNotNull(
        movie.year?.toString(),
        duration(movie.durationMin, movie.approxDuration),
        movie.where.takeIf { it.isNotBlank() },
    ).joinToString("  ·  ")
}
