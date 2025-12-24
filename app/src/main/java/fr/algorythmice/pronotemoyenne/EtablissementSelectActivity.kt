package fr.algorythmice.pronotemoyenne

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

@Suppress("DEPRECATION")
class EtablissementSelectActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: EditText
    private lateinit var manualSearchBtn: Button
    private lateinit var positionSearchBtn: Button
    private lateinit var titleetablissement: TextView
    private lateinit var adapter: EtablissementAdapter
    private lateinit var allEtablissements: List<Etablissement>
    private lateinit var etablissements: List<Etablissement>

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_etablissement_select)

        recyclerView = findViewById(R.id.etablissementRecyclerView)
        searchInput = findViewById(R.id.searchInput)
        manualSearchBtn = findViewById(R.id.manualSearchBtn)
        positionSearchBtn = findViewById(R.id.positionSearchBtn)
        titleetablissement = findViewById(R.id.titleEtablissement)

        etablissements =
            intent.getParcelableArrayListExtra<Etablissement>("etablissements") ?: emptyList()

        val forceManual = intent.getBooleanExtra("forceManual", false)
        val hasLocationPermission = Utils.hasLocationPermission(this)

        val json = Utils.loadJsonFromAssets(this, "etablissements_parent.json")
        allEtablissements = Utils.parseEtablissements(json)

        // Configuration du champ de recherche
        searchInput.apply {
            isSingleLine = true
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
        }

        fun showList(list: List<Etablissement>) {
            recyclerView.layoutManager = LinearLayoutManager(this)
            adapter = EtablissementAdapter(list) { selected ->
                LoginStorage.saveUrlPronote(this, selected.urlPronote)
                Toast.makeText(this, "Établissement enregistré", Toast.LENGTH_SHORT).show()
                finish()
            }
            recyclerView.adapter = adapter
        }

        // Liste par défaut : établissements proches
        if (forceManual || etablissements.isEmpty() || !hasLocationPermission) {
            // Mode recherche manuelle direct
            searchInput.visibility = View.VISIBLE
            manualSearchBtn.visibility = View.GONE
            positionSearchBtn.visibility = View.VISIBLE
            titleetablissement.text = getString(R.string.recherche_tablissement)

            positionSearchBtn.apply {
                isEnabled = false
                alpha = 0.4f
            }

            adapter = EtablissementAdapter(allEtablissements) { selected ->
                LoginStorage.saveUrlPronote(this, selected.urlPronote)
                Toast.makeText(this, "Établissement enregistré", Toast.LENGTH_SHORT).show()
                finish()
            }
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = adapter

            searchInput.requestFocus()
        } else {
            // Mode établissements proches
            showList(etablissements)
            searchInput.visibility = View.GONE
            manualSearchBtn.visibility = View.VISIBLE
            positionSearchBtn.visibility = View.GONE
        }

        // Recherche manuelle
        manualSearchBtn.setOnClickListener {
            searchInput.visibility = View.VISIBLE
            manualSearchBtn.visibility = View.GONE
            positionSearchBtn.visibility = View.VISIBLE
            titleetablissement.text = getString(R.string.recherche_tablissement)

            searchInput.setText("")
            searchInput.requestFocus()

            adapter.updateList(allEtablissements)
        }

        // Retour aux établissements proches
        positionSearchBtn.setOnClickListener {
            if (!Utils.hasLocationPermission(this)) {
                Toast.makeText(
                    this,
                    "La localisation n'est pas autorisée",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            searchInput.visibility = View.GONE
            manualSearchBtn.visibility = View.VISIBLE
            positionSearchBtn.visibility = View.GONE
            titleetablissement.text = getString(R.string.tablissements_proches)
            showList(etablissements)
        }

        // Filtrage en temps réel
        searchInput.doOnTextChanged { text, _, _, _ ->
            val filtered = allEtablissements.filter {
                it.appellationOfficielle.contains(text.toString(), ignoreCase = true)
            }
            adapter.updateList(filtered)
        }
    }
}

