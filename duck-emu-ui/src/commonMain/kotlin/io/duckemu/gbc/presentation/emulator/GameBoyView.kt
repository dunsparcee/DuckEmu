package io.duckemu.gbc.presentation.emulator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.duckemu.emulator.data.ControllerTheme
import io.duckemu.emulator.data.isLandscape
import io.duckemu.emulator.presentation.EmuSettings
import io.duckemu.emulator.presentation.EmulatorScreen

val GbcPurple = Color(0xFF3978F5)
val ButtonGray = Color(0xFFD1D1D1)
var color = Color(0xEE001932)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun GameBoySkin(viewModel: GameBoyViewModel) {
    val theme = viewModel.controllerTheme as? ControllerTheme.GBC ?: ControllerTheme.GBC()

    EmuSettings(viewModel, Color.Black.copy(alpha = 0.85f))

    if (isLandscape())
        GameboyLandscape(viewModel, theme)
    else
        GameBoyHandheld(viewModel, theme)
}

@Composable
private fun GameboyLandscape(
    viewModel: GameBoyViewModel,
    theme: ControllerTheme.GBC
) {
    Box(modifier = Modifier.fillMaxSize()) {
        EmulatorScreen(viewModel)

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp, bottom = 24.dp)
        ) {
            DPad(
                size = theme.dpadSize.dp,
                color = theme.buttonColor.copy(alpha = 0.4f),
                onPress = { upDown(viewModel.inputHandler, true, it) },
                onRelease = { upDown(viewModel.inputHandler, false, it) }
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp, bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionButton(
                    label = "B",
                    size = theme.actionButtonSize.dp,
                    color = theme.buttonColor.copy(alpha = 0.4f),
                    modifier = Modifier.offset(y = theme.bY.dp),
                    onPress = { viewModel.inputHandler.buttonPressed(4) },
                    onRelease = { viewModel.inputHandler.buttonRelease(4) }
                )
                Spacer(Modifier.width(15.dp))
                ActionButton(
                    label = "A",
                    size = theme.actionButtonSize.dp,
                    color = theme.buttonColor.copy(alpha = 0.4f),
                    modifier = Modifier.offset(y = theme.aY.dp),
                    onPress = { viewModel.inputHandler.buttonPressed(5) },
                    onRelease = { viewModel.inputHandler.buttonRelease(5) }
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(600.dp)
        ) {
            SmallRoundButton(
                label = "SELECT",
                width = theme.smallButtonWidth.dp,
                color = theme.buttonColor.copy(alpha = 0.4f),
                onPress = { viewModel.inputHandler.buttonPressed(6) },
                onRelease = { viewModel.inputHandler.buttonRelease(6) }
            )
            SmallRoundButton(
                label = "START",
                width = theme.smallButtonWidth.dp,
                color = theme.buttonColor.copy(alpha = 0.4f),
                onPress = { viewModel.inputHandler.buttonPressed(7) },
                onRelease = { viewModel.inputHandler.buttonRelease(7) }
            )
        }

        IconButton(
            onClick = { viewModel.openSettingsSheet() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun GameBoyHandheld(
    viewModel: GameBoyViewModel,
    theme: ControllerTheme.GBC
) {
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
fun DPadLandspace(
    size: Dp = 150.dp,
    color: Color = ButtonGray,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit
) {
    val armThickness = size * 0.33f
    val centerSize = size * 0.3f

    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        // Cross arms
        Box(
            Modifier
                .size(size, armThickness)
                .clip(RoundedCornerShape(6.dp))
                .background(color)
        )
        Box(
            Modifier
                .size(armThickness, size)
                .clip(RoundedCornerShape(6.dp))
                .background(color)
        )
        // Arrow labels
        Text("▲", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp,
            modifier = Modifier.offset(y = -(size * 0.3f)))
        Text("▼", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp,
            modifier = Modifier.offset(y = size * 0.3f))
        Text("◀", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp,
            modifier = Modifier.offset(x = -(size * 0.3f)))
        Text("▶", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp,
            modifier = Modifier.offset(x = size * 0.3f))

        // Touch zones
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