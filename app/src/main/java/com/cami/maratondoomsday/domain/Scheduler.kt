package com.cami.maratondoomsday.domain

import com.cami.maratondoomsday.data.Movie
import java.time.LocalDate

/**
 * Calcula qué día toca cada película pendiente.
 *
 * - Las pendientes se acomodan en orden desde hoy (o desde el inicio del maratón si aún no empieza).
 * - Cada día admite las películas que diga la regla de ese día de la semana, menos las que ya
 *   se marcaron como vistas ese mismo día.
 * - Lo que no se vio en su día simplemente vuelve a acomodarse desde hoy: nada se pierde.
 * - "Mover al siguiente día disponible" guarda una fecha mínima (notBefore) para esa película;
 *   las que van después de ella en la lista también se recorren para respetar el orden.
 */
object Scheduler {

    fun capacity(rules: List<Int>, day: LocalDate): Int =
        rules.getOrElse(day.dayOfWeek.value - 1) { 0 }.coerceAtLeast(0)

    fun hasAnyDay(rules: List<Int>) = rules.any { it > 0 }

    private fun usedByDate(movies: List<Movie>): MutableMap<LocalDate, Int> =
        movies.filter { it.watched && it.watchedDate != null }
            .groupingBy { it.watchedDate!! }
            .eachCount()
            .toMutableMap()

    /** Devuelve id de película pendiente -> fecha asignada. Vacío si las reglas no permiten ningún día. */
    fun assignDates(
        moviesInOrder: List<Movie>,
        rules: List<Int>,
        today: LocalDate,
        startDate: LocalDate,
    ): Map<String, LocalDate> {
        val pending = moviesInOrder.filter { !it.watched }
        if (pending.isEmpty() || !hasAnyDay(rules)) return emptyMap()
        val used = usedByDate(moviesInOrder)
        val result = LinkedHashMap<String, LocalDate>()
        var day = maxOf(today, startDate)
        for (movie in pending) {
            movie.notBefore?.let { if (it > day) day = it }
            while ((used[day] ?: 0) >= capacity(rules, day)) day = day.plusDays(1)
            result[movie.id] = day
            used[day] = (used[day] ?: 0) + 1
        }
        return result
    }

    /** El primer día después de [after] en el que se puede ver algo según las reglas. */
    fun nextAvailableDay(after: LocalDate, rules: List<Int>): LocalDate? {
        if (!hasAnyDay(rules)) return null
        var d = after.plusDays(1)
        while (capacity(rules, d) <= 0) d = d.plusDays(1)
        return d
    }

    /** Cuántos días de [from] a [toInclusive] todavía tienen lugar para ver algo. */
    fun availableDays(
        moviesInOrder: List<Movie>,
        rules: List<Int>,
        from: LocalDate,
        toInclusive: LocalDate,
    ): Int {
        val used = usedByDate(moviesInOrder)
        var count = 0
        var d = from
        while (!d.isAfter(toInclusive)) {
            if (capacity(rules, d) - (used[d] ?: 0) > 0) count++
            d = d.plusDays(1)
        }
        return count
    }
}
