package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * Fix Y: Attached audio manager for all three libraries:
 * 1. درس‌ها (lessons)
 * 2. گرامر (grammar topics)
 * 3. کتاب گرامر (grammar-book chapters)
 *
 * Attached files are copied into app-private storage (context.filesDir),
 * keyed by library + item NUMBER:
 * files/lesson_audio/<library>/<number>/
 *
 * This ensures that re-importing a lesson pack (replace-by-number)
 * NEVER erases the user's attached sounds, and sounds stay attached
 * to that item number even if the item's content is replaced.
 */
object AttachedAudioRepository {

    val ALLOWED_EXTENSIONS = setOf("mp3", "wav", "m4a", "ogg")

    fun normalizeLibrary(library: String): String {
        return library.trim().lowercase()
    }

    /**
     * Returns the app-private storage directory keyed by library + item NUMBER:
     * files/lesson_audio/<library>/<number>/
     */
    fun getAudioDirectory(context: Context, library: String, itemNumber: Int): File {
        val normLib = normalizeLibrary(library)
        val dir = File(context.filesDir, "lesson_audio/$normLib/$itemNumber")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Lists all valid audio files for this item, sorted by name.
     */
    fun listSounds(context: Context, library: String, itemNumber: Int): List<File> {
        val dir = getAudioDirectory(context, library, itemNumber)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.listFiles { file ->
            file.isFile && file.extension.lowercase() in ALLOWED_EXTENSIONS
        }?.sortedBy { it.name.lowercase() }?.toList() ?: emptyList()
    }

    /**
     * Copies an audio file from a Uri (system file picker) into app-private storage.
     */
    fun attachSound(
        context: Context,
        library: String,
        itemNumber: Int,
        sourceUri: Uri,
        preferredName: String? = null
    ): File? {
        val dir = getAudioDirectory(context, library, itemNumber)
        val displayName = preferredName?.ifBlank { null }
            ?: queryFileName(context, sourceUri)
            ?: "sound_${System.currentTimeMillis()}.mp3"

        val safeName = sanitizeFileName(displayName)
        val destFile = getUniqueFile(dir, safeName)

        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0) {
                destFile
            } else {
                if (destFile.exists()) destFile.delete()
                null
            }
        } catch (e: Exception) {
            if (destFile.exists()) destFile.delete()
            null
        }
    }

    /**
     * Copies an audio file directly from a File (for testing / direct copies).
     */
    fun attachSound(
        context: Context,
        library: String,
        itemNumber: Int,
        sourceFile: File,
        preferredName: String? = null
    ): File {
        val dir = getAudioDirectory(context, library, itemNumber)
        val safeName = sanitizeFileName(preferredName?.ifBlank { null } ?: sourceFile.name)
        val destFile = getUniqueFile(dir, safeName)
        sourceFile.copyTo(destFile, overwrite = true)
        return destFile
    }

    /**
     * Deletes an attached audio file from app-private storage.
     */
    fun deleteSound(soundFile: File): Boolean {
        return try {
            if (soundFile.exists()) soundFile.delete() else true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Deletes an attached sound by file name in the specified library and item number.
     */
    fun deleteSound(context: Context, library: String, itemNumber: Int, fileName: String): Boolean {
        val dir = getAudioDirectory(context, library, itemNumber)
        val file = File(dir, fileName)
        return deleteSound(file)
    }

    fun queryFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            val name = cursor.getString(idx)
                            if (!name.isNullOrBlank()) return name
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null }
    }

    fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(Regex("[/\\\\?%*:|\"<>]"), "_").trim()
        val ext = cleaned.substringAfterLast('.', "").lowercase()
        return if (ext in ALLOWED_EXTENSIONS) {
            cleaned
        } else {
            "$cleaned.mp3"
        }
    }

    fun getUniqueFile(dir: File, name: String): File {
        var target = File(dir, name)
        if (!target.exists()) return target
        val base = name.substringBeforeLast('.', name)
        val ext = if (name.contains('.')) ".${name.substringAfterLast('.')}" else ""
        var counter = 1
        while (target.exists()) {
            target = File(dir, "${base}_$counter$ext")
            counter++
        }
        return target
    }
}
