package fr.algorythmice.pronotemoyenne.grades

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import fr.algorythmice.pronotemoyenne.R
import fr.algorythmice.pronotemoyenne.SettingsActivity
import fr.algorythmice.pronotemoyenne.Utils
import fr.algorythmice.pronotemoyenne.databinding.FragmentNotesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesFragment : Fragment(R.layout.fragment_notes) {

    private var _bind: FragmentNotesBinding? = null
    private val bind get() = _bind!!

    private var updateTimerJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _bind = FragmentNotesBinding.bind(view)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(requireContext()))
        }

        bind.settingsBtn.setOnClickListener {
            startActivity(
                Intent(requireContext(), SettingsActivity::class.java)
            )
        }

        loadNotes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        updateTimerJob?.cancel()
        _bind = null
    }

    private fun startUpdateTimer(lastUpdateMillis: Long) {
        updateTimerJob?.cancel()

        updateTimerJob = viewLifecycleOwner.lifecycleScope.launch {
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
                delay(60000)
            }
        }
    }

    private fun loadNotes() {
        bind.loading.visibility = View.VISIBLE
        bind.noteText.visibility = View.GONE
        bind.notesContainer.removeAllViews()

        val cached = NotesCacheStorage.loadNotes(requireContext())
        if (!cached.isNullOrEmpty()) {
            displayNotesFuturistic(cached)
            val lastUpdate = NotesCacheStorage.getLastUpdate(requireContext())
            startUpdateTimer(lastUpdate)
            bind.loading.visibility = View.VISIBLE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                Utils.fetchAndParseNotes(requireContext())
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
                NotesCacheStorage.saveNotes(requireContext(), result.notes)
                startUpdateTimer(System.currentTimeMillis())
            }
        }
    }

    private fun displayNotesFuturistic(parsed: Map<String, List<Pair<Double, Double>>>) {
        bind.notesContainer.removeAllViews()

        val moyenneGenerale = Utils.computeGeneralAverage(parsed)

        val generalCard = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(25, 25, 25, 25)
            background = requireContext().getDrawable(R.drawable.bg_glass)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 30)
            }
        }

        val generalTitle = TextView(requireContext()).apply {
            text = "Moyenne Générale"
            setTextColor(Color.WHITE)
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
        }

        val generalValue = TextView(requireContext()).apply {
            text = "%.2f/20".format(moyenneGenerale)
            setTextColor(Color.parseColor("#00E8FF"))
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
        }

        generalCard.addView(generalTitle)
        generalCard.addView(generalValue)
        bind.notesContainer.addView(generalCard)

        parsed.forEach { (subject, notes) ->
            val card = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
                background = requireContext().getDrawable(R.drawable.bg_glass)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 20)
                }
            }

            val title = TextView(requireContext()).apply {
                text = "$subject (${notes.size} notes)"
                setTextColor(Color.WHITE)
                textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
            }
            card.addView(title)

            notes.forEach { (note, coef) ->
                card.addView(
                    TextView(requireContext()).apply {
                        text = "%.2f/20 (coef: %.2f)".format(note, coef)
                        setTextColor(Color.parseColor("#E8ECF2"))
                        textSize = 16f
                    }
                )
            }

            val moyenne = notes.sumOf { it.first * it.second } / notes.sumOf { it.second }
            card.addView(
                TextView(requireContext()).apply {
                    text = "Moyenne : %.2f/20".format(moyenne)
                    setTextColor(Color.CYAN)
                    textSize = 16f
                    setTypeface(typeface, Typeface.BOLD)
                }
            )

            bind.notesContainer.addView(card)
        }
    }
}