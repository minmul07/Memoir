package minmul.memoir.data.content

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.graphics.createBitmap

@Singleton
class AndroidImageJpegEncoder @Inject constructor() : ImageJpegEncoder {
    override fun encode(input: InputStream, output: OutputStream) {
        val source = ImageDecoder.createSource(ByteBuffer.wrap(input.readBytes()))
        val decoded = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        var flattened: Bitmap? = null
        try {
            val jpegSource = flattenOnWhite(decoded)
            if (jpegSource !== decoded) {
                flattened = jpegSource
            }
            check(jpegSource.compress(Bitmap.CompressFormat.JPEG, QUALITY, output)) {
                "jpeg compress failed"
            }
        } finally {
            flattened?.recycle()
            decoded.recycle()
        }
    }

    private fun flattenOnWhite(bitmap: Bitmap): Bitmap {
        if (!bitmap.hasAlpha()) {
            return bitmap
        }
        val flattened = createBitmap(bitmap.width, bitmap.height)
        val canvas = Canvas(flattened)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        return flattened
    }

    private companion object {
        const val QUALITY = 80
    }
}
