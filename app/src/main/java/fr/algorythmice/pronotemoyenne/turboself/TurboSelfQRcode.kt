package fr.algorythmice.pronotemoyenne.turboself

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import fr.algorythmice.pronotemoyenne.HomeActivity
import fr.algorythmice.pronotemoyenne.R
import fr.algorythmice.pronotemoyenne.Utils
import fr.algorythmice.pronotemoyenne.databinding.ActivityTurboSelfQrCodeBinding

class TurboSelfQRcode : AppCompatActivity() {
    private lateinit var bind: ActivityTurboSelfQrCodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bind = ActivityTurboSelfQrCodeBinding.inflate(layoutInflater)
        setContentView(bind.root)

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
                LoginTurboSelfStorage.save(this, user, pass)
                goToQRcode()
            } else {
                bind.errorText.text = getString(R.string.please_fill_in_the_fields)
            }
        }

        bind.returnBtn.setOnClickListener {
            goToHommeAcivity()
        }
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

    private fun goToQRcode() {
        setResult(RESULT_OK)
        finish()
    }

    private fun goToHommeAcivity() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

}