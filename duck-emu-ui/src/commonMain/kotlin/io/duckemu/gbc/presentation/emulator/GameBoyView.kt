package io.duckemu.gbc.presentation.emulator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TransitEnterexit
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.duckemu.Emulator
import io.duckemu.EmulatorViewModel
import io.duckemu.emulator.presentation.DuckEmuBlack
import io.duckemu.emulator.presentation.EmulatorScreen

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
            item { MenuItem("Save", Icons.Default.SaveAlt, {}) }
            item { MenuItem("Load", Icons.Default.History, {}) }
            item { MenuItem("Audio", Icons.AutoMirrored.Filled.VolumeUp, {}) }
            item { MenuItem("Forward", Icons.Default.FastForward, {}) }

            item { MenuItem("Saves", Icons.AutoMirrored.Filled.List, {}) }
            item { MenuItem("Capture", Icons.Default.CameraAlt, {}) }
            item { MenuItem("Gamepad", Icons.Default.Gamepad, {}) }
            item { MenuItem("Exit", Icons.AutoMirrored.Filled.ExitToApp, {}) }
        }
    }
}
var color = Color(0xEE001932);
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun GameBoySkin(viewModel: GameBoyViewModel) {
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = color,
            contentColor = Color.White
        ) {
            MenuGridContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .padding(horizontal = 16.dp),
                onClose = {showSheet = false}
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
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
                .background(GbcPurple)
                .padding(bottom = 40.dp)
        ) {
            Box(Modifier.align(Alignment.CenterStart).padding(start = 30.dp)) {
                DPad(
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


            Box(Modifier.align(Alignment.CenterEnd).padding(end = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ActionButton(
                        "B",
                        modifier = Modifier.offset(y = 40.dp),
                        onPress = { viewModel.inputHandler.buttonPressed(4) },
                        onRelease = { viewModel.inputHandler.buttonRelease(4) })
                    Spacer(Modifier.width(15.dp))
                    ActionButton(
                        "A",
                        modifier = Modifier.offset(y = (-20).dp),
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
                    IconButton(
                        onClick = { showSheet = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF444444),
                            modifier = Modifier.size(28.dp)
                                .background(
                                    Color.White.copy(alpha = 0.15f),
                                    CircleShape
                                ).clip(CircleShape)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(30.dp)
                ) {
                    SmallRoundButton(
                        "SELECT",
                        onPress = { viewModel.inputHandler.buttonPressed(6) },
                        onRelease = { viewModel.inputHandler.buttonRelease(6) }
                    )

                    SmallRoundButton(
                        "START",
                        onPress = { viewModel.inputHandler.buttonPressed(7) },
                        onRelease = { viewModel.inputHandler.buttonRelease(7) }
                    )
                }
            }
        }
    }
}

@Composable
fun DPad(onPress: (Int) -> Unit, onRelease: (Int) -> Unit) {
    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(150.dp, 50.dp).clip(RoundedCornerShape(8.dp)).background(ButtonGray))
        Box(Modifier.size(50.dp, 150.dp).clip(RoundedCornerShape(8.dp)).background(ButtonGray))
        Box(Modifier.size(45.dp).clip(CircleShape).background(Color(0xFFBCBCBC)))

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
                    if (event.changes.any { it.pressed && !it.previousPressed }) {
                        onPress(key)
                    }
                    if (event.changes.any { !it.pressed && it.previousPressed }) {
                        onRelease(key)
                    }
                }
            }
        }
    )
}

@Composable
fun ActionButton(
    label: String,
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(ButtonGray)
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
        Text(label, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
    }
}

@Composable
fun SmallRoundButton(
    label: String,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(35.dp, 12.dp) // GBC Select/Start are usually pills
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF444444))
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