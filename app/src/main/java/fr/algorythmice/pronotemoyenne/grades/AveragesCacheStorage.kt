package fr.algorythmice.pronotemoyenne.grades

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object AveragesCacheStorageCacheStorage {

    private const val PREF_NAME = "averages_cache_prefs"
    private const val KEY_AVERAGES = "cached_averages"

    fun saveAverages(context: Context, averages: Map<String, List<Pair<Double, Double>>>) {
        val json = Gson().toJson(averages)

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_AVERAGES, json)
        }
    }

    fun loadAverages(context: Context): Map<String, List<Pair<Double, Double>>>? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_AVERAGES, null) ?: return null

        val type = object : TypeToken<Map<String, List<Pair<Double, Double>>>>() {}.type
        return Gson().fromJson(json, type)
    }
}