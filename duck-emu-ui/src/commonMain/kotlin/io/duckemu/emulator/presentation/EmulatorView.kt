package io.duckemu.emulator.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import io.duckemu.emulator.data.SettingsAction
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
    var showAction by mutableStateOf(SettingsAction.NONE)

    if (viewModel.showSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeSettingsSheet() },
            sheetState = sheetState,
            containerColor = containerColor,
            contentColor = Color.White
        ) {

            when (showAction) {
                SettingsAction.LIST_LOAD -> SaveListContent(
                    saves = viewModel.listSaves(),
                    onSelect = { path ->
                        viewModel.loadState(path)
                        viewModel.closeSettingsSheet()
                        showAction = SettingsAction.NONE
                    },
                    onBack = { showAction = SettingsAction.NONE }
                )
                SettingsAction.SCREENSHOT -> {
                    viewModel.closeSettingsSheet()
                    viewModel.captureAndSave()
                }
                else -> MenuGridContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                        .padding(horizontal = 16.dp),
                    onClose = { viewModel.closeSettingsSheet() },
                    onAction = {
                        when (it) {
                            SettingsAction.SAVE -> viewModel.saveState()
                            SettingsAction.LOAD -> viewModel.loadState()
                            SettingsAction.LIST_LOAD, SettingsAction.SCREENSHOT -> showAction = it
                            SettingsAction.AUDIO -> viewModel.toggleAudio()
                            SettingsAction.FORWARD -> TODO()
                            SettingsAction.CONTROLLER -> TODO()
                            SettingsAction.EXIT_GAME -> viewModel.stop()
                            else -> {}
                        }
                    }
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
            item { MenuItem("Gamepad", Icons.Default.Gamepad, { onAction(SettingsAction.CONTROLLER) }) }
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
