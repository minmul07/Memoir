package minmul.memoir.intake

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import minmul.memoir.IntakeActivity
import minmul.memoir.core.model.ItemSource

object IntakeIntents {
    const val EXTRA_ITEM_SOURCE = "minmul.memoir.extra.ITEM_SOURCE"

    fun picker(context: Context, uris: List<Uri>): Intent =
        pickerIntent(uris).setClass(context, IntakeActivity::class.java)

    fun pickerIntent(uris: List<Uri>): Intent {
        require(uris.isNotEmpty())
        return Intent().apply {
            action = pickerAction(uris.size)
            type = IMAGE_MIME
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(EXTRA_ITEM_SOURCE, ItemSource.Picker.storedValue)
            clipData =
                ClipData("images", arrayOf(IMAGE_MIME), ClipData.Item(uris.first())).also { clip ->
                    uris.drop(1).forEach { uri -> clip.addItem(ClipData.Item(uri)) }
                }
            if (uris.size == 1) {
                putExtra(Intent.EXTRA_STREAM, uris.first())
            } else {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
        }
    }

    fun pickerAction(uriCount: Int): String {
        require(uriCount > 0)
        return if (uriCount == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE
    }

    fun itemSource(intent: Intent): ItemSource =
        itemSource(intent.getStringExtra(EXTRA_ITEM_SOURCE))

    fun itemSource(stored: String?): ItemSource =
        ItemSource.entries.firstOrNull { it.storedValue == stored } ?: ItemSource.Share

    private const val IMAGE_MIME = "image/*"
}
