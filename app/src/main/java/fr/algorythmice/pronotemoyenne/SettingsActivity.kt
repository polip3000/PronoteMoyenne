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

        /* ---------- Toolbar ---------- */
        bind.toolbar.setNavigationOnClickListener {
            finish()
        }

        /* ---------- Pré-remplissage ---------- */

        bind.username.setText(LoginStorage.getUser(this))
        bind.password.setText(LoginStorage.getPass(this))

        // Pré-remplir l'ENT
        val savedEnt = LoginStorage.getEnt(this)
        if (!savedEnt.isNullOrEmpty()) {
            bind.entDropdown.setText(savedEnt, false)
        }

        // Afficher le nom de l'établissement
        val establishmentName = LoginStorage.getEstablishmentName(this)
        if (!establishmentName.isNullOrEmpty()) {
            bind.establishmentField.setText(establishmentName)
        } else {
            bind.establishmentField.setText("Aucun établissement sélectionné")
        }

        // Rendre le champ établissement cliquable
        bind.establishmentField.setOnClickListener {
            bind.selectEtablissementBtn.performClick()
        }

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
        val etablissements = Utils.parseEstablishments(json)

        val hasLocationPermission = Utils.hasLocationPermission(this)

        bind.selectEtablissementBtn.setOnClickListener {
            if (hasLocationPermission) {
                Utils.getLastLocation(
                    this,
                    onSuccess = { lat, lon ->
                        val proches = Utils.getEstablishmentsWithinRadius(etablissements, lat, lon)

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
        // Rafraîchir l'affichage de l'établissement
        val establishmentName = LoginStorage.getEstablishmentName(this)
        if (!establishmentName.isNullOrEmpty()) {
            bind.establishmentField.setText(establishmentName)
        } else {
            bind.establishmentField.setText("Aucun établissement sélectionné")
        }
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
