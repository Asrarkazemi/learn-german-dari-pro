package com.example.data.repository

import android.content.Context
import com.example.data.model.SavedDialogue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Manages saved dialogues and their dedicated offline audio files.
 * Stored in: files/saved_dialogues/<id>/
 *   - dialogue.json
 *   - line_0.mp3 / line_0.wav
 *   - line_1.mp3 / line_1.wav ...
 */
class SavedDialoguesRepository private constructor(private val context: Context) {

    private val baseDir: File = File(context.filesDir, "saved_dialogues").apply { mkdirs() }

    private val _dialoguesFlow = MutableStateFlow<List<SavedDialogue>>(emptyList())
    val dialoguesFlow: StateFlow<List<SavedDialogue>> = _dialoguesFlow.asStateFlow()

    init {
        loadAll()
    }

    private fun loadAll() {
        val list = mutableListOf<SavedDialogue>()
        val subDirs = baseDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
        for (dir in subDirs) {
            val jsonFile = File(dir, "dialogue.json")
            if (jsonFile.exists() && jsonFile.length() > 0) {
                try {
                    val content = jsonFile.readText()
                    val obj = JSONObject(content)
                    val dialogue = SavedDialogue.fromJsonObject(obj)
                    list.add(dialogue)
                } catch (e: Exception) {
                    // Ignore corrupted item
                }
            }
        }
        _dialoguesFlow.value = list.sortedByDescending { it.timestamp }
    }

    fun getDialogueDir(id: String): File {
        return File(baseDir, id).apply { mkdirs() }
    }

    fun findAudioForLine(dialogueId: String, lineIndex: Int): File? {
        val dir = File(baseDir, dialogueId)
        if (!dir.exists()) return null
        val mp3 = File(dir, "line_$lineIndex.mp3")
        if (mp3.exists() && mp3.length() > 0) return mp3
        val wav = File(dir, "line_$lineIndex.wav")
        if (wav.exists() && wav.length() > 0) return wav
        return null
    }

    suspend fun saveDialogue(
        dialogue: SavedDialogue,
        lineAudioFiles: Map<Int, File> = emptyMap()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dir = getDialogueDir(dialogue.id)
            val jsonFile = File(dir, "dialogue.json")
            jsonFile.writeText(dialogue.toJsonObject().toString(2))

            // Copy audio files to the dialogue's folder
            for ((index, srcFile) in lineAudioFiles) {
                if (srcFile.exists() && srcFile.length() > 0) {
                    val ext = srcFile.extension.ifBlank { "mp3" }
                    val targetFile = File(dir, "line_$index.$ext")
                    srcFile.copyTo(targetFile, overwrite = true)
                }
            }

            loadAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDialogue(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = File(baseDir, id)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
            loadAll()
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        @Volatile
        private var instance: SavedDialoguesRepository? = null

        fun getInstance(context: Context): SavedDialoguesRepository {
            return instance ?: synchronized(this) {
                instance ?: SavedDialoguesRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
