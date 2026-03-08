package io.duckemu.emulator.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val DuckEmuBlack = Color(0xFF131313)
val DuckEmuGray = Color(0xFF1C1C1C)
val DuckEmuGrayGroup = Color(0xFF1C1C1E)
val DuckEmuBlue = Color(0xFF5B9BEA)
val DuckEmuTextSecondary = Color(0xFF8E8E93)
val DuckEmuDivider = Color(0xFF38383A)

@Composable
fun DeltaSettingsScreen(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DuckEmuBlack)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Done",
                color = DuckEmuBlue,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clickable { onClose() }
                    .padding(8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            item { SectionHeader("Controllers") }
            item {
                SettingsGroup {
                    SettingsItem("Player 1", "Touch Screen")
                    SettingsItem("Player 2")
                    SettingsItem("Player 3")
                    SettingsItem("Player 4")
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item { SectionHeader("Controller Skins") }
            item {
                SettingsGroup {
                    SettingsItem("Nintendo")
                    SettingsItem("Super Nintendo")
                    SettingsItem("Nintendo 64")
                    SettingsItem("Game Boy Color")
                    SettingsItem("Game Boy Advance")
                    SettingsItem("Nintendo DS")
                }
            }

            item { Spacer(modifier = Modifier.height(50.dp)) }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = DuckEmuTextSecondary,
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DuckEmuGrayGroup),
        content = content
    )
}

@Composable
fun SettingsItem(title: String, subtitle: String? = null) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable { /* Navigate */ }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, color = Color.White, fontSize = 17.sp)

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (subtitle != null) {
                    Text(text = subtitle, color = DuckEmuTextSecondary, fontSize = 17.sp)
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF3C3C3E), // Subtle chevron color
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            thickness = 0.5.dp,
            color = DuckEmuDivider
        )
    }
}