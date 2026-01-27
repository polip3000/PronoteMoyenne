package fr.algorythmice.pronotemoyenne.turboself

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import fr.algorythmice.pronotemoyenne.HomeActivity
import fr.algorythmice.pronotemoyenne.R
import fr.algorythmice.pronotemoyenne.Utils
import fr.algorythmice.pronotemoyenne.databinding.FragmentTurboSelfBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TurboSelfFragment : Fragment(R.layout.fragment_turbo_self) {
    private var _bind: FragmentTurboSelfBinding? = null
    private val bind get() = _bind!!
    private val repository by lazy { TurboselfRepository(requireContext()) }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _bind = FragmentTurboSelfBinding.bind(view)

        setupListeners()
        val credentials = getTurboSelfCredentials()
        val user = credentials.user
        val pass = credentials.pass

        if (!Utils.isTurboSelfLoginComplete(user, pass)) {
            goToTurboselfLogin()
            return
        }

        displayQRcode()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _bind = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        if (_bind != null) {
            setupListeners()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun goToTurboselfLogin() {
        val intent = Intent(requireContext(), TurboSelfQRcode::class.java)
        (requireActivity() as HomeActivity).turboSelfLauncher.launch(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupListeners() {

        bind.settingsBtn.setOnClickListener {
            goToTurboselfLogin()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun refreshUIAfterLogin() {
        if (_bind != null) {
            setupListeners()
            displayQRcode()
        }
    }

    data class TurboSelfCredentials(
        val user: String?,
        val pass: String?
    )

    private fun getTurboSelfCredentials(): TurboSelfCredentials {
        return TurboSelfCredentials(
            user = LoginTurboSelfStorage.getUser(requireContext()),
            pass = LoginTurboSelfStorage.getPass(requireContext())
        )
    }

    private fun displayQRcode() {
        bind.loading.visibility = View.VISIBLE
        bind.noteText.visibility = View.GONE

        val cached = repository.getCachedQr()
        if (!cached.isNullOrEmpty()) {
            val qrCode = QrCodeGenerator.generate(cached)
            bind.qrImageView.setImageBitmap(qrCode)
        }

        val credentials = getTurboSelfCredentials()
        val user = credentials.user
        val pass = credentials.pass

        if (user == "demonstration" && pass == "turboself") {
            bind.loading.visibility = View.GONE
            val qrnumber = "23497865"
            val qrCode = QrCodeGenerator.generate(qrnumber)
            bind.qrImageView.setImageBitmap(qrCode)
        } else {
            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    repository.fetchQrCode()
                }

                bind.loading.visibility = View.GONE

                if (result.error != null) {
                    bind.noteText.apply {
                        visibility = View.VISIBLE
                        text = result.error
                        setTextColor(Color.RED)
                    }
                } else if (result.qrcode != null) {
                    val qrCode = QrCodeGenerator.generate(result.qrcode)
                    bind.qrImageView.setImageBitmap(qrCode)
                }
            }
        }
    }
}