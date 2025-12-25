package fr.algorythmice.pronotemoyenne

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import fr.algorythmice.pronotemoyenne.EntListData.entList
import fr.algorythmice.pronotemoyenne.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var bind: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            if (Utils.isLoginComplete(
                    LoginStorage.getUser(this),
                    LoginStorage.getPass(this),
                    LoginStorage.getEnt(this),
                    LoginStorage.getUrlPronote(this)
                )
            ) {
                goToNotes()
                return
            }


            bind = ActivityMainBinding.inflate(layoutInflater)
            setContentView(bind.root)

            bind.loginBtn.isEnabled = false
            bind.loginBtn.alpha = 0.4f

            bind.username.doOnTextChanged { _, _, _, _ ->
                updateLoginButtonState()
            }

            bind.password.doOnTextChanged { _, _, _, _ ->
                updateLoginButtonState()
            }

            bind.entDropdown.doOnTextChanged { _, _, _, _ ->
                updateLoginButtonState()
            }

            //configuration des propriétés des champs de saisie
            bind.username.isSingleLine = true
            bind.username.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            bind.password.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE


            val json = Utils.loadJsonFromAssets(this, "etablissements.json")
            val etablissements = Utils.parseEtablissements(json)

            val locationPermissionLauncher =
                registerForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->

                    val granted = permissions.any { it.value }

                    if (granted) {
                        bind.findNearbyBtn.performClick()
                    } else {
                        // Permission refusée → recherche manuelle directe
                        openManualSearch()
                    }
                }


            fun requestLocationPermission() {
                locationPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }

            bind.findNearbyBtn.setOnClickListener {
                if (Utils.hasLocationPermission(this)) {
                    Utils.getLastLocation(
                        this,
                        onSuccess = { lat, lon ->
                            val proches = Utils.etablissementsDansUnRayon(etablissements, lat, lon)

                            if (proches.isEmpty()) {
                                Log.d("LOC", "Aucun établissement proche trouvé")
                            } else {
                                val intent = Intent(this, EtablissementSelectActivity::class.java)
                                intent.putParcelableArrayListExtra(
                                    "etablissements",
                                    ArrayList(proches)
                                )
                                startActivity(intent)
                            }
                        },
                        onError = { err ->
                            Log.d("LOC", err)
                        }
                    )
                } else {
                    requestLocationPermission()
                }
            }

            bind.entDropdown.setDropDownBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_gradient_futuristic))

            val adapter = EntAdapter(
                this,
                R.layout.spinner_item,
                entList
            )

            (bind.entDropdown as AutoCompleteTextView).apply {
                setAdapter(adapter)
                threshold = 1
            }

            bind.entDropdown.setOnItemClickListener { _, _, _, _ ->
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(bind.entDropdown.windowToken, 0)
            }


            bind.loginBtn.setOnClickListener {
                val user = bind.username.text.toString()
                val pass = bind.password.text.toString()
                val ent = bind.entDropdown.text.toString()

                if (user.isNotEmpty() && pass.isNotEmpty()) {
                    LoginStorage.save(this, user, pass, ent)
                    goToNotes()
                } else {
                    bind.errorText.text = getString(R.string.please_fill_in_the_fields)
                }
            }
        } catch (e: Exception) {
            e.message?.let { Log.e("MainActivity", it) }
            android.widget.Toast.makeText(this, "Erreur initialisation: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            setContentView(android.R.layout.simple_list_item_1)
        }
    }

    override fun onResume() {
        super.onResume()
        updateLoginButtonState()
    }

    private fun openManualSearch() {
        val intent = Intent(this, EtablissementSelectActivity::class.java)
        intent.putParcelableArrayListExtra(
            "etablissements",
            arrayListOf()
        )
        intent.putExtra("forceManual", true)
        startActivity(intent)
    }


    private fun updateLoginButtonState() {
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

        bind.loginBtn.isEnabled = enabled
        bind.loginBtn.alpha = if (enabled) 1f else 0.4f
    }

    private fun goToNotes() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
