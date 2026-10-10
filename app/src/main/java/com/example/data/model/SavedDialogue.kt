package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a single line in a generated or saved dialogue.
 * Speaker A is male (Conrad/Charon), Speaker B is female (Klara/Kore).
 */
data class DialogueMakerLine(
    val speaker: String, // "A" or "B" or name
    val isSpeakerA: Boolean,
    val german: String,
    val pronunciation: String,
    val translationDari: String
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("speaker", speaker)
        put("isSpeakerA", isSpeakerA)
        put("german", german)
        put("pronunciation", pronunciation)
        put("translationDari", translationDari)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): DialogueMakerLine {
            val speaker = obj.optString("speaker", if (obj.optBoolean("isSpeakerA", true)) "A" else "B")
            val isSpeakerA = obj.optBoolean("isSpeakerA", speaker.trim().uppercase().startsWith("A"))
            return DialogueMakerLine(
                speaker = speaker,
                isSpeakerA = isSpeakerA,
                german = obj.getString("german").trim(),
                pronunciation = obj.optString("pronunciation", "").trim(),
                translationDari = obj.optString("translationDari", obj.optString("meaningDari", "")).trim()
            )
        }
    }
}

/**
 * Represents a full generated / saved dialogue.
 */
data class SavedDialogue(
    val id: String,
    val topic: String,
    val createdAtFormatted: String,
    val timestamp: Long = System.currentTimeMillis(),
    val lines: List<DialogueMakerLine>
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("topic", topic)
        put("createdAtFormatted", createdAtFormatted)
        put("timestamp", timestamp)
        val arr = JSONArray()
        lines.forEach { arr.put(it.toJsonObject()) }
        put("lines", arr)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): SavedDialogue {
            val linesArr = obj.getJSONArray("lines")
            val linesList = mutableListOf<DialogueMakerLine>()
            for (i in 0 until linesArr.length()) {
                linesList.add(DialogueMakerLine.fromJsonObject(linesArr.getJSONObject(i)))
            }
            return SavedDialogue(
                id = obj.getString("id"),
                topic = obj.getString("topic"),
                createdAtFormatted = obj.optString("createdAtFormatted", ""),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                lines = linesList
            )
        }
    }
}
