package fr.algorythmice.pronotemoyenne

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import fr.algorythmice.pronotemoyenne.databinding.ActivitySettingsBinding
import fr.algorythmice.pronotemoyenne.EntListData.entList

class SettingsActivity : AppCompatActivity() {

    private lateinit var bind: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bind = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(bind.root)

        /* ---------- Pré-remplissage ---------- */

        bind.username.setText(LoginStorage.getUser(this))
        bind.password.setText(LoginStorage.getPass(this))
        bind.entDropdown.setText(LoginStorage.getEnt(this))

        /* ---------- Configuration des propriétés des champs de saisie ---------- */
        bind.username.isSingleLine = true
        bind.username.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
        bind.password.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE

        /* ---------- Bouton save désactivé ---------- */

        bind.saveBtn.isEnabled = false
        bind.saveBtn.alpha = 0.4f

        /* ---------- Listeners ---------- */

        bind.username.doOnTextChanged { _, _, _, _ -> updateSaveButtonState() }
        bind.password.doOnTextChanged { _, _, _, _ -> updateSaveButtonState() }
        bind.entDropdown.doOnTextChanged { _, _, _, _ -> updateSaveButtonState() }

        /* ---------- ENT dropdown ---------- */

        bind.entDropdown.setDropDownBackgroundDrawable(
            ContextCompat.getDrawable(this, R.drawable.bg_gradient_futuristic)
        )

        val adapter = EntAdapter(this, R.layout.spinner_item, entList)

        (bind.entDropdown as AutoCompleteTextView).apply {
            setAdapter(adapter)
            threshold = 1
        }

        bind.entDropdown.setOnItemClickListener { _, _, _, _ ->
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(bind.entDropdown.windowToken, 0)
        }

        /* ---------- Sélection établissement ---------- */

        val json = Utils.loadJsonFromAssets(this, "etablissements.json")
        val etablissements = Utils.parseEtablissements(json)

        val hasLocationPermission = Utils.hasLocationPermission(this)

        bind.selectEtablissementBtn.setOnClickListener {
            if (hasLocationPermission) {
                Utils.getLastLocation(
                    this,
                    onSuccess = { lat, lon ->
                        val proches = Utils.etablissementsDansUnRayon(etablissements, lat, lon)

                        if (proches.isNotEmpty()) {
                            val intent = Intent(this, EtablissementSelectActivity::class.java)
                            intent.putParcelableArrayListExtra("etablissements", ArrayList(proches))
                            startActivity(intent)
                        }
                    },
                    onError = { Log.d("LOC", it) }
                )
            } else {
                // Pas de permission → recherche manuelle forcée
                val intent = Intent(this, EtablissementSelectActivity::class.java)
                intent.putParcelableArrayListExtra("etablissements", arrayListOf())
                intent.putExtra("forceManual", true)
                startActivity(intent)
                // Désactiver le bouton pour feedback visuel
                bind.selectEtablissementBtn.isEnabled = false
                bind.selectEtablissementBtn.alpha = 0.4f
                android.widget.Toast.makeText(this, "La localisation n'est pas autorisée", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        /* ---------- Boutons ---------- */

        bind.saveBtn.setOnClickListener {
            updateSaveButtonState()
            if (!bind.saveBtn.isEnabled) return@setOnClickListener

            LoginStorage.save(
                this,
                bind.username.text.toString(),
                bind.password.text.toString(),
                bind.entDropdown.text.toString()
            )
            setResult(RESULT_OK)
            finish()
        }

        updateSaveButtonState()
    }

    override fun onResume() {
        super.onResume()
        updateSaveButtonState()
    }

    /* ---------- Permissions ---------- */

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) bind.selectEtablissementBtn.performClick()
        }

    /* ---------- État bouton ---------- */
    private fun updateSaveButtonState() {
        val user = bind.username.text.toString().trim()
        val pass = bind.password.text.toString().trim()
        val ent = bind.entDropdown.text.toString().trim()
        val urlPronote = LoginStorage.getUrlPronote(this)

        val enabled = Utils.isLoginComplete(
            user,
            pass,
            ent,
            urlPronote
        )


        bind.saveBtn.isEnabled = enabled
        bind.saveBtn.alpha = if (enabled) 1f else 0.4f
    }
}
