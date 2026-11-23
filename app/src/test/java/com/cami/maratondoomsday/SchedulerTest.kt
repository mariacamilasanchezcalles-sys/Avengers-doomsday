package com.cami.maratondoomsday

import com.cami.maratondoomsday.data.DEFAULT_RULES
import com.cami.maratondoomsday.data.Movie
import com.cami.maratondoomsday.domain.Scheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class SchedulerTest {
    private val sunday = LocalDate.of(2026, 11, 22)

    private fun movies(n: Int) = (1..n).map {
        Movie("m$it", "Película $it", 2000, 120, false, "Disney+", it * 1000L, false, null, null)
    }

    @Test
    fun primerDomingoTieneDosYMartesSeSalta() {
        val list = movies(5)
        val dates = Scheduler.assignDates(list, DEFAULT_RULES, sunday, sunday)
        assertEquals(sunday, dates["m1"])
        assertEquals(sunday, dates["m2"])
        assertEquals(sunday.plusDays(1), dates["m3"]) // lunes
        assertEquals(sunday.plusDays(3), dates["m4"]) // miércoles (martes = D&D)
        assertEquals(sunday.plusDays(4), dates["m5"]) // jueves
        assertTrue(dates.values.none { it.dayOfWeek == DayOfWeek.TUESDAY })
    }

    @Test
    fun loNoVistoSeRecorreAlDiaSiguiente() {
        val list = movies(3)
        // Ya es lunes y no se vio nada el domingo: todo empieza hoy.
        val monday = sunday.plusDays(1)
        val dates = Scheduler.assignDates(list, DEFAULT_RULES, monday, sunday)
        assertEquals(monday, dates["m1"])
        assertEquals(monday.plusDays(2), dates["m2"])
        assertEquals(3, dates.size)
    }

    @Test
    fun loVistoHoyOcupaElLugarDeHoy() {
        val list = movies(3).toMutableList()
        val monday = sunday.plusDays(1)
        list[0] = list[0].copy(watched = true, watchedDate = monday)
        val dates = Scheduler.assignDates(list, DEFAULT_RULES, monday, sunday)
        assertEquals(monday.plusDays(2), dates["m2"])
        assertEquals(null, dates["m1"])
    }

    @Test
    fun moverRespetaFechaMinimaYOrden() {
        val list = movies(3).toMutableList()
        list[0] = list[0].copy(notBefore = sunday.plusDays(1))
        val dates = Scheduler.assignDates(list, DEFAULT_RULES, sunday, sunday)
        assertEquals(sunday.plusDays(1), dates["m1"])
        assertEquals(sunday.plusDays(3), dates["m2"])
    }

    @Test
    fun siguienteDiaDisponibleSaltaMartes() {
        assertEquals(sunday.plusDays(3), Scheduler.nextAvailableDay(sunday.plusDays(1), DEFAULT_RULES))
    }

    @Test
    fun reglasEnCeroNoCiclanInfinito() {
        val dates = Scheduler.assignDates(movies(2), List(7) { 0 }, sunday, sunday)
        assertTrue(dates.isEmpty())
    }

    @Test
    fun diasDisponibles() {
        // dom 22 a dom 29: dom, lun, mié, jue, vie, sáb, dom = 7 días
        assertEquals(7, Scheduler.availableDays(movies(1), DEFAULT_RULES, sunday, sunday.plusDays(7)))
    }
}
