package io.duckemu.gbc.domain.nes

import io.duckemu.nes.core.Nes
import io.duckemu.nes.core.ui.Renderer
import java.awt.Button
import java.awt.Dialog
import java.awt.FileDialog
import java.awt.FlowLayout
import java.awt.Frame
import java.awt.Label
import java.awt.Menu
import java.awt.MenuBar
import java.awt.MenuItem
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.IOException
import javax.sound.sampled.LineUnavailableException
import kotlin.system.exitProcess

class Main internal constructor(file: String?) : Frame("Nes Emulator") {
    private var nes: Nes? = null
    private lateinit var r: Renderer

    private val nesLock = Any()

    init {
        val menuBar = MenuBar()
        setMenuBar(menuBar)

        run {
            val menu = Menu("File")
            run {
                val item = MenuItem("Open")
                item.addActionListener { onOpen() }
                menu.add(item)
            }
            run {
                val item = MenuItem("Exit")
                item.addActionListener { onExit() }
                menu.add(item)
            }
            menuBar.add(menu)
        }
        run {
            val menu = Menu("Help")
            val item = MenuItem("About")
            item.addActionListener { onAbout() }
            menu.add(item)
            menuBar.add(menu)
        }

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                onExit()
            }
        })

        initializeNes()

        if (file != null) openRom(file)

        setVisible(true)
        setVisible(false)
        setSize(
            256 + getInsets().left + getInsets().right, (240
                    + getInsets().top + getInsets().bottom)
        )
        setVisible(true)

        loop()
    }

    private fun loop() {
        val FPS = 60

        while (true) {
            synchronized(nesLock) {
                if (nes == null) {
                    continue
                }
                val start = System.nanoTime()
                nes!!.execFrame()
                while (true) {
                    val bufStat = r.soundBufferState
                    if (bufStat < 0) break
                    if (bufStat == 0) {
                        val elapsed = System.nanoTime() - start
                        val wait = (1.0 / FPS - elapsed / 1e-9).toLong()
                        try {
                            if (wait > 0) Thread.sleep(wait)
                        } catch (e: InterruptedException) {
                        }
                        break
                    }
                    try {
                        Thread.sleep(1)
                    } catch (e: InterruptedException) {
                    }
                }
            }
        }
    }

    private fun initializeNes() {
        try {
            r = Renderer()
            r.frame = this
            r.loadKey()
        } catch (e: LineUnavailableException) {
            println("Cannot initialize Renderer.")
            e.printStackTrace()
            exitProcess(0)
        }
    }

    private fun openRom(file: String) {
        synchronized(nesLock) {
            try {
                nes = Nes(r)
                nes!!.load(file)
            } catch (e: IOException) {
                println(
                    ("error: loading " + file + " ("
                            + e.message + ")")
                )
                nes = null
            }
        }
    }

    private fun onOpen() {
        val d = FileDialog(this, "Open ROM", FileDialog.LOAD)
        d.isVisible = true
        val dir = d.directory
        val file = d.getFile()
        openRom(dir + file)
    }

    private fun onExit() {
        exitProcess(0)
    }

    private inner class AboutDialog(owner: Frame?) : Dialog(owner) {
        init {
            setLayout(FlowLayout())

            add(Label("Beautiful Japanese Nes Emulator for Java"))
            add(Label("Version 0.2.0"))

            val b = Button("OK")
            b.addActionListener(object : ActionListener {
                override fun actionPerformed(e: ActionEvent?) {
                    setVisible(false)
                }
            })
            add(b)

            addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent?) {
                    setVisible(false)
                }
            })

            setTitle("About")
            setSize(270, 100)
        }
    }

    private fun onAbout() {
        val dlg: Dialog = AboutDialog(this)
        dlg.setModal(true)
        dlg.isVisible = true
    }
}