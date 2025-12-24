package fr.algorythmice.pronotemoyenne.homeworks

import android.annotation.SuppressLint
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
import fr.algorythmice.pronotemoyenne.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.toColorInt
import fr.algorythmice.pronotemoyenne.databinding.FragmentHomeworksBinding

class HomeworksFragment : Fragment(R.layout.fragment_homeworks) {

    private var _bind: FragmentHomeworksBinding? = null
    private val bind get() = _bind!!

    private var updateTimerJob: Job? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _bind = FragmentHomeworksBinding.bind(view)

        bind.menuBtn.setOnClickListener {
            (requireActivity() as HomeActivity)
                .findViewById<DrawerLayout>(R.id.drawerLayout)
                .openDrawer(GravityCompat.START)
        }

        loadHomeworks()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        updateTimerJob?.cancel()
        _bind = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadHomeworks() {
        bind.loading.visibility = View.VISIBLE
        bind.noteText.visibility = View.GONE
        bind.notesContainer.removeAllViews()

        val cachedRaw = HomeworksCacheStorage.loadHomeworks(requireContext())
        if (!cachedRaw.isNullOrEmpty()) {
            val parsed = Utils.parseHomeworks(cachedRaw)
            displayHomeworks(parsed)
            startUpdateTimer(HomeworksCacheStorage.getLastUpdate(requireContext()))
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
                displayHomeworks(result.homework)
                startUpdateTimer(System.currentTimeMillis())
            }
        }
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

                bind.titleText.text = "Mes Devoirs\n$text"
                delay(60000)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    private fun displayHomeworks(
        parsed: Map<String, Map<String, List<String>>>
    ) {
        bind.notesContainer.removeAllViews()

        // Trier les dates chronologiquement
        val sortedDates = parsed.keys
            .mapNotNull { dateStr ->
                try { java.time.LocalDate.parse(dateStr) } catch (_: Exception) { null }
            }
            .sorted() // du plus ancien au plus récent
            .map { it.toString() }

        sortedDates.forEach { date ->
            val subjects = parsed[date] ?: return@forEach

            val card = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(25, 25, 25, 25)
                background = requireContext().getDrawable(R.drawable.bg_glass)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 30) }
            }

            card.addView(TextView(requireContext()).apply {
                text = Utils.formatDateFr(date)
                setTextColor(Color.CYAN)
                textSize = 22f
                setTypeface(typeface, Typeface.BOLD)
            })

            subjects.forEach { (subject, list) ->
                card.addView(TextView(requireContext()).apply {
                    text = subject
                    setTextColor(Color.WHITE)
                    textSize = 20f
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, 15, 0, 10)
                })

                list.forEach {
                    card.addView(TextView(requireContext()).apply {
                        text = "• $it"
                        setTextColor("#E8ECF2".toColorInt())
                        textSize = 16f
                        setPadding(10, 5, 0, 5)
                    })
                }
            }

            bind.notesContainer.addView(card)
        }
    }
}
