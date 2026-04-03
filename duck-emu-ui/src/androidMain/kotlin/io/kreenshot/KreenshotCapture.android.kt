package io.kreenshot

import android.app.Activity
import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.Window
import java.io.ByteArrayOutputStream
import androidx.core.graphics.createBitmap
import java.io.OutputStream
import java.lang.ref.WeakReference

actual object KreenshotCapture {
    private var activityRef: WeakReference<Activity>? = null

    fun init(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    actual fun capture(onComplete: (ByteArray?) -> Unit) {
        val activity = activityRef?.get()
        if (activity == null) {
            onComplete(null)
            return
        }

        val window = activity.window
        val decorView = window.decorView

        if (decorView.width == 0 || decorView.height == 0) {
            onComplete(null)
            return
        }

        val bitmap = createBitmap(decorView.width, decorView.height)

        val handler = Handler(Looper.getMainLooper())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PixelCopy.request(
                window,
                bitmap,
                { result ->
                    if (result == PixelCopy.SUCCESS) {
                        onComplete(bitmapToByteArray(bitmap))
                    } else {
                        onComplete(null)
                    }
                },
                handler
            )
        } else {
            val view = decorView.rootView
            view.isDrawingCacheEnabled = true
            val cacheBitmap = Bitmap.createBitmap(view.drawingCache)
            view.isDrawingCacheEnabled = false
            onComplete(bitmapToByteArray(cacheBitmap))
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    actual fun save(bytes: ByteArray, fileName: String) {
        activityRef?.get()?.let { activity ->

            val resolver = activity.contentResolver

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/Screenshots"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let { targetUri ->
                val outputStream: OutputStream? = resolver.openOutputStream(targetUri)
                outputStream?.use { it.write(bytes) }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(targetUri, contentValues, null, null)
                }
            }
        }
    }
}
