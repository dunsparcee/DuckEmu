package io.duckemu.gbc.gpu

import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt

fun interface ScreenListener {
    fun onFrameReady(image: BufferedImage?, skipFrame: Int)
}

internal class ScreenImplement(
    registers: ByteArray,
    memory: Array<ByteArray>,
    oam: ByteArray,
    gbcFeatures: Boolean,
    defaultPalette: IntArray,
    screenListener: ScreenListener,
    maxFrameSkip: Int
) : ScreenAbstract(registers, memory, oam, gbcFeatures, screenListener, maxFrameSkip) {
    private val frameBuffer: IntArray

    private val transparentImage = IntArray(0)
    private val tileImage: Array<IntArray?>
    private val tileReadState: BooleanArray
    private var tempPix: IntArray
    private var windowSourceLine = 0

    init {
        colors = defaultPalette
        gbcMask = -0x80000000
        transparentCutoff = if (gbcFeatures) 32 else 4
        tileImage = arrayOfNulls<IntArray>(tileCount * colorCount)
        tileReadState = BooleanArray(tileCount)
        tempPix = IntArray(8 * 8)
        frameBuffer = IntArray(8 * 8 * 20 * 18)
    }

    override fun addressWrite(addr: Int, data: Byte) {
        if (videoRam[addr] == data) return

        if (addr < 0x1800) {
            val tileIndex: Int = (addr shr 4) + tileOffset

            if (tileReadState[tileIndex]) {
                var r: Int = tileImage.size - tileCount + tileIndex

                do {
                    tileImage[r] = null
                    r -= tileCount
                } while (r >= 0)
                tileReadState[tileIndex] = false
            }
        }
        videoRam[addr] = data
    }

    override fun notifyScanline(line: Int) {
        if (skipping || line >= 144) {
            return
        }

        if (line == 0) {
            windowSourceLine = 0
        }

        var windowLeft: Int
        if (winEnabled && (registers[0x4A].toInt() and 0xff) <= line) {
            windowLeft = (registers[0x4B].toInt() and 0xff) - 7
            if (windowLeft > 160) windowLeft = 160
        } else windowLeft = 160

        val skippedAnything = drawBackgroundForLine(line, windowLeft, 0)

        drawSpritesForLine(line)

        if (skippedAnything) {
            drawBackgroundForLine(line, windowLeft, 0x80)
        }

        if (windowLeft < 160) windowSourceLine++

        if (line == 143) {
            updateFrameBufferImage()
        }
    }

    override fun invalidateAll(pal: Int) {
        val start: Int = pal * tileCount * 4
        val stop: Int = (pal + 1) * tileCount * 4
        for (r in start..<stop) {
            tileImage[r] = null
        }
    }

    override fun setGBCPalette(index: Int, data: Int) {
        super.setGBCPalette(index, data)
        if ((index and 0x6) == 0) {
            gbcPalette[index shr 1] = gbcPalette[index shr 1] and 0x00ffffff
        }
    }

    private fun drawSpritesForLine(line: Int) {
        if (!spritesEnabled) return

        val minSpriteY = if (doubledSprites) line - 15 else line - 7

        var priorityFlag = if (spritePriorityEnabled) 0x80 else 0

        while (priorityFlag >= 0) {
            var oamIx = 159

            while (oamIx >= 0) {
                val attributes = 0xff and this.oam[oamIx--].toInt()

                if ((attributes and 0x80) == priorityFlag || !spritePriorityEnabled) {
                    var tileNum = (0xff and this.oam[oamIx--].toInt())
                    val spriteX = (0xff and this.oam[oamIx--].toInt()) - 8
                    val spriteY = (0xff and this.oam[oamIx--].toInt()) - 16

                    val offset = line - spriteY
                    if (spriteX >= 160 || spriteY < minSpriteY || offset < 0) continue

                    if (doubledSprites) {
                        tileNum = tileNum and 0xFE
                    }

                    var spriteAttrib =
                        (attributes shr 5) and 0x03

                    if (this.gbcFeatures) {
                        spriteAttrib += 0x20 + ((attributes and 0x07) shl 2)
                        tileNum += (384 shr 3) * (attributes and 0x08)
                    } else {
                        spriteAttrib += 4 + ((attributes and 0x10) shr 2)
                    }

                    if (priorityFlag == 0x80) {
                        if (doubledSprites) {
                            if ((spriteAttrib and TILE_FLIPY) != 0) {
                                drawPartBgSprite(
                                    (tileNum or 1) - (offset shr 3),
                                    spriteX,
                                    line,
                                    offset and 7,
                                    spriteAttrib
                                )
                            } else {
                                drawPartBgSprite(
                                    (tileNum and -2) + (offset shr 3),
                                    spriteX,
                                    line,
                                    offset and 7,
                                    spriteAttrib
                                )
                            }
                        } else {
                            drawPartBgSprite(tileNum, spriteX, line, offset, spriteAttrib)
                        }
                    } else {
                        // foreground
                        if (doubledSprites) {
                            if ((spriteAttrib and TILE_FLIPY) != 0) {
                                drawPartFgSprite(
                                    (tileNum or 1) - (offset shr 3),
                                    spriteX,
                                    line,
                                    offset and 7,
                                    spriteAttrib
                                )
                            } else {
                                drawPartFgSprite(
                                    (tileNum and -2) + (offset shr 3),
                                    spriteX,
                                    line,
                                    offset and 7,
                                    spriteAttrib
                                )
                            }
                        } else {
                            drawPartFgSprite(tileNum, spriteX, line, offset, spriteAttrib)
                        }
                    }
                } else {
                    oamIx -= 3
                }
            }
            priorityFlag -= 0x80
        }
    }

    private fun drawBackgroundForLine(line: Int, windowLeft: Int, priority: Int): Boolean {
        var skippedTile = false

        val sourceY: Int = line + (this.registers[0x42].toInt() and 0xff)
        val sourceImageLine = sourceY and 7

        var tileNum: Int
        var tileX: Int = (this.registers[0x43].toInt() and 0xff) shr 3
        val memStart = (if (hiBgTileMapAddress) 0x1c00 else 0x1800) + ((sourceY and 0xf8) shl 2)

        var screenX: Int = -(this.registers[0x43].toInt() and 7)
        while (screenX < windowLeft) {
            if (bgWindowDataSelect) {
                tileNum = videoRamBanks[0][memStart + (tileX and 0x1f)].toInt() and 0xff
            } else {
                tileNum = 256 + (videoRamBanks[0][memStart + (tileX and 0x1f)].toInt())
            }

            var tileAttrib = 0

            if (this.gbcFeatures) {
                val mapAttrib: Int = videoRamBanks[1][memStart + (tileX and 0x1f)].toInt()

                if ((mapAttrib and 0x80) != priority) {
                    skippedTile = true
                    tileX++
                    screenX += 8
                    continue
                }

                tileAttrib += (mapAttrib and 0x07) shl 2
                tileAttrib += (mapAttrib shr 5) and 0x03
                tileNum += 384 * ((mapAttrib shr 3) and 0x01)
            }

            drawPartCopy(tileNum, screenX, line, sourceImageLine, tileAttrib)
            tileX++
            screenX += 8
        }

        if (windowLeft < 160) {
            val windowStartAddress = if (hiWinTileMapAddress) 0x1c00 else 0x1800

            var tileAddress: Int

            val windowSourceTileY = windowSourceLine shr 3
            val windowSourceTileLine = windowSourceLine and 7

            tileAddress = windowStartAddress + (windowSourceTileY * 32)

            screenX = windowLeft
            while (screenX < 160) {
                if (bgWindowDataSelect) {
                    tileNum = videoRamBanks[0][tileAddress].toInt() and 0xff
                } else {
                    tileNum = 256 + videoRamBanks[0][tileAddress]
                }

                var tileAttrib = 0

                if (this.gbcFeatures) {
                    val mapAttrib: Int = videoRamBanks[1][tileAddress].toInt()

                    if ((mapAttrib and 0x80) != priority) {
                        skippedTile = true
                        tileAddress++
                        screenX += 8
                        continue
                    }

                    tileAttrib += (mapAttrib and 0x07) shl 2
                    tileAttrib += (mapAttrib shr 5) and 0x03
                    tileNum += 384 * ((mapAttrib shr 3) and 0x01)
                }

                drawPartCopy(tileNum, screenX, line, windowSourceTileLine, tileAttrib)
                tileAddress++
                screenX += 8
            }
        }
        return skippedTile
    }

    private fun updateFrameBufferImage() {
        if (!lcdEnabled) {
            val buffer = frameBuffer
            for (i in buffer.indices) buffer[i] = -1
            frameBufferImage = createImage(width, height, buffer)
            return
        }
        frameBufferImage = createImage(width, height, frameBuffer)
    }

    private fun updateImage(tileIndex: Int, attribs: Int): IntArray? {
        val index: Int = tileIndex + tileCount * attribs

        val otherBank = (tileIndex >= 384)

        var offset = if (otherBank) ((tileIndex - 384) shl 4) else (tileIndex shl 4)

        val paletteStart = attribs and 0xfc

        val vram: ByteArray = if (otherBank) videoRamBanks[1] else videoRamBanks[0]
        val palette: IntArray = if (this.gbcFeatures) gbcPalette else gbPalette
        var transparent = attribs >= transparentCutoff

        var pixix = 0
        var pixixdx = 1
        var pixixdy = 0

        if ((attribs and TILE_FLIPY) != 0) {
            pixixdy = -2 * 8
            pixix = 8 * (8 - 1)
        }
        if ((attribs and TILE_FLIPX) == 0) {
            pixixdx = -1
            pixix += 8 - 1
            pixixdy += 8 * 2
        }

        var y = 8
        while (--y >= 0) {
            var num: Int = weaveLookup[vram[offset++].toInt() and 0xff] +
                    (weaveLookup[vram[offset++].toInt() and 0xff] shl 1)
            if (num != 0) transparent = false

            var x = 8
            while (--x >= 0) {
                tempPix[pixix] = palette[paletteStart + (num and 3)]
                pixix += pixixdx

                num = num shr 2
            }
            pixix += pixixdy
        }

        if (transparent) {
            tileImage[index] = transparentImage
        } else {
            tileImage[index] = tempPix
            tempPix = IntArray(8 * 8)
        }

        tileReadState[tileIndex] = true

        return tileImage[index]
    }

    private fun drawPartCopy(tileIndex: Int, x: Int, y: Int, sourceLine: Int, attribs: Int) {
        val ix: Int = tileIndex + tileCount * attribs
        var im = tileImage[ix]

        if (im == null) {
            im = updateImage(tileIndex, attribs)
        }

        var dst = x + y * 160
        var src = sourceLine * 8
        val dstEnd = if (x + 8 > 160) ((y + 1) * 160) else (dst + 8)

        if (x < 0) { // adjust left
            dst -= x
            src -= x
        }

        while (dst < dstEnd) frameBuffer[dst++] = im!![src++]
    }

    // draws one scanline of the block
    // overwrites background when source pixel is opaque
    private fun drawPartFgSprite(tileIndex: Int, x: Int, y: Int, sourceLine: Int, attribs: Int) {
        val ix: Int = tileIndex + tileCount * attribs
        var im = tileImage[ix]

        if (im == null) {
            im = updateImage(tileIndex, attribs)
        }

        if (im.contentEquals(transparentImage)) {
            return
        }

        var dst = x + y * 160
        var src = sourceLine * 8
        val dstEnd = if (x + 8 > 160) ((y + 1) * 160) else (dst + 8)

        if (x < 0) {
            dst -= x
            src -= x
        }

        while (dst < dstEnd) {
            if (im!![src] < 0)
                frameBuffer[dst] = im[src]

            dst++
            src++
        }
    }

    private fun drawPartBgSprite(tileIndex: Int, x: Int, y: Int, sourceLine: Int, attribs: Int) {
        val ix: Int = tileIndex + tileCount * attribs
        var im = tileImage[ix]

        if (im == null) {
            im = updateImage(tileIndex, attribs)
        }

        if (im.contentEquals(transparentImage)) {
            return
        }

        var dst = x + y * 160
        var src = sourceLine * 8
        val dstEnd = if (x + 8 > 160) ((y + 1) * 160) else (dst + 8)

        if (x < 0) {
            dst -= x
            src -= x
        }

        while (dst < dstEnd) {
            if (im!![src] < 0 && frameBuffer[dst] >= 0)
                frameBuffer[dst] = im[src]

            dst++
            src++
        }
    }

    private fun createImage(width: Int, height: Int, pixes: IntArray): BufferedImage {
        val bi = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val target = (bi.raster.getDataBuffer() as DataBufferInt).getData()
        System.arraycopy(pixes, 0, target, 0, target.size)
        return bi
    }
}