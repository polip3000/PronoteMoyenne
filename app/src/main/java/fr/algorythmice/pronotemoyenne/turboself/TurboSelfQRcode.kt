package fr.algorythmice.pronotemoyenne.turboself

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import fr.algorythmice.pronotemoyenne.Utils
import fr.algorythmice.pronotemoyenne.databinding.ActivityTurboSelfQrCodeBinding

class TurboSelfQRcode : AppCompatActivity() {
    private lateinit var bind: ActivityTurboSelfQrCodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bind = ActivityTurboSelfQrCodeBinding.inflate(layoutInflater)
        setContentView(bind.root)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        displayQRcode()
    }

    private fun displayQRcode() {
        val cached = TurboSelfCacheStorage.getQRcodeNumber(this)
        if (!cached.isNullOrEmpty()) {
            val qrCode = generateQrCode(cached)
            bind.qrImageView.setImageBitmap(qrCode)
            bind.loading.visibility = View.VISIBLE
        }

        val result = Utils.fetchQRcode(this)

        if (result.error != null) {
            bind.noteText.apply {
                visibility = View.VISIBLE
                text = result.error
                setTextColor(Color.RED)
            }
        } else {
            val qrCode = generateQrCode(result.qrcode)
            bind.qrImageView.setImageBitmap(qrCode)
        }


        bind.loading.visibility = View.GONE


    }

    private fun generateQrCode(content: String): Bitmap {
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 400, 400)
        val bitmap = createBitmap(400, 400, Bitmap.Config.RGB_565)

        for (x in 0 until 400) {
            for (y in 0 until 400) {
                bitmap[x, y] = if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        return bitmap
    }
}