package com.cami.maratondoomsday

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cami.maratondoomsday.data.Movie
import com.cami.maratondoomsday.ui.AppViewModel
import com.cami.maratondoomsday.ui.MarathonUi
import com.cami.maratondoomsday.ui.Screen
import com.cami.maratondoomsday.ui.screens.ChooseAuthorScreen
import com.cami.maratondoomsday.ui.screens.CodeCreatedDialog
import com.cami.maratondoomsday.ui.screens.ListScreen
import com.cami.maratondoomsday.ui.screens.LoadingScreen
import com.cami.maratondoomsday.ui.screens.MissingConfigScreen
import com.cami.maratondoomsday.ui.screens.MovieDetailScreen
import com.cami.maratondoomsday.ui.screens.NotFoundScreen
import com.cami.maratondoomsday.ui.screens.SettingsScreen
import com.cami.maratondoomsday.ui.screens.SetupScreen
import com.cami.maratondoomsday.ui.screens.TodayScreen
import com.cami.maratondoomsday.ui.theme.MaratonTheme
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val firebaseReady = FirebaseApp.getApps(this).isNotEmpty()
        setContent {
            MaratonTheme {
                if (firebaseReady) AppRoot() else MissingConfigScreen()
            }
        }
    }
}

private fun shareCode(context: Context, code: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            "¡Únete a nuestro maratón rumbo a Doomsday! 🍿\nAbre la app, elige “Unirse con código” y escribe: $code",
        )
    }
    context.startActivity(Intent.createChooser(send, "Compartir código"))
}

@Composable
private fun AppRoot(vm: AppViewModel = viewModel()) {
    val screen by vm.screen.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val author by vm.author.collectAsStateWithLifecycle()
    val createdCode by vm.createdCode.collectAsStateWithLifecycle()
    val context = LocalContext.current

    when (val s = screen) {
        Screen.Setup -> SetupScreen(busy, error, onCreate = vm::createMarathon, onJoin = vm::joinMarathon)
        Screen.Loading -> LoadingScreen(error, onRetry = vm::ensureSignedIn, onLeave = vm::leaveMarathon)
        Screen.NotFound -> NotFoundScreen(onLeave = vm::leaveMarathon)
        Screen.ChooseAuthor -> ChooseAuthorScreen(onChoose = vm::setAuthor)
        is Screen.Main -> MainScreen(s.ui, author, vm)
    }

    createdCode?.let { code ->
        CodeCreatedDialog(code, onShare = { shareCode(context, code) }, onDismiss = vm::dismissCreatedCode)
    }
}

@Composable
private fun MainScreen(ui: MarathonUi, author: String?, vm: AppViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var openMovieId by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Marca como vista y ofrece "Deshacer" por si se presionó sin querer.
    val markWatched: (Movie) -> Unit = { movie ->
        vm.markWatched(movie)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(
                message = "¡Listo! “${movie.title}” quedó como vista ✓",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Long,
            )
            if (r == SnackbarResult.ActionPerformed) vm.unmarkWatched(movie)
        }
    }

    val openItem = openMovieId?.let { id -> ui.items.firstOrNull { it.movie.id == id } }
    if (openItem != null) {
        BackHandler { openMovieId = null }
        MovieDetailScreen(
            item = openItem,
            ui = ui,
            snackbar = snackbar,
            onBack = { openMovieId = null },
            onWatched = markWatched,
            onUnwatch = vm::unmarkWatched,
            onChangeWatchedDate = { m, d -> vm.markWatched(m, d) },
            onMoveNext = { m, current -> vm.moveToNextAvailableDay(m, current, ui.info.rules) },
            onClearPostpone = vm::clearPostpone,
            onEdit = vm::updateMovie,
            onDelete = { m -> vm.deleteMovie(m, ui.notesFor(m.id)); openMovieId = null },
            onAddNote = vm::addNote,
            onEditNote = vm::updateNote,
            onDeleteNote = vm::deleteNote,
        )
        return
    }

    val tabs = listOf(
        Triple("Hoy", Icons.Filled.Home, 0),
        Triple("Lista", Icons.AutoMirrored.Filled.List, 1),
        Triple("Ajustes", Icons.Filled.Settings, 2),
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                tabs.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        when (tab) {
            0 -> TodayScreen(
                ui = ui,
                contentPadding = padding,
                onWatched = markWatched,
                onOpenMovie = { openMovieId = it },
                onAddNote = vm::addNote,
                onEditNote = vm::updateNote,
                onDeleteNote = vm::deleteNote,
            )
            1 -> ListScreen(
                ui = ui,
                contentPadding = padding,
                onOpenMovie = { openMovieId = it },
                onMove = { index, delta -> vm.moveInList(ui.items.map { it.movie }, index, delta) },
                onAdd = { t, y, d, w -> vm.addMovie(t, y, d, w, ui.items.maxOfOrNull { it.movie.order } ?: 0L) },
            )
            else -> SettingsScreen(
                ui = ui,
                author = author,
                contentPadding = padding,
                onAuthor = vm::setAuthor,
                onCinemaDate = vm::setCinemaDate,
                onRules = vm::setRules,
                onShareCode = { shareCode(context, ui.info.code) },
                onLeave = vm::leaveMarathon,
            )
        }
    }
}
