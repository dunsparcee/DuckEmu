package io.duckemu.emulator.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import duckemu.duck_emu_ui.generated.resources.Res
import duckemu.duck_emu_ui.generated.resources.no_cover
import io.duckemu.emulator.repository.game.Game
import io.kamel.core.getOrNull
import io.kamel.image.asyncPainterResource
import io.ktor.http.*
import org.jetbrains.compose.resources.painterResource

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
                    .aspectRatio(0.75f),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = game.name,
                    color = Color.LightGray,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    minLines = 2,
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