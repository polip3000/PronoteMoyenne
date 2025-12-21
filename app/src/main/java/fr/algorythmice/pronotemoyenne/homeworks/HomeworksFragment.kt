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
import fr.algorythmice.pronotemoyenne.databinding.FragmentNotesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.toColorInt

class HomeworksFragment : Fragment(R.layout.fragment_homeworks) {

    private var _bind: FragmentNotesBinding? = null
    private val bind get() = _bind!!

    private var updateTimerJob: Job? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _bind = FragmentNotesBinding.bind(view)

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
                val diffMin = (System.currentTimeMillis() - lastUpdateMillis) / 60000
                bind.titleText.text =
                    "Mes Devoirs\nMis à jour il y a $diffMin min"
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

        parsed.forEach { (date, subjects) ->
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
