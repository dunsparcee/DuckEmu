package io.duckemu.emulator.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.duckemu.EmulatorViewModel
import io.duckemu.emulator.data.searchMatch
import io.duckemu.emulator.repository.config.ConfigRepository
import io.duckemu.emulator.repository.game.Game
import io.duckemu.emulator.repository.game.GameRepository
import io.duckemu.gbc.presentation.emulator.GameBoySkin
import io.duckemu.gbc.presentation.emulator.GameBoyViewModel
import io.duckemu.gbc.presentation.emulator.GbcPurple
import io.duckemu.nes.presentation.NesSkin
import io.duckemu.nes.presentation.NesViewModel
import io.github.compose_keyhandler.KeyHandlerHost
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch

val BackgroundDark = Color(0xFF1F1F1F)
val CardDark = Color(0xFF2A2A2A)

data class ConsoleEntry(
    val emulator: EmulatorViewModel,
    val mobileSkin: @Composable () -> Unit
)

object MainViewModel {
    var consoleRunning: ConsoleEntry? = null

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen(mobileDevice: Boolean = false) {
        var showSettings by remember { mutableStateOf(false) }
        val consoles: Map<String, ConsoleEntry> = remember {
            mapOf(
                "gb" to ConsoleEntry(GameBoyViewModel) { GameBoySkin(GameBoyViewModel) },
                "gbc" to ConsoleEntry(GameBoyViewModel) { GameBoySkin(GameBoyViewModel) },
                "nes" to ConsoleEntry(NesViewModel) { NesSkin(NesViewModel) }
            )
        }

        val config = remember { ConfigRepository() }
        val gameRepository = remember { GameRepository() }
        val gameLibraryViewModel = remember { GameLibraryViewModel(gameRepository, config) }
        val scope = rememberCoroutineScope()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        gameLibraryViewModel.loadConfig()
        val games by gameLibraryViewModel.gamesByConsole.collectAsState()

        val launcher = rememberFilePickerLauncher { file ->
            file?.let {
                gameLibraryViewModel.addGame(file)
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

            consoles.values.find { it.emulator.isEmuRunning() }?.let {
                KeyHandlerHost(it.emulator.controllerSetup()) {
                    Box {
                        if (it.emulator.openSettings)
                            showSettings = true

                        if (mobileDevice)
                            it.mobileSkin.invoke()
                        else
                            EmulatorScreen(it.emulator)
                    }
                }
            } ?: run {
                Box {
                    DuckEmuHome(
                        games = games,
                        onOpenRom = { launcher.launch() },
                        onSettings = { showSettings = true },
                        onGameClick = { game ->
                            scope.launch {
                                consoles[game.fileType]?.let {
                                    consoleRunning = it
                                    it.emulator.start(PlatformFile(game.path))
                                }
                            }
                        }
                    )
                }
            }

            if (showSettings) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showSettings = false
                        consoles.values.find { it.emulator.isEmuRunning() }
                            ?.emulator?.let { it.openSettings = false }
                    },
                    sheetState = sheetState,
                    containerColor = DuckEmuGray,
                    dragHandle = { },
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                    modifier = Modifier.statusBarsPadding().fillMaxWidth()
                ) {
                    DeltaSettingsScreen(onClose = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showSettings = false
                                consoles.values.find { it.emulator.isEmuRunning() }
                                    ?.emulator?.let { it.openSettings = false }
                            }
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun DuckEmuHome(
    games: Map<String, List<Game>>,
    onOpenRom: () -> Unit,
    onSettings: () -> Unit,
    onGameClick: (Game) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredGames = remember(games, searchQuery) {
        games.map { it ->
            it.value.filter { it.name.contains(searchQuery, ignoreCase = true) }
                .toMutableList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = GbcPurple,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "DuckEmu",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            IconButton(onClick = onOpenRom) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Game",
                    tint = GbcPurple,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 5.dp),
            placeholder = { Text("Search", color = Color.Gray) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search Games", tint = Color.Gray)
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF262626),
                unfocusedContainerColor = Color(0xFF262626),
                focusedTextColor = Color(0xFFE0E0E0),
                unfocusedTextColor = Color(0xFFE0E0E0),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = Color.White,
            )
        )

        Spacer(Modifier.height(8.dp))

        if (filteredGames.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No games loaded", color = Color.Gray, fontSize = 22.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap '+' to load games and start playing",
                        color = Color.DarkGray,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            val grid =
                if (LocalWindowInfo.current.containerSize.width > 600) GridCells.Fixed(5) else GridCells.Fixed(
                    2
                )
            LazyVerticalGrid(
                columns = grid,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                games.forEach { console ->
                    console.value.forEach { it ->
                        if (searchQuery.isBlank() || searchMatch(searchQuery, it.name)) {
                            item {
                                GameCard(it, { onGameClick(it) })
                            }
                        }
                    }
                }
            }
        }
    }
}