package com.cami.maratondoomsday.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cami.maratondoomsday.data.MARATHON_START
import com.cami.maratondoomsday.data.MarathonInfo
import com.cami.maratondoomsday.data.MarathonRepository
import com.cami.maratondoomsday.data.MarathonRepository.MarathonSnapshot
import com.cami.maratondoomsday.data.Movie
import com.cami.maratondoomsday.data.Note
import com.cami.maratondoomsday.data.Prefs
import com.cami.maratondoomsday.data.connectivityFlow
import com.cami.maratondoomsday.domain.Scheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Una película con la fecha que le toca (si está pendiente) o en la que se vio. */
data class ScheduledMovie(val movie: Movie, val date: LocalDate?)

data class Behind(val pending: Int, val availableDays: Int)

data class MarathonUi(
    val info: MarathonInfo,
    val today: LocalDate,
    val items: List<ScheduledMovie>,
    val notesByMovie: Map<String, List<Note>>,
) {
    val total = items.size
    val watchedCount = items.count { it.movie.watched }
    val todayPending = items.filter { !it.movie.watched && it.date == today }
    val todayWatched = items.filter { it.movie.watched && it.movie.watchedDate == today }
    val nextPending = items.firstOrNull { !it.movie.watched && it.date != null && it.date > today }
    val allDone = total > 0 && watchedCount == total
    val daysToCinema: Long? = info.cinemaDate?.let { ChronoUnit.DAYS.between(today, it) }

    val behind: Behind? = run {
        val cinema = info.cinemaDate ?: return@run null
        val pending = items.filter { !it.movie.watched }
        if (pending.isEmpty()) return@run null
        val last = pending.mapNotNull { it.date }.maxOrNull()
        if (last != null && !last.isAfter(cinema)) return@run null
        val days = if (today.isAfter(cinema)) 0
        else Scheduler.availableDays(items.map { it.movie }, info.rules, today, cinema)
        Behind(pending.size, days)
    }

    fun notesFor(movieId: String) = notesByMovie[movieId].orEmpty()
}

sealed interface Screen {
    data object Setup : Screen
    data object Loading : Screen
    data object NotFound : Screen
    data object ChooseAuthor : Screen
    data class Main(val ui: MarathonUi) : Screen
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val repo = MarathonRepository(FirebaseFirestore.getInstance())
    private val auth = FirebaseAuth.getInstance()

    private val _code = MutableStateFlow(prefs.code)
    val code: StateFlow<String?> = _code.asStateFlow()

    private val _author = MutableStateFlow(prefs.author)
    val author: StateFlow<String?> = _author.asStateFlow()

    private val signedIn = MutableStateFlow(auth.currentUser != null)

    /** Mensaje de error para mostrar en pantalla (null = sin error). */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** Código recién creado, para mostrarlo en un aviso. */
    private val _createdCode = MutableStateFlow<String?>(null)
    val createdCode: StateFlow<String?> = _createdCode.asStateFlow()

    /** Indicador de conexión para el encabezado. */
    val online: StateFlow<Boolean> = connectivityFlow(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val today = flow {
        while (true) {
            emit(LocalDate.now())
            delay(30_000)
        }
    }.distinctUntilChanged()

    private val activeCode = combine(_code, signedIn) { c, s -> if (s) c else null }

    private val marathon = activeCode.flatMapLatest { c ->
        if (c == null) flowOf(MarathonSnapshot.Loading) else repo.observeMarathon(c)
    }
    private val movies = activeCode.flatMapLatest { c ->
        if (c == null) flowOf(emptyList()) else repo.observeMovies(c)
    }
    private val notes = activeCode.flatMapLatest { c ->
        if (c == null) flowOf(emptyList()) else repo.observeNotes(c)
    }

    private val ui = combine(marathon, movies, notes, today) { m, mv, n, t ->
        when (m) {
            is MarathonSnapshot.Found -> m to buildUi(m.info, mv, n, t)
            else -> m to null
        }
    }

    val screen: StateFlow<Screen> = combine(_code, _author, ui) { c, a, (snap, built) ->
        when {
            c == null -> Screen.Setup
            snap is MarathonSnapshot.NotFound -> Screen.NotFound
            built == null -> Screen.Loading
            a == null -> Screen.ChooseAuthor
            else -> Screen.Main(built)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, if (prefs.code == null) Screen.Setup else Screen.Loading)

    init {
        ensureSignedIn()
    }

    private fun buildUi(info: MarathonInfo, movies: List<Movie>, notes: List<Note>, today: LocalDate): MarathonUi {
        val sorted = movies.sortedBy { it.order }
        val dates = Scheduler.assignDates(sorted, info.rules, today, info.startDate)
        val items = sorted.map { ScheduledMovie(it, if (it.watched) it.watchedDate else dates[it.id]) }
        return MarathonUi(info, today, items, notes.groupBy { it.movieId })
    }

    fun ensureSignedIn() {
        if (auth.currentUser != null) {
            signedIn.value = true
            return
        }
        viewModelScope.launch {
            try {
                auth.signInAnonymously().await()
                signedIn.value = true
                _error.value = null
            } catch (e: Exception) {
                _error.value = "No pudimos conectarnos. Revisa que tengas internet y vuelve a intentar."
            }
        }
    }

    private suspend fun awaitSignIn() {
        if (auth.currentUser == null) auth.signInAnonymously().await()
        signedIn.value = true
    }

    fun clearError() {
        _error.value = null
    }

    // ---------- Crear / unirse / salir ----------

    fun createMarathon() {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                awaitSignIn()
                val start = maxOf(LocalDate.now(), MARATHON_START)
                val newCode = repo.createMarathon(start)
                prefs.code = newCode
                _code.value = newCode
                _createdCode.value = newCode
            } catch (e: Exception) {
                _error.value = "No se pudo crear el maratón. Necesitas internet la primera vez. Inténtalo de nuevo."
            } finally {
                _busy.value = false
            }
        }
    }

    fun joinMarathon(input: String) {
        if (_busy.value) return
        val c = MarathonRepository.normalizeCode(input)
        if (c.length != 6) {
            _error.value = "El código debe tener 6 letras o números."
            return
        }
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                awaitSignIn()
                if (repo.exists(c)) {
                    prefs.code = c
                    _code.value = c
                } else {
                    _error.value = "No encontramos ningún maratón con el código $c. Revisa que esté bien escrito."
                }
            } catch (e: Exception) {
                _error.value = "No se pudo revisar el código. Necesitas internet para unirte. Inténtalo de nuevo."
            } finally {
                _busy.value = false
            }
        }
    }

    fun dismissCreatedCode() {
        _createdCode.value = null
    }

    fun leaveMarathon() {
        prefs.code = null
        _code.value = null
    }

    fun setAuthor(name: String) {
        prefs.author = name
        _author.value = name
    }

    // ---------- Películas ----------

    private inline fun withCode(block: (String) -> Unit) {
        _code.value?.let(block)
    }

    fun markWatched(movie: Movie, date: LocalDate = LocalDate.now()) = withCode {
        repo.setWatched(it, movie.id, true, date)
    }

    fun unmarkWatched(movie: Movie) = withCode {
        repo.setWatched(it, movie.id, false, null)
    }

    /** Pospone la película al siguiente día con lugar después de [currentDate]. */
    fun moveToNextAvailableDay(movie: Movie, currentDate: LocalDate, rules: List<Int>) = withCode { c ->
        Scheduler.nextAvailableDay(currentDate, rules)?.let { repo.setNotBefore(c, movie.id, it) }
    }

    fun clearPostpone(movie: Movie) = withCode { repo.setNotBefore(it, movie.id, null) }

    fun addMovie(title: String, year: Int?, durationMin: Int?, where: String, currentMax: Long) = withCode {
        repo.addMovie(it, title, year, durationMin, where, currentMax + MarathonRepository.ORDER_STEP)
    }

    fun updateMovie(movie: Movie, title: String, year: Int?, durationMin: Int?, where: String) = withCode {
        repo.updateMovie(it, movie.id, title, year, durationMin, where)
    }

    fun deleteMovie(movie: Movie, notes: List<Note>) = withCode {
        repo.deleteMovie(it, movie.id, notes.map { n -> n.id })
    }

    /** Sube (-1) o baja (+1) una película en la lista. */
    fun moveInList(movies: List<Movie>, index: Int, delta: Int) = withCode { c ->
        val target = index + delta
        if (index !in movies.indices || target !in movies.indices) return@withCode
        val reordered = movies.toMutableList().apply { add(target, removeAt(index)) }
        val newOrders = reordered.mapIndexed { i, m -> m.id to (i + 1) * MarathonRepository.ORDER_STEP }
            .filter { (id, order) -> movies.first { it.id == id }.order != order }
            .toMap()
        if (newOrders.isNotEmpty()) repo.setOrders(c, newOrders)
    }

    // ---------- Ajustes ----------

    fun setCinemaDate(date: LocalDate?) = withCode { repo.setCinemaDate(it, date) }

    fun setRules(rules: List<Int>) = withCode { repo.setRules(it, rules) }

    // ---------- Notas ----------

    fun addNote(movieId: String, text: String) = withCode {
        val t = text.trim()
        if (t.isNotEmpty()) repo.addNote(it, movieId, t, _author.value ?: "Alguien")
    }

    fun updateNote(note: Note, text: String) = withCode {
        val t = text.trim()
        if (t.isNotEmpty()) repo.updateNote(it, note.id, t)
    }

    fun deleteNote(note: Note) = withCode { repo.deleteNote(it, note.id) }
}
