package minmul.memoir.core.design

import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

private val thumbnailDispatcher = Dispatchers.IO.limitedParallelism(2)

/** Displays a shared URI or an app-private relative image path without decoding full resolution. */
@Composable
fun ImageThumbnail(
    imagePath: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val preview = LocalInspectionMode.current
    var bitmap by remember(imagePath) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(imagePath) { mutableStateOf(false) }
    LaunchedEffect(imagePath, context, preview) {
        if (preview) return@LaunchedEffect
        try {
            bitmap = withContext(thumbnailDispatcher) {
                val uri = imagePath.toUri()
                val source = if (uri.scheme == null) {
                    ImageDecoder.createSource(File(context.filesDir, imagePath))
                } else {
                    ImageDecoder.createSource(context.contentResolver, uri)
                }
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val scale = minOf(1f, 512f / maxOf(info.size.width, info.size.height))
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1),
                    )
                }.asImageBitmap()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failed = true
        }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val image = bitmap
        when {
            preview -> Text("…")
            image != null -> Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            failed -> Text(stringResource(R.string.image_preview_failed))
            else -> CircularProgressIndicator()
        }
    }
}
