package fr.algorythmice.pronotemoyenne

import android.content.Context
import androidx.core.content.edit

object LoginStorage {
    private const val PREF_NAME = "pronote_prefs"
    private const val KEY_USER = "username"
    private const val KEY_PASS = "password"
    private const val KEY_ENT = "ent"
    private const val KEY_URL_PRONOTE = "url_pronote"

    fun save(context: Context, user: String, pass: String, ent: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
                putString(KEY_USER, user)
                .putString(KEY_PASS, pass)
                .putString(KEY_ENT, ent)
        }
    }

    fun saveUrlPronote(context: Context, urlPronote: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
                putString(KEY_URL_PRONOTE, urlPronote)
        }
    }

    fun getUser(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER, null)

    fun getPass(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PASS, null)

    fun getEnt(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ENT, null)

    fun getUrlPronote(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_URL_PRONOTE, null)

    fun clear(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            clear()
        }
    }
}

