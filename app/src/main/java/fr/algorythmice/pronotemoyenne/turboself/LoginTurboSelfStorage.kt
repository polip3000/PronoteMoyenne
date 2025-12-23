package fr.algorythmice.pronotemoyenne.turboself

import android.content.Context
import androidx.core.content.edit

object LoginTurboSelfStorage {

    private const val PREF_NAME = "turboself_prefs"
    private const val KEY_USER = "username"
    private const val KEY_PASS = "password"

    fun save(context: Context, user: String, pass: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_USER, user)
                .putString(KEY_PASS, pass)
        }
    }

    fun getUser(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER, null)

    fun getPass(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PASS, null)

}