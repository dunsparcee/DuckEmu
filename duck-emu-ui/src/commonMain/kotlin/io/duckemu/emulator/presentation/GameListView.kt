package io.duckemu.emulator.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import duckemu.duck_emu_ui.generated.resources.Res
import duckemu.duck_emu_ui.generated.resources.gb
import duckemu.duck_emu_ui.generated.resources.gba
import duckemu.duck_emu_ui.generated.resources.gbc
import duckemu.duck_emu_ui.generated.resources.nes
import duckemu.duck_emu_ui.generated.resources.no_cover
import io.duckemu.EmulatorViewModel
import io.duckemu.emulator.repository.game.Game
import io.github.vinceglb.filekit.PlatformFile
import io.kamel.core.getOrNull
import io.kamel.image.asyncPainterResource
import io.ktor.http.URLBuilder
import io.ktor.http.encodeURLPathPart
import io.ktor.http.encodedPath
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
fun GameLibraryScreen(viewModel: GameLibraryViewModel, emulators: Map<String, EmulatorViewModel>) {
    val console by viewModel.console.collectAsState()
    val gamesByConsole by viewModel.gamesByConsole.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val filteredGames = remember(gamesByConsole, searchQuery) {
        gamesByConsole.map { it ->
            it.value.filter { it.name.contains(searchQuery, ignoreCase = true) }
                .toMutableList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2D2D2D))
    ) {
        SearchBar(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            console.forEach { console ->
                val games = gamesByConsole[console] ?: emptyList()

                if (games.isNotEmpty() || searchQuery.isBlank()) {
                    item(
                        key = "dir_${console}",
                    ) {
                        ConsoleCard(console, games, {
                            val file = PlatformFile(it.path)
                            scope.launch {
                                emulators[console]?.start(file)
                            }
                        })
                    }
                }
            }
        }

        AddDirectoryButton(
            onClick = { viewModel.addNewFiles() }
        )
    }
}

@Composable
private fun ConsoleCard(console: String, games: List<Game>, onSelectGame: (Game) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    val consoleImage = when (console) {
        "gb" -> Res.drawable.gb
        "gbc" -> Res.drawable.gbc
        "gba" -> Res.drawable.gba
        "nes" -> Res.drawable.nes
        else -> Res.drawable.gb
    }

    val consoleName = when (console) {
        "gb" -> "GameBoy"
        "gbc" -> "GameBoy Color"
        "gba" -> "GameBoy Advance"
        "nes" -> "Nintendo"
        else -> ""
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(25.dp))
            .clickable(onClick = { showDialog = true })
            .padding(5.dp)
    ) {
        Image(
            painter = painterResource(consoleImage),
            contentDescription = "console icon",
            modifier = Modifier.width(150.dp)
                .padding(5.dp),
            contentScale = ContentScale.Fit
        )
        Text(
            text = consoleName,
            color = Color.LightGray,
            fontSize = 25.sp,
        )
    }

    if (showDialog) {
        Dialog(
            onDismissRequest = { showDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(modifier = Modifier.fillMaxSize(0.8f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "$consoleName List",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 200.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(games) { game ->
                            GameCard(game, {
                                onSelectGame(it)
                            })
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = { showDialog = false }) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun GameCard(
    game: Game,
    onClick: (Game) -> Unit,
    modifier: Modifier = Modifier
) {
    val gameCover = remember(game.coverPath) {
        game.coverPath?.let { url ->
            URLBuilder(url).apply {
                val filename = pathSegments.last().encodeURLPathPart()
                encodedPath = encodedPath.substringBeforeLast("/") + "/$filename"
            }.buildString()
        }
    }
    val painterResource = gameCover?.let {
        asyncPainterResource(data = it)
    }

    Card(
        modifier = modifier.clickable { onClick(game) },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D))
    ) {
        Column {
            Image(
                painter = painterResource?.getOrNull() ?: painterResource(Res.drawable.no_cover),
                contentDescription = game.cropName(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = game.name,
                    color = Color.LightGray,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = game.size,
                    color = Color.LightGray,
                    fontSize = 13.sp
                )

                game.playTime?.let { playTime ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = playTime,
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AddDirectoryButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .background(Color(0xFF2D2D2D))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Add New Games",
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

@Composable
fun SearchBar(
    value: String = "",
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    color = Color(0xFF262626),
                    shape = RoundedCornerShape(6.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF404040),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = Color(0xFFE0E0E0),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                ),
                singleLine = true,
                cursorBrush = SolidColor(Color.White),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = "Search",
                            color = Color(0xFF808080),
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}