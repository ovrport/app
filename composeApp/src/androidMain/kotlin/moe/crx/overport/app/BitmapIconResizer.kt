package moe.crx.overport.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import moe.crx.overport.patches.IconResizer
import java.io.ByteArrayOutputStream

object BitmapIconResizer : IconResizer {

    override fun resize(bytes: ByteArray, width: Int, height: Int): ByteArray {
        val baos = ByteArrayOutputStream()
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            .copy(Bitmap.Config.ARGB_8888, true).apply {
                reconfigure(width, height, Bitmap.Config.ARGB_8888)
            }.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return baos.toByteArray()
    }
}