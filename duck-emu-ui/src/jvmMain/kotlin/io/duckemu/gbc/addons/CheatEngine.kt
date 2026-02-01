package io.duckemu.gbc.addons
class Cheat {
    var code: String? = null
    var address: Int? = null
    var ifIs: Byte? = null
    var changeTo: Byte? = null
}

fun newCheat(code: String?): Cheat? {
    if (code == null) return null
    if (code.matches("^[0-9A-Fa-f]{4}:[0-9A-Fa-f]{2}$".toRegex())) {
        val cheat = Cheat()
        cheat.code = code
        cheat.address = code.substring(0, 4).toInt(16)
        cheat.changeTo = code.substring(5, 7).toInt(16).toByte()
        return cheat
    }
    if (code.matches("^[0-9A-Fa-f]{4}\\?[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}$".toRegex())) {
        val cheat = Cheat()
        cheat.code = code
        cheat.address = code.substring(0, 4).toInt(16)
        cheat.ifIs = code.substring(5, 7).toInt(16).toByte()
        cheat.changeTo = code.substring(8, 10).toInt(16).toByte()
        return cheat
    }
    if (code.matches("^01[0-9A-Fa-f]{6}$".toRegex())) {
        val cheat = Cheat()
        cheat.code = code
        cheat.address = (code.substring(6, 8) + code.substring(4, 6)).toInt(16)
        cheat.changeTo = code.substring(2, 4).toInt(16).toByte()
        return cheat
    }
    if (code.matches("^00[0-9A-Fa-f]{4}-[0-9A-Fa-f]{2}$".toRegex())) {
        val cheat = Cheat()
        cheat.address = code.substring(2, 6).toInt(16)
        cheat.changeTo = code.substring(7, 9).toInt(16).toByte()
        return cheat
    }
    return null
}
