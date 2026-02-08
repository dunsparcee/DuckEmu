package io.duckemu.gbc.presentation.emulator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
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
import coil3.PlatformContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import io.duckemu.gbc.data.game.Game

@Composable
fun GameLibraryScreen(viewModel: GameLibraryViewModel) {
    val console by viewModel.console.collectAsState()
    val gamesByConsole by viewModel.gamesByConsole.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

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

        LazyColumn(modifier = Modifier.weight(1f)) {
            console.forEach { console ->
                val gamesForDir = gamesByConsole[console] ?: emptyList()

                if (gamesForDir.isNotEmpty() || searchQuery.isBlank()) {
                    item(key = "dir_${console}") {
                        DirectoryRow(
                            path = console,
                            onClick = { }
                        )
                    }

                    items(items = gamesForDir) { game ->
                        GameCard(
                            game = game,
                            onClick = { }
                        )
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
fun GameCard(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(320.dp)
            .clickable(onClick = onClick)
            .padding(10.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            hoveredElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2D2D2D)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = game.iconPath,
                    contentDescription = game.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.None,
                    filterQuality = FilterQuality.High
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = game.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = game.fileType,
                color = Color(0xFFAAAAAA),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = game.size,
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )
            }

            // Play Time (if available)
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
fun DirectoryRow(
    path: String,
    isExpanded: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .background(Color(0xFF252525))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = path,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
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