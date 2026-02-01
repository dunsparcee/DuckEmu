package io.duckemu.gbc

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmulatorScreen(controller: GameBoyViewModel) {
    Canvas(Modifier.fillMaxSize()) {
        controller.gameBoyImage?.let { image ->

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

val GbcPurple = Color(0xFF7D39F5)
val ButtonGray = Color(0xFFD1D1D1)

@Composable
fun GameBoySkin(viewModel: GameBoyViewModel) {
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

            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(30.dp)
            ) {
                SmallRoundButton(
                    "SELECT",
                    onPress = { viewModel.inputHandler.buttonPressed(6) },
                    onRelease = { viewModel.inputHandler.buttonRelease(6) })

                SmallRoundButton(
                    "START",
                    onPress = { viewModel.inputHandler.buttonPressed(7) },
                    onRelease = { viewModel.inputHandler.buttonRelease(7) })
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