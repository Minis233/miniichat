package com.miniichat.util

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream

/** Read a content:// or file:// URI and return base64 + mime + name + size. */
data class LoadedAttachment(
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val base64: String
)

object AttachmentLoader {

    fun queryNameSize(resolver: ContentResolver, uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment ?: "file"
        var size = 0L
        runCatching {
            resolver.query(uri, null, null, null, null)?.use { c ->
                val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                if (c.moveToFirst()) {
                    if (nameIdx >= 0) name = c.getString(nameIdx) ?: name
                    if (sizeIdx >= 0) size = c.getLong(sizeIdx)
                }
            }
        }
        return name to size
    }

    fun load(resolver: ContentResolver, uri: Uri, mimeFallback: String): LoadedAttachment {
        val (name, size) = queryNameSize(resolver, uri)
        val mime = resolver.getType(uri) ?: mimeFallback
        val bytes = ByteArrayOutputStream().use { out ->
            resolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Cannot open input stream for $uri" }
                input.copyTo(out)
            }
            out.toByteArray()
        }
        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return LoadedAttachment(name = name, mimeType = mime, sizeBytes = size, base64 = b64)
    }

    /** Crude byte-count formatter. */
    fun formatBytes(b: Long): String = when {
        b <= 0 -> "—"
        b < 1024 -> "${b}B"
        b < 1024 * 1024 -> "${b / 1024}KB"
        b < 1024 * 1024 * 1024 -> "${b / (1024 * 1024)}MB"
        else -> "${b / (1024 * 1024 * 1024)}GB"
    }
}
