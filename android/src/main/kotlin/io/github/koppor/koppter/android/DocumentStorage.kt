package io.github.koppor.koppter.android

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import io.github.koppor.koppter.storage.TodoStorage
import java.io.FileNotFoundException

/**
 * A todo.txt file behind a document provider, such as a cloud storage app.
 * The application never sees a path, only the `content:` URI the user picked.
 */
// [impl->dsn~document-storage~1]
class DocumentStorage(private val resolver: ContentResolver, private val uri: Uri) : TodoStorage {
    override fun read(): String =
        resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: throw FileNotFoundException(uri.toString())

    // "wt": truncate, otherwise a shorter file keeps the tail of the longer one.
    override fun write(text: String) {
        val stream = resolver.openOutputStream(uri, "wt") ?: throw FileNotFoundException(uri.toString())
        stream.use { it.write(text.encodeToByteArray()) }
    }

    override fun version(): Any? {
        val columns = arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED, DocumentsContract.Document.COLUMN_SIZE)
        return resolver.query(uri, columns, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) to cursor.getLong(1) else null
        }
    }
}
