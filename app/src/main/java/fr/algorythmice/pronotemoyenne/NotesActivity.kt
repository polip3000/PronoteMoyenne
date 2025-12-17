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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            loadNotes()
        }
    }

    private fun loadNotes() {
        val user = LoginStorage.getUser(this)
        val pass = LoginStorage.getPass(this)
        val ent = LoginStorage.getEnt(this)
        val pronoteUrl = LoginStorage.getUrlPronote(this)



        bind.loading.visibility = View.VISIBLE
        bind.notesContainer.removeAllViews()
        bind.noteText.visibility = View.GONE

        if (user.isNullOrEmpty() || pass.isNullOrEmpty() || ent.isNullOrEmpty()) {
            bind.loading.visibility = View.GONE
            bind.noteText.apply {
                visibility = View.VISIBLE
                text = "Erreur : identifiants ou ent non fournis ou invalides."
                setTextColor(Color.RED)
                textSize = 18f
            }
            return
        }

        lifecycleScope.launch(Dispatchers.Main) {
            val rawResult = withContext(Dispatchers.IO) {
                try {
                    val py = Python.getInstance()
                    val module = py.getModule("pronote_fetch")
                    module.callAttr(
                        "get_notes",
                        pronoteUrl,
                        user,
                        pass,
                        ent
                    ).toString()
                } catch (_: Exception) {
                    null
                }
            }

            bind.loading.visibility = View.GONE

            if (rawResult == null) {
                bind.noteText.apply {
                    visibility = View.VISIBLE
                    text = "Erreur : impossible de récupérer les notes. Vérifiez votre connexion réseau. Ou vos identifiants"
                    setTextColor(Color.RED)
                    textSize = 18f
                }
            } else if (rawResult.contains("Erreur", ignoreCase = true)) {
                bind.noteText.apply {
                    visibility = View.VISIBLE
                    text = "Erreur : identifiants incorrects ou session expirée."
                    setTextColor(Color.RED)
                    textSize = 18f
                }
            } else {
                try {
                    val parsedNotes = parseAndComputeNotes(rawResult) // ta fonction qui retourne Map<String, List<Pair<Double, Double>>>
                    displayNotesFuturistic(parsedNotes)
                } catch (e: Exception) {
                    bind.noteText.apply {
                        visibility = View.VISIBLE
                        text = "Erreur lors du traitement des notes : ${e.message}"
                        setTextColor(Color.RED)
                        textSize = 18f
                    }
                }
            }
        }
    }

    private fun parseAndComputeNotes(raw: String): Map<String, List<Pair<Double, Double>>> {
        val result = mutableMapOf<String, MutableList<Pair<Double, Double>>>()
        val lines = raw.lines()
        var currentSubject = ""
        var notes = mutableListOf<Pair<Double, Double>>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("Matière :")) {
                if (notes.isNotEmpty()) {
                    result[currentSubject] = notes
                    notes = mutableListOf()
                }
                currentSubject = trimmed.removePrefix("Matière :").trim()
            } else if (trimmed.isNotEmpty() && !trimmed.contains("abs", ignoreCase = true)) {
                val noteMatch = Regex("""([\d.,]+)/(\d+)\s*\(coef:\s*([\d.,]+)\)""").find(trimmed)
                if (noteMatch != null) {
                    val (noteStr, surStr, coefStr) = noteMatch.destructured
                    val note = noteStr.replace(",", ".").toDouble()
                    val sur = surStr.toDouble()
                    val coef = coefStr.replace(",", ".").toDouble()

                    val noteSur20: Double
                    val coefFinal: Double
                    if (sur != 20.0) {
                        noteSur20 = note * 20 / sur
                        coefFinal = coef * sur / 20
                    } else {
                        noteSur20 = note
                        coefFinal = coef
                    }

                    notes.add(noteSur20 to coefFinal)
                }
            }
        }

        if (notes.isNotEmpty()) {
            result[currentSubject] = notes
        }

        return result
    }

    private fun computeGeneralAverage(parsed: Map<String, List<Pair<Double, Double>>>): Double {
        val subjectAverages = parsed.map { (_, notes) ->
            val moyenneMatiere = notes.sumOf { it.first * it.second } / notes.sumOf { it.second }
            moyenneMatiere
        }

        return subjectAverages.average()
    }

    private fun displayNotesFuturistic(parsed: Map<String, List<Pair<Double, Double>>>) {
        bind.notesContainer.removeAllViews()

        val moyenneGenerale = computeGeneralAverage(parsed)

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
