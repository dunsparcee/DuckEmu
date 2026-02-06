package io.duckemu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.duckemu.gbc.GameBoySkin
import io.duckemu.gbc.GameBoyViewModel
import io.duckemu.gbc.setupKeyHandler
import io.github.compose_keyhandler.KeyHandlerHost
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val gbController = remember { GameBoyViewModel() }
    val keyHandler = remember { setupKeyHandler(gbController) }
    val scope = rememberCoroutineScope()

    val launcher = rememberFilePickerLauncher { file ->
        file?.let {
            scope.launch {
                gbController.startGBC(it)
            }
        }
    }

    KeyHandlerHost(keyHandler) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

            if (gbController.isRunning) {
                GameBoySkin(gbController)
            } else {
                DuckEmuHome(onOpenRom = { launcher.launch() })
            }

            if (gbController.isRunning) {
                Text("FPS: 60", color = Color.Green, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
    }
}