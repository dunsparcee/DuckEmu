package io.duckemu.emulator.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.duckemu.EmulatorViewModel
import io.duckemu.controller.GamepadController
import io.duckemu.emulator.data.ControllerSource
import io.duckemu.emulator.data.EmuController
import io.duckemu.emulator.data.SettingsAction
import io.duckemu.emulator.data.defaultEmuController
import io.duckemu.emulator.data.ControllerTheme
import kotlin.time.Instant

@Composable
fun EmulatorScreen(emuViewModel: EmulatorViewModel) {
    Canvas(Modifier.fillMaxSize()) {
        emuViewModel.graphics?.let { image ->

            val canvasWidth = size.width
            val canvasHeight = size.height
            val scale = minOf(canvasWidth / image.width, canvasHeight / image.height)

            val scaledWidth = image.width * scale
            val scaledHeight = image.height * scale

            val dx = (canvasWidth - scaledWidth) / 2
            val dy = (canvasHeight - scaledHeight) / 2

            drawImage(
                image = image,
                dstOffset = IntOffset(dx.toInt(), dy.toInt()),
                dstSize = IntSize(scaledWidth.toInt(), scaledHeight.toInt()),
                filterQuality = FilterQuality.None
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EmuSettings(
    viewModel: EmulatorViewModel,
    containerColor: Color = Color.Unspecified
) {
    val sheetState = rememberModalBottomSheetState()

    if (viewModel.showSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeSettingsSheet() },
            sheetState = sheetState,
            containerColor = containerColor,
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            when (viewModel.settingsAction) {
                SettingsAction.LIST_LOAD -> SaveListContent(
                    saves = viewModel.listSaves(),
                    onSelect = {
                        viewModel.loadState(it)
                        viewModel.closeSettingsSheet()
                    },
                    onBack = { viewModel.settingsAction = SettingsAction.NONE }
                )

                SettingsAction.GAMEPAD_SKIN -> SelectTheme(
                    themes = listOf(viewModel.controllerTheme!!),
                    onSelect = { viewModel.controllerTheme = it },
                    onBack = { viewModel.settingsAction = SettingsAction.NONE }
                )

                SettingsAction.CONTROLLER_CONNECTION -> ControllerSourceContent(
                    playerSources = viewModel.playerSources,
                    onPlayerSelect = { player, source -> viewModel.setPlayerSource(player, source) },
                    onBack = { viewModel.settingsAction = SettingsAction.NONE }
                )

                else -> MenuGridContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    onClose = { viewModel.closeSettingsSheet() },
                    onAction = { viewModel.onSettingsAction(it) }
                )
            }
        }
    }
}

@Composable
fun MenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 90.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun MenuGridContent(
    onClose: () -> Unit,
    onAction: (SettingsAction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MENU",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { MenuItem("Save", Icons.Default.SaveAlt, { onAction(SettingsAction.SAVE) }) }
            item { MenuItem("Fast Load", Icons.Default.History, { onAction(SettingsAction.LOAD) }) }
            item { MenuItem("Audio", Icons.AutoMirrored.Filled.VolumeUp, { onAction(SettingsAction.AUDIO) }) }
            item { MenuItem("Forward", Icons.Default.FastForward, { onAction(SettingsAction.FORWARD) }) }

            item { MenuItem("Saves", Icons.AutoMirrored.Filled.List, { onAction(SettingsAction.LIST_LOAD) }) }
            item { MenuItem("Capture", Icons.Default.CameraAlt, { onAction(SettingsAction.SCREENSHOT) }) }
            item { MenuItem("Gamepad Skin", Icons.Default.VideogameAsset, { onAction(SettingsAction.GAMEPAD_SKIN) }) }
            item { MenuItem("Controller", Icons.Default.Gamepad, { onAction(SettingsAction.CONTROLLER_CONNECTION) }) }
            item { MenuItem("Exit", Icons.AutoMirrored.Filled.ExitToApp, { onAction(SettingsAction.EXIT_GAME) }) }
        }
    }
}

@Composable
fun SaveListContent(
    saves: List<String>,
    onSelect: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Save states",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.weight(1f))
            if (saves.isNotEmpty()) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "${saves.size} saves",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        HorizontalDivider()

        if (saves.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No saves found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(saves) { index, path ->
                    val timestamp = path
                        .substringAfterLast("/")
                        .substringAfterLast(".")
                        .substringBefore(".ss")
                        .toLongOrNull()

                    val formatted = timestamp?.let {
                        Instant.fromEpochMilliseconds(it).toString()
                    } ?: path.substringAfterLast("/")

                    val isLatest = index == 0

                    OutlinedCard(
                        onClick = { onSelect(path) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.Save,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Slot ${index + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    formatted,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isLatest) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "Latest",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                            }

                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectTheme(
    themes: List<ControllerTheme>,
    onSelect: (ControllerTheme) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f) // Slightly taller to accommodate preview icons
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Controller Skins",
                style = MaterialTheme.typography.titleMedium
            )
        }

        HorizontalDivider()

        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(themes) { theme ->
                val (name, description) = when (theme) {
                    is ControllerTheme.GBC -> "Game Boy Color" to "Classic handheld layout"
                    is ControllerTheme.NES -> "Nintendo Entertainment System" to "Retro horizontal layout"
                }

                OutlinedCard(
                    onClick = { onSelect(theme) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(theme.backgroundColorArgb),
                            modifier = Modifier.size(44.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(theme.buttonColorArgb),
                                    modifier = Modifier.size(16.dp)
                                ) {}
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ControllerSourceContent(
    playerSources: Map<Int, EmuController>,
    onPlayerSelect: (player: Int, EmuController) -> Unit,
    onBack: () -> Unit
) {
    var selectedPlayer by remember { mutableStateOf<Int?>(null) }

    AnimatedContent(
        targetState = selectedPlayer,
        transitionSpec = {
            if (targetState != null) {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
            } else {
                slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
            }
        }
    ) { player ->
        if (player == null) {
            PlayerListScreen(
                playerSources = playerSources,
                onPlayerClick = { selectedPlayer = it },
                onBack = onBack
            )
        } else {
            PlayerDetailScreen(
                player = player,
                selected = playerSources[player] ?: defaultEmuController,
                onSelect = { onPlayerSelect(player, it) },
                onBack = { selectedPlayer = null }
            )
        }
    }
}


@Composable
private fun PlayerListScreen(
    playerSources: Map<Int, EmuController>,
    onPlayerClick: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Controllers", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
        }

        HorizontalDivider()

        LazyColumn {
            playerSources.entries.sortedBy { it.key }.forEach { (player, source) ->
                item(key = player) {
                    PlayerListRow(
                        player = player,
                        controller = source,
                        onClick = { onPlayerClick(player) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerListRow(
    player: Int,
    controller: EmuController,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Player $player",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            controller.source.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun PlayerDetailScreen(
    player: Int,
    selected: EmuController,
    onSelect: (EmuController) -> Unit,
    onBack: () -> Unit
) {
    val gameControllers = GamepadController.findGamepadConnected()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
            Text("Player $player", style = MaterialTheme.typography.titleMedium)
        }

        HorizontalDivider()

        LazyColumn(contentPadding = PaddingValues(vertical = 16.dp)) {
            item {
                SourceSectionLabel("THIS DEVICE")
            }
            item {
                SourceRow(
                    controller = selected,
                    selected = selected.port == defaultEmuController.port,
                    onClick = { onSelect(selected) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
            item { SourceSectionLabel("GAME CONTROLLERS") }

            if (gameControllers.isEmpty()) {
                item {
                    Text(
                        "No Connected Controllers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                }
            } else {
                items(gameControllers.entries.toList()) { (index, name) ->
                    ControllerFind(
                        controller = index,
                        name = name,
                        selected = selected.port == index,
                        onClick = { onSelect(EmuController(ControllerSource.GAMEPAD, index)) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun SourceRow(
    controller: EmuController,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = controller.source.icon,
            contentDescription = controller.source.label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            controller.source.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}


@Composable
private fun ControllerFind(
    controller: Int,
    selected: Boolean,
    onClick: () -> Unit,
    name: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = ControllerSource.GAMEPAD.icon,
            contentDescription = ControllerSource.GAMEPAD.label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "$controller - $name",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
