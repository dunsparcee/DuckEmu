package io.duckemu.nes.presentation

import androidx.compose.ui.input.key.Key
import io.duckemu.EmulatorViewModel
import io.duckemu.nes.domain.Nes
import io.duckemu.nes.domain.ui.Renderer
import io.github.compose_keyhandler.KeyActionBuilder
import io.github.compose_keyhandler.KeyHandler
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock

object NesViewModel : EmulatorViewModel() {
    var nes: Nes? = null
    var gameLoaded = ""

    override suspend fun start(path: PlatformFile) {
        stop()
        gameLoaded = path.name
        val saveFile = FileKit.filesDir.path.plus("/${gameLoaded}.sram.sav")
        val r = Renderer()
        nes = Nes(r)
        nes!!.load(path.path, saveFile)
        startup()
        isRunning = true
    }

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun run() {
        val fps = 60
        val frameDurationMs = 500.0 / fps

        while (true) {
            val start = Clock.System.now().toEpochMilliseconds()
            graphics = nes?.execFrame()

            while (true) {
                val bufStat = nes?.renderer?.soundBufferState
                if (bufStat != null && bufStat < 0) break

                if (bufStat == 0) {
                    val elapsed = Clock.System.now().toEpochMilliseconds() - start
                    val wait = (frameDurationMs - elapsed).toLong()
                    if (wait > 0) {
                        runBlocking { delay(wait) }
                    }
                    break
                }
                runBlocking { delay(1) }
            }
        }
    }

    fun running(): Boolean {
        return job?.isActive == true
    }

    fun startup() {
        if (!running()) {
            job = scope.launch {
                run()
            }
        }
    }

    override fun toggleAudio() {

    }

    override fun controllerSetup(): KeyHandler {
        return setupKeyHandler()
    }

    override fun isEmuRunning(): Boolean {
        return isRunning
    }

    override fun stop() {
        val saveFile = FileKit.filesDir.path.plus("/${gameLoaded}.sram.sav")
        nes?.saveSram(saveFile)
        nes = null
        graphics = null
        isRunning = false
    }

    fun setupKeyHandler(): KeyHandler {
        return KeyHandler {
            onPress {
                keys(true)
            }
            onRelease {
                keys(false)
            }
        }
    }

    val keysToMap = arrayOf(
        // ─── Alphabet ───────────────────────────────────────
        Key.A, Key.B, Key.C, Key.D, Key.E, Key.F, Key.G,
        Key.H, Key.I, Key.J, Key.K, Key.L, Key.M, Key.N,
        Key.O, Key.P, Key.Q, Key.R, Key.S, Key.T, Key.U,
        Key.V, Key.W, Key.X, Key.Y, Key.Z,

        // ─── Digits (row) ───────────────────────────────────
        Key.Zero, Key.One, Key.Two, Key.Three, Key.Four,
        Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine,

        // ─── Numpad ─────────────────────────────────────────
        Key.NumPad0, Key.NumPad1, Key.NumPad2, Key.NumPad3, Key.NumPad4,
        Key.NumPad5, Key.NumPad6, Key.NumPad7, Key.NumPad8, Key.NumPad9,
        Key.NumPadDot, Key.NumPadComma,
        Key.NumPadEnter, Key.NumPadEquals,
        Key.NumPadAdd, Key.NumPadSubtract,
        Key.NumPadMultiply, Key.NumPadDivide,
        Key.NumLock,

        // ─── Function keys ──────────────────────────────────
        Key.F1, Key.F2, Key.F3, Key.F4, Key.F5, Key.F6,
        Key.F7, Key.F8, Key.F9, Key.F10, Key.F11, Key.F12,

        // ─── Modifiers ──────────────────────────────────────
        Key.ShiftLeft, Key.ShiftRight,
        Key.CtrlLeft, Key.CtrlRight,
        Key.AltLeft, Key.AltRight,
        Key.MetaLeft, Key.MetaRight,
        Key.CapsLock, Key.ScrollLock,

        // ─── Navigation / arrows ────────────────────────────
        Key.DirectionUp, Key.DirectionDown,
        Key.DirectionLeft, Key.DirectionRight,
        Key.DirectionCenter,
        Key.DirectionUpLeft, Key.DirectionUpRight,
        Key.DirectionDownLeft, Key.DirectionDownRight,
        Key.PageUp, Key.PageDown,
        Key.MoveHome, Key.MoveEnd,
        Key.Insert,

        // ─── Editing ────────────────────────────────────────
        Key.Backspace,    // ← Delete backwards
        Key.Delete,       // ← Delete forwards (Del key)
        Key.Enter,
        Key.Tab,
        Key.Escape,
        Key.Spacebar,

        // ─── Punctuation / symbols ──────────────────────────
        Key.Grave,              // `
        Key.Minus,              // -
        Key.Equals,             // =
        Key.LeftBracket,        // [
        Key.RightBracket,       // ]
        Key.Backslash,          // \
        Key.Semicolon,          // ;
        Key.Apostrophe,         // '
        Key.Slash,              // /
        Key.Comma,              // ,
        Key.Period,             // .
        Key.Plus,
        Key.At,                 // @
        Key.Pound,              // #

        // ─── System ─────────────────────────────────────────
        Key.Home, Key.Back, Key.Menu,
        Key.Search, Key.Notification,
        Key.Power, Key.Camera,
        Key.Call, Key.EndCall,
        Key.VolumeUp, Key.VolumeDown, Key.VolumeMute,
        Key.Sleep, Key.WakeUp,
        Key.Refresh, Key.PrintScreen,
        Key.Cut, Key.Copy, Key.Paste,
        Key.Bookmark, Key.Help,
        Key.Forward, Key.MediaPlay, Key.MediaStop,
        Key.MediaNext, Key.MediaPrevious,
        Key.MediaFastForward, Key.MediaRewind,
        Key.MediaRecord, Key.MediaPlayPause,
        Key.MediaClose, Key.MediaEject,
        Key.HeadsetHook,
        Key.Focus,
        Key.ZoomIn, Key.ZoomOut,
        Key.PictureSymbols, Key.SwitchCharset,
        Key.Number,
        Key.Info, Key.NavigateIn, Key.NavigateOut,
        Key.NavigatePrevious, Key.NavigateNext,
        Key.SystemNavigationUp, Key.SystemNavigationDown,
        Key.SystemNavigationLeft, Key.SystemNavigationRight,

        // ─── Gamepad / Controller ───────────────────────────
        Key.ButtonA,            // Xbox A  / south
        Key.ButtonB,            // Xbox B  / east
        Key.ButtonX,            // Xbox X  / west
        Key.ButtonY,            // Xbox Y  / north
        Key.ButtonL1,           // LB (left bumper)
        Key.ButtonR1,           // RB (right bumper)
        Key.ButtonL2,           // LT digital (left trigger pressed)
        Key.ButtonR2,           // RT digital (right trigger pressed)
        Key.ButtonThumbLeft,    // L3 (left stick click)
        Key.ButtonThumbRight,   // R3 (right stick click)
        Key.ButtonStart,        // Menu / Start
        Key.ButtonSelect,       // View / Select / Back
        Key.ButtonMode,         // Mode button

        // Generic numbered buttons (some controllers expose these):
        Key.Button1, Key.Button2, Key.Button3, Key.Button4,
        Key.Button5, Key.Button6, Key.Button7, Key.Button8,
        Key.Button9, Key.Button10, Key.Button11, Key.Button12,
        Key.Button13, Key.Button14, Key.Button15, Key.Button16,

        // ─── TV / media ─────────────────────────────────────
        Key.ProgramRed, Key.ProgramGreen,
        Key.ProgramYellow, Key.ProgramBlue,
        // (many more TV-specific keys exist: TvInputHdmi1..4,
        //  TvAudioDescription, TvNetwork, TvAntennaCable, etc.)
    )


    private fun KeyActionBuilder.keys(isPressed: Boolean) {
        keysToMap.forEach { targetKey ->
            key(targetKey) {
                upDown(isPressed, targetKey)
            }
        }
    }

    fun upDown(isPressed: Boolean, index: Key) {
        nes?.renderer?.onKey(index, isPressed)
    }

    override fun listSaves(): List<String> {
        TODO("Not yet implemented")
    }

    override fun loadState(path: String) {
    }

    override fun loadState() {
    }

    override fun saveState() {
    }
}