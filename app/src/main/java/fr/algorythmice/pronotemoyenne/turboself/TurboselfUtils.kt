package fr.algorythmice.pronotemoyenne.turboself

import android.content.Context
import android.util.Log
import fr.algorythmice.pronotemoyenne.Utils.isTurboSelfLoginComplete
import kotlinx.coroutines.runBlocking
import fr.algorythmice.turboselfapi.TurbApi

object TurboselfUtils {

    data class FetchQRcodeResult(
        val qrcode: String = "",
        val error: String? = null
    )

    fun fetchQRCode(context: Context): FetchQRcodeResult {
        val user = LoginTurboSelfStorage.getUser(context)
        val pass = LoginTurboSelfStorage.getPass(context)

        if (!isTurboSelfLoginComplete(user, pass)) {
            return FetchQRcodeResult(error = "Identifiants incomplets")
        }

        return try {
            val qrCode = runBlocking {
                val api = TurbApi(user, pass)
                api.initLogin()
                val qr = api.getQrPayload()
                api.close()
                qr
            }

            TurboSelfCacheStorage.save(context, qrCode)

            FetchQRcodeResult(qrcode = qrCode)

        } catch (e: Exception) {
            FetchQRcodeResult(error = e.message ?: e.toString())
            Log.e("TurboselfUtils", "Erreur lors de la récupération du QR code", e)
        } as FetchQRcodeResult
    }
}
