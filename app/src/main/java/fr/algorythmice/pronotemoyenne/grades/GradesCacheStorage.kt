package fr.algorythmice.pronotemoyenne.grades

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fr.algorythmice.pronotemoyenne.pronote.PronoteUtils.NoteEntry
import fr.algorythmice.pronotemoyenne.pronote.PronoteUtils

object GradesCacheStorage {

    private const val PREF_NAME = "notes_cache_prefs"
    private const val KEY_NOTES = "cached_notes"
    private const val KEY_LAST_UPDATE = "last_update"

    fun saveNotes(context: Context, notes: Map<String, List<NoteEntry>>) {
        val json = Gson().toJson(notes)

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_NOTES, json)
            putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
        }
    }

    fun loadNotes(context: Context): Map<String, List<NoteEntry>>? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_NOTES, null) ?: return null

        val type = object : TypeToken<Map<String, List<PronoteUtils.NoteEntry>>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun getLastUpdate(context: Context): Long =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_UPDATE, 0L)
}