package io.duckemu.nes.util

fun bit(x: Int, n: Int): Boolean {
    return ((x shr n) and 1) != 0
}