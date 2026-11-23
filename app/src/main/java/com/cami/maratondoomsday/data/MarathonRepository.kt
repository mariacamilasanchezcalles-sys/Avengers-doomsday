package com.cami.maratondoomsday.data

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.time.LocalDate

/**
 * Todo lo que se guarda en Firestore:
 *   marathons/{CODIGO}                 -> fecha del cine, reglas, fecha de inicio
 *   marathons/{CODIGO}/movies/{id}     -> cada película
 *   marathons/{CODIGO}/notes/{id}      -> cada nota (con el id de su película)
 *
 * Las escrituras no esperan respuesta del servidor: Firestore las guarda en el
 * celular y las sube cuando haya internet (funciona sin conexión).
 */
class MarathonRepository(private val db: FirebaseFirestore) {

    private fun marathon(code: String) = db.collection("marathons").document(code)
    private fun movies(code: String) = marathon(code).collection("movies")
    private fun notes(code: String) = marathon(code).collection("notes")

    sealed interface MarathonSnapshot {
        data object Loading : MarathonSnapshot
        data object NotFound : MarathonSnapshot
        data class Found(val info: MarathonInfo) : MarathonSnapshot
    }

    fun observeMarathon(code: String): Flow<MarathonSnapshot> = callbackFlow {
        val reg = marathon(code).addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w(TAG, "Error escuchando maratón", err)
                return@addSnapshotListener
            }
            if (snap == null) return@addSnapshotListener
            val value = when {
                snap.exists() -> MarathonSnapshot.Found(snap.toMarathon(code))
                snap.metadata.isFromCache -> MarathonSnapshot.Loading
                else -> MarathonSnapshot.NotFound
            }
            trySend(value)
        }
        awaitClose { reg.remove() }
    }

    fun observeMovies(code: String): Flow<List<Movie>> = callbackFlow {
        val reg = movies(code).addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w(TAG, "Error escuchando películas", err)
                return@addSnapshotListener
            }
            snap?.let { s -> trySend(s.documents.map { it.toMovie() }.sortedBy { it.order }) }
        }
        awaitClose { reg.remove() }
    }

    fun observeNotes(code: String): Flow<List<Note>> = callbackFlow {
        val reg = notes(code).addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w(TAG, "Error escuchando notas", err)
                return@addSnapshotListener
            }
            snap?.let { s -> trySend(s.documents.map { it.toNote() }.sortedBy { it.createdAt }) }
        }
        awaitClose { reg.remove() }
    }

    /** Crea un maratón nuevo con la lista inicial. Necesita internet. Devuelve el código. */
    suspend fun createMarathon(startDate: LocalDate): String {
        repeat(10) {
            val code = randomCode()
            val existing = marathon(code).get(Source.SERVER).await()
            if (!existing.exists()) {
                val batch = db.batch()
                batch.set(
                    marathon(code),
                    mapOf(
                        "createdAt" to FieldValue.serverTimestamp(),
                        "startDate" to startDate.toString(),
                        "cinemaDate" to null,
                        "rules" to DEFAULT_RULES,
                    )
                )
                SEED_MOVIES.forEachIndexed { i, s ->
                    batch.set(
                        movies(code).document(),
                        movieMap(s.title, s.year, s.durationMin, s.approx, DEFAULT_WHERE, (i + 1) * ORDER_STEP)
                    )
                }
                batch.commit().await()
                return code
            }
        }
        error("No se pudo generar un código")
    }

    /** Revisa en el servidor si existe el maratón. Necesita internet. */
    suspend fun exists(code: String): Boolean =
        marathon(code).get(Source.SERVER).await().exists()

    fun setWatched(code: String, movieId: String, watched: Boolean, date: LocalDate?) {
        movies(code).document(movieId)
            .update(mapOf("watched" to watched, "watchedDate" to date?.toString()))
            .logFailure()
    }

    fun setNotBefore(code: String, movieId: String, date: LocalDate?) {
        movies(code).document(movieId).update("notBefore", date?.toString()).logFailure()
    }

    fun updateMovie(code: String, movieId: String, title: String, year: Int?, durationMin: Int?, where: String) {
        movies(code).document(movieId).update(
            mapOf(
                "title" to title,
                "year" to year,
                "durationMin" to durationMin,
                "approxDuration" to false,
                "where" to where,
            )
        ).logFailure()
    }

    fun addMovie(code: String, title: String, year: Int?, durationMin: Int?, where: String, order: Long) {
        movies(code).document().set(movieMap(title, year, durationMin, false, where, order)).logFailure()
    }

    fun deleteMovie(code: String, movieId: String, noteIds: List<String>) {
        val batch = db.batch()
        batch.delete(movies(code).document(movieId))
        noteIds.forEach { batch.delete(notes(code).document(it)) }
        batch.commit().logFailure()
    }

    fun setOrders(code: String, orders: Map<String, Long>) {
        val batch = db.batch()
        orders.forEach { (id, order) -> batch.update(movies(code).document(id), "order", order) }
        batch.commit().logFailure()
    }

    fun setCinemaDate(code: String, date: LocalDate?) {
        marathon(code).update("cinemaDate", date?.toString()).logFailure()
    }

    fun setRules(code: String, rules: List<Int>) {
        marathon(code).update("rules", rules).logFailure()
    }

    fun addNote(code: String, movieId: String, text: String, author: String) {
        notes(code).document().set(
            mapOf(
                "movieId" to movieId,
                "text" to text,
                "author" to author,
                "createdAt" to System.currentTimeMillis(),
                "editedAt" to null,
            )
        ).logFailure()
    }

    fun updateNote(code: String, noteId: String, text: String) {
        notes(code).document(noteId)
            .update(mapOf("text" to text, "editedAt" to System.currentTimeMillis()))
            .logFailure()
    }

    fun deleteNote(code: String, noteId: String) {
        notes(code).document(noteId).delete().logFailure()
    }

    private fun movieMap(
        title: String, year: Int?, durationMin: Int?, approx: Boolean, where: String, order: Long,
    ) = mapOf(
        "title" to title,
        "year" to year,
        "durationMin" to durationMin,
        "approxDuration" to approx,
        "where" to where,
        "order" to order,
        "watched" to false,
        "watchedDate" to null,
        "notBefore" to null,
    )

    private fun <T> com.google.android.gms.tasks.Task<T>.logFailure() {
        addOnFailureListener { Log.w(TAG, "No se pudo guardar", it) }
    }

    companion object {
        private const val TAG = "MarathonRepo"
        const val ORDER_STEP = 1000L
        private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // sin 0/O ni 1/I
        private val random = SecureRandom()

        fun randomCode(): String = (1..6).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")

        fun normalizeCode(input: String): String =
            input.uppercase().filter { it.isLetterOrDigit() }.take(6)
    }
}

private fun String?.toDate(): LocalDate? =
    this?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

private fun DocumentSnapshot.toMarathon(code: String): MarathonInfo {
    val rules = (get("rules") as? List<*>)?.map { (it as? Number)?.toInt() ?: 0 }
        ?.takeIf { it.size == 7 } ?: DEFAULT_RULES
    return MarathonInfo(
        code = code,
        cinemaDate = getString("cinemaDate").toDate(),
        rules = rules,
        startDate = getString("startDate").toDate() ?: MARATHON_START,
    )
}

private fun DocumentSnapshot.toMovie() = Movie(
    id = id,
    title = getString("title") ?: "(sin título)",
    year = getLong("year")?.toInt(),
    durationMin = getLong("durationMin")?.toInt(),
    approxDuration = getBoolean("approxDuration") ?: false,
    where = getString("where") ?: "",
    order = getLong("order") ?: 0L,
    watched = getBoolean("watched") ?: false,
    watchedDate = getString("watchedDate").toDate(),
    notBefore = getString("notBefore").toDate(),
)

private fun DocumentSnapshot.toNote() = Note(
    id = id,
    movieId = getString("movieId") ?: "",
    text = getString("text") ?: "",
    author = getString("author") ?: "",
    createdAt = getLong("createdAt") ?: 0L,
    editedAt = getLong("editedAt"),
)
