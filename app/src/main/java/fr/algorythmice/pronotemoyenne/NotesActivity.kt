package fr.algorythmice.pronotemoyenne

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import fr.algorythmice.pronotemoyenne.databinding.ActivityNotesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesActivity : AppCompatActivity() {

    private lateinit var bind: ActivityNotesBinding
    private var updateTimerJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        bind = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(bind.root)

        bind.settingsBtn.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivityForResult(intent, 100)
        }



        loadNotes()
    }

    private fun startUpdateTimer(lastUpdateMillis: Long) {
        updateTimerJob?.cancel() // annule un éventuel timer précédent

        updateTimerJob = lifecycleScope.launch {
            while (true) {
                val diffMs = System.currentTimeMillis() - lastUpdateMillis
                val diffMin = diffMs / 60000
                val diffHour = diffMin / 60
                val diffDay = diffHour / 24
                val diffMonth = diffDay / 30
                val diffYear = diffDay / 365

                val text = when {
                    diffMin < 1 -> "Mis à jour à l’instant"
                    diffMin < 60 -> "Mis à jour il y a $diffMin min"
                    diffHour < 24 -> "Mis à jour il y a $diffHour h"
                    diffDay < 30 -> "Mis à jour il y a $diffDay jour${if (diffDay > 1) "s" else ""}"
                    diffMonth < 12 -> "Mis à jour il y a $diffMonth mois"
                    else -> "Mis à jour il y a $diffYear an${if (diffYear > 1) "s" else ""}"
                }

                bind.titleText.text = "Mes Notes\n$text"

                kotlinx.coroutines.delay(60000) // mise à jour toutes les minutes
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            loadNotes()
        }
    }

    private fun loadNotes() {
        bind.loading.visibility = View.VISIBLE
        bind.noteText.visibility = View.GONE
        bind.notesContainer.removeAllViews()

        //Affichage du cache si présent
        val cached = NotesCacheStorage.loadNotes(this)
        if (cached != null && cached.isNotEmpty()) {
            displayNotesFuturistic(cached)
            val lastUpdate = NotesCacheStorage.getLastUpdate(this)
            startUpdateTimer(lastUpdate) // lance le timer pour le cache
            bind.loading.visibility = View.VISIBLE
        }

        //Récupération des nouvelles notes
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                Utils.fetchAndParseNotes(this@NotesActivity)
            }

            bind.loading.visibility = View.GONE

            if (result.error != null) {
                bind.noteText.apply {
                    visibility = View.VISIBLE
                    text = result.error
                    setTextColor(Color.RED)
                }
            } else {
                displayNotesFuturistic(result.notes)

                // sauvegarde du cache et mise à jour immédiate
                NotesCacheStorage.saveNotes(this@NotesActivity, result.notes)
                val now = System.currentTimeMillis()
                startUpdateTimer(now) // relance le timer avec "Mis à jour maintenant"
            }
        }
    }

    private fun displayNotesFuturistic(parsed: Map<String, List<Pair<Double, Double>>>) {
        bind.notesContainer.removeAllViews()

        val moyenneGenerale = Utils.computeGeneralAverage(parsed)

        val generalCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(25, 25, 25, 25)
            background = getDrawable(R.drawable.bg_glass)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 30)
            layoutParams = params
        }

        val generalTitle = TextView(this).apply {
            text = "Moyenne Générale"
            setTextColor(Color.WHITE)
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
        }

        val generalValue = TextView(this).apply {
            text = "%.2f/20".format(moyenneGenerale)
            setTextColor(Color.parseColor("#00E8FF"))
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
        }

        generalCard.addView(generalTitle)
        generalCard.addView(generalValue)

        bind.notesContainer.addView(generalCard)

        parsed.forEach { (subject, notes) ->
            // Crée une carte
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
                background = getDrawable(R.drawable.bg_glass)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 20)
                layoutParams = params
            }

            // Titre de la matière avec nombre de notes
            val title = TextView(this).apply {
                text = "$subject (${notes.size} notes)"
                setTextColor(Color.WHITE)
                textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
            }
            card.addView(title)

            // Notes
            notes.forEach { (note, coef) ->
                val noteView = TextView(this).apply {
                    text = "%.2f/20 (coef: %.2f)".format(note, coef)
                    setTextColor(Color.parseColor("#E8ECF2"))
                    textSize = 16f
                }
                card.addView(noteView)
            }

            // Moyenne
            val moyenne = notes.sumOf { it.first * it.second } / notes.sumOf { it.second }
            val moyenneView = TextView(this).apply {
                text = "Moyenne : %.2f/20".format(moyenne)
                setTextColor(Color.CYAN)
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
            }
            card.addView(moyenneView)

            bind.notesContainer.addView(card)
        }
    }
}
