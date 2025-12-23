package fr.algorythmice.pronotemoyenne.turboself

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import fr.algorythmice.pronotemoyenne.R
import fr.algorythmice.pronotemoyenne.databinding.FragmentTurboSelfBinding
import fr.algorythmice.pronotemoyenne.HomeActivity
import fr.algorythmice.pronotemoyenne.Utils

class TurboSelfFragment : Fragment(R.layout.fragment_turbo_self) {
    private var _bind: FragmentTurboSelfBinding? = null
    private val bind get() = _bind!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Utils.isLoginCompleteTurboSelf(
                LoginTurboSelfStorage.getUser(requireContext()),
                LoginTurboSelfStorage.getPass(requireContext())
            )
        ) {
            goToQRcode()
            return
        }

        _bind = FragmentTurboSelfBinding.bind(view)

        bind.loginBtn.isEnabled = false
        bind.loginBtn.alpha = 0.4f

        bind.username.doOnTextChanged { _, _, _, _ ->
            updateLoginButtonState()
        }

        bind.password.doOnTextChanged { _, _, _, _ ->
            updateLoginButtonState()
        }

        bind.username.isSingleLine = true
        bind.username.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
        bind.password.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE


        bind.loginBtn.setOnClickListener {
            val user = bind.username.text.toString()
            val pass = bind.password.text.toString()

            if (user.isNotEmpty() && pass.isNotEmpty()) {
                LoginTurboSelfStorage.save(requireContext(), user, pass)
                goToQRcode()
            } else {
                bind.errorText.text = getString(R.string.veuillez_remplir_les_champs)
            }
        }



    }

    override fun onDestroyView() {
        super.onDestroyView()
        _bind = null
    }

    private fun updateLoginButtonState() {
        val user = bind.username.text.toString().trim()
        val pass = bind.password.text.toString().trim()

        val enabled = Utils.isLoginCompleteTurboSelf(
            user,
            pass
        )

        bind.loginBtn.isEnabled = enabled
        bind.loginBtn.alpha = if (enabled) 1f else 0.4f
    }

    private fun goToQRcode(){
        val intent = Intent(requireContext(), TurboSelfQRcode::class.java)
        (requireActivity() as HomeActivity).settingsLauncher.launch(intent)
    }
}