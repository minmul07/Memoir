package minmul.memoir.core.storage

import java.io.File
import java.io.OutputStream
import minmul.memoir.core.model.ItemFiles

class OriginalFileStore(private val filesDir: File) {
    fun writeAtomically(itemId: String, write: (OutputStream) -> Unit): String {
        val relativePath = ItemFiles.originalRelativePath(itemId)
        val itemDir = itemDir(itemId)
        if (!itemDir.exists() && !itemDir.mkdirs() && !itemDir.isDirectory) {
            error("cannot create ${itemDir.path}")
        }
        val tmp = File(itemDir, TMP_NAME)
        val dest = File(itemDir, ORIGINAL_NAME)
        try {
            tmp.outputStream().use(write)
            if (dest.exists() && !dest.delete()) {
                error("cannot replace ${dest.path}")
            }
            if (!tmp.renameTo(dest)) {
                tmp.inputStream().use { input ->
                    dest.outputStream().use { input.copyTo(it) }
                }
                tmp.delete()
            }
            return relativePath
        } catch (error: Throwable) {
            tmp.delete()
            dest.delete()
            itemDir.deleteRecursively()
            throw error
        }
    }

    fun deleteItemDir(itemId: String) {
        itemDir(itemId).deleteRecursively()
    }

    private fun itemDir(itemId: String): File = File(filesDir, "items/$itemId")

    private companion object {
        const val ORIGINAL_NAME = "original"
        const val TMP_NAME = "original.tmp"
    }
}
