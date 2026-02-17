package io.duckemu.gbc.presentation.emulator

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import duckemu.duck_emu_ui.generated.resources.Res
import duckemu.duck_emu_ui.generated.resources.gb
import duckemu.duck_emu_ui.generated.resources.gba
import io.duckemu.gbc.data.game.Game
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
fun GameLibraryScreen(viewModel: GameLibraryViewModel, gameBoyViewModel: GameBoyViewModel) {
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
                                gameBoyViewModel.startGBC(file)
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
        "gbc" -> Res.drawable.gb
        "gba" -> Res.drawable.gba
        else -> Res.drawable.gb
    }

    val consoleName = when (console) {
        "gbc" -> "GameBoy Color"
        "gba" -> "GameBoy Advance"
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
        Dialog(onDismissRequest = { showDialog = false }) {
            Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "$consoleName List",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
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
    Card(
        modifier = modifier
            .width(150.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick(game) }
            .padding(10.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF2D2D2D)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = game.iconPath,
                    contentDescription = game.cropName(),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.High
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = game.cropName(),
                color = Color.DarkGray,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = game.size,
                    color = Color.DarkGray,
                    fontSize = 13.sp
                )
            }

            game.playTime?.let { playTime ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
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