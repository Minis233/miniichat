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

class AttachmentTooLargeException(val limitBytes: Long) :
    RuntimeException("Attachment exceeds ${limitBytes / 1024 / 1024} MB limit")

object AttachmentLoader {
    /** Hard cap to keep base64 + JSON-encoded payload under request limits and avoid OOM. */
    const val MAX_ATTACHMENT_BYTES: Long = 10L * 1024 * 1024  // 10 MB

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
        if (size > MAX_ATTACHMENT_BYTES) throw AttachmentTooLargeException(MAX_ATTACHMENT_BYTES)
        val mime = resolver.getType(uri) ?: mimeFallback

        // Stream-copy with a running size guard so we also catch attachments where
        // SIZE column isn't reported.
        val out = ByteArrayOutputStream()
        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open input stream for $uri" }
            val buf = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                total += n
                if (total > MAX_ATTACHMENT_BYTES) throw AttachmentTooLargeException(MAX_ATTACHMENT_BYTES)
                out.write(buf, 0, n)
            }
        }
        val bytes = out.toByteArray()
        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return LoadedAttachment(
            name = name,
            mimeType = mime,
            sizeBytes = if (size > 0) size else bytes.size.toLong(),
            base64 = b64
        )
    }

    /** Byte-count formatter with one decimal. */
    fun formatBytes(b: Long): String {
        if (b <= 0) return "—"
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            b < kb -> "${b}B"
            b < mb -> "%.1fKB".format(b / kb)
            b < gb -> "%.1fMB".format(b / mb)
            else -> "%.2fGB".format(b / gb)
        }
    }
}
