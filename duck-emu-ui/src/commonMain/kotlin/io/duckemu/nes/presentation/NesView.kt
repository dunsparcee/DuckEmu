package io.duckemu.nes.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.duckemu.emulator.presentation.EmuSettings
import io.duckemu.emulator.presentation.EmulatorScreen
import io.duckemu.emulator.data.ControllerTheme

val NesRed = Color(0xFFE60012)
val NesGray = Color(0xFF8B8B8B)
val NesDark = Color(0xFF2A2A2A)
val NesBeige = Color(0xFFD4C5A9)
val NesButtonGray = Color(0xFF555555)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NesSkin(viewModel: NesViewModel) {
    val theme = viewModel.controllerTheme as? ControllerTheme.NES ?: ControllerTheme.NES()
    EmuSettings(viewModel, Color(0xEE1A1A2E))

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
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
                .padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(NesRed)
                    .align(Alignment.TopCenter)
            )

            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DuckEmu",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )
            }

            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = theme.dpadXOffset.dp)
                    .offset(y = theme.dpadYOffset.dp)
            ) {
                NesDPad(
                    size = theme.dpadSize.dp,
                    onPress = { viewModel.upDown(true, it) },
                    onRelease = { viewModel.upDown(false, it) }
                )
            }
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
                    .offset(y = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NesActionButton(
                        label = "B",
                        size = theme.actionButtonSize.dp,
                        color = theme.buttonColor,
                        modifier = Modifier.offset(y = theme.bYOffset.dp),
                        onPress = { viewModel.upDown(true, Key.Z) },
                        onRelease = { viewModel.upDown(false, Key.Z) }
                    )
                    Spacer(Modifier.width(20.dp))
                    NesActionButton(
                        label = "A",
                        size = theme.actionButtonSize.dp,
                        color = theme.buttonColor,
                        modifier = Modifier.offset(y = theme.aYOffset.dp),
                        onPress = { viewModel.upDown(true, Key.X) },
                        onRelease = { viewModel.upDown(false, Key.X) },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp, start = 16.dp, end = 16.dp)
            ) {
                IconButton(onClick = { viewModel.openSettingsSheet() }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(26.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            .clip(CircleShape)
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NesSmallButton(
                        label = "SELECT",
                        onPress = { viewModel.upDown(true, Key.ShiftLeft) },
                        onRelease = { viewModel.upDown(false, Key.ShiftLeft) }
                    )
                    NesSmallButton(
                        label = "START",
                        onPress = { viewModel.upDown(true, Key.Enter) },
                        onRelease = { viewModel.upDown(false, Key.Enter) }
                    )
                }
            }
        }
    }
}

@Composable
fun NesDPad(onPress: (Key) -> Unit, onRelease: (Key) -> Unit, size: Dp) {
    val armThickness = size * 0.33f
    val centerCircleSize = size * 0.28f

    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size, armThickness)
                .clip(RoundedCornerShape((size.value * 0.04).dp)) // Scale corner radius too
                .background(NesDark)
        )
        Box(
            Modifier
                .size(armThickness, size)
                .clip(RoundedCornerShape((size.value * 0.04).dp))
                .background(NesDark)
        )
        Box(
            Modifier
                .size(centerCircleSize)
                .clip(CircleShape)
                .background(Color(0xFF1A1A1A))
        )

        Column(Modifier.fillMaxSize()) {
            NesDPadZone(Modifier.weight(1f).fillMaxWidth(), Key.DirectionUp, onPress, onRelease) // Up
            Row(Modifier.weight(1f).fillMaxWidth()) {
                NesDPadZone(Modifier.weight(1f).fillMaxHeight(), Key.DirectionLeft, onPress, onRelease) // Left
                Spacer(Modifier.weight(1f))
                NesDPadZone(Modifier.weight(1f).fillMaxHeight(), Key.DirectionRight, onPress, onRelease) // Right
            }
            NesDPadZone(Modifier.weight(1f).fillMaxWidth(), Key.DirectionDown, onPress, onRelease) // Down
        }
    }
}

@Composable
private fun NesDPadZone(
    modifier: Modifier,
    key: Key,
    onPress: (Key) -> Unit,
    onRelease: (Key) -> Unit
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
fun NesActionButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    size: Dp
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
        Text(label, fontSize = (size.value * 0.31).sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun NesSmallButton(
    label: String,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp, 12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(NesButtonGray)
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
        Text(label, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}