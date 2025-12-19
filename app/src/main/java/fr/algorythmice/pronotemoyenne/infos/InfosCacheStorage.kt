package fr.algorythmice.pronotemoyenne.infos

import android.content.Context
import androidx.core.content.edit



object InfosCacheStorage {

    private const val PREF_NAME = "notes_cache_prefs"
    private const val KEY_CLASS_NAME = "class_name"
    private const val KEY_ESTABLISHMENT = "establishment"
    private const val KEY_STUDENT_NAME = "studentName"


    fun save(context: Context, className: String, establishment: String, studentName: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_CLASS_NAME, className)
                .putString(KEY_ESTABLISHMENT, establishment)
                .putString(KEY_STUDENT_NAME, studentName)
        }
    }

    fun getClassName(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CLASS_NAME, null)

    fun getEstablishment(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ESTABLISHMENT, null)

    fun getStudentName(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_STUDENT_NAME, null)
}