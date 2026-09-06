package minmul.memoir.core.storage

import java.io.File
import java.io.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class OriginalFileStoreTest {
    @TempDir
    lateinit var filesDir: File

    @Test
    fun `writeAtomically stores bytes at the original relative path`() {
        val store = OriginalFileStore(filesDir)

        val path = store.writeAtomically("item-1") { output ->
            output.write("hello".toByteArray())
        }

        assertEquals("items/item-1/original", path)
        assertEquals("hello", File(filesDir, path).readText())
        assertFalse(File(filesDir, "items/item-1/original.tmp").exists())
    }

    @Test
    fun `writeAtomically deletes the item directory when write fails`() {
        val store = OriginalFileStore(filesDir)

        val error = runCatching {
            store.writeAtomically("item-1") { _ ->
                throw IOException("write failed")
            }
        }.exceptionOrNull()

        assertTrue(error is IOException)
        assertFalse(File(filesDir, "items/item-1").exists())
    }

    @Test
    fun `deleteItemDir removes a written original`() {
        val store = OriginalFileStore(filesDir)
        store.writeAtomically("item-1") { output ->
            output.write("hello".toByteArray())
        }

        store.deleteItemDir("item-1")

        assertFalse(File(filesDir, "items/item-1").exists())
    }
}
