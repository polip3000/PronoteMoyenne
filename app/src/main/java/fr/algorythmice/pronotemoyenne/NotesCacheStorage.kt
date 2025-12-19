package fr.algorythmice.pronotemoyenne

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object NotesCacheStorage {

    private const val PREF_NAME = "notes_cache_prefs"
    private const val KEY_NOTES = "cached_notes"
    private const val KEY_LAST_UPDATE = "last_update"

    fun saveNotes(context: Context, notes: Map<String, List<Pair<Double, Double>>>) {
        val json = Gson().toJson(notes)

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_NOTES, json)
            putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
        }
    }

    fun loadNotes(context: Context): Map<String, List<Pair<Double, Double>>>? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_NOTES, null) ?: return null

        val type = object : TypeToken<Map<String, List<Pair<Double, Double>>>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun getLastUpdate(context: Context): Long =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_UPDATE, 0L)

    fun clear(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            clear()
        }
    }

    fun getLastUpdateText(context: Context): String {
        val last = getLastUpdate(context)

        if (last == 0L) return "Jamais"

        val diffMs = System.currentTimeMillis() - last
        val diffMin = diffMs / 60000
        val diffHour = diffMin / 60
        val diffDay = diffHour / 24
        val diffMonth = diffDay / 30
        val diffYear = diffDay / 365

        return when {
            diffMin < 1 -> " à l’instant"
            diffMin < 60 -> "il y a $diffMin min"
            diffHour < 24 -> "il y a $diffHour h"
            diffDay < 30 -> "il y a $diffDay jour${if (diffDay > 1) "s" else ""}"
            diffMonth < 12 -> "il y a $diffMonth mois"
            else -> "il y a $diffYear an${if (diffYear > 1) "s" else ""}"
        }
    }
}
