package fr.algorythmice.pronotemoyenne.grades

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import fr.algorythmice.pronotemoyenne.HomeActivity
import fr.algorythmice.pronotemoyenne.R
import fr.algorythmice.pronotemoyenne.SettingsActivity
import fr.algorythmice.pronotemoyenne.Utils
import fr.algorythmice.pronotemoyenne.databinding.FragmentNotesBinding
import fr.algorythmice.pronotemoyenne.pronote.PronoteUtils
import fr.algorythmice.pronotemoyenne.pronote.PronoteUtils.NoteEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class GradesFragment : Fragment(R.layout.fragment_notes) {

    private var _bind: FragmentNotesBinding? = null
    private val bind get() = _bind!!

    private var updateTimerJob: Job? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _bind = FragmentNotesBinding.bind(view)


        bind.settingsBtn.setOnClickListener {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            (requireActivity() as HomeActivity).settingsLauncher.launch(intent)
        }

        bind.menuBtn.setOnClickListener {
            (requireActivity() as HomeActivity)
                .findViewById<DrawerLayout>(R.id.drawerLayout)
                .openDrawer(GravityCompat.START)
        }

        loadNotes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        updateTimerJob?.cancel()
        _bind = null
    }

    @SuppressLint("SetTextI18n")
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun reloadNotes() {
        updateTimerJob?.cancel()
        loadNotes()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadNotes() {
        bind.loading.visibility = View.VISIBLE
        bind.noteText.visibility = View.GONE
        bind.notesContainer.removeAllViews()

        val cachedNotes = GradesCacheStorage.loadNotes(requireContext())
        val cachedAverages = AveragesCacheStorageCacheStorage.loadAverages(requireContext())

        if (!cachedNotes.isNullOrEmpty() && !cachedAverages.isNullOrEmpty()) {
            displayNotes(cachedNotes, cachedAverages)
            val lastUpdate = GradesCacheStorage.getLastUpdate(requireContext())
            startUpdateTimer(lastUpdate)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                PronoteUtils.syncPronoteData(requireContext())
            }

            bind.loading.visibility = View.GONE

            if (result.error != null) {
                bind.noteText.apply {
                    visibility = View.VISIBLE
                    text = result.error
                    setTextColor(Color.RED)
                }
            } else {
                displayNotes(result.notes, result.average)
                startUpdateTimer(System.currentTimeMillis())
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n", "UseKtx")
    private fun displayNotes(parsedNotes: Map<String, List<NoteEntry>>,
                             parsedAverages: Map<String, List<Pair<Double, Double>>>) {
        bind.notesContainer.removeAllViews()

        val allAverages = parsedAverages.values.flatten()
        val moyenneGenerale = if (allAverages.isNotEmpty()) {
            val totalWeight = allAverages.sumOf { it.second }
            if (totalWeight != 0.0) allAverages.sumOf { it.first * it.second } / totalWeight else 0.0
        } else null

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

        parsedNotes.forEach { (subject, notes) ->
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

            notes.forEach { entry ->
                card.addView(
                    TextView(requireContext()).apply {
                        text = "%.2f/%.0f (coef: %.2f)".format(entry.note, entry.outOf, entry.coef)
                        setTextColor(Color.parseColor("#E8ECF2"))
                        textSize = 16f
                    }
                )
            }

            val moyenne = parsedAverages[subject]?.let { averages ->
                val total = averages.sumOf { it.second }
                if (total != 0.0) averages.sumOf { it.first * it.second } / total else null
            } ?: run {
                val total = notes.sumOf { it.coef }
                if (total != 0.0) notes.sumOf {
                    val note20 = if (it.outOf != 20.0) it.note * 20 / it.outOf else it.note
                    note20 * it.coef
                } / total else null
            }
            card.addView(
                TextView(requireContext()).apply {
                    text = moyenne?.let { "Moyenne : %.2f/20".format(it) } ?: "Moyenne : --/20"
                    setTextColor(Color.CYAN)
                    textSize = 16f
                    setTypeface(typeface, Typeface.BOLD)
                }
            )

            bind.notesContainer.addView(card)
        }
    }
}