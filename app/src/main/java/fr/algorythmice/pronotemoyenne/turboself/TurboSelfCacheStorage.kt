package fr.algorythmice.pronotemoyenne.turboself

import android.content.Context
import androidx.core.content.edit

object TurboSelfCacheStorage {

    private const val PREF_NAME = "turbo_self_cache_prefs"

    private const val QR_CODE_KEY = "qr_code_number"

    fun save(context: Context, qr_code_number: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putString(QR_CODE_KEY, qr_code_number)
        }
    }

    fun getQRcodeNumber(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(QR_CODE_KEY, null)
}