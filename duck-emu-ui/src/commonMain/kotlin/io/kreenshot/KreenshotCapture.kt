package io.kreenshot

expect object KreenshotCapture {
    fun capture(onComplete: (ByteArray?) -> Unit)
    fun save(bytes: ByteArray, fileName: String)
}
