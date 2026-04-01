package io.duckemu.gbc.presentation.emulator

import kotlinx.serialization.SerialName
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.duckemu.EmulatorViewModel
import io.duckemu.emulator.presentation.EmulatorScreen
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
sealed class ControllerTheme {
    abstract val backgroundColorArgb: Int
    abstract val buttonColorArgb: Int
    abstract val backgroundAlpha: Float

    @Serializable
    @SerialName("gbc")
    data class GBC(
        override val backgroundColorArgb: Int = 0xFF3978F5.toInt(),
        override val buttonColorArgb: Int = 0xFFD1D1D1.toInt(),
        override val backgroundAlpha: Float = 1f,
        val actionButtonSize: Float = 80f,
        val dpadSize: Float = 150f,
        val dpadX: Float = 30f,
        val dpadY: Float = 0f,
        val aX: Float = -10f,
        val aY: Float = -20f,
        val bX: Float = -10f,
        val bY: Float = 40f,
        val smallButtonWidth: Float = 35f,
        val startSelectX: Float = 0f,
        val startSelectY: Float = 0f
    ) : ControllerTheme() {
        val backgroundColor get() = Color(backgroundColorArgb)
        val buttonColor get() = Color(buttonColorArgb)
    }

    @Serializable
    @SerialName("gba")
    data class GBA(
        override val backgroundColorArgb: Int = 0xFF222222.toInt(),
        override val buttonColorArgb: Int = 0xFFD1D1D1.toInt(),
        override val backgroundAlpha: Float = 1f,
        val actionButtonSize: Float = 80f,
        val dpadSize: Float = 150f,
        val lButtonOffsetX: Float = 0f,
        val rButtonOffsetX: Float = 0f,
    ) : ControllerTheme()

    @Serializable
    @SerialName("n64")
    data class N64(
        override val backgroundColorArgb: Int = 0xFF111111.toInt(),
        override val buttonColorArgb: Int = 0xFFD1D1D1.toInt(),
        override val backgroundAlpha: Float = 1f,
        val analogStickSize: Float = 100f,
        val analogOffsetX: Float = 0f,
        val analogOffsetY: Float = 0f,
        val cButtonSize: Float = 40f,
        val dpadSize: Float = 100f,
    ) : ControllerTheme()
}

val GbcPurple = Color(0xFF3978F5)
val ButtonGray = Color(0xFFD1D1D1)

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
    onAction: (String) -> Unit = {},
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
            item { MenuItem("Save", Icons.Default.SaveAlt, { onAction("SAVE") }) }
            item { MenuItem("Fast Load", Icons.Default.History, { onAction("LOAD") }) }
            item { MenuItem("Audio", Icons.AutoMirrored.Filled.VolumeUp, {}) }
            item { MenuItem("Forward", Icons.Default.FastForward, {}) }

            item { MenuItem("Saves", Icons.AutoMirrored.Filled.List, { onAction("LIST_LOAD") }) }
            item { MenuItem("Capture", Icons.Default.CameraAlt, {}) }
            item { MenuItem("Gamepad", Icons.Default.Gamepad, {}) }
            item { MenuItem("Exit", Icons.AutoMirrored.Filled.ExitToApp, {}) }
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

var color = Color(0xEE001932);

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun GameBoySkin(viewModel: GameBoyViewModel) {
    val theme = viewModel.controllerTheme as? ControllerTheme.GBC ?: ControllerTheme.GBC()

    EmuSettings(viewModel, containerColor = color)

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            EmulatorScreen(viewModel)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f)
                .background(theme.backgroundColor.copy(alpha = theme.backgroundAlpha))
                .padding(bottom = 40.dp)
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = theme.dpadX.dp, y = theme.dpadY.dp)
            ) {
                DPad(
                    size = theme.dpadSize.dp,
                    color = theme.buttonColor,
                    onPress = { upDown(viewModel.inputHandler, true, it) },
                    onRelease = { upDown(viewModel.inputHandler, false, it) }
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DuckEmu",
                    fontSize = 20.sp,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                Modifier
                    .align(Alignment.CenterEnd)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ActionButton(
                        label = "B",
                        size = theme.actionButtonSize.dp,
                        color = theme.buttonColor,
                        modifier = Modifier.offset(y = theme.bY.dp, x = theme.bX.dp),
                        onPress = { viewModel.inputHandler.buttonPressed(4) },
                        onRelease = { viewModel.inputHandler.buttonRelease(4) }
                    )
                    Spacer(Modifier.width(15.dp))
                    ActionButton(
                        label = "A",
                        size = theme.actionButtonSize.dp,
                        color = theme.buttonColor,
                        modifier = Modifier.offset(y = theme.aY.dp, x = theme.aX.dp),
                        onPress = { viewModel.inputHandler.buttonPressed(5) },
                        onRelease = { viewModel.inputHandler.buttonRelease(5) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp, start = 20.dp, end = 20.dp)
            ) {
                Column(verticalArrangement = Arrangement.Center) {
                    IconButton(onClick = { viewModel.openSettingsSheet() }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF444444),
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .clip(CircleShape)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = theme.startSelectX.dp, y = theme.startSelectY.dp),
                    horizontalArrangement = Arrangement.spacedBy(30.dp)
                ) {
                    SmallRoundButton(
                        label = "SELECT",
                        width = theme.smallButtonWidth.dp,
                        color = theme.buttonColor,
                        onPress = { viewModel.inputHandler.buttonPressed(6) },
                        onRelease = { viewModel.inputHandler.buttonRelease(6) }
                    )
                    SmallRoundButton(
                        label = "START",
                        width = theme.smallButtonWidth.dp,
                        color = theme.buttonColor,
                        onPress = { viewModel.inputHandler.buttonPressed(7) },
                        onRelease = { viewModel.inputHandler.buttonRelease(7) }
                    )
                }
            }
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
    var showAction by mutableStateOf("")

    if (viewModel.showSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeSettingsSheet() },
            sheetState = sheetState,
            containerColor = containerColor,
            contentColor = Color.White
        ) {

            when (showAction) {
                "LIST_LOAD" -> SaveListContent(
                    saves = viewModel.listSaves(),
                    onSelect = { path ->
                        viewModel.loadState(path)
                        viewModel.closeSettingsSheet()
                        showAction = "DEFAULT"
                    },
                    onBack = { showAction = "DEFAULT" }
                )
                else -> MenuGridContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                        .padding(horizontal = 16.dp),
                    onClose = { viewModel.closeSettingsSheet() },
                    onAction = {
                        when (it) {
                            "SAVE" -> viewModel.saveState()
                            "LOAD" -> viewModel.loadState()
                            "LIST_LOAD" -> showAction = it
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DPad(
    size: Dp = 150.dp,
    color: Color = ButtonGray,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit
) {
    val armThickness = size * 0.33f
    val centerSize = size * 0.3f

    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size, armThickness).clip(RoundedCornerShape(8.dp)).background(color))
        Box(Modifier.size(armThickness, size).clip(RoundedCornerShape(8.dp)).background(color))
        Box(Modifier.size(centerSize).clip(CircleShape).background(color.copy(alpha = 0.8f)))

        Column(Modifier.fillMaxSize()) {
            DPadZone(Modifier.weight(1f).fillMaxWidth(), 2, onPress, onRelease)
            Row(Modifier.weight(1f).fillMaxWidth()) {
                DPadZone(Modifier.weight(1f).fillMaxHeight(), 1, onPress, onRelease)
                Spacer(Modifier.weight(1f).fillMaxHeight())
                DPadZone(Modifier.weight(1f).fillMaxHeight(), 0, onPress, onRelease)
            }
            DPadZone(Modifier.weight(1f).fillMaxWidth(), 3, onPress, onRelease)
        }
    }
}

@Composable
private fun DPadZone(
    modifier: Modifier,
    key: Int,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit
) {
    Box(
        modifier = modifier.pointerInput(key) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.any { it.pressed && !it.previousPressed }) onPress(key)
                    if (event.changes.any { !it.pressed && it.previousPressed }) onRelease(key)
                }
            }
        }
    )
}

@Composable
fun ActionButton(
    label: String,
    size: Dp = 80.dp,
    color: Color = ButtonGray,
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.pressed && !it.previousPressed }) onPress()
                        if (event.changes.any { !it.pressed && it.previousPressed }) onRelease()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = (size.value * 0.3f).sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
    }
}

@Composable
fun SmallRoundButton(
    label: String,
    width: Dp = 35.dp,
    color: Color = Color(0xFF444444),
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width, 12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.pressed && !it.previousPressed }) onPress()
                            if (event.changes.any { !it.pressed && it.previousPressed }) onRelease()
                        }
                    }
                }
        )
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}