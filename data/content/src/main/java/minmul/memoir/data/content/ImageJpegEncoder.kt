package minmul.memoir.data.content

import java.io.InputStream
import java.io.OutputStream

fun interface ImageJpegEncoder {
    fun encode(input: InputStream, output: OutputStream)
}
