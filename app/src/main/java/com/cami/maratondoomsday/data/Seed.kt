package com.cami.maratondoomsday.data

import java.time.LocalDate

data class SeedMovie(val title: String, val year: Int?, val durationMin: Int, val approx: Boolean = false)

/** La #1 se empieza el domingo 22 de noviembre de 2026. */
val MARATHON_START: LocalDate = LocalDate.of(2026, 11, 22)

val SEED_MOVIES = listOf(
    SeedMovie("X-Men", 2000, 104),
    SeedMovie("X2", 2003, 134),
    SeedMovie("Capitán América: El primer vengador", 2011, 124),
    SeedMovie("Los Vengadores", 2012, 143),
    SeedMovie("Avengers: Infinity War", 2018, 149),
    SeedMovie("Avengers: Endgame", 2019, 181),
    SeedMovie("Loki T1, episodios 1–3", 2021, 150, approx = true),
    SeedMovie("Loki T1, episodios 4–6", 2021, 150, approx = true),
    SeedMovie("Loki T2, episodios 1–3", 2023, 150, approx = true),
    SeedMovie("Loki T2, episodios 4–6", 2023, 160, approx = true),
    SeedMovie("Shang-Chi y la leyenda de los Diez Anillos", 2021, 132),
    SeedMovie("Spider-Man: Sin regreso a casa", 2021, 148),
    SeedMovie("Doctor Strange en el Multiverso de la Locura", 2022, 126),
    SeedMovie("Black Panther: Wakanda para siempre", 2022, 161),
    SeedMovie("Deadpool y Wolverine", 2024, 128),
    SeedMovie("Capitán América: Un mundo feliz", 2025, 118),
    SeedMovie("Thunderbolts*", 2025, 126),
    SeedMovie("Los Cuatro Fantásticos: Primeros pasos", 2025, 115),
)

const val DEFAULT_WHERE = "Disney+"
