package com.cami.maratondoomsday.data

import java.time.LocalDate

/** Una película o un bloque de episodios del maratón. */
data class Movie(
    val id: String,
    val title: String,
    val year: Int?,
    val durationMin: Int?,
    val approxDuration: Boolean,
    val where: String,
    val order: Long,
    val watched: Boolean,
    val watchedDate: LocalDate?,
    /** Si se usó "Mover al siguiente día disponible", no se agenda antes de esta fecha. */
    val notBefore: LocalDate?,
)

data class Note(
    val id: String,
    val movieId: String,
    val text: String,
    val author: String,
    val createdAt: Long,
    val editedAt: Long?,
)

data class MarathonInfo(
    val code: String,
    val cinemaDate: LocalDate?,
    /** Películas por día, de lunes (0) a domingo (6). */
    val rules: List<Int>,
    val startDate: LocalDate,
)

/** Lunes, martes (D&D), miércoles, jueves, viernes, sábado, domingo. */
val DEFAULT_RULES = listOf(1, 0, 1, 1, 1, 1, 2)
