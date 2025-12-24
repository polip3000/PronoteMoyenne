package fr.algorythmice.pronotemoyenne.turboself

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import fr.algorythmice.pronotemoyenne.R
import fr.algorythmice.pronotemoyenne.databinding.FragmentTurboSelfBinding
import fr.algorythmice.pronotemoyenne.HomeActivity
import fr.algorythmice.pronotemoyenne.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TurboSelfFragment : Fragment(R.layout.fragment_turbo_self) {
    private var _bind: FragmentTurboSelfBinding? = null
    private val bind get() = _bind!!
    private var needRefreshAfterLogin = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _bind = FragmentTurboSelfBinding.bind(view)

        setupListeners()

        if (!Utils.isLoginCompleteTurboSelf(
                LoginTurboSelfStorage.getUser(requireContext()),
                LoginTurboSelfStorage.getPass(requireContext())
            )
        ) {
            goToTurboselfLogin()
            return
        }

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(requireContext()))
        }

        displayQRcode()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _bind = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun goToTurboselfLogin(){
        val intent = Intent(requireContext(), TurboSelfQRcode::class.java)
        (requireActivity() as HomeActivity).turboSelfLauncher.launch(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupListeners() {
        bind.menuBtn.setOnClickListener {
            (requireActivity() as HomeActivity)
                .findViewById<DrawerLayout>(R.id.drawerLayout)
                .openDrawer(GravityCompat.START)
        }

        bind.settingsBtn.setOnClickListener {
            goToTurboselfLogin()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun refreshUIAfterLogin() {
        if (_bind != null) {
            setupListeners()
            displayQRcode()
        } else {
            needRefreshAfterLogin = true
        }
    }


    private fun displayQRcode() {
        bind.loading.visibility = View.VISIBLE
        bind.noteText.visibility = View.GONE

        val cached = TurboSelfCacheStorage.getQRcodeNumber(requireContext())
        if (!cached.isNullOrEmpty()) {
            val qrCode = generateQrCode(cached)
            bind.qrImageView.setImageBitmap(qrCode)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                Utils.fetchQRcode(requireContext())
            }

            bind.loading.visibility = View.GONE

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
        }
    }


    private fun generateQrCode(content: String): Bitmap {
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 500, 500)
        val bitmap = createBitmap(500, 500, Bitmap.Config.RGB_565)

        for (x in 0 until 500) {
            for (y in 0 until 500) {
                bitmap[x, y] = if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        return bitmap
    }
}