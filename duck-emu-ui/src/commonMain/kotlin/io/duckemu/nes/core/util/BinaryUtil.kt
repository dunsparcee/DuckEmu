package io.duckemu.nes.core.util

fun bit(x: Int, n: Int): Boolean {
    return ((x shr n) and 1) != 0
}