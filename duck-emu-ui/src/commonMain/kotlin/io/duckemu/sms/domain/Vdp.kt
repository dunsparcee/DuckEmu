package io.duckemu.sms.domain

import io.duckemu.sms.data.BuildSettings
import io.duckemu.sms.data.EmuState
import io.duckemu.sms.data.EmuState.h_start
import io.duckemu.sms.data.Setup
import kotlin.math.min

class Vdp
    (
    /** Emulated display  */
    private val display: IntArray?
) {
    // --------------------------------------------------------------------------------------------
    // VDP Emulation
    // --------------------------------------------------------------------------------------------
    /** Video RAM  */
    val VRAM: ByteArray

    /** Colour RAM  */
    private val CRAM: IntArray

    /** VDP Registers  */
    private val vdpreg: IntArray?

    /** Status Register  */
    private var status = 0

    /** First or Second Byte of Command Word  */
    private var firstByte = false

    /** Command Word First Byte Latch  */
    private var commandByte = 0

    /** Location in VRAM  */
    private var location = 0

    /** Store type of operation taking place  */
    private var operation = 0

    /** Buffer VRAM Reads  */
    private var readBuffer = 0

    /** Current Line Number to Render  */
    var line: Int = 0

    /** Vertical Line Interrupt Counter  */
    private var counter = 0

    /** Background Priorities  */
    private val bgPriority: BooleanArray?

    // FIX #1: spriteCol must NOT be pre-initialized here with wrong size.
    // It gets properly sized in the init block based on SMS_WIDTH.
    /** Sprite Collisions  */
    private var spriteCol: BooleanArray? = null

    /** Address of background table (32x28x2 = 0x700 bytes)  */
    private var bgt = 0

    /** As vscroll cannot be changed during the active display period  */
    private var vScrollLatch = 0

    // --------------------------------------------------------------------------------------------
    // Emulation Related
    // --------------------------------------------------------------------------------------------

    // --------------------------------------------------------------------------------------------
    // Decoded SAT Table
    // --------------------------------------------------------------------------------------------
    /** Address of sprite attribute table (256 bytes)  */
    private var sat = 0

    /** Determine whether SAT has been written to  */
    private var isSatDirty = false

    /** Decoded SAT by each scanline  */
    private val lineSprites: Array<IntArray?>? = Array(SMS_HEIGHT) { IntArray(1 + (3 * SPRITES_PER_LINE)) }

    // FIX #2: tiles and isTileDirty must NOT be pre-initialized with wrong sizes (16).
    // createCachedImages() sets them correctly to TOTAL_TILES=512 / TILE_SIZE*TILE_SIZE=64.
    // Declare with correct sizes directly so they are never wrong even before init runs.
    /** Decoded tile data  */
    private var tiles: Array<IntArray> = Array(TOTAL_TILES) { IntArray(TILE_SIZE * TILE_SIZE) }

    /** Store whether tile has been written to  */
    private var isTileDirty: BooleanArray = BooleanArray(TOTAL_TILES)

    /** Min / Max of dirty tile index  */
    private var minDirty = 0
    private var maxDirty = 0


    // --------------------------------------------------------------------------------------------
    /**
     * Vdp Constructor.
     *
     * @param display    Pointer to Generated Display
     */
    init {
        // 16K of Video RAM
        VRAM = ByteArray(0x4000)

        // Note, we don't directly emulate CRAM but actually store the converted Java palette
        // in it. Therefore the length is different to on the real GameGear where it's actually
        // 64 bytes.
        CRAM = IntArray(0x20)

        // 15 Registers, (0-10) used by SMS, but some programs write > 10
        vdpreg = IntArray(16)

        bgPriority = BooleanArray(SMS_WIDTH)

        if (Setup.VDP_SPRITE_COLLISIONS) spriteCol = BooleanArray(SMS_WIDTH)

        createCachedImages()
    }

    /**
     * Reset VDP.
     */
    fun reset() {
        generateConvertedPals()

        firstByte = true

        location = 0
        counter = 0
        status = 0
        operation = 0
        vdpreg!![0] = 0
        vdpreg[1] = 0
        vdpreg[2] = 0x0E // B1-B3 high on startup
        vdpreg[3] = 0
        vdpreg[4] = 0
        vdpreg[5] = 0x7E // B1-B6 high on startup
        vdpreg[6] = 0
        vdpreg[7] = 0
        vdpreg[8] = 0
        vdpreg[9] = 0
        vdpreg[10] = 0

        vScrollLatch = 0

        Z80.interruptLine = false

        isSatDirty = true

        minDirty = TOTAL_TILES
        maxDirty = -1
    }

    /**
     * Force full redraw of entire cache
     */
    fun forceFullRedraw() {
        bgt = (vdpreg!![2] and 0x0f and 0x01.inv()) shl 10
        minDirty = 0
        maxDirty = TOTAL_TILES - 1
        var i = isTileDirty.size
        while (i-- != 0) {
            isTileDirty[i] = true
        }

        sat = (vdpreg[5] and 0x01.inv() and 0x80.inv()) shl 7
        isSatDirty = true
    }

    val vCount: Int
        /**
         * Read Vertical Port
         *
         * @return             VCounter Value
         */
        get() {
            if (videoMode == NTSC) {
                if (line > 0xDA)  // Values from 00 to DA, then jump to D5-FF
                    return line - 6
            } else {
                if (line > 0xF2) return line - 0x39
            }

            return line
        }

    /**
     * Read VDP Control Port (0xBF)
     *
     * @return     Copy of Status Register
     */
    fun controlRead(): Int {
        // Reset flag
        firstByte = true

        // Create copy, as we'll need to clear bits of status reg
        val statuscopy = status

        // Clear b7, b6, b5 when status register read
        status = 0 // other bits never used anyway

        // Clear IRQ Line
        Z80.interruptLine = false

        return statuscopy
    }

    /**
     * Write to VDP Control Port (0xBF)
     *
     * @param  value   Value to Write
     */
    fun controlWrite(value: Int) {
        // Store First Byte of Command Word
        if (firstByte) {
            firstByte = false
            commandByte = value
            location = (location and 0x3F00) or value
        } else {
            firstByte = true
            operation = (value shr 6) and 3
            location = commandByte or ((value and 0x3F) shl 8)

            // Read value from VRAM
            if (operation == 0) {
                readBuffer = VRAM[(location++) and 0x3FFF].toInt() and 0xFF
            } else if (operation == 2) {
                val reg = (value and 0x0F)

                when (reg) {
                    0 -> if (Setup.ACCURATE_INTERRUPT_EMULATION && (status and STATUS_HINT) != 0) Z80.interruptLine =
                        (commandByte and 0x10) != 0

                    1 -> {
                        if (((status and STATUS_VINT) != 0) && (commandByte and 0x20) != 0) Z80.interruptLine = true

                        // By writing here we've updated the height of the sprites and need to update
                        // the sprites on each line
                        if ((commandByte and 3) != (vdpreg!![reg] and 3)) isSatDirty = true
                    }

                    2 ->                         // Address of Background Table in VRAM
                        bgt = (commandByte and 0x0f and 0x01.inv()) shl 10

                    5 -> {
                        val old = sat
                        // Address of Sprite Attribute Table in RAM
                        sat = (commandByte and 0x01.inv() and 0x80.inv()) shl 7

                        if (old != sat) {
                            // Should also probably update tiles here?
                            isSatDirty = true
                        }
                    }
                }
                vdpreg!![reg] = commandByte // Set reg to previous byte
            }
        }
    }

    /**
     * Read VDP Data Port (0xBE)
     *
     * @return     Buffered read from VRAM
     */
    fun dataRead(): Int // 0xBE
    {
        firstByte = true // Reset flag

        val value = readBuffer // Stores value to be returned
        readBuffer = VRAM[(location++) and 0x3FFF].toInt() and 0xFF

        return value
    }

    /**
     * Write to VDP Data Port (0xBE)
     *
     * @param  value   Value to Write
     */
    fun dataWrite(value: Int) {
        // Reset flag
        firstByte = true

        when (operation) {
            0x00, 0x01, 0x02 -> {
                val address = location and 0x3FFF
                // Check VRAM value has actually changed
                if (value != (VRAM[address].toInt() and 0xFF)) {
                    if (address >= sat && address < sat + 64)  // Don't write dirty to SAT
                        isSatDirty = true
                    else if (address >= sat + 128 && address < sat + 256) isSatDirty = true
                    else {
                        val tileIndex = address shr 5

                        // Get tile number that's being written to (divide VRAM location by 32)
                        isTileDirty[tileIndex] = true
                        if (tileIndex < minDirty) minDirty = tileIndex
                        if (tileIndex > maxDirty) maxDirty = tileIndex
                    }

                    VRAM[address] = value.toByte()
                }
            }

            0x03 -> if (EmuState.is_sms) CRAM[location and 0x1F] = SMS_PALETTE!![value and 0x3F]
            else if (EmuState.is_gg) {
                if ((location and 1) == 0)  // first byte
                    CRAM[(location and 0x3F) shr 1] = GG_PALETTE1!![value] // GG
                else CRAM[(location and 0x3F) shr 1] = CRAM[(location and 0x3F) shr 1] or GG_PALETTE2!![value and 0x0F]
            }
        }

        if (BuildSettings.ACCURATE) readBuffer = value

        location++
    }


    fun interrupts(lineno: Int) {
        if (lineno <= 192) {
            if (!Setup.ACCURATE_INTERRUPT_EMULATION && lineno == 192) status = status or STATUS_VINT

            // Counter Expired = Line Interrupt Pending
            if (counter == 0) {
                // Reload Counter
                counter = vdpreg!![10]
                status = status or STATUS_HINT
            } else counter--

            // Line Interrupts Enabled and Pending. Assert IRQ Line.
            if (((status and STATUS_HINT) != 0) && ((vdpreg!![0] and 0x10) != 0)) Z80.interruptLine = true
        } else {
            // Reload counter on every line outside active display + 1
            counter = vdpreg!![10]

            // Frame Interrupts Enabled and Pending. Assert IRQ Line.
            if (((status and STATUS_VINT) != 0) && ((vdpreg[1] and 0x20) != 0) && (lineno < 224)) Z80.interruptLine =
                true

            // Update the VSCROLL latch for the next active display period
            if (BuildSettings.ACCURATE && lineno == EmuState.no_of_scanlines - 1) vScrollLatch = vdpreg[9]
        }
    }

    fun setVBlankFlag() {
        status = status or STATUS_VINT
    }

    /**
     * Render Line of SMS/GG Display
     *
     * @param  lineno  Line Number to Render
     */
    fun drawLine(lineno: Int) {
        // ----------------------------------------------------------------------------------------
        // Check we are in the visible drawing region
        // ----------------------------------------------------------------------------------------
        if (EmuState.is_gg) {
            if (lineno < GG_Y_OFFSET || lineno >= GG_Y_OFFSET + GG_HEIGHT) return
        }

        // ----------------------------------------------------------------------------------------
        // Clear sprite collision array if enabled
        // ----------------------------------------------------------------------------------------
        if (Setup.VDP_SPRITE_COLLISIONS) {
            var i = spriteCol!!.size
            while (i-- != 0) {
                spriteCol!![i] = false
            }
        }

        // ----------------------------------------------------------------------------------------
        // Check Screen is switched on
        // ----------------------------------------------------------------------------------------
        if ((vdpreg!![1] and 0x40) != 0) {
            // ------------------------------------------------------------------------------------
            // Draw Background Layer
            // ------------------------------------------------------------------------------------
            if (maxDirty != -1) decodeTiles()

            drawBg(lineno)

            // ------------------------------------------------------------------------------------
            // Draw Sprite Layer
            // ------------------------------------------------------------------------------------
            if (isSatDirty) decodeSat()

            if (lineSprites!![lineno]!![SPRITE_COUNT] != 0) drawSprite(lineno)

            // ------------------------------------------------------------------------------------
            // Blank Leftmost Column (SMS Only)
            // ------------------------------------------------------------------------------------
            if (EmuState.is_sms && (vdpreg[0] and 0x20) != 0) {
                val colour = CRAM[16 + (vdpreg[7] and 0x0F)]
                var location = lineno shl 8

                // Don't use a loop here for speed purposes
                display!![location++] = colour
                display[location++] = colour
                display[location++] = colour
                display[location++] = colour
                display[location++] = colour
                display[location++] = colour
                display[location++] = colour
                display[location] = colour
            }
        } else {
            drawBGColour(lineno)
        }
    }

    private fun drawBg(lineno: Int) {
        // Horizontal Scroll
        var hscroll = vdpreg!![8]

        // Vertical Scroll
        val vscroll = if (BuildSettings.ACCURATE) vScrollLatch else vdpreg[9]

        // Top Two Rows Not Affected by Horizontal Scrolling (SMS Only)
        if (lineno < 16 && ((vdpreg[0] and 0x40) != 0)) hscroll = 0

        // Lock Right eight columns
        val lock = vdpreg[0] and 0x80

        // Column to start drawing at (0 - 31) [Add extra columns for GG]
        var tile_column = (32 - (hscroll shr 3)) + h_start

        // Row to start drawing at (0 - 27)
        var tile_row = (lineno + vscroll) shr 3

        if (tile_row > 27) tile_row -= 28

        // Actual y position in tile (0 - 7) (Also times by 8 here for quick access to pixel)
        var tile_y = ((lineno + (vscroll and 7)) and 7) shl 3

        // Array Position
        val rowprecal = lineno shl 8

        // Cycle through background table
        for (tx in EmuState.h_start..<EmuState.h_end) {
            val tile_props = bgt + ((tile_column and 0x1F) shl 1) + (tile_row shl 6)
            val secondbyte = VRAM[tile_props + 1].toInt() and 0xFF

            // Select Palette (Either 0 or 16)
            val pal = (secondbyte and 0x08) shl 1

            // Screen X Position
            var sx = (tx shl 3) + (hscroll and 7)

            // Do V-Flip (take into account the fact that everything is times 8)
            val pixY = if ((secondbyte and 0x04) == 0) tile_y else ((7 shl 3) - tile_y)

            // Pattern Number (0 - 512)
            val tile = tiles[(VRAM[tile_props].toInt() and 0xFF) + ((secondbyte and 0x01) shl 8)]

            // -----------------------------------------------------------------------------------
            // Plot 8 Pixel Row (No H-Flip)
            // -----------------------------------------------------------------------------------
            if ((secondbyte and 0x02) == 0) {
                var pixX = 0
                while (pixX < 8 && sx < SMS_WIDTH) {
                    val colour = tile[pixX + pixY]

                    // Set Priority Array (Sprites over/under background tile)
                    bgPriority!![sx] = ((secondbyte and 0x10) != 0) && (colour != 0)
                    display!![sx + rowprecal] = CRAM[colour + pal]
                    pixX++
                    sx++
                }
            } else {
                // -----------------------------------------------------------------------------------
                // Plot 8 Pixel Row (H-Flip)
                // -----------------------------------------------------------------------------------
                var pixX = 7
                while (pixX >= 0 && sx < SMS_WIDTH) {
                    val colour = tile[pixX + pixY]

                    // Set Priority Array (Sprites over/under background tile)
                    bgPriority!![sx] = ((secondbyte and 0x10) != 0) && (colour != 0)
                    display!![sx + rowprecal] = CRAM[colour + pal]
                    pixX--
                    sx++
                }
            }
            tile_column++

            // ------------------------------------------------------------------------------------
            // Rightmost 8 columns Not Affected by Vertical Scrolling
            // ------------------------------------------------------------------------------------
            if (lock != 0 && tx == 23) {
                tile_row = lineno shr 3
                tile_y = (lineno and 7) shl 3
            }
        }
    }

    /**
     * Render Line of Sprite Layer
     *
     * - Notes: Sprites do not wrap on the x-axis.
     *
     * @param  lineno  Line Number to Render
     */
    private fun drawSprite(lineno: Int) {
        // Reference to the sprites that should appear on this line
        val sprites = lineSprites!![lineno]

        // Number of sprites to draw on this scanline
        val count = min(SPRITES_PER_LINE, sprites!![SPRITE_COUNT])

        // Zoom Sprites (0 = off, 1 = on)
        val zoomed = vdpreg!![1] and 0x01

        val row_precal = lineno shl 8

        // Get offset into array
        var off = (count * 3)

        // Have to iterate backwards here as we've already cached tiles
        var i = count
        while (i-- != 0) {
            // Sprite Pattern Index
            // Also mask on Pattern Index from 100 - 1FFh (if reg 6 bit 3 set)
            var n = sprites[off--] or ((vdpreg[6] and 0x04) shl 6)

            // Sprite Y Position
            val y = sprites[off--]

            // Sprite X Position
            // Shift pixels left by 8 if necessary
            var x = sprites[off--] - (vdpreg[0] and 0x08)

            // Row of tile data to render (0-7)
            val tileRow = (lineno - y) shr zoomed

            // When using 8x16 sprites LSB has no effect
            if ((vdpreg[1] and 0x02) != 0) n = n and 0x01.inv()

            // Pattern Number (0 - 512)
            val tile = tiles[n + ((tileRow and 0x08) shr 3)]

            // If X Co-ordinate is negative, do a fix to draw from position 0
            var pix = 0

            if (x < 0) {
                pix = (-x)
                x = 0
            }

            // Offset into decoded tile data
            var offset = pix + ((tileRow and 7) shl 3)

            // --------------------------------------------------------------------------------
            // Plot Normal Sprites (Width = 8)
            // --------------------------------------------------------------------------------
            if (zoomed == 0) {
                while (pix < 8 && x < SMS_WIDTH) {
                    val colour = tile[offset++]

                    if (colour != 0 && !bgPriority!![x]) {
                        display!![x + row_precal] = CRAM[colour + 16]

                        // Emulate sprite collision (when two opaque pixels overlap)
                        if (Setup.VDP_SPRITE_COLLISIONS) {
                            if (!spriteCol!![x]) spriteCol!![x] = true
                            else status = status or 0x20 // Bit 5 of status flag indicates collision
                        }
                    }
                    pix++
                    x++
                }
            } else {
                // --------------------------------------------------------------------------------
                // Plot Zoomed Sprites (Width = 16)
                // --------------------------------------------------------------------------------
                while (pix < 8 && x < SMS_WIDTH) {
                    val colour = tile[offset++]

                    // Plot first pixel
                    if (colour != 0 && !bgPriority!![x]) {
                        display!![x + row_precal] = CRAM[colour + 16]

                        if (Setup.VDP_SPRITE_COLLISIONS) {
                            if (!spriteCol!![x]) spriteCol!![x] = true
                            else status = status or 0x20 // Bit 5 of status flag indicates collision
                        }
                    }

                    // Plot second pixel
                    if (colour != 0 && !bgPriority!![x + 1]) {
                        display!![x + row_precal + 1] = CRAM[colour + 16]

                        if (Setup.VDP_SPRITE_COLLISIONS) {
                            if (!spriteCol!![x + 1]) spriteCol!![x + 1] = true
                            else status = status or 0x20 // Bit 5 of status flag indicates collision
                        }
                    }
                    pix++
                    x += 2
                }
            }
        }

        // Sprite Overflow (more than 8 sprites on line)
        if (sprites[SPRITE_COUNT] >= SPRITES_PER_LINE) {
            status = status or 0x40
        }
    }


    /**
     * Draw a Line of the current Background Colour
     *
     * @param  lineno  Line Number to Render
     */
    private fun drawBGColour(lineno: Int) {
        val colour = CRAM[16 + (vdpreg!![7] and 0x0F)]
        var row_precal = lineno shl 8

        var x = SMS_WIDTH
        while (x-- != 0) {
            display!![row_precal++] = colour
        }
    }


    // --------------------------------------------------------------------------------------------
    // Generated pre-converted palettes.
    //
    // SMS and GG colours are converted to Java RGB for speed purposes
    //
    // Java: 0xAARRGGBB (4 bytes) Java colour
    //
    // SMS : 00BBGGRR   (1 byte)
    // GG  : GGGGRRRR   (1st byte)
    //       0000BBBB   (2nd byte)
    // --------------------------------------------------------------------------------------------
    private fun generateConvertedPals() {
        // FIX #3: SMS_JAVA, GG_JAVA1, GG_JAVA2 must start as null in the companion object
        // so these null checks actually trigger and fill the arrays with real colour values.
        // Pre-initializing them to empty IntArrays caused all-black video output.
        if (EmuState.is_sms && SMS_PALETTE == null) {
            SMS_PALETTE = IntArray(0x40)

            for (i in SMS_PALETTE!!.indices) {
                val r = i and 0x03
                val g = (i shr 2) and 0x03
                val b = (i shr 4) and 0x03

                SMS_PALETTE!![i] = (0xFF shl 24) or ((r * 85) shl 16) or ((g * 85) shl 8) or (b * 85)
            }
        } else if (EmuState.is_gg && GG_PALETTE1 == null) {
            GG_PALETTE1 = IntArray(0x100)
            GG_PALETTE2 = IntArray(0x10)

            // Green & Blue
            for (i in GG_PALETTE1!!.indices) {
                val g = i and 0x0F
                val b = (i shr 4) and 0x0F

                // Shift and fill with the original bitpattern
                // so %1111 becomes %11111111, %1010 becomes %10101010
                GG_PALETTE1!![i] = (0xFF shl 24) or (g shl 20) or (g shl 16) or (b shl 12) or (b shl 8)
            }

            // Red
            for (i in GG_PALETTE2!!.indices) {
                GG_PALETTE2!![i] = (i shl 4) or i
            }
        }
    }

    // --------------------------------------------------------------------------------------------
    // Decode all background tiles
    //
    // Tiles are 8x8
    //
    // Background table is a 32x28 matrix of words stored in VRAM
    //
    //  MSB          LSB
    //  ---pcvhnnnnnnnnn
    //
    // p = priority
    // c = palette
    // v = vertical flip
    // h = horizontal flip
    // n = pattern index (0 - 512)
    // --------------------------------------------------------------------------------------------
    private fun createCachedImages() {
        tiles = Array(TOTAL_TILES) { IntArray(TILE_SIZE * TILE_SIZE) }
        isTileDirty = BooleanArray(TOTAL_TILES)
    }

    private fun decodeTiles() {
        for (i in minDirty..maxDirty) {
            // Only decode tiles that have changed since the last iteration
            if (!isTileDirty[i]) continue

            // Note that we've updated the tile
            isTileDirty[i] = false

            // FIX #4: tile must NOT be nullable — tiles[i] always returns a valid IntArray.
            // The unnecessary nullable declaration (IntArray?) and !! assertions were removed.
            val tile: IntArray = tiles[i]

            var pixel_index = 0

            // 4 bytes per row, total of 32 bytes per tile
            var address = (i shl 5)

            // Plot column of 8 pixels
            for (y in 0..<TILE_SIZE) {
                val address0 = VRAM[address++].toInt() and 0xFF
                val address1 = VRAM[address++].toInt() and 0xFF
                val address2 = VRAM[address++].toInt() and 0xFF
                val address3 = VRAM[address++].toInt() and 0xFF

                // Plot row of 8 pixels
                var bit = 0x80
                while (bit != 0) {
                    var colour = 0

                    // Set Colour of Pixel (0-15)
                    if ((address0 and bit) != 0) colour = colour or 0x01
                    if ((address1 and bit) != 0) colour = colour or 0x02
                    if ((address2 and bit) != 0) colour = colour or 0x04
                    if ((address3 and bit) != 0) colour = colour or 0x08

                    tile[pixel_index++] = colour
                    bit = bit shr 1
                }
            }
        }

        // Reset min/max dirty counters
        minDirty = TOTAL_TILES
        maxDirty = -1
    }


    // --------------------------------------------------------------------------------------------
    //
    //  DECODE SAT TABLE
    //
    //   Each sprite is defined in the sprite attribute table (SAT), a 256-byte
    //   table located in VRAM. The SAT has the following layout:
    //
    //      00: yyyyyyyyyyyyyyyy
    //      10: yyyyyyyyyyyyyyyy
    //      20: yyyyyyyyyyyyyyyy
    //      30: yyyyyyyyyyyyyyyy
    //      40: ????????????????
    //      50: ????????????????
    //      60: ????????????????
    //      70: ????????????????
    //      80: xnxnxnxnxnxnxnxn
    //      90: xnxnxnxnxnxnxnxn
    //      A0: xnxnxnxnxnxnxnxn
    //      B0: xnxnxnxnxnxnxnxn
    //      C0: xnxnxnxnxnxnxnxn
    //      D0: xnxnxnxnxnxnxnxn
    //      E0: xnxnxnxnxnxnxnxn
    //      F0: xnxnxnxnxnxnxnxn
    //
    //   y = Y coordinate + 1
    //   x = X coordinate
    //   n = Pattern index
    //   ? = Unused
    // --------------------------------------------------------------------------------------------
    /**
     * Creates a list of sprites per scanline
     */
    private fun decodeSat() {
        isSatDirty = false

        // ----------------------------------------------------------------------------------------
        // Clear Existing Table
        // ----------------------------------------------------------------------------------------
        var i = lineSprites!!.size
        while (i-- != 0) {
            lineSprites[i]!![SPRITE_COUNT] = 0
        }

        // Height of Sprites (8x8 or 8x16)
        var height = if ((vdpreg!![1] and 0x02) == 0) 8 else 16

        // Enable Zoomed Sprites
        if ((vdpreg[1] and 0x01) == 0x01) {
            height = height shl 1
        }

        // ----------------------------------------------------------------------------------------
        // Search Sprite Attribute Table (64 Bytes)
        // ----------------------------------------------------------------------------------------
        for (spriteno in 0..0x3f) {
            // Sprite Y Position
            var y = VRAM[sat + spriteno].toInt() and 0xFF

            // VDP stops drawing if y == 208
            if (y == 208) {
                return
            }

            // y is actually at +1 of value
            y++

            // If off screen, draw from negative 16 onwards
            if (y > 240) {
                y -= 256
            }

            for (lineno in 0..<SMS_HEIGHT) {
                // --------------------------------------------------------------------------------
                // Does Sprite fall on this line?
                // --------------------------------------------------------------------------------
                if ((lineno >= y) && ((lineno - y) < height)) {
                    val sprites: IntArray = lineSprites[lineno]!!

                    if (sprites[SPRITE_COUNT] < SPRITES_PER_LINE) {
                        // Get offset into array
                        var off = (sprites[SPRITE_COUNT] * 3) + SPRITE_X

                        // Address of Sprite in Sprite Attribute Table
                        var address = sat + (spriteno shl 1) + 0x80

                        // Sprite X Position
                        sprites[off++] = (VRAM[address++].toInt() and 0xFF)

                        // Sprite Y Position
                        sprites[off++] = y

                        // Sprite Pattern Index
                        sprites[off++] = (VRAM[address].toInt() and 0xFF)

                        // Increment number of sprites on this scanline
                        sprites[SPRITE_COUNT]++
                    }
                }
            }
        }
    }

    var state: IntArray
        // --------------------------------------------------------------------------------------------
        get() {
            val state: IntArray = IntArray(3 + vdpreg!!.size + CRAM.size)

            state[0] = videoMode or (status shl 8) or (if (firstByte) (1 shl 16) else 0) or (commandByte shl 24)
            state[1] = location or (operation shl 16) or (readBuffer shl 24)
            state[2] = counter or (vScrollLatch shl 8) or (line shl 16)

            vdpreg!!.copyInto(state, destinationOffset = 3)
            CRAM.copyInto(state, destinationOffset = 3 + vdpreg.size)

            return state
        }
        set(state) {
            var temp = state[0]
            videoMode = temp and 0xFF
            status = (temp shr 8) and 0xFF
            firstByte = ((temp shr 16) and 0xFF) != 0
            commandByte = (temp shr 24) and 0xFF

            temp = state[1]
            location = temp and 0xFFFF
            operation = (temp shr 16) and 0xFF
            readBuffer = (temp shr 24) and 0xFF

            temp = state[2]
            counter = temp and 0xFF
            vScrollLatch = (temp shr 8) and 0xFF
            line = (temp shr 16) and 0xFFFF

            state.copyInto(vdpreg!!, destinationOffset = 0, startIndex = 3, endIndex = 3 + vdpreg.size)
            state.copyInto(CRAM, destinationOffset = 0, startIndex = 3 + vdpreg.size, endIndex = 3 + vdpreg.size + CRAM.size)

            // Force redraw of all cached tile data
            forceFullRedraw()
        }

    companion object {
        // --------------------------------------------------------------------------------------------
        // Screen Dimensions
        // --------------------------------------------------------------------------------------------
        const val NTSC: Int = 0
        const val PAL: Int = 1

        /** NTSC / PAL Emulation  */
        var videoMode: Int = NTSC

        /** X Pixels, including blanking  */
        const val SMS_X_PIXELS: Int = 342

        /** Y Pixels (NTSC), including blanking  */
        const val SMS_Y_PIXELS_NTSC: Int = 262

        /** Y Pixels (PAL), including blanking  */
        const val SMS_Y_PIXELS_PAL: Int = 313

        /** SMS Visible Screen Width  */
        const val SMS_WIDTH: Int = 256

        /** SMS Visible Screen Height  */
        const val SMS_HEIGHT: Int = 192

        /** GG Visible Screen Width  */
        const val GG_WIDTH: Int = 160

        /** GG Visible Screen Height  */
        const val GG_HEIGHT: Int = 144

        /** GG Visible Window Starts Here (x)  */
        const val GG_X_OFFSET: Int = 48

        /** GG Window Starts Here (y)  */
        const val GG_Y_OFFSET: Int = 24

        private const val STATUS_VINT = 0x80 // Frame Interrupt Pending
        private const val STATUS_OVERFLOW = 0x40 // Sprite Overflow
        private const val STATUS_COLLISION = 0x20 // Sprite Collision
        private const val STATUS_HINT = 0x04 // Line interrupt Pending

        /** This would be different in 224 line mode  */
        private val BGT_LENGTH = 32 * 28 * 2

        // FIX #3 (companion side): These must be null so generateConvertedPals() actually
        // populates them with real colour data. Pre-initializing to empty IntArrays
        // caused all colours to be zero (black screen).
        /** SMS Colours  */
        private var SMS_PALETTE: IntArray? = null

        /** GG Colours  */
        private var GG_PALETTE1: IntArray? = null
        private var GG_PALETTE2: IntArray? = null


        /** Max number of sprites hardware can handle per scanline  */
        private const val SPRITES_PER_LINE = 8

        /** References into lineSprites table  */
        private const val SPRITE_COUNT = 0 // Number of sprites on line

        private const val SPRITE_X = 1 // Sprite X Position
        private const val SPRITE_Y = 2 // Sprite Y Position
        private const val SPRITE_N = 3 // Sprite Pattern

        // --------------------------------------------------------------------------------------------
        // Decoded Tiles
        // --------------------------------------------------------------------------------------------
        /** Total number of tiles in VRAM  */
        private const val TOTAL_TILES = 512

        /** Tile size  */
        private const val TILE_SIZE = 8
    }
}